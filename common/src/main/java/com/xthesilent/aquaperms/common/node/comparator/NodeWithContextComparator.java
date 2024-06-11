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

package com.xthesilent.aquaperms.common.node.comparator;

import com.xthesilent.aquaperms.common.context.comparator.ContextSetComparator;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.node.Node;

import java.util.Comparator;

/**
 * Compares permission nodes based upon their supposed "priority".
 */
public class NodeWithContextComparator implements Comparator<Node> {
    private static final Comparator<? super Node> INSTANCE = new NodeWithContextComparator(ContextSetComparator.normal(), NodeComparator.normal());
    private static final Comparator<? super Node> REVERSE = new NodeWithContextComparator(ContextSetComparator.reverse(), NodeComparator.reverse());

    public static Comparator<? super Node> normal() {
        return INSTANCE;
    }

    public static Comparator<? super Node> reverse() {
        return REVERSE;
    }

    private final Comparator<? super ImmutableContextSet> contextSetComparator;
    private final Comparator<? super Node> nodeComparator;

    NodeWithContextComparator(Comparator<? super ImmutableContextSet> contextSetComparator, Comparator<? super Node> nodeComparator) {
        this.contextSetComparator = contextSetComparator;
        this.nodeComparator = nodeComparator;
    }

    @Override
    public int compare(Node o1, Node o2) {
        if (o1.equals(o2)) {
            return 0;
        }

        int result = this.contextSetComparator.compare(o1.getContexts(), o2.getContexts());
        if (result != 0) {
            return result;
        }

        return this.nodeComparator.compare(o1, o2);
    }

}
