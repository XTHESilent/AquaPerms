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

package com.xthesilent.aquaperms.common.commands.generic.permission;

import com.xthesilent.aquaperms.common.command.abstraction.GenericChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.command.utils.SortMode;
import com.xthesilent.aquaperms.common.command.utils.SortType;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.comparator.NodeWithContextComparator;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Iterators;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PermissionInfo extends GenericChildCommand {
    public PermissionInfo() {
        super(CommandSpec.PERMISSION_INFO, "info", CommandPermission.USER_PERM_INFO, CommandPermission.GROUP_PERM_INFO, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int page = args.getIntOrDefault(0, 1);
        SortMode sortMode = SortMode.determine(args);

        // get the holders nodes
        List<Node> nodes = new ArrayList<>(target.normalData().asSortedSet());

        // remove irrelevant types (these are displayed in the other info commands)
        nodes.removeIf(NodeType.INHERITANCE.predicate(n -> n.getValue() && plugin.getGroupManager().isLoaded(n.getGroupName()))
                .or(NodeType.META_OR_CHAT_META.predicate()));

        // handle empty
        if (nodes.isEmpty()) {
            Message.PERMISSION_INFO_NO_DATA.send(sender, target);
            return;
        }

        // sort the list alphabetically instead
        if (sortMode.getType() == SortType.ALPHABETICALLY) {
            nodes.sort(ALPHABETICAL_NODE_COMPARATOR);
        }

        // reverse the order if necessary
        if (!sortMode.isAscending()) {
            Collections.reverse(nodes);
        }

        int pageIndex = page - 1;
        List<List<Node>> pages = Iterators.divideIterable(nodes, 19);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        List<Node> content = pages.get(pageIndex);

        // send header
        Message.PERMISSION_INFO.send(sender, target, page, pages.size(), nodes.size());

        // send content
        for (Node node : content) {
            if (node.hasExpiry()) {
                Message.PERMISSION_INFO_TEMPORARY_NODE_ENTRY.send(sender, node, target, label);
            } else {
                Message.PERMISSION_INFO_NODE_ENTRY.send(sender, node, target, label);
            }
        }
    }

    private static final Comparator<Node> ALPHABETICAL_NODE_COMPARATOR = (o1, o2) -> {
        int i = o1.getKey().compareTo(o2.getKey());
        if (i != 0) {
            return i;
        }

        // fallback to priority
        return NodeWithContextComparator.reverse().compare(o1, o2);
    };
}
