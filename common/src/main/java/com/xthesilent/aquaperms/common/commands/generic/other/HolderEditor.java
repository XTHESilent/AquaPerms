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

import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.matcher.ConstraintNodeMatcher;
import com.xthesilent.aquaperms.common.node.matcher.StandardNodeMatchers;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.common.webeditor.WebEditorRequest;
import com.xthesilent.aquaperms.common.webeditor.WebEditorSession;
import com.aquasplashmc.api.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HolderEditor<T extends PermissionHolder> extends ChildCommand<T> {
    public HolderEditor(HolderType type) {
        super(CommandSpec.HOLDER_EDITOR, "editor", type == HolderType.USER ? CommandPermission.USER_EDITOR : CommandPermission.GROUP_EDITOR, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, T target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target) || ArgumentPermissions.checkGroup(plugin, sender, target, ImmutableContextSetImpl.EMPTY)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        List<PermissionHolder> holders = new ArrayList<>();

        // also include users who are a member of the group
        if (target instanceof Group) {
            Group group = (Group) target;
            ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.key(Inheritance.key(group.getName()));
            WebEditorRequest.includeMatchingUsers(holders, matcher, true, plugin);
        }

        // include the original holder too
        holders.add(target);

        // remove holders which the sender doesn't have perms to view
        holders.removeIf(h -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), h) || ArgumentPermissions.checkGroup(plugin, sender, h, ImmutableContextSetImpl.EMPTY));

        // they don't have perms to view any of them
        if (holders.isEmpty()) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Message.EDITOR_START.send(sender);

        WebEditorSession.create(holders, Collections.emptyList(), sender, label, plugin).open();
    }

}
