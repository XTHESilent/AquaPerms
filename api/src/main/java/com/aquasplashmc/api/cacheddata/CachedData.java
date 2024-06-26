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

package com.aquasplashmc.api.cacheddata;

import com.aquasplashmc.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Holds cached lookup data for a given set of query options.
 *
 * <p>All calls will account for inheritance, as well as any default data
 * provided by the platform. These calls are heavily cached and are therefore
 * fast.</p>
 */
public interface CachedData {

    /**
     * Gets the query options this container is holding data for.
     *
     * @return the query options this container is caching
     */
    @NonNull QueryOptions getQueryOptions();

}
