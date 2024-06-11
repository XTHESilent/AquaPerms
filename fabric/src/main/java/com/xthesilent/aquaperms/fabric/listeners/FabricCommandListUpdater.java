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

package com.xthesilent.aquaperms.fabric.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.xthesilent.aquaperms.common.api.implementation.ApiGroup;
import com.xthesilent.aquaperms.common.cache.BufferedRequest;
import com.xthesilent.aquaperms.common.event.AquaPermsEventListener;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;
import com.xthesilent.aquaperms.fabric.LPFabricPlugin;
import com.aquasplashmc.api.event.EventBus;
import com.aquasplashmc.api.event.context.ContextUpdateEvent;
import com.aquasplashmc.api.event.group.GroupDataRecalculateEvent;
import com.aquasplashmc.api.event.user.UserDataRecalculateEvent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Calls {@link net.minecraft.server.PlayerManager#sendCommandTree()} when a players permissions change.
 *
 * TODO: Extract base class for this and BukkitCommandListUpdater
 */
public class FabricCommandListUpdater implements AquaPermsEventListener {

    private final LPFabricPlugin plugin;
    private final LoadingCache<UUID, SendBuffer> sendingBuffers = CaffeineFactory.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(SendBuffer::new);

    public FabricCommandListUpdater(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(EventBus bus) {
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        bus.subscribe(GroupDataRecalculateEvent.class, this::onGroupDataRecalculate);
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent e) {
        requestUpdate(e.getUser().getUniqueId());
    }

    private void onGroupDataRecalculate(GroupDataRecalculateEvent e) {
        plugin.getUserManager().getAll().values().stream()
            .filter(u -> u.resolveInheritanceTree(u.getQueryOptions())
                .contains(ApiGroup.cast(e.getGroup())))
            .forEach(u -> requestUpdate(u.getUniqueId()));
    }

    private void onContextUpdate(ContextUpdateEvent e) {
        e.getSubject(ServerPlayerEntity.class).ifPresent(p -> requestUpdate(p.getUuid()));
    }

    private void requestUpdate(UUID uniqueId) {
        if (!this.plugin.getBootstrap().isPlayerOnline(uniqueId)) {
            return;
        }

        // Buffer the request to send a commands update.
        this.sendingBuffers.get(uniqueId).request();
    }

    // Called when the buffer times out.
    private void sendUpdate(UUID uniqueId) {
        this.plugin.getBootstrap().getScheduler().sync()
                .execute(() -> this.plugin.getBootstrap().getPlayer(uniqueId)
                    .ifPresent(player -> this.plugin.getBootstrap().getServer()
                        .ifPresent(server -> {
                            server.getPlayerManager().sendCommandTree(player);
                        })
                    )
                );
    }

    private final class SendBuffer extends BufferedRequest<Void> {
        private final UUID uniqueId;

        SendBuffer(UUID uniqueId) {
            super(500, TimeUnit.MILLISECONDS, FabricCommandListUpdater.this.plugin.getBootstrap().getScheduler());
            this.uniqueId = uniqueId;
        }

        @Override
        protected Void perform() {
            sendUpdate(this.uniqueId);
            return null;
        }
    }
}
