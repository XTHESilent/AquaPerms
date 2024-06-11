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

import com.google.common.collect.ImmutableList;
import com.xthesilent.aquaperms.common.actionlog.Log;
import com.xthesilent.aquaperms.common.command.abstraction.Command;
import com.xthesilent.aquaperms.common.command.abstraction.ParentCommand;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class LogParentCommand extends ParentCommand<Log, Void> {
    private final ReentrantLock lock = new ReentrantLock();

    public LogParentCommand() {
        super(CommandSpec.LOG, "Log", Type.NO_TARGET_ARGUMENT, ImmutableList.<Command<Log>>builder()
                .add(new LogRecent())
                .add(new LogSearch())
                .add(new LogNotify())
                .add(new LogUserHistory())
                .add(new LogGroupHistory())
                .add(new LogTrackHistory())
                .build()
        );
    }

    @Override
    protected ReentrantLock getLockForTarget(Void target) {
        return this.lock; // all commands target the same log, so we share a lock between all "targets"
    }

    @Override
    protected Log getTarget(Void target, AquaPermsPlugin plugin, Sender sender) {
        Log log = plugin.getStorage().getLog().join();

        if (log == null) {
            Message.LOG_LOAD_ERROR.send(sender);
        }

        return log;
    }

    @Override
    protected void cleanup(Log log, AquaPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(AquaPermsPlugin plugin) {
        // should never be called if we specify Type.NO_TARGET_ARGUMENT in the constructor
        throw new UnsupportedOperationException();
    }

    @Override
    protected Void parseTarget(String target, AquaPermsPlugin plugin, Sender sender) {
        // should never be called if we specify Type.NO_TARGET_ARGUMENT in the constructor
        throw new UnsupportedOperationException();
    }

}
