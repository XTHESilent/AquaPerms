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

package com.xthesilent.aquaperms.common.api;

import com.xthesilent.aquaperms.common.api.implementation.ApiActionLogger;
import com.xthesilent.aquaperms.common.api.implementation.ApiContextManager;
import com.xthesilent.aquaperms.common.api.implementation.ApiGroupManager;
import com.xthesilent.aquaperms.common.api.implementation.ApiMessagingService;
import com.xthesilent.aquaperms.common.api.implementation.ApiMetaStackFactory;
import com.xthesilent.aquaperms.common.api.implementation.ApiNodeBuilderRegistry;
import com.xthesilent.aquaperms.common.api.implementation.ApiNodeMatcherFactory;
import com.xthesilent.aquaperms.common.api.implementation.ApiPlatform;
import com.xthesilent.aquaperms.common.api.implementation.ApiPlayerAdapter;
import com.xthesilent.aquaperms.common.api.implementation.ApiQueryOptionsRegistry;
import com.xthesilent.aquaperms.common.api.implementation.ApiTrackManager;
import com.xthesilent.aquaperms.common.api.implementation.ApiUserManager;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.event.AbstractEventBus;
import com.xthesilent.aquaperms.common.messaging.AquaPermsMessagingService;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.plugin.bootstrap.BootstrappedWithLoader;
import com.xthesilent.aquaperms.common.plugin.bootstrap.AquaPermsBootstrap;
import com.xthesilent.aquaperms.common.plugin.logging.PluginLogger;
import com.aquasplashmc.api.AquaPerms;
import com.aquasplashmc.api.AquaPermsProvider;
import com.aquasplashmc.api.actionlog.ActionLogger;
import com.aquasplashmc.api.context.ContextManager;
import com.aquasplashmc.api.messaging.MessagingService;
import com.aquasplashmc.api.messenger.MessengerProvider;
import com.aquasplashmc.api.metastacking.MetaStackFactory;
import com.aquasplashmc.api.model.group.GroupManager;
import com.aquasplashmc.api.model.user.UserManager;
import com.aquasplashmc.api.node.NodeBuilderRegistry;
import com.aquasplashmc.api.node.matcher.NodeMatcherFactory;
import com.aquasplashmc.api.platform.Health;
import com.aquasplashmc.api.platform.Platform;
import com.aquasplashmc.api.platform.PlayerAdapter;
import com.aquasplashmc.api.platform.PluginMetadata;
import com.aquasplashmc.api.query.QueryOptionsRegistry;
import com.aquasplashmc.api.track.TrackManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements the AquaPerms API using the plugin instance
 */
public class AquaPermsApiProvider implements AquaPerms {

    private final AquaPermsPlugin plugin;

    private final ApiPlatform platform;
    private final UserManager userManager;
    private final GroupManager groupManager;
    private final TrackManager trackManager;
    private final PlayerAdapter<?> playerAdapter;
    private final ActionLogger actionLogger;
    private final ContextManager contextManager;
    private final MetaStackFactory metaStackFactory;

    public AquaPermsApiProvider(AquaPermsPlugin plugin) {
        this.plugin = plugin;

        this.platform = new ApiPlatform(plugin);
        this.userManager = new ApiUserManager(plugin, plugin.getUserManager());
        this.groupManager = new ApiGroupManager(plugin, plugin.getGroupManager());
        this.trackManager = new ApiTrackManager(plugin, plugin.getTrackManager());
        this.playerAdapter = new ApiPlayerAdapter<>(plugin.getUserManager(), plugin.getContextManager());
        this.actionLogger = new ApiActionLogger(plugin);
        this.contextManager = new ApiContextManager(plugin, plugin.getContextManager());
        this.metaStackFactory = new ApiMetaStackFactory(plugin);
    }

    public void ensureApiWasLoadedByPlugin() {
        AquaPermsBootstrap bootstrap = this.plugin.getBootstrap();
        ClassLoader pluginClassLoader;
        if (bootstrap instanceof BootstrappedWithLoader) {
            pluginClassLoader = ((BootstrappedWithLoader) bootstrap).getLoader().getClass().getClassLoader();
        } else {
            pluginClassLoader = bootstrap.getClass().getClassLoader();
        }

        for (Class<?> apiClass : new Class[]{AquaPerms.class, AquaPermsProvider.class}) {
            ClassLoader apiClassLoader = apiClass.getClassLoader();

            if (!apiClassLoader.equals(pluginClassLoader)) {
                String guilty = "unknown";
                try {
                    guilty = bootstrap.identifyClassLoader(apiClassLoader);
                } catch (Exception e) {
                    // ignore
                }

                PluginLogger logger = this.plugin.getLogger();
                logger.warn("It seems that the AquaPerms API has been (class)loaded by a plugin other than AquaPerms!");
                logger.warn("The API was loaded by " + apiClassLoader + " (" + guilty + ") and the " +
                        "AquaPerms plugin was loaded by " + pluginClassLoader.toString() + ".");
                logger.warn("This indicates that the other plugin has incorrectly \"shaded\" the " +
                        "AquaPerms API into its jar file. This can cause errors at runtime and should be fixed.");
                return;
            }
        }
    }

    @Override
    public @NonNull String getServerName() {
        return this.plugin.getConfiguration().get(ConfigKeys.SERVER);
    }

    @Override
    public @NonNull Platform getPlatform() {
        return this.platform;
    }

    @Override
    public @NonNull PluginMetadata getPluginMetadata() {
        return this.platform;
    }

    @Override
    public @NonNull UserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public @NonNull GroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public @NonNull TrackManager getTrackManager() {
        return this.trackManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @NonNull PlayerAdapter<T> getPlayerAdapter(@NonNull Class<T> playerClass) {
        Objects.requireNonNull(playerClass, "playerClass");
        Class<?> expectedClass = this.plugin.getContextManager().getPlayerClass();
        if (!expectedClass.equals(playerClass)) {
            throw new IllegalArgumentException("Player class " + playerClass.getName() + " does not equal " + expectedClass.getName());
        }
        return (PlayerAdapter<T>) this.playerAdapter;
    }

    @Override
    public @NonNull CompletableFuture<Void> runUpdateTask() {
        return this.plugin.getSyncTaskBuffer().request();
    }

    @Override
    public @NonNull Health runHealthCheck() {
        return this.plugin.runHealthCheck();
    }

    @Override
    public @NonNull AbstractEventBus<?> getEventBus() {
        return this.plugin.getEventDispatcher().getEventBus();
    }

    @Override
    public @NonNull Optional<MessagingService> getMessagingService() {
        return this.plugin.getMessagingService().map(ApiMessagingService::new);
    }

    @Override
    public void registerMessengerProvider(@NonNull MessengerProvider messengerProvider) {
        if (this.plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).equals("custom")) {
            this.plugin.setMessagingService(new AquaPermsMessagingService(this.plugin, messengerProvider));
        }
    }

    @Override
    public @NonNull ActionLogger getActionLogger() {
        return this.actionLogger;
    }

    @Override
    public @NonNull ContextManager getContextManager() {
        return this.contextManager;
    }

    @Override
    public @NonNull NodeBuilderRegistry getNodeBuilderRegistry() {
        return ApiNodeBuilderRegistry.INSTANCE;
    }

    @Override
    public @NonNull QueryOptionsRegistry getQueryOptionsRegistry() {
        return ApiQueryOptionsRegistry.INSTANCE;
    }

    @Override
    public @NonNull MetaStackFactory getMetaStackFactory() {
        return this.metaStackFactory;
    }

    @Override
    public @NonNull NodeMatcherFactory getNodeMatcherFactory() {
        return ApiNodeMatcherFactory.INSTANCE;
    }

}
