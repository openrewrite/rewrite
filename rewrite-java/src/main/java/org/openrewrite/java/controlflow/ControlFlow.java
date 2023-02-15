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
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.SelfLoathing;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.java.controlflow.ControlFlowIllegalStateException.exceptionMessageBuilder;

@Incubating(since = "7.25.0")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ControlFlow {
    private static final String CONTROL_FLOW_MESSAGE_KEY = "__CONTROL_FLOW_SUMMARY";

    @Nullable
    private Cursor start;

    /**
     * A return value of {@link Optional#empty()} indicates that control flow can not be computed for the given
     * start point.
     */
    public Optional<ControlFlowSummary> findControlFlow() {
        if (start == null) {
            return Optional.empty();
        }
        return start.computeMessageIfAbsent(CONTROL_FLOW_MESSAGE_KEY, __ -> {
            ControlFlowSimpleSummary summary = findControlFlowInternal(start, ControlFlowNode.GraphType.METHOD_BODY_OR_STATIC_INITIALIZER_OR_INSTANCE_INITIALIZER);
            return Optional.of(ControlFlowSummary.forGraph(summary.start, summary.end));
        });
    }

    private static ControlFlowSimpleSummary findControlFlowInternal(Cursor start, ControlFlowNode.GraphType graphType) {
        ControlFlowNode.Start startNode = ControlFlowNode.Start.create(graphType);
        ControlFlowAnalysis<Integer> analysis = new ControlFlowAnalysis<>(startNode, true);
        analysis.visit(start.getValue(), 1, start.getParentOrThrow());
        ControlFlowNode.End end = (ControlFlowNode.End) analysis.current.iterator().next();
        return new ControlFlowSimpleSummary(startNode, end);
    }

    @Value
    private static class ControlFlowSimpleSummary {
        ControlFlowNode.Start start;
        ControlFlowNode.End end;
    }

    public static ControlFlow startingAt(Cursor start) {
        Iterator<Cursor> cursorPath = start.getPathAsCursors();
        Cursor methodDeclarationBlockCursor = null;
        while (cursorPath.hasNext()) {
            Cursor nextCursor = cursorPath.next();
            Object next = nextCursor.getValue();
            if (next instanceof J.Block) {
                methodDeclarationBlockCursor = nextCursor;
                if (J.Block.isStaticOrInitBlock(nextCursor)) {
                    return new ControlFlow(nextCursor);
                }
            } else if (next instanceof J.MethodDeclaration) {
                return new ControlFlow(methodDeclarationBlockCursor);
            }
        }
        return new ControlFlow(null);
    }

    private static class ControlFlowAnalysis<P> extends JavaIsoVisitor<P> {
        /**
         * @implNote This MUST be 'protected' or package-private. This is set by anonymous inner classes.
         */
        protected Set<? extends ControlFlowNode> current;

        private final ControlFlowNode.GraphType graphType;

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

        ControlFlowAnalysis(ControlFlowNode.Start start, boolean methodEntryPoint) {
            this.current = Collections.singleton(Objects.requireNonNull(start, "start cannot be null"));
            this.graphType = start.getGraphType();
            this.methodEntryPoint = methodEntryPoint;
        }

        ControlFlowAnalysis(Set<? extends ControlFlowNode> current, ControlFlowNode.GraphType graphType) {
            this.current = Objects.requireNonNull(current, "current cannot be null");
            this.graphType = Objects.requireNonNull(graphType, "graphType cannot be null");
            this.methodEntryPoint = false;
        }

        ControlFlowNode.BasicBlock currentAsBasicBlock() {
            if (current.isEmpty()) {
                throw new ControlFlowIllegalStateException(exceptionMessageBuilder("No current node!").addCursor(getCursor()));
            }
            if (current.size() == 1 && current.iterator().next() instanceof ControlFlowNode.BasicBlock) {
                return (ControlFlowNode.BasicBlock) current.iterator().next();
            } else {
                return addBasicBlockToCurrent();
            }
        }

        /**
         * Create a new, guaranteed to be empty {@link ControlFlowNode.BasicBlock}.
         */
        ControlFlowNode.BasicBlock newEmptyBasicBlockFromCurrent() {
            ControlFlowNode.BasicBlock currentBasicBlock = currentAsBasicBlock();
            if (currentBasicBlock.hasLeader()) {
                // The current basic block is already non-empty. Create a new one.
                return currentBasicBlock.addBasicBlock();
            } else {
                // The current basic block is empty. Return it.
                return currentBasicBlock;
            }
        }

        ControlFlowNode.BasicBlock addBasicBlockToCurrent() {
            Set<ControlFlowNode> newCurrent = new HashSet<>(current);
            ControlFlowNode.BasicBlock basicBlock = addBasicBlock(newCurrent);
            current = Collections.singleton(basicBlock);
            return basicBlock;
        }

        <C extends ControlFlowNode> void addSuccessorToCurrent(C node) {
            current.forEach(c -> c.addSuccessor(node));
            current = Collections.singleton(node);
        }

        private void addCursorToBasicBlock() {
            currentAsBasicBlock().addCursorToBasicBlock(getCursor());
        }

        ControlFlowAnalysis<P> visitRecursive(Set<? extends ControlFlowNode> start, Tree toVisit, P param) {
            ControlFlowAnalysis<P> analysis = new ControlFlowAnalysis<>(start, graphType);
            analysis.visit(toVisit, param, getCursor());
            return analysis;
        }

        ControlFlowAnalysis<P> visitRecursiveTransferringExit(Set<? extends ControlFlowNode> start, Tree toVisit, P param) {
            ControlFlowAnalysis<P> analysis = visitRecursive(start, toVisit, param);
            if (!analysis.exitFlow.isEmpty()) {
                this.exitFlow.addAll(analysis.exitFlow);
            }
            return analysis;
        }

        ControlFlowAnalysis<P> visitRecursiveTransferringAll(Set<? extends ControlFlowNode> start, Tree toVisit, P param) {
            ControlFlowAnalysis<P> analysis = visitRecursiveTransferringExit(start, toVisit, param);
            if (!analysis.continueFlow.isEmpty()) {
                this.continueFlow.addAll(analysis.continueFlow);
            }
            if (!analysis.breakFlow.isEmpty()) {
                this.breakFlow.addAll(analysis.breakFlow);
            }
            return analysis;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, P p) {
            addCursorToBasicBlock();
            visit(assignment.getAssignment(), p);
            visit(assignment.getVariable(), p);
            return assignment;
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            addCursorToBasicBlock();
            for (Statement statement : block.getStatements()) {
                ControlFlowAnalysis<P> analysis = visitRecursive(current, statement, p);
                current = analysis.current;
                continueFlow.addAll(analysis.continueFlow);
                breakFlow.addAll(analysis.breakFlow);
                exitFlow.addAll(analysis.exitFlow);
                if (current.isEmpty() && statement instanceof J.Try) {
                    // TODO: Support try-catch blocks properly.
                    // This case occurs when a try block has exhaustive exits,
                    // and catch blocks handle errors gracefully
                    break;
                }
            }
            if (methodEntryPoint) {
                ControlFlowNode end = ControlFlowNode.End.create(this.graphType);
                addSuccessorToCurrent(end);
                exitFlow.forEach(exit -> exit.addSuccessor(end));
                current = Collections.singleton(end);
            }
            return block;
        }

        @Override
        public J.Synchronized visitSynchronized(J.Synchronized _sync, P p) {
            addCursorToBasicBlock();
            visit(_sync.getLock(), p);
            visit(_sync.getBody(), p);
            return _sync;
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
            visit(multiVariable.getTypeExpression(), p);
            for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                visit(variable, p);
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

        private static ControlFlowNode.ConditionNode getControlFlowNodeMissingOneSuccessor(Set<ControlFlowNode.ConditionNode> nodes) {
            for (ControlFlowNode.ConditionNode node : nodes) {
                if (node.getTruthySuccessor() == null ^ node.getFalsySuccessor() == null) {
                    return node;
                }
            }
            throw new IllegalArgumentException("No control flow node missing only one successor");
        }

        private static ControlFlowNode.ConditionNode getControlFlowNodeMissingBothSuccessors(Set<ControlFlowNode.ConditionNode> nodes) {
            for (ControlFlowNode.ConditionNode node : nodes) {
                if (node.getTruthySuccessor() == null && node.getFalsySuccessor() == null) {
                    return node;
                }
            }
            throw new IllegalArgumentException("No control flow node missing both successors");
        }

        private interface BranchingAdapter {
            Expression getCondition();

            J getTruePart();

            @Nullable J getFalsePart();

            static BranchingAdapter of(J.If ifStatement) {
                return new BranchingAdapter() {
                    @Override
                    public Expression getCondition() {
                        return ifStatement.getIfCondition();
                    }

                    @Override
                    public J getTruePart() {
                        return ifStatement.getThenPart();
                    }

                    @Override
                    @Nullable
                    public J getFalsePart() {
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
                    public J getFalsePart() {
                        return ternary.getFalsePart();
                    }
                };
            }
        }

        private void visitBranching(BranchingAdapter branching, P p) {
            addCursorToBasicBlock(); // Add the if node first

            // First the condition is invoked
            ControlFlowAnalysis<P> conditionAnalysis = visitRecursiveTransferringAll(current, branching.getCondition(), p);
            J truePart = branching.getTruePart();

            Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
            // Then the then block is visited
            ControlFlowAnalysis<P> thenAnalysis = visitRecursiveTransferringAll(conditionNodes, branching.getTruePart(), p);
            Set<ControlFlowNode> newCurrent = Collections.singleton(getControlFlowNodeMissingOneSuccessor(conditionNodes));
            if (branching.getFalsePart() != null) {
                // Then the else block is visited
                ControlFlowAnalysis<P> elseAnalysis = visitRecursiveTransferringAll(newCurrent, branching.getFalsePart(), p);
                current = Stream.concat(thenAnalysis.current.stream(), elseAnalysis.current.stream()).collect(Collectors.toSet());
            } else {
                current = Stream.concat(newCurrent.stream(), thenAnalysis.current.stream()).collect(Collectors.toSet());
            }
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

        @SuppressWarnings("SpellCheckingInspection")
        @Override
        public J.If.Else visitElse(J.If.Else elze, P p) {
            addCursorToBasicBlock();
            visit(elze.getBody(), p);
            return elze;
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, P p) {
            if (J.Binary.Type.And.equals(binary.getOperator())) {
                ControlFlowAnalysis<P> left = visitRecursive(current, binary.getLeft(), p);
                Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingTruthFirst(left.current);
                ControlFlowAnalysis<P> right = visitRecursive(conditionNodes, binary.getRight(), p);
                current = Stream.concat(right.current.stream(), Stream.of(getControlFlowNodeMissingOneSuccessor(conditionNodes))).collect(Collectors.toSet());
            } else if (J.Binary.Type.Or.equals(binary.getOperator())) {
                ControlFlowAnalysis<P> left = visitRecursive(current, binary.getLeft(), p);
                Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingFalseFirst(left.current);
                ControlFlowAnalysis<P> right = visitRecursive(conditionNodes, binary.getRight(), p);
                current = Stream.concat(Stream.of(getControlFlowNodeMissingOneSuccessor(conditionNodes)), right.current.stream()).collect(Collectors.toSet());
            } else {
                visit(binary.getLeft(), p); // First the left is invoked
                visit(binary.getRight(), p); // Then the right is invoked
                addCursorToBasicBlock(); // Add the binary node last
            }
            return binary;
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, P p) {
            visit(instanceOf.getExpression(), p); // First the expression is invoked
            addCursorToBasicBlock(); // Then the instanceof node
            return instanceOf;
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
            addCursorToBasicBlock(); // Add the while node first
            ControlFlowNode.BasicBlock basicBlock = newEmptyBasicBlockFromCurrent();
            // First the body is visited
            // Only transfer exits
            ControlFlowAnalysis<P> bodyAnalysis = visitRecursiveTransferringExit(Collections.singleton(basicBlock), doWhileLoop.getBody(), p);
            // Then the condition is invoked
            ControlFlowAnalysis<P> conditionAnalysis = visitRecursive(bodyAnalysis.current, doWhileLoop.getWhileCondition(), p);
            Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
            // Add the 'loop' in
            conditionNodes.forEach(conditionNode -> {
                conditionNode.addSuccessor(basicBlock);
                bodyAnalysis.continueFlow.forEach(continueNode -> continueNode.addSuccessor(conditionNode));
            });
            current = Stream.concat(Stream.of(getControlFlowNodeMissingOneSuccessor(conditionNodes)), bodyAnalysis.breakFlow.stream()).collect(Collectors.toSet());
            return doWhileLoop;
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
            addCursorToBasicBlock(); // Add the while node first
            ControlFlowNode.BasicBlock entryBlock = currentAsBasicBlock();
            // First the condition is invoked
            ControlFlowAnalysis<P> conditionAnalysis = visitRecursive(Collections.singleton(entryBlock), whileLoop.getCondition(), p);
            Set<ControlFlowNode.ConditionNode> conditionNodes = allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
            // Then the body is visited
            // Only transfer exits
            ControlFlowAnalysis<P> bodyAnalysis = visitRecursiveTransferringExit(conditionNodes, whileLoop.getBody(), p);
            // Add the 'loop' in
            bodyAnalysis.current.forEach(controlFlowNode -> controlFlowNode.addSuccessor(entryBlock.getSuccessor()));
            bodyAnalysis.continueFlow.forEach(controlFlowNode -> controlFlowNode.addSuccessor(entryBlock.getSuccessor()));
            current = Stream.concat(Stream.of(getControlFlowNodeMissingOneSuccessor(conditionNodes)), bodyAnalysis.breakFlow.stream()).collect(Collectors.toSet());

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
            ControlFlowNode.BasicBlock[] entryBlock = new ControlFlowNode.BasicBlock[1];
            ControlFlowAnalysis<P> controlAnalysisFirstBit = new ControlFlowAnalysis<P>(current, graphType) {
                @Override
                public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
                    // First the initialization is invoked
                    visit(control.getInit(), p);
                    entryBlock[0] = currentAsBasicBlock();
                    // Then the condition is invoked
                    final Expression condition;
                    if (control.getCondition() instanceof J.Empty) {
                        // If the condition is empty, then the condition is always true.
                        // The AST won't have this, as allowing an empty statement is syntactic sugar for `true`.
                        condition = trueLiteral();
                    } else {
                        condition = control.getCondition();
                    }
                    ControlFlowAnalysis<P> conditionAnalysis = visitRecursive(current, condition, p);
                    current = allAsConditionNodesMissingTruthFirst(conditionAnalysis.current);
                    return control;
                }
            };
            controlAnalysisFirstBit.visit(forLoop.getControl(), p, getCursor());

            // Then the body is invoked
            // Only transfer the exit
            ControlFlowAnalysis<P> bodyAnalysis = visitRecursiveTransferringExit(controlAnalysisFirstBit.current, forLoop.getBody(), p);
            // Then the update is invoked
            ControlFlowAnalysis<P> controlAnalysisSecondBit = new ControlFlowAnalysis<P>(bodyAnalysis.current, graphType) {
                @Override
                public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
                    if (current.isEmpty()) {
                        // The for loop ended in a return statement, so we need to create a new basic block for the update
                        current = Collections.singleton(ControlFlowNode.BasicBlock.create());
                    }
                    // Now the update is invoked
                    if (control.getUpdate().isEmpty() || control.getUpdate().get(0) instanceof J.Empty) {
                        visit(control.getUpdate(), p);
                        return control;
                    }
                    current = Collections.singleton(newEmptyBasicBlockFromCurrent());
                    visit(control.getUpdate(), p);
                    return control;
                }
            };
            controlAnalysisSecondBit.visit(forLoop.getControl(), p, getCursor());
            // Point any `continue` nodes to the condition node
            bodyAnalysis.continueFlow.forEach(continueControlFlowNode -> continueControlFlowNode.addSuccessor(entryBlock[0].getSuccessor()));
            // Add the 'increment' statement to the basic block as the last element
            controlAnalysisSecondBit.current.forEach(controlFlowNode -> {
                controlFlowNode.addSuccessor(entryBlock[0].getSuccessor());
            });

            current = Stream.concat(Stream.of(getControlFlowNodeMissingOneSuccessor(allAsConditionNodesMissingTruthFirst(controlAnalysisFirstBit.current))), bodyAnalysis.breakFlow.stream()).collect(Collectors.toSet());
            return forLoop;
        }

        private static J.Literal trueLiteral() {
            return new J.Literal(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    true,
                    "true",
                    null,
                    JavaType.Primitive.Boolean
            );
        }

        /**
         * Generate the control flow graph for {@link J.ForEachLoop}.
         * </p>
         * Unlike normal graph generation, because {@link J.ForEachLoop} are heavy syntactic sugar, "fake" AST nodes need to be generated.
         * These "fake" AST nodes allows us to preserve some fundamental assumptions about the ControlFlowNode graph,
         * in particular that a given {@link ControlFlowNode.BasicBlock} only has one successor.
         *
         * @implNote Creates a fake initialization of an {@link Iterator} before the for-each loop, and assigns that to a fake variable.
         * Then the looping {@link ControlFlowNode.ConditionNode} is a fake call to {@link Iterator#hasNext()} on that iterator.
         */
        @Override
        @SelfLoathing(name = "Jonathan Leitschuh")
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
            String iteratorVariableNumber =
                    VariableNameUtils.generateVariableName(
                            forLoop.getControl().getVariable().getVariables().get(0).getSimpleName() + "Iterator",
                            this.getCursor(),
                            VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER
                    );

            JavaType controlLoopType = forLoop.getControl().getVariable().getVariables().get(0).getType();
            if (controlLoopType == null) {
                throw new ControlFlowIllegalStateException(
                        exceptionMessageBuilder("No type for for loop control variable")
                                .addCursor(getCursor())
                );
            }
            J.VariableDeclarations fakeIteratorAssignment = createFakeIteratorVariableDeclarations(
                    iteratorVariableNumber,
                    controlLoopType,
                    forLoop
            );
            visit(fakeIteratorAssignment, p, getCursor());

            addCursorToBasicBlock();

            // NOTE: Don't move this line into the `visitForEachControl`, it will break. The cursor at this scope is important.
            J.MethodInvocation fakeConditionalMethod = createFakeConditionalMethod(
                    fakeIteratorAssignment.getVariables().get(0).getName(),
                    forLoop.getControl().getIterable()
            );

            ControlFlowAnalysis<P> controlAnalysis = new ControlFlowAnalysis<P>(current, graphType) {
                @Override
                public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
                    addCursorToBasicBlock();
                    visit(control.getVariable(), p);
                    visit(fakeConditionalMethod, p);
                    return control;
                }
            };
            controlAnalysis.visit(forLoop.getControl(), p, getCursor());

            ControlFlowNode.ConditionNode conditionalEntry = controlAnalysis.currentAsBasicBlock().addConditionNodeTruthFirst();
            ControlFlowAnalysis<P> bodyAnalysis = visitRecursiveTransferringExit(Collections.singleton(conditionalEntry), forLoop.getBody(), p);
            // Point any `continue` nodes to the condition node
            bodyAnalysis.continueFlow.forEach(continueFlowNode -> continueFlowNode.addSuccessor(conditionalEntry));
            // Add the iterate statement to the basic block as the last element
            bodyAnalysis.current.forEach(controlFlowNode -> controlFlowNode.addSuccessor(conditionalEntry));
            current = Stream.concat(Stream.of(conditionalEntry), bodyAnalysis.breakFlow.stream()).collect(Collectors.toSet());
            return forLoop;
        }

        /**
         * Part of the de-sugaring process to create a cleaner control flow graph for {@link J.ForEachLoop}.
         * </p>
         * Generates a fake iterable assigned to a variable from the {@link J.ForEachLoop}'s iterable.
         * </p>
         * Example 1:
         * <pre>{@code
         * for (String s : new String[] {"Hello", "Goodbye"}) {
         *     System.out.println(s);
         * }
         * }</pre>
         * Will become:
         * <pre>{@code
         * final Iterator<String> sIterator = Arrays.stream(new String[] {"Hello", "Goodbye"}).iterator();
         * for (String s : ...) {
         *      System.out.println(s);
         * }
         * }</pre>
         * </p>
         * Example 2:
         * <pre>{@code
         * for (String s : Collections.singletonList("Hello")) {
         *     System.out.println(s);
         * }
         * }</pre>
         * Will become:
         * <pre>{@code
         * final Iterator<String> sIterator = Collections.singletonList("Hello").iterator();
         * for (String s : ...) {
         *      System.out.println(s);
         * }
         * }</pre>
         */
        @SelfLoathing(name = "Jonathan Leitschuh")
        private J.VariableDeclarations createFakeIteratorVariableDeclarations(String variableName, JavaType iteratorType, J.ForEachLoop forLoop) {
            Expression iterable = forLoop.getControl().getIterable();
            String type = iteratorType instanceof JavaType.Primitive ? ((JavaType.Primitive) iteratorType).getClassName() : iteratorType.toString();
            Supplier<Cursor> cursorSupplier = () -> getCursor().dropParentUntil(J.Block.class::isInstance);
            JavaTemplate fakeIterableVariableTemplate = iterable.getType() instanceof JavaType.Array ?
                    JavaTemplate.builder(cursorSupplier, "final Iterator<" + type + "> " + variableName + " = Arrays.stream(#{anyArray()}).iterator()").imports("java.util.Arrays").build() :
                    JavaTemplate.builder(cursorSupplier, "final Iterator<" + type + "> " + variableName + " = #{any(java.lang.Iterable)}.iterator()").build();
            // Unfortunately, because J.NewArray isn't a statement, we have to find a place in the AST where a statement could be placed. This way a statement will always be generated.
            // Find the closes outer block to place our statement.
            J.Block block = getCursor().firstEnclosing(J.Block.class);
            if (block == null) {
                throw new ControlFlowIllegalStateException(
                        exceptionMessageBuilder("Unable to create new J.VariableDeclarations, couldn't find an outer J.Block")
                                .addCursor(getCursor())
                );
            }
            // If the for loop is within a label, then get the coordinates for it instead.
            // TODO: Support labeled labels 🤦‍
            J.Label maybeParentLabel = getCursor().firstEnclosing(J.Label.class);
            JavaCoordinates coordinates;
            if (maybeParentLabel != null && maybeParentLabel.getStatement() == forLoop) {
                coordinates = maybeParentLabel.getCoordinates().before();
            } else {
                coordinates = forLoop.getCoordinates().before();
            }
            // Use the template within the scope of the parent block
            J.Block newFakeBlock = block.withTemplate(fakeIterableVariableTemplate, coordinates, iterable);
            // Find the newly generated statement within the block
            for (Statement statement : newFakeBlock.getStatements()) {
                if (!(statement instanceof J.VariableDeclarations)) {
                    continue;
                }
                J.VariableDeclarations maybeNewDeclaration = (J.VariableDeclarations) statement;
                if (maybeNewDeclaration.getVariables().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName).anyMatch(name -> name.equals(variableName))) {
                    return maybeNewDeclaration;
                }
            }
            throw new ControlFlowIllegalStateException(
                    exceptionMessageBuilder("Unable to create new J.VariableDeclarations with name `" + variableName + "`")
                            .addCursor(getCursor())
            );
        }

        @SelfLoathing(name = "Jonathan Leitschuh")
        protected J.MethodInvocation createFakeConditionalMethod(J.Identifier iteratorIdentifier, Expression iterable) {
            JavaTemplate fakeConditionalTemplate = JavaTemplate.builder(this::getCursor, "#{any(java.util.Iterator)}.hasNext()").build();
            JavaCoordinates coordinates = iterable.getCoordinates().replace();
            J.MethodInvocation fakeConditional = iterable.withTemplate(fakeConditionalTemplate, coordinates, iteratorIdentifier);
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
            visit(_try.getResources(), p);
            if (_try.getFinally() == null) {
                visit(_try.getBody(), p);
            } else {
                ControlFlowAnalysis<P> tryBodyAnalysis = new ControlFlowAnalysis<>(current, graphType);
                tryBodyAnalysis.visit(_try.getBody(), p, getCursor());
                if (!tryBodyAnalysis.current.isEmpty()) {
                    ControlFlowAnalysis<P> finallyAnalysisFromCurrent =
                            visitRecursiveTransferringAll(tryBodyAnalysis.current, _try.getFinally(), p);
                    current = finallyAnalysisFromCurrent.current;
                } else {
                    current = Collections.emptySet();
                }
                if (!tryBodyAnalysis.exitFlow.isEmpty()) {
                    ControlFlowAnalysis<P> finallyAnalysisFromExitFlow =
                            visitRecursiveTransferringAll(tryBodyAnalysis.exitFlow, _try.getFinally(), p);
                    exitFlow.addAll(finallyAnalysisFromExitFlow.current);
                }
                if (!tryBodyAnalysis.breakFlow.isEmpty()) {
                    ControlFlowAnalysis<P> finallyAnalysisFromBreakFlow =
                            visitRecursiveTransferringAll(tryBodyAnalysis.breakFlow, _try.getFinally(), p);
                    breakFlow.addAll(finallyAnalysisFromBreakFlow.current);
                }
                if (!tryBodyAnalysis.continueFlow.isEmpty()) {
                    ControlFlowAnalysis<P> finallyAnalysisFromContinueFlow =
                            visitRecursiveTransferringAll(tryBodyAnalysis.continueFlow, _try.getFinally(), p);
                    continueFlow.addAll(finallyAnalysisFromContinueFlow.current);
                }
            }
            return _try;
        }

        @Override
        public J.Try.Resource visitTryResource(J.Try.Resource tryResource, P p) {
            visit(tryResource.getVariableDeclarations(), p);
            return tryResource;
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, P p) {
            addCursorToBasicBlock();
            if (lambda.getBody() instanceof J.Block) {
                ControlFlowSimpleSummary summary = findControlFlowInternal(new Cursor(getCursor(), lambda.getBody()), ControlFlowNode.GraphType.LAMBDA);
                currentAsBasicBlock().addSuccessor(summary.start);
                current = Collections.singleton(summary.end);
                return lambda;
            } else {
                return super.visitLambda(lambda, p);
            }
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
            addCursorToBasicBlock();
            return classDecl;
        }

        @Override
        public J.Switch visitSwitch(J.Switch _switch, P p) {
            addCursorToBasicBlock();
            return _switch;
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, P p) {
            J.MethodInvocation parent = getCursor().firstEnclosing(J.MethodInvocation.class);
            if (parent != null && parent.getArguments().contains(empty)) {
                return empty;
            }
            addCursorToBasicBlock();
            return super.visitEmpty(empty, p);
        }

        @Override
        public J.Return visitReturn(J.Return _return, P p) {
            visit(_return.getExpression(), p); // First the expression is invoked
            addCursorToBasicBlock(); // Then the return
            exitFlow.addAll(current);
            current = Collections.emptySet();
            return _return;
        }

        @Override
        public J.Throw visitThrow(J.Throw thrown, P p) {
            visit(thrown.getException(), p); // First the expression is invoked
            addCursorToBasicBlock(); // Then the return
            exitFlow.addAll(current);
            current = Collections.emptySet();
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
