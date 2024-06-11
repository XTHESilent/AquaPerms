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

package com.xthesilent.aquaperms.common.node.types;

import com.xthesilent.aquaperms.common.node.AbstractNode;
import com.xthesilent.aquaperms.common.node.AbstractNodeBuilder;
import com.xthesilent.aquaperms.common.node.factory.Delimiters;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.node.ChatMetaType;
import com.aquasplashmc.api.node.metadata.NodeMetadataKey;
import com.aquasplashmc.api.node.types.PrefixNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Prefix extends AbstractNode<PrefixNode, PrefixNode.Builder> implements PrefixNode {
    public static final String NODE_KEY = "prefix";
    public static final String NODE_MARKER = NODE_KEY + ".";

    public static String key(int priority, String prefix) {
        return NODE_MARKER + priority + AbstractNode.NODE_SEPARATOR + Delimiters.escapeCharacters(prefix);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String prefix, int priority) {
        return builder().prefix(prefix).priority(priority);
    }

    private final String prefix;
    private final int priority;

    public Prefix(String prefix, int priority, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, Object> metadata) {
        super(key(priority, prefix), value, expireAt, contexts, metadata);
        this.prefix = prefix;
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public @NonNull String getMetaValue() {
        return this.prefix;
    }

    @Override
    public @NonNull ChatMetaType getMetaType() {
        return ChatMetaType.PREFIX;
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.prefix, this.priority, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static @Nullable Builder parse(String key) {
        if (!key.toLowerCase(Locale.ROOT).startsWith(NODE_MARKER)) {
            return null;
        }

        Iterator<String> metaParts = Delimiters.SPLIT_BY_NODE_SEPARATOR_IN_TWO.split(key.substring(NODE_MARKER.length())).iterator();

        if (!metaParts.hasNext()) return null;
        String priority = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        try {
            return builder()
                    .priority(Integer.parseInt(priority))
                    .prefix(Delimiters.unescapeCharacters(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static final class Builder extends AbstractNodeBuilder<PrefixNode, PrefixNode.Builder> implements PrefixNode.Builder {
        private String prefix;
        private Integer priority;

        private Builder() {
            this.prefix = null;
            this.priority = null;
        }

        public Builder(String prefix, int priority, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, Object> metadata) {
            super(value, expireAt, context, metadata);
            this.prefix = prefix;
            this.priority = priority;
        }

        @Override
        public @NonNull Builder prefix(@NonNull String prefix) {
            this.prefix = Objects.requireNonNull(prefix, "prefix");
            return this;
        }

        @Override
        public @NonNull Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public @NonNull Prefix build() {
            ensureDefined(this.prefix, "prefix");
            ensureDefined(this.priority, "priority");
            return new Prefix(this.prefix, this.priority, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
