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

package com.xthesilent.aquaperms.fabric.listeners;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.fabric.LPFabricPlugin;
import com.xthesilent.aquaperms.fabric.event.PreExecuteCommandCallback;
import net.minecraft.server.command.ServerCommandSource;

import java.util.regex.Pattern;

public class FabricOtherListeners {
    private static final Pattern OP_COMMAND_PATTERN = Pattern.compile("^/?(deop|op)( .*)?$");

    private LPFabricPlugin plugin;

    public FabricOtherListeners(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PreExecuteCommandCallback.EVENT.register(this::onPreExecuteCommand);
    }

    private boolean onPreExecuteCommand(ServerCommandSource source, String input) {
        if (input.isEmpty()) {
            return true;
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            return true;
        }

        if (OP_COMMAND_PATTERN.matcher(input).matches()) {
            Message.OP_DISABLED.send(this.plugin.getSenderFactory().wrap(source));
            return false;
        }

        return true;
    }
}
