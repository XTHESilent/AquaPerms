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

package com.aquasplashmc.api.event.log;

import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.event.AquaPermsEvent;
import com.aquasplashmc.api.event.type.Cancellable;
import com.aquasplashmc.api.event.util.Param;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

/**
 * Called when a log is about to be published to the network via the MessagingService
 */
public interface LogNetworkPublishEvent extends AquaPermsEvent, Cancellable {

    /**
     * Gets the ID of the log entry being published
     *
     * @return the id of the log entry being published
     */
    @Param(0)
    @NonNull UUID getLogId();

    /**
     * Gets the log entry to be published
     *
     * @return the log entry to be published
     */
    @Param(1)
    @NonNull Action getEntry();

}
