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

package com.xthesilent.aquaperms.forge.calculator;

import com.xthesilent.aquaperms.common.cacheddata.CacheMetadata;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.calculator.PermissionCalculator;
import com.xthesilent.aquaperms.common.calculator.processor.DirectProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.PermissionProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.RegexProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.SpongeWildcardProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.WildcardProcessor;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.forge.LPForgePlugin;
import com.xthesilent.aquaperms.forge.context.ForgeContextManager;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.List;

public class ForgeCalculatorFactory implements CalculatorFactory {
    private final LPForgePlugin plugin;

    public ForgeCalculatorFactory(LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PermissionCalculator build(QueryOptions queryOptions, CacheMetadata metadata) {
        List<PermissionProcessor> processors = new ArrayList<>(5);

        processors.add(new DirectProcessor());

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(new RegexProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(new WildcardProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS_SPONGE)) {
            processors.add(new SpongeWildcardProcessor());
        }

        boolean integratedOwner = queryOptions.option(ForgeContextManager.INTEGRATED_SERVER_OWNER).orElse(false);
        if (integratedOwner && this.plugin.getConfiguration().get(ConfigKeys.INTEGRATED_SERVER_OWNER_BYPASSES_CHECKS)) {
            processors.add(ServerOwnerProcessor.INSTANCE);
        }

        return new PermissionCalculator(this.plugin, metadata, processors);
    }

}
