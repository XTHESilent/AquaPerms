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

package com.xthesilent.aquaperms.common.command.abstraction;

import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.CompletionSupplier;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A shared main command. Shared meaning it can apply to both users and groups.
 * This extends sub command as they're actually sub commands of the main user/group commands.
 * @param <T>
 */
public class GenericParentCommand<T extends PermissionHolder> extends ChildCommand<T> {

    private final List<GenericChildCommand> children;

    private final HolderType type;

    public GenericParentCommand(CommandSpec spec, String name, HolderType type, List<GenericChildCommand> children) {
        super(spec, name, null, Predicates.alwaysFalse());
        this.children = children;
        this.type = type;
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, T holder, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsageDetailed(sender, label);
            return;
        }

        GenericChildCommand sub = this.children.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .findFirst()
                .orElse(null);

        if (sub == null) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return;
        }

        if (!sub.isAuthorized(sender, this.type)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (sub.getArgumentCheck().test(args.size() - 1)) {
            sub.sendDetailedUsage(sender);
            return;
        }

        try {
            sub.execute(plugin, sender, holder, args.subList(1, args.size()), label, this.type == HolderType.USER ? sub.getUserPermission() : sub.getGroupPermission());
        } catch (CommandException e) {
            e.handle(sender, sub);
        }
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith(() -> this.children.stream()
                        .filter(s -> s.isAuthorized(sender, this.type))
                        .map(s -> s.getName().toLowerCase(Locale.ROOT))
                ))
                .from(1, partial -> this.children.stream()
                        .filter(s -> s.isAuthorized(sender, this.type))
                        .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                        .findFirst()
                        .map(cmd -> cmd.tabComplete(plugin, sender, args.subList(1, args.size())))
                        .orElse(Collections.emptyList())
                )
                .complete(args);
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return this.children.stream().anyMatch(sc -> sc.isAuthorized(sender, this.type));
    }

    private void sendUsageDetailed(Sender sender, String label) {
        List<GenericChildCommand> subs = this.children.stream()
                .filter(s -> s.isAuthorized(sender, this.type))
                .collect(Collectors.toList());

        if (!subs.isEmpty()) {
            switch (this.type) {
                case USER:
                    Message.MAIN_COMMAND_USAGE_HEADER.send(sender, getName(), String.format("/%s user <user> " + getName().toLowerCase(Locale.ROOT), label));
                    break;
                case GROUP:
                    Message.MAIN_COMMAND_USAGE_HEADER.send(sender, getName(), String.format("/%s group <group> " + getName().toLowerCase(Locale.ROOT), label));
                    break;
                default:
                    throw new AssertionError(this.type);
            }

            for (GenericChildCommand s : subs) {
                s.sendUsage(sender);
            }

        } else {
            Message.COMMAND_NO_PERMISSION.send(sender);
        }
    }

}
