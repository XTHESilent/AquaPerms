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

package com.xthesilent.aquaperms.common.messaging;

import com.xthesilent.aquaperms.common.cache.BufferedRequest;
import com.xthesilent.aquaperms.common.model.User;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.messenger.Messenger;
import com.aquasplashmc.api.messenger.MessengerProvider;

public interface InternalMessagingService {

    /**
     * Gets the name of this messaging service
     *
     * @return the name of this messaging service
     */
    String getName();

    Messenger getMessenger();

    MessengerProvider getMessengerProvider();

    /**
     * Closes the messaging service
     */
    void close();

    /**
     * Gets the buffer for sending updates to other servers
     *
     * @return the update buffer
     */
    BufferedRequest<Void> getUpdateBuffer();

    /**
     * Uses the messaging service to inform other servers about a general
     * change.
     */
    void pushUpdate();

    /**
     * Pushes an update for a specific user.
     *
     * @param user the user
     */
    void pushUserUpdate(User user);

    /**
     * Pushes a log entry to connected servers.
     *
     * @param logEntry the log entry
     */
    void pushLog(Action logEntry);

    /**
     * Pushes a custom payload to connected servers.
     *
     * @param channelId the channel id
     * @param payload the payload
     */
    void pushCustomPayload(String channelId, String payload);

}
