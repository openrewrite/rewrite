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
package org.openrewrite.java.utils;

import org.openrewrite.Tree;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.java.tree.J.Binary.Type;
import java.util.List;

public final class ExpressionUtils {
    private ExpressionUtils() {}

    /**
     * Concat two literals to an expression with '+' and surrounded with single space.
     */
    public static J.Binary concatAdditionBinary(Expression left, Expression right) {
        JLeftPadded<Type> leftPadded = new JLeftPadded<>(Space.SINGLE_SPACE,
                Type.Addition,
                Markers.EMPTY);

        return new J.Binary(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                left,
                leftPadded,
                right.withPrefix(Space.SINGLE_SPACE),
                left.getType());
    }

    /**
     * Concat expressions to an expression with '+' connected.
     */
    public static Expression additiveExpression(Expression... expressions) {
        Expression expression = null;

        for (Expression element : expressions) {
            if (element != null) {
                expression = (expression == null) ? element : concatAdditionBinary(expression, element);
            }
        }
        return expression;
    }

    public static Expression additiveExpression(List<Expression> expressions) {
       return additiveExpression(expressions.toArray(new Expression[0]));
    }
}
