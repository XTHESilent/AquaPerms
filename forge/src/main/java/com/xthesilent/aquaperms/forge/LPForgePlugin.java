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

package com.xthesilent.aquaperms.forge;

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
import com.xthesilent.aquaperms.forge.calculator.ForgeCalculatorFactory;
import com.xthesilent.aquaperms.forge.capabilities.UserCapabilityListener;
import com.xthesilent.aquaperms.forge.context.ForgeContextManager;
import com.xthesilent.aquaperms.forge.context.ForgePlayerCalculator;
import com.xthesilent.aquaperms.forge.listeners.ForgeAutoOpListener;
import com.xthesilent.aquaperms.forge.listeners.ForgeCommandListUpdater;
import com.xthesilent.aquaperms.forge.listeners.ForgeConnectionListener;
import com.xthesilent.aquaperms.forge.listeners.ForgePlatformListener;
import com.xthesilent.aquaperms.forge.messaging.ForgeMessagingFactory;
import com.xthesilent.aquaperms.forge.messaging.PluginMessageMessenger;
import com.xthesilent.aquaperms.forge.service.ForgePermissionHandlerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import com.aquasplashmc.api.AquaPerms;
import com.aquasplashmc.api.query.QueryOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.fml.ModContainer;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * AquaPerms implementation for Forge.
 */
public class LPForgePlugin extends AbstractAquaPermsPlugin {
    private final LPForgeBootstrap bootstrap;

    private ForgeSenderFactory senderFactory;
    private ForgeConnectionListener connectionListener;
    private ForgeCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private ForgeContextManager contextManager;

    public LPForgePlugin(LPForgeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPForgeBootstrap getBootstrap() {
        return this.bootstrap;
    }

    protected void registerEarlyListeners() {
        this.connectionListener = new ForgeConnectionListener(this);
        this.bootstrap.registerListeners(this.connectionListener);

        ForgePlatformListener platformListener = new ForgePlatformListener(this);
        this.bootstrap.registerListeners(platformListener);

        UserCapabilityListener userCapabilityListener = new UserCapabilityListener();
        this.bootstrap.registerListeners(userCapabilityListener);

        ForgePermissionHandlerListener permissionHandlerListener = new ForgePermissionHandlerListener(this);
        this.bootstrap.registerListeners(permissionHandlerListener);

        this.commandManager = new ForgeCommandExecutor(this);
        this.bootstrap.registerListeners(this.commandManager);

        PluginMessageMessenger.registerChannel();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new ForgeSenderFactory(this);
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
        return new ForgeConfigAdapter(this, resolveConfig("aquaperms.conf"));
    }

    @Override
    protected void registerPlatformListeners() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new ForgeMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new ForgeCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new ForgeContextManager(this);

        ForgePlayerCalculator playerCalculator = new ForgePlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.registerListeners(playerCalculator);
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<ModContainer> provideEventBus(AquaPermsApiProvider provider) {
        return new ForgeEventBus(this, provider);
    }

    @Override
    protected void registerApiOnPlatform(AquaPerms api) {
    }

    @Override
    protected void performFinalSetup() {
        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new ForgeAutoOpListener(this));
        }

        // register forge command list updater
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new ForgeCommandListUpdater(this));
        }
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer()
                        .map(MinecraftServer::getPlayerList)
                        .map(PlayerList::getPlayers)
                        .map(players -> players.stream().map(player -> this.senderFactory.wrap(player.createCommandSourceStack()))).orElseGet(Stream::empty)
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.bootstrap.getServer()
                .map(server -> this.senderFactory.wrap(server.createCommandSourceStack()))
                .orElseGet(() -> new DummyConsoleSender(this) {
                    @Override
                    public void sendMessage(Component message) {
                        LPForgePlugin.this.bootstrap.getPluginLogger().info(PlainTextComponentSerializer.plainText().serialize(TranslationManager.render(message)));
                    }
                });
    }

    public ForgeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public ForgeConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public ForgeCommandExecutor getCommandManager() {
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
    public ForgeContextManager getContextManager() {
        return this.contextManager;
    }

}
