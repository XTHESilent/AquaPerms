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

package com.xthesilent.aquaperms.fabric;

import com.xthesilent.aquaperms.common.api.AquaPermsApiProvider;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.config.generic.adapter.ConfigurationAdapter;
import com.xthesilent.aquaperms.common.dependencies.Dependency;
import com.xthesilent.aquaperms.common.event.AbstractEventBus;
import com.xthesilent.aquaperms.common.locale.TranslationManager;
import com.xthesilent.aquaperms.common.messaging.MessagingFactory;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.model.manager.group.StandardGroupManager;
import com.xthesilent.aquaperms.common.model.manager.track.StandardTrackManager;
import com.xthesilent.aquaperms.common.model.manager.user.StandardUserManager;
import com.xthesilent.aquaperms.common.plugin.AbstractAquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.DummyConsoleSender;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.fabric.context.FabricContextManager;
import com.xthesilent.aquaperms.fabric.context.FabricPlayerCalculator;
import com.xthesilent.aquaperms.fabric.listeners.FabricAutoOpListener;
import com.xthesilent.aquaperms.fabric.listeners.FabricCommandListUpdater;
import com.xthesilent.aquaperms.fabric.listeners.FabricConnectionListener;
import com.xthesilent.aquaperms.fabric.listeners.FabricOtherListeners;
import com.xthesilent.aquaperms.fabric.listeners.FabricPermissionsApiListener;
import com.xthesilent.aquaperms.fabric.messaging.FabricMessagingFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import com.aquasplashmc.api.AquaPerms;
import com.aquasplashmc.api.query.QueryOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorList;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class LPFabricPlugin extends AbstractAquaPermsPlugin {
    private final LPFabricBootstrap bootstrap;

    private FabricConnectionListener connectionListener;
    private FabricCommandExecutor commandManager;
    private FabricSenderFactory senderFactory;
    private FabricContextManager contextManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;

    public LPFabricPlugin(LPFabricBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPFabricBootstrap getBootstrap() {
        return this.bootstrap;
    }

    protected void registerFabricListeners() {
        // Events are registered very early on, and persist between game states
        this.connectionListener = new FabricConnectionListener(this);
        this.connectionListener.registerListeners();

        new FabricPermissionsApiListener(this).registerListeners();

        // Command registration also need to occur early, and will persist across game states as well.
        this.commandManager = new FabricCommandExecutor(this);
        this.commandManager.register();

        new FabricOtherListeners(this).registerListeners();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new FabricSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_HOCON);
        dependencies.add(Dependency.HOCON_CONFIG);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new FabricConfigAdapter(this, resolveConfig("aquaperms.conf"));
    }

    @Override
    protected void registerPlatformListeners() {
        // Too late for Fabric, registered in #registerFabricListeners
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new FabricMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        // Too late for Fabric, registered in #registerFabricListeners
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new FabricCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new FabricContextManager(this);

        FabricPlayerCalculator playerCalculator = new FabricPlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        playerCalculator.registerListeners();
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<ModContainer> provideEventBus(AquaPermsApiProvider provider) {
        return new FabricEventBus(this, provider);
    }

    @Override
    protected void registerApiOnPlatform(AquaPerms api) {
    }

    @Override
    protected void performFinalSetup() {
        // remove all operators on startup if they're disabled
        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                OperatorList operatorList = server.getPlayerManager().getOpList();
                operatorList.values().clear();
                try {
                    operatorList.save();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        }

        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new FabricAutoOpListener(this));
        }

        // register fabric command list updater
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new FabricCommandListUpdater(this));
        }
    }

    public FabricSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public FabricConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public FabricCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public FabricContextManager getContextManager() {
        return this.contextManager;
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
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer().map(MinecraftServer::getPlayerManager).map(s -> s.getPlayerList().stream().map(p -> this.senderFactory.wrap(p.getCommandSource()))).orElseGet(Stream::empty)
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.bootstrap.getServer()
                .map(s -> this.senderFactory.wrap(s.getCommandSource()))
                .orElseGet(() -> new DummyConsoleSender(this) {
                    @Override
                    public void sendMessage(Component message) {
                        LPFabricPlugin.this.bootstrap.getPluginLogger().info(PlainComponentSerializer.plain().serialize(TranslationManager.render(message)));
                    }
                });
    }

}
