/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.marker.InfixNotation;
import org.openrewrite.scala.marker.RightAssociative;
import org.openrewrite.scala.tree.S;

import java.util.UUID;

/**
 * Adds the parentheses Scala's operator precedence needs for an expression to read the way the tree groups it.
 */
public class ScalaParenthesizeVisitor<P> extends ScalaVisitor<P> {

    // Scala ranks an infix operator by its first character; from the loosest to the tightest
    private static final int LOWEST = 0;
    private static final int LETTER = 1;
    private static final int PIPE = 2;
    private static final int CARET = 3;
    private static final int AMPERSAND = 4;
    private static final int EQUALS = 5;
    private static final int ANGLE = 6;
    private static final int COLON = 7;
    private static final int ADDITIVE = 8;
    private static final int MULTIPLICATIVE = 9;
    private static final int OTHER_SYMBOL = 10;
    private static final int PREFIX = 11;
    private static final int POSTFIX = 12;
    private static final int PRIMARY = 13;

    /**
     * Determines whether an expression needs to be parenthesized when replacing another expression in the tree.
     *
     * @param newTree the expression that will replace an existing expression
     * @param cursor  cursor to the expression being replaced
     * @return either newTree itself or newTree wrapped in parentheses if needed
     */
    public static Expression maybeParenthesize(Expression newTree, Cursor cursor) {
        UUID slot = cursor.<Tree>getValue().getId();
        Object parent = cursor.getParentTreeCursor().getValue();
        return needsParentheses(newTree, slot, parent) ? parenthesize(newTree) : newTree;
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        J j = super.postVisit(tree, p);
        Object parent = getCursor().getParentTreeCursor().getValue();
        return j instanceof Expression && needsParentheses((Expression) j, j.getId(), parent) ?
                parenthesize((Expression) j) : j;
    }

    private static boolean needsParentheses(Expression child, UUID slot, Object parent) {
        int precedence = precedence(child);
        if (precedence == PRIMARY) {
            return false;
        }
        if (parent instanceof J.Binary) {
            J.Binary b = (J.Binary) parent;
            return needsParentheses(child, precedence, slot, b.getLeft(), b.getRight(), operator(b), precedence(b),
                    false);
        } else if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) parent;
            if (isInfix(m)) {
                // a right-associative call holds its printed right operand as the select
                boolean rightAssociative = isRightAssociative(m);
                Expression left = rightAssociative ? m.getArguments().get(0) : m.getSelect();
                Expression right = rightAssociative ? m.getSelect() : m.getArguments().get(0);
                return needsParentheses(child, precedence, slot, left, right, m.getSimpleName(), precedence(m),
                        rightAssociative);
            }
            return is(m.getSelect(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.Unary) {
            // a prefix operator applies to a simple expression: `-a.abs` is `-(a.abs)`
            return precedence < POSTFIX;
        } else if (parent instanceof J.TypeCast) {
            return is(((J.TypeCast) parent).getExpression(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.InstanceOf) {
            return is(((J.InstanceOf) parent).getExpression(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.FieldAccess) {
            return is(((J.FieldAccess) parent).getTarget(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.ArrayAccess) {
            return is(((J.ArrayAccess) parent).getIndexed(), slot) && precedence < POSTFIX;
        }
        return false;
    }

    // the operands of an infix operator, in the order they print
    private static boolean needsParentheses(Expression child, int precedence, UUID slot, @Nullable Expression left,
                                            @Nullable Expression right, Object operator, int parentPrecedence,
                                            boolean rightAssociative) {
        if (precedence != parentPrecedence) {
            return precedence < parentPrecedence;
        }
        // Scala rejects left- and right-associative operators of one precedence side by side
        if (isRightAssociative(child) != rightAssociative) {
            return true;
        }
        // The operand on the side the operator doesn't group toward would regroup if bare
        if (is(rightAssociative ? left : right, slot)) {
            return true;
        }
        // An equally binding operand with another operator keeps its parentheses, as Java's do
        Object childOperator = operator(child);
        return !operator.equals(childOperator) &&
               !(isAdditive(operator) && isAdditive(childOperator)) &&
               !(isMultiplicative(operator) && isMultiplicative(childOperator));
    }

    private static int precedence(Expression e) {
        if (e instanceof J.Binary) {
            return precedence((J.Binary) e);
        } else if (e instanceof J.MethodInvocation && isInfix((J.MethodInvocation) e)) {
            return precedence((J.MethodInvocation) e);
        } else if (e instanceof J.Unary) {
            return PREFIX;
        } else if (e instanceof J.Assignment || e instanceof J.AssignmentOperation || e instanceof S.TypeAscription) {
            return LOWEST;
        }
        return PRIMARY;
    }

    private static int precedence(J.Binary binary) {
        return operatorPrecedence(operator(binary));
    }

    private static int precedence(J.MethodInvocation infix) {
        return operatorPrecedence(infix.getSimpleName());
    }

    // An operator that ends in `=` and isn't a comparison is an assignment, which binds loosest
    private static int operatorPrecedence(String operator) {
        if (operator.endsWith("=") && !operator.startsWith("=") &&
            !"<=".equals(operator) && !">=".equals(operator) && !"!=".equals(operator)) {
            return LOWEST;
        }
        char first = operator.charAt(0);
        if (Character.isLetter(first) || first == '_' || first == '$') {
            return LETTER;
        }
        switch (first) {
            case '|':
                return PIPE;
            case '^':
                return CARET;
            case '&':
                return AMPERSAND;
            case '=':
            case '!':
                return EQUALS;
            case '<':
            case '>':
                return ANGLE;
            case ':':
                return COLON;
            case '+':
            case '-':
                return ADDITIVE;
            case '*':
            case '/':
            case '%':
                return MULTIPLICATIVE;
            default:
                return OTHER_SYMBOL;
        }
    }

    private static String operator(J.Binary binary) {
        switch (binary.getOperator()) {
            case Addition:
                return "+";
            case Subtraction:
                return "-";
            case Multiplication:
                return "*";
            case Division:
                return "/";
            case Modulo:
                return "%";
            case LessThan:
                return "<";
            case GreaterThan:
                return ">";
            case LessThanOrEqual:
                return "<=";
            case GreaterThanOrEqual:
                return ">=";
            case Equal:
                return "==";
            case NotEqual:
                return "!=";
            case BitAnd:
                return "&";
            case BitOr:
                return "|";
            case BitXor:
                return "^";
            case LeftShift:
                return "<<";
            case RightShift:
                return ">>";
            case UnsignedRightShift:
                return ">>>";
            case And:
                return "&&";
            case Or:
            default:
                return "||";
        }
    }

    private static @Nullable Object operator(Expression e) {
        if (e instanceof J.Binary) {
            return operator((J.Binary) e);
        } else if (e instanceof J.MethodInvocation) {
            return ((J.MethodInvocation) e).getSimpleName();
        }
        return null;
    }

    private static boolean isAdditive(@Nullable Object operator) {
        return "+".equals(operator) || "-".equals(operator);
    }

    private static boolean isMultiplicative(@Nullable Object operator) {
        return "*".equals(operator) || "/".equals(operator) || "%".equals(operator);
    }

    private static boolean isInfix(J.MethodInvocation method) {
        return method.getMarkers().findFirst(InfixNotation.class).isPresent() && method.getSelect() != null &&
               method.getArguments().size() == 1;
    }

    private static boolean isRightAssociative(Expression e) {
        return e instanceof J.MethodInvocation && e.getMarkers().findFirst(RightAssociative.class).isPresent();
    }

    private static boolean is(@Nullable Expression operand, UUID slot) {
        return operand != null && operand.getId().equals(slot);
    }

    private static Expression parenthesize(Expression tree) {
        return new J.Parentheses<>(
                Tree.randomId(),
                tree.getPrefix(),
                Markers.EMPTY,
                JRightPadded.build(tree.withPrefix(Space.EMPTY))
        );
    }
}
