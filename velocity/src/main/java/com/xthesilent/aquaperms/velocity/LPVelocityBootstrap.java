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

package com.xthesilent.aquaperms.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.xthesilent.aquaperms.common.plugin.bootstrap.AquaPermsBootstrap;
import com.xthesilent.aquaperms.common.plugin.classpath.ClassPathAppender;
import com.xthesilent.aquaperms.common.plugin.logging.PluginLogger;
import com.xthesilent.aquaperms.common.plugin.logging.Slf4jPluginLogger;
import com.xthesilent.aquaperms.common.plugin.scheduler.SchedulerAdapter;
import com.aquasplashmc.api.platform.Platform;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for AquaPerms running on Velocity.
 */
@Plugin(
        id = "aquaperms",
        name = "AquaPerms",
        version = "@version@",
        authors = "Luck",
        description = "A permissions plugin",
        url = "https://aquasplashmc.ddns.net/aquaperms"
)
public class LPVelocityBootstrap implements AquaPermsBootstrap {

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

    /**
     * The plugin instance
     */
    private final LPVelocityPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    @Inject
    private ProxyServer proxy;

    @Inject
    @DataDirectory
    private Path configDirectory;

    @Inject
    public LPVelocityBootstrap(Logger logger) {
        this.logger = new Slf4jPluginLogger(logger);
        this.schedulerAdapter = new VelocitySchedulerAdapter(this);
        this.classPathAppender = new VelocityClassPathAppender(this);
        this.plugin = new LPVelocityPlugin(this);
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    // lifecycle

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisable(ProxyShutdownEvent e) {
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

    // getters for the injected velocity instances

    public ProxyServer getProxy() {
        return this.proxy;
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return "@version@";
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.VELOCITY;
    }

    @Override
    public String getServerBrand() {
        return this.proxy.getVersion().getName();
    }

    @Override
    public String getServerVersion() {
        return this.proxy.getVersion().getVersion();
    }

    @Override
    public Path getDataDirectory() {
        return this.configDirectory.toAbsolutePath();
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return this.proxy.getPlayer(uniqueId);
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return Optional.empty();
    }

    @Override
    public int getPlayerCount() {
        return this.proxy.getPlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<Player> players = this.proxy.getAllPlayers();
        List<String> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.getUsername());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        Collection<Player> players = this.proxy.getAllPlayers();
        List<UUID> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.getUniqueId());
        }
        return list;
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        Player player = this.proxy.getPlayer(uniqueId).orElse(null);
        return player != null && player.isActive();
    }
}
