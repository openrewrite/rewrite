/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class SimplifyDurationCreationUnits extends Recipe {

    private enum DurationUnits {
        // order is important; factors are tried in this order (largest-to-smallest)
        // (the Java spec states that `.values()` uses declaration order)
        DAYS("ofDays", ChronoUnit.DAYS),
        HOURS("ofHours", ChronoUnit.HOURS),
        MINUTES("ofMinutes", ChronoUnit.MINUTES),
        SECONDS("ofSeconds", ChronoUnit.SECONDS),
        MILLIS("ofMillis", ChronoUnit.MILLIS);

        final String methodName;
        final long millisFactor;
        final MethodMatcher methodMatcher;

        DurationUnits(String methodName, TemporalUnit unit) {
            this.methodName = methodName;
            this.millisFactor = Duration.of(1, unit).toMillis();
            this.methodMatcher = new MethodMatcher("java.time.Duration " + methodName + "(long)");
        }
    }

    @Override
    public String getDisplayName() {
        return "Simplify `java.time.Duration` units";
    }

    @Override
    public String getDescription() {
        return "Simplifies `java.time.Duration` units to be more human-readable.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesMethod<>(new MethodMatcher("java.time.Duration of*(long)"));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                method = super.visitMethodInvocation(method, ctx);

                @Nullable DurationUnits invocationUnits = null;
                for (DurationUnits maybeUnit : DurationUnits.values()) {
                    if (maybeUnit.methodMatcher.matches(method)) {
                        invocationUnits = maybeUnit;
                        break;
                    }
                }

                if (invocationUnits == null) {
                    return method;
                }

                Long invocationUnitCount = getConstantIntegralValue(method.getArguments().get(0));
                if (invocationUnitCount == null) {
                    return method;
                }

                final long millis = invocationUnitCount * invocationUnits.millisFactor;
                @Nullable DurationUnits simplifiedUnits = null;
                for (DurationUnits maybeUnit : DurationUnits.values()) {
                    if (millis % maybeUnit.millisFactor == 0) {
                        simplifiedUnits = maybeUnit;
                        break;
                    }
                }

                if (simplifiedUnits == null || simplifiedUnits == invocationUnits) {
                    return method;
                }

                JavaTemplate template = JavaTemplate.builder(this::getCursor, "#{}(#{})").build();
                return maybeAutoFormat(
                        method,
                        method.withTemplate(
                                template,
                                method.getCoordinates().replaceMethod(),
                                simplifiedUnits.methodName,
                                millis / simplifiedUnits.millisFactor
                        ),
                        ctx
                );
            }
        };
    }

    private static @Nullable Long getConstantIntegralValue(Expression expression) {
        if (expression instanceof J.Literal) {
            J.Literal literal = (J.Literal) expression;
            if (literal.getType() != JavaType.Primitive.Int && literal.getType() != JavaType.Primitive.Long) {
                return null;
            }
            Object value = literal.getValue();
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } else if (expression instanceof J.Binary) {
            J.Binary binary = (J.Binary) expression;
            if (binary.getOperator() != J.Binary.Type.Multiplication) {
                return null;
            }

            Long left = getConstantIntegralValue(binary.getLeft());
            if (left == null) {
                return null;
            }

            Long right = getConstantIntegralValue(binary.getRight());
            if (right == null) {
                return null;
            }

            return left * right;
        }
        return null;
    }
}
