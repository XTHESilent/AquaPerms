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

import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.context.MutableContextSetImpl;
import com.aquasplashmc.api.context.ContextSetFactory;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.context.MutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ApiContextSetFactory implements ContextSetFactory {
    public static final ApiContextSetFactory INSTANCE = new ApiContextSetFactory();

    private ApiContextSetFactory() {

    }

    @Override
    public ImmutableContextSet.@NonNull Builder immutableBuilder() {
        return new ImmutableContextSetImpl.BuilderImpl();
    }

    @Override
    public @NonNull ImmutableContextSet immutableOf(@NonNull String key, @NonNull String value) {
        return ImmutableContextSetImpl.of(key, value);
    }

    @Override
    public @NonNull ImmutableContextSet immutableEmpty() {
        return ImmutableContextSetImpl.EMPTY;
    }

    @Override
    public @NonNull MutableContextSet mutable() {
        return new MutableContextSetImpl();
    }
}
