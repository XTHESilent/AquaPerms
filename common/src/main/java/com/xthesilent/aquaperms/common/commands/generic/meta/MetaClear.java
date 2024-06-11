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

package com.xthesilent.aquaperms.common.commands.generic.meta;

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
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.context.MutableContextSet;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.NodeType;

import java.util.List;
import java.util.Locale;

public class MetaClear extends GenericChildCommand {
    public MetaClear() {
        super(CommandSpec.META_CLEAR, "clear", CommandPermission.USER_META_CLEAR, CommandPermission.GROUP_META_CLEAR, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        NodeType<?> type = null;
        if (!args.isEmpty()) {
            String typeId = args.get(0).toLowerCase(Locale.ROOT);
            if (typeId.equals("any") || typeId.equals("all") || typeId.equals("*")) {
                type = NodeType.META_OR_CHAT_META;
            }
            if (typeId.equals("chat") || typeId.equals("chatmeta")) {
                type = NodeType.CHAT_META;
            }
            if (typeId.equals("meta")) {
                type = NodeType.META;
            }
            if (typeId.equals("prefix") || typeId.equals("prefixes")) {
                type = NodeType.PREFIX;
            }
            if (typeId.equals("suffix") || typeId.equals("suffixes")) {
                type = NodeType.SUFFIX;
            }

            if (type != null) {
                args.remove(0);
            }
        }

        if (type == null) {
            type = NodeType.META_OR_CHAT_META;
        }

        int before = target.normalData().size();

        MutableContextSet context = args.getContextOrDefault(0, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (context.isEmpty()) {
            target.removeIf(DataType.NORMAL, null, type::matches, false);
        } else {
            target.removeIf(DataType.NORMAL, context, type::matches, false);
        }

        int changed = before - target.normalData().size();
        Message.META_CLEAR_SUCCESS.send(sender, target, type.name().toLowerCase(Locale.ROOT), context, changed);

        LoggedAction.build().source(sender).target(target)
                .description("meta", "clear", context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(0, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
