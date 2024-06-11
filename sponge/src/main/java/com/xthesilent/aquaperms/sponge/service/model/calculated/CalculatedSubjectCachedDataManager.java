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

package com.xthesilent.aquaperms.sponge.service.model.calculated;

import com.google.common.collect.ImmutableList;
import com.xthesilent.aquaperms.common.cacheddata.AbstractCachedDataManager;
import com.xthesilent.aquaperms.common.cacheddata.CacheMetadata;
import com.xthesilent.aquaperms.common.cacheddata.type.MetaAccumulator;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.calculator.PermissionCalculator;
import com.xthesilent.aquaperms.common.calculator.processor.DirectProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.PermissionProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.SpongeWildcardProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.WildcardProcessor;
import com.xthesilent.aquaperms.common.metastacking.SimpleMetaStackDefinition;
import com.xthesilent.aquaperms.common.metastacking.StandardStackElements;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.verbose.VerboseCheckTarget;
import com.xthesilent.aquaperms.sponge.calculator.FixedTypeDefaultsProcessor;
import com.xthesilent.aquaperms.sponge.calculator.RootDefaultsProcessor;
import com.aquasplashmc.api.metastacking.DuplicateRemovalFunction;
import com.aquasplashmc.api.metastacking.MetaStackDefinition;
import com.aquasplashmc.api.node.ChatMetaType;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

public class CalculatedSubjectCachedDataManager extends AbstractCachedDataManager implements CalculatorFactory {
    private static final MetaStackDefinition DEFAULT_META_STACK = new SimpleMetaStackDefinition(
            ImmutableList.of(StandardStackElements.HIGHEST),
            DuplicateRemovalFunction.RETAIN_ALL,
            "", "", ""
    );

    private final CalculatedSubject subject;

    CalculatedSubjectCachedDataManager(CalculatedSubject subject, AquaPermsPlugin plugin) {
        super(plugin);
        this.subject = subject;
    }

    @Override
    protected CacheMetadata getMetadataForQueryOptions(QueryOptions queryOptions) {
        VerboseCheckTarget target = VerboseCheckTarget.of(this.subject.getParentCollection().getIdentifier(), this.subject.getIdentifier().getName());
        return new CacheMetadata(null, target, queryOptions);
    }

    @Override
    protected QueryOptions getQueryOptions() {
        return this.subject.sponge().getQueryOptions();
    }

    @Override
    protected CalculatorFactory getCalculatorFactory() {
        return this;
    }

    @Override
    protected MetaStackDefinition getDefaultMetaStackDefinition(ChatMetaType type) {
        return DEFAULT_META_STACK;
    }

    @Override
    protected <M extends Map<String, Node>> M resolvePermissions(IntFunction<M> mapFactory, QueryOptions queryOptions) {
        M map = mapFactory.apply(16);
        this.subject.resolveAllPermissions(map, queryOptions);
        return map;
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator, QueryOptions queryOptions) {
        this.subject.resolveAllOptions(accumulator, queryOptions);
    }

    @Override
    public PermissionCalculator build(QueryOptions queryOptions, CacheMetadata metadata) {
        List<PermissionProcessor> processors = new ArrayList<>(5);
        processors.add(new DirectProcessor());
        processors.add(new SpongeWildcardProcessor());
        processors.add(new WildcardProcessor());

        if (!this.subject.getParentCollection().isDefaultsCollection()) {
            processors.add(new FixedTypeDefaultsProcessor(this.subject.getService(), queryOptions, this.subject.getDefaults(), true));
            processors.add(new RootDefaultsProcessor(this.subject.getService(), queryOptions, true));
        }

        return new PermissionCalculator(getPlugin(), metadata, processors);
    }
}
