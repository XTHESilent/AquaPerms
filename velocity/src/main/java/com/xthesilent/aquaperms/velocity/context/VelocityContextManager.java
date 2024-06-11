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

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.velocitypowered.api.proxy.Player;
import com.xthesilent.aquaperms.common.context.manager.ContextManager;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsCache;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsSupplier;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;
import com.xthesilent.aquaperms.velocity.LPVelocityPlugin;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityContextManager extends ContextManager<Player, Player> {

    private final LoadingCache<Player, QueryOptionsCache<Player>> subjectCaches = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> new QueryOptionsCache<>(key, this));

    public VelocityContextManager(LPVelocityPlugin plugin) {
        super(plugin, Player.class, Player.class);
    }

    @Override
    public UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsSupplier getCacheFor(Player subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return this.subjectCaches.get(subject);
    }

    @Override
    protected void invalidateCache(Player subject) {
        QueryOptionsCache<Player> cache = this.subjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }
    }

    @Override
    public QueryOptions formQueryOptions(Player subject, ImmutableContextSet contextSet) {
        return formQueryOptions(contextSet);
    }
}
