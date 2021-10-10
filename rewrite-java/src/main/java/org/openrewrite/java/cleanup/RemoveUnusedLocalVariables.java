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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("ConstantConditions")
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
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
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
                            is instanceof J.ForEachLoop.Control ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Resource ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.MultiCatch ||
                            is instanceof J.Lambda
            );
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            Cursor parentScope = getCursorToParentScope(getCursor());
            J parent = parentScope.getValue();
            if (parentScope.getParent() == null ||
                    // skip class instance variables
                    parentScope.getParent().getValue() instanceof J.ClassDeclaration ||
                    // skip anonymous class instance variables
                    parentScope.getParent().getValue() instanceof J.NewClass ||
                    // skip if method declaration parameter
                    parent instanceof J.MethodDeclaration ||
                    // skip if defined in an enhanced or standard for loop, since there isn't much we can do about the semantics at that point
                    parent instanceof J.ForLoop.Control || parent instanceof J.ForEachLoop.Control ||
                    // skip if defined in a try's catch clause as an Exception variable declaration
                    parent instanceof J.Try.Resource || parent instanceof J.Try.Catch || parent instanceof J.MultiCatch ||
                    // skip if defined as a parameter to a lambda expression
                    parent instanceof J.Lambda
            ) {
                return variable;
            }

            List<J> readReferences = References.findRhsReferences(parentScope.getValue(), variable.getName());
            if (readReferences.isEmpty()) {
                List<Statement> assignmentReferences = References.findLhsReferences(parentScope.getValue(), variable.getName());
                for (Statement ref : assignmentReferences) {
                    doAfterVisit(new DeleteStatement<>(ref));
                }
                return null;
            }

            return super.visitVariable(variable, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            if (!multiVariable.getAllAnnotations().isEmpty()) {
                return multiVariable;
            }

            J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
            if (mv.getVariables().isEmpty()) {
                doAfterVisit(new DeleteStatement<>(mv));
            }
            return mv;
        }

    }

    private static class References {
        private static final J.Unary.Type[] incrementKinds = {
                J.Unary.Type.PreIncrement,
                J.Unary.Type.PreDecrement,
                J.Unary.Type.PostIncrement,
                J.Unary.Type.PostDecrement
        };
        private static final Predicate<Cursor> isUnaryIncrementKind = t -> t.getValue() instanceof J.Unary && isIncrementKind(t);

        private static boolean isIncrementKind(Cursor tree) {
            if (tree.getValue() instanceof J.Unary) {
                J.Unary unary = tree.getValue();
                return Arrays.stream(incrementKinds).anyMatch(kind -> kind == unary.getOperator());
            }
            return false;
        }

        private static @Nullable Cursor dropParentWhile(Predicate<Object> valuePredicate, Cursor cursor) {
            while (cursor != null && valuePredicate.test(cursor.getValue())) {
                cursor = cursor.getParent();
            }
            return cursor;
        }

        private static @Nullable Cursor dropParentUntil(Predicate<Object> valuePredicate, Cursor cursor) {
            while (cursor != null && !valuePredicate.test(cursor.getValue())) {
                cursor = cursor.getParent();
            }
            return cursor;
        }

        private static boolean isRhsValue(Cursor tree) {
            if (!(tree.getValue() instanceof J.Identifier)) {
                return false;
            }

            Cursor parent = dropParentWhile(J.Parentheses.class::isInstance, tree.getParent());
            assert parent != null;
            if (parent.getValue() instanceof J.Assignment) {
                if (dropParentUntil(J.ControlParentheses.class::isInstance, parent) != null) {
                    return true;
                }
                J.Assignment assignment = parent.getValue();
                return assignment.getVariable() != tree.getValue();
            }

            if (parent.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                J.VariableDeclarations.NamedVariable namedVariable = parent.getValue();
                return namedVariable.getName() != tree.getValue();
            }

            if (parent.getValue() instanceof J.AssignmentOperation) {
                J.AssignmentOperation assignmentOperation = parent.getValue();
                if (assignmentOperation.getVariable() == tree.getValue()) {
                    J grandParent = parent.dropParentUntil(J.class::isInstance).getValue();
                    return (grandParent instanceof Expression || grandParent instanceof J.Return);
                }
            }

            return !(isUnaryIncrementKind.test(parent) && parent.dropParentUntil(J.class::isInstance).getValue() instanceof J.Block);
        }

        /**
         * An identifier is considered a right-hand side ("rhs") read operation if it is not used as the left operand
         * of an assignment, nor as the operand of a stand-alone increment.
         *
         * @param j      The subtree to search.
         * @param target A {@link J.Identifier} to check for usages.
         * @return found {@link J} locations of "right-hand" read calls.
         */
        private static List<J> findRhsReferences(J j, J.Identifier target) {
            final List<J> refs = new ArrayList<>();
            new JavaIsoVisitor<List<J>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, List<J> ctx) {
                    if (identifier.getSimpleName().equals(target.getSimpleName()) && isRhsValue(getCursor())) {
                        ctx.add(identifier);
                    }
                    return super.visitIdentifier(identifier, ctx);
                }
            }.visit(j, refs);
            return refs;
        }

        /**
         * @param j      The subtree to search.
         * @param target A {@link J.Identifier} to check for usages.
         * @return found {@link Statement} locations of "left-hand" assignment write calls.
         */
        private static List<Statement> findLhsReferences(J j, J.Identifier target) {
            JavaIsoVisitor<List<Statement>> visitor = new JavaIsoVisitor<List<Statement>>() {
                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, List<Statement> ctx) {
                    if (assignment.getVariable() instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) assignment.getVariable();
                        if (i.getSimpleName().equals(target.getSimpleName())) {
                            ctx.add(assignment);
                        }
                    }
                    return super.visitAssignment(assignment, ctx);
                }

                @Override
                public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, List<Statement> ctx) {
                    if (assignOp.getVariable() instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) assignOp.getVariable();
                        if (i.getSimpleName().equals(target.getSimpleName())) {
                            ctx.add(assignOp);
                        }
                    }
                    return super.visitAssignmentOperation(assignOp, ctx);
                }

                @Override
                public J.Unary visitUnary(J.Unary unary, List<Statement> ctx) {
                    if (unary.getExpression() instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) unary.getExpression();
                        if (i.getSimpleName().equals(target.getSimpleName())) {
                            ctx.add(unary);
                        }
                    }
                    return super.visitUnary(unary, ctx);
                }
            };

            List<Statement> refs = new ArrayList<>();
            visitor.visit(j, refs);
            return refs;
        }
    }

}
