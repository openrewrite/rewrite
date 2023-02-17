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
     * Concat two literals to an expression with '+'.
     */
    public static J.Binary concatBinary(Expression left, Expression right) {
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
                expression = (expression == null) ? element : concatBinary(expression, element);
            }
        }
        return expression;
    }

    public static Expression additiveExpression(List<Expression> expressions) {
       return additiveExpression(expressions.toArray(new Expression[0]));
    }
}
