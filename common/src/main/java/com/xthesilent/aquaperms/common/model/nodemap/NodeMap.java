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

package com.xthesilent.aquaperms.common.model.nodemap;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.node.comparator.NodeWithContextComparator;
import com.xthesilent.aquaperms.common.util.Difference;
import com.aquasplashmc.api.context.ContextSet;
import com.aquasplashmc.api.context.ImmutableContextSet;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.NodeType;
import com.aquasplashmc.api.node.types.InheritanceNode;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A map of nodes held by a {@link PermissionHolder}.
 */
public interface NodeMap {

    boolean isEmpty();

    int size();

    default List<Node> asList() {
        List<Node> list = new ArrayList<>();
        copyTo(list);
        return list;
    }

    default LinkedHashSet<Node> asSet() {
        LinkedHashSet<Node> set = new LinkedHashSet<>();
        copyTo(set);
        return set;
    }

    default SortedSet<Node> asSortedSet() {
        SortedSet<Node> set = new TreeSet<>(NodeWithContextComparator.reverse());
        copyTo(set);
        return set;
    }

    default ImmutableSet<Node> asImmutableSet() {
        ImmutableSet.Builder<Node> builder = ImmutableSet.builder();
        copyTo(builder);
        return builder.build();
    }

    Map<ImmutableContextSet, Collection<Node>> asMap();

    default List<InheritanceNode> inheritanceAsList() {
        List<InheritanceNode> set = new ArrayList<>();
        copyInheritanceNodesTo(set);
        return set;
    }

    default LinkedHashSet<InheritanceNode> inheritanceAsSet() {
        LinkedHashSet<InheritanceNode> set = new LinkedHashSet<>();
        copyInheritanceNodesTo(set);
        return set;
    }

    default SortedSet<InheritanceNode> inheritanceAsSortedSet() {
        SortedSet<InheritanceNode> set = new TreeSet<>(NodeWithContextComparator.reverse());
        copyInheritanceNodesTo(set);
        return set;
    }

    default ImmutableSet<InheritanceNode> inheritanceAsImmutableSet() {
        ImmutableSet.Builder<InheritanceNode> builder = ImmutableSet.builder();
        copyInheritanceNodesTo(builder);
        return builder.build();
    }

    Map<ImmutableContextSet, Collection<InheritanceNode>> inheritanceAsMap();

    void forEach(Consumer<? super Node> consumer);

    void forEach(QueryOptions filter, Consumer<? super Node> consumer);

    void copyTo(Collection<? super Node> collection);

    void copyTo(ImmutableCollection.Builder<? super Node> collection);

    void copyTo(Collection<? super Node> collection, QueryOptions filter);

    <T extends Node> void copyTo(Collection<? super T> collection, NodeType<T> type, QueryOptions filter);

    void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection);

    void copyInheritanceNodesTo(ImmutableCollection.Builder<? super InheritanceNode> collection);

    void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection, QueryOptions filter);

    Collection<Node> nodesInContext(ContextSet context);

    Collection<InheritanceNode> inheritanceNodesInContext(ContextSet context);

    // mutate methods

    Difference<Node> add(Node nodeWithoutInheritanceOrigin);

    Difference<Node> remove(Node node);

    Difference<Node> removeExact(Node node);

    Difference<Node> removeIf(Predicate<? super Node> predicate);

    Difference<Node> removeIf(ContextSet contextSet, Predicate<? super Node> predicate);

    Difference<Node> removeThenAdd(Node nodeToRemove, Node nodeToAdd);

    Difference<Node> clear();

    Difference<Node> clear(ContextSet contextSet);

    Difference<Node> setContent(Iterable<? extends Node> set);

    Difference<Node> setContent(Stream<? extends Node> stream);

    Difference<Node> applyChanges(Difference<Node> changes);

    Difference<Node> addAll(Iterable<? extends Node> set);

    Difference<Node> addAll(Stream<? extends Node> stream);

}
