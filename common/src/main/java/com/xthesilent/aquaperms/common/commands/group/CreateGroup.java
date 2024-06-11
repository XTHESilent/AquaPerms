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

package com.xthesilent.aquaperms.common.commands.group;

import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentException;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.command.utils.StorageAssistant;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.node.types.DisplayName;
import com.xthesilent.aquaperms.common.node.types.Weight;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.DataConstraints;
import com.xthesilent.aquaperms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.event.cause.CreationCause;
import com.aquasplashmc.api.model.data.DataType;

import java.util.Locale;

public class CreateGroup extends SingleCommand {
    public CreateGroup() {
        super(CommandSpec.CREATE_GROUP, "CreateGroup", CommandPermission.CREATE_GROUP, Predicates.notInRange(1, 3));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return;
        }

        String groupName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            Message.GROUP_INVALID_ENTRY.send(sender, groupName);
            return;
        }

        if (plugin.getStorage().loadGroup(groupName).join().isPresent()) {
            Message.ALREADY_EXISTS.send(sender, groupName);
            return;
        }

        Integer weight = null;
        try {
            weight = args.getPriority(1);
        } catch (ArgumentException | IndexOutOfBoundsException e) {
            // ignored
        }

        String displayName = null;
        try {
            displayName = args.get(weight != null ? 2 : 1);
        } catch (IndexOutOfBoundsException e) {
            // ignored
        }

        try {
            Group group = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.COMMAND).get();

            if (weight != null) {
                group.setNode(DataType.NORMAL, Weight.builder(weight).build(), false);
            }

            if (displayName != null) {
                group.setNode(DataType.NORMAL, DisplayName.builder(displayName).build(), false);
            }

            StorageAssistant.save(group, sender, plugin);
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst creating group", e);
            Message.CREATE_ERROR.send(sender, Component.text(groupName));
            return;
        }

        Message.CREATE_SUCCESS.send(sender, Component.text(groupName));

        LoggedAction.build().source(sender).targetName(groupName).targetType(Action.Target.Type.GROUP)
                .description("create")
                .build().submit(plugin, sender);
    }
}
