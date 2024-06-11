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

package com.xthesilent.aquaperms.standalone.stub;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.context.manager.ContextManager;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsCache;
import com.xthesilent.aquaperms.standalone.LPStandalonePlugin;
import com.xthesilent.aquaperms.standalone.app.integration.SingletonPlayer;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.UUID;

public class StandaloneContextManager extends ContextManager<SingletonPlayer, SingletonPlayer> {
    private final QueryOptionsCache<SingletonPlayer> singletonCache = new QueryOptionsCache<>(SingletonPlayer.INSTANCE, this);

    public StandaloneContextManager(LPStandalonePlugin plugin) {
        super(plugin, SingletonPlayer.class, SingletonPlayer.class);
    }

    @Override
    public UUID getUniqueId(SingletonPlayer player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsCache<SingletonPlayer> getCacheFor(SingletonPlayer subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }
        return this.singletonCache;
    }

    @Override
    protected void invalidateCache(SingletonPlayer subject) {
        this.singletonCache.invalidate();
    }

    @Override
    public QueryOptions formQueryOptions(SingletonPlayer subject, ImmutableContextSet contextSet) {
        QueryOptions.Builder queryOptions = this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder();
        return queryOptions.context(contextSet).build();
    }
}
