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

package com.xthesilent.aquaperms.standalone;

import com.xthesilent.aquaperms.common.command.CommandManager;
import com.xthesilent.aquaperms.common.command.utils.ArgumentTokenizer;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.standalone.app.integration.CommandExecutor;
import com.xthesilent.aquaperms.standalone.app.integration.SingletonPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StandaloneCommandManager extends CommandManager implements CommandExecutor {
    private final LPStandalonePlugin plugin;

    public StandaloneCommandManager(LPStandalonePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> execute(String command) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(SingletonPlayer.INSTANCE);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(command);
        return executeCommand(wrapped, "lp", arguments);
    }

    @Override
    public List<String> tabComplete(String command) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(SingletonPlayer.INSTANCE);
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(command);
        return tabCompleteCommand(wrapped, arguments);
    }

}
