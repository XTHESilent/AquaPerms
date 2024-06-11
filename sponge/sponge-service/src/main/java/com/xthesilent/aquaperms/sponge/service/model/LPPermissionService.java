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

package com.xthesilent.aquaperms.sponge.service.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.xthesilent.aquaperms.common.context.manager.ContextManager;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.sponge.service.PermissionAndContextService;
import com.xthesilent.aquaperms.sponge.service.reference.SubjectReferenceFactory;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.context.ImmutableContextSet;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.plugin.PluginContainer;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * AquaPerms model for the Sponge {@link PermissionService}
 */
public interface LPPermissionService {

    AquaPermsPlugin getPlugin();

    ContextManager<Subject, ServerPlayer> getContextManager();

    SubjectReferenceFactory getReferenceFactory();

    PermissionAndContextService sponge();

    LPSubjectCollection getUserSubjects();

    LPSubjectCollection getGroupSubjects();

    LPSubjectCollection getDefaultSubjects();

    LPSubject getRootDefaults();

    Predicate<String> getIdentifierValidityPredicate();

    LPSubjectCollection getCollection(String identifier);

    ImmutableMap<String, LPSubjectCollection> getLoadedCollections();

    LPPermissionDescription registerPermissionDescription(String id, Component description, PluginContainer owner);

    Optional<LPPermissionDescription> getDescription(String permission);

    ImmutableCollection<LPPermissionDescription> getDescriptions();

    void registerContextCalculator(ContextCalculator calculator);

    ImmutableContextSet getContextsForCause(Cause cause);

    ImmutableContextSet getContextsForCurrentCause();

    void fireUpdateEvent(LPSubjectData subjectData);

    void invalidateAllCaches();
}
