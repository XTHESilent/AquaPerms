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

package com.xthesilent.aquaperms.velocity.context;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.velocity.LPVelocityPlugin;
import com.aquasplashmc.api.context.ContextCalculator;
import com.aquasplashmc.api.context.ContextConsumer;
import com.aquasplashmc.api.context.ContextSet;
import com.aquasplashmc.api.context.DefaultContextKeys;
import com.aquasplashmc.api.context.ImmutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class VelocityPlayerCalculator implements ContextCalculator<Player> {
    private final LPVelocityPlugin plugin;

    public VelocityPlayerCalculator(LPVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void calculate(@NonNull Player subject, @NonNull ContextConsumer consumer) {
        ServerConnection server = subject.getCurrentServer().orElse(null);
        if (server == null) {
            return;
        }
        this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(server.getServerInfo().getName(), consumer);
    }

    @Override
    public @NotNull @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        for (RegisteredServer server : this.plugin.getBootstrap().getProxy().getAllServers()) {
            builder.add(DefaultContextKeys.WORLD_KEY, server.getServerInfo().getName());
        }
        return builder.build();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerConnect(ServerConnectedEvent e) {
        this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
    }
}
