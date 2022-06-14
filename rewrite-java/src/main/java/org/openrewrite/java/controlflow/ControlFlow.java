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

import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(staticName = "startingAt")
public final class ControlFlow {
    private final Cursor start;

    ControlFlowSummary findControlFlow() {
        Cursor methodDeclarationBlockCursor = getMethodDeclarationBlockCursor();
        ControlFlowNode.Start start = ControlFlowNode.Start.create();
        ControlFlowAnalysis<Integer> analysis = new ControlFlowAnalysis<>(start, true);
        analysis.visit(methodDeclarationBlockCursor.getValue(), 1, methodDeclarationBlockCursor);
        ControlFlowNode.End end = (ControlFlowNode.End) analysis.current.iterator().next();
        return ControlFlowSummary.forGraph(start, end);
    }

    private Cursor getMethodDeclarationBlockCursor() {
        Iterator<Cursor> cursorPath = start.getPathAsCursors();
        Cursor methodDeclarationBlockCursor = null;
        while (cursorPath.hasNext()) {
            Cursor nextCursor = cursorPath.next();
            Object next = nextCursor.getValue();
            if (next instanceof J.Block) {
                methodDeclarationBlockCursor = nextCursor;
            } else if (next instanceof J.MethodDeclaration) {
                break;
            }
        }
        if (methodDeclarationBlockCursor == null) {
            throw new IllegalArgumentException(
                    "Invalid start point: Could not find a Method Declaration to begin computing Control Flow"
            );
        }
        return methodDeclarationBlockCursor;
    }

    private static class ControlFlowAnalysis<P> extends JavaIsoVisitor<P> {

        private Set<ControlFlowNode> current;
        private ControlFlowNode truthyCurrent;

        private ControlFlowNode getTruthyCurrent() {
            if (truthyCurrent != null) {
                return truthyCurrent;
            } else {
                return currentAsBasicBlock();
            }
        }

        private final boolean methodEntryPoint;

        /**
         * Flows that terminate in a {@link J.Return} or {@link J.Throw} statement.
         */
        private final Set<ControlFlowNode> exitFlow = new HashSet<>();
        private boolean jumps;

        ControlFlowAnalysis(ControlFlowNode start, boolean methodEntryPoint) {
            this.current = Collections.singleton(Objects.requireNonNull(start, "start cannot be null"));
            this.methodEntryPoint = methodEntryPoint;
        }

        ControlFlowAnalysis(Set<ControlFlowNode> current) {
            this.current = Objects.requireNonNull(current, "current cannot be null");
            this.methodEntryPoint = false;
        }

        ControlFlowNode.BasicBlock currentAsBasicBlock() {
            jumps = false;
            assert !current.isEmpty() : "No current node!";
            if (current.size() == 1 && current.iterator().next() instanceof ControlFlowNode.BasicBlock) {
                return (ControlFlowNode.BasicBlock) current.iterator().next();
            } else {
                if (!exitFlow.isEmpty()) {
                    return addBasicBlockToCurrent();
                }
                return addBasicBlockToCurrent();
            }
        }

        ControlFlowNode.BasicBlock addBasicBlockToCurrent() {
            Set<ControlFlowNode> newCurrent = new HashSet<>(current);
            if (truthyCurrent != null) {
                newCurrent.add(truthyCurrent);
                truthyCurrent = null;
            }
            ControlFlowNode.BasicBlock basicBlock = addBasicBlock(newCurrent);
            current = Collections.singleton(basicBlock);
            return basicBlock;
        }

        <C extends ControlFlowNode> C addSuccessorToCurrent(C node) {
            current.forEach(c -> c.addSuccessor(node));
            current = Collections.singleton(node);
            return node;
        }

        private void addCursorToBasicBlock() {
            currentAsBasicBlock().addNodeToBasicBlock(getCursor());
        }

        ControlFlowAnalysis<P> visitRecursive(Set<ControlFlowNode> start, Tree toVisit, P param) {
            ControlFlowAnalysis<P> analysis = new ControlFlowAnalysis<>(start);
            analysis.visit(toVisit, param, getCursor());
            return analysis;
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            addBasicBlockToCurrent();
            addCursorToBasicBlock();
            for (Statement statement : block.getStatements()) {
                visit(statement, p);
            }
            if (methodEntryPoint) {
                ControlFlowNode end = ControlFlowNode.End.create();
                if (!jumps) {
                    addSuccessorToCurrent(end);
                }
                exitFlow.forEach(exit -> exit.addSuccessor(end));
                current = Collections.singleton(end);
            }
            return block;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
            visit(method.getSelect(), p); // First the select is invoked
            visit(method.getArguments(), p); // Then the arguments are invoked
            addCursorToBasicBlock(); // Then the method invocation
            return method;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
            for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                visit(variable.getInitializer(), p); // First the initializer is invoked
            }
            addCursorToBasicBlock(); // Then the variable declaration
            return multiVariable;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
            visit(variable.getInitializer(), p); // First add the initializer
            visit(variable.getName(), p); // Then add the name
            addCursorToBasicBlock(); // Then add the variable declaration
            return variable;
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, P p) {
            visit(unary.getExpression(), p); // The expression is invoked
            addCursorToBasicBlock(); // Then the unary
            return unary;
        }

        @Override
        public J.If visitIf(J.If iff, P p) {
            addCursorToBasicBlock(); // Add the if node first

            ControlFlowAnalysis<P> conditionAnalysis =
                    visitRecursive(current, iff.getIfCondition().getTree(), p); // First the condition is invoked
            ControlFlowNode truthNode = conditionAnalysis.getTruthyCurrent();
            ControlFlowNode.ConditionNode conditionNode = truthNode.addConditionNodeTruthFirst();
            ControlFlowAnalysis<P> thenAnalysis =
                    visitRecursive(Collections.singleton(conditionNode), iff.getThenPart(), p); // Then the then block is visited
            if (!thenAnalysis.exitFlow.isEmpty()) {
                exitFlow.addAll(thenAnalysis.exitFlow);
            }
            HashSet<ControlFlowNode> newCurrent = new HashSet<>();
            newCurrent.addAll(conditionAnalysis.current.stream().filter(c -> !conditionNode.getPredecessors().contains(c)).collect(Collectors.toSet()));
            newCurrent.add(conditionNode);
            boolean exhaustiveJump = thenAnalysis.jumps;
            if (iff.getElsePart() != null) {
                ControlFlowAnalysis<P> elseAnalysis =
                        visitRecursive(newCurrent, iff.getElsePart(), p); // Then the else block is visited
                if (!elseAnalysis.exitFlow.isEmpty()) {
                    exitFlow.addAll(elseAnalysis.exitFlow);
                }
                current = Stream.concat(
                        thenAnalysis.current.stream(),
                        elseAnalysis.current.stream()
                ).collect(Collectors.toSet());
                exhaustiveJump &= elseAnalysis.jumps;
            } else {
                current = newCurrent;
            }
            jumps = exhaustiveJump;
            return iff;
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, P p) {
            if (J.Binary.Type.And.equals(binary.getOperator())) {
                ControlFlowAnalysis<P> left = visitRecursive(current, binary.getLeft(), p);
                ControlFlowNode.ConditionNode conditionLeft = left.getTruthyCurrent().addConditionNodeTruthFirst();
                ControlFlowAnalysis<P> right = visitRecursive(
                        Collections.singleton(conditionLeft),
                        binary.getRight(),
                        p
                );
                current = right.current;
                addCursorToBasicBlock();
                truthyCurrent = currentAsBasicBlock();
                current = Stream.concat(
                        left.current.stream().filter(c -> !conditionLeft.getPredecessors().contains(c)),
                        Stream.of(conditionLeft)
                ).collect(Collectors.toSet());
            } else if (J.Binary.Type.Or.equals(binary.getOperator())) {
                ControlFlowAnalysis<P> left = visitRecursive(current, binary.getLeft(), p);
                ControlFlowNode.ConditionNode conditionLeft = left.getTruthyCurrent().addConditionNodeFalseFirst();
                ControlFlowAnalysis<P> right = visitRecursive(
                        Collections.singleton(conditionLeft),
                        binary.getRight(),
                        p
                );
                current = Stream.concat(
                        Stream.of(conditionLeft),
                        right.current.stream()
                ).collect(Collectors.toSet());
                addCursorToBasicBlock();
            } else {
                visit(binary.getLeft(), p); // First the left is invoked
                visit(binary.getRight(), p); // Then the right is invoked
                addCursorToBasicBlock(); // Add the binary node last
            }
            return binary;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, P p) {
            addCursorToBasicBlock();
            return identifier;
        }

        @Override
        public J.Return visitReturn(J.Return _return, P p) {
            visit(_return.getExpression(), p); // First the expression is invoked
            addCursorToBasicBlock(); // Then the return
            exitFlow.addAll(current);
            current = Collections.emptySet();
            jumps = true;
            return _return;
        }

        @Override
        public J.Throw visitThrow(J.Throw thrown, P p) {
            visit(thrown.getException(), p); // First the expression is invoked
            addCursorToBasicBlock(); // Then the return
            exitFlow.addAll(current);
            current = Collections.emptySet();
            jumps = true;
            return thrown;
        }

        private static ControlFlowNode.BasicBlock addBasicBlock(Collection<ControlFlowNode> nodes) {
            if (nodes.isEmpty()) {
                throw new IllegalStateException("No nodes to add to a basic block!");
            }
            Iterator<ControlFlowNode> cfnIterator = nodes.iterator();
            ControlFlowNode.BasicBlock basicBlock = cfnIterator.next().addBasicBlock();
            while (cfnIterator.hasNext()) {
                cfnIterator.next().addSuccessor(basicBlock);
            }
            return basicBlock;
        }
    }
}
