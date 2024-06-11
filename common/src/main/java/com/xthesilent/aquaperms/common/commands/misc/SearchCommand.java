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

import com.google.common.collect.Maps;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.Comparison;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.Constraint;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.StandardComparison;
import com.xthesilent.aquaperms.common.cache.LoadingMap;
import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompletions;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.node.comparator.NodeEntryComparator;
import com.xthesilent.aquaperms.common.node.matcher.ConstraintNodeMatcher;
import com.xthesilent.aquaperms.common.node.matcher.StandardNodeMatchers;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.NodeEntry;
import com.xthesilent.aquaperms.common.util.Iterators;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchCommand extends SingleCommand {
    public SearchCommand() {
        super(CommandSpec.SEARCH, "Search", CommandPermission.SEARCH, Predicates.notInRange(1, 3));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        Comparison comparison = StandardComparison.parseComparison(args.get(0));
        if (comparison == null) {
            comparison = StandardComparison.EQUAL;
            args.add(0, "==");
        }

        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.of(Constraint.of(comparison, args.get(1)));
        int page = args.getIntOrDefault(2, 1);

        Message.SEARCH_SEARCHING.send(sender, matcher.toString());

        List<NodeEntry<UUID, Node>> matchedUsers = plugin.getStorage().searchUserNodes(matcher).join();
        List<NodeEntry<String, Node>> matchedGroups = plugin.getStorage().searchGroupNodes(matcher).join();

        int users = matchedUsers.size();
        int groups = matchedGroups.size();

        Message.SEARCH_RESULT.send(sender, users + groups, users, groups);

        if (!matchedUsers.isEmpty()) {
            Map<UUID, String> uuidLookups = LoadingMap.of(u -> plugin.lookupUsername(u).orElseGet(u::toString));
            sendResult(sender, matchedUsers, uuidLookups::get, Message.SEARCH_SHOWING_USERS, HolderType.USER, label, page, comparison);
        }

        if (!matchedGroups.isEmpty()) {
            sendResult(sender, matchedGroups, Function.identity(), Message.SEARCH_SHOWING_GROUPS, HolderType.GROUP, label, page, comparison);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.permissions(plugin))
                .complete(args);
    }

    private static <T extends Comparable<T>> void sendResult(Sender sender, List<NodeEntry<T, Node>> results, Function<T, String> lookupFunction, Message.Args3<Integer, Integer, Integer> headerMessage, HolderType holderType, String label, int page, Comparison comparison) {
        results = new ArrayList<>(results);
        results.sort(NodeEntryComparator.normal());

        int pageIndex = page - 1;
        List<List<NodeEntry<T, Node>>> pages = Iterators.divideIterable(results, 15);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        List<NodeEntry<T, Node>> content = pages.get(pageIndex);

        List<Map.Entry<String, NodeEntry<T, Node>>> mappedContent = content.stream()
                .map(hp -> Maps.immutableEntry(lookupFunction.apply(hp.getHolder()), hp))
                .collect(Collectors.toList());

        // send header
        headerMessage.send(sender, page, pages.size(), results.size());

        for (Map.Entry<String, NodeEntry<T, Node>> ent : mappedContent) {
            Message.SEARCH_NODE_ENTRY.send(sender, comparison != StandardComparison.EQUAL, ent.getValue().getNode(), ent.getKey(), holderType, label, sender.getPlugin());
        }
    }
}
