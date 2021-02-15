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

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.UnwrapParentheses;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Incubating(since = "7.0.0")
public class SimplifyBooleanExpressionVisitor<P> extends JavaVisitor<P> {

    private static final String MAYBE_AUTO_FORMAT_ME = "MAYBE_AUTO_FORMAT_ME";

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = visitAndCast(cu, p, super::visitCompilationUnit);
        if (c != cu) {
            doAfterVisit(new SimplifyBooleanExpressionVisitor());
        }
        return c;
    }

    @Override
    public J visitBinary(J.Binary binary, P p) {
        J j = super.visitBinary(binary, p);
        J.Binary asBinary = (J.Binary) j;

        if (asBinary.getOperator() == J.Binary.Type.And) {
            if (isLiteralFalse(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            } else if (isLiteralFalse(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (asBinary.getLeft().printTrimmed().replaceAll("\\s", "").equals(asBinary.getRight().printTrimmed().replaceAll("\\s", ""))) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == J.Binary.Type.Or) {
            if (isLiteralTrue(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            } else if (isLiteralTrue(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (asBinary.getLeft().printTrimmed().replaceAll("\\s", "").equals(asBinary.getRight().printTrimmed().replaceAll("\\s", ""))) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == J.Binary.Type.Equal) {
            if (isLiteralTrue(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (isLiteralTrue(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == J.Binary.Type.NotEqual) {
            if (isLiteralFalse(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (isLiteralFalse(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        }

        getCursor().dropParentUntil(J.class::isInstance).putMessage(MAYBE_AUTO_FORMAT_ME, "");
        return j;
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        J j = super.postVisit(tree, p);
        if (getCursor().pollMessage(MAYBE_AUTO_FORMAT_ME) != null) {
            j = new AutoFormatVisitor<>().visit(j, p, getCursor());
        }
        return j;
    }

    @Override
    public J visitUnary(J.Unary unary, P p) {
        J j = super.visitUnary(unary, p);
        J.Unary asUnary = (J.Unary) j;

        if (asUnary.getOperator() == J.Unary.Type.Not) {
            if (isLiteralTrue(asUnary.getExpression())) {
                maybeUnwrapParentheses();
                j = ((J.Literal) asUnary.getExpression()).withValue(false).withValueSource("false");
            } else if (isLiteralFalse(asUnary.getExpression())) {
                maybeUnwrapParentheses();
                j = ((J.Literal) asUnary.getExpression()).withValue(true).withValueSource("true");
            } else if (asUnary.getExpression() instanceof J.Unary && ((J.Unary) asUnary.getExpression()).getOperator() == J.Unary.Type.Not) {
                maybeUnwrapParentheses();
                j = ((J.Unary) asUnary.getExpression()).getExpression();
            }
        }

        getCursor().dropParentUntil(J.class::isInstance).putMessage(MAYBE_AUTO_FORMAT_ME, "");
        return j;
    }

    /**
     * Specifically for removing immediately-enclosing parentheses on Identifiers and Literals.
     * This queues a potential unwrap operation for the next visit. After unwrapping something, it's possible
     * there are more Simplifications this recipe can identify and perform, which is why visitCompilationUnit
     * checks for any changes to the entire Compilation Unit, and if so, queues up another SimplifyBooleanExpression
     * recipe call. This convergence loop eventually reconciles any remaining Boolean Expression Simplifications
     * the recipe can perform.
     */
    private void maybeUnwrapParentheses() {
        Cursor c = getCursor().getParentOrThrow().dropParentUntil(J.class::isInstance);
        if (c.getValue() instanceof J.Parentheses) {
            doAfterVisit(new UnwrapParentheses<>(c.getValue()));
        }
    }

    private boolean isLiteralTrue(@Nullable Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(true);
    }

    private boolean isLiteralFalse(@Nullable Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(false);
    }

}
