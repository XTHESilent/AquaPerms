/*
 * This file is part of AquaPerms, licensed under the MIT License.
 *
 *  Copyright (c) AquasplashMC (XTHESilent) <xthesilent@aquasplashmc.com>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.xthesilent.aquaperms.common.cacheddata.type;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.xthesilent.aquaperms.common.cacheddata.result.IntegerResult;
import com.xthesilent.aquaperms.common.cacheddata.result.StringResult;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.node.types.Weight;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.aquasplashmc.api.metastacking.MetaStackDefinition;
import com.aquasplashmc.api.node.ChatMetaType;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.types.ChatMetaNode;
import com.aquasplashmc.api.node.types.MetaNode;
import com.aquasplashmc.api.node.types.PrefixNode;
import com.aquasplashmc.api.node.types.SuffixNode;
import com.aquasplashmc.api.node.types.WeightNode;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds temporary mutable meta whilst this object is passed up the
 * inheritance tree to accumulate meta from parents
 */
public class MetaAccumulator {

    public static MetaAccumulator makeFromConfig(AquaPermsPlugin plugin) {
        return new MetaAccumulator(
                plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS),
                plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS)
        );
    }

    /**
     * Represents the current state of a {@link MetaAccumulator}.
     */
    private enum State {
        /** Marks that the accumulator is still gaining (accumulating) new data. */
        ACCUMULATING,
        /** Marks that this accumulator is being completed. */
        COMPLETING,
        /** Marks that the process of gaining (accumulating) new data is complete. */
        COMPLETE
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.ACCUMULATING);

    private final ListMultimap<String, StringResult<MetaNode>> meta;
    private final SortedMap<Integer, StringResult<PrefixNode>> prefixes;
    private final SortedMap<Integer, StringResult<SuffixNode>> suffixes;
    private IntegerResult<WeightNode> weight;
    private String primaryGroup;

    private Set<String> seenNodeKeys = new HashSet<>();

    private final MetaStackDefinition prefixDefinition;
    private final MetaStackDefinition suffixDefinition;
    private final MetaStackAccumulator<PrefixNode> prefixAccumulator;
    private final MetaStackAccumulator<SuffixNode> suffixAccumulator;

    public MetaAccumulator(MetaStackDefinition prefixDefinition, MetaStackDefinition suffixDefinition) {
        Objects.requireNonNull(prefixDefinition, "prefixDefinition");
        Objects.requireNonNull(suffixDefinition, "suffixDefinition");
        this.meta = ArrayListMultimap.create();
        this.prefixes = new TreeMap<>(Comparator.reverseOrder());
        this.suffixes = new TreeMap<>(Comparator.reverseOrder());
        this.weight = IntegerResult.nullResult();
        this.prefixDefinition = prefixDefinition;
        this.suffixDefinition = suffixDefinition;
        this.prefixAccumulator = new MetaStackAccumulator<>(this.prefixDefinition, ChatMetaType.PREFIX);
        this.suffixAccumulator = new MetaStackAccumulator<>(this.suffixDefinition, ChatMetaType.SUFFIX);
    }

    private void ensureState(State state) {
        if (this.state.get() != state) {
            throw new IllegalStateException("State must be " + state + ", but is actually " + this.state.get());
        }
    }

    /**
     * "Completes" the accumulator, preventing any further changes.
     *
     * Also performs some final processing on the accumulators state, before
     * data is read.
     */
    public void complete() {
        if (!this.state.compareAndSet(State.ACCUMULATING, State.COMPLETING)) {
            return;
        }

        // perform final changes
        if (!this.meta.containsKey(Weight.NODE_KEY) && !this.weight.isNull()) {
            this.meta.put(Weight.NODE_KEY, StringResult.of(String.valueOf(this.weight.intResult())));
        }
        if (this.primaryGroup != null && !this.meta.containsKey("primarygroup")) {
            this.meta.put("primarygroup", StringResult.of(this.primaryGroup));
        }
        this.seenNodeKeys = null; // free up for GC

        this.state.set(State.COMPLETE);
    }

    // accumulate methods

    public void accumulateNode(Node n) {
        ensureState(State.ACCUMULATING);

        // only process distinct nodes once, allows inheritance to be
        // "cancelled out" by assigning a false copy.
        if (!this.seenNodeKeys.add(n.getKey())) {
            return;
        }

        if (!n.getValue()) {
            return;
        }

        if (n instanceof MetaNode) {
            MetaNode mn = (MetaNode) n;
            this.meta.put(mn.getMetaKey(), StringResult.of(mn));
        }

        if (n instanceof PrefixNode) {
            PrefixNode pn = (PrefixNode) n;
            this.prefixes.putIfAbsent(pn.getPriority(), StringResult.of(pn));
            this.prefixAccumulator.offer(pn);
        }

        if (n instanceof SuffixNode) {
            SuffixNode pn = (SuffixNode) n;
            this.suffixes.putIfAbsent(pn.getPriority(), StringResult.of(pn));
            this.suffixAccumulator.offer(pn);
        }
    }

    public void accumulateWeight(IntegerResult<WeightNode> weight) {
        ensureState(State.ACCUMULATING);
        if (this.weight.isNull() || weight.intResult() > this.weight.intResult()) {
            this.weight = weight;
        }
    }

    public void setPrimaryGroup(String primaryGroup) {
        ensureState(State.ACCUMULATING);
        this.primaryGroup = primaryGroup;
    }

    // read methods

    public ListMultimap<String, StringResult<MetaNode>> getMeta() {
        ensureState(State.COMPLETE);
        return this.meta;
    }

    public Map<Integer, ? extends StringResult<? extends ChatMetaNode<?, ?>>> getChatMeta(ChatMetaType type) {
        ensureState(State.COMPLETE);
        return type == ChatMetaType.PREFIX ? this.prefixes : this.suffixes;
    }

    public SortedMap<Integer, StringResult<PrefixNode>> getPrefixes() {
        ensureState(State.COMPLETE);
        return this.prefixes;
    }

    public SortedMap<Integer, StringResult<SuffixNode>> getSuffixes() {
        ensureState(State.COMPLETE);
        return this.suffixes;
    }

    public IntegerResult<WeightNode> getWeight() {
        ensureState(State.COMPLETE);
        return this.weight;
    }

    public String getPrimaryGroup() {
        ensureState(State.COMPLETE);
        return this.primaryGroup;
    }

    public MetaStackDefinition getPrefixDefinition() {
        ensureState(State.COMPLETE);
        return this.prefixDefinition;
    }

    public MetaStackDefinition getSuffixDefinition() {
        ensureState(State.COMPLETE);
        return this.suffixDefinition;
    }

    public StringResult<PrefixNode> getPrefix() {
        ensureState(State.COMPLETE);
        return this.prefixAccumulator.toResult();
    }

    public StringResult<SuffixNode> getSuffix() {
        ensureState(State.COMPLETE);
        return this.suffixAccumulator.toResult();
    }

    @Override
    public String toString() {
        return "MetaAccumulator(" +
                "meta=" + this.meta + ", " +
                "prefixes=" + this.prefixes + ", " +
                "suffixes=" + this.suffixes + ", " +
                "weight=" + this.weight + ", " +
                "primaryGroup=" + this.primaryGroup + ", " +
                "prefixStack=" + this.prefixAccumulator + ", " +
                "suffixStack=" + this.suffixAccumulator + ")";
    }
}
