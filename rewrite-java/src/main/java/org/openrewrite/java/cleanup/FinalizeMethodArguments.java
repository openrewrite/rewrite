/**
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 **/
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Empty;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.java.tree.JavaType.FullyQualified.Kind;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

/**
 * Finalize method arguments v2
 */
public class FinalizeMethodArguments extends Recipe {

    @Override
    public String getDisplayName() {
        return "Finalize method arguments";
    }

    @Override
    public String getDescription() {
        return "Adds the `final` modifier keyword to method parameters.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public MethodDeclaration visitMethodDeclaration(MethodDeclaration methodDeclaration, ExecutionContext executionContext) {
                MethodDeclaration declarations = super.visitMethodDeclaration(methodDeclaration, executionContext);

                if (isWrongKind(methodDeclaration) ||
                    isEmpty(declarations.getParameters()) ||
                    hasFinalModifiers(declarations.getParameters()) ||
                    isAbstractMethod(methodDeclaration)) {
                    return declarations;
                }

                final AtomicBoolean assigned = new AtomicBoolean(false);

                methodDeclaration.getParameters().forEach(p -> checkIfAssigned(assigned, p));
                if (assigned.get()) {
                    return declarations;
                }

                List<Statement> parameters = ListUtils.map(declarations.getParameters(), FinalizeMethodArguments::updateParam);
                declarations = declarations.withParameters(parameters);
                return declarations;
            }

            private void checkIfAssigned(final AtomicBoolean assigned, final Statement p) {
                if (p instanceof VariableDeclarations) {
                    VariableDeclarations variableDeclarations = (VariableDeclarations) p;
                    if (variableDeclarations.getVariables().stream()
                            .anyMatch(namedVariable ->
                                    FindAssignmentReferencesToVariable.find(getCursor()
                                                            .getParentTreeCursor()
                                                            .getValue(),
                                                    namedVariable)
                                            .get())) {
                        assigned.set(true);
                    }
                }
            }

            @Override
            public boolean isAcceptable(final SourceFile sourceFile, final ExecutionContext executionContext) {
                return sourceFile instanceof JavaSourceFile;
            }
        };
    }

    private static boolean isWrongKind(final MethodDeclaration methodDeclaration) {
        return Optional.ofNullable(methodDeclaration.getMethodType())
                .map(Method::getDeclaringType)
                .map(FullyQualified::getKind)
                .filter(Kind.Interface::equals)
                .isPresent();
    }

    private static boolean isAbstractMethod(MethodDeclaration method) {
        return method.getModifiers().stream().anyMatch(modifier -> modifier.getType() == Type.Abstract);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class FindAssignmentReferencesToVariable extends JavaIsoVisitor<AtomicBoolean> {

        VariableDeclarations.NamedVariable variable;

        /**
         * @param subtree  The subtree to search.
         * @param variable A {@link J.VariableDeclarations.NamedVariable} to check for any reassignment calls.
         * @return An {@link AtomicBoolean} that is true if the variable has been reassigned and false otherwise.
         */
        static AtomicBoolean find(J subtree, VariableDeclarations.NamedVariable variable) {
            return new FindAssignmentReferencesToVariable(variable)
                    .reduce(subtree, new AtomicBoolean());
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment a, AtomicBoolean hasAssignment) {
            // Return quickly if the variable has been reassigned before
            if (hasAssignment.get()) {
                return a;
            }

            J.Assignment assignment = super.visitAssignment(a, hasAssignment);

            if (assignment.getVariable() instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) assignment.getVariable();

                if (identifier.getSimpleName().equals(variable.getSimpleName())) {
                    hasAssignment.set(true);
                }
            }

            return assignment;
        }
    }

    private static Statement updateParam(final Statement p) {
        if (p instanceof VariableDeclarations) {
            VariableDeclarations variableDeclarations = (VariableDeclarations) p;
            if (variableDeclarations.getModifiers().isEmpty()) {
                variableDeclarations = updateModifiers(variableDeclarations, !((VariableDeclarations) p).getLeadingAnnotations().isEmpty());
                variableDeclarations = updateDeclarations(variableDeclarations);
                return variableDeclarations;
            }
        }
        return p;
    }

    private static VariableDeclarations updateDeclarations(final VariableDeclarations variableDeclarations) {
        return variableDeclarations.withTypeExpression(variableDeclarations.getTypeExpression() != null ?
                variableDeclarations.getTypeExpression().withPrefix(Space.SINGLE_SPACE) : null);
    }

    private static VariableDeclarations updateModifiers(final VariableDeclarations variableDeclarations, final boolean leadingAnnotations) {
        List<Modifier> modifiers = variableDeclarations.getModifiers();
        Modifier finalModifier = new Modifier(Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Type.Final,
                emptyList());
        if (leadingAnnotations) {
            finalModifier = finalModifier.withPrefix(Space.SINGLE_SPACE);
        }
        return variableDeclarations.withModifiers(ListUtils.concat(finalModifier, modifiers));
    }

    private boolean hasFinalModifiers(final List<Statement> parameters) {
        return parameters.stream().allMatch(p -> {
            if (p instanceof VariableDeclarations) {
                final List<Modifier> modifiers = ((VariableDeclarations) p).getModifiers();
                return !modifiers.isEmpty()
                       && modifiers.stream()
                               .allMatch(m -> m.getType().equals(Type.Final));
            }
            return false;
        });
    }

    private boolean isEmpty(final List<Statement> parameters) {
        return parameters.size() == 1 && (parameters.get(0) instanceof Empty);
    }
}
