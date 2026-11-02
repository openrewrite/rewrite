/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.hcl.tree;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;

/**
 * Helpers for reading statically known values out of HCL expressions.
 */
@Incubating(since = "8.94.0")
public final class HclValues {

    private HclValues() {
    }

    /**
     * Returns the value of an expression when it is statically known, unwrapping any parentheses.
     * <ul>
     *     <li>{@code "hello"} and {@code ("hello")} yield {@code hello}, escape sequences are returned as written</li>
     *     <li>{@code ""} yields the empty string</li>
     *     <li>{@code 3}, {@code 1.50} and {@code true} yield their source text</li>
     * </ul>
     *
     * @param expression The expression to evaluate.
     * @return The value, or {@code null} for {@code null} literals and anything that is not a literal or a quoted
     * string without interpolations, such as {@code "${var.a}b"}, {@code var.a} or heredocs.
     */
    public static @Nullable String stringValue(@Nullable Expression expression) {
        Expression e = expression;
        while (e instanceof Hcl.Parentheses) {
            e = ((Hcl.Parentheses) e).getExpression();
        }
        if (e instanceof Hcl.QuotedTemplate) {
            StringBuilder value = new StringBuilder();
            for (Expression part : ((Hcl.QuotedTemplate) e).getExpressions()) {
                if (!(part instanceof Hcl.Literal)) {
                    return null;
                }
                value.append(((Hcl.Literal) part).getValueSource());
            }
            return value.toString();
        }
        if (e instanceof Hcl.Literal) {
            Hcl.Literal literal = (Hcl.Literal) e;
            if (literal.getValue() instanceof String) {
                return (String) literal.getValue();
            }
            return literal.getValue() == null ? null : literal.getValueSource();
        }
        return null;
    }
}
