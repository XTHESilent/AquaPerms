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

package com.xthesilent.aquaperms.bukkit.vault;

import com.google.common.base.Strings;
import com.xthesilent.aquaperms.bukkit.LPBukkitPlugin;
import com.xthesilent.aquaperms.common.cacheddata.type.MetaAccumulator;
import com.xthesilent.aquaperms.common.cacheddata.type.MetaCache;
import com.xthesilent.aquaperms.common.cacheddata.type.MonitoredMetaCache;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.types.Meta;
import com.xthesilent.aquaperms.common.node.types.Prefix;
import com.xthesilent.aquaperms.common.node.types.Suffix;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.aquasplashmc.api.context.DefaultContextKeys;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.ChatMetaType;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.NodeType;
import com.aquasplashmc.api.query.Flag;
import com.aquasplashmc.api.query.QueryOptions;
import net.milkbowl.vault.chat.Chat;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * An implementation of the Vault {@link Chat} API using AquaPerms.
 *
 * AquaPerms is a multithreaded permissions plugin, and some actions require considerable
 * time to execute. (database queries, re-population of caches, etc) In these cases, the
 * operations required to make the edit apply will be processed immediately, but the process
 * of saving the change to the plugin storage will happen in the background.
 *
 * Methods that have to query data from the database will throw exceptions when called
 * from the main thread. Users of the Vault API expect these methods to be "main thread friendly",
 * which they simply cannot be, as LP utilises databases for data storage. Server admins
 * willing to take the risk of lagging their server can disable these exceptions in the config file.
 */
public class AquaPermsVaultChat extends AbstractVaultChat {

    // the plugin instance
    private final LPBukkitPlugin plugin;

    // the vault permission implementation
    private final AquaPermsVaultPermission vaultPermission;

    AquaPermsVaultChat(LPBukkitPlugin plugin, AquaPermsVaultPermission vaultPermission) {
        super(vaultPermission);
        this.plugin = plugin;
        this.vaultPermission = vaultPermission;
    }

    @Override
    public String getName() {
        return "AquaPerms";
    }

    @Override
    protected String convertWorld(String world) {
        return this.vaultPermission.isIgnoreWorld() ? null : super.convertWorld(world);
    }

    @Override
    public String getUserChatPrefix(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(uuid, world);
        MetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        return Strings.nullToEmpty(metaData.getPrefix(CheckOrigin.THIRD_PARTY_API).result());
    }

    @Override
    public String getUserChatSuffix(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(uuid, world);
        MetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        return Strings.nullToEmpty(metaData.getSuffix(CheckOrigin.THIRD_PARTY_API).result());
    }

    @Override
    public void setUserChatPrefix(String world, UUID uuid, String prefix) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        setChatMeta(user, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setUserChatSuffix(String world, UUID uuid, String suffix) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        setChatMeta(user, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getUserMeta(String world, UUID uuid, String key) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(uuid, world);
        MonitoredMetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        return metaData.getMetaValue(key, CheckOrigin.THIRD_PARTY_API).result();
    }

    @Override
    public void setUserMeta(String world, UUID uuid, String key, Object value) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        setMeta(user, key, value, world);
    }

    @Override
    public String getGroupChatPrefix(String world, String name) {
        Objects.requireNonNull(name, "name");
        MonitoredMetaCache metaData = getGroupMetaCache(name, world);
        if (metaData == null) {
            return null;
        }
        return Strings.nullToEmpty(metaData.getPrefix(CheckOrigin.THIRD_PARTY_API).result());
    }

    @Override
    public String getGroupChatSuffix(String world, String name) {
        Objects.requireNonNull(name, "name");
        MonitoredMetaCache metaData = getGroupMetaCache(name, world);
        if (metaData == null) {
            return null;
        }
        return Strings.nullToEmpty(metaData.getSuffix(CheckOrigin.THIRD_PARTY_API).result());
    }

    @Override
    public void setGroupChatPrefix(String world, String name, String prefix) {
        Objects.requireNonNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setChatMeta(group, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setGroupChatSuffix(String world, String name, String suffix) {
        Objects.requireNonNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setChatMeta(group, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getGroupMeta(String world, String name, String key) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(key, "key");
        MonitoredMetaCache metaData = getGroupMetaCache(name, world);
        if (metaData == null) {
            return null;
        }
        return metaData.getMetaValue(key, CheckOrigin.THIRD_PARTY_API).result();
    }

    @Override
    public void setGroupMeta(String world, String name, String key, Object value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(key, "key");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setMeta(group, key, value, world);
    }

    // utility methods for getting user and group instances

    private Group getGroup(String name) {
        return this.plugin.getGroupManager().getByDisplayName(name);
    }

    private MonitoredMetaCache getGroupMetaCache(String name, String world) {
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(null, world);
        return group.getCachedData().getMetaData(queryOptions);
    }

    private void setChatMeta(PermissionHolder holder, ChatMetaType type, String value, String world) {
        // remove all prefixes/suffixes directly set on the user/group
        holder.removeIf(DataType.NORMAL, null, type.nodeType()::matches, false);

        if (value == null) {
            this.vaultPermission.holderSave(holder);
            return;
        }

        // find the max inherited priority & add 10
        MetaAccumulator metaAccumulator = holder.accumulateMeta(createQueryOptionsForWorldSet(world));
        int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0) + 10;

        Node node = type.builder(value, priority)
                .withContext(DefaultContextKeys.SERVER_KEY, this.vaultPermission.getVaultServer())
                .withContext(DefaultContextKeys.WORLD_KEY, world == null ? "global" : world).build();

        holder.setNode(DataType.NORMAL, node, true);
        this.vaultPermission.holderSave(holder);
    }

    private void setMeta(PermissionHolder holder, String key, Object value, String world) {
        if (key.equalsIgnoreCase(Prefix.NODE_KEY) || key.equalsIgnoreCase(Suffix.NODE_KEY)) {
            setChatMeta(holder, ChatMetaType.valueOf(key.toUpperCase(Locale.ROOT)), value == null ? null : value.toString(), world);
            return;
        }

        holder.removeIf(DataType.NORMAL, null, NodeType.META.predicate(n -> n.getMetaKey().equals(key)), false);

        if (value == null) {
            this.vaultPermission.holderSave(holder);
            return;
        }

        Node node = Meta.builder(key, value.toString())
                .withContext(DefaultContextKeys.SERVER_KEY, this.vaultPermission.getVaultServer())
                .withContext(DefaultContextKeys.WORLD_KEY, world == null ? "global" : world)
                .build();

        holder.setNode(DataType.NORMAL, node, true);
        this.vaultPermission.holderSave(holder);
    }

    private QueryOptions createQueryOptionsForWorldSet(String world) {
        ImmutableContextSet.Builder context = new ImmutableContextSetImpl.BuilderImpl();
        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("global")) {
            context.add(DefaultContextKeys.WORLD_KEY, world.toLowerCase(Locale.ROOT));
        }
        context.add(DefaultContextKeys.SERVER_KEY, this.vaultPermission.getVaultServer());

        QueryOptions.Builder builder = QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder();
        builder.context(context.build());
        builder.flag(Flag.INCLUDE_NODES_WITHOUT_SERVER_CONTEXT, this.vaultPermission.isIncludeGlobal());
        return builder.build();
    }
}
