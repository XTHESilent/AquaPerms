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

package com.xthesilent.aquaperms.common.api.implementation;

import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.actionlog.ActionLog;
import com.aquasplashmc.api.actionlog.ActionLogger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;

public class ApiActionLogger implements ActionLogger {
    private final AquaPermsPlugin plugin;

    public ApiActionLogger(AquaPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Action.@NonNull Builder actionBuilder() {
        return LoggedAction.build();
    }

    @Override
    public @NonNull CompletableFuture<ActionLog> getLog() {
        return this.plugin.getStorage().getLog().thenApply(ApiActionLog::new);
    }

    @Override
    public @NonNull CompletableFuture<Void> submit(@NonNull Action entry) {
        return CompletableFuture.runAsync(() -> this.plugin.getLogDispatcher().dispatchFromApi((LoggedAction) entry), this.plugin.getBootstrap().getScheduler().async());
    }

    @Override
    public @NonNull CompletableFuture<Void> submitToStorage(@NonNull Action entry) {
        return this.plugin.getStorage().logAction(entry);
    }

    @Override
    public @NonNull CompletableFuture<Void> broadcastAction(@NonNull Action entry) {
        return CompletableFuture.runAsync(() -> this.plugin.getLogDispatcher().broadcastFromApi((LoggedAction) entry), this.plugin.getBootstrap().getScheduler().async());
    }
}
