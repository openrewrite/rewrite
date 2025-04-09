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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;

/**
 * Visitor that adds parentheses to Java expressions where needed based on operator precedence
 * and context to ensure correct evaluation order and readability.
 */
public class ParenthesizeVisitor<P> extends JavaVisitor<P> {

    @Override
    public J visitBinary(J.Binary binary, P p) {
        J j = super.visitBinary(binary, p);
        if (!(j instanceof J.Binary)) {
            return j;
        }

        J.Binary b = (J.Binary) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (parent.getValue() instanceof J.Unary) {
            return parenthesize(b);
        } else if (parent.getValue() instanceof J.Binary) {
            J.Binary parentBinary = parent.getValue();
            if (needsParenthesesForPrecedence(b, parentBinary)) {
                return parenthesize(b);
            }
        } else if (parent.getValue() instanceof J.InstanceOf ||
                   parent.getValue() instanceof J.MethodInvocation ||
                   parent.getValue() instanceof J.NewClass) {
            return parenthesize(b);
        }

        return b;
    }

    @Override
    public J visitUnary(J.Unary unary, P p) {
        J j = super.visitUnary(unary, p);
        if (!(j instanceof J.Unary)) {
            return j;
        }

        J.Unary u = (J.Unary) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (parent.getValue() instanceof J.Unary) {
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
        J j = super.visitTernary(ternary, p);
        if (!(j instanceof J.Ternary)) {
            return j;
        }

        J.Ternary t = (J.Ternary) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (parent.getValue() instanceof J.Unary ||
            parent.getValue() instanceof J.Binary ||
            parent.getValue() instanceof J.InstanceOf ||
            parent.getValue() instanceof J.MethodInvocation ||
            parent.getValue() instanceof J.NewClass) {
            return parenthesize(t);
        }
        
        // Don't add parentheses for nested ternary expressions
        // This allows expressions like: a > b ? a > c ? a : c : b > c ? b : c
        // to remain as is without unnecessary parentheses

        return t;
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J j = super.visitInstanceOf(instanceOf, p);
        if (!(j instanceof J.InstanceOf)) {
            return j;
        }

        J.InstanceOf i = (J.InstanceOf) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (parent.getValue() instanceof J.Binary ||
            parent.getValue() instanceof J.Unary ||
            parent.getValue() instanceof J.MethodInvocation ||
            parent.getValue() instanceof J.NewClass) {
            return parenthesize(i);
        }

        return i;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, P p) {
        J j = super.visitAssignment(assignment, p);
        if (!(j instanceof J.Assignment)) {
            return j;
        }

        J.Assignment a = (J.Assignment) j;

        Cursor parent = getCursor().getParentTreeCursor();
        if (parent.getValue() instanceof J.Binary ||
            parent.getValue() instanceof J.Unary ||
            parent.getValue() instanceof J.Ternary ||
            parent.getValue() instanceof J.MethodInvocation ||
            parent.getValue() instanceof J.NewClass) {
            return parenthesize(a);
        }

        return a;
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
            if (!isAssociative(outer.getOperator())) {
                // Check if inner is the right operand of outer
                Cursor cursor = getCursor();
                
                // Find the nearest binary expression cursor
                Cursor binaryCursor = cursor;
                while (binaryCursor != null && !(binaryCursor.getValue() instanceof J.Binary)) {
                    binaryCursor = binaryCursor.getParent();
                }
                
                if (binaryCursor != null && binaryCursor.getValue() == outer) {
                    // If we're in a right operand position of the outer binary
                    Cursor parent = cursor.getParent();
                    while (parent != null && parent != binaryCursor) {
                        cursor = parent;
                        parent = parent.getParent();
                    }
                    
                    return cursor.getValue() == outer.getRight();
                }
            }
        }
        
        // For different operators with the same precedence level that aren't in the same group,
        // we need parentheses for clarity
        return inner.getOperator() != outer.getOperator() && !(isAddSubGroup || isMulDivGroup);
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

    private <T extends J> J parenthesize(T tree) {
        if (tree instanceof J.Parentheses) {
            return tree;
        }

        if (tree instanceof Expression) {
            Expression expression = (Expression) tree;
            return new J.Parentheses<>(
                    Tree.randomId(),
                    expression.getPrefix(),
                    expression.getMarkers(),
                    JRightPadded.build(expression.withPrefix(Space.EMPTY))
            );
        }

        return tree;
    }
}