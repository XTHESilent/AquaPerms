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

import com.github.benmanes.caffeine.cache.Cache;
import com.xthesilent.aquaperms.common.bulkupdate.BulkUpdate;
import com.xthesilent.aquaperms.common.bulkupdate.BulkUpdateBuilder;
import com.xthesilent.aquaperms.common.bulkupdate.BulkUpdateStatistics;
import com.xthesilent.aquaperms.common.bulkupdate.DataType;
import com.xthesilent.aquaperms.common.bulkupdate.action.DeleteAction;
import com.xthesilent.aquaperms.common.bulkupdate.action.UpdateAction;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.Comparison;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.Constraint;
import com.xthesilent.aquaperms.common.bulkupdate.comparison.StandardComparison;
import com.xthesilent.aquaperms.common.bulkupdate.query.Query;
import com.xthesilent.aquaperms.common.bulkupdate.query.QueryField;
import com.xthesilent.aquaperms.common.command.abstraction.CommandException;
import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentException;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.CaffeineFactory;
import com.xthesilent.aquaperms.common.util.Predicates;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BulkUpdateCommand extends SingleCommand {
    private final Cache<String, BulkUpdate> pendingOperations = CaffeineFactory.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public BulkUpdateCommand() {
        super(CommandSpec.BULK_UPDATE, "BulkUpdate", CommandPermission.BULK_UPDATE, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) throws CommandException {
        if (plugin.getConfiguration().get(ConfigKeys.DISABLE_BULKUPDATE)) {
            Message.BULK_UPDATE_DISABLED.send(sender);
            return;
        }

        if (!sender.isConsole()) {
            Message.BULK_UPDATE_MUST_USE_CONSOLE.send(sender);
            return;
        }

        if (args.size() == 2 && args.get(0).equalsIgnoreCase("confirm")) {
            String id = args.get(1);
            BulkUpdate operation = this.pendingOperations.asMap().remove(id);

            if (operation == null) {
                Message.BULK_UPDATE_UNKNOWN_ID.send(sender, id);
                return;
            }

            runOperation(operation, plugin, sender);
            return;
        }

        if (args.size() < 2) {
            throw new ArgumentException.DetailedUsage();
        }

        BulkUpdateBuilder bulkUpdateBuilder = BulkUpdateBuilder.create();

        bulkUpdateBuilder.trackStatistics(!args.remove("-s"));

        try {
            bulkUpdateBuilder.dataType(DataType.valueOf(args.remove(0).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            Message.BULK_UPDATE_INVALID_DATA_TYPE.send(sender);
            return;
        }

        String action = args.remove(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "delete":
                bulkUpdateBuilder.action(DeleteAction.create());
                break;
            case "update":
                if (args.size() < 2) {
                    throw new ArgumentException.DetailedUsage();
                }

                String field = args.remove(0);
                QueryField queryField = QueryField.of(field);
                if (queryField == null) {
                    throw new ArgumentException.DetailedUsage();
                }
                String value = args.remove(0);

                bulkUpdateBuilder.action(UpdateAction.of(queryField, value));
                break;
            default:
                throw new ArgumentException.DetailedUsage();
        }

        for (String constraint : args) {
            String[] parts = constraint.split(" ");
            if (parts.length != 3) {
                Message.BULK_UPDATE_INVALID_CONSTRAINT.send(sender, constraint);
                return;
            }

            QueryField field = QueryField.of(parts[0]);
            if (field == null) {
                Message.BULK_UPDATE_INVALID_CONSTRAINT.send(sender, constraint);
                return;
            }

            Comparison comparison = StandardComparison.parseComparison(parts[1]);
            if (comparison == null) {
                Message.BULK_UPDATE_INVALID_COMPARISON.send(sender, parts[1]);
                return;
            }

            String expr = parts[2];
            bulkUpdateBuilder.query(Query.of(field, Constraint.of(comparison, expr)));
        }

        BulkUpdate bulkUpdate = bulkUpdateBuilder.build();

        if (plugin.getConfiguration().get(ConfigKeys.SKIP_BULKUPDATE_CONFIRMATION)) {
            runOperation(bulkUpdate, plugin, sender);
        } else {
            String id = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            this.pendingOperations.put(id, bulkUpdate);

            Message.BULK_UPDATE_QUEUED.send(sender, bulkUpdate.buildAsSql().toReadableString().replace("{table}", bulkUpdate.getDataType().getName()));
            Message.BULK_UPDATE_CONFIRM.send(sender, label, id);
        }
    }

    private static void runOperation(BulkUpdate operation, AquaPermsPlugin plugin, Sender sender) {
        Message.BULK_UPDATE_STARTING.send(sender);
        plugin.getStorage().applyBulkUpdate(operation).whenCompleteAsync((v, ex) -> {
            if (ex == null) {
                plugin.getSyncTaskBuffer().requestDirectly();
                Message.BULK_UPDATE_SUCCESS.send(sender);
                if (operation.isTrackingStatistics()) {
                    BulkUpdateStatistics stats = operation.getStatistics();
                    Message.BULK_UPDATE_STATISTICS.send(sender, stats.getAffectedNodes(), stats.getAffectedUsers(), stats.getAffectedGroups());
                }
            } else {
                ex.printStackTrace();
                Message.BULK_UPDATE_FAILURE.send(sender);
            }
        }, plugin.getBootstrap().getScheduler().async());
    }
}