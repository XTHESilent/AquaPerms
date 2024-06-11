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

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.server.ServerCommandEvent;
import com.xthesilent.aquaperms.common.command.CommandManager;
import com.xthesilent.aquaperms.common.command.utils.ArgumentTokenizer;
import com.xthesilent.aquaperms.common.sender.Sender;

import java.util.List;

public class NukkitCommandExecutor extends CommandManager implements CommandExecutor, Listener {
    private final LPNukkitPlugin plugin;
    private final PluginCommand<?> command;

    public NukkitCommandExecutor(LPNukkitPlugin plugin, PluginCommand<?> command) {
        super(plugin);
        this.plugin = plugin;
        this.command = command;
    }

    public void register() {
        this.command.setExecutor(this);
        this.plugin.getBootstrap().getServer().getPluginManager().registerEvents(this, this.plugin.getLoader());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        executeCommand(wrapped, label, arguments);
        return true;
    }

    // Support LP commands prefixed with a '/' from the console.
    @EventHandler(ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent e) {
        if (!(e.getSender() instanceof ConsoleCommandSender)) {
            return;
        }

        String buffer = e.getCommand();
        if (buffer.isEmpty() || buffer.charAt(0) != '/') {
            return;
        }

        buffer = buffer.substring(1);

        String commandLabel;
        int firstSpace = buffer.indexOf(' ');
        if (firstSpace == -1) {
            commandLabel = buffer;
        } else {
            commandLabel = buffer.substring(0, firstSpace);
        }

        Command command = this.plugin.getBootstrap().getServer().getCommandMap().getCommand(commandLabel);
        if (command != this.command) {
            return;
        }

        e.setCommand(buffer);
    }
}
