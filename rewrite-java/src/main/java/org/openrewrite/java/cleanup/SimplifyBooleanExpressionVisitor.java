/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;

public class SimplifyBooleanExpressionVisitor extends JavaVisitor<ExecutionContext> {
    @Override
    public J visitBinary(J.Binary binary, ExecutionContext ctx) {
        J j = super.visitBinary(binary, ctx);
        J.Binary asBinary = (J.Binary) j;

        if (asBinary.getOperator() == J.Binary.Type.And) {
            if (isLiteralFalse(asBinary.getLeft())) {
                j = asBinary.getLeft();
            } else if (isLiteralFalse(asBinary.getRight())) {
                j = asBinary.getRight().withPrefix(asBinary.getRight().getPrefix().withWhitespace(""));
            } else if (isLiteralTrue(asBinary.getLeft())) {
                j = asBinary.getRight();
            } else if (isLiteralTrue(asBinary.getRight())) {
                j = asBinary.getLeft().withPrefix(asBinary.getLeft().getPrefix().withWhitespace(""));
            } else if (SemanticallyEqual.areEqual(asBinary.getLeft(), asBinary.getRight())) {
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == J.Binary.Type.Or) {
            if (isLiteralTrue(asBinary.getLeft())) {
                j = asBinary.getLeft();
            } else if (isLiteralTrue(asBinary.getRight())) {
                j = asBinary.getRight().withPrefix(asBinary.getRight().getPrefix().withWhitespace(""));
            } else if (isLiteralFalse(asBinary.getLeft())) {
                j = asBinary.getRight();
            } else if (isLiteralFalse(asBinary.getRight())) {
                j = asBinary.getLeft().withPrefix(asBinary.getLeft().getPrefix().withWhitespace(""));
            } else if (SemanticallyEqual.areEqual(asBinary.getLeft(), asBinary.getRight())) {
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == J.Binary.Type.Equal) {
            if (isLiteralTrue(asBinary.getLeft())) {
                if (shouldSimplifyEqualsOn(asBinary.getRight())) {
                    j = asBinary.getRight().withPrefix(asBinary.getRight().getPrefix().withWhitespace(""));
                }
            } else if (isLiteralTrue(asBinary.getRight())) {
                if (shouldSimplifyEqualsOn(asBinary.getLeft())) {
                    j = asBinary.getLeft().withPrefix(asBinary.getLeft().getPrefix().withWhitespace(" "));
                }
            } else {
                j = maybeReplaceCompareWithNull(asBinary, true);
            }
        } else if (asBinary.getOperator() == J.Binary.Type.NotEqual) {
            if (isLiteralFalse(asBinary.getLeft())) {
                if (shouldSimplifyEqualsOn(asBinary.getRight())) {
                    j = asBinary.getRight().withPrefix(asBinary.getRight().getPrefix().withWhitespace(""));
                }
            } else if (isLiteralFalse(asBinary.getRight())) {
                if (shouldSimplifyEqualsOn(asBinary.getLeft())) {
                    j = asBinary.getLeft().withPrefix(asBinary.getLeft().getPrefix().withWhitespace(" "));
                }
            } else {
                j = maybeReplaceCompareWithNull(asBinary, false);
            }
        }
        if (asBinary != j) {
            j = j.withPrefix(asBinary.getPrefix());
        }
        return j;
    }

    @Override
    public J postVisit(J tree, ExecutionContext ctx) {
        J j = tree;
        if (j instanceof J.Parentheses) {
            j = new UnnecessaryParenthesesVisitor<>().visit(j, ctx, getCursor().getParentOrThrow());
        }
        return j;
    }

    @Override
    public J visitUnary(J.Unary unary, ExecutionContext ctx) {
        J j = super.visitUnary(unary, ctx);
        J.Unary asUnary = (J.Unary) j;

        if (asUnary.getOperator() == J.Unary.Type.Not) {
            Expression expr = asUnary.getExpression();
            if (isLiteralTrue(expr)) {
                j = ((J.Literal) expr).withValue(false).withValueSource("false");
            } else if (isLiteralFalse(expr)) {
                j = ((J.Literal) expr).withValue(true).withValueSource("true");
            } else if (expr instanceof J.Unary && ((J.Unary) expr).getOperator() == J.Unary.Type.Not) {
                j = ((J.Unary) expr).getExpression();
            } else if (expr instanceof J.Parentheses && ((J.Parentheses<?>) expr).getTree() instanceof J.Binary) {
                J.Binary binary = (J.Binary) ((J.Parentheses<?>) expr).getTree();
                J.Binary.Type negated = negate(binary.getOperator());
                if (negated != binary.getOperator()) {
                    j = binary.withOperator(negated).withPrefix(j.getPrefix());
                }
            } else if (expr instanceof J.Parentheses && ((J.Parentheses<?>) expr).getTree() instanceof J.Unary) {
                J.Unary unary1 = (J.Unary) ((J.Parentheses<?>) expr).getTree();
                J.Unary.Type operator = unary1.getOperator();
                if (operator == J.Unary.Type.Not) {
                    j = unary1.getExpression().withPrefix(j.getPrefix());
                }
            }
        }
        if (asUnary != j) {
            j = j.withPrefix(asUnary.getPrefix());
        }
        return j;
    }

    private J.Binary.Type negate(J.Binary.Type operator) {
        switch (operator) {
            case LessThan:
                return J.Binary.Type.GreaterThanOrEqual;
            case GreaterThan:
                return J.Binary.Type.LessThanOrEqual;
            case LessThanOrEqual:
                return J.Binary.Type.GreaterThan;
            case GreaterThanOrEqual:
                return J.Binary.Type.LessThan;
            case Equal:
                return J.Binary.Type.NotEqual;
            case NotEqual:
                return J.Binary.Type.Equal;
            default:
                return operator;
        }
    }

    private final MethodMatcher isEmpty = new MethodMatcher("java.lang.String isEmpty()");

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        J j = super.visitMethodInvocation(method, executionContext);
        J.MethodInvocation asMethod = (J.MethodInvocation) j;
        Expression select = asMethod.getSelect();
        if (isEmpty.matches(asMethod)
                && select instanceof J.Literal
                && select.getType() == JavaType.Primitive.String) {
            return booleanLiteral(method, J.Literal.isLiteralValue(select, ""));
        }
        return j;
    }

    private boolean isLiteralTrue(@Nullable Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(true);
    }

    private boolean isLiteralFalse(@Nullable Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(false);
    }

    private boolean isNullLiteral(Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getType() == JavaType.Primitive.Null;
    }

    private boolean isNonNullLiteral(Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getType() != JavaType.Primitive.Null;
    }

    private J maybeReplaceCompareWithNull(J.Binary asBinary, boolean valueIfEqual) {
        Expression left = asBinary.getLeft();
        Expression right = asBinary.getRight();

        boolean leftIsNull = isNullLiteral(left);
        boolean rightIsNull = isNullLiteral(right);
        if (leftIsNull && rightIsNull) {
            return booleanLiteral(asBinary, valueIfEqual);
        }
        boolean leftIsNonNullLiteral = isNonNullLiteral(left);
        boolean rightIsNonNullLiteral = isNonNullLiteral(right);
        if ((leftIsNull && rightIsNonNullLiteral) || (rightIsNull && leftIsNonNullLiteral)) {
            return booleanLiteral(asBinary, !valueIfEqual);
        }

        return asBinary;
    }

    private J.Literal booleanLiteral(J j, boolean value) {
        return new J.Literal(Tree.randomId(),
                j.getPrefix(),
                j.getMarkers(),
                value,
                String.valueOf(value),
                Collections.emptyList(),
                JavaType.Primitive.Boolean);
    }

    /**
     * Override this method to disable simplification of equals expressions,
     * specifically for Kotlin while that is not yet part of the OpenRewrite/rewrite.
     * <p>
     * Comparing Kotlin nullable type `?` with tree/false can not be simplified,
     * e.g. `X?.fun() == true` is not equivalent to `X?.fun()`
     * <p>
     * Subclasses will want to check if the `org.openrewrite.kotlin.marker.IsNullSafe`
     * marker is present.
     *
     * @param j the expression to simplify
     * @return true by default, unless overridden
     */
    protected boolean shouldSimplifyEqualsOn(J j) {
        return true;
    }
}
