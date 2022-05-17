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
        Object taintStmt = null;
        Iterator<Object> cursorPath = root.getCursor().getPath();

        String taint = computeVariableAssignment(cursorPath);

        if (taint != null) {
            while (cursorPath.hasNext()) {
                Object next = cursorPath.next();
                if (next instanceof J.Block) {
                    break;
                }
                if (next instanceof J) {
                    taintStmt = next;
                }
            }

            HashMap<String, FlowGraph> initialFlow = new HashMap<>();
            initialFlow.put(taint, root);
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

                String newVariableName = computeVariableAssignment(getCursor().getPath());

                if(newVariableName != null) {
                    flowsByIdentifier.peek().put(newVariableName, next);
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

    @Nullable
    private static String computeVariableAssignment(Iterator<Object> cursorPath) {
        if (cursorPath.hasNext()) {
            // Must avoid inspecting the 'current' node to compute the variable assignment.
            // This is because we perform filtering here, and filtered types may be valid 'source' types.
            cursorPath.next();
        }
        while (cursorPath.hasNext()) {
            Object ancestor = cursorPath.next();
            if (ancestor instanceof J.Binary) {
                return null;
            } else if (ancestor instanceof J.MethodInvocation) {
                if (!methodMatcherToString.matches((J.MethodInvocation) ancestor)) {
                    // If the method invocation is not `toString` on a `String`, it's not dataflow
                    return null;
                }
            } else if (ancestor instanceof J.Assignment) {
                Expression variable = ((J.Assignment) ancestor).getVariable();
                if (variable instanceof J.Identifier) {
                    return ((J.Identifier) variable).getSimpleName();
                }
            } else if (ancestor instanceof J.AssignmentOperation) {
                Expression variable = ((J.AssignmentOperation) ancestor).getVariable();
                if (variable instanceof J.Identifier) {
                    return ((J.Identifier) variable).getSimpleName();
                }
            } else if (ancestor instanceof J.VariableDeclarations.NamedVariable) {
                return ((J.VariableDeclarations.NamedVariable) ancestor).getName().getSimpleName();
            }
        }
        return null;
    }
}
