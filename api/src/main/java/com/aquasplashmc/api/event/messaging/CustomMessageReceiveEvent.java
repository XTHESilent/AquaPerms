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

package com.aquasplashmc.api.event.messaging;

import com.aquasplashmc.api.event.AquaPermsEvent;
import com.aquasplashmc.api.event.util.Param;
import com.aquasplashmc.api.messaging.MessagingService;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Called when a custom payload message is received via the {@link MessagingService}.
 *
 * <p>This event is effectively the 'other end' of
 * {@link MessagingService#sendCustomMessage(String, String)}.</p>
 *
 * @since 5.5
 */
public interface CustomMessageReceiveEvent extends AquaPermsEvent {

    /**
     * Gets the channel id.
     *
     * @return the channel id
     */
    @Param(0)
    @NonNull String getChannelId();

    /**
     * Gets the custom payload that was sent.
     *
     * @return the custom payload
     */
    @Param(1)
    @NonNull String getPayload();

}
