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
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

/**
 * Visitor that adds parentheses to Java expressions where needed based on operator precedence
 * and context to ensure correct evaluation order and readability.
 */
public class ParenthesizeVisitor<P> extends JavaVisitor<P> {

    private final boolean recursive;

    public ParenthesizeVisitor() {
        this.recursive = true;
    }

    private ParenthesizeVisitor(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Determines whether an expression needs to be parenthesized when replacing another expression
     * in the tree.
     *
     * @param newTree the expression that will replace an existing expression
     * @param cursor  cursor to the expression being replaced
     * @return either newTree itself or newTree wrapped in parentheses if needed
     */
    public static Expression maybeParenthesize(Expression newTree, Cursor cursor) {
        if (!(newTree instanceof J.Binary ||
              newTree instanceof J.Unary ||
              newTree instanceof J.Ternary ||
              newTree instanceof J.Assignment ||
              newTree instanceof J.AssignmentOperation ||
              newTree instanceof J.InstanceOf ||
              newTree instanceof J.TypeCast ||
              newTree instanceof J.SwitchExpression)) {
            return newTree;
        }

        Tree originalTree = cursor.getValue();
        Expression newTreeWithOriginalId = newTree.withId(originalTree.getId());

        J result = new ParenthesizeVisitor<>(false).visit(newTreeWithOriginalId, 0, cursor.getParentOrThrow());
        if (result instanceof J.Parentheses) {
            return new J.Parentheses<>(
                    Tree.randomId(),
                    newTree.getPrefix(),
                    Markers.EMPTY,
                    JRightPadded.build(newTree.withPrefix(Space.EMPTY))
            );
        }

        return newTree;
    }

    @Override
    public J visitBinary(J.Binary binary, P p) {
        J j = recursive ? super.visitBinary(binary, p) : binary;
        if (!(j instanceof J.Binary)) {
            return j;
        }

        J.Binary b = (J.Binary) j;
        Cursor parent = getCursor().getParentTreeCursor();

        if (needsParentheses(b, parent.getValue())) {
            return parenthesize(b);
        } else if (parent.getValue() instanceof J.InstanceOf || parent.getValue() instanceof J.TypeCast) {
            return parenthesize(b);
        } else if (parent.getValue() instanceof J.Binary && needsParenthesesForPrecedence(b, parent.getValue())) {
            return parenthesize(b);
        }

        return b;
    }

    private boolean needsParentheses(Expression expr, Object parent) {
        return parent instanceof J.Unary ||
               isSelectOrTarget(expr, parent) ||
                (expr instanceof J.SwitchExpression);
    }

    // a method select, field access target, indexed array or method reference qualifier binds tighter than operators
    private boolean isSelectOrTarget(Expression expr, Object parent) {
        return (parent instanceof J.MethodInvocation && expr.isScope(((J.MethodInvocation) parent).getSelect())) ||
               (parent instanceof J.FieldAccess && expr.isScope(((J.FieldAccess) parent).getTarget())) ||
               (parent instanceof J.ArrayAccess && expr.isScope(((J.ArrayAccess) parent).getIndexed())) ||
               (parent instanceof J.MemberReference && expr.isScope(((J.MemberReference) parent).getContaining()));
    }

    private boolean needsParenthesesForPrecedence(J.Binary inner, J.Binary outer) {
        // Get precedence levels (higher number = higher precedence)
        int innerPrecedence = getPrecedence(inner.getOperator());
        int outerPrecedence = getPrecedence(outer.getOperator());

        // If inner has higher precedence than outer, it does NOT need parentheses
        if (innerPrecedence > outerPrecedence) {
            return false;
        }

        // If inner has lower precedence than outer, it needs parentheses
        if (innerPrecedence < outerPrecedence) {
            return true;
        }

        // From here, we're dealing with equal precedence

        // Every J.Binary operator is left-associative, so a bare right operand that binds equally would regroup
        if (outer.getRight().isScope(inner)) {
            return true;
        }

        // For operations with the same precedence, check if they're in the same mathematical group
        boolean isAddSubGroup = isInAddSubGroup(inner.getOperator()) && isInAddSubGroup(outer.getOperator());
        boolean isMulDivGroup = isInMulDivGroup(inner.getOperator()) && isInMulDivGroup(outer.getOperator());

        // For different operators with the same precedence level that aren't in the same group,
        // we need parentheses for clarity
        return inner.getOperator() != outer.getOperator() && !(isAddSubGroup || isMulDivGroup);
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, P p) {
        J j = recursive ? super.visitTypeCast(typeCast, p) : typeCast;
        if (!(j instanceof J.TypeCast)) {
            return j;
        }

        J.TypeCast tc = (J.TypeCast) j;
        Cursor parent = getCursor().getParentTreeCursor();

        if (needsParentheses(tc, parent.getValue())) {
            return parenthesize(tc);
        } else if (parent.getValue() instanceof J.Binary ||
                   parent.getValue() instanceof J.Unary ||
                   parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(tc);
        }
        return tc;
    }

    @Override
    public J visitUnary(J.Unary unary, P p) {
        J j = recursive ? super.visitUnary(unary, p) : unary;
        if (!(j instanceof J.Unary)) {
            return j;
        }

        J.Unary u = (J.Unary) j;
        Cursor parent = getCursor().getParentTreeCursor();

        if (u.getOperator() == J.Unary.Type.Not && parent.getValue() instanceof J.Unary) {
            // no parens for `!!true` but for `-(-1)` and for `+(+1)`
            return u;
        } else if (needsParentheses(u, parent.getValue())) {
            return parenthesize(u);
        } else if (parent.getValue() instanceof J.TypeCast && isPrefixPlusOrMinus(u.getOperator()) &&
                   !(((J.TypeCast) parent.getValue()).getClazz().getTree() instanceof J.Primitive)) {
            // `(Integer) -i` reads as `Integer - i`, since only a primitive cast may precede `+`, `-`, `++` or `--`
            return parenthesize(u);
        } else if (parent.getValue() instanceof J.Unary) {
            J.Unary parentUnary = parent.getValue();
            // Ensure proper precedence for nested unary operations
            if (parentUnary.getOperator() != u.getOperator()) {
                return parenthesize(u);
            }
        }

        return u;
    }

    @Override
    public J visitTernary(J.Ternary ternary, P p) {
        J j = recursive ? super.visitTernary(ternary, p) : ternary;
        if (!(j instanceof J.Ternary)) {
            return j;
        }

        J.Ternary t = (J.Ternary) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (needsParentheses(t, parent.getValue())) {
            return parenthesize(t);
        } else if (parent.getValue() instanceof J.Binary ||
                   parent.getValue() instanceof J.InstanceOf ||
                   parent.getValue() instanceof J.TypeCast) {
            return parenthesize(t);
        } else if (parent.getValue() instanceof J.Ternary &&
                   t.isScope(((J.Ternary) parent.getValue()).getCondition())) {
            // `a ? b : c ? d : e` groups as `a ? b : (c ? d : e)`, so a ternary condition needs parentheses
            return parenthesize(t);
        }

        return t;
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J j = recursive ? super.visitInstanceOf(instanceOf, p) : instanceOf;
        if (!(j instanceof J.InstanceOf)) {
            return j;
        }

        J.InstanceOf i = (J.InstanceOf) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (needsParentheses(i, parent.getValue()) || parent.getValue() instanceof J.TypeCast) {
            return parenthesize(i);
        }

        // For binary expressions, we need to be more selective
        if (parent.getValue() instanceof J.Binary) {
            J.Binary parentBinary = parent.getValue();
            int instanceOfPrecedence = 2; // Same precedence as relational operators
            int parentPrecedence = getPrecedence(parentBinary.getOperator());

            if (parentPrecedence > instanceOfPrecedence) {
                return parenthesize(i);
            }
        }

        return i;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, P p) {
        J j = recursive ? super.visitAssignment(assignment, p) : assignment;
        if (!(j instanceof J.Assignment)) {
            return j;
        }

        J.Assignment a = (J.Assignment) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (needsParentheses(a, parent.getValue()) ||
            parent.getValue() instanceof J.Binary ||
            parent.getValue() instanceof J.Ternary ||
            parent.getValue() instanceof J.TypeCast ||
            parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(a);
        }

        return a;
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J j = recursive ? super.visitAssignmentOperation(assignOp, p) : assignOp;
        if (!(j instanceof J.AssignmentOperation)) {
            return j;
        }

        J.AssignmentOperation a = (J.AssignmentOperation) j;

        // a compound assignment binds as loosely as an assignment does
        Cursor parent = getCursor().getParentTreeCursor();
        if (needsParentheses(a, parent.getValue()) ||
            parent.getValue() instanceof J.Binary ||
            parent.getValue() instanceof J.Ternary ||
            parent.getValue() instanceof J.TypeCast ||
            parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(a);
        }

        return a;
    }

    @Override
    public J visitSwitchExpression(J.SwitchExpression switch_, P p) {
        return parenthesize((Expression) super.visitSwitchExpression(switch_, p));
    }

    private boolean isPrefixPlusOrMinus(J.Unary.Type operator) {
        return operator == J.Unary.Type.Positive || operator == J.Unary.Type.Negative ||
               operator == J.Unary.Type.PreIncrement || operator == J.Unary.Type.PreDecrement;
    }

    private boolean isInAddSubGroup(J.Binary.Type operator) {
        return operator == J.Binary.Type.Addition || operator == J.Binary.Type.Subtraction;
    }

    private boolean isInMulDivGroup(J.Binary.Type operator) {
        return operator == J.Binary.Type.Multiplication || operator == J.Binary.Type.Division || operator == J.Binary.Type.Modulo;
    }

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
            case BitOr:
                return -1;
            case And:
                return -2;
            case Or:
                return -3;
            case BitXor:
            default:
                return 0;
        }
    }

    private <T extends Expression> Expression parenthesize(T tree) {
        if (tree instanceof J.Parentheses) {
            return tree;
        }

        return new J.Parentheses<>(
                Tree.randomId(),
                tree.getPrefix(),
                Markers.EMPTY,
                JRightPadded.build(tree.withPrefix(Space.EMPTY))
        );
    }
}
