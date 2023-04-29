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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;

public class UseSystemLineSeparator extends Recipe {
    private static final MethodMatcher GET_PROPERTY = new MethodMatcher("java.lang.System getProperty(..)");
    private static final String LINE_SEPARATOR = "line.separator";

    @Override
    public String getDisplayName() {
        return "Use `System.lineSeparator()`";
    }

    @Override
    public String getDescription() {
        return "Replace calls to `System.getProperty(\"line.separator\")` with `System.lineSeparator()`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GET_PROPERTY), new JavaVisitor<ExecutionContext>() {
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation invocation = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (GET_PROPERTY.matches(method)) {
                    String param = "";
                    if (method.getArguments().size() == 1 && method.getArguments().get(0) instanceof J.Literal) {
                        Object value = ((J.Literal) method.getArguments().get(0)).getValue();
                        if (value == null) {
                            return method;
                        }
                        param = value.toString();
                    }
                    if (!LINE_SEPARATOR.equals(param)) {
                        return invocation;
                    }

                    if (method.getSelect() != null) {
                        final JavaTemplate template = JavaTemplate
                                .builder(this::getCursor, "#{any(java.lang.System)}.lineSeparator()")
                                .build();

                        return method.withTemplate(template,
                                method.getCoordinates().replace(),
                                method.getSelect());
                    } else {
                        // static import scenario
                        maybeRemoveImport("java.lang.System.getProperty");
                        maybeAddImport("java.lang.System", "lineSeparator");

                        final JavaTemplate template = JavaTemplate
                                .builder(this::getCursor, "lineSeparator()")
                                .staticImports("java.lang.System.lineSeparator")
                                .build();

                        return method.withTemplate(template,
                                method.getCoordinates()
                                        .replace());
                    }
                }

                return invocation;
            }
        });
    }
}
