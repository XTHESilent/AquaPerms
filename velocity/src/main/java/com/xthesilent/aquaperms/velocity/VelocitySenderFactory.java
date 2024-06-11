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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.xthesilent.aquaperms.common.locale.TranslationManager;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.sender.SenderFactory;
import com.xthesilent.aquaperms.velocity.service.CompatibilityUtil;
import com.xthesilent.aquaperms.velocity.util.AdventureCompat;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.util.Tristate;

import java.util.Locale;
import java.util.UUID;

public class VelocitySenderFactory extends SenderFactory<LPVelocityPlugin, CommandSource> {
    public VelocitySenderFactory(LPVelocityPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSource source, Component message) {
        Locale locale = null;
        if (source instanceof Player) {
            locale = ((Player) source).getPlayerSettings().getLocale();
        }
        Component rendered = TranslationManager.render(message, locale);
        AdventureCompat.sendMessage(source, rendered);
    }

    @Override
    protected Tristate getPermissionValue(CommandSource source, String node) {
        return CompatibilityUtil.convertTristate(source.getPermissionValue(node));
    }

    @Override
    protected boolean hasPermission(CommandSource source, String node) {
        return source.hasPermission(node);
    }

    @Override
    protected void performCommand(CommandSource source, String command) {
        getPlugin().getBootstrap().getProxy().getCommandManager().executeAsync(source, command).join();
    }

    @Override
    protected boolean isConsole(CommandSource sender) {
        return sender instanceof ConsoleCommandSource;
    }
}
