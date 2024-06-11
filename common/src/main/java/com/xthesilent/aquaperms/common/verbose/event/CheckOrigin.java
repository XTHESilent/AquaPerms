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

package com.xthesilent.aquaperms.common.verbose.event;

/**
 * Represents the origin of a meta check
 */
public enum CheckOrigin {

    /**
     * Indicates the check was caused by a lookup in a platform API
     */
    PLATFORM_API,

    /**
     * Indicates the check was caused by a 'hasPermission' check on the platform
     */
    PLATFORM_API_HAS_PERMISSION,

    /**
     * Indicates the check was caused by a 'hasPermissionSet' type check on the platform
     */
    PLATFORM_API_HAS_PERMISSION_SET,

    /**
     * Indicates the check was caused by a 3rd party API call
     */
    THIRD_PARTY_API,

    /**
     * Indicates the check was caused by a AquaPerms API call
     */
    LUCKPERMS_API,

    /**
     * Indicates the check was caused by a AquaPerms internal
     */
    INTERNAL

}
