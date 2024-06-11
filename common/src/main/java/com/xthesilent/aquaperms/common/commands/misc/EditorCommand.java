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

package com.xthesilent.aquaperms.common.commands.misc;

import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.node.matcher.ConstraintNodeMatcher;
import com.xthesilent.aquaperms.common.node.matcher.StandardNodeMatchers;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.common.webeditor.WebEditorRequest;
import com.xthesilent.aquaperms.common.webeditor.WebEditorSession;
import com.aquasplashmc.api.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditorCommand extends SingleCommand {
    public EditorCommand() {
        super(CommandSpec.EDITOR, "Editor", CommandPermission.EDITOR, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        Type type = Type.ALL;
        String filter = null;

        // attempt to parse type
        String arg0 = args.getOrDefault(0, null);
        if (arg0 != null) {
            try {
                type = Type.valueOf(arg0.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // assume they meant it as a filter
                filter = arg0;
            }

            if (filter == null) {
                filter = args.getOrDefault(1, null);
            }
        }

        // run a sync task
        plugin.getSyncTaskBuffer().requestDirectly();

        // collect holders
        List<PermissionHolder> holders = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();

        if (type.includingGroups) {
            WebEditorRequest.includeMatchingGroups(holders, Predicates.alwaysTrue(), plugin);
            tracks.addAll(plugin.getTrackManager().getAll().values());
        }

        if (type.includingUsers) {
            // include all online players
            ConstraintNodeMatcher<Node> matcher = filter != null ? StandardNodeMatchers.keyStartsWith(filter) : null;
            WebEditorRequest.includeMatchingUsers(holders, matcher, type.includingOffline, plugin);
        }

        if (holders.isEmpty()) {
            Message.EDITOR_NO_MATCH.send(sender);
            return;
        }

        // remove holders which the sender doesn't have perms to view
        holders.removeIf(holder -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder) || ArgumentPermissions.checkGroup(plugin, sender, holder, ImmutableContextSetImpl.EMPTY));
        tracks.removeIf(track -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), track));

        // they don't have perms to view any of them
        if (holders.isEmpty() && tracks.isEmpty()) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Message.EDITOR_START.send(sender);

        WebEditorSession.create(holders, tracks, sender, label, plugin).open();
    }

    private enum Type {
        ALL(true, true, true),
        ONLINE(true, true, false),
        USERS(true, false, true),
        GROUPS(false, true, true);

        private final boolean includingUsers;
        private final boolean includingGroups;
        private final boolean includingOffline;

        Type(boolean includingUsers, boolean includingGroups, boolean includingOffline) {
            this.includingUsers = includingUsers;
            this.includingGroups = includingGroups;
            this.includingOffline = includingOffline;
        }
    }
}
