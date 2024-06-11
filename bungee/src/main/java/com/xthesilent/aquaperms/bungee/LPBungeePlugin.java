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

package com.xthesilent.aquaperms.bungee;

import com.xthesilent.aquaperms.bungee.calculator.BungeeCalculatorFactory;
import com.xthesilent.aquaperms.bungee.context.BungeeContextManager;
import com.xthesilent.aquaperms.bungee.context.BungeePlayerCalculator;
import com.xthesilent.aquaperms.bungee.context.RedisBungeeCalculator;
import com.xthesilent.aquaperms.bungee.listeners.BungeeConnectionListener;
import com.xthesilent.aquaperms.bungee.listeners.BungeePermissionCheckListener;
import com.xthesilent.aquaperms.bungee.messaging.BungeeMessagingFactory;
import com.xthesilent.aquaperms.common.api.AquaPermsApiProvider;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.command.CommandManager;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.config.generic.adapter.ConfigurationAdapter;
import com.xthesilent.aquaperms.common.dependencies.Dependency;
import com.xthesilent.aquaperms.common.event.AbstractEventBus;
import com.xthesilent.aquaperms.common.messaging.MessagingFactory;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.model.manager.group.StandardGroupManager;
import com.xthesilent.aquaperms.common.model.manager.track.StandardTrackManager;
import com.xthesilent.aquaperms.common.model.manager.user.StandardUserManager;
import com.xthesilent.aquaperms.common.plugin.AbstractAquaPermsPlugin;
import com.xthesilent.aquaperms.common.plugin.util.AbstractConnectionListener;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.aquasplashmc.api.AquaPerms;
import com.aquasplashmc.api.context.DefaultContextKeys;
import com.aquasplashmc.api.query.QueryOptions;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * AquaPerms implementation for the BungeeCord API.
 */
public class LPBungeePlugin extends AbstractAquaPermsPlugin {
    private final LPBungeeBootstrap bootstrap;

    private BungeeSenderFactory senderFactory;
    private BungeeConnectionListener connectionListener;
    private CommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private BungeeContextManager contextManager;

    public LPBungeePlugin(LPBungeeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPBungeeBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public Plugin getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new BungeeSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        dependencies.add(Dependency.ADVENTURE_PLATFORM);
        dependencies.add(Dependency.ADVENTURE_PLATFORM_BUNGEECORD);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new BungeeConfigAdapter(this, resolveConfig("config.yml").toFile());
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new BungeeConnectionListener(this);
        this.bootstrap.getProxy().getPluginManager().registerListener(this.bootstrap.getLoader(), this.connectionListener);
        this.bootstrap.getProxy().getPluginManager().registerListener(this.bootstrap.getLoader(), new BungeePermissionCheckListener(this));
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new BungeeMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new CommandManager(this);
        BungeeCommandExecutor command = new BungeeCommandExecutor(this, this.commandManager);
        command.register();

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        this.bootstrap.getProxy().getDisabledCommands().add("perms");
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new BungeeCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new BungeeContextManager(this);

        Set<String> disabledContexts = getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS);
        if (!disabledContexts.contains(DefaultContextKeys.WORLD_KEY)) {
            BungeePlayerCalculator playerCalculator = new BungeePlayerCalculator(this);
            this.bootstrap.getProxy().getPluginManager().registerListener(this.bootstrap.getLoader(), playerCalculator);
            this.contextManager.registerCalculator(playerCalculator);
        }

        if (!disabledContexts.contains("proxy") && this.bootstrap.getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            this.contextManager.registerCalculator(new RedisBungeeCalculator());
        }
    }

    @Override
    protected void setupPlatformHooks() {

    }

    @Override
    protected AbstractEventBus<?> provideEventBus(AquaPermsApiProvider apiProvider) {
        return new BungeeEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(AquaPerms api) {
        // BungeeCord doesn't have a services manager
    }

    @Override
    protected void performFinalSetup() {

    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getProxy().getPlayers().stream().map(p -> this.senderFactory.wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.senderFactory.wrap(this.bootstrap.getProxy().getConsole());
    }

    public BungeeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public BungeeContextManager getContextManager() {
        return this.contextManager;
    }

}
