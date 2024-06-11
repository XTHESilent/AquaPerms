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

package com.xthesilent.aquaperms.common.commands.user;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.xthesilent.aquaperms.common.command.abstraction.Command;
import com.xthesilent.aquaperms.common.command.abstraction.ParentCommand;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.commands.generic.meta.CommandMeta;
import com.xthesilent.aquaperms.common.commands.generic.other.HolderClear;
import com.xthesilent.aquaperms.common.commands.generic.other.HolderEditor;
import com.xthesilent.aquaperms.common.commands.generic.other.HolderShowTracks;
import com.xthesilent.aquaperms.common.commands.generic.parent.CommandParent;
import com.xthesilent.aquaperms.common.commands.generic.permission.CommandPermission;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.model.UserIdentifier;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;
import com.xthesilent.aquaperms.common.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class UserParentCommand extends ParentCommand<User, UserIdentifier> {

    // we use a lock per unique user
    // this helps prevent race conditions where commands are being executed concurrently
    // and overriding each other.
    // it's not a great solution, but it mostly works.
    private final LoadingCache<UUID, ReentrantLock> locks = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> new ReentrantLock());

    public UserParentCommand() {
        super(CommandSpec.USER, "User", Type.TAKES_ARGUMENT_FOR_TARGET, ImmutableList.<Command<User>>builder()
                .add(new UserInfo())
                .add(new CommandPermission<>(HolderType.USER))
                .add(new CommandParent<>(HolderType.USER))
                .add(new CommandMeta<>(HolderType.USER))
                .add(new HolderEditor<>(HolderType.USER))
                .add(new UserPromote())
                .add(new UserDemote())
                .add(new HolderShowTracks<>(HolderType.USER))
                .add(new HolderClear<>(HolderType.USER))
                .add(new UserClone())
                .build()
        );
    }

    public static UUID parseTargetUniqueId(String target, AquaPermsPlugin plugin, Sender sender) {
        UUID parsed = Uuids.parse(target);
        if (parsed != null) {
            return parsed;
        }

        if (!plugin.testUsernameValidity(target)) {
            Message.USER_INVALID_ENTRY.send(sender, target);
            return null;
        }

        UUID lookup = plugin.lookupUniqueId(target).orElse(null);
        if (lookup == null) {
            Message.USER_NOT_FOUND.send(sender, target);
            return null;
        }

        return lookup;
    }

    @Override
    protected UserIdentifier parseTarget(String target, AquaPermsPlugin plugin, Sender sender) {
        UUID uniqueId = parseTargetUniqueId(target, plugin, sender);
        if (uniqueId == null) {
            return null;
        }

        String name = plugin.getStorage().getPlayerName(uniqueId).join();
        return UserIdentifier.of(uniqueId, name);
    }

    @Override
    protected User getTarget(UserIdentifier target, AquaPermsPlugin plugin, Sender sender) {
        User user = plugin.getStorage().loadUser(target.getUniqueId(), target.getUsername().orElse(null)).join();
        user.auditTemporaryNodes();
        return user;
    }

    @Override
    protected ReentrantLock getLockForTarget(UserIdentifier target) {
        return this.locks.get(target.getUniqueId());
    }

    @Override
    protected void cleanup(User user, AquaPermsPlugin plugin) {
        plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());
    }

    @Override
    protected List<String> getTargets(AquaPermsPlugin plugin) {
        return new ArrayList<>(plugin.getBootstrap().getPlayerList());
    }
}