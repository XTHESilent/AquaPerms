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

package com.xthesilent.aquaperms.forge.service;

import com.xthesilent.aquaperms.common.cacheddata.type.MetaCache;
import com.xthesilent.aquaperms.common.cacheddata.type.PermissionCache;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.xthesilent.aquaperms.forge.LPForgeBootstrap;
import com.xthesilent.aquaperms.forge.LPForgePlugin;
import com.xthesilent.aquaperms.forge.capabilities.UserCapabilityImpl;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.query.QueryMode;
import com.aquasplashmc.api.query.QueryOptions;
import com.aquasplashmc.api.util.Tristate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionType;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ForgePermissionHandler implements IPermissionHandler {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(LPForgeBootstrap.ID, "permission_handler");

    private final LPForgePlugin plugin;
    private final Set<PermissionNode<?>> permissionNodes;

    public ForgePermissionHandler(LPForgePlugin plugin, Collection<PermissionNode<?>> permissionNodes) {
        this.plugin = plugin;
        this.permissionNodes = Collections.unmodifiableSet(new HashSet<>(permissionNodes));

        for (PermissionNode<?> node : this.permissionNodes) {
            this.plugin.getPermissionRegistry().insert(node.getNodeName());
        }
    }

    @Override
    public ResourceLocation getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<PermissionNode<?>> getRegisteredNodes() {
        return this.permissionNodes;
    }

    @Override
    public <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        UserCapabilityImpl capability = UserCapabilityImpl.getNullable(player);

        if (capability != null) {
            User user = capability.getUser();
            QueryOptions queryOptions = capability.getQueryOptionsCache().getQueryOptions();

            T value = getPermissionValue(user, queryOptions, node, context);
            if (value != null) {
                return value;
            }
        }

        return node.getDefaultResolver().resolve(player, player.getUUID(), context);
    }

    @Override
    public <T> T getOfflinePermission(UUID player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        User user = this.plugin.getUserManager().getIfLoaded(player);

        if (user != null) {
            QueryOptions queryOptions = user.getQueryOptions();
            T value = getPermissionValue(user, queryOptions, node, context);
            if (value != null) {
                return value;
            }
        }

        return node.getDefaultResolver().resolve(null, player, context);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPermissionValue(User user, QueryOptions queryOptions, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        queryOptions = appendContextToQueryOptions(queryOptions, context);
        String key = node.getNodeName();
        PermissionType<T> type = node.getType();

        // permission check
        if (type == PermissionTypes.BOOLEAN) {
            PermissionCache cache = user.getCachedData().getPermissionData(queryOptions);
            Tristate value = cache.checkPermission(key, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
            if (value != Tristate.UNDEFINED) {
                return (T) (Boolean) value.asBoolean();
            }
        }

        // meta lookup
        if (node.getType() == PermissionTypes.STRING) {
            MetaCache cache = user.getCachedData().getMetaData(queryOptions);
            String value = cache.getMetaOrChatMetaValue(node.getNodeName(), CheckOrigin.PLATFORM_API);
            if (value != null) {
                return (T) value;
            }
        }

        // meta lookup (integer)
        if (node.getType() == PermissionTypes.INTEGER) {
            MetaCache cache = user.getCachedData().getMetaData(queryOptions);
            String value = cache.getMetaOrChatMetaValue(node.getNodeName(), CheckOrigin.PLATFORM_API);
            if (value != null) {
                try {
                    return (T) Integer.valueOf(Integer.parseInt(value));
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    private static QueryOptions appendContextToQueryOptions(QueryOptions queryOptions, PermissionDynamicContext<?>... context) {
        if (context.length == 0 || queryOptions.mode() != QueryMode.CONTEXTUAL) {
            return queryOptions;
        }

        ImmutableContextSet.Builder contextBuilder = new ImmutableContextSetImpl.BuilderImpl()
                .addAll(queryOptions.context());

        for (PermissionDynamicContext<?> dynamicContext : context) {
            contextBuilder.add(dynamicContext.getDynamic().name(), dynamicContext.getSerializedValue());
        }

        return queryOptions.toBuilder().context(contextBuilder.build()).build();
    }

}
