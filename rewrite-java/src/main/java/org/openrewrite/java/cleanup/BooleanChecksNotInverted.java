/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class BooleanChecksNotInverted extends Recipe {

    @Override
    public String getDisplayName() {
        return "Boolean checks should not be inverted";
    }

    @Override
    public String getDescription() {
        return "It is needlessly complex to invert the result of a boolean comparison. The opposite comparison should be made instead. "
                + "Also double negation of boolean expressions should be avoided. This recipes takes care of that.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1940");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @SuppressWarnings("ConstantConditions")
            @Override
            public J visitUnary(J.Unary unary, ExecutionContext ctx) {
                if (unary.getOperator() == J.Unary.Type.Not && unary.getExpression() instanceof J.Parentheses) {
                    J.Parentheses<?> expr = (J.Parentheses<?>) unary.getExpression();
                    if (expr.getTree() instanceof J.Binary) {
                        J.Binary binary = (J.Binary) expr.getTree();
                        switch (binary.getOperator()) {
                            case LessThan:
                                return super.visit(binary.withOperator(J.Binary.Type.GreaterThanOrEqual), ctx).withPrefix(unary.getPrefix());
                            case GreaterThan:
                                return super.visit(binary.withOperator(J.Binary.Type.LessThanOrEqual), ctx).withPrefix(unary.getPrefix());
                            case LessThanOrEqual:
                                return super.visit(binary.withOperator(J.Binary.Type.GreaterThan), ctx).withPrefix(unary.getPrefix());
                            case GreaterThanOrEqual:
                                return super.visit(binary.withOperator(J.Binary.Type.LessThan), ctx).withPrefix(unary.getPrefix());
                            case Equal:
                                return super.visit(binary.withOperator(J.Binary.Type.NotEqual), ctx).withPrefix(unary.getPrefix());
                            case NotEqual:
                                return super.visit(binary.withOperator(J.Binary.Type.Equal), ctx).withPrefix(unary.getPrefix());
                        }
                    } else if (expr.getTree() instanceof J.Unary) {
                        J.Unary nestedUnary = (J.Unary) expr.getTree();
                        if (nestedUnary.getOperator() == J.Unary.Type.Not) {
                            return super.visit(nestedUnary.getExpression(), ctx).withPrefix(unary.getPrefix());
                        }
                    }
                }
                return super.visitUnary(unary, ctx);
            }
        };
    }
}
