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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.UnwrapParentheses;
import org.openrewrite.java.tree.*;

import java.util.Collections;

import static java.util.Objects.requireNonNull;

public class SimplifyBooleanExpressionVisitor extends JavaVisitor<ExecutionContext> {
    private static final String MAYBE_AUTO_FORMAT_ME = "MAYBE_AUTO_FORMAT_ME";

    @Override
    public J visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(super.visit(tree, ctx));
            if (tree != cu) {
                // recursive simplification
                cu = (JavaSourceFile) visitNonNull(cu, ctx);
            }
            return cu;
        }
        return super.visit(tree, ctx);
    }

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
            } else if (removeAllSpace(asBinary.getLeft()).printTrimmed(getCursor())
                    .equals(removeAllSpace(asBinary.getRight()).printTrimmed(getCursor()))) {
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
            } else if (removeAllSpace(asBinary.getLeft()).printTrimmed(getCursor())
                    .equals(removeAllSpace(asBinary.getRight()).printTrimmed(getCursor()))) {
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
            getCursor().getParentTreeCursor().putMessage(MAYBE_AUTO_FORMAT_ME, "");
        }
        return j;
    }

    @Override
    public J postVisit(J tree, ExecutionContext ctx) {
        J j = super.postVisit(tree, ctx);
        if (j instanceof J.Parentheses) {
            j = new UnwrapParentheses<>((J.Parentheses<?>) j).visit(j, ctx, getCursor().getParentOrThrow());
        }
        if (j != null && getCursor().pollMessage(MAYBE_AUTO_FORMAT_ME) != null) {
            j = autoFormat(j, ctx);
        }
        return j;
    }

    @Override
    public J visitUnary(J.Unary unary, ExecutionContext ctx) {
        J j = super.visitUnary(unary, ctx);
        J.Unary asUnary = (J.Unary) j;

        if (asUnary.getOperator() == J.Unary.Type.Not) {
            if (isLiteralTrue(asUnary.getExpression())) {
                j = ((J.Literal) asUnary.getExpression()).withValue(false).withValueSource("false");
            } else if (isLiteralFalse(asUnary.getExpression())) {
                j = ((J.Literal) asUnary.getExpression()).withValue(true).withValueSource("true");
            } else if (asUnary.getExpression() instanceof J.Unary && ((J.Unary) asUnary.getExpression()).getOperator() == J.Unary.Type.Not) {
                j = ((J.Unary) asUnary.getExpression()).getExpression();
            }
        }
        if (asUnary != j) {
            getCursor().getParentTreeCursor().putMessage(MAYBE_AUTO_FORMAT_ME, "");
        }
        return j;
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

    private J removeAllSpace(J j) {
        //noinspection ConstantConditions
        return new JavaIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                return Space.EMPTY;
            }
        }.visit(j, 0);
    }

    /**
     * Override this method to disable simplification of equals expressions,
     * specifically for Kotlin while that is not yet part of the OpenRewrite/rewrite.
     *
     * Comparing Kotlin nullable type `?` with tree/false can not be simplified,
     * e.g. `X?.fun() == true` is not equivalent to `X?.fun()`
     *
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
