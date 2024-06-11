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

import com.google.common.collect.ForwardingList;
import com.xthesilent.aquaperms.common.command.abstraction.CommandException;
import com.xthesilent.aquaperms.common.commands.user.UserParentCommand;
import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.context.MutableContextSetImpl;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.DurationParser;
import com.aquasplashmc.api.context.Context;
import com.aquasplashmc.api.context.DefaultContextKeys;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.context.MutableContextSet;
import com.aquasplashmc.api.model.data.TemporaryNodeMergeStrategy;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A list of {@link String} arguments, with extra methods to help
 * with parsing.
 */
public class ArgumentList extends ForwardingList<String> {
    private final List<String> backingList;

    public ArgumentList(List<String> backingList) {
        this.backingList = backingList;
    }

    @Override
    protected List<String> delegate() {
        return this.backingList;
    }

    public boolean indexOutOfBounds(int index) {
        return index < 0 || index >= size();
    }

    @Override
    public String get(int index) throws IndexOutOfBoundsException {
        return super.get(index).replace("{SPACE}", " ");
    }

    public String getOrDefault(int index, String defaultValue) {
        if (indexOutOfBounds(index)) {
            return defaultValue;
        }
        return get(index);
    }

    @Override
    public @NonNull ArgumentList subList(int fromIndex, int toIndex) {
        return new ArgumentList(super.subList(fromIndex, toIndex));
    }

    public int getIntOrDefault(int index, int defaultValue) {
        if (indexOutOfBounds(index)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(get(index));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getLowercase(int index, Predicate<? super String> test) throws ArgumentException.DetailedUsage {
        String arg = get(index).toLowerCase(Locale.ROOT);
        if (!test.test(arg)) {
            throw new ArgumentException.DetailedUsage();
        }
        return arg;
    }

    public boolean getBooleanOrInsert(int index, boolean defaultValue) {
        if (!indexOutOfBounds(index)) {
            String arg = get(index);
            if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(arg);
            }
        }

        add(index, Boolean.toString(defaultValue));
        return defaultValue;
    }

    public int getPriority(int index) throws ArgumentException {
        try {
            return Integer.parseInt(get(index));
        } catch (NumberFormatException e) {
            throw new ArgumentException.InvalidPriority(get(index));
        }
    }

    public UUID getUserTarget(int index, AquaPermsPlugin plugin, Sender sender) {
        String arg = get(index);
        return UserParentCommand.parseTargetUniqueId(arg, plugin, sender);
    }

    public Duration getDuration(int index) throws ArgumentException {
        String arg = get(index);
        return parseDuration(arg).orElseThrow(() -> new ArgumentException.InvalidDate(arg));
    }

    public Duration getDurationOrDefault(int index, Duration defaultValue) throws ArgumentException {
        if (indexOutOfBounds(index)) {
            return defaultValue;
        }

        return parseDuration(get(index)).orElse(defaultValue);
    }

    private static Optional<Duration> parseDuration(String input) throws ArgumentException.PastDate {
        try {
            long number = Long.parseLong(input);
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Duration duration = checkPastDate(Duration.between(now, Instant.ofEpochSecond(number)));
            return Optional.of(duration);
        } catch (NumberFormatException e) {
            // ignore
        }

        try {
            Duration duration = checkPastDate(DurationParser.parseDuration(input));
            return Optional.of(duration);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        return Optional.empty();
    }

    private static Duration checkPastDate(Duration duration) throws ArgumentException.PastDate {
        if (duration.isNegative()) {
            throw new ArgumentException.PastDate();
        }
        return duration;
    }

    public Optional<TemporaryNodeMergeStrategy> getTemporaryModifierAndRemove(int index) {
        if (indexOutOfBounds(index)) {
            return Optional.empty();
        }

        TemporaryNodeMergeStrategy strategy = parseTemporaryModifier(get(index));
        if (strategy == null) {
            return Optional.empty();
        }

        remove(index);
        return Optional.of(strategy);
    }

    private static TemporaryNodeMergeStrategy parseTemporaryModifier(String s) {
        switch (s.toLowerCase(Locale.ROOT)) {
            case "accumulate":
                return TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING;
            case "replace":
                return TemporaryNodeMergeStrategy.REPLACE_EXISTING_IF_DURATION_LONGER;
            case "deny":
            case "none":
                return TemporaryNodeMergeStrategy.NONE;
            default:
                return null;
        }
    }

    public MutableContextSet getContextOrDefault(int fromIndex, AquaPermsPlugin plugin) throws CommandException {
        if (size() <= fromIndex) {
            return plugin.getConfiguration().getContextsFile().getDefaultContexts().mutableCopy();
        }
        return parseContext(fromIndex);
    }

    public ImmutableContextSet getContextOrEmpty(int fromIndex) {
        if (size() <= fromIndex) {
            return ImmutableContextSetImpl.EMPTY;
        }
        return parseContext(fromIndex).immutableCopy();
    }

    private MutableContextSet parseContext(int fromIndex) {
        MutableContextSet contextSet = new MutableContextSetImpl();
        List<String> entries = subList(fromIndex, size());
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            int sep = entry.indexOf('=');

            String key;
            String value;

            if (sep != -1) {
                key = entry.substring(0, sep);
                value = entry.substring(sep + 1);
            } else {
                key = i == 1 ? DefaultContextKeys.WORLD_KEY : DefaultContextKeys.SERVER_KEY;
                value = entry;
            }

            if (!Context.isValidKey(key) || !Context.isValidValue(value)) {
                continue;
            }

            contextSet.add(key, value);
        }

        return contextSet;
    }

}
