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

package com.xthesilent.aquaperms.forge.listeners;

import com.mojang.authlib.GameProfile;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.locale.TranslationManager;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.plugin.util.AbstractConnectionListener;
import com.xthesilent.aquaperms.forge.ForgeSenderFactory;
import com.xthesilent.aquaperms.forge.LPForgePlugin;
import com.xthesilent.aquaperms.forge.capabilities.UserCapabilityImpl;
import com.xthesilent.aquaperms.forge.util.AsyncConfigurationTask;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ForgeConnectionListener extends AbstractConnectionListener {
    private static final ConfigurationTask.Type USER_LOGIN_TASK_TYPE = new ConfigurationTask.Type("aquaperms:user_login");

    private final LPForgePlugin plugin;

    public ForgeConnectionListener(LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onGatherLoginConfigurationTasks(GatherLoginConfigurationTasksEvent event) {
        PacketListener packetListener = event.getConnection().getPacketListener();
        if (!(packetListener instanceof ServerConfigurationPacketListenerImpl)) {
            return;
        }

        GameProfile gameProfile = ((ServerConfigurationPacketListenerImpl) packetListener).getOwner();
        if (gameProfile == null) {
            return;
        }

        String username = gameProfile.getName();
        UUID uniqueId = gameProfile.getId();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login (sync phase) for " + uniqueId + " - " + username);
        }

        event.addTask(new AsyncConfigurationTask(this.plugin, USER_LOGIN_TASK_TYPE, ctx -> CompletableFuture.runAsync(() -> {
            onPlayerNegotiationAsync(ctx.getConnection(), uniqueId, username);
        }, this.plugin.getBootstrap().getScheduler().async())));
    }

    private void onPlayerNegotiationAsync(Connection connection, UUID uniqueId, String username) {
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login (async phase) for " + uniqueId + " - " + username);
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(uniqueId, username);
            recordConnection(uniqueId);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uniqueId + " - " + username, ex);

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                Component component = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                connection.send(new ClientboundLoginDisconnectPacket(ForgeSenderFactory.toNativeText(component)));
                connection.disconnect(ForgeSenderFactory.toNativeText(component));
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, null);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        GameProfile profile = player.getGameProfile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + profile.getId() + " - " + profile.getName());
        }

        User user = this.plugin.getUserManager().getIfLoaded(profile.getId());

        if (user == null) {
            if (!getUniqueConnections().contains(profile.getId())) {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            Component component = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLanguage());
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                player.connection.disconnect(ForgeSenderFactory.toNativeText(component));
                return;
            } else {
                player.sendSystemMessage(ForgeSenderFactory.toNativeText(component));
            }
        }

        // initialise capability
        UserCapabilityImpl userCapability = UserCapabilityImpl.get(player);
        userCapability.initialise(user, player, this.plugin.getContextManager());
        this.plugin.getContextManager().signalContextUpdate(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        handleDisconnect(player.getGameProfile().getId());
    }

}