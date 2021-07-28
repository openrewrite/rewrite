/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Determines if an assignment to an identifier has any read operations performed on it.
 * Any read operations performed on an assignment LHS (left-hand side) means the assignment RHS (right-hand side) is used.
 * <p>
 * If an assignment is overwritten by a reassignment later, the previous assignment may need to be removed.
 * In that case, we need to look at whether there are read operations performed on the LHS assignment
 * in the scope between the assignment and the reassignment.
 * <p>
 * If there are read operations, we should not remove the assignment. But if there are not any LHS read operations
 * between the first assignment and the reassignment, then we can prune the original assignment. It's dead code.
 */
@Incubating(since = "7.10.0")
@SuppressWarnings("AnonymousInnerClassMayBeStatic")
public class RemoveUnusedAssignments extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unused assignments";
    }

    @Override
    public String getDescription() {
        return "An assignment is unused when a local variable is assigned a value that is not read by any subsequent instruction. " +
                "Calculating a value followed by overwriting it could indicate a coding error, or at least a waste of resources. " +
                "Assignments without a subsequent read which are overwritten by a reassignment are removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1854");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final Map<String, List<J.Assignment>> identifierAssignments = new HashMap<>();
                final List<J.VariableDeclarations.NamedVariable> variablesDeclaredWithinScope = new ArrayList<>();

                final JavaIsoVisitor<ExecutionContext> markAssignments = new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        variablesDeclaredWithinScope.addAll(multiVariable.getVariables());
                        return super.visitVariableDeclarations(multiVariable, ctx);
                    }

                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                        if (assignment.getVariable() instanceof J.Identifier) {
                            J.Identifier lhs = (J.Identifier) assignment.getVariable();
                            if (variablesDeclaredWithinScope.stream().anyMatch(vd -> vd.getName().getSimpleName().equals(lhs.getSimpleName()))) {
                                identifierAssignments.computeIfAbsent(lhs.getSimpleName(), f -> new ArrayList<>()).add(assignment);
                            }
                        }
                        return super.visitAssignment(assignment, ctx);
                    }
                };
                markAssignments.visit(method, ctx);

                final JavaIsoVisitor<ExecutionContext> sweepAssignments = new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                        if (assignment.getVariable() instanceof J.Identifier) {
                            J.Identifier lhs = (J.Identifier) assignment.getVariable();
                            if (identifierAssignments.containsKey(lhs.getSimpleName())) {
                                List<J.Assignment> assignments = identifierAssignments.get(lhs.getSimpleName());
                                if (assignments.contains(assignment)) {
                                    ListIterator<J.Assignment> assignmentIterator = assignments.listIterator(assignments.indexOf(assignment) + 1);
                                    // "null" implying "end of list"; as in, no other assignments to this identifier.
                                    J.Assignment nextAssignment = assignmentIterator.hasNext() ? assignmentIterator.next() : null;
                                    // check for any read operations performed on the assignment identifier between the last seen assignment and the current reassignment.
                                    Set<J> readReferences = References.findRhsReferences(getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class), assignment, nextAssignment, lhs);
                                    if (readReferences.isEmpty()) {
                                        doAfterVisit(new DeleteStatement<>(assignment));
                                        assignments.remove(assignment);
                                    }
                                }
                            }
                        }
                        return super.visitAssignment(assignment, ctx);
                    }
                };
                method = (J.MethodDeclaration) sweepAssignments.visit(method, ctx);

                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }

    private static class References {
        private static Set<J> findRhsReferences(J j, @Nullable Tree startAt, @Nullable Tree stopAt, J.Identifier target) {
            final AtomicBoolean withinScope = new AtomicBoolean(false);
            final Set<J> refs = new HashSet<>();
            new JavaIsoVisitor<Set<J>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Set<J> ctx) {
                    J.Identifier i = super.visitIdentifier(identifier, ctx);
                    if ((startAt != null || stopAt != null) && !withinScope.get()) {
                        return i;
                    }
                    if (i.getSimpleName().equals(target.getSimpleName())) {
                        J parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
                        if (parent instanceof J.Assignment) {
                            J.Assignment parentTree = (J.Assignment) parent;
                            if (!parentTree.getVariable().isScope(i)) {
                                ctx.add(parentTree);
                            }
                        } else if (parent instanceof J.AssignmentOperation) {
                            J.AssignmentOperation parentTree = (J.AssignmentOperation) parent;
                            if (parentTree.getVariable().isScope(i)) {
                                assert getCursor().getParent() != null;
                                J grandParent = getCursor().getParent().dropParentUntil(J.class::isInstance).getValue();
                                if (grandParent instanceof Expression || grandParent instanceof J.Return) {
                                    ctx.add(grandParent);
                                }
                            } else {
                                ctx.add(parent);
                            }
                        } else if (parent instanceof J.VariableDeclarations.NamedVariable) {
                            J.VariableDeclarations.NamedVariable parentTree = (J.VariableDeclarations.NamedVariable) parent;
                            if (!parentTree.getName().getSimpleName().equals(target.getSimpleName())) {
                                ctx.add(parentTree);
                            }
                        } else {
                            ctx.add(parent);
                        }
                    }
                    return i;
                }

                @Nullable
                @Override
                public J visit(@Nullable Tree tree, Set<J> ctx) {
                    if (startAt != null && startAt.isScope(tree)) {
                        withinScope.set(true);
                    }
                    if (stopAt != null && stopAt.isScope(tree)) {
                        withinScope.set(false);
                        return (J) tree;
                    }
                    return super.visit(tree, ctx);
                }

            }.visit(j, refs);
            return refs;
        }
    }

}
