/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.controlflow;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.Expression;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * To create an instance call {@link ControlFlow#findControlFlow()}.
 */
@Incubating(since = "7.25.0")
@AllArgsConstructor(staticName = "forGraph", access = AccessLevel.PACKAGE)
public final class ControlFlowSummary {
    private final ControlFlowNode.Start start;
    private final ControlFlowNode.End end;

    @Getter(lazy = true)
    private final Set<ControlFlowNode> allNodes = getAllControlFlowNodes(start, end);

    private static Set<ControlFlowNode> getAllControlFlowNodes(ControlFlowNode.Start start, ControlFlowNode.End end) {
        // LinkedHashSet to preserve insertion order of nodes
        LinkedHashSet<ControlFlowNode> all = new LinkedHashSet<>();
        recurseGetAllControlFlowNodes(start, all, ControlFlowNode::getSuccessors);
        // Sometimes the end may not be reachable because of an infinite loop.
        // In this case, we need to add the end node and look backwards as well to capture 'all' nodes.
        recurseGetAllControlFlowNodes(end, all, ControlFlowNode::getPredecessors);
        return all;
    }


    private static void recurseGetAllControlFlowNodes(ControlFlowNode current, Set<ControlFlowNode> visited, Function<ControlFlowNode, Set<ControlFlowNode>> getNext) {
        visited.add(current);
        Queue<ControlFlowNode> toVisit = new LinkedList<>(getNext.apply(current));
        toVisit.removeAll(visited);
        toVisit.forEach(node -> recurseGetAllControlFlowNodes(node, visited, getNext));
    }

    public Set<ControlFlowNode.BasicBlock> getBasicBlocks() {
        return getAllNodes()
                .stream()
                .filter(node -> node instanceof ControlFlowNode.BasicBlock)
                .map(node -> (ControlFlowNode.BasicBlock) node)
                .collect(Collectors.toSet());
    }

    public Set<ControlFlowNode.ConditionNode> getConditionNodes() {
        return getAllNodes()
                .stream()
                .filter(node -> node instanceof ControlFlowNode.ConditionNode)
                .map(node -> (ControlFlowNode.ConditionNode) node)
                .collect(Collectors.toSet());
    }

    public Set<Expression> computeReachableExpressions(BarrierGuardPredicate predicate) {
        return computeExecutableCodePoints(predicate)
                .stream()
                .filter(cursor -> cursor.getValue() instanceof Expression)
                .map(cursor -> (Expression) cursor.getValue())
                .collect(Collectors.toSet());
    }

    public Set<Cursor> computeExecutableCodePoints(BarrierGuardPredicate predicate) {
        return computeReachableBasicBlock(predicate)
                .stream()
                .flatMap(b -> b.getNodeCursors().stream())
                .collect(Collectors.toSet());
    }

    public Set<ControlFlowNode.BasicBlock> computeReachableBasicBlock(BarrierGuardPredicate predicate) {
        Set<ControlFlowNode> reachable = new LinkedHashSet<>();
        recurseComputeReachableBasicBlock(start, predicate, reachable);
        return reachable
                .stream()
                .filter(cfn -> cfn instanceof ControlFlowNode.BasicBlock)
                .map(cfn -> (ControlFlowNode.BasicBlock) cfn)
                .collect(Collectors.toSet());
    }

    private void recurseComputeReachableBasicBlock(ControlFlowNode visit, BarrierGuardPredicate predicate, Set<ControlFlowNode> reachable) {
        reachable.add(visit);
        final Queue<ControlFlowNode> toVisit = new LinkedList<>();
        if (visit instanceof ControlFlowNode.ConditionNode) {
            toVisit.addAll(((ControlFlowNode.ConditionNode) visit).visit(predicate));
        } else if (!(visit instanceof ControlFlowNode.End)) {
            toVisit.addAll(visit.getSuccessors());
        } else {
            // End node does not need to be visited
            return;
        }
        toVisit.removeAll(reachable);
        toVisit.forEach(n -> recurseComputeReachableBasicBlock(n, predicate, reachable));
    }

    int getBasicBlockCount() {
        return getBasicBlocks().size();
    }

    int getConditionNodeCount() {
        return getConditionNodes().size();
    }

    int getExitCount() {
        return end.getPredecessors().size();
    }
}
