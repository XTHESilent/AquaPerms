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

package com.xthesilent.aquaperms.common.model;

import com.xthesilent.aquaperms.common.api.implementation.ApiGroup;
import com.xthesilent.aquaperms.common.cache.Cache;
import com.xthesilent.aquaperms.common.cacheddata.GroupCachedDataManager;
import com.xthesilent.aquaperms.common.cacheddata.result.IntegerResult;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.aquasplashmc.api.node.NodeType;
import com.aquasplashmc.api.node.types.DisplayNameNode;
import com.aquasplashmc.api.node.types.WeightNode;
import com.aquasplashmc.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Optional;

public class Group extends PermissionHolder {
    private final ApiGroup apiProxy = new ApiGroup(this);

    /**
     * The name of the group
     */
    private final String name;

    /**
     * Caches the groups weight
     */
    private final Cache<IntegerResult<WeightNode>> weightCache = new WeightCache(this);

    /**
     * Caches the groups display name
     */
    private final Cache<Optional<String>> displayNameCache = new DisplayNameCache();

    /**
     * The groups data cache instance
     */
    private final GroupCachedDataManager cachedData;

    public Group(String name, AquaPermsPlugin plugin) {
        super(plugin, name.toLowerCase(Locale.ROOT));
        this.name = getIdentifier().getName();

        this.cachedData = new GroupCachedDataManager(this);
        getPlugin().getEventDispatcher().dispatchGroupCacheLoad(this, this.cachedData);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();

        // invalidate our caches
        this.weightCache.invalidate();
        this.displayNameCache.invalidate();
    }

    // name getters
    public String getName() {
        return this.name;
    }

    public ApiGroup getApiProxy() {
        return this.apiProxy;
    }

    @Override
    public QueryOptions getQueryOptions() {
        return getPlugin().getContextManager().getStaticQueryOptions();
    }

    @Override
    public GroupCachedDataManager getCachedData() {
        return this.cachedData;
    }

    @Override
    public Component getFormattedDisplayName() {
        String displayName = getDisplayName().orElse(null);
        if (displayName != null) {
            return Component.text()
                    .content(this.name)
                    .append(Component.space())
                    .append(Component.text()
                            .color(NamedTextColor.WHITE)
                            .append(Message.OPEN_BRACKET)
                            .append(Component.text(displayName))
                            .append(Message.CLOSE_BRACKET)
                    )
                    .build();
        } else {
            return Component.text(this.name);
        }
    }

    @Override
    public String getPlainDisplayName() {
        return getDisplayName().orElse(getName());
    }

    public Optional<String> getDisplayName() {
        return this.displayNameCache.get();
    }

    public Optional<String> calculateDisplayName(QueryOptions queryOptions) {
        // query for a displayname node
        for (DisplayNameNode n : getOwnNodes(NodeType.DISPLAY_NAME, queryOptions)) {
            return Optional.of(n.getDisplayName());
        }

        // fallback to config
        String name = getPlugin().getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).get(this.name);
        return name == null || name.equals(this.name) ? Optional.empty() : Optional.of(name);
    }

    @Override
    public IntegerResult<WeightNode> getWeightResult() {
        return this.weightCache.get();
    }

    @Override
    public HolderType getType() {
        return HolderType.GROUP;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Group)) return false;
        final Group other = (Group) o;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "Group(name=" + this.name + ")";
    }

    /**
     * Cache instance to supply the display name of a {@link Group}.
     */
    public class DisplayNameCache extends Cache<Optional<String>> {
        @Override
        protected @NonNull Optional<String> supply() {
            return calculateDisplayName(getPlugin().getContextManager().getStaticQueryOptions());
        }
    }
}
