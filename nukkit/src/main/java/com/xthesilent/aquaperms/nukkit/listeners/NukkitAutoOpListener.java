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

package com.xthesilent.aquaperms.nukkit.listeners;

import cn.nukkit.Player;
import com.xthesilent.aquaperms.common.api.implementation.ApiUser;
import com.xthesilent.aquaperms.common.event.AquaPermsEventListener;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.nukkit.LPNukkitPlugin;
import com.aquasplashmc.api.event.EventBus;
import com.aquasplashmc.api.event.context.ContextUpdateEvent;
import com.aquasplashmc.api.event.user.UserDataRecalculateEvent;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.Map;

/**
 * Implements the AquaPerms auto op feature.
 */
public class NukkitAutoOpListener implements AquaPermsEventListener {
    private static final String NODE = "aquaperms.autoop";

    private final LPNukkitPlugin plugin;

    public NukkitAutoOpListener(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(EventBus bus) {
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent e) {
        User user = ApiUser.cast(e.getUser());
        this.plugin.getBootstrap().getPlayer(user.getUniqueId()).ifPresent(this::refreshAutoOp);
    }

    private void onContextUpdate(ContextUpdateEvent e) {
        e.getSubject(Player.class).ifPresent(this::refreshAutoOp);
    }

    private void refreshAutoOp(Player player) {
        User user = this.plugin.getUserManager().getIfLoaded(player.getUniqueId());
        boolean value;

        if (user != null) {
            QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
            Map<String, Boolean> permData = user.getCachedData().getPermissionData(queryOptions).getPermissionMap();
            value = permData.getOrDefault(NODE, false);
        } else {
            value = false;
        }

        player.setOp(value);
    }

}
