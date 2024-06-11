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

package com.xthesilent.aquaperms.common.commands.misc;

import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.messaging.InternalMessagingService;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;

import java.util.Optional;

public class NetworkSyncCommand extends SingleCommand {
    public NetworkSyncCommand() {
        super(CommandSpec.NETWORK_SYNC, "NetworkSync", CommandPermission.SYNC, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        Message.UPDATE_TASK_REQUEST.send(sender);
        plugin.getSyncTaskBuffer().request().join();
        Message.UPDATE_TASK_COMPLETE_NETWORK.send(sender);

        Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
        if (!messagingService.isPresent()) {
            Message.UPDATE_TASK_PUSH_FAILURE_NOT_SETUP.send(sender);
            return;
        }

        try {
            messagingService.get().pushUpdate();
            Message.UPDATE_TASK_PUSH_SUCCESS.send(sender, messagingService.get().getName());
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst pushing changes to other servers", e);
            Message.UPDATE_TASK_PUSH_FAILURE.send(sender);
        }
    }
}