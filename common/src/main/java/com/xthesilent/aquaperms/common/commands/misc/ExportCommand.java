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

import com.xthesilent.aquaperms.common.backup.Exporter;
import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExportCommand extends SingleCommand {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
            .withZone(ZoneId.systemDefault());

    private final AtomicBoolean running = new AtomicBoolean(false);

    public ExportCommand() {
        super(CommandSpec.EXPORT, "Export", CommandPermission.EXPORT, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (this.running.get()) {
            Message.EXPORT_ALREADY_RUNNING.send(sender);
            return;
        }

        boolean includeUsers = !args.remove("--without-users");
        boolean includeGroups = !args.remove("--without-groups");
        boolean upload = args.remove("--upload");

        Exporter exporter;
        if (upload) {
            if (!this.running.compareAndSet(false, true)) {
                Message.EXPORT_ALREADY_RUNNING.send(sender);
                return;
            }

            exporter = new Exporter.WebUpload(plugin, sender, includeUsers, includeGroups, label);
        } else {
            Path dataDirectory = plugin.getBootstrap().getDataDirectory();
            Path path;
            if (args.isEmpty()) {
                path = dataDirectory.resolve("aquaperms-" + DATE_FORMAT.format(Instant.now()) + ".json.gz");
            } else {
                path = dataDirectory.resolve(args.get(0) + ".json.gz");
            }

            if (!path.getParent().equals(dataDirectory)) {
                Message.FILE_NOT_WITHIN_DIRECTORY.send(sender, path.toString());
                return;
            }

            if (Files.exists(path)) {
                Message.EXPORT_FILE_ALREADY_EXISTS.send(sender, path.toString());
                return;
            }

            try {
                Files.createFile(path);
            } catch (IOException e) {
                Message.EXPORT_FILE_FAILURE.send(sender);
                plugin.getLogger().warn("Error whilst writing to the file", e);
                return;
            }

            if (!Files.isWritable(path)) {
                Message.EXPORT_FILE_NOT_WRITABLE.send(sender, path.toString());
                return;
            }

            if (!this.running.compareAndSet(false, true)) {
                Message.EXPORT_ALREADY_RUNNING.send(sender);
                return;
            }

            exporter = new Exporter.SaveFile(plugin, sender, path, includeUsers, includeGroups);
        }

        // Run the exporter in its own thread.
        plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                exporter.run();
            } finally {
                this.running.set(false);
            }
        });
    }

    public boolean isRunning() {
        return this.running.get();
    }

}
