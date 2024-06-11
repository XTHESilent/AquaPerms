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

package com.xthesilent.aquaperms.common.commands.generic.permission;

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
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.factory.NodeBuilders;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.context.MutableContextSet;
import com.aquasplashmc.api.model.data.DataMutateResult;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.model.data.TemporaryNodeMergeStrategy;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.types.InheritanceNode;

import java.time.Duration;
import java.util.List;

public class PermissionSetTemp extends GenericChildCommand {
    public PermissionSetTemp() {
        super(CommandSpec.PERMISSION_SETTEMP, "settemp", CommandPermission.USER_PERM_SET_TEMP, CommandPermission.GROUP_PERM_SET_TEMP, Predicates.inRange(0, 2));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String node = args.get(0);
        boolean value = args.getBooleanOrInsert(1, true);
        Duration duration = args.getDuration(2);
        TemporaryNodeMergeStrategy modifier = args.getTemporaryModifierAndRemove(3).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
        MutableContextSet context = args.getContextOrDefault(3, plugin);

        if (node.isEmpty()) {
            Message.INVALID_PERMISSION_EMPTY.send(sender);
            return;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, node)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Node builtNode = NodeBuilders.determineMostApplicable(node).value(value).withContext(context).expiry(duration).build();

        if (builtNode instanceof InheritanceNode) {
            if (ArgumentPermissions.checkGroup(plugin, sender, ((InheritanceNode) builtNode).getGroupName(), context)) {
                Message.COMMAND_NO_PERMISSION.send(sender);
                return;
            }
        }

        DataMutateResult.WithMergedNode result = target.setNode(DataType.NORMAL, builtNode, modifier);

        if (result.getResult().wasSuccessful()) {
            duration = result.getMergedNode().getExpiryDuration();
            Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, value, target, duration, context);

            LoggedAction.build().source(sender).target(target)
                    .description("permission", "settemp", node, value, duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, target, node, context);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.permissions(plugin))
                .at(1, TabCompletions.booleans())
                .from(3, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
