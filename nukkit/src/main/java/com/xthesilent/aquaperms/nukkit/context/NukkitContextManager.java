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

package com.xthesilent.aquaperms.nukkit.context;

import cn.nukkit.Player;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.xthesilent.aquaperms.common.cache.LoadingMap;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.context.manager.ContextManager;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsCache;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;
import com.xthesilent.aquaperms.nukkit.LPNukkitPlugin;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.query.OptionKey;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NukkitContextManager extends ContextManager<Player, Player> {

    public static final OptionKey<Boolean> OP_OPTION = OptionKey.of("op", Boolean.class);

    // cache the creation of ContextsCache instances for online players with no expiry
    private final LoadingMap<Player, QueryOptionsCache<Player>> onlineSubjectCaches = LoadingMap.of(key -> new QueryOptionsCache<>(key, this));

    // cache the creation of ContextsCache instances for offline players with a 1m expiry
    private final LoadingCache<Player, QueryOptionsCache<Player>> offlineSubjectCaches = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> {
                QueryOptionsCache<Player> cache = this.onlineSubjectCaches.getIfPresent(key);
                if (cache != null) {
                    return cache;
                }
                return new QueryOptionsCache<>(key, this);
            });

    public NukkitContextManager(LPNukkitPlugin plugin) {
        super(plugin, Player.class, Player.class);
    }

    public void onPlayerQuit(Player player) {
        this.onlineSubjectCaches.remove(player);
    }

    @Override
    public UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsCache<Player> getCacheFor(Player subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        if (subject.isOnline()) {
            return this.onlineSubjectCaches.get(subject);
        } else {
            return this.offlineSubjectCaches.get(subject);
        }
    }

    @Override
    protected void invalidateCache(Player subject) {
        QueryOptionsCache<Player> cache = this.onlineSubjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }

        cache = this.offlineSubjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }
    }

    @Override
    public QueryOptions formQueryOptions(Player subject, ImmutableContextSet contextSet) {
        QueryOptions.Builder queryOptions = this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder();
        if (subject.isOp()) {
            queryOptions.option(OP_OPTION, true);
        }

        return queryOptions.context(contextSet).build();
    }
}
