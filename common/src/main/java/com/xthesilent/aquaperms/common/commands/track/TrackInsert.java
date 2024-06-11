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

package com.xthesilent.aquaperms.common.commands.track;

import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompletions;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.command.utils.StorageAssistant;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.DataConstraints;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.model.data.DataMutateResult;

import java.util.List;
import java.util.Locale;

public class TrackInsert extends ChildCommand<Track> {
    public TrackInsert() {
        super(CommandSpec.TRACK_INSERT, "insert", CommandPermission.TRACK_INSERT, Predicates.not(2));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, Track target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String groupName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            sendDetailedUsage(sender, label);
            return;
        }

        int pos;
        try {
            pos = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            Message.TRACK_INSERT_ERROR_NUMBER.send(sender, args.get(1));
            return;
        }

        Group group = StorageAssistant.loadGroup(groupName, sender, plugin, false);
        if (group == null) {
            return;
        }

        try {
            DataMutateResult result = target.insertGroup(group, pos - 1);

            if (result.wasSuccessful()) {
                Message.TRACK_INSERT_SUCCESS.send(sender, group.getName(), target.getName(), pos);
                if (target.getGroups().size() > 1) {
                    Message.TRACK_PATH_HIGHLIGHTED.send(sender, target.getGroups(), group.getName());
                }

                LoggedAction.build().source(sender).target(target)
                        .description("insert", group.getName(), pos)
                        .build().submit(plugin, sender);

                StorageAssistant.save(target, sender, plugin);
            } else {
                Message.TRACK_ALREADY_CONTAINS.send(sender, target.getName(), group);
            }

        } catch (IndexOutOfBoundsException e) {
            Message.TRACK_INSERT_ERROR_INVALID_POS.send(sender, pos);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .complete(args);
    }
}
