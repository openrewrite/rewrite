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

import org.jspecify.annotations.Nullable;
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
        } else if (parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(b);
        } else if (parent.getValue() instanceof J.Binary) {
            J.Binary parentBinary = parent.getValue();

            // Special handling for string concatenation cases
            if (b.getOperator() == J.Binary.Type.Addition && parentBinary.getOperator() == J.Binary.Type.Addition) {
                // Only apply string concatenation rules if we're actually dealing with strings
                boolean isStringContext = isStringType(b.getType()) ||
                                          isStringType(parentBinary.getType()) ||
                                          isStringType(b.getLeft().getType()) ||
                                          isStringType(b.getRight().getType()) ||
                                          isStringType(parentBinary.getLeft().getType()) ||
                                          isStringType(parentBinary.getRight().getType());

                if (isStringContext) {
                    return handleStringConcatenation(b, parentBinary);
                }
            }

            // Normal precedence rules for non-string operations
            if (needsParenthesesForPrecedence(b, parentBinary)) {
                return parenthesize(b);
            }
        }

        return b;
    }

    private J handleStringConcatenation(J.Binary inner, J.Binary outer) {
        boolean outerLeftString = isStringType(outer.getLeft().getType());
        boolean innerLeftString = isStringType(inner.getLeft().getType());
        boolean innerRightString = isStringType(inner.getRight().getType());

        // Case 1: "Value: " + (1 + 2) - We need parentheses
        // When right operand is a numeric addition inside a string context
        if (outerLeftString && !innerLeftString && !innerRightString && inner.isScope(outer.getRight())) {
            return parenthesize(inner);
        }

        // Case 2: 1 + (2 + " is the result") - We need parentheses
        // When inner contains a string but outer left is numeric
        if (!outerLeftString && (innerLeftString || innerRightString) && inner.isScope(outer.getRight())) {
            return parenthesize(inner);
        }

        // Case 3: 1 + 2 + " is the result" - NO parentheses needed
        // This is a left-associative operation where inner is 1+2 and outer is (1+2)+" is the result"
        boolean outerRightString = isStringType(outer.getRight().getType());
        if (!innerLeftString && !innerRightString && outerRightString && inner.isScope(outer.getRight())) {
            // Don't add parentheses in this case
            return inner;
        }

        // For other string concatenation cases, follow normal precedence rules
        if (needsParenthesesForPrecedence(inner, outer)) {
            return parenthesize(inner);
        }

        return inner;
    }

    private boolean isStringType(@Nullable JavaType type) {
        if (type == JavaType.Primitive.String) {
            return true;
        } else if (type == null || type instanceof JavaType.Primitive) {
            return false;
        }

        return TypeUtils.isAssignableTo("java.lang.String", type);
    }

    private boolean needsParentheses(Expression expr, Object parent) {
        return parent instanceof J.Unary ||
               (parent instanceof J.MethodInvocation && expr.isScope(((J.MethodInvocation) parent).getSelect())) ||
                (expr instanceof J.SwitchExpression);
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

        // For operations with the same precedence, check if they're in the same mathematical group
        boolean isAddSubGroup = isInAddSubGroup(inner.getOperator()) && isInAddSubGroup(outer.getOperator());
        boolean isMulDivGroup = isInMulDivGroup(inner.getOperator()) && isInMulDivGroup(outer.getOperator());

        // If they're in the same group (like addition/subtraction), we need to consider associativity
        if (isAddSubGroup || isMulDivGroup) {
            // For associative operations with the same precedence level in the same group,
            // we don't need parentheses
            if (isAssociative(inner.getOperator()) && isAssociative(outer.getOperator())) {
                return false;
            }

            // For non-associative operations (like subtraction/division), we need parentheses
            // when the inner expression is on the right side of the outer expression
            // OR when the operators are different (like a - (b + c) or a + (b - c))
            if (!isAssociative(inner.getOperator()) || !isAssociative(outer.getOperator()) || inner.getOperator() != outer.getOperator()) {
                // Check if inner is the right operand of outer
                boolean innerIsRightOperandOfOuter = outer.getRight().isScope(inner);

                // For non-associative operators like subtraction, we need parentheses when inner is on the right
                if (innerIsRightOperandOfOuter) {
                    return true;
                }
            }
        }

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
                   parent.getValue() instanceof J.InstanceOf) {
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
        if (parent.getValue() instanceof J.Unary ||
            parent.getValue() instanceof J.MethodInvocation ||
            parent.getValue() instanceof J.NewClass) {
            return parenthesize(i);
        }

        // For binary expressions, we need to be more selective
        if (parent.getValue() instanceof J.Binary) {
            J.Binary parentBinary = parent.getValue();
            int instanceOfPrecedence = 2; // Same precedence as relational operators
            int parentPrecedence = getPrecedence(parentBinary.getOperator());

            if (parentPrecedence > instanceOfPrecedence) {
                if (parentBinary.getRight().isScope(i)) {
                    return parenthesize(i);
                }
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
        if (parent.getValue() instanceof J.Binary ||
            parent.getValue() instanceof J.Unary ||
            parent.getValue() instanceof J.Ternary) {
            return parenthesize(a);
        }

        return a;
    }

    @Override
    public J visitSwitchExpression(J.SwitchExpression switch_, P p) {
        return parenthesize((Expression) super.visitSwitchExpression(switch_, p));
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
