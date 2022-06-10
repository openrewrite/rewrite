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
import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;
import java.util.function.Supplier;

@AllArgsConstructor(staticName = "startingAt")
public final class ControlFlow {
    private final Cursor start;

    ControlFlowSummary findControlFlow() {
        Cursor methodDeclarationBlockCursor = getMethodDeclarationBlockCursor();
        ControlFlowNode.Start start = ControlFlowNode.Start.create();
        ControlFlowAnalysis<Integer> analysis = new ControlFlowAnalysis<>(start, true);
        analysis.visit(methodDeclarationBlockCursor.getValue(), 1, methodDeclarationBlockCursor);
        ControlFlowNode.End end = (ControlFlowNode.End) analysis.current;
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

        @NonNull
        private ControlFlowNode current;
        private final boolean methodEntryPoint;
        private final List<ControlFlowNode> danglingEnds = new ArrayList<>();
        /**
         * Flows that terminate in a {@link J.Return} or {@link J.Throw} statement.
         */
        private final Set<ControlFlowNode> exitFlow = new HashSet<>();
        private boolean jumps;

        ControlFlowAnalysis(ControlFlowNode start, boolean methodEntryPoint) {
            this.current = Objects.requireNonNull(start);
            this.methodEntryPoint = methodEntryPoint;
        }

        ControlFlowNode.BasicBlock currentAsBasicBlock() {
            jumps = false;
            if (current instanceof ControlFlowNode.BasicBlock) {
                return (ControlFlowNode.BasicBlock) current;
            } else {
                if (!danglingEnds.isEmpty()) {
                    Iterator<ControlFlowNode> cfnIterator = danglingEnds.iterator();
                    ControlFlowNode.BasicBlock basicBlock = cfnIterator.next().addBasicBlock();
                    while (cfnIterator.hasNext()) {
                        cfnIterator.next().addSuccessor(basicBlock);
                    }
                    danglingEnds.clear();
                    return basicBlock;
                }
                if (!exitFlow.isEmpty()) {
                    return (ControlFlowNode.BasicBlock) (current = current.addBasicBlock());
                }
                throw new IllegalStateException("Not in a Basic Block. Is: " + current);
            }
        }

        private void addCursorToBasicBlock() {
            currentAsBasicBlock().addNodeToBasicBlock(getCursor());
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            current = current.addBasicBlock();
            addCursorToBasicBlock();
            for (Statement statement : block.getStatements()) {
                visit(statement, p);
            }
            if (methodEntryPoint) {
                ControlFlowNode end = ControlFlowNode.End.create();
                if (!danglingEnds.isEmpty()) {
                    danglingEnds.forEach(dangling-> dangling.addSuccessor(end));
                } else if (!jumps) {
                    current.addSuccessor(end);
                }
                exitFlow.forEach(exit -> exit.addSuccessor(end));
                current = end;
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
            return super.visitVariableDeclarations(multiVariable, p);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
            addCursorToBasicBlock(); // First add the variable declaration
            visit(variable.getInitializer(), p); // Then add the initializer
            visit(variable.getName(), p); // Then add the name
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
            visit(iff.getIfCondition().getTree(), p); // First the condition is invoked
            ControlFlowAnalysis<P> thenAnalysis = new ControlFlowAnalysis<>(current, false);
            thenAnalysis.visit(iff.getThenPart(), p); // Then the then block is visited
            boolean exhaustiveJump = true;
            if (!thenAnalysis.exitFlow.isEmpty()) {
                exitFlow.addAll(thenAnalysis.exitFlow);
            }
            if (!thenAnalysis.exitFlow.contains(thenAnalysis.current)) {
                danglingEnds.add(thenAnalysis.current);
            }
            exhaustiveJump &= thenAnalysis.jumps;
            if (iff.getElsePart() != null) {
                ControlFlowAnalysis<P> elseAnalysis = new ControlFlowAnalysis<>(current, false);
                elseAnalysis.visit(iff.getElsePart(), p); // Then the else block is visited
                if (!elseAnalysis.exitFlow.isEmpty()) {
                    exitFlow.addAll(elseAnalysis.exitFlow);
                }
                if (!elseAnalysis.exitFlow.contains(elseAnalysis.current)) {
                    danglingEnds.add(elseAnalysis.current);
                }
                exhaustiveJump &= elseAnalysis.jumps;
            }
            jumps = exhaustiveJump;
            return iff;
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, P p) {
            visit(binary.getLeft(), p); // First the left is invoked
            visit(binary.getRight(), p); // Then the right is invoked
            addCursorToBasicBlock(); // Add the binary node last
            if (isBranchPoint()) {
                current = current.addConditionNode(getCursor());
            }
            return binary;
        }

        boolean isBranchPoint() {
            J.If maybeIf = getCursor().firstEnclosing(J.If.class);
            if (maybeIf != null) {
                return maybeIf.getIfCondition().getTree() == getCursor().getValue();
            }
            return false;
        }

        @Override
        public J.Return visitReturn(J.Return _return, P p) {
            visit(_return.getExpression(), p); // First the expression is invoked
            addCursorToBasicBlock(); // Then the return
            exitFlow.add(current);
            jumps = true;
            return _return;
        }
    }
}
