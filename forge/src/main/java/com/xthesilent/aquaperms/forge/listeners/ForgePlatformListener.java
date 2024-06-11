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

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.forge.LPForgePlugin;
import com.xthesilent.aquaperms.forge.util.BrigadierInjector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.players.ServerOpList;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.util.Locale;

public class ForgePlatformListener {
    private final LPForgePlugin plugin;

    public ForgePlatformListener(LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();

        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
                if (!(node.getNode() instanceof LiteralCommandNode)) {
                    continue;
                }

                String name = node.getNode().getName().toLowerCase(Locale.ROOT);
                if (name.equals("op") || name.equals("deop")) {
                    Message.OP_DISABLED.send(this.plugin.getSenderFactory().wrap(context.getSource()));
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        Commands commands = event.getServerResources().getCommands();
        BrigadierInjector.inject(this.plugin, commands.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            ServerOpList ops = event.getServer().getPlayerList().getOps();
            ops.getEntries().clear();
            try {
                ops.save();
            } catch (IOException ex) {
                this.plugin.getLogger().severe("Encountered an error while saving ops", ex);
            }
        }
    }

}
