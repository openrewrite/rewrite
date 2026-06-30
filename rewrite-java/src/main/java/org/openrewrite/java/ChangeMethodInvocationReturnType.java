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
import org.openrewrite.java.search.UsesMethod;
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
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);
        return Preconditions.check(new UsesMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !newReturnType.equals(type.getReturnType().toString())) {
                    type = type.withReturnType(JavaType.buildType(newReturnType));
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
                JavaType.FullyQualified originalType = multiVariable.getTypeAsFullyQualified();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                boolean initializedByMatch = mv.getVariables().stream()
                        .anyMatch(v -> isInitializedByMatch(v.getInitializer()));
                if (!methodUpdated || !initializedByMatch || mv.getTypeExpression() == null) {
                    return mv;
                }

                // `ShortenFullyQualifiedTypeReferences` deliberately leaves `java.lang.*` fully qualified, so strip it here.
                String templateType = newReturnType.replaceAll("\\bjava\\.lang\\.([A-Z][A-Za-z0-9_]*)(?![.A-Za-z0-9_])", "$1");
                J.VariableDeclarations resolved = JavaTemplate.builder(templateType + " __cmirt__")
                        .contextSensitive()
                        .build()
                        .apply(updateCursor(mv), mv.getCoordinates().replace());
                TypeTree newTypeExpression = resolved.getTypeExpression();
                if (newTypeExpression == null || newTypeExpression.getType() instanceof JavaType.Unknown) {
                    return mv;
                }

                JavaType newType = newTypeExpression.getType();
                maybeRemoveImport(originalType);
                mv = mv.withTypeExpression(newTypeExpression.withPrefix(mv.getTypeExpression().getPrefix()));
                if (!(newTypeExpression instanceof J.Primitive)) {
                    doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(mv.getTypeExpression()));
                }
                return mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                    JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                    if (varType != null && !varType.equals(newType)) {
                        return var.withType(newType).withName(var.getName().withType(newType));
                    }
                    return var;
                }));
            }

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
        });
    }
}
