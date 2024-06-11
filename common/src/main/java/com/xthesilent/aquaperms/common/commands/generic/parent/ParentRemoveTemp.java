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

package com.xthesilent.aquaperms.common.commands.generic.parent;

import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.command.abstraction.CommandException;
import com.xthesilent.aquaperms.common.command.abstraction.GenericChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompletions;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.command.utils.StorageAssistant;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.DataConstraints;
import com.xthesilent.aquaperms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.context.MutableContextSet;
import com.aquasplashmc.api.model.data.DataMutateResult;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.Node;

import java.time.Duration;
import java.util.List;

public class ParentRemoveTemp extends GenericChildCommand {
    public ParentRemoveTemp() {
        super(CommandSpec.PARENT_REMOVE_TEMP, "removetemp", CommandPermission.USER_PARENT_REMOVE_TEMP, CommandPermission.GROUP_PARENT_REMOVE_TEMP, Predicates.is(0));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String groupName = args.getLowercase(0, DataConstraints.GROUP_NAME_TEST_ALLOW_SPACE);
        Duration duration = args.getDurationOrDefault(1, null);
        int fromIndex = duration == null ? 1 : 2;
        MutableContextSet context = args.getContextOrDefault(fromIndex, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, groupName, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, groupName)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        DataMutateResult.WithMergedNode result = target.unsetNode(DataType.NORMAL, Inheritance.builder(groupName).expiry(10L).withContext(context).build(), duration);
        if (result.getResult().wasSuccessful()) {
            Node mergedNode = result.getMergedNode();
            //noinspection ConstantConditions
            if (mergedNode != null) {
                Message.UNSET_TEMP_INHERIT_SUBTRACT_SUCCESS.send(sender, target, Component.text(groupName), mergedNode.getExpiryDuration(), context, duration);

                LoggedAction.build().source(sender).target(target)
                        .description("parent", "removetemp", groupName, duration, context)
                        .build().submit(plugin, sender);
            } else {
                Message.UNSET_TEMP_INHERIT_SUCCESS.send(sender, target, Component.text(groupName), context);

                LoggedAction.build().source(sender).target(target)
                        .description("parent", "removetemp", groupName, context)
                        .build().submit(plugin, sender);
            }

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.DOES_NOT_TEMP_INHERIT.send(sender, target, Component.text(groupName), context);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
