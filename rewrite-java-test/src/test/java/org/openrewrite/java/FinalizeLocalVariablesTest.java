/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import lombok.EqualsAndHashCode;
import lombok.Value;

class FinalizeLocalVariablesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FinalizeLocalVariables());
    }

    @ParameterizedTest
    @ValueSource(ints = { 11, 17, 21 })
    @Issue("https://github.com/openrewrite/rewrite/issues/3744")
    void missingTypePlusVarNotMissingSpace(int javaVersion) {
        rewriteRun(
          s -> s.typeValidationOptions(TypeValidation.none()),
          //language=java
          version(
            java(
              """
                import foo.Unknown;
                class Bar {
                    void test(Unknown b) {
                        var a = b;
                    }
                }
                """,
              """
                import foo.Unknown;
                class Bar {
                    void test(Unknown b) {
                        final var a = b;
                    }
                }
                """
            ), javaVersion
          )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = { 11, 17, 21 })
    @Issue("https://github.com/openrewrite/rewrite/issues/3744")
    void explicitTypePlusVarNotMissingSpace(int javaVersion) {
        rewriteRun(
          s -> s.typeValidationOptions(TypeValidation.none()),
          //language=java
          version(
            java(
              """
                class Bar {
                    void test(String b) {
                        var a = b;
                    }
                }
                """,
              """
                class Bar {
                    void test(String b) {
                        final var a = b;
                    }
                }
                """
            ), javaVersion
          )
        );
    }

    public static class FinalizeLocalVariables extends Recipe {
        @Override
        public String getDisplayName() {
            return "Finalize local variables";
        }

        @Override
        public String getDescription() {
            return "Adds the `final` modifier keyword to local variables which are not reassigned.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                                        ExecutionContext ctx) {
                    J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                    // if this already has "final", we don't need to bother going any further; we're done
                    if (mv.hasModifier(J.Modifier.Type.Final)) {
                        return mv;
                    }

                    // consider uninitialized local variables non-final
                    if (mv.getVariables().stream().anyMatch(nv -> nv.getInitializer() == null)) {
                        return mv;
                    }

                    if (isDeclaredInForLoopControl(getCursor())) {
                        return mv;
                    }

                    // ignore fields (aka "instance variable" or "class variable")
                    if (mv.getVariables().stream().anyMatch(v -> v.isField(getCursor()))) {
                        return mv;
                    }

                    // ignores anonymous class fields, contributed code for issue #t
                    if (this.getCursorToParentScope(this.getCursor()).getValue() instanceof J.NewClass) {
                        return mv;
                    }

                    if (mv.getVariables().stream()
                          .noneMatch(v -> {
                              Cursor declaringCursor = v.getDeclaringScope(getCursor());
                              return FindAssignmentReferencesToVariable.find(declaringCursor.getValue(), v)
                                                                       .get();
                          })) {
                        mv = autoFormat(
                          mv.withModifiers(
                            ListUtils.concat(mv.getModifiers(),
                                             new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null,
                                                            J.Modifier.Type.Final, Collections.emptyList()))
                          ), ctx);
                    }

                    return mv;
                }

                private Cursor getCursorToParentScope(final Cursor cursor) {
                    return cursor.dropParentUntil(
                      is -> is instanceof J.NewClass || is instanceof J.ClassDeclaration);
                }
            };
        }

        private boolean isDeclaredInForLoopControl(Cursor cursor) {
            return cursor.getParentTreeCursor()
                         .getValue() instanceof J.ForLoop.Control;
        }

        @Value
        @EqualsAndHashCode(callSuper = false)
        private static class FindAssignmentReferencesToVariable extends JavaIsoVisitor<AtomicBoolean> {

            J.VariableDeclarations.NamedVariable variable;

            /**
             * @param j        The subtree to search.
             * @param variable A {@link J.VariableDeclarations.NamedVariable} to check for any reassignment calls.
             * @return An {@link AtomicBoolean} that is true if the variable has been reassigned and false otherwise.
             */
            static AtomicBoolean find(J j, J.VariableDeclarations.NamedVariable variable) {
                return new FindAssignmentReferencesToVariable(variable)
                  .reduce(j, new AtomicBoolean());
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean hasAssignment) {
                if (hasAssignment.get()) {
                    return assignment;
                }
                J.Assignment a = super.visitAssignment(assignment, hasAssignment);
                if (a.getVariable() instanceof J.Identifier) {
                    J.Identifier i = (J.Identifier) a.getVariable();
                    if (i.getSimpleName().equals(variable.getSimpleName())) {
                        hasAssignment.set(true);
                    }
                }
                return a;
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp,
                                                                  AtomicBoolean hasAssignment) {
                if (hasAssignment.get()) {
                    return assignOp;
                }

                J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, hasAssignment);
                if (a.getVariable() instanceof J.Identifier) {
                    J.Identifier i = (J.Identifier) a.getVariable();
                    if (i.getSimpleName().equals(variable.getSimpleName())) {
                        hasAssignment.set(true);
                    }
                }
                return a;
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, AtomicBoolean hasAssignment) {
                if (hasAssignment.get()) {
                    return unary;
                }

                J.Unary u = super.visitUnary(unary, hasAssignment);
                if (u.getOperator().isModifying() && u.getExpression() instanceof J.Identifier) {
                    J.Identifier i = (J.Identifier) u.getExpression();
                    if (i.getSimpleName().equals(variable.getSimpleName())) {
                        hasAssignment.set(true);
                    }
                }
                return u;
            }
        }
    }
}
