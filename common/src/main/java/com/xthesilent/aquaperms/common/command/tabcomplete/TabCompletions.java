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

package com.xthesilent.aquaperms.common.command.tabcomplete;

import com.google.common.base.Splitter;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.treeview.PermissionRegistry;
import com.xthesilent.aquaperms.common.treeview.TreeNode;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.context.ImmutableContextSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Common completion suppliers used by the plugin
 */
public final class TabCompletions {

    private static final CompletionSupplier BOOLEAN = CompletionSupplier.startsWith("true", "false");

    private final CompletionSupplier groups;
    private final CompletionSupplier tracks;
    private final CompletionSupplier permissions;
    private final CompletionSupplier contexts;

    public TabCompletions(AquaPermsPlugin plugin) {
        this.groups = CompletionSupplier.startsWith(() -> plugin.getGroupManager().getAll().keySet().stream());
        this.tracks = CompletionSupplier.startsWith(() -> plugin.getTrackManager().getAll().keySet().stream());
        this.permissions = partial -> {
            PermissionRegistry cache = plugin.getPermissionRegistry();

            String start = partial.toLowerCase(Locale.ROOT);
            List<String> parts = new ArrayList<>(Splitter.on('.').splitToList(start));
            TreeNode root = cache.getRootNode();

            if (parts.size() <= 1) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                return CompletionSupplier.startsWith(root.getChildren().get().keySet()).supplyCompletions(start);
            }

            String incomplete = parts.remove(parts.size() - 1);

            for (String s : parts) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                TreeNode n = root.getChildren().get().get(s);
                if (n == null) {
                    return Collections.emptyList();
                }

                root = n;
            }

            if (!root.getChildren().isPresent()) {
                return Collections.emptyList();
            }

            return root.getChildren().get().keySet().stream()
                    .filter(Predicates.startsWithIgnoreCase(incomplete))
                    .map(s -> String.join(".", parts) + "." + s)
                    .collect(Collectors.toList());
        };
        this.contexts = partial -> {
            ImmutableContextSet potentialContexts = plugin.getContextManager().getPotentialContexts();

            int index = partial.indexOf('=');
            if (index == -1) {
                // cursor is specifying the key
                return CompletionSupplier.contains(potentialContexts.toMap().keySet()).supplyCompletions(partial);
            }

            // cursor is specifying the value
            String key = partial.substring(0, index);
            if (key.isEmpty() || key.trim().isEmpty()) {
                return Collections.emptyList();
            }

            String value = partial.substring(index + 1).trim();
            Set<String> potentialValues = potentialContexts.getValues(key);
            return potentialValues.stream()
                    .filter(Predicates.startsWithIgnoreCase(value))
                    .map(s -> key + "=" + s)
                    .collect(Collectors.toList());
        };
    }

    // bit of a weird pattern, but meh it kinda works, reduces the boilerplate
    // of calling the commandmanager + tabcompletions getters every time

    public static CompletionSupplier booleans() {
        return BOOLEAN;
    }

    public static CompletionSupplier groups(AquaPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().groups;
    }

    public static CompletionSupplier tracks(AquaPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().tracks;
    }

    public static CompletionSupplier permissions(AquaPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().permissions;
    }

    public static CompletionSupplier contexts(AquaPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().contexts;
    }

}
