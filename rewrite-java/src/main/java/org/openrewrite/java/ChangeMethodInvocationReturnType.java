/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method invocation return type",
            description = "The fully qualified new return type of method invocation. " +
                    "Parameterized types like `java.util.Set<java.lang.String>` are supported.",
            example = "long")
    String newReturnType;

    String displayName = "Change method invocation return type";

    String description = "Changes the return type of a method invocation.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !newReturnType.equals(type.getReturnType().toString())) {
                    methodUpdated = true;
                }
                return m;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                methodUpdated = false;
                JavaType originalType = multiVariable.getType();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                // Only change the declared type when a variable's initializer is itself the matched
                // method invocation. A match nested deeper (e.g. as an argument to another call, such
                // as `Cell c = row.createCell(i, other.getCellType())`) must not change the variable type.
                boolean initializedByMatch = mv.getVariables().stream()
                        .anyMatch(v -> isInitializedByMatch(v.getInitializer()));

                if (methodUpdated && initializedByMatch && mv.getTypeExpression() != null) {
                    TypeTree originalTypeExpression = mv.getTypeExpression();
                    maybeRemoveImports(originalType);

                    // Parse the new return type in type position via JavaTemplate so that it is resolved against
                    // the classpath (properly attributed) and its imports are added, rather than synthesizing a
                    // shallow type tree by hand. The resolved type expression is then spliced into the existing
                    // declaration, preserving its modifiers, variable names and initializer.
                    // `ShortenFullyQualifiedTypeReferences` intentionally leaves `java.lang` references qualified,
                    // so render direct `java.lang` members with their simple (implicitly imported) name up front.
                    String templateType = newReturnType.replaceAll("\\bjava\\.lang\\.([A-Z][A-Za-z0-9_]*)(?![.A-Za-z0-9_])", "$1");
                    J.VariableDeclarations resolved = JavaTemplate.builder(templateType + " __cmirt__")
                            .contextSensitive()
                            .build()
                            .apply(updateCursor(mv), mv.getCoordinates().replace());
                    TypeTree newTypeExpression = resolved.getTypeExpression();
                    // Only rewrite when the new type actually resolved against the classpath; if it could not be
                    // resolved (e.g. it is not on the classpath) leave the declaration untouched rather than
                    // introducing an unattributed type that would never stabilize.
                    if (newTypeExpression != null && !(newTypeExpression.getType() instanceof JavaType.Unknown)) {
                        JavaType newType = newTypeExpression.getType();
                        TypeTree splicedTypeExpression = newTypeExpression.withPrefix(originalTypeExpression.getPrefix());
                        mv = mv.withTypeExpression(splicedTypeExpression);
                        // The template renders fully-qualified names; shorten them and add the corresponding
                        // imports (for the raw type and every type parameter).
                        if (!(splicedTypeExpression instanceof J.Primitive)) {
                            doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(splicedTypeExpression));
                        }
                        mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                            if (isInitializedByMatch(var.getInitializer())) {
                                var = var.withInitializer(updateMatchedReturnType(var.getInitializer(), newType));
                            }
                            JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                            if (varType != null && !varType.equals(newType)) {
                                var = var.withType(newType).withName(var.getName().withType(newType));
                            }
                            return var;
                        }));
                    }
                }

                return mv;
            }

            /**
             * Re-attribute the return type of every matched invocation within the initializer to the resolved
             * type, preserving wrapping parentheses and ternaries. This keeps the variable's declared type and
             * its initializer consistent and lets the recipe stabilize in a single cycle.
             */
            private @Nullable Expression updateMatchedReturnType(@Nullable Expression initializer, @Nullable JavaType newType) {
                if (initializer == null) {
                    return null;
                }
                return (Expression) new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, p);
                        if (methodMatcher.matches(m) && m.getMethodType() != null) {
                            JavaType.Method type = m.getMethodType().withReturnType(newType);
                            m = m.withMethodType(type);
                            if (m.getName().getType() != null) {
                                m = m.withName(m.getName().withType(type));
                            }
                        }
                        return m;
                    }
                }.visit(initializer, 0);
            }

            /**
             * Remove the imports for a type that is being replaced, walking parameterized types, arrays and
             * wildcard bounds so that imports introduced for type parameters are also cleaned up when no longer
             * referenced.
             */
            private void maybeRemoveImports(@Nullable JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    maybeRemoveImport(parameterized.getType());
                    for (JavaType typeParameter : parameterized.getTypeParameters()) {
                        maybeRemoveImports(typeParameter);
                    }
                } else if (type instanceof JavaType.Array) {
                    maybeRemoveImports(((JavaType.Array) type).getElemType());
                } else if (type instanceof JavaType.GenericTypeVariable) {
                    for (JavaType bound : ((JavaType.GenericTypeVariable) type).getBounds()) {
                        maybeRemoveImports(bound);
                    }
                } else if (type instanceof JavaType.FullyQualified) {
                    maybeRemoveImport((JavaType.FullyQualified) type);
                }
            }

            /**
             * Returns true when the matched invocation is the direct initializer, is inside
             * wrapping parentheses (stripped before checking), or is a branch of a ternary —
             * in all of those positions the invocation determines the variable's type.
             */
            private boolean isInitializedByMatch(@Nullable Expression expression) {
                if (expression == null) {
                    return false;
                }
                Expression unwrapped = expression.unwrap();
                if (unwrapped instanceof J.MethodInvocation) {
                    return methodMatcher.matches((J.MethodInvocation) unwrapped);
                }
                if (unwrapped instanceof J.Ternary) {
                    J.Ternary ternary = (J.Ternary) unwrapped;
                    return isInitializedByMatch(ternary.getTruePart()) ||
                            isInitializedByMatch(ternary.getFalsePart());
                }
                return false;
            }
        };
    }
}
