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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

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
            } else if (!(asBinary.getLeft() instanceof MethodCall) &&
                       SemanticallyEqual.areEqual(asBinary.getLeft(), asBinary.getRight())) {
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
            } else if (!(asBinary.getLeft() instanceof MethodCall) &&
                       SemanticallyEqual.areEqual(asBinary.getLeft(), asBinary.getRight())) {
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
            } else if (isLiteralFalse(asBinary.getLeft())) {
                if (shouldSimplifyEqualsOn(asBinary.getRight())) {
                    j = not(asBinary.getRight());
                }
            } else if (isLiteralFalse(asBinary.getRight())) {
                if (shouldSimplifyEqualsOn(asBinary.getLeft())) {
                    j = not(asBinary.getLeft());
                }
            } else {
                j = maybeReplaceCompareWithNull(asBinary, true);
                if (j == asBinary) {
                    // If null comparison didn't apply, try numeric comparison
                    Boolean result = compareNumericLiterals(asBinary);
                    if (result != null) {
                        j = booleanLiteral(asBinary, result);
                    }
                }
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
            } else if (isLiteralTrue(asBinary.getLeft())) {
                if (shouldSimplifyEqualsOn(asBinary.getRight())) {
                    j = not(asBinary.getRight());
                }
            } else if (isLiteralTrue(asBinary.getRight())) {
                if (shouldSimplifyEqualsOn(asBinary.getLeft())) {
                    j = not(asBinary.getLeft());
                }
            } else {
                j = maybeReplaceCompareWithNull(asBinary, false);
                if (j == asBinary) {
                    // If null comparison didn't apply, try numeric comparison
                    Boolean result = compareNumericLiterals(asBinary);
                    if (result != null) {
                        j = booleanLiteral(asBinary, result);
                    }
                }
            }
        } else if (asBinary.getOperator() == J.Binary.Type.LessThan ||
                   asBinary.getOperator() == J.Binary.Type.GreaterThan ||
                   asBinary.getOperator() == J.Binary.Type.LessThanOrEqual ||
                   asBinary.getOperator() == J.Binary.Type.GreaterThanOrEqual) {
            // Simplify numeric literal comparisons
            Boolean result = compareNumericLiterals(asBinary);
            if (result != null) {
                j = booleanLiteral(asBinary, result);
            }
        }
        if (asBinary != j) {
            j = j.withPrefix(asBinary.getPrefix());
        }
        return j;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
        J j = super.visitParentheses(parens, ctx);
        if (j != parens && j instanceof J.Parentheses) {
            j = new UnnecessaryParenthesesVisitor<>().visit(j, ctx, getCursor().getParentOrThrow());
        }
        return j;
    }

    @Override
    public J visitUnary(J.Unary unary, ExecutionContext ctx) {
        J j = super.visitUnary(unary, ctx);
        if (j instanceof J.Unary) {
            J.Unary asUnary = (J.Unary) j;

            if (asUnary.getOperator() == J.Unary.Type.Not) {
                j = unpackExpression(asUnary.getExpression(), asUnary);
            }
            if (asUnary != j) {
                j = j.withPrefix(asUnary.getPrefix());
            }
        }
        return j;
    }

    @Override
    public J visitTernary(J.Ternary ternary, ExecutionContext executionContext) {
        J j = super.visitTernary(ternary, executionContext);
        if (j instanceof J.Ternary) {
            J.Ternary asTernary = (J.Ternary) j;
            if (asTernary.getCondition() instanceof J.Unary) {
                Expression negated = maybeNegate(asTernary.getCondition());
                if (negated != asTernary.getCondition()) {
                    j = asTernary
                            .withCondition(negated)
                            .withTruePart(asTernary.getFalsePart())
                            .withFalsePart(asTernary.getTruePart());
                }
            } else if (asTernary.getCondition() instanceof J.Literal) {
                if (isLiteralTrue(asTernary.getCondition())) {
                    j = asTernary.getTruePart();
                } else if (isLiteralFalse(asTernary.getCondition())) {
                    j = asTernary.getFalsePart();
                }
            } else if (asTernary.getCondition() instanceof J.Parentheses) {
                J.Parentheses<Expression> parenthesized = (J.Parentheses<Expression>) asTernary.getCondition();
                if (parenthesized.getTree() instanceof J.Literal) {
                    if (isLiteralTrue(parenthesized.getTree())) {
                        j = asTernary.getTruePart();
                    } else if (isLiteralFalse(parenthesized.getTree())) {
                        j = asTernary.getFalsePart();
                    }
                }
            }
        }
        return j;
    }

    private Expression unpackExpression(Expression expr, Expression j) {
        if (isLiteralTrue(expr)) {
            j = ((J.Literal) expr).withValue(false).withValueSource("false");
        } else if (isLiteralFalse(expr)) {
            j = ((J.Literal) expr).withValue(true).withValueSource("true");
        } else if (expr instanceof J.Unary && ((J.Unary) expr).getOperator() == J.Unary.Type.Not) {
            j = ((J.Unary) expr).getExpression();
        } else if (expr instanceof J.Parentheses) {
            J parenthesized = ((J.Parentheses<?>) expr).getTree();
            if (parenthesized instanceof J.Binary) {
                J.Binary binary = (J.Binary) parenthesized;
                J.Binary.Type negated = maybeNegate(binary.getOperator());
                if (negated != binary.getOperator()) {
                    j = binary.withOperator(negated).withPrefix(j.getPrefix());
                }
            } else if (parenthesized instanceof J.Unary) {
                J.Unary unary1 = (J.Unary) parenthesized;
                J.Unary.Type operator = unary1.getOperator();
                if (operator == J.Unary.Type.Not) {
                    j = unary1.getExpression().withPrefix(j.getPrefix());
                }
            } else if (parenthesized instanceof J.Ternary) {
                J.Ternary ternary = (J.Ternary) parenthesized;
                j = ternary
                        .withTruePart(maybeNegate(ternary.getTruePart()))
                        .withFalsePart(maybeNegate(ternary.getFalsePart()))
                        .withPrefix(j.getPrefix());
            } else if (parenthesized instanceof Expression) {
                j = unpackExpression((Expression) parenthesized, j);
            }
        }
        return j;
    }

    private Expression maybeNegate(Expression expr) {
        if (expr instanceof J.Binary) {
            J.Binary.Type negated = maybeNegate(((J.Binary) expr).getOperator());
            if (negated != ((J.Binary) expr).getOperator()) {
                return ((J.Binary) expr).withOperator(negated).withPrefix(expr.getPrefix());
            }
        } else if (expr instanceof J.Unary && ((J.Unary) expr).getOperator() == J.Unary.Type.Not) {
            return ((J.Unary) expr).getExpression().withPrefix(expr.getPrefix());
        } else if (expr instanceof J.Ternary) {
            J.Ternary ternary = (J.Ternary) expr;
            Expression negatedCondition = maybeNegate(ternary.getCondition());
            if (negatedCondition != ternary.getCondition()) {
                return ternary
                        .withCondition(negatedCondition)
                        .withTruePart(ternary.getFalsePart())
                        .withFalsePart(ternary.getTruePart())
                        .withPrefix(expr.getPrefix());
            }
        } else if (isLiteralTrue(expr)) {
            return ((J.Literal) expr).withValue(false).withValueSource("false");
        } else if (isLiteralFalse(expr)) {
            return ((J.Literal) expr).withValue(true).withValueSource("true");
        }
        return not(expr).withPrefix(expr.getPrefix());
    }

    private J.Binary.Type maybeNegate(J.Binary.Type operator) {
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
    private final MethodMatcher equals = new MethodMatcher("java.lang.String equals(java.lang.Object)");

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        J j = super.visitMethodInvocation(method, executionContext);
        J.MethodInvocation asMethod = (J.MethodInvocation) j;
        Expression select = asMethod.getSelect();
        if (isEmpty.matches(asMethod) &&
            select instanceof J.Literal &&
            select.getType() == JavaType.Primitive.String) {
            return booleanLiteral(method, J.Literal.isLiteralValue(select, ""));
        } else if (equals.matches(asMethod)) {
            Expression arg = asMethod.getArguments().get(0);
            if (arg instanceof J.Literal && select instanceof J.Literal) {
                return booleanLiteral(method, ((J.Literal) select).getValue().equals(((J.Literal) arg).getValue()));
            } else if (SemanticallyEqual.areEqual(select, arg)) {
                return booleanLiteral(method, true);
            }
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

    private static boolean isNumericLiteral(Expression expression) {
        return expression instanceof J.Literal &&
                ((JavaType.Primitive) expression.getType()).isNumeric();
    }

    private static boolean isIntegralType(Expression expression) {
        if (!(expression instanceof J.Literal)) {
            return false;
        }
        JavaType type = expression.getType();
        return type == JavaType.Primitive.Byte ||
               type == JavaType.Primitive.Short ||
               type == JavaType.Primitive.Int ||
               type == JavaType.Primitive.Long;
    }

    private @Nullable Boolean compareNumericLiterals(J.Binary binary) {
        if (!isNumericLiteral(binary.getLeft()) || !isNumericLiteral(binary.getRight())) {
            return null;
        }

        Object leftValue = ((J.Literal) binary.getLeft()).getValue();
        Object rightValue = ((J.Literal) binary.getRight()).getValue();

        if (!(leftValue instanceof Number) || !(rightValue instanceof Number)) {
            return null;
        }

        // If both operands are integral types, compare as long to avoid precision loss
        if (isIntegralType(binary.getLeft()) && isIntegralType(binary.getRight())) {
            long left = ((Number) leftValue).longValue();
            long right = ((Number) rightValue).longValue();

            switch (binary.getOperator()) {
                case LessThan:
                    return left < right;
                case GreaterThan:
                    return left > right;
                case LessThanOrEqual:
                    return left <= right;
                case GreaterThanOrEqual:
                    return left >= right;
                case Equal:
                    return left == right;
                case NotEqual:
                    return left != right;
                default:
                    return null;
            }
        } else {
            // If either operand is floating-point, compare as double
            double left = ((Number) leftValue).doubleValue();
            double right = ((Number) rightValue).doubleValue();

            switch (binary.getOperator()) {
                case LessThan:
                    return left < right;
                case GreaterThan:
                    return left > right;
                case LessThanOrEqual:
                    return left <= right;
                case GreaterThanOrEqual:
                    return left >= right;
                case Equal:
                    return Double.compare(left, right) == 0;
                case NotEqual:
                    return Double.compare(left, right) != 0;
                default:
                    return null;
            }
        }
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

    private static J.Literal booleanLiteral(J j, boolean value) {
        return new J.Literal(Tree.randomId(),
                j.getPrefix(),
                j.getMarkers(),
                value,
                String.valueOf(value),
                emptyList(),
                JavaType.Primitive.Boolean);
    }

    private static J.Unary not(Expression sideRetained) {
        if (!(sideRetained instanceof J.FieldAccess) &&
            !(sideRetained instanceof J.Identifier) &&
            !(sideRetained instanceof J.Literal) &&
            !(sideRetained instanceof J.MethodInvocation) &&
            !(sideRetained instanceof J.Parentheses) &&
            !(sideRetained instanceof J.Unary)) {
            sideRetained = new J.Parentheses<>(Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    JRightPadded.build(sideRetained));
        }
        return new J.Unary(Tree.randomId(),
                sideRetained.getPrefix(),
                sideRetained.getMarkers(),
                JLeftPadded.build(J.Unary.Type.Not),
                sideRetained
                        .withPrefix(Space.EMPTY)
                        .withMarkers(Markers.EMPTY),
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
