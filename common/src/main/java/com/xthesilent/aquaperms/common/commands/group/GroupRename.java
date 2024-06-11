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

import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.bulkupdate.BulkUpdate;
import com.xthesilent.aquaperms.common.bulkupdate.BulkUpdateBuilder;
import com.xthesilent.aquaperms.common.bulkupdate.action.UpdateAction;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.Constraint;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.StandardComparison;
import com.xthesilent.aquaperms.common.bulkupdate.query.Query;
import com.xthesilent.aquaperms.common.bulkupdate.query.QueryField;
import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.ArgumentPermissions;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.CompletionSupplier;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.command.utils.StorageAssistant;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.storage.misc.DataConstraints;
import com.xthesilent.aquaperms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import com.aquasplashmc.api.event.cause.CreationCause;
import com.aquasplashmc.api.event.cause.DeletionCause;
import com.aquasplashmc.api.model.data.DataType;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class GroupRename extends ChildCommand<Group> {
    public GroupRename() {
        super(CommandSpec.GROUP_RENAME, "rename", CommandPermission.GROUP_RENAME, Predicates.notInRange(1, 2));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, Group target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String newGroupName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.GROUP_NAME_TEST.test(newGroupName)) {
            Message.GROUP_INVALID_ENTRY.send(sender, newGroupName);
            return;
        }

        if (plugin.getStorage().loadGroup(newGroupName).join().isPresent()) {
            Message.ALREADY_EXISTS.send(sender, newGroupName);
            return;
        }

        Group newGroup;
        try {
            newGroup = plugin.getStorage().createAndLoadGroup(newGroupName, CreationCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst creating group", e);
            Message.CREATE_ERROR.send(sender, Component.text(newGroupName));
            return;
        }

        try {
            plugin.getStorage().deleteGroup(target, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst deleting group", e);
            Message.DELETE_ERROR.send(sender, target.getFormattedDisplayName());
            return;
        }

        newGroup.setNodes(DataType.NORMAL, target.normalData().asList(), false);

        Message.RENAME_SUCCESS.send(sender, target.getFormattedDisplayName(), newGroup.getFormattedDisplayName());

        LoggedAction.build().source(sender).target(target)
                .description("rename", newGroup.getName())
                .build().submit(plugin, sender);

        StorageAssistant.save(newGroup, sender, plugin)
                .thenCompose((v) -> {
                    if (args.remove("--update-parent-lists")) {
                        // the group is now renamed, proceed to update its representing inheritance nodes
                        BulkUpdate operation = BulkUpdateBuilder.create()
                                .trackStatistics(false)
                                .dataType(com.xthesilent.aquaperms.common.bulkupdate.DataType.ALL)
                                .action(UpdateAction.of(QueryField.PERMISSION, Inheritance.key(newGroupName)))
                                .query(Query.of(QueryField.PERMISSION, Constraint.of(StandardComparison.EQUAL, Inheritance.key(target.getName()))))
                                .build();
                        return plugin.getStorage().applyBulkUpdate(operation);
                    } else {
                        return CompletableFuture.completedFuture(v);
                    }
        }).whenCompleteAsync((v, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            }

            plugin.getSyncTaskBuffer().requestDirectly();
        }, plugin.getBootstrap().getScheduler().async());
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, CompletionSupplier.startsWith("--update-parent-lists"))
                .complete(args);
    }
}
