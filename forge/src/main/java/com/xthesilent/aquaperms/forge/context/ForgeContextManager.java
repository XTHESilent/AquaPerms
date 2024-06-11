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

package com.xthesilent.aquaperms.forge.context;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.context.manager.ContextManager;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsCache;
import com.xthesilent.aquaperms.forge.LPForgePlugin;
import com.xthesilent.aquaperms.forge.capabilities.UserCapabilityImpl;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.query.OptionKey;
import com.aquasplashmc.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ForgeContextManager extends ContextManager<ServerPlayer, ServerPlayer> {
    public static final OptionKey<Boolean> INTEGRATED_SERVER_OWNER = OptionKey.of("integrated_server_owner", Boolean.class);

    public ForgeContextManager(LPForgePlugin plugin) {
        super(plugin, ServerPlayer.class, ServerPlayer.class);
    }

    @Override
    public UUID getUniqueId(ServerPlayer player) {
        return player.getUUID();
    }

    @Override
    public QueryOptionsCache<ServerPlayer> getCacheFor(ServerPlayer subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return UserCapabilityImpl.get(subject).getQueryOptionsCache();
    }

    @Override
    public QueryOptions formQueryOptions(ServerPlayer subject, ImmutableContextSet contextSet) {
        QueryOptions.Builder builder = this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder();
        if (subject.getServer() != null && subject.getServer().isSingleplayerOwner(subject.getGameProfile())) {
            builder.option(INTEGRATED_SERVER_OWNER, true);
        }

        return builder.context(contextSet).build();
    }

    @Override
    public void invalidateCache(ServerPlayer subject) {
        UserCapabilityImpl capability = UserCapabilityImpl.getNullable(subject);
        if (capability != null) {
            capability.getQueryOptionsCache().invalidate();
        }
    }

}
