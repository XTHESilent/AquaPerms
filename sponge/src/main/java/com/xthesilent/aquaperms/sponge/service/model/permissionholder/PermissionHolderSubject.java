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

package com.xthesilent.aquaperms.sponge.service.model.permissionholder;

import com.google.common.collect.ImmutableList;
import com.xthesilent.aquaperms.common.cacheddata.type.MetaCache;
import com.xthesilent.aquaperms.common.graph.TraversalAlgorithm;
import com.xthesilent.aquaperms.common.inheritance.InheritanceGraph;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.common.node.types.Prefix;
import com.xthesilent.aquaperms.common.node.types.Suffix;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.xthesilent.aquaperms.sponge.LPSpongePlugin;
import com.xthesilent.aquaperms.sponge.model.SpongeGroup;
import com.xthesilent.aquaperms.sponge.service.AquaPermsService;
import com.xthesilent.aquaperms.sponge.service.ProxyFactory;
import com.xthesilent.aquaperms.sponge.service.model.LPProxiedSubject;
import com.xthesilent.aquaperms.sponge.service.model.LPSubject;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectReference;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.query.QueryOptions;
import com.aquasplashmc.api.util.Tristate;
import org.spongepowered.api.service.permission.PermissionService;

import java.util.Optional;

/**
 * Implements {@link LPSubject} for a {@link PermissionHolder}.
 */
public abstract class PermissionHolderSubject<T extends PermissionHolder> implements LPSubject {
    protected final T parent;
    protected final LPSpongePlugin plugin;

    private final PermissionHolderSubjectData subjectData;
    private final PermissionHolderSubjectData transientSubjectData;

    private LPProxiedSubject spongeSubject = null;

    PermissionHolderSubject(LPSpongePlugin plugin, T parent) {
        this.parent = parent;
        this.plugin = plugin;
        this.subjectData = new PermissionHolderSubjectData(plugin.getService(), DataType.NORMAL, parent, this);
        this.transientSubjectData = new PermissionHolderSubjectData(plugin.getService(), DataType.TRANSIENT, parent, this);
    }

    public void fireUpdateEvent() {
        this.plugin.getService().fireUpdateEvent(this.subjectData);
        this.plugin.getService().fireUpdateEvent(this.transientSubjectData);
    }

    public T getParent() {
        return this.parent;
    }

    @Override
    public synchronized LPProxiedSubject sponge() {
        if (this.spongeSubject == null) {
            this.spongeSubject = ProxyFactory.toSponge(this);
        }
        return this.spongeSubject;
    }

    @Override
    public AquaPermsService getService() {
        return this.plugin.getService();
    }

    @Override
    public LPSubject getDefaults() {
        return this.plugin.getService().getDefaultSubjects().getTypeDefaults(getParentCollection().getIdentifier());
    }

    @Override
    public PermissionHolderSubjectData getSubjectData() {
        return this.subjectData;
    }

    @Override
    public PermissionHolderSubjectData getTransientSubjectData() {
        return this.transientSubjectData;
    }

    @Override
    public Tristate getPermissionValue(QueryOptions options, String permission) {
        return this.parent.getCachedData().getPermissionData(options).checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION_SET).result();
    }

    @Override
    public Tristate getPermissionValue(ImmutableContextSet contexts, String permission) {
        QueryOptions queryOptions = this.plugin.getContextManager().formQueryOptions(contexts);
        return getPermissionValue(queryOptions, permission);
    }

    @Override
    public boolean isChildOf(ImmutableContextSet contexts, LPSubjectReference parent) {
        return parent.collectionIdentifier().equals(PermissionService.SUBJECTS_GROUP) &&
                getPermissionValue(contexts, Inheritance.key(parent.subjectIdentifier())).asBoolean();
    }

    @Override
    public ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts) {
        InheritanceGraph graph = this.plugin.getInheritanceGraphFactory().getGraph(this.plugin.getContextManager().formQueryOptions(contexts));
        Iterable<PermissionHolder> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this.parent);

        ImmutableList.Builder<LPSubjectReference> subjects = ImmutableList.builder();
        for (PermissionHolder parent : traversal) {
            if (!(parent instanceof Group)) {
                continue;
            }

            subjects.add(((SpongeGroup) parent).sponge().toReference());
        }
        return subjects.build();
    }

    @Override
    public Optional<String> getOption(ImmutableContextSet contexts, String s) {
        MetaCache data = this.parent.getCachedData().getMetaData(this.plugin.getContextManager().formQueryOptions(contexts));
        if (s.equalsIgnoreCase(Prefix.NODE_KEY)) {
            String prefix = data.getPrefix(CheckOrigin.PLATFORM_API).result();
            if (prefix != null) {
                return Optional.of(prefix);
            }
        }

        if (s.equalsIgnoreCase(Suffix.NODE_KEY)) {
            String suffix = data.getSuffix(CheckOrigin.PLATFORM_API).result();
            if (suffix != null) {
                return Optional.of(suffix);
            }
        }

        String val = data.getMetaValue(s, CheckOrigin.PLATFORM_API).result();
        if (val != null) {
            return Optional.of(val);
        }

        Optional<String> v = getParentCollection().getDefaults().getOption(contexts, s);
        if (v.isPresent()) {
            return v;
        }

        return this.plugin.getService().getRootDefaults().getOption(contexts, s);
    }

    @Override
    public void performCacheCleanup() {
        // do nothing, permission holders are "cleaned up" by a different task
    }

    @Override
    public void invalidateCaches() {
        // invalidate for all changes
        this.parent.getCachedData().invalidate();
    }

}
