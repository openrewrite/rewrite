/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * FinalizeLocalVariables will add the "final" modifier keyword to local variables which are not being reassigned.
 * A local variable is defined as being declared between the opening and closing braces of a method.
 * <p>
 * (Note, this is not related to the "finalize" method provided by the root Object class. The "Finalize" in the name
 * just refers to making a local variable "final".)
 * <p>
 * For the time being, we keep the definition of "local variable" strictly to the technical definition of a variable
 * declared between the opening and closing braces of a method. This means a method parameter variable will not be
 * considered as a target for adding the "final" modifier; nor instance variables (non-static fields) nor class variables (static fields).
 * <p>
 * See the official <a href="https://docs.oracle.com/javase/tutorial/java/nutsandbolts/variables.html">variables tutorial</a>.
 * <p>
 * There are specific situations which count as a "local variable". In fact, "local variable" has a specific
 * technical definition, even though it is occasionally referred to when discussing other things (such as method parameter variables, or
 * class variables ("static fields"), or instance variables ("non-static fields"), etc.).
 * By the technical definition, a "local variable" is defined by the location it is declared.
 */
@Incubating(since = "7.0.0")
public class FinalizeLocalVariablesVisitor<P> extends JavaIsoVisitor<P> {

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations mv = visitAndCast(multiVariable, p, super::visitVariableDeclarations);

        // if this already has "final", we don't need to bother going any further; we're done
        if (mv.hasModifier(J.Modifier.Type.Final)) {
            return mv;
        }

        // we don't try to finalize method parameter variables
        if (isMethodParameterVariable()) {
            return mv;
        }

        // we don't want to add a "final" keyword to a lambda parameter variable declaration
        if (isDeclaredInLambda()) {
            return mv;
        }

        // do not add a "final" keyword to a catch block
        if (isDeclaredInCatch()) {
            return mv;
        }

        if(isDeclaredInForLoopControl()) {
            return mv;
        }

        // ignore fields (aka "instance variable" or "class variable")
        if (mv.getVariables().stream().anyMatch(v -> v.isField(getCursor()) || isField(getCursor()))) {
            return mv;
        }

        Predicate<J.VariableDeclarations.NamedVariable> hasReassignment;
        if (isDeclaredInForEachLoop()) {
            // ForEach loops (aka "enhanced for-loop") will always have the variable initialized
            hasReassignment = v -> !FindAssignmentReferencesToVariable.find(getCursor().firstEnclosingOrThrow(J.ForEachLoop.class), v).isEmpty();
        } else {
            // off-sets number of acceptable "reassignments" depending on whether there's an initializer at declaration
            hasReassignment = v -> FindAssignmentReferencesToVariable.find(getCursor().dropParentUntil(J.class::isInstance).getValue(), v).size() + (v.getInitializer() == null ? -1 : 0) > 0;
        }

        if (mv.getVariables().stream().noneMatch(hasReassignment)) {
            mv = maybeAutoFormat(mv,
                    mv.withModifiers(
                            ListUtils.concat(mv.getModifiers(), new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Final, Collections.emptyList()))
                    ), p, getCursor().dropParentUntil(J.class::isInstance));
        }

        return mv;
    }

    private boolean isMethodParameterVariable() {
        return getCursor()
                .dropParentUntil(J.class::isInstance) // maybe J.MethodDeclaration
                .getValue() instanceof J.MethodDeclaration;
    }

    private boolean isDeclaredInForLoopControl() {
        return getCursor()
                .dropParentUntil(J.class::isInstance)
                .getValue() instanceof J.ForLoop.Control;
    }

    private boolean isDeclaredInForEachLoop() {
        return getCursor()
                .dropParentUntil(J.class::isInstance) // maybe J.ForEachLoop.Control
                .dropParentUntil(J.class::isInstance) // maybe J.ForEachLoop
                .getValue() instanceof J.ForEachLoop;
    }

    private boolean isDeclaredInLambda() {
        return getCursor()
                .dropParentUntil(J.class::isInstance) // maybe J.Lambda
                .getValue() instanceof J.Lambda;
    }

    private boolean isDeclaredInCatch() {
        return getCursor()
                .dropParentUntil(J.class::isInstance) // maybe J.ControlParentheses
                .dropParentUntil(J.class::isInstance) // maybe J.Try.Catch
                .getValue() instanceof J.Try.Catch;
    }

    private boolean isField(Cursor cursor) {
        return cursor
                // .getParentOrThrow() // JRightPadded
                // .getParentOrThrow() // J.VariableDeclarations
                .getParentOrThrow() // JRightPadded
                .getParentOrThrow() // J.Block
                .getParentOrThrow() // maybe J.ClassDeclaration
                .getValue() instanceof J.ClassDeclaration;
    }


    private static class FindAssignmentReferencesToVariable {
        private FindAssignmentReferencesToVariable() {
        }

        /**
         * @param j        The subtree to search.
         * @param variable A {@link J.VariableDeclarations.NamedVariable} to check for any reassignment calls.
         * @return A set of {@link NameTree} locations of reassignment calls to this variable.
         */
        private static Set<NameTree> find(J j, J.VariableDeclarations.NamedVariable variable) {
            JavaIsoVisitor<Set<NameTree>> findVisitor = new JavaIsoVisitor<Set<NameTree>>() {
                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, Set<NameTree> ctx) {
                    J.Assignment a = super.visitAssignment(assignment, ctx);
                    if (a.getVariable() instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) a.getVariable();
                        if (i.getSimpleName().equals(variable.getSimpleName())) {
                            ctx.add(i);
                        }
                    }
                    return a;
                }

                @Override
                public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Set<NameTree> ctx) {
                    J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, ctx);
                    if (a.getVariable() instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) a.getVariable();
                        if (i.getSimpleName().equals(variable.getSimpleName())) {
                            ctx.add(i);
                        }
                    }
                    return a;
                }

                @Override
                public J.Unary visitUnary(J.Unary unary, Set<NameTree> ctx) {
                    J.Unary u = super.visitUnary(unary, ctx);
                    if (u.getExpression() instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) u.getExpression();
                        if (i.getSimpleName().equals(variable.getSimpleName())) {
                            ctx.add(i);
                        }
                    }
                    return u;
                }

            };

            Set<NameTree> refs = new HashSet<>();
            findVisitor.visit(j, refs);
            return refs;
        }
    }
}
