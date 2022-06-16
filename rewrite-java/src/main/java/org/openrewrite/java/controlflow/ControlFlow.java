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
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Incubating(since = "7.25.0")
@AllArgsConstructor(staticName = "startingAt")
public final class ControlFlow {
    private final Cursor start;

    public ControlFlowSummary findControlFlow() {
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
                if (((J.Block) next).isStatic()) {
                    break;
                }
            } else if (next instanceof J.MethodDeclaration) {
                break;
            }
        }
        if (methodDeclarationBlockCursor == null) {
            throw new IllegalArgumentException(
                    "Invalid start point: Could not find a method declaration or static block to begin computing Control Flow"
            );
        }
        return methodDeclarationBlockCursor;
    }

    private static class ControlFlowAnalysis<P> extends JavaIsoVisitor<P> {

        /**
         * @implNote This MUST be 'protected' or package-private. This is set by annonymous inner classes.
         */
        protected Set<? extends ControlFlowNode> current;

        private final boolean methodEntryPoint;

        /**
         * Flows that terminate in a {@link J.Return} or {@link J.Throw} statement.
         */
        private final Set<ControlFlowNode> exitFlow = new HashSet<>();

        /**
         * Flows that terminate in a {@link J.Continue} statement.
         */
        private final Set<ControlFlowNode> continueFlow = new HashSet<>();

        /**
         * Flows that terminate in a {@link J.Break} statement.
         */
        private final Set<ControlFlowNode> breakFlow = new HashSet<>();
        private boolean jumps;

        ControlFlowAnalysis(ControlFlowNode start, boolean methodEntryPoint) {
            this.current = Collections.singleton(Objects.requireNonNull(start, "start cannot be null"));
            this.methodEntryPoint = methodEntryPoint;
        }

        ControlFlowAnalysis(Set<? extends ControlFlowNode> current) {
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
            currentAsBasicBlock().addCursorToBasicBlock(getCursor());
        }

        ControlFlowAnalysis<P> visitRecursive(Set<? extends ControlFlowNode> start, Tree toVisit, P param) {
            ControlFlowAnalysis<P> analysis = new ControlFlowAnalysis<>(start);
            analysis.visit(toVisit, param, getCursor());
            return analysis;
        }

        ControlFlowAnalysis<P> visitRecursiveTransferringExit(Set<? extends ControlFlowNode> start, Tree toVisit, P param) {
            final ControlFlowAnalysis<P> analysis = visitRecursive(start, toVisit, param);
            if (!analysis.exitFlow.isEmpty()) {
                this.exitFlow.addAll(analysis.exitFlow);
            }
            return analysis;
        }

        ControlFlowAnalysis<P> visitRecursiveTransferringAll(Set<? extends ControlFlowNode> start, Tree toVisit, P param) {
            final ControlFlowAnalysis<P> analysis = visitRecursiveTransferringExit(start, toVisit, param);
            if (!analysis.continueFlow.isEmpty()) {
                this.continueFlow.addAll(analysis.continueFlow);
            }
            if (!analysis.breakFlow.isEmpty()) {
                this.breakFlow.addAll(analysis.breakFlow);
            }
            return analysis;
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            addCursorToBasicBlock();
            for (Statement statement : block.getStatements()) {
                ControlFlowAnalysis<P> analysis = visitRecursive(current, statement, p);
                current = analysis.current;
                jumps = analysis.jumps;
                continueFlow.addAll(analysis.continueFlow);
                breakFlow.addAll(analysis.breakFlow);
                exitFlow.addAll(analysis.exitFlow);
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
        public J.NewClass visitNewClass(J.NewClass newClass, P p) {
            visit(newClass.getEnclosing(), p); // First the enclosing is invoked
            visit(newClass.getArguments(), p); // Then the arguments are invoked
            addCursorToBasicBlock(); // Then the new class
            // TODO: Maybe invoke a visitor on the body? (Anonymous inner classes)
            return newClass;
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, P p) {
            addCursorToBasicBlock();
            return literal;
        }

        @Override
        public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, P p) {
            addCursorToBasicBlock();
            visit(parens.getTree(), p);
            return parens;
        }

        @Override
        public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
            addCursorToBasicBlock();
            visit(controlParens.getTree(), p);
            return controlParens;
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
            visit(typeCast.getExpression(), p);
            addCursorToBasicBlock();
            return typeCast;
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
            if (unary.getOperator() == J.Unary.Type.Not) {
                addCursorToBasicBlock();
                visit(unary.getExpression(), p);
                current.forEach(controlFlowNode -> {
                    if (controlFlowNode instanceof ControlFlowNode.BasicBlock) {
                        ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) controlFlowNode;
                        basicBlock.invertNextConditional();
                    }
                });
            } else {
                visit(unary.getExpression(), p); // The expression is invoked
                addCursorToBasicBlock(); // Then the unary
            }
            return unary;
        }

        private static Set<ControlFlowNode.ConditionNode> allAsConditionNodesMissingTruthFirst(Set<? extends ControlFlowNode> nodes) {
            return nodes.stream().map(controlFlowNode -> {
                if (controlFlowNode instanceof ControlFlowNode.ConditionNode) {
                    return ((ControlFlowNode.ConditionNode) controlFlowNode);
                } else {
                    return controlFlowNode.addConditionNodeTruthFirst();
                }
            }).collect(Collectors.toSet());
        }

        private static Set<ControlFlowNode.ConditionNode> allAsConditionNodesMissingFalseFirst(Set<? extends ControlFlowNode> nodes) {
            return nodes.stream().map(controlFlowNode -> {
                if (controlFlowNode instanceof ControlFlowNode.ConditionNode) {
                    return ((ControlFlowNode.ConditionNode) controlFlowNode);
                } else {
                    return controlFlowNode.addConditionNodeFalseFirst();
                }
            }).collect(Collectors.toSet());
        }

        private static ControlFlowNode.ConditionNode getControlFlowNodeMissingSuccessors(Set<ControlFlowNode.ConditionNode> nodes) {
            for (ControlFlowNode.ConditionNode node : nodes) {
                if (node.getTruthySuccessor() == null || node.getFalsySuccessor() == null) {
                    return node;
                }
            }
            throw new IllegalArgumentException("No control flow node missing successors");
        }

        private interface BranchingAdapter {
            Expression getCondition();

            J getTruePart();

            @Nullable
            J getFalsePart();

            static BranchingAdapter of(J.If ifStatement) {
                return new BranchingAdapter() {
                    @Override
                    public Expression getCondition() {
                        return ifStatement.getIfCondition().getTree();
                    }

                    @Override
                    public J getTruePart() {
                        return ifStatement.getThenPart();
                    }

                    @Override
                    public @Nullable J getFalsePart() {
                        return ifStatement.getElsePart();
                    }
                };
            }

            static BranchingAdapter of(J.Ternary ternary) {
                return new BranchingAdapter() {
                    @Override
                    public Expression getCondition() {
                        return ternary.getCondition();
                    }

                    @Override
                    public J getTruePart() {
                        return ternary.getTruePart();
                    }

                    @Override
                    public @Nullable J getFalsePart() {
                        return ternary.getFalsePart();
                    }
                };
            }
        }

        private void visitBranching(BranchingAdapter branching, P p) {
            addCursorToBasicBlock(); // Add the if node first

            // First the condition is invoked
            ControlFlowAnalysis<P> conditionAnalysis =
                    visitRecursiveTransferringAll(current, branching.getCondition(), p);

            Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
            // Then the then block is visited
            ControlFlowAnalysis<P> thenAnalysis =
                    visitRecursiveTransferringAll(conditionNodes, branching.getTruePart(), p);
            Set<ControlFlowNode> newCurrent = Collections.singleton(getControlFlowNodeMissingSuccessors(conditionNodes));
            boolean exhaustiveJump = thenAnalysis.jumps;
            if (branching.getFalsePart() != null) {
                // Then the else block is visited
                ControlFlowAnalysis<P> elseAnalysis =
                        visitRecursiveTransferringAll(newCurrent, branching.getFalsePart(), p);
                current = Stream.concat(
                        thenAnalysis.current.stream(),
                        elseAnalysis.current.stream()
                ).collect(Collectors.toSet());
                exhaustiveJump &= elseAnalysis.jumps;
            } else {
                current = newCurrent;
            }
            jumps = exhaustiveJump;
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, P p) {
            visitBranching(BranchingAdapter.of(ternary), p);
            return ternary;
        }

        @Override
        public J.If visitIf(J.If iff, P p) {
            visitBranching(BranchingAdapter.of(iff), p);
            return iff;
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, P p) {
            if (J.Binary.Type.And.equals(binary.getOperator())) {
                ControlFlowAnalysis<P> left = visitRecursive(current, binary.getLeft(), p);
                Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingTruthFirst(left.current);
                ControlFlowAnalysis<P> right = visitRecursive(
                        conditionNodes,
                        binary.getRight(),
                        p
                );
                current = Stream.concat(
                        right.current.stream(),
                        Stream.of(getControlFlowNodeMissingSuccessors(conditionNodes))
                ).collect(Collectors.toSet());
            } else if (J.Binary.Type.Or.equals(binary.getOperator())) {
                ControlFlowAnalysis<P> left = visitRecursive(current, binary.getLeft(), p);
                Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingFalseFirst(left.current);
                ControlFlowAnalysis<P> right = visitRecursive(
                        conditionNodes,
                        binary.getRight(),
                        p
                );
                current = Stream.concat(
                        Stream.of(getControlFlowNodeMissingSuccessors(conditionNodes)),
                        right.current.stream()
                ).collect(Collectors.toSet());
            } else {
                visit(binary.getLeft(), p); // First the left is invoked
                visit(binary.getRight(), p); // Then the right is invoked
                addCursorToBasicBlock(); // Add the binary node last
            }
            return binary;
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
            addCursorToBasicBlock(); // Add the while node first
            ControlFlowNode.BasicBlock entryBlock = currentAsBasicBlock();
            ControlFlowNode.BasicBlock basicBlock = entryBlock.addBasicBlock();
            // First the body is visited
            // Only transfer exits
            ControlFlowAnalysis<P> bodyAnalysis =
                    visitRecursiveTransferringExit(Collections.singleton(basicBlock), doWhileLoop.getBody(), p);
            // Then the condition is invoked
            ControlFlowAnalysis<P> conditionAnalysis =
                    visitRecursive(bodyAnalysis.current, doWhileLoop.getWhileCondition().getTree(), p);
            Set<ControlFlowNode.ConditionNode> conditionNodes =
                    allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
            // Add the 'loop' in
            conditionNodes.forEach(conditionNode -> {
                conditionNode.addSuccessor(basicBlock);
                bodyAnalysis.continueFlow.forEach(continueNode -> continueNode.addSuccessor(conditionNode));
            });
            current = Stream.concat(
                    Stream.of(getControlFlowNodeMissingSuccessors(conditionNodes)),
                    bodyAnalysis.breakFlow.stream()
            ).collect(Collectors.toSet());
            return doWhileLoop;
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
            addCursorToBasicBlock(); // Add the while node first
            ControlFlowNode.BasicBlock entryBlock = currentAsBasicBlock();
            // First the condition is invoked
            ControlFlowAnalysis<P> conditionAnalysis =
                    visitRecursive(Collections.singleton(entryBlock), whileLoop.getCondition().getTree(), p);
            Set<ControlFlowNode.ConditionNode> conditionNodes =
                    allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
            // Then the body is visited
            // Only transfer exits
            ControlFlowAnalysis<P> bodyAnalysis =
                    visitRecursiveTransferringExit(conditionNodes, whileLoop.getBody(), p);
            // Add the 'loop' in
            bodyAnalysis.current.forEach(controlFlowNode ->
                    controlFlowNode.addSuccessor(entryBlock.getSuccessor())
            );
            bodyAnalysis.continueFlow.forEach(controlFlowNode ->
                    controlFlowNode.addSuccessor(entryBlock.getSuccessor())
            );
            current = Stream.concat(
                    Stream.of(getControlFlowNodeMissingSuccessors(conditionNodes)),
                    bodyAnalysis.breakFlow.stream()
            ).collect(Collectors.toSet());
            return whileLoop;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
            // Basic Block has
            //  - For Loop Statement
            //  - Initialization
            //  - Condition
            //  Node has
            //  - Condition
            // Basic Block has
            //  - Body
            //  - Update
            addCursorToBasicBlock(); // Add the for node first
            // First the control is invoked
            final ControlFlowNode.BasicBlock[] entryBlock = new ControlFlowNode.BasicBlock[1];
            ControlFlowAnalysis<P> controlAnalysisFirstBit = new ControlFlowAnalysis<P>(current) {
                @Override
                public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
                    // First the initialization is invoked
                    visit(control.getInit(), p);
                    entryBlock[0] = currentAsBasicBlock();
                    // Then the condition is invoked
                    ControlFlowAnalysis<P> conditionAnalysis =
                            visitRecursive(current, control.getCondition(), p);
                    current = allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
                    return control;
                }
            };
            controlAnalysisFirstBit.visit(forLoop.getControl(), p, getCursor());

            // Then the body is invoked
            // Only transfer the exit
            ControlFlowAnalysis<P> bodyAnalysis =
                    visitRecursiveTransferringExit(controlAnalysisFirstBit.current, forLoop.getBody(), p);
            // Then the update is invoked
            ControlFlowAnalysis<P> controlAnalysisSecondBit = new ControlFlowAnalysis<P>(bodyAnalysis.current) {
                @Override
                public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
                    // Now the update is invoked
                    if (control.getUpdate().isEmpty() || control.getUpdate().get(0) instanceof J.Empty) {
                        visit(control.getUpdate(), p);
                        return control;
                    }
                    current = Collections.singleton(currentAsBasicBlock().addBasicBlock());
                    visit(control.getUpdate(), p);
                    return control;
                }
            };
            controlAnalysisSecondBit.visit(forLoop.getControl(), p, getCursor());
            // Add the 'increment' statement to the basic block as the last element
            controlAnalysisSecondBit.current.forEach(controlFlowNode -> {
                bodyAnalysis.continueFlow.forEach(continueControlFlowNode ->
                        continueControlFlowNode.addSuccessor(controlFlowNode)
                );
                controlFlowNode.addSuccessor(entryBlock[0].getSuccessor());
            });

            current = Stream.concat(
                    Stream.of(getControlFlowNodeMissingSuccessors(
                            allAsConditionNodesMissingTruthFirst(controlAnalysisFirstBit.current)
                    )),
                    bodyAnalysis.breakFlow.stream()
            ).collect(Collectors.toSet());
            return forLoop;
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
            // We treat the for each loop as if there is a fake conditional call to
            // `#{any(java.lang.Iterable)}.iterator().hasNext()` before every loop.
            // This "fake" allows us to preserve some fundamental assumptions about the ControlFlowNode graph,
            // in particular that a given BasicBlock only has one successor
            addCursorToBasicBlock();
            ControlFlowAnalysis<P> controlAnalysis = new ControlFlowAnalysis<P>(current) {
                @Override
                public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
                    visit(control.getIterable(), p);
                    visit(control.getVariable(), p);
                    addBasicBlockToCurrent();
                    return control;
                }
            };

            J.MethodInvocation fakeConditionalMethod = createFakeConditionalMethod(forLoop.getControl().getIterable());
            visit(fakeConditionalMethod, p);

            ControlFlowNode.ConditionNode conditionalEntry =
                    controlAnalysis.currentAsBasicBlock().addConditionNodeTruthFirst();
            ControlFlowAnalysis<P> bodyAnalysis =
                    visitRecursiveTransferringAll(Collections.singleton(conditionalEntry), forLoop.getBody(), p);
            bodyAnalysis.current.forEach(controlFlowNode -> {
                controlFlowNode.addSuccessor(conditionalEntry);
                bodyAnalysis.continueFlow.forEach(continueFlowNode -> continueFlowNode.addSuccessor(controlFlowNode));
            });
            current = Stream.concat(
                    Stream.of(conditionalEntry),
                    bodyAnalysis.breakFlow.stream()
            ).collect(Collectors.toSet());
            return forLoop;
        }

        private J.MethodInvocation createFakeConditionalMethod(Expression iterable) {
            final JavaTemplate fakeConditionalTemplate;
            if (iterable.getType() instanceof JavaType.Array) {
                fakeConditionalTemplate = JavaTemplate.builder(
                        this::getCursor,
                        "Arrays.asList(#{any()}).iterator().hasNext()"
                ).imports("java.util.Arrays").build();
            } else {
                fakeConditionalTemplate = JavaTemplate.builder(
                        this::getCursor,
                        "#{any(java.lang.Iterable)}.iterator().hasNext()"
                ).build();
            }
            final JavaCoordinates coordinates;
            if (iterable instanceof Statement) {
                coordinates = ((Statement) iterable).getCoordinates().replace();
            } else if (iterable instanceof J.Identifier) {
                coordinates = ((J.Identifier) iterable).getCoordinates().replace();
            } else {
                coordinates = new JavaCoordinates(iterable, Space.Location.ANY, JavaCoordinates.Mode.REPLACEMENT, null);
            }
            J.MethodInvocation fakeConditional = iterable.withTemplate(
                    fakeConditionalTemplate,
                    coordinates,
                    iterable
            );
            if (iterable == fakeConditional) {
                throw new IllegalStateException("Failed to create a fake conditional!");
            }
            return fakeConditional;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, P p) {
            addCursorToBasicBlock();
            return identifier;
        }

        @Override
        public J.Assert visitAssert(J.Assert _assert, P p) {
            // INTERESTING CASE HERE:
            // We could treat assert as a branch point where the condition being true points to the next basic block
            // and false points to an exceptional exit, but that would introduce another basic block.
            // The problem is that assertions are only enabled at testing time, and not in production.
            // The decision was made to treat asserts as statements that are non-branching.
            // They will appear in the basic block, but they will not create a conditional node.
            visit(_assert.getCondition(), p);
            if (_assert.getDetail() != null) {
                visit(_assert.getDetail().getElement(), p);
            }
            addCursorToBasicBlock();
            return _assert;
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
            addCursorToBasicBlock();
            return arrayAccess;
        }

        @Override
        public J.Try visitTry(J.Try _try, P p) {
            addCursorToBasicBlock();
            return _try;
        }

        @Override
        public J.Switch visitSwitch(J.Switch _switch, P p) {
            addCursorToBasicBlock();
            return _switch;
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

        @Override
        public J.Continue visitContinue(J.Continue continueStatement, P p) {
            addCursorToBasicBlock();
            continueFlow.add(currentAsBasicBlock());
            current = Collections.emptySet();
            return continueStatement;
        }

        @Override
        public J.Break visitBreak(J.Break breakStatement, P p) {
            addCursorToBasicBlock();
            breakFlow.add(currentAsBasicBlock());
            current = Collections.emptySet();
            return breakStatement;
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
