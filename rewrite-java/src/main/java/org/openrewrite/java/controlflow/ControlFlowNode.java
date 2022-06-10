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
import lombok.NoArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.stream.Collectors;

@Incubating(since = "7.25.0")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ControlFlowNode {
    final Set<ControlFlowNode> predecessors = new HashSet<>();
    private final Set<ControlFlowNode> successors = new HashSet<>();

    Set<ControlFlowNode> getSuccessors() {
        return Collections.unmodifiableSet(successors);
    }

    Set<ControlFlowNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    <C extends ControlFlowNode> C addSuccessor(C successor) {
        successors.add(successor);
        successor.predecessors.add(this);
        return successor;
    }

    BasicBlock addBasicBlock() {
        return addSuccessor(new BasicBlock());
    }

    ConditionNode addConditionNode(Cursor condition) {
        return addSuccessor(new ConditionNode(condition));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class ConditionNode extends ControlFlowNode {
        final Cursor condition;
    }

    static class BasicBlock extends ControlFlowNode {
        final List<Cursor> node = new ArrayList<>();

        public J getLeader() {
            return node.get(0).getValue();
        }

        public List<Cursor> getNodeCursors() {
            return Collections.unmodifiableList(node);
        }

        public List<J> getNodes() {
            return node.stream().map(Cursor::<J>getValue).collect(Collectors.toList());
        }

        boolean addNodeToBasicBlock(Cursor expression) {
            return node.add(expression);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "create")
    static class Start extends ControlFlowNode {

        @Override
        public String toString() {
            return "Start";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PACKAGE, staticName = "create")
    static class End extends ControlFlowNode {

        @Override
        <C extends ControlFlowNode> C addSuccessor(C successor) {
            throw new UnsupportedOperationException("End nodes cannot have successors");
        }

        @Override
        public String toString() {
            return "End";
        }
    }
}
