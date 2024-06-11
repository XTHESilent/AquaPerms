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

import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.tabcomplete.CompletionSupplier;
import com.xthesilent.aquaperms.common.command.tabcomplete.TabCompleter;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.http.UnsuccessfulRequestException;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.common.verbose.InvalidFilterException;
import com.xthesilent.aquaperms.common.verbose.VerboseFilter;
import com.xthesilent.aquaperms.common.verbose.VerboseHandler;
import com.xthesilent.aquaperms.common.verbose.VerboseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VerboseCommand extends SingleCommand {
    public VerboseCommand() {
        super(CommandSpec.VERBOSE, "Verbose", CommandPermission.VERBOSE, Predicates.is(0));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return;
        }

        VerboseHandler verboseHandler = plugin.getVerboseHandler();
        String mode = args.get(0).toLowerCase(Locale.ROOT);

        if (mode.equals("command") || mode.equals("cmd")) {
            if (args.size() < 3) {
                sendDetailedUsage(sender, label);
                return;
            }

            String name = args.get(1);
            Sender executor;

            if (name.equals("me") || name.equals("self") || name.equalsIgnoreCase(sender.getName())) {
                executor = sender;
            } else {
                if (!CommandPermission.VERBOSE_COMMAND_OTHERS.isAuthorized(sender)) {
                    Message.COMMAND_NO_PERMISSION.send(sender);
                    return;
                }

                executor = plugin.getOnlineSenders()
                        .filter(s -> !s.isConsole())
                        .filter(s -> s.getName().equalsIgnoreCase(name))
                        .findAny()
                        .orElse(null);

                if (executor == null) {
                    Message.USER_NOT_ONLINE.send(sender, name);
                    return;
                }
            }

            String commandWithSlash = String.join(" ", args.subList(2, args.size()));
            String command = commandWithSlash.charAt(0) == '/' ? commandWithSlash.substring(1) : commandWithSlash;

            plugin.getBootstrap().getScheduler().sync().execute(() -> {
                Message.VERBOSE_ON_COMMAND.send(sender, executor.getName(), command);

                verboseHandler.registerListener(sender, VerboseFilter.acceptAll(), true);
                executor.performCommand(command);

                VerboseListener listener = verboseHandler.unregisterListener(sender);
                if (listener.getMatchedCount() == 0) {
                    Message.VERBOSE_OFF_COMMAND_NO_CHECKS.send(sender);
                } else {
                    Message.VERBOSE_OFF_COMMAND.send(sender);
                }
            });

            return;
        }

        if (mode.equals("on") || mode.equals("true") || mode.equals("record")) {
            List<String> filters = new ArrayList<>();
            if (args.size() != 1) {
                filters.addAll(args.subList(1, args.size()));
            }

            String filter = filters.isEmpty() ? "" : String.join(" ", filters);

            VerboseFilter compiledFilter;
            try {
                compiledFilter = VerboseFilter.compile(filter);
            } catch (InvalidFilterException e) {
                Message.VERBOSE_INVALID_FILTER.send(sender, filter, e.getCause().getMessage());
                return;
            }

            boolean notify = !mode.equals("record");

            verboseHandler.registerListener(sender, compiledFilter, notify);

            if (notify) {
                if (!filter.isEmpty()) {
                    Message.VERBOSE_ON_QUERY.send(sender, filter);
                } else {
                    Message.VERBOSE_ON.send(sender);
                }
            } else {
                if (!filter.isEmpty()) {
                    Message.VERBOSE_RECORDING_ON_QUERY.send(sender, filter);
                } else {
                    Message.VERBOSE_RECORDING_ON.send(sender);
                }
            }

            return;
        }

        if (mode.equals("off") || mode.equals("false") || mode.equals("paste") || mode.equals("upload")) {
            VerboseListener listener = verboseHandler.unregisterListener(sender);

            if (mode.equals("paste") || mode.equals("upload")) {
                if (listener == null) {
                    Message.VERBOSE_OFF.send(sender);
                } else {
                    Message.VERBOSE_UPLOAD_START.send(sender);

                    String id;
                    try {
                        id = listener.uploadPasteData(plugin.getBytebin());
                    } catch (UnsuccessfulRequestException e) {
                        Message.GENERIC_HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
                        return;
                    } catch (IOException e) {
                        plugin.getLogger().warn("Error uploading data to bytebin", e);
                        Message.GENERIC_HTTP_UNKNOWN_FAILURE.send(sender);
                        return;
                    }

                    String url = plugin.getConfiguration().get(ConfigKeys.VERBOSE_VIEWER_URL_PATTERN) + id;
                    Message.VERBOSE_RESULTS_URL.send(sender, url);
                    return;
                }
            } else {
                Message.VERBOSE_OFF.send(sender);
            }

            return;
        }

        sendUsage(sender, label);
    }

    @Override
    public List<String> tabComplete(AquaPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith("on", "record", "off", "upload", "paste", "command"))
                .complete(args);
    }
}
