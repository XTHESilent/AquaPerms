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

package com.xthesilent.aquaperms.common.command.utils;

import com.xthesilent.aquaperms.common.command.abstraction.Command;
import com.xthesilent.aquaperms.common.command.abstraction.CommandException;
import com.xthesilent.aquaperms.common.command.abstraction.GenericChildCommand;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.sender.Sender;

public abstract class ArgumentException extends CommandException {

    public static class DetailedUsage extends ArgumentException {
        @Override
        protected void handle(Sender sender) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void handle(Sender sender, String label, Command<?> command) {
            command.sendDetailedUsage(sender, label);
        }

        @Override
        public void handle(Sender sender, GenericChildCommand command) {
            command.sendDetailedUsage(sender);
        }
    }

    public static class PastDate extends ArgumentException {
        @Override
        protected void handle(Sender sender) {
            Message.PAST_DATE_ERROR.send(sender);
        }
    }

    public static class InvalidDate extends ArgumentException {
        private final String invalidDate;

        public InvalidDate(String invalidDate) {
            this.invalidDate = invalidDate;
        }

        @Override
        protected void handle(Sender sender) {
            Message.ILLEGAL_DATE_ERROR.send(sender, this.invalidDate);
        }
    }

    public static class InvalidPriority extends ArgumentException {
        private final String invalidPriority;

        public InvalidPriority(String invalidPriority) {
            this.invalidPriority = invalidPriority;
        }

        @Override
        public void handle(Sender sender) {
            Message.META_INVALID_PRIORITY.send(sender, this.invalidPriority);
        }
    }
}
