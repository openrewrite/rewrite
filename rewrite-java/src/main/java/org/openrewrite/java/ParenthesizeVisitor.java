/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;

public class ParenthesizeVisitor extends JavaIsoVisitor<ExecutionContext> {
    @Override
    public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
        J.Binary b = super.visitBinary(binary, ctx);

        if (b.getLeft() instanceof J.Binary) {
            if (needsParenthesesForPrecedence((J.Binary) b.getLeft(), b)) {
                b = b.withLeft(parenthesize(b.getLeft()));
            }
        }
        if (b.getRight() instanceof J.Binary) {
            if (needsParenthesesForPrecedence((J.Binary) b.getRight(), b)) {
                b = b.withRight(parenthesize(b.getRight()));
            }
        }

        return b;
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
        J.Unary u = super.visitUnary(unary, ctx);
        if (u.getExpression() instanceof J.Binary) {
            u = u.withExpression(parenthesize(u.getExpression()));
        } else if (u.getExpression() instanceof J.InstanceOf) {
            u = u.withExpression(parenthesize(u.getExpression()));
        } else if (u.getExpression() instanceof J.Assignment) {
            u = u.withExpression(parenthesize(u.getExpression()));
        }
        return u;
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary, ExecutionContext ctx) {
        J.Ternary t = super.visitTernary(ternary, ctx);

        // Check if this ternary is inside an expression that requires parentheses
        Cursor parent = getCursor().getParentOrThrow();
        if (parent.getValue() instanceof J.Unary) {
            return parenthesize(t);
        } else if (parent.getValue() instanceof J.Binary) {
            return parenthesize(t);
        } else if (parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(t);
        } else if (parent.getValue() instanceof J.Ternary && parent.getValue() != t) {
            return parenthesize(t);
        }

        return t;
    }

    /**
     * Determine if a binary expression needs parentheses based on operator precedence
     * when nested inside another binary expression.
     */
    private boolean needsParenthesesForPrecedence(J.Binary inner, J.Binary outer) {
        // Get precedence levels (higher number = higher precedence)
        int innerPrecedence = getPrecedence(inner.getOperator());
        int outerPrecedence = getPrecedence(outer.getOperator());

        // If inner has lower precedence than outer, it needs parentheses
        if (innerPrecedence < outerPrecedence) {
            return true;
        }

        // If they have the same precedence but are not associative,
        // and inner is the right operand, it needs parentheses
        if (innerPrecedence == outerPrecedence && !isAssociative(outer.getOperator())) {
            return outer.getRight() == inner;
        }

        return false;
    }

    /**
     * Get the precedence level of a binary operator.
     * Higher number means higher precedence.
     */
    private int getPrecedence(J.Binary.Type operator) {
        switch (operator) {
            case Multiplication:
            case Division:
            case Modulo:
                return 5;
            case Addition:
            case Subtraction:
                return 4;
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                return 3;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
            case Equal:
            case NotEqual:
                return 2;
            case BitAnd:
                return 1;
            case BitXor:
                return 0;
            case BitOr:
                return -1;
            case And:
                return -2;
            case Or:
                return -3;
            default:
                return 0;
        }
    }

    /**
     * Determine if a binary operator is associative.
     */
    private boolean isAssociative(J.Binary.Type operator) {
        switch (operator) {
            case Addition:
            case Multiplication:
            case BitAnd:
            case BitOr:
            case BitXor:
            case And:
            case Or:
                return true;
            default:
                return false;
        }
    }

    /**
     * Wrap an expression in parentheses if it's not already parenthesized.
     */
    private <T extends J> T parenthesize(T tree) {
        if (tree instanceof J.Parentheses) {
            return tree;
        }

        if (tree instanceof Expression) {
            Expression expression = (Expression) tree;
            return (T) new J.Parentheses<>(
                    Tree.randomId(),
                    expression.getPrefix(),
                    expression.getMarkers(),
                    JRightPadded.build(expression.withPrefix(Space.EMPTY))
            );
        }

        return tree;
    }
}
