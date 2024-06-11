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

package com.xthesilent.aquaperms.common.commands.generic.other;

import com.google.common.collect.Maps;
import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.types.InheritanceNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HolderShowTracks<T extends PermissionHolder> extends ChildCommand<T> {
    public HolderShowTracks(HolderType type) {
        super(CommandSpec.HOLDER_SHOWTRACKS, "showtracks", type == HolderType.USER ? CommandPermission.USER_SHOW_TRACKS : CommandPermission.GROUP_SHOW_TRACKS, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, T target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        try {
            plugin.getStorage().loadAllTracks().get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst loading tracks", e);
            Message.TRACKS_LOAD_ERROR.send(sender);
            return;
        }

        List<Map.Entry<Track, InheritanceNode>> lines = new ArrayList<>();

        if (target.getType() == HolderType.USER) {
            // if the holder is a user, we want to query parent groups for tracks
            Set<InheritanceNode> nodes = target.normalData().inheritanceAsList().stream()
                    .filter(Node::getValue)
                    .filter(n -> !n.hasExpiry())
                    .collect(Collectors.toSet());

            for (InheritanceNode node : nodes) {
                String groupName = node.getGroupName();
                List<Track> tracks = plugin.getTrackManager().getAll().values().stream()
                        .filter(t -> t.containsGroup(groupName))
                        .collect(Collectors.toList());

                for (Track track : tracks) {
                    lines.add(Maps.immutableEntry(track, node));
                }
            }
        } else {
            // otherwise, just lookup for the actual group
            String groupName = ((Group) target).getName();
            List<Track> tracks = plugin.getTrackManager().getAll().values().stream()
                    .filter(t -> t.containsGroup(groupName))
                    .sorted(Comparator.comparing(Track::getName))
                    .collect(Collectors.toList());

            for (Track track : tracks) {
                lines.add(Maps.immutableEntry(track, Inheritance.builder(groupName).build()));
            }
        }

        if (lines.isEmpty()) {
            Message.LIST_TRACKS_EMPTY.send(sender, target);
            return;
        }

        Message.LIST_TRACKS.send(sender, target);
        for (Map.Entry<Track, InheritanceNode> line : lines) {
            Track track = line.getKey();
            InheritanceNode node = line.getValue();
            Message.LIST_TRACKS_ENTRY.send(sender, track.getName(), node.getContexts(), Message.formatTrackPath(track.getGroups(), node.getGroupName()));
        }
    }
}
