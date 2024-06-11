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

package com.xthesilent.aquaperms.common.model;

import com.xthesilent.aquaperms.common.cache.Cache;
import com.xthesilent.aquaperms.common.cacheddata.result.IntegerResult;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.aquasplashmc.api.node.NodeType;
import com.aquasplashmc.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Map;

/**
 * Cache instance to supply the weight of a {@link Group}.
 */
public class WeightCache extends Cache<IntegerResult<WeightNode>> {
    private final Group group;

    public WeightCache(Group group) {
        this.group = group;
    }

    @Override
    protected @NonNull IntegerResult<WeightNode> supply() {
        IntegerResult<WeightNode> weight = null;

        for (WeightNode n : this.group.getOwnNodes(NodeType.WEIGHT, QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL)) {
            int value = n.getWeight();
            if (weight == null || value > weight.intResult()) {
                weight = IntegerResult.of(n);
            }
        }

        if (weight == null) {
            Map<String, Integer> configWeights = this.group.getPlugin().getConfiguration().get(ConfigKeys.GROUP_WEIGHTS);
            Integer value = configWeights.get(this.group.getIdentifier().getName().toLowerCase(Locale.ROOT));
            if (value != null) {
                weight = IntegerResult.of(value);
            }
        }

        return weight != null ? weight : IntegerResult.nullResult();
    }
}
