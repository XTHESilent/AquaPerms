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

package com.aquasplashmc.api.event.group;

import com.aquasplashmc.api.event.AquaPermsEvent;
import com.aquasplashmc.api.event.cause.CreationCause;
import com.aquasplashmc.api.event.util.Param;
import com.aquasplashmc.api.model.group.Group;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Called when a group is created
 */
public interface GroupCreateEvent extends AquaPermsEvent {

    /**
     * Gets the new group
     *
     * @return the new group
     */
    @Param(0)
    @NonNull Group getGroup();

    /**
     * Gets the cause of the creation
     *
     * @return the cause of the creation
     */
    @Param(1)
    @NonNull CreationCause getCause();

}
