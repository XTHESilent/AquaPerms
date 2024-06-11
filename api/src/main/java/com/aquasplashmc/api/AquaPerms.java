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

package com.aquasplashmc.api;

import com.aquasplashmc.api.actionlog.ActionLogger;
import com.aquasplashmc.api.context.ContextCalculator;
import com.aquasplashmc.api.context.ContextManager;
import com.aquasplashmc.api.event.EventBus;
import com.aquasplashmc.api.messaging.MessagingService;
import com.aquasplashmc.api.messenger.MessengerProvider;
import com.aquasplashmc.api.metastacking.MetaStackDefinition;
import com.aquasplashmc.api.metastacking.MetaStackElement;
import com.aquasplashmc.api.metastacking.MetaStackFactory;
import com.aquasplashmc.api.model.group.Group;
import com.aquasplashmc.api.model.group.GroupManager;
import com.aquasplashmc.api.model.user.User;
import com.aquasplashmc.api.model.user.UserManager;
import com.aquasplashmc.api.node.NodeBuilderRegistry;
import com.aquasplashmc.api.node.matcher.NodeMatcherFactory;
import com.aquasplashmc.api.platform.Health;
import com.aquasplashmc.api.platform.Platform;
import com.aquasplashmc.api.platform.PlayerAdapter;
import com.aquasplashmc.api.platform.PluginMetadata;
import com.aquasplashmc.api.query.QueryOptionsRegistry;
import com.aquasplashmc.api.track.Track;
import com.aquasplashmc.api.track.TrackManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The AquaPerms API.
 *
 * <p>The API allows other plugins on the server to read and modify AquaPerms
 * data, change behaviour of the plugin, listen to certain events, and integrate
 * AquaPerms into other plugins and systems.</p>
 *
 * <p>This interface represents the base of the API package. All functions are
 * accessed via this interface.</p>
 *
 * <p>To start using the API, you need to obtain an instance of this interface.
 * These are registered by the AquaPerms plugin to the platforms Services
 * Manager. This is the preferred method for obtaining an instance.</p>
 *
 * <p>For ease of use, and for platforms without a Service Manager, an instance
 * can also be obtained from the static singleton accessor in
 * {@link AquaPermsProvider}.</p>
 */
public interface AquaPerms {

    /**
     * Gets the name of this server.
     *
     * <p>This is defined in the AquaPerms configuration file, and is used for
     * server specific permission handling.</p>
     *
     * <p>The default server name is "global".</p>
     *
     * @return the server name
     */
    @NonNull String getServerName();

    /**
     * Gets the {@link UserManager}, responsible for managing
     * {@link User} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link User} by uuid
     * or name, or query all loaded users.</p>
     *
     * @return the user manager
     */
    @NonNull UserManager getUserManager();

    /**
     * Gets the {@link GroupManager}, responsible for managing
     * {@link Group} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Group} by
     * name, or query all loaded groups.</p>
     *
     * @return the group manager
     */
    @NonNull GroupManager getGroupManager();

    /**
     * Gets the {@link TrackManager}, responsible for managing
     * {@link Track} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Track} by
     * name, or query all loaded tracks.</p>
     *
     * @return the track manager
     */
    @NonNull TrackManager getTrackManager();

    /**
     * Gets the {@link PlayerAdapter} instance, a utility class for adapting platform Player
     * instances to {@link User}s.
     *
     * <p>The {@code playerClass} parameter must be equal to the class or interface used by the
     * server platform to represent players.</p>
     *
     * <p>Specifically:</p>
     *
     * <p></p>
     * <ul>
     * <li>{@code org.bukkit.entity.Player}</li>
     * <li>{@code net.md_5.bungee.api.connection.ProxiedPlayer}</li>
     * <li>{@code org.spongepowered.api/entity.living.player.Player}</li>
     * <li>{@code net.minecraft.server.network.ServerPlayerEntity} (Fabric)</li>
     * <li>{@code cn.nukkit.Player}</li>
     * <li>{@code com.velocitypowered.api.proxy.Player}</li>
     * </ul>
     *
     * @param playerClass the class used by the platform to represent players
     * @param <T> the player class type
     * @return the player adapter
     * @throws IllegalArgumentException if the player class is not correct
     * @since 5.1
     */
    <T> @NonNull PlayerAdapter<T> getPlayerAdapter(@NonNull Class<T> playerClass);

    /**
     * Gets the {@link Platform}, which represents the server platform the
     * plugin is running on.
     *
     * @return the platform
     */
    @NonNull Platform getPlatform();

    /**
     * Gets the {@link PluginMetadata}, responsible for providing metadata about
     * the AquaPerms plugin currently running.
     *
     * @return the plugin metadata
     */
    @NonNull PluginMetadata getPluginMetadata();

    /**
     * Gets the {@link EventBus}, used for subscribing to internal AquaPerms
     * events.
     *
     * @return the event bus
     */
    @NonNull EventBus getEventBus();

    /**
     * Gets the {@link MessagingService}, used to dispatch updates throughout a
     * network of servers running the plugin.
     *
     * <p>Not all instances of AquaPerms will have a messaging service setup and
     * configured.</p>
     *
     * @return the messaging service instance, if present.
     */
    @NonNull Optional<MessagingService> getMessagingService();

    /**
     * Gets the {@link ActionLogger}, responsible for saving and broadcasting
     * defined actions occurring on the platform.
     *
     * @return the action logger
     */
    @NonNull ActionLogger getActionLogger();

    /**
     * Gets the {@link ContextManager}, responsible for managing
     * {@link ContextCalculator}s, and calculating applicable contexts.
     *
     * @return the context manager
     */
    @NonNull ContextManager getContextManager();

    /**
     * Gets the {@link MetaStackFactory}.
     *
     * <p>The metastack factory provides methods for retrieving
     * {@link MetaStackElement}s and constructing
     * {@link MetaStackDefinition}s.</p>
     *
     * @return the meta stack factory
     */
    @NonNull MetaStackFactory getMetaStackFactory();

    /**
     * Schedules the execution of an update task, and returns an encapsulation
     * of the task as a {@link CompletableFuture}.
     *
     * <p>The exact actions performed in an update task remains an
     * implementation detail of the plugin, however, as a minimum, it is
     * expected to perform a full reload of user, group and track data, and
     * ensure that any changes are fully applied and propagated.</p>
     *
     * @return a future
     */
    @NonNull CompletableFuture<Void> runUpdateTask();

    /**
     * Executes a health check.
     *
     * <p>This task checks if the AquaPerms implementation is running and
     * whether it has a connection to the database (if applicable).</p>
     *
     * @return the health status
     * @since 5.5
     */
    @NonNull Health runHealthCheck();

    /**
     * Registers a {@link MessengerProvider} for use by the platform.
     *
     * <p>Note that the mere action of registering a provider doesn't
     * necessarily mean that it will be used.</p>
     *
     * @param messengerProvider the messenger provider.
     */
    void registerMessengerProvider(@NonNull MessengerProvider messengerProvider);

    /**
     * Gets the {@link NodeBuilderRegistry}.
     *
     * @return the node builder registry
     */
    @Internal
    @NonNull NodeBuilderRegistry getNodeBuilderRegistry();

    /**
     * Gets the {@link QueryOptionsRegistry}.
     *
     * @return the query options registry
     * @since 5.1
     */
    @Internal
    @NonNull QueryOptionsRegistry getQueryOptionsRegistry();

    /**
     * Gets the {@link NodeMatcherFactory}.
     *
     * @return the node matcher factory
     * @since 5.1
     */
    @Internal
    @NonNull NodeMatcherFactory getNodeMatcherFactory();

}