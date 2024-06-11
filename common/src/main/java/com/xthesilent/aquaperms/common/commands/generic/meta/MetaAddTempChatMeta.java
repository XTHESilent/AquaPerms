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
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.context.MutableContextSet;
import com.aquasplashmc.api.model.data.DataMutateResult;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.model.data.TemporaryNodeMergeStrategy;
import com.aquasplashmc.api.node.ChatMetaType;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class MetaAddTempChatMeta extends GenericChildCommand {

    public static MetaAddTempChatMeta forPrefix() {
        return new MetaAddTempChatMeta(
                ChatMetaType.PREFIX,
                CommandSpec.META_ADDTEMP_PREFIX,
                "addtempprefix",
                CommandPermission.USER_META_ADD_TEMP_PREFIX,
                CommandPermission.GROUP_META_ADD_TEMP_PREFIX
        );
    }

    public static MetaAddTempChatMeta forSuffix() {
        return new MetaAddTempChatMeta(
                ChatMetaType.SUFFIX,
                CommandSpec.META_ADDTEMP_SUFFIX,
                "addtempsuffix",
                CommandPermission.USER_META_ADD_TEMP_SUFFIX,
                CommandPermission.GROUP_META_ADD_TEMP_SUFFIX
        );
    }

    private final ChatMetaType type;

    private MetaAddTempChatMeta(ChatMetaType type, CommandSpec spec, String name, CommandPermission userPermission, CommandPermission groupPermission) {
        super(spec, name, userPermission, groupPermission, Predicates.inRange(0, 2));
        this.type = type;
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int priority = args.getPriority(0);
        String meta = args.get(1);
        Duration duration = args.getDuration(2);
        TemporaryNodeMergeStrategy modifier = args.getTemporaryModifierAndRemove(3).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
        MutableContextSet context = args.getContextOrDefault(3, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        DataMutateResult.WithMergedNode result = target.setNode(DataType.NORMAL, this.type.builder(meta, priority).expiry(duration).withContext(context).build(), modifier);

        if (result.getResult().wasSuccessful()) {
            duration = result.getMergedNode().getExpiryDuration();

            Message.ADD_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, meta, priority, duration, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "addtemp" + this.type.name().toLowerCase(Locale.ROOT), priority, meta, duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.ALREADY_HAS_TEMP_CHAT_META.send(sender, target, this.type, meta, priority, context);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(3, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
