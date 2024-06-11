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
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.command.utils.StorageAssistant;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.DataConstraints;
import com.xthesilent.aquaperms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.event.cause.CreationCause;
import com.aquasplashmc.api.event.cause.DeletionCause;

import java.util.Locale;

public class TrackRename extends ChildCommand<Track> {
    public TrackRename() {
        super(CommandSpec.TRACK_RENAME, "rename", CommandPermission.TRACK_RENAME, Predicates.not(1));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, Track target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String newTrackName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.TRACK_NAME_TEST.test(newTrackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender, newTrackName);
            return;
        }

        if (plugin.getStorage().loadTrack(newTrackName).join().isPresent()) {
            Message.ALREADY_EXISTS.send(sender, newTrackName);
            return;
        }

        Track newTrack;
        try {
            newTrack = plugin.getStorage().createAndLoadTrack(newTrackName, CreationCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst creating track", e);
            Message.CREATE_ERROR.send(sender, Component.text(newTrackName));
            return;
        }

        try {
            plugin.getStorage().deleteTrack(target, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst deleting track", e);
            Message.DELETE_ERROR.send(sender, Component.text(target.getName()));
            return;
        }

        newTrack.setGroups(target.getGroups());

        Message.RENAME_SUCCESS.send(sender, Component.text(target.getName()), Component.text(newTrack.getName()));

        LoggedAction.build().source(sender).target(target)
                .description("rename", newTrack.getName())
                .build().submit(plugin, sender);

        StorageAssistant.save(newTrack, sender, plugin);
    }
}
