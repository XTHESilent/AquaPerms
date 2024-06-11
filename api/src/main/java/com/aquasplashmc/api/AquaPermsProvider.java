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

package com.aquasplashmc.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import static org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Provides static access to the {@link AquaPerms} API.
 *
 * <p>Ideally, the ServiceManager for the platform should be used to obtain an
 * instance, however, this provider can be used if this is not viable.</p>
 */
public final class AquaPermsProvider {
    private static AquaPerms instance = null;

    /**
     * Gets an instance of the {@link AquaPerms} API,
     * throwing {@link IllegalStateException} if the API is not loaded yet.
     *
     * <p>This method will never return null.</p>
     *
     * @return an instance of the AquaPerms API
     * @throws IllegalStateException if the API is not loaded yet
     */
    public static @NonNull AquaPerms get() {
        AquaPerms instance = AquaPermsProvider.instance;
        if (instance == null) {
            throw new NotLoadedException();
        }
        return instance;
    }

    @Internal
    static void register(AquaPerms instance) {
        AquaPermsProvider.instance = instance;
    }

    @Internal
    static void unregister() {
        AquaPermsProvider.instance = null;
    }

    @Internal
    private AquaPermsProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Exception thrown when the API is requested before it has been loaded.
     */
    private static final class NotLoadedException extends IllegalStateException {
        private static final String MESSAGE = "The AquaPerms API isn't loaded yet!\n" +
                "This could be because:\n" +
                "  a) the AquaPerms plugin is not installed or it failed to enable\n" +
                "  b) the plugin in the stacktrace does not declare a dependency on AquaPerms\n" +
                "  c) the plugin in the stacktrace is retrieving the API before the plugin 'enable' phase\n" +
                "     (call the #get method in onEnable, not the constructor!)\n";

        NotLoadedException() {
            super(MESSAGE);
        }
    }

}
