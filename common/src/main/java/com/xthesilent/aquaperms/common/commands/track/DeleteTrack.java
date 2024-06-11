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
import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompletions;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.event.cause.DeletionCause;

import java.util.List;
import java.util.Locale;

public class DeleteTrack extends SingleCommand {
    public DeleteTrack() {
        super(CommandSpec.DELETE_TRACK, "DeleteTrack", CommandPermission.DELETE_TRACK, Predicates.not(1));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return;
        }

        String trackName = args.get(0).toLowerCase(Locale.ROOT);
        Track track = plugin.getStorage().loadTrack(trackName).join().orElse(null);
        if (track == null) {
            Message.TRACK_LOAD_ERROR.send(sender);
            return;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), track)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        try {
            plugin.getStorage().deleteTrack(track, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst deleting track", e);
            Message.DELETE_ERROR.send(sender, Component.text(track.getName()));
            return;
        }

        Message.DELETE_SUCCESS.send(sender, Component.text(trackName));

        LoggedAction.build().source(sender).targetName(trackName).targetType(Action.Target.Type.TRACK)
                .description("delete")
                .build().submit(plugin, sender);
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.tracks(plugin))
                .complete(args);
    }
}
