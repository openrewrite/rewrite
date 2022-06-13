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

import lombok.*;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Incubating(since = "7.25.0")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ControlFlowNode {
    final Set<ControlFlowNode> predecessors = new HashSet<>();

    abstract Set<ControlFlowNode> getSuccessors();

    protected abstract void _addSuccessorInternal(ControlFlowNode successor);

    Set<ControlFlowNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    <C extends ControlFlowNode> C addSuccessor(C successor) {
        _addSuccessorInternal(successor);
        successor.predecessors.add(this);
        return successor;
    }

    BasicBlock addBasicBlock() {
        return addSuccessor(new BasicBlock());
    }

    ConditionNode addConditionNode() {
        throw new IllegalStateException("Can only add a condition node to a basic block");
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class ConditionNode extends ControlFlowNode {
        private final Cursor condition;
        /**
         * The successor that will be evaluated if the {@link #condition} is true.
         */
        @Getter
        private ControlFlowNode truthySuccessor;
        /**
         * The successor that will be evaluated if the {@link #condition} is false.
         */
        @Getter
        private ControlFlowNode falsySuccessor;

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            if (truthySuccessor == null) {
                truthySuccessor = successor;
            } else if (falsySuccessor == null) {
                falsySuccessor = successor;
            } else {
                throw new IllegalStateException("Condition node already has both successors");
            }
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            if (truthySuccessor == null && falsySuccessor == null) {
                throw new IllegalStateException("Condition node has no successors. Should have both!");
            }
            if (truthySuccessor == null) {
                throw new IllegalStateException("Condition node has no truthy successor");
            }
            if (falsySuccessor == null) {
                throw new IllegalStateException("Condition node has no falsy successor");
            }
            return Stream.of(truthySuccessor, falsySuccessor).collect(Collectors.toSet());
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class BasicBlock extends ControlFlowNode {
        private ControlFlowNode successor;
        private final List<Cursor> node = new ArrayList<>();

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

        @Override
        ConditionNode addConditionNode() {
            if (node.isEmpty()) {
                throw new IllegalStateException("Cannot add condition node to empty basic block");
            }
            return addSuccessor(new ControlFlowNode.ConditionNode(node.get(node.size() - 1)));
        }

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            if (successor instanceof BasicBlock) {
                throw new IllegalStateException("Can't add a basic block as a successor of a basic block");
            }
            if (this.successor == successor) {
                return;
            }
            if (this.successor != null) {
                throw new IllegalStateException("Basic block already has a successor");
            }
            this.successor = successor;
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            if (successor == null) {
                throw new IllegalStateException("Basic block has no successor");
            }
            return Collections.singleton(successor);
        }
    }

    @NoArgsConstructor(access = AccessLevel.PACKAGE, staticName = "create")
    static class Start extends ControlFlowNode {
        private ControlFlowNode successor = null;

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            if (this.successor != null) {
                throw new IllegalStateException("Start node already has a successor");
            }
            this.successor = successor;
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            return Collections.singleton(successor);
        }

        @Override
        public String toString() {
            return "Start";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PACKAGE, staticName = "create")
    static class End extends ControlFlowNode {

        @Override
        Set<ControlFlowNode> getSuccessors() {
            return Collections.emptySet();
        }

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            throw new IllegalStateException("End nodes cannot have successors");
        }

        @Override
        public String toString() {
            return "End";
        }
    }
}
