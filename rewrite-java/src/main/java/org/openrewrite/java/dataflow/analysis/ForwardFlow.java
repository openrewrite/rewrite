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
package org.openrewrite.java.dataflow.analysis;

import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;

public class ForwardFlow extends JavaVisitor<Integer> {
    private static final MethodMatcher methodMatcherToString = new MethodMatcher("java.lang.String toString()");

    public static void findSinks(FlowGraph root) {
        Iterator<Cursor> cursorPath = root.getCursor().getPathAsCursors();

        VariableNameToFlowGraph variableNameToFlowGraph =
                computeVariableAssignment(cursorPath, root);

        if (variableNameToFlowGraph.nextVariableName != null) {
            // The parent statement of the source. Data flow can not start before the source.
            Object taintStmt = null;
            while (cursorPath.hasNext()) {
                Object next = cursorPath.next().getValue();
                if (next instanceof J.Block) {
                    break;
                }
                if (next instanceof J) {
                    taintStmt = next;
                }
            }

            HashMap<String, FlowGraph> initialFlow = new HashMap<>();
            initialFlow.put(variableNameToFlowGraph.nextVariableName, variableNameToFlowGraph.nextFlowGraph);
            Analysis analysis = new Analysis(initialFlow);

            boolean seenRoot = false;
            Cursor blockCursor = root.getCursor().dropParentUntil(J.Block.class::isInstance);
            for (Statement statement : ((J.Block) blockCursor.getValue()).getStatements()) {
                if (seenRoot) {
                    analysis.visit(statement,0, blockCursor);
                }
                if (statement == taintStmt) {
                    seenRoot = true;
                }
            }
        }
    }

    private static class Analysis extends JavaVisitor<Integer> {
        Stack<Map<String, FlowGraph>> flowsByIdentifier = new Stack<>();

        Analysis(Map<String, FlowGraph> initial) {
            this.flowsByIdentifier.push(initial);
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            // a new variable declaration kills existing taints
            flowsByIdentifier.peek().remove(variable.getSimpleName());
            return super.visitVariable(variable, p);
        }

        @Override
        public J visitLambda(J.Lambda lambda, Integer p) {
            for (J parameter : lambda.getParameters().getParameters()) {
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                        flowsByIdentifier.peek().remove(identifier.getSimpleName());
                        return identifier;
                    }
                }.visit(parameter, 0);
            }

            return super.visitLambda(lambda, p);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, Integer p) {
            FlowGraph flowGraph = flowsByIdentifier.peek().get(ident.getSimpleName());
            if (flowGraph != null) {
                FlowGraph next = new FlowGraph(getCursor());
                if (flowGraph.getEdges().isEmpty()) {
                    flowGraph.setEdges(new ArrayList<>(2));
                }
                flowGraph.getEdges().add(next);
                flowsByIdentifier.peek().put(ident.getSimpleName(), next);

                VariableNameToFlowGraph variableNameToFlowGraph =
                        computeVariableAssignment(getCursor().getPathAsCursors(), next);

                if(variableNameToFlowGraph.nextVariableName != null) {
                    flowsByIdentifier.peek().put(
                            variableNameToFlowGraph.nextVariableName,
                            variableNameToFlowGraph.nextFlowGraph
                    );
                }
            }
            return ident;
        }

        @Override
        public J visitBlock(J.Block block, Integer p) {
            flowsByIdentifier.push(new HashMap<>(flowsByIdentifier.peek()));
            J b = super.visitBlock(block, p);
            flowsByIdentifier.pop();
            return b;
        }
    }

    @AllArgsConstructor
    private static final class VariableNameToFlowGraph {
        @Nullable
        String nextVariableName;
        FlowGraph nextFlowGraph;
    }

    private static VariableNameToFlowGraph computeVariableAssignment(Iterator<Cursor> cursorPath, FlowGraph currentFlow) {
        if (cursorPath.hasNext()) {
            // Must avoid inspecting the 'current' node to compute the variable assignment.
            // This is because we perform filtering here, and filtered types may be valid 'source' types.
            cursorPath.next();
        }
        String nextVariableName = null;
        FlowGraph nextFlowGraph = currentFlow;
        while (cursorPath.hasNext()) {
            Cursor ancestorCursor = cursorPath.next();
            Object ancestor = ancestorCursor.getValue();
            if (ancestor instanceof J.Binary) {
                break;
            } else if (ancestor instanceof J.MethodInvocation) {
                if (methodMatcherToString.matches((J.MethodInvocation) ancestor)) {
                    if (nextFlowGraph.getEdges().isEmpty()) {
                        nextFlowGraph.setEdges(new ArrayList<>(1));
                    }
                    FlowGraph next = new FlowGraph(ancestorCursor);
                    nextFlowGraph.getEdges().add(next);
                    nextFlowGraph = next;
                } else {
                    // If the method invocation is not `toString` on a `String`, it's not dataflow
                    break;
                }
            } else if (ancestor instanceof J.TypeCast || ancestor instanceof J.Parentheses || ancestor instanceof J.ControlParentheses) {
                if (nextFlowGraph.getEdges().isEmpty()) {
                    nextFlowGraph.setEdges(new ArrayList<>(1));
                }
                FlowGraph next = new FlowGraph(ancestorCursor);
                nextFlowGraph.getEdges().add(next);
                nextFlowGraph = next;
            } else if (ancestor instanceof J.NewClass) {
                break;
            } else if (ancestor instanceof J.Assignment) {
                Expression variable = ((J.Assignment) ancestor).getVariable();
                if (variable instanceof J.Identifier) {
                    nextVariableName = ((J.Identifier) variable).getSimpleName();
                    break;
                }
            } else if (ancestor instanceof J.AssignmentOperation) {
                Expression variable = ((J.AssignmentOperation) ancestor).getVariable();
                if (variable instanceof J.Identifier) {
                    nextVariableName = ((J.Identifier) variable).getSimpleName();
                    break;
                }
            } else if (ancestor instanceof J.VariableDeclarations.NamedVariable) {
                nextVariableName = ((J.VariableDeclarations.NamedVariable) ancestor).getName().getSimpleName();
                break;
            }
        }
        return new VariableNameToFlowGraph(nextVariableName, nextFlowGraph);
    }
}
