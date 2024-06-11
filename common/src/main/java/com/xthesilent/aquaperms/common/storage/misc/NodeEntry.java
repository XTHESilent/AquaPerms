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

package com.xthesilent.aquaperms.common.storage.misc;

import com.aquasplashmc.api.node.HeldNode;
import com.aquasplashmc.api.node.Node;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("deprecation")
public final class NodeEntry<H extends Comparable<H>, N extends Node> implements HeldNode<H> {

    public static <H extends Comparable<H>, N extends Node> NodeEntry<H, N> of(H holder, N node) {
        return new NodeEntry<>(holder, node);
    }

    private final H holder;
    private final N node;

    private NodeEntry(H holder, N node) {
        this.holder = holder;
        this.node = node;
    }

    @Override
    public @NonNull H getHolder() {
        return this.holder;
    }

    @Override
    public @NonNull N getNode() {
        return this.node;
    }

    @Override
    public String toString() {
        return "NodeEntry(holder=" + this.holder + ", node=" + this.node + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NodeEntry)) return false;
        final NodeEntry<?, ?> other = (NodeEntry<?, ?>) o;
        return this.getHolder().equals(other.getHolder()) && this.getNode().equals(other.getNode());
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getHolder().hashCode();
        result = result * PRIME + this.getNode().hashCode();
        return result;
    }
}
