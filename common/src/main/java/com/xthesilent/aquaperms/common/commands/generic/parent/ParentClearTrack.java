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
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.DataConstraints;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.NodeType;

import java.util.List;
import java.util.Locale;

public class ParentClearTrack extends GenericChildCommand {
    public ParentClearTrack() {
        super(CommandSpec.PARENT_CLEAR_TRACK, "cleartrack", CommandPermission.USER_PARENT_CLEAR_TRACK, CommandPermission.GROUP_PARENT_CLEAR_TRACK, Predicates.is(0));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        final String trackName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.TRACK_NAME_TEST.test(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender, trackName);
            return;
        }

        Track track = StorageAssistant.loadTrack(trackName, sender, plugin);
        if (track == null) {
            return;
        }

        if (track.getSize() <= 1) {
            Message.TRACK_EMPTY.send(sender, track.getName());
            return;
        }

        int before = target.normalData().size();

        ImmutableContextSet context = args.getContextOrDefault(1, plugin).immutableCopy();

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, track.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        target.removeIf(DataType.NORMAL, context.isEmpty() ? null : context, NodeType.INHERITANCE.predicate(n -> track.containsGroup(n.getGroupName())), false);

        if (target.getType() == HolderType.USER) {
            plugin.getUserManager().giveDefaultIfNeeded((User) target);
        }

        int changed = before - target.normalData().size();
        Message.PARENT_CLEAR_TRACK_SUCCESS.send(sender, target, track.getName(), context, changed);

        LoggedAction.build().source(sender).target(target)
                .description("parent", "cleartrack", track.getName(), context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.tracks(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
