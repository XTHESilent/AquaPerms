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

import com.xthesilent.aquaperms.common.cacheddata.result.TristateResult;
import com.xthesilent.aquaperms.common.calculator.processor.WildcardProcessor;
import com.xthesilent.aquaperms.common.command.abstraction.CommandException;
import com.xthesilent.aquaperms.common.command.abstraction.GenericChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompletions;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.AbstractNode;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.model.PermissionHolder.Identifier;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.metadata.types.InheritanceOriginMetadata;
import com.aquasplashmc.api.node.types.PermissionNode;
import com.aquasplashmc.api.node.types.RegexPermissionNode;
import com.aquasplashmc.api.query.QueryOptions;
import com.aquasplashmc.api.util.Tristate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PermissionCheck extends GenericChildCommand {
    public PermissionCheck() {
        super(CommandSpec.PERMISSION_CHECK, "check", CommandPermission.USER_PERM_CHECK, CommandPermission.GROUP_PERM_CHECK, Predicates.is(0));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String node = args.get(0);
        if (node.isEmpty()) {
            Message.INVALID_PERMISSION_EMPTY.send(sender);
            return;
        }

        // accumulate nodes
        List<Node> own = new ArrayList<>();
        List<Node> inherited = new ArrayList<>();
        List<Node> wildcards = new ArrayList<>();

        List<Node> resolved = target.resolveInheritedNodes(QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL);
        for (Node n : resolved) {
            if (matches(node, n, plugin)) {
                if (isInherited(n, target)) {
                    inherited.add(n);
                } else {
                    own.add(n);
                }
            }
            if (matchesWildcard(node, n, plugin)) {
                wildcards.add(n);
            }
        }

        // send results
        Message.PERMISSION_CHECK_INFO_HEADER.send(sender, node);
        if (own.isEmpty()) {
            Message.PERMISSION_CHECK_INFO_NOT_DIRECTLY.send(sender, target, node);
        } else {
            for (Node n : own) {
                Message.PERMISSION_CHECK_INFO_DIRECTLY.send(sender, target, n.getKey(), Tristate.of(n.getValue()), n.getContexts());
            }
        }
        if (inherited.isEmpty()) {
            Message.PERMISSION_CHECK_INFO_NOT_INHERITED.send(sender, target, node);
        } else {
            for (Node n : inherited) {
                String origin = n.metadata(InheritanceOriginMetadata.KEY).getOrigin().getName();
                Message.PERMISSION_CHECK_INFO_INHERITED.send(sender, target, n.getKey(), Tristate.of(n.getValue()), n.getContexts(), origin);
            }
        }
        for (Node n : wildcards) {
            if (isInherited(n, target)) {
                String origin = n.metadata(InheritanceOriginMetadata.KEY).getOrigin().getName();
                Message.PERMISSION_CHECK_INFO_INHERITED.send(sender, target, n.getKey(), Tristate.of(n.getValue()), n.getContexts(), origin);
            } else {
                Message.PERMISSION_CHECK_INFO_DIRECTLY.send(sender, target, n.getKey(), Tristate.of(n.getValue()), n.getContexts());
            }
        }

        // blank line
        sender.sendMessage(Message.prefixed(Component.empty()));

        // perform a "real" check
        QueryOptions queryOptions = target.getQueryOptions();
        TristateResult checkResult = target.getCachedData().getPermissionData(queryOptions).checkPermission(node, CheckOrigin.INTERNAL);

        Tristate result = checkResult.result();
        String processor = checkResult.processorClassFriendly();
        Node cause = checkResult.node();
        ImmutableContextSet context = queryOptions.context();

        // send results
        Message.PERMISSION_CHECK_RESULT.send(sender, node, result, processor, cause, context);
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.permissions(plugin))
                .complete(args);
    }

    private static boolean isInherited(Node n, PermissionHolder target) {
        Identifier origin = n.getMetadata(InheritanceOriginMetadata.KEY)
                .map(InheritanceOriginMetadata::getOrigin)
                .orElse(null);

        return origin != null && !target.getIdentifier().equals(origin);
    }

    private static boolean matchesWildcard(String permission, Node node, AquaPermsPlugin plugin) {
        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            if (node instanceof PermissionNode && ((PermissionNode) node).isWildcard()) {
                String key = node.getKey();
                if (WildcardProcessor.isRootWildcard(key)) {
                    return true;
                } else {
                    // aquaperms.* becomes aquaperms.
                    String wildcardBody = key.substring(0, key.length() - 1);
                    if (permission.startsWith(wildcardBody)) {
                        return true;
                    }
                }
            }
        }

        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS_SPONGE)) {
            String key = node.getKey();

            int endIndex = key.lastIndexOf(AbstractNode.NODE_SEPARATOR);
            if (endIndex > 0) {
                String wildcardBody = key.substring(0, endIndex);
                if (permission.startsWith(wildcardBody)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean matches(String permission, Node node, AquaPermsPlugin plugin) {
        if (node.getKey().equals(permission)) {
            return true;
        }

        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND)) {
            if (node.resolveShorthand().contains(permission)) {
                return true;
            }
        }

        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            if (node instanceof RegexPermissionNode) {
                Pattern pattern = ((RegexPermissionNode) node).getPattern().orElse(null);
                if (pattern != null && pattern.matcher(permission).matches()) {
                    return true;
                }
            }
        }

        return false;
    }
}
