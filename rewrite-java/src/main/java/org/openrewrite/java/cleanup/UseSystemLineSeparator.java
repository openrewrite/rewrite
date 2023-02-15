/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;

public class UseSystemLineSeparator extends Recipe {

    private static final MethodMatcher GET_PROPERTY = new MethodMatcher("java.lang.System getProperty(..)");

    @Override
    public String getDisplayName() {
        return "Use `System.lineSeparator()`";
    }

    @Override
    public String getDescription() {
        return "Replaces calls to `System.getProperty(\"line.separator\")` with `System.lineSeparator()`.";
    }
    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(GET_PROPERTY);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            final JavaTemplate.Builder template = JavaTemplate
                    .builder(this::getCursor, "#{any(java.lang.System)}.lineSeparator()");

            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J invocation = super.visitMethodInvocation(method, ctx);
                if ("System.getProperty(\"line.separator\")".equalsIgnoreCase(invocation.toString())) {
                    return method.withTemplate(template.build(),
                            method.getCoordinates().replace(),
                            method.getSelect());
                } else if ("getProperty(\"line.separator\")".equalsIgnoreCase(invocation.toString()) &&
                        method.getMethodType() != null && System.class.getName().equals(method.getMethodType().getDeclaringType().getFullyQualifiedName())) {

                    maybeRemoveImport("java.lang.System.getProperty");
                    maybeAddImport("java.lang.System","lineSeparator");

                    final JavaTemplate template = JavaTemplate
                            .builder(this::getCursor, "lineSeparator()")
                            .staticImports("java.lang.System.lineSeparator")
                            .build();

                    return method.withTemplate(template,
                            method.getCoordinates()
                                    .replace());

                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
