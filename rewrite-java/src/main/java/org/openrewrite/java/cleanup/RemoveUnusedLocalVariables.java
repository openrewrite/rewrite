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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RemoveUnusedLocalVariables extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unused local variables";
    }

    @Override
    public String getDescription() {
        return "If a local variable is declared but not used, it is dead code and should be removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1481");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveUnusedLocalVariablesVisitor();
    }

    private static class RemoveUnusedLocalVariablesVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static Cursor getCursorToParentScope(Cursor cursor) {
            return cursor.dropParentUntil(is ->
                    is instanceof J.Block ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.ForLoop ||
                            is instanceof J.ForEachLoop ||
                            is instanceof J.ForLoop.Control ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.MultiCatch ||
                            is instanceof J.Lambda
            );
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            Cursor parentScope = getCursorToParentScope(getCursor());
            if (!(parentScope.getParent() != null &&
                    // skip if class declaration field
                    parentScope.getParent().getValue() instanceof J.ClassDeclaration) &&
                    // skip if method declaration parameter
                    !(parentScope.getValue() instanceof J.MethodDeclaration) &&
                    // skip if defined in an enhanced for loop, since there isn't much we can do about the semantics at that point
                    !(parentScope.getValue() instanceof J.ForEachLoop) &&
                    // skip if try resource
                    !(parentScope.getValue() instanceof J.Try) &&
                    // skip if defined in a try's catch clause
                    !(parentScope.getValue() instanceof J.Try.Catch || parentScope.getValue() instanceof J.MultiCatch) &&
                    // skip if defined as a parameter to a lambda expression
                    !(parentScope.getValue() instanceof J.Lambda)) {
                if (FindReadReferencesToVariable.find(parentScope.getValue(), variable).isEmpty()) {
                    FindAssignmentReferencesToVariable.find(parentScope.getValue(), variable).forEach(ref -> doAfterVisit(new DeleteStatement<>(ref)));
                    return null;
                }
            }

            return super.visitVariable(variable, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
            if (mv.getVariables().isEmpty()) {
                doAfterVisit(new DeleteStatement<>(mv));
            }
            return mv;
        }

    }

    private static class FindReadReferencesToVariable {
        private FindReadReferencesToVariable() {
        }

        /**
         * @param j        The subtree to search.
         * @param variable A {@link J.VariableDeclarations.NamedVariable} to check for any read calls.
         * @return A set of {@link NameTree} locations of read-access calls to this variable.
         */
        public static Set<NameTree> find(J j, J.VariableDeclarations.NamedVariable variable) {
            JavaIsoVisitor<Set<NameTree>> findVisitor = new JavaIsoVisitor<Set<NameTree>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Set<NameTree> ctx) {
                    J.Identifier i = super.visitIdentifier(identifier, ctx);
                    if (i.getSimpleName().equals(variable.getSimpleName())) {
                        assert getCursor().getParent() != null;
                        Object parent = getCursor().getParent().getValue();
                        if (!(parent instanceof J.Assignment || parent instanceof J.AssignmentOperation || parent instanceof J.VariableDeclarations.NamedVariable)) {
                            ctx.add(i);
                        }
                    }
                    return i;
                }
            };

            Set<NameTree> refs = new HashSet<>();
            findVisitor.visit(j, refs);
            return refs;
        }
    }

    private static class FindAssignmentReferencesToVariable {
        private FindAssignmentReferencesToVariable() {
        }

        /**
         * @param j        The subtree to search.
         * @param variable A {@link J.VariableDeclarations.NamedVariable} to check for any reassignment calls.
         * @return A set of {@link Statement} locations of reassignment calls to this variable.
         */
        private static Set<Statement> find(J j, J.VariableDeclarations.NamedVariable variable) {
            JavaIsoVisitor<Set<Statement>> findVisitor = new JavaIsoVisitor<Set<Statement>>() {
                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, Set<Statement> ctx) {
                    J.Assignment a = super.visitAssignment(assignment, ctx);
                    if (a.getVariable() instanceof J.Identifier) {
                        J.Identifier i = ((J.Identifier) a.getVariable());
                        if (i.getSimpleName().equals(variable.getSimpleName())) {
                            ctx.add(assignment);
                        }
                    }
                    return a;
                }

                @Override
                public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Set<Statement> ctx) {
                    J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, ctx);
                    if (a.getVariable() instanceof J.Identifier) {
                        J.Identifier i = ((J.Identifier) a.getVariable());
                        if (i.getSimpleName().equals(variable.getSimpleName())) {
                            ctx.add(assignOp);
                        }
                    }
                    return a;
                }

                @Override
                public J.Unary visitUnary(J.Unary unary, Set<Statement> ctx) {
                    J.Unary u = super.visitUnary(unary, ctx);
                    if (u.getExpression() instanceof J.Identifier) {
                        J.Identifier i = ((J.Identifier) u.getExpression());
                        if (i.getSimpleName().equals(variable.getSimpleName())) {
                            ctx.add(unary);
                        }
                    }
                    return u;
                }

            };

            Set<Statement> refs = new HashSet<>();
            findVisitor.visit(j, refs);
            return refs;
        }
    }

}
