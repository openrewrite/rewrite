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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Incubating(since = "7.25.0")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ControlFlowNode {
    final Set<ControlFlowNode> predecessors = new HashSet<>();

    abstract Set<ControlFlowNode> getSuccessors();

    protected abstract void _addSuccessorInternal(ControlFlowNode successor);

    Set<ControlFlowNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    <C extends ControlFlowNode> C addSuccessor(C successor) {
        if (this == successor) {
            throw new IllegalArgumentException("Cannot add a node as a successor of itself");
        }
        _addSuccessorInternal(successor);
        successor.predecessors.add(this);
        return successor;
    }

    BasicBlock addBasicBlock() {
        return addSuccessor(new BasicBlock());
    }

    ConditionNode addConditionNodeTruthFirst() {
        throw new IllegalStateException("Can only add a condition node to a basic block");
    }

    ConditionNode addConditionNodeFalseFirst() {
        throw new IllegalStateException("Can only add a condition node to a basic block");
    }

    public List<J> getNodeValues() {
        return Collections.emptyList();
    }

    /**
     * A control flow node that represents a branching point in the code.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class ConditionNode extends ControlFlowNode {

        private final Cursor condition;
        private final boolean truthFirst;
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

        public J getCondition() {
            return condition.getValue();
        }

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            if (truthFirst) {
                if (truthySuccessor == null) {
                    truthySuccessor = successor;
                } else if (falsySuccessor == null) {
                    falsySuccessor = successor;
                } else {
                    throw new IllegalStateException("Condition node already has both successors");
                }
            } else {
                if (falsySuccessor == null) {
                    falsySuccessor = successor;
                } else if (truthySuccessor == null) {
                    truthySuccessor = successor;
                } else {
                    throw new IllegalStateException("Condition node already has both successors");
                }
            }
        }

        private Optional<J.Literal> asBooleanLiteral() {
            if (condition.getValue() instanceof J.Literal) {
                J.Literal literal = condition.getValue();
                if (TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, literal.getType())) {
                    return Optional.of(literal);
                }
            }
            return Optional.empty();
        }

        private boolean isAlwaysTrue() {
            return asBooleanLiteral().map(l -> (Boolean) l.getValue()).orElse(false);
        }

        private boolean isAlwaysFalse() {
            return asBooleanLiteral().map(l -> !((Boolean) l.getValue())).orElse(false);
        }


        @Override
        public List<J> getNodeValues() {
            List<J> l = new LinkedList<>();
            l.add(condition.getValue());
            return l;
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            verifyState();
            if (isAlwaysTrue()) {
                return Collections.singleton(truthySuccessor);
            }
            if (isAlwaysFalse()) {
                return Collections.singleton(falsySuccessor);
            }
            return Stream.of(truthySuccessor, falsySuccessor).collect(Collectors.toSet());
        }

        private void verifyState() {
            if (truthySuccessor == null && falsySuccessor == null) {
                throw new IllegalStateException("Condition node has no successors. Should have both!");
            }
            if (truthySuccessor == null) {
                throw new IllegalStateException("Condition node has no truthy successor");
            }
            if (falsySuccessor == null) {
                throw new IllegalStateException("Condition node has no falsy successor");
            }
        }

        Set<ControlFlowNode> visit(BarrierGuardPredicate isBarrierGuard) {
            verifyState();
            Set<ControlFlowNode> nodes = new HashSet<>(2);
            if (isAlwaysTrue()) {
                nodes.add(truthySuccessor);
            } else if (isAlwaysFalse()) {
                nodes.add(falsySuccessor);
            } else {
                if (!isBarrierGuard.isBarrierGuard(asGuard(), true)) {
                    nodes.add(getTruthySuccessor());
                }
                if (!isBarrierGuard.isBarrierGuard(asGuard(), false)) {
                    nodes.add(getFalsySuccessor());
                }
            }
            return nodes;
        }

        Guard asGuard() {
            return Guard.from(condition).orElseThrow(() -> new IllegalStateException("Condition node has no guard!\n\tAST Node: " + condition.getValue() + "\n\tCursor: " + condition));
        }

        @Override
        public String toString() {
            return "ConditionNode{" + "condition=" + condition.getValue() + ", truthySuccessor=" + truthySuccessor + ", falsySuccessor=" + falsySuccessor + '}';
        }
    }

    /**
     * A basic block is a straight-line code sequence with no branches in except to the entry and
     * no branches out except at the exit.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Basic_block">Wikipedia: Basic Block</a>
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class BasicBlock extends ControlFlowNode {
        @Getter
        private ControlFlowNode successor;
        private final List<Cursor> node = new ArrayList<>();
        private boolean nextConditionDefault = true;

        public J getLeader() {
            if (node.isEmpty()) {
                throw new IllegalStateException("Basic block has no nodes!");
            }
            return node.get(0).getValue();
        }

        boolean hasLeader() {
            return !node.isEmpty();
        }

        public List<Cursor> getNodeCursors() {
            return Collections.unmodifiableList(node);
        }

        String getStatementsWithinBlock() {
            ControlFlowJavaPrinter.ControlFlowPrintOutputCapture<Integer> capture
                    = new ControlFlowJavaPrinter.ControlFlowPrintOutputCapture<>(0);
            ControlFlowJavaPrinter<Integer> printer = new ControlFlowJavaPrinter<>(getNodeValues());
            Cursor commonBlock = getCommonBlock();
            printer.visit(commonBlock.getValue(), capture, commonBlock.getParentOrThrow());
            return StringUtils.trimIndentPreserveCRLF(capture.getOut()).
                    replaceAll("(?m)^[ \t]*\r?\n", "");
        }

        /**
         * The highest common {@link J.Block} that contains all the statements in this basic block.
         */
        Cursor getCommonBlock() {
            // For each cursor in the block, computes the list of J.Blocks the cursor belongs in.
            // Then, gets the list of J.Blocks that appear in all the basic block's cursors' cursor paths
            // (by taking the smallest list)
            List<Cursor> shortestList = node.stream().map(BasicBlock::computeBlockList).min(Comparator.comparingInt(List::size))
                    .orElseThrow(() -> new IllegalStateException("Could not find common block for basic block"));
            if (shortestList.isEmpty()) {
                throw new IllegalStateException("Could not find common block for basic block");
            }
            // Obtains the deepest J.Block cursor in the AST which
            // encompasses all the cursors in the basic block.
            return shortestList.get(shortestList.size() - 1);
        }

        @Override
        public List<J> getNodeValues() {
            return node.stream().map(Cursor::<J>getValue).collect(Collectors.toList());
        }

        boolean addCursorToBasicBlock(Cursor expression) {
            return node.add(expression);
        }

        /**
         * When the next {@link #addConditionNodeTruthFirst()} or {@link #addConditionNodeFalseFirst()} is called,
         * invert the default condition.
         */
        void invertNextConditional() {
            nextConditionDefault = !nextConditionDefault;
        }

        @Override
        ConditionNode addConditionNodeTruthFirst() {
            if (node.isEmpty()) {
                throw new IllegalStateException("Cannot add condition node to empty basic block");
            }
            return addSuccessor(new ControlFlowNode.ConditionNode(node.get(node.size() - 1), nextConditionDefault));
        }

        @Override
        ConditionNode addConditionNodeFalseFirst() {
            if (node.isEmpty()) {
                throw new IllegalStateException("Cannot add condition node to empty basic block");
            }
            return addSuccessor(new ControlFlowNode.ConditionNode(node.get(node.size() - 1), !nextConditionDefault));
        }

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            if (this.successor == successor) {
                return;
            }
            if (this.successor != null) {
                throw new IllegalStateException("Basic block already has a successor ");
            }
            this.successor = successor;
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            if (successor == null) {
                throw new IllegalStateException("Basic block " + this.getStatementsWithinBlock() + " has no successor ");
            }
            return Collections.singleton(successor);
        }

        @Override
        public String toString() {
            if (node.isEmpty()) {
                return "BasicBlock { No leader yet! }";
            } else {
                return "BasicBlock { leader=" + getLeader() + " }";
            }
        }

        private static List<Cursor> computeBlockList(Cursor cursor) {
            List<Cursor> blocks = new ArrayList<>();
            cursor.getPathAsCursors(c -> c.getValue() instanceof J.Block)
                    .forEachRemaining(blocks::add);
            Collections.reverse(blocks);
            return blocks;
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
