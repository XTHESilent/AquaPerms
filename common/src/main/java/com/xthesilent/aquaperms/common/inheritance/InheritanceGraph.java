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

package com.xthesilent.aquaperms.common.inheritance;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.graph.Graph;
import com.xthesilent.aquaperms.common.graph.TraversalAlgorithm;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.aquasplashmc.api.node.types.InheritanceNode;
import com.aquasplashmc.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link Graph} which represents an "inheritance tree".
 */
public class InheritanceGraph implements Graph<PermissionHolder> {
    private final AquaPermsPlugin plugin;

    /**
     * The contexts to resolve inheritance in.
     */
    private final QueryOptions queryOptions;

    public InheritanceGraph(AquaPermsPlugin plugin, QueryOptions queryOptions) {
        this.plugin = plugin;
        this.queryOptions = queryOptions;
    }

    @Override
    public Iterable<? extends PermissionHolder> successors(PermissionHolder holder) {
        Set<Group> successors = new LinkedHashSet<>();
        for (InheritanceNode n : holder.getOwnInheritanceNodes(this.queryOptions)) {
            Group g = this.plugin.getGroupManager().getIfLoaded(n.getGroupName());
            if (g != null) {
                successors.add(g);
            }
        }

        List<Group> successorsSorted = new ArrayList<>(successors);
        successorsSorted.sort(holder.getInheritanceComparator());
        return successorsSorted;
    }

    /**
     * Returns an iterable which will traverse this inheritance graph using the specified
     * algorithm starting at the given permission holder start node.
     *
     * @param algorithm the algorithm to use when traversing
     * @param postTraversalSort if a final sort according to inheritance (weight, primary group) rules
     *                          should be performed after the traversal algorithm has completed
     * @param startNode the start node in the inheritance graph
     * @return an iterable
     */
    public Iterable<PermissionHolder> traverse(TraversalAlgorithm algorithm, boolean postTraversalSort, PermissionHolder startNode) {
        Iterable<PermissionHolder> traversal = traverse(algorithm, startNode);

        // perform post traversal sort if needed
        if (postTraversalSort) {
            List<PermissionHolder> resolvedTraversal = new ArrayList<>();
            for (PermissionHolder node : traversal) {
                resolvedTraversal.add(node);
            }

            resolvedTraversal.sort(startNode.getInheritanceComparator());
            traversal = resolvedTraversal;
        }

        return traversal;
    }

    /**
     * Perform a traversal according to the rules defined in the configuration.
     *
     * @param startNode the start node in the inheritance graph
     * @return an iterable
     */
    public Iterable<PermissionHolder> traverse(PermissionHolder startNode) {
        return traverse(
                this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM),
                this.plugin.getConfiguration().get(ConfigKeys.POST_TRAVERSAL_INHERITANCE_SORT),
                startNode
        );
    }

}
