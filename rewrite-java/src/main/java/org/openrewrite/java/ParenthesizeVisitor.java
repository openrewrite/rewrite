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
        
            // Check for string concatenation cases that need special handling
            if (isStringConcatenationRequiringParentheses(b, parentBinary)) {
                return parenthesize(b);
            }
        
            if (needsParenthesesForPrecedence(b, parentBinary)) {
                return parenthesize(b);
            }
        } else if (parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(b);
        }

        return b;
    }

    /**
     * Determines if a binary expression within a string concatenation context requires parentheses.
     * Specifically handles cases where removing parentheses would change the result.
     */
    private boolean isStringConcatenationRequiringParentheses(J.Binary inner, J.Binary outer) {
        // We're only concerned with addition operations
        if (inner.getOperator() != J.Binary.Type.Addition || outer.getOperator() != J.Binary.Type.Addition) {
            return false;
        }

        // Case 1: "Value: " + (1 + 2) - arithmetic on right side of string
        // We need parentheses if the outer operation is string concatenation (left operand is string)
        // and the inner operation is purely numeric
        if (isStringType(outer.getLeft().getType()) && !isStringType(inner.getLeft().getType()) && !isStringType(inner.getRight().getType())) {
            // Only need parentheses if this is the right operand of the outer expression
            return outer.getRight() == inner;
        }

        // Case 2: 1 + (2 + " is the result") - string on right side of arithmetic
        // We need parentheses if the inner operation involves a string (either operand)
        // and the outer left operand is numeric
        if (!isStringType(outer.getLeft().getType()) &&
            (isStringType(inner.getLeft().getType()) || isStringType(inner.getRight().getType()))) {
            // Only need parentheses if this is the right operand of the outer expression
            return outer.getRight() == inner;
        }

        // No other cases require special parentheses for string concatenation
        return false;
    }

    private boolean isStringType(@Nullable JavaType type) {
        if (type == JavaType.Primitive.String) {
            return true;
        } else if (type == null || type instanceof JavaType.Primitive) {
            return false;
        }

        return TypeUtils.isAssignableTo("java.lang.String", type);
    }

    private boolean needsParenthesesForPrecedence(J.Binary inner, J.Binary outer) {
        // Special case for string concatenation
        if (inner.getOperator() == J.Binary.Type.Addition && 
            outer.getOperator() == J.Binary.Type.Addition) {
        
            // If inner is arithmetic and outer is string concatenation, inner needs parentheses
            boolean innerIsArithmetic = !isStringType(inner.getLeft().getType()) && 
                                   !isStringType(inner.getRight().getType());
            boolean outerIsString = isStringType(outer.getLeft().getType()) || 
                                isStringType(outer.getType());
        
            if (innerIsArithmetic && outerIsString) {
                return true;
            }
        }
    
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
            parent.getValue() instanceof J.InstanceOf) {
            return parenthesize(t);
        }
        
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
                if (parentBinary.getRight() == i) {
                    return parenthesize(i);
                }
            }
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
            parent.getValue() instanceof J.Ternary) {
            return parenthesize(a);
        }

        return a;
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
                tree.getMarkers(),
                JRightPadded.build(tree.withPrefix(Space.EMPTY))
        );
    }
}