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
package org.openrewrite.kotlin;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.marker.Infix;
import org.openrewrite.kotlin.marker.IsNullSafe;
import org.openrewrite.kotlin.marker.NotIs;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.UUID;

/**
 * Adds the parentheses Kotlin's operator precedence needs for an expression to read the way the tree groups it.
 */
public class KotlinParenthesizeVisitor<P> extends KotlinVisitor<P> {

    // Kotlin's precedence levels, from the loosest to the tightest; every infix call binds at INFIX
    private static final int ASSIGNMENT = 0;
    private static final int DISJUNCTION = 1;
    private static final int CONJUNCTION = 2;
    private static final int EQUALITY = 3;
    private static final int COMPARISON = 4;
    private static final int NAMED_CHECK = 5;
    private static final int ELVIS = 6;
    private static final int INFIX = 7;
    private static final int RANGE = 8;
    private static final int ADDITIVE = 9;
    private static final int MULTIPLICATIVE = 10;
    private static final int AS = 11;
    private static final int PREFIX = 12;
    private static final int POSTFIX = 13;
    private static final int PRIMARY = 14;

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
            return b.getOperator() == J.Binary.Type.LessThan && is(b.getLeft(), slot) && endsWithType(child) ||
                   needsParentheses(child, precedence, slot, b.getLeft(), b.getRight(), b.getOperator(), precedence(b));
        } else if (parent instanceof K.Binary) {
            K.Binary b = (K.Binary) parent;
            return needsParentheses(child, precedence, slot, b.getLeft(), b.getRight(), b.getOperator(), precedence(b));
        } else if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) parent;
            if (isInfix(m)) {
                return needsParentheses(child, precedence, slot, m.getSelect(), m.getArguments().get(0),
                        m.getSimpleName(), INFIX);
            }
            return is(m.getSelect(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.Unary) {
            J.Unary u = (J.Unary) parent;
            if (isPostfix(u)) {
                return precedence < POSTFIX;
            }
            // `-(-x)` printed bare is `--x`, a decrement, so a prefix operator under another keeps its parentheses
            return precedence < PREFIX || precedence == PREFIX && ((J.Unary) child).getOperator() != J.Unary.Type.Not;
        } else if (parent instanceof K.Unary) {
            return precedence < POSTFIX;
        } else if (parent instanceof J.TypeCast) {
            return precedence < AS;
        } else if (parent instanceof J.InstanceOf) {
            return precedence < NAMED_CHECK;
        } else if (parent instanceof J.FieldAccess) {
            return is(((J.FieldAccess) parent).getTarget(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.ArrayAccess) {
            return is(((J.ArrayAccess) parent).getIndexed(), slot) && precedence < POSTFIX;
        } else if (parent instanceof J.MemberReference) {
            return is(((J.MemberReference) parent).getContaining(), slot) && precedence < POSTFIX;
        }
        return false;
    }

    // the operands of a binary operator or infix call
    private static boolean needsParentheses(Expression child, int precedence, UUID slot, @Nullable Expression left,
                                            Expression right, Object operator, int parentPrecedence) {
        if (precedence != parentPrecedence) {
            return precedence < parentPrecedence;
        }
        // Every operator binds to the left, so a bare right operand that binds equally would regroup
        if (is(right, slot)) {
            return true;
        }
        // An equally binding left operand with another operator keeps its parentheses, as Java's do
        Object childOperator = operator(child);
        return is(left, slot) && !operator.equals(childOperator) &&
               !(isAdditive(operator) && isAdditive(childOperator)) &&
               !(isMultiplicative(operator) && isMultiplicative(childOperator));
    }

    // After a type, `<` starts type arguments: `x as Int < y` doesn't parse
    private static boolean endsWithType(Expression e) {
        if (e instanceof J.TypeCast || e instanceof J.InstanceOf) {
            return true;
        } else if (e instanceof J.Binary) {
            return endsWithType(((J.Binary) e).getRight());
        } else if (e instanceof K.Binary) {
            return endsWithType(((K.Binary) e).getRight());
        } else if (e instanceof J.MethodInvocation && isInfix((J.MethodInvocation) e)) {
            return endsWithType(((J.MethodInvocation) e).getArguments().get(0));
        }
        return false;
    }

    private static int precedence(Expression e) {
        if (e instanceof J.Binary) {
            return precedence((J.Binary) e);
        } else if (e instanceof K.Binary) {
            return precedence((K.Binary) e);
        } else if (e instanceof J.Unary) {
            return isPostfix((J.Unary) e) ? POSTFIX : PREFIX;
        } else if (e instanceof K.Unary) {
            return POSTFIX;
        } else if (e instanceof J.TypeCast) {
            return AS;
        } else if (e instanceof J.InstanceOf) {
            return NAMED_CHECK;
        } else if (e instanceof J.MethodInvocation && isInfix((J.MethodInvocation) e)) {
            return INFIX;
        } else if (e instanceof J.Assignment || e instanceof J.AssignmentOperation) {
            return ASSIGNMENT;
        }
        return PRIMARY;
    }

    private static int precedence(J.Binary binary) {
        switch (binary.getOperator()) {
            case Multiplication:
            case Division:
            case Modulo:
                return MULTIPLICATIVE;
            case Addition:
            case Subtraction:
                return ADDITIVE;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                return COMPARISON;
            case Equal:
            case NotEqual:
                return EQUALITY;
            case And:
                return CONJUNCTION;
            case Or:
                return DISJUNCTION;
            default:
                // the infix functions `and`, `or`, `xor`, `shl`, `shr` and `ushr`
                return INFIX;
        }
    }

    private static int precedence(K.Binary binary) {
        switch (binary.getOperator()) {
            case RangeTo:
            case RangeUntil:
                return RANGE;
            case Elvis:
                return ELVIS;
            case Contains:
            case NotContains:
                return NAMED_CHECK;
            case IdentityEquals:
            case IdentityNotEquals:
                return EQUALITY;
            default:
                return POSTFIX;
        }
    }

    private static @Nullable Object operator(Expression e) {
        if (e instanceof J.Binary) {
            return ((J.Binary) e).getOperator();
        } else if (e instanceof K.Binary) {
            return ((K.Binary) e).getOperator();
        } else if (e instanceof J.MethodInvocation) {
            return ((J.MethodInvocation) e).getSimpleName();
        } else if (e instanceof J.InstanceOf) {
            return e.getMarkers().findFirst(NotIs.class).isPresent() ? "!is" : "is";
        } else if (e instanceof J.TypeCast) {
            return e.getMarkers().findFirst(IsNullSafe.class).isPresent() ? "as?" : "as";
        }
        return null;
    }

    private static boolean isAdditive(@Nullable Object operator) {
        return operator == J.Binary.Type.Addition || operator == J.Binary.Type.Subtraction;
    }

    private static boolean isMultiplicative(@Nullable Object operator) {
        return operator == J.Binary.Type.Multiplication || operator == J.Binary.Type.Division ||
               operator == J.Binary.Type.Modulo;
    }

    private static boolean isInfix(J.MethodInvocation method) {
        return method.getMarkers().findFirst(Infix.class).isPresent() && method.getSelect() != null &&
               method.getArguments().size() == 1;
    }

    private static boolean isPostfix(J.Unary unary) {
        return unary.getOperator() == J.Unary.Type.PostIncrement || unary.getOperator() == J.Unary.Type.PostDecrement;
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
