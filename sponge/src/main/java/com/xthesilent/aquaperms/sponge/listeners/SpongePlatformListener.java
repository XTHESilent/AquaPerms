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

package com.xthesilent.aquaperms.sponge.listeners;

import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.sponge.LPSpongePlugin;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.ExecuteCommandEvent;

import java.util.Locale;

public class SpongePlatformListener {
    private final LPSpongePlugin plugin;

    public SpongePlatformListener(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onSendCommand(ExecuteCommandEvent e) {
        CommandCause source = e.commandCause();

        final String name = e.command().toLowerCase(Locale.ROOT);
        if (((name.equals("op") || name.equals("minecraft:op")) && source.hasPermission("minecraft.command.op")) || ((name.equals("deop") || name.equals("minecraft:deop")) && source.hasPermission("minecraft.command.deop"))) {
            Message.OP_DISABLED_SPONGE.send(this.plugin.getSenderFactory().wrap(source.audience()));
        }
    }
}
