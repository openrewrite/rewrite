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
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.trait.expr.VarAccess;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Incubating(since = "7.24.0")
public class ForwardFlow extends JavaVisitor<Integer> {

    public static void findSinks(FlowGraph root, LocalFlowSpec<?, ?> spec) {
        VariableNameToFlowGraph variableNameToFlowGraph =
                computeVariableAssignment(root.getCursor(), root, spec);
        if (variableNameToFlowGraph.identifierToFlow.isEmpty()) {
            return;
        }
        // The parent statement of the source. Data flow can not start before the source.
        Object taintStmt = null;
        Cursor taintStmtCursorParent = null;
        if (variableNameToFlowGraph.currentCursor != null && variableNameToFlowGraph.currentCursor.getValue() instanceof J) {
            taintStmt = variableNameToFlowGraph.currentCursor.getValue();
            taintStmtCursorParent = variableNameToFlowGraph.currentCursor.getParent();
        }
        Iterator<Cursor> remainingPath = variableNameToFlowGraph.remainingCursorPath;
        while (remainingPath.hasNext()) {
            taintStmtCursorParent = remainingPath.next();
            Object next = taintStmtCursorParent.getValue();
            if (next instanceof J.Block) {
                break;
            }
            if (next instanceof J) {
                taintStmt = next;
            }
        }

        Analysis analysis = new Analysis(spec, variableNameToFlowGraph.identifierToFlow.copy());
        if (taintStmtCursorParent == null) {
            throw new IllegalStateException("`taintStmtCursorParent` is null. Computing flow starting at " + root.getCursor().getValue());
        }
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
            assert taintStmt != null : "taintStmt is null";
            visitBlocksRecursive(root.getCursor().dropParentUntil(J.Block.class::isInstance), taintStmt, analysis);
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
        declaredVariables.forEach(analysis.flowsByIdentifier.peek()::remove);

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

    @AllArgsConstructor
    static class IdentifierToFlows {
        private final Map<String, Set<FlowGraph>> identifierToFlows;

        public IdentifierToFlows() {
            this(new HashMap<>());
        }

        public void put(String identifier, FlowGraph flow) {
            identifierToFlows.computeIfAbsent(identifier, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(flow);
        }

        public void putAll(IdentifierToFlows other) {
            other.identifierToFlows.forEach((identifier, flows) -> flows.forEach(flow -> put(identifier, flow)));
        }

        public FlowGraph addForIdentifierVisit(String identifier, Cursor cursor) {
            if (!hasFlows(identifier)) {
                throw new IllegalArgumentException("No flows for identifier " + identifier);
            }
            Iterator<FlowGraph> iterator = get(identifier).iterator();
            FlowGraph flow = iterator.next();
            // Create a FlowGraph for the current identifier being visited
            FlowGraph newFlowGraph = flow.addEdge(cursor);
            while (iterator.hasNext()) {
                // Add edges to all other flows for this identifier, pointing all existing flows to the new flow
                FlowGraph next = iterator.next();
                next.addEdge(newFlowGraph);
            }
            // Replace the existing flows with the new flow
            identifierToFlows.get(identifier).clear();
            put(identifier, newFlowGraph);
            return newFlowGraph;
        }

        public Set<FlowGraph> get(String identifier) {
            return identifierToFlows.getOrDefault(identifier, Collections.emptySet());
        }

        public boolean hasFlows(String identifier) {
            return identifierToFlows.containsKey(identifier);
        }

        public Set<FlowGraph> remove(String identifier) {
            return identifierToFlows.remove(identifier);
        }

        public boolean isEmpty() {
            return identifierToFlows.isEmpty();
        }

        public IdentifierToFlows copy() {
            HashMap<String, Set<FlowGraph>> newIdentifierToFlows = new HashMap<>();
            identifierToFlows.forEach((identifier, flows) -> newIdentifierToFlows.put(identifier, new HashSet<>(flows)));
            return new IdentifierToFlows(newIdentifierToFlows);
        }
    }

    private static class Analysis extends JavaVisitor<Integer> {
        final LocalFlowSpec<?, ?> localFlowSpec;
        Stack<IdentifierToFlows> flowsByIdentifier = new Stack<>();

        Analysis(LocalFlowSpec<?, ?> localFlowSpec, IdentifierToFlows initial) {
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
            // The identifier must be a variable access to be used in a flow
            if (VarAccess.viewOf(getCursor()).map(va -> !va.isRValue()).orSuccess(true)) {
                return ident;
            }
            // If the identifier is a field access then it is not local flow
            J.FieldAccess parentFieldAccess = getCursor().firstEnclosing(J.FieldAccess.class);
            if (parentFieldAccess != null && parentFieldAccess.getName() == ident) {
                return ident;
            }

            if (flowsByIdentifier.peek().hasFlows(ident.getSimpleName())) {

                FlowGraph next = flowsByIdentifier.peek().addForIdentifierVisit(ident.getSimpleName(), getCursor());

                VariableNameToFlowGraph variableNameToFlowGraph =
                        computeVariableAssignment(getCursor(), next, localFlowSpec);

                if (!variableNameToFlowGraph.identifierToFlow.isEmpty()) {
                    flowsByIdentifier.peek().putAll(variableNameToFlowGraph.identifierToFlow);
                }
            }
            return ident;
        }

        @Override
        public J visitBlock(J.Block block, Integer p) {
            flowsByIdentifier.push(flowsByIdentifier.peek().copy());
            J b = super.visitBlock(block, p);
            flowsByIdentifier.pop();
            return b;
        }

        @Override
        public J visitAssignment(J.Assignment assignment, Integer integer) {
            J.Assignment a = (J.Assignment) super.visitAssignment(assignment, integer);
            Expression left = a.getVariable().unwrap();
            if (left instanceof J.Identifier) {
                String variableName = ((J.Identifier) left).getSimpleName();
                if (flowsByIdentifier.peek().hasFlows(variableName) &&
                        flowsByIdentifier.peek().get(variableName).stream().allMatch(v -> v.getCursor().getValue() != a.getAssignment())) {
                    flowsByIdentifier.peek().remove(variableName);
                }
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
        /**
         * A map of variable names to the flow graph that represents the flow to that variable.
         * <p/>
         * <ul>
         *     <li>For statements that do not terminate in an assignment, this will be an empty map.</li>
         *     <li>For statements that terminate in an assignment, this will be a map of the variable name to the flow graph that created it. Usually meaning the map only has one element.</li>
         *     <li>For statements where data/taint flow occurs to the subject/qualifier of a {@link J.MethodInvocation}, the map can be larger than one element.<li/>
         * </ul>
         */
        IdentifierToFlows identifierToFlow;
        Cursor currentCursor;
        Iterator<Cursor> remainingCursorPath;
    }

    private static VariableNameToFlowGraph computeVariableAssignment(Cursor startCursor, FlowGraph currentFlow, LocalFlowSpec<?, ?> spec) {
        Iterator<Cursor> cursorPath = startCursor.getPathAsCursors();
        Cursor ancestorCursor = null;
        if (cursorPath.hasNext()) {
            // Must avoid inspecting the 'current' node to compute the variable assignment.
            // This is because we perform filtering here, and filtered types may be valid 'source' types.
            ancestorCursor = cursorPath.next();
        }
        IdentifierToFlows identifierToFlow = new IdentifierToFlows();
        FlowGraph nextFlowGraph = currentFlow;
        while (cursorPath.hasNext()) {
            ancestorCursor = cursorPath.next();
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
                // Support flow from any argument to the subject of a method invocation
                Cursor methodInvocationCursor = previousCursor.getParentTreeCursor();
                if (methodInvocationCursor.getValue() instanceof J.MethodInvocation) {
                    // The parent is a MethodInvocation, `previousCursor` must be either an argument or the select
                    J.MethodInvocation methodInvocation = methodInvocationCursor.getValue();
                    if (methodInvocation.getSelect() != null && methodInvocation.getArguments().contains(previousCursor.getValue())) {
                        Cursor selectCursor = new Cursor(methodInvocationCursor, methodInvocation.getSelect());
                        if (spec.isFlowStep(
                                previousCursor.getValue(),
                                previousCursor,
                                methodInvocation.getSelect(),
                                selectCursor
                        )) {
                            nextFlowGraph = nextFlowGraph.addEdge(selectCursor);
                            Expression unwrappedSelect = methodInvocation.getSelect().unwrap();
                            VariableNameToFlowGraph variableNameToFlowGraph =
                                    computeVariableAssignment(selectCursor, nextFlowGraph, spec);
                            if (unwrappedSelect instanceof J.Identifier) {
                                // If the select is an identifier, then we can add it to the map of variable names to flow graphs
                                String variableName = ((J.Identifier) unwrappedSelect).getSimpleName();
                                variableNameToFlowGraph.identifierToFlow.put(variableName, nextFlowGraph);
                            }
                            return variableNameToFlowGraph;
                        }
                    }
                }
                if (spec.isFlowStep(
                        previousCursor.getValue(),
                        previousCursor,
                        (Expression) ancestor,
                        ancestorCursor
                )) {
                    nextFlowGraph = nextFlowGraph.addEdge(ancestorCursor);
                    J ancestorParent = ancestorCursor.getParentTreeCursor().getValue();
                    if (ancestorParent instanceof J.Block || ancestorParent instanceof J.Case) {
                        // If the ancestor is a block or a case, then we've reached the end of the flow.
                        // We can stop here.
                        // This is important to ensure we retain the remaining `cursorPath` for the
                        // `Analysis` to have a valid starting point when a flow passes from argument to subject
                        break;
                    } else {
                        // Continue to the next ancestor
                        continue;
                    }
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
                Cursor parent = ancestorCursor.getParentOrThrow();
                if (parent.getValue() instanceof J.Switch || parent.getValue() instanceof J.SwitchExpression) {
                    // Don't add control flow to control parentheses in switch statements
                    break;
                }
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
                variable = variable.unwrap();
                if (variable instanceof J.Identifier) {
                    String nextVariableName = ((J.Identifier) variable).getSimpleName();
                    identifierToFlow.put(nextVariableName, nextFlowGraph);
                    break;
                }
            }
        }
        return new VariableNameToFlowGraph(identifierToFlow, ancestorCursor, cursorPath);
    }
}
