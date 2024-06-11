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
import com.xthesilent.aquaperms.common.cacheddata.type.MetaAccumulator;
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
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
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
import java.util.OptionalInt;

public class MetaSetTempChatMeta extends GenericChildCommand {

    public static MetaSetTempChatMeta forPrefix() {
        return new MetaSetTempChatMeta(
                ChatMetaType.PREFIX,
                CommandSpec.META_SETTEMP_PREFIX,
                "settempprefix",
                CommandPermission.USER_META_SET_TEMP_PREFIX,
                CommandPermission.GROUP_META_SET_TEMP_PREFIX
        );
    }

    public static MetaSetTempChatMeta forSuffix() {
        return new MetaSetTempChatMeta(
                ChatMetaType.SUFFIX,
                CommandSpec.META_SETTEMP_SUFFIX,
                "settempsuffix",
                CommandPermission.USER_META_SET_TEMP_SUFFIX,
                CommandPermission.GROUP_META_SET_TEMP_SUFFIX
        );
    }

    private final ChatMetaType type;

    private MetaSetTempChatMeta(ChatMetaType type, CommandSpec spec, String name, CommandPermission userPermission, CommandPermission groupPermission) {
        super(spec, name, userPermission, groupPermission, Predicates.inRange(0, 1));
        this.type = type;
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int priority = args.getIntOrDefault(0, Integer.MIN_VALUE);
        String meta;
        Duration duration;
        TemporaryNodeMergeStrategy modifier;
        MutableContextSet context;

        if (priority == Integer.MIN_VALUE) {
            // priority wasn't defined, meta is at index 0, duration at index 1
            meta = args.get(0);
            duration = args.getDuration(1);
            modifier = args.getTemporaryModifierAndRemove(2).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
            context = args.getContextOrDefault(2, plugin);
        } else {
            // priority was defined, meta should be at index 1, duration at index 2
            if (args.size() <= 2) {
                sendDetailedUsage(sender);
                return;
            }

            meta = args.get(1);
            duration = args.getDuration(2);
            modifier = args.getTemporaryModifierAndRemove(3).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
            context = args.getContextOrDefault(3, plugin);
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        // remove all other prefixes/suffixes set in these contexts
        target.removeIf(DataType.NORMAL, context, this.type.nodeType()::matches, false);

        // determine the priority to set at
        if (priority == Integer.MIN_VALUE) {
            MetaAccumulator metaAccumulator = target.accumulateMeta(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(context).build());
            priority = metaAccumulator.getChatMeta(this.type).keySet().stream().mapToInt(e -> e).max().orElse(0) + 1;

            if (target instanceof Group) {
                OptionalInt weight = target.getWeight();
                if (weight.isPresent() && weight.getAsInt() > priority) {
                    priority = weight.getAsInt();
                }
            }
        }

        DataMutateResult.WithMergedNode result = target.setNode(DataType.NORMAL, this.type.builder(meta, priority).expiry(duration).withContext(context).build(), modifier);

        if (result.getResult().wasSuccessful()) {
            duration = result.getMergedNode().getExpiryDuration();

            Message.ADD_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, meta, priority, duration, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "settemp" + this.type.name().toLowerCase(Locale.ROOT), priority, meta, duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.ALREADY_HAS_TEMP_CHAT_META.send(sender, target, this.type, meta, priority, context);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
