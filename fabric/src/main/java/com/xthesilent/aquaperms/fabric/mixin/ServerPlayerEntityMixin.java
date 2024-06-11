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

package com.xthesilent.aquaperms.fabric.mixin;

import com.xthesilent.aquaperms.common.cacheddata.type.MetaCache;
import com.xthesilent.aquaperms.common.cacheddata.type.PermissionCache;
import com.xthesilent.aquaperms.common.context.manager.QueryOptionsCache;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.xthesilent.aquaperms.fabric.context.FabricContextManager;
import com.xthesilent.aquaperms.fabric.event.PlayerChangeWorldCallback;
import com.xthesilent.aquaperms.fabric.model.MixinUser;
import com.aquasplashmc.api.query.QueryOptions;
import com.aquasplashmc.api.util.Tristate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ServerPlayerEntity} to store LP caches and implement {@link MixinUser}.
 *
 * <p>This mixin is also temporarily used to implement our internal PlayerChangeWorldCallback,
 * until a similar event is added to Fabric itself.</p>
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements MixinUser {

    /** Cache a reference to the LP {@link User} instance loaded for this player */
    private User aquaperms$user;

    /**
     * Hold a QueryOptionsCache instance on the player itself, so we can just cast instead of
     * having to maintain a map of Player->Cache.
     */
    private QueryOptionsCache<ServerPlayerEntity> aquaperms$queryOptions;

    // Used by PlayerChangeWorldCallback hook below.
    @Shadow public abstract ServerWorld getServerWorld();

    @Override
    public User getAquaPermsUser() {
        return this.aquaperms$user;
    }

    @Override
    public QueryOptionsCache<ServerPlayerEntity> getQueryOptionsCache() {
        return this.aquaperms$queryOptions;
    }

    @Override
    public QueryOptionsCache<ServerPlayerEntity> getQueryOptionsCache(FabricContextManager contextManager) {
        if (this.aquaperms$queryOptions == null) {
            this.aquaperms$queryOptions = contextManager.newQueryOptionsCache((ServerPlayerEntity) (Object) this);
        }
        return this.aquaperms$queryOptions;
    }

    @Override
    public void initializePermissions(User user) {
        this.aquaperms$user = user;

        // ensure query options cache is initialised too.
        if (this.aquaperms$queryOptions == null) {
            this.getQueryOptionsCache((FabricContextManager) user.getPlugin().getContextManager());
        }
    }

    @Override
    public Tristate hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        if (this.aquaperms$user == null || this.aquaperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return Tristate.UNDEFINED;
        }
        return hasPermission(permission, this.aquaperms$queryOptions.getQueryOptions());
    }

    @Override
    public Tristate hasPermission(String permission, QueryOptions queryOptions) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        final User user = this.aquaperms$user;
        if (user == null || this.aquaperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return Tristate.UNDEFINED;
        }

        PermissionCache data = user.getCachedData().getPermissionData(queryOptions);
        return data.checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
    }

    @Override
    public String getOption(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (this.aquaperms$user == null || this.aquaperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return null;
        }
        return getOption(key, this.aquaperms$queryOptions.getQueryOptions());
    }

    @Override
    public String getOption(String key, QueryOptions queryOptions) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        final User user = this.aquaperms$user;
        if (user == null || this.aquaperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return null;
        }

        MetaCache cache = user.getCachedData().getMetaData(queryOptions);
        return cache.getMetaOrChatMetaValue(key, CheckOrigin.PLATFORM_API);
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    private void aquaperms_copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        MixinUser oldMixin = (MixinUser) oldPlayer;
        this.aquaperms$user = oldMixin.getAquaPermsUser();
        this.aquaperms$queryOptions = oldMixin.getQueryOptionsCache();
        this.aquaperms$queryOptions.invalidate();
    }

    @Inject(at = @At("TAIL"), method = "worldChanged")
    private void aquaperms_onChangeDimension(ServerWorld targetWorld, CallbackInfo ci) {
        PlayerChangeWorldCallback.EVENT.invoker().onChangeWorld(this.getServerWorld(), targetWorld, (ServerPlayerEntity) (Object) this);
    }
}
