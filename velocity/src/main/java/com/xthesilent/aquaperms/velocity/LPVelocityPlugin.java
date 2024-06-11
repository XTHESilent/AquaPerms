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
import com.xthesilent.aquaperms.velocity.calculator.VelocityCalculatorFactory;
import com.xthesilent.aquaperms.velocity.context.VelocityContextManager;
import com.xthesilent.aquaperms.velocity.context.VelocityPlayerCalculator;
import com.xthesilent.aquaperms.velocity.listeners.MonitoringPermissionCheckListener;
import com.xthesilent.aquaperms.velocity.listeners.VelocityConnectionListener;
import com.xthesilent.aquaperms.velocity.messaging.VelocityMessagingFactory;
import com.aquasplashmc.api.AquaPerms;
import com.aquasplashmc.api.context.DefaultContextKeys;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * AquaPerms implementation for the Velocity API.
 */
public class LPVelocityPlugin extends AbstractAquaPermsPlugin {
    private final LPVelocityBootstrap bootstrap;

    private VelocitySenderFactory senderFactory;
    private VelocityConnectionListener connectionListener;
    private VelocityCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private VelocityContextManager contextManager;

    public LPVelocityPlugin(LPVelocityBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPVelocityBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new VelocitySenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        // required for loading the LP config
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_YAML);
        dependencies.add(Dependency.SNAKEYAML);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new VelocityConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new VelocityConnectionListener(this);
        this.bootstrap.getProxy().getEventManager().register(this.bootstrap, this.connectionListener);
        this.bootstrap.getProxy().getEventManager().register(this.bootstrap, new MonitoringPermissionCheckListener(this));
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new VelocityMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new VelocityCommandExecutor(this);
        this.commandManager.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new VelocityCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new VelocityContextManager(this);

        Set<String> disabledContexts = getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS);
        if (!disabledContexts.contains(DefaultContextKeys.WORLD_KEY)) {
            VelocityPlayerCalculator playerCalculator = new VelocityPlayerCalculator(this);
            this.bootstrap.getProxy().getEventManager().register(this.bootstrap, playerCalculator);
            this.contextManager.registerCalculator(playerCalculator);
        }
    }

    @Override
    protected void setupPlatformHooks() {

    }

    @Override
    protected AbstractEventBus<?> provideEventBus(AquaPermsApiProvider apiProvider) {
        return new VelocityEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(AquaPerms api) {
        // Velocity doesn't have a services manager
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
                this.bootstrap.getProxy().getAllPlayers().stream().map(p -> this.senderFactory.wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.senderFactory.wrap(this.bootstrap.getProxy().getConsoleCommandSource());
    }

    public VelocitySenderFactory getSenderFactory() {
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
    public VelocityContextManager getContextManager() {
        return this.contextManager;
    }

}
