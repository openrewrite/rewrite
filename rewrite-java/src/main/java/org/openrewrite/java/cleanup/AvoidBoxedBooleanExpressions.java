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

import org.openrewrite.*;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static java.util.Collections.singleton;

public class AvoidBoxedBooleanExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Avoid boxed boolean expressions";
    }

    @Override
    public String getDescription() {
        return "Under certain conditions the `java.lang.Boolean` type is used as an expression, " +
               "and it may throw a `NullPointerException` if the value is null.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-5411");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.of(5, ChronoUnit.MINUTES);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("java.lang.Boolean", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                Expression e = (Expression) super.visitExpression(expression, ctx);
                if (TypeUtils.isOfClassType(e.getType(), "java.lang.Boolean")) {
                    if (isControlExpression(expression)) {
                        return e.withTemplate(
                                JavaTemplate.builder(this::getCursor,
                                        "Boolean.TRUE.equals(#{any(java.lang.Boolean)})").build(),
                                e.getCoordinates().replace(),
                                e
                        );
                    }
                }
                return e;
            }

            @Override
            public J visitUnary(J.Unary unary, ExecutionContext executionContext) {
                J.Unary un = (J.Unary) super.visitUnary(unary, executionContext);
                if (J.Unary.Type.Not == un.getOperator() && TypeUtils.isOfClassType(un.getExpression().getType(), "java.lang.Boolean")) {
                    return un.withTemplate(
                            JavaTemplate.builder(this::getCursor,
                                    "Boolean.FALSE.equals(#{any(java.lang.Boolean)})").build(),
                            un.getCoordinates().replace(),
                            un.getExpression()
                    );
                }
                return un;
            }

            private boolean isControlExpression(Expression expression) {
                Cursor parentCursor = getCursor().getParentTreeCursor();
                if (parentCursor.getValue() instanceof J.ControlParentheses &&
                    parentCursor.getParentTreeCursor().getValue() instanceof J.If) {
                    return true;
                } else if (parentCursor.getValue() instanceof J.Ternary) {
                    return ((J.Ternary) parentCursor.getValue()).getCondition() == expression;
                }
                return false;
            }
        });
    }
}
