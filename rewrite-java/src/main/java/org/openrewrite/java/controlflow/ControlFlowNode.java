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
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.java.controlflow.ControlFlowIllegalStateException.exceptionMessageBuilder;

@Incubating(since = "7.25.0")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ControlFlowNode {
    final Set<ControlFlowNode> predecessors = Collections.newSetFromMap(new IdentityHashMap<>());

    abstract Set<ControlFlowNode> getSuccessors();

    protected abstract void _addSuccessorInternal(ControlFlowNode successor);

    Set<ControlFlowNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    <C extends ControlFlowNode> C addSuccessor(C successor) {
        if (this == successor && !(this instanceof ConditionNode)) {
            // Self loops are allowed in one case only:
            // while(conditional());
            throw new ControlFlowIllegalStateException("Cannot add a node as a successor of itself", this);
        }
        _addSuccessorInternal(successor);
        successor.predecessors.add(this);
        return successor;
    }

    BasicBlock addBasicBlock() {
        return addSuccessor(new BasicBlock());
    }

    ConditionNode addConditionNodeTruthFirst() {
        throw new ControlFlowIllegalStateException("Can only add a condition node to a basic block", this);
    }

    ConditionNode addConditionNodeFalseFirst() {
        throw new ControlFlowIllegalStateException("Can only add a condition node to a basic block", this);
    }

    private static final ThreadLocal<AtomicInteger> recursionCounter =
            ThreadLocal.withInitial(() -> new AtomicInteger(0));

    String toDescriptiveString() {
        if (recursionCounter.get().incrementAndGet() > 2) {
            recursionCounter.get().set(0);
            return "...";
        }
        try {
            String value = internalToDescriptiveString();
            recursionCounter.get().set(0);
            return value;
        } catch (RuntimeException e) {
            recursionCounter.get().set(0);
            return toString();
        }
    }

    abstract String internalToDescriptiveString();

    /**
     * Called when rendering by the {@link ControlFlowDotFileGenerator}.
     */
    abstract String toVisualizerString();

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
                    throw new ControlFlowIllegalStateException("Condition node already has both successors", this);
                }
            } else {
                if (falsySuccessor == null) {
                    falsySuccessor = successor;
                } else if (truthySuccessor == null) {
                    truthySuccessor = successor;
                } else {
                    throw new ControlFlowIllegalStateException("Condition node already has both successors", this);
                }
            }
        }

        private Optional<Boolean> asBooleanLiteralValue() {
            if (condition.getValue() instanceof J.Literal) {
                J.Literal literal = condition.getValue();
                if (TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, literal.getType())) {
                    Boolean value = (Boolean) literal.getValue();
                    return Optional.ofNullable(value);
                }
            }
            return Optional.empty();
        }

        private boolean isAlwaysTrue() {
            return asBooleanLiteralValue().orElse(false);
        }

        private boolean isAlwaysFalse() {
            return asBooleanLiteralValue().orElse(false);
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
                throw new ControlFlowIllegalStateException("Condition node has no successors. Should have both!", this);
            }
            if (truthySuccessor == null) {
                throw new ControlFlowIllegalStateException("Condition node has no truthy successor", this);
            }
            if (falsySuccessor == null) {
                throw new ControlFlowIllegalStateException("Condition node has no falsy successor", this);
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
            return Guard
                    .from(condition)
                    .orElseThrow(() -> new ControlFlowIllegalStateException(exceptionMessageBuilder("Condition node has no guard!").thisNode(this)));
        }

        @Override
        String internalToDescriptiveString() {
            String truthyDescriptive = null;
            if (truthySuccessor != null) {
                truthyDescriptive = truthySuccessor.internalToDescriptiveString();
            }
            String falsyDescriptive = null;
            if (falsySuccessor != null) {
                falsyDescriptive = falsySuccessor.internalToDescriptiveString();
            }
            return "ConditionNode{" + "condition=" + condition.getValue() + ", truthySuccessor=" + truthyDescriptive + ", falsySuccessor=" + falsyDescriptive + '}';
        }

        @Override
        String toVisualizerString() {
            return condition.getValue().toString();
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
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("Basic block has no nodes!").addPredecessors(this));
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
                    .orElseThrow(() -> new ControlFlowIllegalStateException(exceptionMessageBuilder("Could not find common block for basic block").thisNode(this)));
            if (shortestList.isEmpty()) {
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("Could not find common block for basic block").thisNode(this));
            }
            // Obtains the deepest J.Block cursor in the AST which
            // encompasses all the cursors in the basic block.
            return shortestList.get(shortestList.size() - 1);
        }

        private List<J> getNodeValues() {
            return node.stream().map(Cursor::<J>getValue).collect(Collectors.toList());
        }

        void addCursorToBasicBlock(Cursor expression) {
            node.add(expression);
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
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("Cannot add condition node to empty basic block").addPredecessors(this));
            }
            return addSuccessor(new ControlFlowNode.ConditionNode(node.get(node.size() - 1), nextConditionDefault));
        }

        @Override
        ConditionNode addConditionNodeFalseFirst() {
            if (node.isEmpty()) {
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("Cannot add condition node to empty basic block").addPredecessors(this));
            }
            return addSuccessor(new ControlFlowNode.ConditionNode(node.get(node.size() - 1), !nextConditionDefault));
        }

        @Override
        protected void _addSuccessorInternal(ControlFlowNode successor) {
            if (this.successor == successor) {
                return;
            }
            if (this.successor != null) {
                throw new ControlFlowIllegalStateException(
                        exceptionMessageBuilder("Basic block already has a successor").thisNode(this).current(this.successor).otherNode(successor)
                );
            }
            this.successor = successor;
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            if (successor == null) {
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("Basic block has no successor").thisNode(this));
            }
            return Collections.singleton(successor);
        }

        @Override
        String toVisualizerString() {
            return getStatementsWithinBlock();
        }

        @Override
        String internalToDescriptiveString() {
            String statementsWithinBlock = getStatementsWithinBlock();
            if (statementsWithinBlock.contains("\n")) {
                return "BasicBlock { contents=```\n" + statementsWithinBlock + "\n``` }";
            } else {
                return "BasicBlock { contents=`" + statementsWithinBlock + "` }";
            }
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
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("Start node already has a successor").current(this.successor).otherNode(successor));
            }
            this.successor = successor;
        }

        @Override
        Set<ControlFlowNode> getSuccessors() {
            return Collections.singleton(successor);
        }

        @Override
        String internalToDescriptiveString() {
            if (successor == null) {
                return toString();
            } else {
                return "Start { successor=" + successor.toDescriptiveString() + " }";
            }
        }

        @Override
        String toVisualizerString() {
            return toString();
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
            throw new ControlFlowIllegalStateException(exceptionMessageBuilder("End nodes cannot have successors").otherNode(successor));
        }

        @Override
        String internalToDescriptiveString() {
            return "End { predecessors=" + getPredecessors().size() + " }";
        }

        @Override
        String toVisualizerString() {
            return toString();
        }

        @Override
        public String toString() {
            return "End";
        }
    }
}
