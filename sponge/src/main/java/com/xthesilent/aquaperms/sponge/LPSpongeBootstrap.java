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

package com.xthesilent.aquaperms.sponge;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.xthesilent.aquaperms.common.loader.LoaderBootstrap;
import com.xthesilent.aquaperms.common.plugin.bootstrap.BootstrappedWithLoader;
import com.xthesilent.aquaperms.common.plugin.bootstrap.AquaPermsBootstrap;
import com.xthesilent.aquaperms.common.plugin.classpath.ClassPathAppender;
import com.xthesilent.aquaperms.common.plugin.classpath.JarInJarClassPathAppender;
import com.xthesilent.aquaperms.common.plugin.logging.Log4jPluginLogger;
import com.xthesilent.aquaperms.common.plugin.logging.PluginLogger;
import com.xthesilent.aquaperms.common.util.MoreFiles;
import com.aquasplashmc.api.platform.Platform;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Bootstrap plugin for AquaPerms running on Sponge.
 */
public class LPSpongeBootstrap implements AquaPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    private final Object loader;

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SpongeSchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

    /**
     * The plugin instance
     */
    private final LPSpongePlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    /**
     * Reference to the central {@link Game} instance in the API
     */
    private final Game game;

    /**
     * Injected plugin container for the plugin
     */
    private final PluginContainer pluginContainer;

    /**
     * Injected configuration directory for the plugin
     */
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDirectory;

    public LPSpongeBootstrap(Supplier<Injector> loader) {
        this.loader = loader;

        Injector injector = loader.get();
        this.logger = new Log4jPluginLogger(injector.getInstance(Logger.class));
        this.game = injector.getInstance(Game.class);
        this.pluginContainer = injector.getInstance(PluginContainer.class);
        injector.injectMembers(this);

        this.schedulerAdapter = new SpongeSchedulerAdapter(this.game, this.pluginContainer);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPSpongePlugin(this);
    }

    // provide adapters

    @Override
    public Object getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SpongeSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    // lifecycle

    @Override
    public void onLoad() {
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
    }

    public void onEnable() {
        this.startTime = Instant.now();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    public void onDisable() {
        this.plugin.disable();
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    // getters for the injected sponge instances

    public Game getGame() {
        return this.game;
    }

    public Optional<Server> getServer() {
        return this.game.isServerAvailable() ? Optional.of(this.game.server()) : Optional.empty();
    }

    public PluginContainer getPluginContainer() {
        return this.pluginContainer;
    }

    public void registerListeners(Object obj) {
        // Check if we are running Sponge API 9+
        try {
            final Method method = org.spongepowered.api.event.EventManager.class.getDeclaredMethod("registerListeners", PluginContainer.class, Object.class, MethodHandles.Lookup.class);
            method.invoke(this.game.eventManager(), this.pluginContainer, obj, MethodHandles.lookup());
            return;
        } catch (Throwable t) {
            // ignore
        }
        // Fallback to Sponge API 8
        this.game.eventManager().registerListeners(this.pluginContainer, obj);
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return this.pluginContainer.metadata().version().toString();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.SPONGE;
    }

    @Override
    public String getServerBrand() {
        PluginMetadata brandMetadata = this.game.platform().container(Component.IMPLEMENTATION).metadata();
        return brandMetadata.name().orElseGet(brandMetadata::id);
    }

    @Override
    public String getServerVersion() {
        PluginMetadata api = this.game.platform().container(Component.API).metadata();
        PluginMetadata impl = this.game.platform().container(Component.IMPLEMENTATION).metadata();
        return api.name().orElse("API") + ": " + api.version() + " - " + impl.name().orElse("Impl") + ": " + impl.version();
    }
    
    @Override
    public Path getDataDirectory() {
        Path dataDirectory = this.game.gameDirectory().toAbsolutePath().resolve("aquaperms");
        try {
            MoreFiles.createDirectoriesIfNotExists(dataDirectory);
        } catch (IOException e) {
            this.logger.warn("Unable to create AquaPerms directory", e);
        }
        return dataDirectory;
    }

    @Override
    public Path getConfigDirectory() {
        return this.configDirectory.toAbsolutePath();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public Optional<ServerPlayer> getPlayer(UUID uniqueId) {
        return getServer().flatMap(s -> s.player(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return getServer().flatMap(server -> server.gameProfileManager().profile(username)
                .thenApply(p -> Optional.of(p.uniqueId()))
                .exceptionally(x -> Optional.empty())
                .join()
        );
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getServer().flatMap(server -> server.gameProfileManager().profile(uniqueId)
                .thenApply(GameProfile::name)
                .exceptionally(x -> Optional.empty())
                .join()
        );
    }

    @Override
    public int getPlayerCount() {
        return getServer().map(server -> server.onlinePlayers().size()).orElse(0);
    }

    @Override
    public Collection<String> getPlayerList() {
        return getServer().map(server -> {
            Collection<ServerPlayer> players = server.onlinePlayers();
            List<String> list = new ArrayList<>(players.size());
            for (Player player : players) {
                list.add(player.name());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return getServer().map(server -> {
            Collection<ServerPlayer> players = server.onlinePlayers();
            List<UUID> list = new ArrayList<>(players.size());
            for (Player player : players) {
                list.add(player.uniqueId());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return getServer().map(server -> server.player(uniqueId).isPresent()).orElse(false);
    }
    
}
