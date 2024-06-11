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

package com.xthesilent.aquaperms.common.command.tabcomplete;

import com.xthesilent.aquaperms.common.util.Predicates;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface CompletionSupplier {

    CompletionSupplier EMPTY = partial -> Collections.emptyList();

    static CompletionSupplier startsWith(String... strings) {
        return startsWith(() -> Arrays.stream(strings));
    }

    static CompletionSupplier startsWith(Collection<String> strings) {
        return startsWith(strings::stream);
    }

    static CompletionSupplier startsWith(Supplier<Stream<String>> stringsSupplier) {
        return partial -> stringsSupplier.get().filter(Predicates.startsWithIgnoreCase(partial)).collect(Collectors.toList());
    }

    static CompletionSupplier contains(String... strings) {
        return contains(() -> Arrays.stream(strings));
    }

    static CompletionSupplier contains(Collection<String> strings) {
        return contains(strings::stream);
    }

    static CompletionSupplier contains(Supplier<Stream<String>> stringsSupplier) {
        return partial -> stringsSupplier.get().filter(Predicates.containsIgnoreCase(partial)).collect(Collectors.toList());
    }

    List<String> supplyCompletions(String partial);

}
