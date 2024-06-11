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

package com.xthesilent.aquaperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.plugin.service.ServicePriority;
import cn.nukkit.utils.Config;
import com.xthesilent.aquaperms.common.api.AquaPermsApiProvider;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.config.generic.adapter.ConfigurationAdapter;
import com.xthesilent.aquaperms.common.event.AbstractEventBus;
import com.xthesilent.aquaperms.common.messaging.MessagingFactory;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.model.manager.group.StandardGroupManager;
import com.xthesilent.aquaperms.common.model.manager.track.StandardTrackManager;
import com.xthesilent.aquaperms.common.model.manager.user.StandardUserManager;
import com.xthesilent.aquaperms.common.plugin.AbstractAquaPermsPlugin;
import com.xthesilent.aquaperms.common.plugin.util.AbstractConnectionListener;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.nukkit.calculator.NukkitCalculatorFactory;
import com.xthesilent.aquaperms.nukkit.context.NukkitContextManager;
import com.xthesilent.aquaperms.nukkit.context.NukkitPlayerCalculator;
import com.xthesilent.aquaperms.nukkit.inject.PermissionDefault;
import com.xthesilent.aquaperms.nukkit.inject.permissible.AquaPermsPermissible;
import com.xthesilent.aquaperms.nukkit.inject.permissible.PermissibleInjector;
import com.xthesilent.aquaperms.nukkit.inject.permissible.PermissibleMonitoringInjector;
import com.xthesilent.aquaperms.nukkit.inject.server.InjectorDefaultsMap;
import com.xthesilent.aquaperms.nukkit.inject.server.InjectorPermissionMap;
import com.xthesilent.aquaperms.nukkit.inject.server.InjectorSubscriptionMap;
import com.xthesilent.aquaperms.nukkit.inject.server.AquaPermsDefaultsMap;
import com.xthesilent.aquaperms.nukkit.inject.server.AquaPermsPermissionMap;
import com.xthesilent.aquaperms.nukkit.inject.server.AquaPermsSubscriptionMap;
import com.xthesilent.aquaperms.nukkit.listeners.NukkitAutoOpListener;
import com.xthesilent.aquaperms.nukkit.listeners.NukkitConnectionListener;
import com.xthesilent.aquaperms.nukkit.listeners.NukkitPlatformListener;
import com.aquasplashmc.api.AquaPerms;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * AquaPerms implementation for the Nukkit API.
 */
public class LPNukkitPlugin extends AbstractAquaPermsPlugin {
    private final LPNukkitBootstrap bootstrap;

    private NukkitSenderFactory senderFactory;
    private NukkitConnectionListener connectionListener;
    private NukkitCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private NukkitContextManager contextManager;
    private AquaPermsSubscriptionMap subscriptionMap;
    private AquaPermsPermissionMap permissionMap;
    private AquaPermsDefaultsMap defaultPermissionMap;

    public LPNukkitPlugin(LPNukkitBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPNukkitBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public PluginBase getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new NukkitSenderFactory(this);
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new NukkitConfigAdapter(this, resolveConfig("config.yml").toFile());
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new NukkitConnectionListener(this);
        this.bootstrap.getServer().getPluginManager().registerEvents(this.connectionListener, this.bootstrap.getLoader());
        this.bootstrap.getServer().getPluginManager().registerEvents(new NukkitPlatformListener(this), this.bootstrap.getLoader());
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        PluginCommand<?> command = (PluginCommand<?>) this.bootstrap.getServer().getPluginCommand("aquaperms");
        this.commandManager = new NukkitCommandExecutor(this, command);
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
        return new NukkitCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new NukkitContextManager(this);

        NukkitPlayerCalculator playerCalculator = new NukkitPlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.getServer().getPluginManager().registerEvents(playerCalculator, this.bootstrap.getLoader());
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        // inject our own custom permission maps
        Runnable[] injectors = new Runnable[]{
                new InjectorSubscriptionMap(this),
                new InjectorPermissionMap(this),
                new InjectorDefaultsMap(this),
                new PermissibleMonitoringInjector(this, PermissibleMonitoringInjector.Mode.INJECT)
        };

        for (Runnable injector : injectors) {
            injector.run();

            // schedule another injection after all plugins have loaded
            // the entire pluginmanager instance is replaced by some plugins :(
            this.bootstrap.getServer().getScheduler().scheduleDelayedTask(this.bootstrap.getLoader(), injector, 1, true);
        }
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(AquaPermsApiProvider apiProvider) {
        return new NukkitEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(AquaPerms api) {
        this.bootstrap.getServer().getServiceManager().register(AquaPerms.class, api, this.bootstrap.getLoader(), ServicePriority.NORMAL);
    }

    @Override
    protected void performFinalSetup() {
        // register permissions
        PluginManager pluginManager = this.bootstrap.getServer().getPluginManager();
        PermissionDefault permDefault = getConfiguration().get(ConfigKeys.COMMANDS_ALLOW_OP) ? PermissionDefault.OP : PermissionDefault.FALSE;

        for (CommandPermission permission : CommandPermission.values()) {
            Permission bukkitPermission = new Permission(permission.getPermission(), null, permDefault.toString());
            pluginManager.removePermission(bukkitPermission);
            pluginManager.addPermission(bukkitPermission);
        }

        // remove all operators on startup if they're disabled
        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            Config ops = this.bootstrap.getServer().getOps();
            ops.getKeys(false).forEach(ops::remove);
        }

        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new NukkitAutoOpListener(this));
        }

        // Load any online users (in the case of a reload)
        for (Player player : this.bootstrap.getServer().getOnlinePlayers().values()) {
            this.bootstrap.getScheduler().executeAsync(() -> {
                try {
                    User user = this.connectionListener.loadUser(player.getUniqueId(), player.getName());
                    if (user != null) {
                        this.bootstrap.getScheduler().executeSync(() -> {
                            try {
                                AquaPermsPermissible lpPermissible = new AquaPermsPermissible(player, user, this);
                                PermissibleInjector.inject(player, lpPermissible);
                            } catch (Throwable t) {
                                getLogger().severe("Exception thrown when setting up permissions for " +
                                        player.getUniqueId() + " - " + player.getName(), t);
                            }
                        });
                    }
                } catch (Exception e) {
                    getLogger().severe("Exception occurred whilst loading data for " + player.getUniqueId() + " - " + player.getName(), e);
                }
            });
        }
    }

    @Override
    protected void removePlatformHooks() {
        // uninject from players
        for (Player player : this.bootstrap.getServer().getOnlinePlayers().values()) {
            try {
                PermissibleInjector.uninject(player, false);
            } catch (Exception e) {
                getLogger().severe("Exception thrown when unloading permissions from " +
                        player.getUniqueId() + " - " + player.getName(), e);
            }

            if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
                player.setOp(false);
            }

            final User user = getUserManager().getIfLoaded(player.getUniqueId());
            if (user != null) {
                user.getCachedData().invalidate();
                getUserManager().unload(user.getUniqueId());
            }
        }

        // uninject custom maps
        InjectorSubscriptionMap.uninject();
        InjectorPermissionMap.uninject();
        InjectorDefaultsMap.uninject();
        new PermissibleMonitoringInjector(this, PermissibleMonitoringInjector.Mode.UNINJECT).run();
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer().getOnlinePlayers().values().stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(this.bootstrap.getServer().getConsoleSender());
    }

    public NukkitSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public NukkitCommandExecutor getCommandManager() {
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
    public NukkitContextManager getContextManager() {
        return this.contextManager;
    }

    public AquaPermsSubscriptionMap getSubscriptionMap() {
        return this.subscriptionMap;
    }

    public void setSubscriptionMap(AquaPermsSubscriptionMap subscriptionMap) {
        this.subscriptionMap = subscriptionMap;
    }

    public AquaPermsPermissionMap getPermissionMap() {
        return this.permissionMap;
    }

    public void setPermissionMap(AquaPermsPermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }

    public AquaPermsDefaultsMap getDefaultPermissionMap() {
        return this.defaultPermissionMap;
    }

    public void setDefaultPermissionMap(AquaPermsDefaultsMap defaultPermissionMap) {
        this.defaultPermissionMap = defaultPermissionMap;
    }

}
