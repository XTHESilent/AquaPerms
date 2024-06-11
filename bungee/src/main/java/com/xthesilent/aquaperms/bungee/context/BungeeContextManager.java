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

package com.xthesilent.aquaperms.bungee.context;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.xthesilent.aquaperms.bungee.LPBungeePlugin;
import com.xthesilent.aquaperms.common.context.manager.ContextManager;
import com.xthesilent.aquaperms.common.context.manager.InlineQueryOptionsSupplier;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsSupplier;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.query.QueryOptions;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeContextManager extends ContextManager<ProxiedPlayer, ProxiedPlayer> {

    private final LoadingCache<ProxiedPlayer, QueryOptions> contextsCache = CaffeineFactory.newBuilder()
            .expireAfterWrite(50, TimeUnit.MILLISECONDS)
            .build(this::calculate);

    public BungeeContextManager(LPBungeePlugin plugin) {
        super(plugin, ProxiedPlayer.class, ProxiedPlayer.class);
    }

    @Override
    public UUID getUniqueId(ProxiedPlayer player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsSupplier getCacheFor(ProxiedPlayer subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return new InlineQueryOptionsSupplier<>(subject, this.contextsCache);
    }

    // override getContext, getQueryOptions and invalidateCache to skip the QueryOptionsSupplier
    @Override
    public ImmutableContextSet getContext(ProxiedPlayer subject) {
        return getQueryOptions(subject).context();
    }

    @Override
    public QueryOptions getQueryOptions(ProxiedPlayer subject) {
        return this.contextsCache.get(subject);
    }

    @Override
    protected void invalidateCache(ProxiedPlayer subject) {
        this.contextsCache.invalidate(subject);
    }

    @Override
    public QueryOptions formQueryOptions(ProxiedPlayer subject, ImmutableContextSet contextSet) {
        return formQueryOptions(contextSet);
    }

}
