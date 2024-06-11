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

package com.xthesilent.aquaperms.common.commands.log;

import com.xthesilent.aquaperms.common.actionlog.Log;
import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Paginated;
import com.xthesilent.aquaperms.common.util.Predicates;

import java.util.List;
import java.util.UUID;

public class LogRecent extends ChildCommand<Log> {
    private static final int ENTRIES_PER_PAGE = 10;
    
    public LogRecent() {
        super(CommandSpec.LOG_RECENT, "recent", CommandPermission.LOG_RECENT, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, Log log, ArgumentList args, String label) {
        if (args.isEmpty()) {
            // No page or user
            Paginated<LoggedAction> content = new Paginated<>(log.getContent());
            showLog(content.getMaxPages(ENTRIES_PER_PAGE), false, sender, content);
            return;
        }

        int page = args.getIntOrDefault(0, Integer.MIN_VALUE);
        if (page != Integer.MIN_VALUE) {
            Paginated<LoggedAction> content = new Paginated<>(log.getContent());
            showLog(page, false, sender, content);
            return;
        }

        // User and possibly page
        UUID uuid = args.getUserTarget(0, plugin, sender);
        if (uuid == null) {
            return;
        }

        Paginated<LoggedAction> content = new Paginated<>(log.getContent(uuid));
        page = args.getIntOrDefault(1, Integer.MIN_VALUE);
        if (page != Integer.MIN_VALUE) {
            showLog(page, true, sender, content);
        } else {
            showLog(content.getMaxPages(ENTRIES_PER_PAGE), true, sender, content);
        }
    }

    private static void showLog(int page, boolean specificUser, Sender sender, Paginated<LoggedAction> log) {
        int maxPage = log.getMaxPages(ENTRIES_PER_PAGE);
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return;
        }

        List<Paginated.Entry<LoggedAction>> entries = log.getPage(page, ENTRIES_PER_PAGE);
        if (specificUser) {
            String name = entries.stream().findAny().get().value().getSource().getName();
            if (name.contains("@")) {
                name = name.split("@")[0];
            }
            Message.LOG_RECENT_BY_HEADER.send(sender, name, page, maxPage);
        } else {
            Message.LOG_RECENT_HEADER.send(sender, page, maxPage);
        }

        for (Paginated.Entry<LoggedAction> e : entries) {
            Message.LOG_ENTRY.send(sender, e.position(), e.value());
        }
    }
}
