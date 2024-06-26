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

package com.xthesilent.aquaperms.fabric.model;

import com.xthesilent.aquaperms.common.context.manager.QueryOptionsCache;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.fabric.context.FabricContextManager;
import com.aquasplashmc.api.query.QueryOptions;
import com.aquasplashmc.api.util.Tristate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

/**
 * Mixin interface for {@link ServerPlayerEntity} implementing {@link User} related
 * caches and functions.
 */
public interface MixinUser {

    User getAquaPermsUser();

    QueryOptionsCache<ServerPlayerEntity> getQueryOptionsCache();

    /**
     * Gets (or creates using the manager) the objects {@link QueryOptionsCache}.
     *
     * @param contextManager the contextManager
     * @return the cache
     */
    QueryOptionsCache<ServerPlayerEntity> getQueryOptionsCache(FabricContextManager contextManager);

    /**
     * Initialises permissions for this player using the given {@link User}.
     *
     * @param user the user
     */
    void initializePermissions(User user);

    // methods to perform permission checks using the User instance initialised on login

    Tristate hasPermission(String permission);

    Tristate hasPermission(String permission, QueryOptions queryOptions);

    String getOption(String key);

    String getOption(String key, QueryOptions queryOptions);

}
