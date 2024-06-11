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

package com.xthesilent.aquaperms.common.context.calculator;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.config.AquaPermsConfiguration;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.aquasplashmc.api.context.ContextConsumer;
import com.aquasplashmc.api.context.ContextSet;
import com.aquasplashmc.api.context.DefaultContextKeys;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.context.StaticContextCalculator;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ConfigurationContextCalculator implements StaticContextCalculator {
    private final AquaPermsConfiguration config;

    public ConfigurationContextCalculator(AquaPermsConfiguration config) {
        this.config = config;
    }

    @Override
    public void calculate(@NonNull ContextConsumer consumer) {
        String server = this.config.get(ConfigKeys.SERVER);
        if (!server.equals("global")) {
            consumer.accept(DefaultContextKeys.SERVER_KEY, server);
        }
        consumer.accept(this.config.getContextsFile().getStaticContexts());
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        calculate(builder::add);
        return builder.build();
    }
}
