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
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForwardFlow extends JavaVisitor<Integer> {

    public static void findSinks(SinkFlow<?, ?> root) {
        Iterator<Cursor> cursorPath = root.getCursor().getPathAsCursors();

        VariableNameToFlowGraph variableNameToFlowGraph =
                computeVariableAssignment(cursorPath, root, root.getSpec());

        if (variableNameToFlowGraph.nextVariableName != null) {
            // The parent statement of the source. Data flow can not start before the source.
            Object taintStmt = null;
            Cursor taintStmtCursorParent = null;
            while (cursorPath.hasNext()) {
                taintStmtCursorParent = cursorPath.next();
                Object next = taintStmtCursorParent.getValue();
                if (next instanceof J.Block) {
                    break;
                }
                if (next instanceof J) {
                    taintStmt = next;
                }
            }

            HashMap<String, FlowGraph> initialFlow = new HashMap<>();
            initialFlow.put(variableNameToFlowGraph.nextVariableName, variableNameToFlowGraph.nextFlowGraph);
            Analysis analysis = new Analysis(root.getSpec(), initialFlow);

            if (taintStmt instanceof J.WhileLoop ||
                    taintStmt instanceof J.DoWhileLoop ||
                    taintStmt instanceof J.ForLoop) {
                // This occurs when an assignment occurs within the control parenthesis of a loop
                Statement body;
                if (taintStmt instanceof J.WhileLoop) {
                    body = ((J.WhileLoop) taintStmt).getBody();
                } else if (taintStmt instanceof J.DoWhileLoop) {
                    body = ((J.DoWhileLoop) taintStmt).getBody();
                } else {
                    body = ((J.ForLoop) taintStmt).getBody();
                }
                analysis.visit(body, 0, taintStmtCursorParent);
            } else if (taintStmt instanceof J.Try) {
                J.Try _try = (J.Try) taintStmt;
                analysis.visit(_try.getBody(), 0, taintStmtCursorParent);
                analysis.visit(_try.getFinally(), 0, taintStmtCursorParent);
            } else {
                // This is when assignment occurs within the body of a block
                visitBlocksRecursive(root.getCursor().dropParentUntil(J.Block.class::isInstance), taintStmt, analysis);
            }
        }
    }

    /**
     * @param blockCursor    The cursor for the current {@link J.Block} being explored.
     * @param startStatement The statement to start looking for flow from. Should not start before this point.
     * @param analysis       The analysis visitor to use.
     */
    private static void visitBlocksRecursive(Cursor blockCursor, Object startStatement, Analysis analysis) {
        boolean seenRoot = false;
        J.Block block = blockCursor.getValue();
        final List<String> declaredVariables = new ArrayList<>();
        for (Statement statement : block.getStatements()) {
            if (statement instanceof J.VariableDeclarations) {
                J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) statement;
                for (J.VariableDeclarations.NamedVariable variableDeclaration : variableDeclarations.getVariables()) {
                    declaredVariables.add(variableDeclaration.getSimpleName());
                }
            }
            if (seenRoot) {
                analysis.visit(statement, 0, blockCursor);
            }
            if (statement == startStatement) {
                seenRoot = true;
            }
        }
        J.MethodDeclaration parentMethodDeclaration = blockCursor.firstEnclosing(J.MethodDeclaration.class);
        if (parentMethodDeclaration != null && parentMethodDeclaration.getBody() == block) {
            // This block is the body of a method, so we don't need to visit any higher
            return;
        }
        J.Block parentBlock = blockCursor.getParentOrThrow().firstEnclosing(J.Block.class);
        if (parentBlock != null && parentBlock.getStatements().contains(block) &&
                J.Block.isStaticOrInitBlock(blockCursor)) {
            // This block is the body of a static block or an init block, so we don't need to visit any higher
            return;
        }

        // Remove any variables that were declared in this block
        declaredVariables.forEach(declared -> analysis.flowsByIdentifier.peek().remove(declared));

        // Get the parent J
        J nextStartStatement = blockCursor.getParentOrThrow().firstEnclosing(J.class);
        if (nextStartStatement instanceof J.Block && ((J.Block) nextStartStatement).getStatements().contains(block)) {
            // If the parent J is a block, and the current block is a statement in the of the parent J,
            // then use it as the starting point.
            nextStartStatement = block;
        } else if (nextStartStatement == null || !getPossibleSubBlock(nextStartStatement).contains(block)) {
            // We found *a* parent J, but it wasn't a parent J that we should use as a starting point.
            return;
        }
        visitBlocksRecursive(blockCursor.dropParentUntil(J.Block.class::isInstance), nextStartStatement, analysis);
    }

    private static Set<Statement> getPossibleSubBlock(J j) {
        if (j instanceof J.If) {
            J.If _if = (J.If) j;
            if (_if.getElsePart() != null) {
                return Stream.of(_if.getThenPart(), _if.getElsePart().getBody()).collect(Collectors.toSet());
            } else {
                return Collections.singleton(_if.getThenPart());
            }
        }
        if (j instanceof J.WhileLoop) {
            return Collections.singleton(((J.WhileLoop) j).getBody());
        } else if (j instanceof J.DoWhileLoop) {
            return Collections.singleton(((J.DoWhileLoop) j).getBody());
        } else if (j instanceof J.ForLoop) {
            return Collections.singleton(((J.ForLoop) j).getBody());
        } else if (j instanceof J.ForEachLoop) {
            return Collections.singleton(((J.ForEachLoop) j).getBody());
        } else if (j instanceof J.Try) {
            J.Try _try = (J.Try) j;
            return Stream.concat(
                            Stream.of(_try.getBody(), _try.getFinally()),
                            _try.getCatches().stream().map(J.Try.Catch::getBody)
                    )
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private static class Analysis extends JavaVisitor<Integer> {
        final LocalFlowSpec<?, ?> localFlowSpec;
        Stack<Map<String, FlowGraph>> flowsByIdentifier = new Stack<>();

        Analysis(LocalFlowSpec<?, ?> localFlowSpec, Map<String, FlowGraph> initial) {
            this.localFlowSpec = localFlowSpec;
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
            // If the variable side of an assignment is not a flow step, so we don't need to do anything
            J.Assignment parentAssignment = getCursor().firstEnclosing(J.Assignment.class);
            if (parentAssignment != null && unwrap(parentAssignment.getVariable()) == ident) {
                return ident;
            }
            // If the identifier is a field access then it is not local flow
            J.FieldAccess parentFieldAccess = getCursor().firstEnclosing(J.FieldAccess.class);
            if (parentFieldAccess != null && parentFieldAccess.getName() == ident) {
                return ident;
            }
            // If the identifier is a new class name, then it is not local flow
            J.NewClass parentNewClass = getCursor().firstEnclosing(J.NewClass.class);
            if (parentNewClass != null && parentNewClass.getClazz() == ident) {
                return ident;
            }
            // If the identifier is a method name, then it is not local flow
            J.MethodInvocation parentMethodInvocation = getCursor().firstEnclosing(J.MethodInvocation.class);
            if (parentMethodInvocation != null && parentMethodInvocation.getName() == ident) {
                return ident;
            }

            FlowGraph flowGraph = flowsByIdentifier.peek().get(ident.getSimpleName());
            if (flowGraph != null) {
                FlowGraph next = flowGraph.addEdge(getCursor());
                flowsByIdentifier.peek().put(ident.getSimpleName(), next);

                VariableNameToFlowGraph variableNameToFlowGraph =
                        computeVariableAssignment(getCursor().getPathAsCursors(), next, localFlowSpec);

                if (variableNameToFlowGraph.nextVariableName != null) {
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

        @Override
        public J visitAssignment(J.Assignment assignment, Integer integer) {
            J.Assignment a = (J.Assignment) super.visitAssignment(assignment, integer);
            String variableName = ((J.Identifier) unwrap(a.getVariable())).getSimpleName();
            // Remove the variable name from the flowsByIdentifier map when assignment occurs
            if (a.getAssignment() != flowsByIdentifier.peek().get(variableName).getCursor().getValue()) {
                flowsByIdentifier.peek().remove(variableName);
            }
            return a;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, Integer integer) {
            return super.visitNewClass(newClass, integer);
        }
    }

    @AllArgsConstructor
    private static final class VariableNameToFlowGraph {
        @Nullable
        String nextVariableName;
        FlowGraph nextFlowGraph;
    }

    private static VariableNameToFlowGraph computeVariableAssignment(Iterator<Cursor> cursorPath, FlowGraph currentFlow, LocalFlowSpec<?, ?> spec) {
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
            if (ancestor instanceof Expression) {
                // Offer the cursor of the current flow graph, and a next possible expression to
                // `isAdditionalFlowStep` to see if it should be added to the flow graph.
                // This allows the API user to extend what the definition of 'flow' is.
                Cursor previousCursor = nextFlowGraph.getCursor();
                if (spec.isBarrier(
                        (Expression) ancestor,
                        ancestorCursor
                )) {
                    break;
                }
                if (spec.isFlowStep(
                        previousCursor.getValue(),
                        previousCursor,
                        (Expression) ancestor,
                        ancestorCursor
                )) {
                    nextFlowGraph = nextFlowGraph.addEdge(ancestorCursor);
                    continue;
                }
            }

            if (ancestor instanceof J.Binary) {
                break;
            } else if (ancestor instanceof J.MethodInvocation) {
                break;
            } else if (ancestor instanceof J.Ternary) {
                J.Ternary ternary = (J.Ternary) ancestor;
                Object previousCursorValue = nextFlowGraph.getCursor().getValue();
                if (ternary.getTruePart() == previousCursorValue ||
                        ternary.getFalsePart() == previousCursorValue) {
                    nextFlowGraph = nextFlowGraph.addEdge(ancestorCursor);
                } else {
                    // Data flow does not occur from the ternary conditional part
                    break;
                }
            } else if (ancestor instanceof J.TypeCast ||
                    ancestor instanceof J.Parentheses ||
                    ancestor instanceof J.ControlParentheses) {
                nextFlowGraph = nextFlowGraph.addEdge(ancestorCursor);
            } else if (ancestor instanceof J.NewClass) {
                break;
            } else if (ancestor instanceof J.Assignment ||
                    ancestor instanceof J.AssignmentOperation ||
                    ancestor instanceof J.VariableDeclarations.NamedVariable
            ) {
                Expression variable;
                if (ancestor instanceof J.Assignment) {
                    variable = ((J.Assignment) ancestor).getVariable();
                } else if (ancestor instanceof J.AssignmentOperation) {
                    variable = ((J.AssignmentOperation) ancestor).getVariable();
                } else {
                    variable = ((J.VariableDeclarations.NamedVariable) ancestor).getName();
                }
                variable = unwrap(variable);
                if (variable instanceof J.Identifier) {
                    nextVariableName = ((J.Identifier) variable).getSimpleName();
                    break;
                }
            }
        }
        return new VariableNameToFlowGraph(nextVariableName, nextFlowGraph);
    }

    private static Expression unwrap(Expression expression) {
        if (expression instanceof J.Parentheses) {
            return unwrap((Expression) ((J.Parentheses<?>) expression).getTree());
        } else {
            return expression;
        }
    }
}
