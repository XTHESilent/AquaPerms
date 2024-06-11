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

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.xthesilent.aquaperms.common.command.abstraction.Command;
import com.xthesilent.aquaperms.common.command.abstraction.ParentCommand;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.StorageAssistant;
import com.xthesilent.aquaperms.common.commands.generic.meta.CommandMeta;
import com.xthesilent.aquaperms.common.commands.generic.other.HolderClear;
import com.xthesilent.aquaperms.common.commands.generic.other.HolderEditor;
import com.xthesilent.aquaperms.common.commands.generic.other.HolderShowTracks;
import com.xthesilent.aquaperms.common.commands.generic.parent.CommandParent;
import com.xthesilent.aquaperms.common.commands.generic.permission.CommandPermission;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class GroupParentCommand extends ParentCommand<Group, String> {

    // we use a lock per unique group
    // this helps prevent race conditions where commands are being executed concurrently
    // and overriding each other.
    // it's not a great solution, but it mostly works.
    private final LoadingCache<String, ReentrantLock> locks = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> new ReentrantLock());

    public GroupParentCommand() {
        super(CommandSpec.GROUP, "Group", Type.TAKES_ARGUMENT_FOR_TARGET, ImmutableList.<Command<Group>>builder()
                .add(new GroupInfo())
                .add(new CommandPermission<>(HolderType.GROUP))
                .add(new CommandParent<>(HolderType.GROUP))
                .add(new CommandMeta<>(HolderType.GROUP))
                .add(new HolderEditor<>(HolderType.GROUP))
                .add(new GroupListMembers())
                .add(new GroupSetWeight())
                .add(new GroupSetDisplayName())
                .add(new HolderShowTracks<>(HolderType.GROUP))
                .add(new HolderClear<>(HolderType.GROUP))
                .add(new GroupRename())
                .add(new GroupClone())
                .build()
        );
    }

    @Override
    protected String parseTarget(String target, AquaPermsPlugin plugin, Sender sender) {
        Group group = plugin.getGroupManager().getByDisplayName(target);
        if (group != null) {
            return group.getName();
        } else {
            return target.toLowerCase(Locale.ROOT);
        }
    }

    @Override
    protected Group getTarget(String target, AquaPermsPlugin plugin, Sender sender) {
        return StorageAssistant.loadGroup(target, sender, plugin, true);
    }

    @Override
    protected ReentrantLock getLockForTarget(String target) {
        return this.locks.get(target);
    }

    @Override
    protected void cleanup(Group group, AquaPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(AquaPermsPlugin plugin) {
        return new ArrayList<>(plugin.getGroupManager().getAll().keySet());
    }
}
