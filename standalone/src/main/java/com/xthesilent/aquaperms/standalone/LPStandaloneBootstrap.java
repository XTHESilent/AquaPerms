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

package com.xthesilent.aquaperms.standalone;

import com.xthesilent.aquaperms.common.loader.LoaderBootstrap;
import com.xthesilent.aquaperms.common.plugin.bootstrap.BootstrappedWithLoader;
import com.xthesilent.aquaperms.common.plugin.bootstrap.AquaPermsBootstrap;
import com.xthesilent.aquaperms.common.plugin.classpath.ClassPathAppender;
import com.xthesilent.aquaperms.common.plugin.classpath.JarInJarClassPathAppender;
import com.xthesilent.aquaperms.common.plugin.logging.Log4jPluginLogger;
import com.xthesilent.aquaperms.common.plugin.logging.PluginLogger;
import com.xthesilent.aquaperms.standalone.app.AquaPermsApplication;
import com.aquasplashmc.api.platform.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for AquaPerms running as a standalone app.
 */
public class LPStandaloneBootstrap implements AquaPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    private final AquaPermsApplication loader;

    private final PluginLogger logger;
    private final StandaloneSchedulerAdapter schedulerAdapter;
    private final ClassPathAppender classPathAppender;
    private final LPStandalonePlugin plugin;

    private Instant startTime;
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPStandaloneBootstrap(AquaPermsApplication loader) {
        this.loader = loader;

        this.logger = new Log4jPluginLogger(AquaPermsApplication.LOGGER);
        this.schedulerAdapter = new StandaloneSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPStandalonePlugin(this);
    }

    // visible for testing
    protected LPStandaloneBootstrap(AquaPermsApplication loader, ClassPathAppender classPathAppender) {
        this.loader = loader;

        this.logger = new Log4jPluginLogger(AquaPermsApplication.LOGGER);
        this.schedulerAdapter = new StandaloneSchedulerAdapter(this);
        this.classPathAppender = classPathAppender;
        this.plugin = createTestPlugin();
    }

    // visible for testing
    protected LPStandalonePlugin createTestPlugin() {
        return new LPStandalonePlugin(this);
    }

    // provide adapters

    @Override
    public AquaPermsApplication getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public StandaloneSchedulerAdapter getScheduler() {
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

    @Override
    public void onEnable() {
        this.startTime = Instant.now();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Override
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

    // provide information about the plugin

    @Override
    public String getVersion() {
        return this.loader.getVersion();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.STANDALONE;
    }

    @Override
    public String getServerBrand() {
        return "standalone";
    }

    @Override
    public String getServerVersion() {
        return "n/a";
    }

    @Override
    public Path getDataDirectory() {
        return Paths.get("data").toAbsolutePath();
    }

    @Override
    public Optional<?> getPlayer(UUID uniqueId) {
        return Optional.empty();
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
        return 0;
    }

    @Override
    public Collection<String> getPlayerList() {
        return Collections.emptyList();
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return Collections.emptyList();
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
       return false;
    }

}
