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

package com.xthesilent.aquaperms.common.api.implementation;

import com.google.common.collect.ImmutableListMultimap;
import com.xthesilent.aquaperms.common.api.ApiUtils;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.manager.group.GroupManager;
import com.xthesilent.aquaperms.common.node.matcher.ConstraintNodeMatcher;
import com.xthesilent.aquaperms.common.node.matcher.StandardNodeMatchers;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.storage.misc.NodeEntry;
import com.xthesilent.aquaperms.common.util.ImmutableCollectors;
import com.aquasplashmc.api.event.cause.CreationCause;
import com.aquasplashmc.api.event.cause.DeletionCause;
import com.aquasplashmc.api.node.HeldNode;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.matcher.NodeMatcher;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiGroupManager extends ApiAbstractManager<Group, com.aquasplashmc.api.model.group.Group, GroupManager<?>> implements com.aquasplashmc.api.model.group.GroupManager {
    public ApiGroupManager(AquaPermsPlugin plugin, GroupManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected com.aquasplashmc.api.model.group.Group proxy(com.xthesilent.aquaperms.common.model.Group internal) {
        return internal == null ? null : internal.getApiProxy();
    }

    @Override
    public @NonNull CompletableFuture<com.aquasplashmc.api.model.group.Group> createAndLoadGroup(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().createAndLoadGroup(name, CreationCause.API)
                .thenApply(this::proxy);
    }

    @Override
    public @NonNull CompletableFuture<Optional<com.aquasplashmc.api.model.group.Group>> loadGroup(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().loadGroup(name).thenApply(opt -> opt.map(this::proxy));
    }

    @Override
    public @NonNull CompletableFuture<Void> saveGroup(com.aquasplashmc.api.model.group.@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return this.plugin.getStorage().saveGroup(ApiGroup.cast(group));
    }

    @Override
    public @NonNull CompletableFuture<Void> deleteGroup(com.aquasplashmc.api.model.group.@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        if (group.getName().equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }

        return this.plugin.getStorage().deleteGroup(ApiGroup.cast(group), DeletionCause.API);
    }

    @Override
    public @NonNull CompletableFuture<Void> modifyGroup(@NonNull String name, @NonNull Consumer<? super com.aquasplashmc.api.model.group.Group> action) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");

        return this.plugin.getStorage().createAndLoadGroup(name, CreationCause.API)
                .thenApplyAsync(group -> {
                    action.accept(group.getApiProxy());
                    return group;
                }, this.plugin.getBootstrap().getScheduler().async())
                .thenCompose(group -> this.plugin.getStorage().saveGroup(group));
    }

    @Override
    public @NonNull CompletableFuture<Void> loadAllGroups() {
        return this.plugin.getStorage().loadAllGroups();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Deprecated
    public @NonNull CompletableFuture<List<HeldNode<String>>> getWithPermission(@NonNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return (CompletableFuture) this.plugin.getStorage().searchGroupNodes(StandardNodeMatchers.key(permission));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> @NonNull CompletableFuture<Map<String, Collection<T>>> searchAll(@NonNull NodeMatcher<? extends T> matcher) {
        Objects.requireNonNull(matcher, "matcher");
        ConstraintNodeMatcher<? extends T> constraint = (ConstraintNodeMatcher<? extends T>) matcher;
        return this.plugin.getStorage().searchGroupNodes(constraint).thenApply(list -> {
            ImmutableListMultimap.Builder<String, T> builder = ImmutableListMultimap.builder();
            for (NodeEntry<String, ? extends T> row : list) {
                builder.put(row.getHolder(), row.getNode());
            }
            return builder.build().asMap();
        });
    }

    @Override
    public com.aquasplashmc.api.model.group.Group getGroup(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return proxy(this.handle.getIfLoaded(name));
    }

    @Override
    public @NonNull Set<com.aquasplashmc.api.model.group.Group> getLoadedGroups() {
        return this.handle.getAll().values().stream()
                .map(this::proxy)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.isLoaded(name);
    }
}
