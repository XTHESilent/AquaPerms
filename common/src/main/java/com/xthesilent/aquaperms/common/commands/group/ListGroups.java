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

package com.xthesilent.aquaperms.common.commands.group;

import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Iterators;
import com.xthesilent.aquaperms.common.util.Predicates;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ListGroups extends SingleCommand {
    public ListGroups() {
        super(CommandSpec.LIST_GROUPS, "ListGroups", CommandPermission.LIST_GROUPS, Predicates.notInRange(0, 1));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        try {
            plugin.getStorage().loadAllGroups().get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst loading groups", e);
            Message.GROUPS_LOAD_ERROR.send(sender);
            return;
        }

        int page = args.getIntOrDefault(0, 1);
        int pageIndex = page - 1;

        List<Group> groups = plugin.getGroupManager().getAll().values().stream().sorted((o1, o2) -> {
                    int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                    return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
                }).collect(Collectors.toList());

        List<List<Group>> pages = Iterators.divideIterable(groups, 8);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        Message.SEARCH_SHOWING_GROUPS.send(sender, page, pages.size(), groups.size());
        Message.GROUPS_LIST.send(sender);

        Collection<? extends Track> allTracks = plugin.getTrackManager().getAll().values();

        for (Group group : pages.get(pageIndex)) {
            List<String> tracks = allTracks.stream().filter(t -> t.containsGroup(group)).map(Track::getName).collect(Collectors.toList());
            Message.GROUPS_LIST_ENTRY.send(sender, group, group.getWeight().orElse(0), tracks);
        }
    }
}
