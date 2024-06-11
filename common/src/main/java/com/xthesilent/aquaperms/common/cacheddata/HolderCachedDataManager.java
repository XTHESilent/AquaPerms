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

package com.xthesilent.aquaperms.common.cacheddata;

import com.xthesilent.aquaperms.common.cacheddata.type.MetaAccumulator;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.aquasplashmc.api.metastacking.MetaStackDefinition;
import com.aquasplashmc.api.node.ChatMetaType;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.Map;
import java.util.function.IntFunction;

/**
 * Holds an easily accessible cache of a holders data in a number of contexts
 */
public abstract class HolderCachedDataManager<T extends PermissionHolder> extends AbstractCachedDataManager {

    /**
     * The holder whom this data instance is representing
     */
    protected final T holder;

    public HolderCachedDataManager(T holder) {
        super(holder.getPlugin());
        this.holder = holder;
    }

    @Override
    protected QueryOptions getQueryOptions() {
        return this.holder.getQueryOptions();
    }

    @Override
    protected CalculatorFactory getCalculatorFactory() {
        return getPlugin().getCalculatorFactory();
    }

    @Override
    protected MetaStackDefinition getDefaultMetaStackDefinition(ChatMetaType type) {
        switch (type) {
            case PREFIX:
                return getPlugin().getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS);
            case SUFFIX:
                return getPlugin().getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS);
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected <M extends Map<String, Node>> M resolvePermissions(IntFunction<M> mapFactory, QueryOptions queryOptions) {
        return this.holder.exportPermissions(mapFactory, queryOptions, true, getPlugin().getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND));
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator, QueryOptions queryOptions) {
        this.holder.accumulateMeta(accumulator, queryOptions);
    }
}
