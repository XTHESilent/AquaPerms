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
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.model.data.DataMutateResult;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.ChatMetaType;

import java.util.List;
import java.util.Locale;

public class MetaRemoveTempChatMeta extends GenericChildCommand {

    public static MetaRemoveTempChatMeta forPrefix() {
        return new MetaRemoveTempChatMeta(
                ChatMetaType.PREFIX,
                CommandSpec.META_REMOVETEMP_PREFIX,
                "removetempprefix",
                CommandPermission.USER_META_REMOVE_TEMP_PREFIX,
                CommandPermission.GROUP_META_REMOVE_TEMP_PREFIX
        );
    }

    public static MetaRemoveTempChatMeta forSuffix() {
        return new MetaRemoveTempChatMeta(
                ChatMetaType.SUFFIX,
                CommandSpec.META_REMOVETEMP_SUFFIX,
                "removetempsuffix",
                CommandPermission.USER_META_REMOVE_TEMP_SUFFIX,
                CommandPermission.GROUP_META_REMOVE_TEMP_SUFFIX
        );
    }

    private final ChatMetaType type;

    private MetaRemoveTempChatMeta(ChatMetaType type, CommandSpec spec, String name, CommandPermission userPermission, CommandPermission groupPermission) {
        super(spec, name, userPermission, groupPermission, Predicates.is(0));
        this.type = type;
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int priority = args.getPriority(0);
        String meta = args.getOrDefault(1, "null");
        ImmutableContextSet context = args.getContextOrDefault(2, plugin).immutableCopy();

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        // Handle bulk removal
        if (meta.equalsIgnoreCase("null") || meta.equals("*")) {
            target.removeIf(DataType.NORMAL, context, this.type.nodeType().predicate(n -> n.getPriority() == priority && n.hasExpiry()), false);
            Message.BULK_REMOVE_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, priority, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "removetemp" + this.type.name().toLowerCase(Locale.ROOT), priority, "*", context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
            return;
        }

        DataMutateResult result = target.unsetNode(DataType.NORMAL, this.type.builder(meta, priority).expiry(10L).withContext(context).build());

        if (result.wasSuccessful()) {
            Message.REMOVE_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, meta, priority, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "removetemp" + this.type.name().toLowerCase(Locale.ROOT), priority, meta, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.DOES_NOT_HAVE_TEMP_CHAT_META.send(sender, target, this.type, meta, priority, context);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
