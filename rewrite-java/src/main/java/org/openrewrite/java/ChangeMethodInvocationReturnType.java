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
                    type = type.withReturnType(TypeUtils.buildTypeTree(newReturnType).getType());
                    m = m.withMethodType(type);
                    if (m.getName().getType() != null) {
                        m = m.withName(m.getName().withType(type));
                    }
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

                if (methodUpdated && initializedByMatch) {
                    TypeTree newTypeTree = TypeUtils.buildTypeTree(newReturnType);
                    JavaType newType = newTypeTree.getType();

                    maybeRemoveImports(originalType);

                    if (mv.getTypeExpression() != null) {
                        TypeTree newTypeExpression = newTypeTree.withPrefix(mv.getTypeExpression().getPrefix());
                        mv = mv.withTypeExpression(newTypeExpression);
                        // The tree is built with fully-qualified names; shorten them and add imports for the
                        // new type and all of its type parameters (recursively for nested generics).
                        if (!(newTypeExpression instanceof J.Identifier) && !(newTypeExpression instanceof J.Primitive)) {
                            doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(newTypeExpression));
                        }
                    }

                    mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                        JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                        if (varType != null && !varType.equals(newType)) {
                            return var.withType(newType).withName(var.getName().withType(newType));
                        }
                        return var;
                    }));
                }

                return mv;
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
