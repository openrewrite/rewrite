/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.controlflow;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.*;

import java.util.Optional;

@Incubating(since = "7.25.0")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Guard {
    private final Cursor cursor;

    @Getter
    private final Expression expression;

    public static Optional<Guard> from(Cursor cursor) {
        if (!(cursor.getValue() instanceof Expression)) {
            throw new IllegalArgumentException("Cursor must be on an expression");
        }
        Expression e = cursor.getValue();
        if (e instanceof J.ControlParentheses) {
            if (getControlParenthesesFromParent(cursor).map(c -> c == e).orElse(false)) {
                return Optional.empty();
            }
        }
        return getTypeSafe(cursor, e)
                .map(type -> {
                    if (TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, type)) {
                        return new Guard(cursor, e);
                    } else {
                        return null;
                    }
                });
    }

    private static Optional<J.ControlParentheses<?>> getControlParenthesesFromParent(Cursor cursor) {
        Statement parent = cursor.dropParentUntil(v ->
                v instanceof J.If ||
                        v instanceof Loop ||
                        v instanceof J.Block
        ).getValue();
        J.ControlParentheses<?> parentControlParentheses;
        if (parent instanceof J.If) {
            parentControlParentheses = ((J.If) parent).getIfCondition();
        } else if (parent instanceof J.WhileLoop){
            parentControlParentheses = ((J.WhileLoop) parent).getCondition();
        } else if (parent instanceof J.DoWhileLoop){
            parentControlParentheses = ((J.DoWhileLoop) parent).getWhileCondition();
        } else {
            parentControlParentheses = null;
        }
        return Optional.ofNullable(parentControlParentheses);
    }

    /**
     * Keeping this around in case we need it eventually.
     */
    @Deprecated
    private static boolean isGuardType(Expression e) {
        // method invocation
        // field accesses
        // array accesses
        // cast
        // instanceof
        // boolean operations
        // binary operations on booleans
        // parentheses
        // control parenthesis
        // relational operation
        // negation
        // ternary
        // Assignment
        // literals intentionally excluded
        return e instanceof J.MethodInvocation ||
                e instanceof J.FieldAccess ||
                e instanceof J.ArrayAccess ||
                e instanceof J.TypeCast ||
                e instanceof J.InstanceOf ||
                e instanceof J.Binary ||
                e instanceof J.Parentheses ||
                e instanceof J.ControlParentheses ||
                e instanceof J.Assignment ||
                e instanceof J.AssignmentOperation ||
                e instanceof J.Ternary ||
                (e instanceof J.Unary && ((J.Unary) e).getOperator() == J.Unary.Type.Not);
    }

    private static Optional<JavaType> getTypeSafe(Cursor c, Expression e) {
        JavaType type = e.getType();
        if (type != null && !JavaType.Unknown.getInstance().equals(type)) {
            return Optional.of(type);
        }
        if (e instanceof J.Binary) {
            J.Binary binary = (J.Binary) e;
            switch (binary.getOperator()) {
                case And:
                case Or:
                case Equal:
                case NotEqual:
                case LessThan:
                case LessThanOrEqual:
                case GreaterThan:
                case GreaterThanOrEqual:
                    return Optional.of(JavaType.Primitive.Boolean);
                default:
                    break;
            }
        } else if (e instanceof J.InstanceOf) {
            return Optional.of(JavaType.Primitive.Boolean);
        } else if (e instanceof J.Unary) {
            J.Unary unary = (J.Unary) e;
            if (unary.getOperator() == J.Unary.Type.Not) {
                return Optional.of(JavaType.Primitive.Boolean);
            }
        } else if (e instanceof J.MethodInvocation) {
            J.MethodInvocation methodInvocation = (J.MethodInvocation) e;
            if (methodInvocation.getSimpleName().equals("equals")) {
                return Optional.of(JavaType.Primitive.Boolean);
            }
        }
        J firstEnclosing = c.getParentOrThrow().firstEnclosing(J.class);
        if (firstEnclosing instanceof J.Binary) {
            J.Binary binary = (J.Binary) firstEnclosing;
            if (binary.getLeft() == e || binary.getRight() == e) {
                switch (binary.getOperator()) {
                    case And:
                    case Or:
                        return Optional.of(JavaType.Primitive.Boolean);
                    default:
                        break;
                }
            }
        } else if (firstEnclosing instanceof J.Unary) {
            J.Unary unary = (J.Unary) firstEnclosing;
            if (unary.getExpression() == e && unary.getOperator() == J.Unary.Type.Not) {
                return Optional.of(JavaType.Primitive.Boolean);
            }
        } else if (firstEnclosing instanceof J.Ternary) {
            J.Ternary ternary = (J.Ternary) firstEnclosing;
            if (ternary.getCondition() == e) {
                return Optional.of(JavaType.Primitive.Boolean);
            }
        } else if (firstEnclosing instanceof J.ControlParentheses) {
            J.ControlParentheses<?> controlParentheses = (J.ControlParentheses<?>) firstEnclosing;
            J.If ifStatement = c.getParentOrThrow().firstEnclosing(J.If.class);
            if (controlParentheses.getTree() == e && ifStatement != null && ifStatement.getIfCondition() == controlParentheses) {
                return Optional.of(JavaType.Primitive.Boolean);
            } else {
                Cursor parent = c.dropParentUntil(J.class::isInstance);
                return getTypeSafe(parent, parent.getValue());
            }
        } else if (firstEnclosing instanceof J.Parentheses) {
            J.Parentheses<?> parentheses = (J.Parentheses<?>) firstEnclosing;
            if (parentheses.getTree() == e) {
                Cursor parent = c.dropParentUntil(J.class::isInstance);
                return getTypeSafe(parent, parent.getValue());
            }
        } else if (firstEnclosing instanceof J.VariableDeclarations.NamedVariable) {
            J.VariableDeclarations.NamedVariable namedVariable = (J.VariableDeclarations.NamedVariable) firstEnclosing;
            if (namedVariable.getInitializer() == e) {
                return Optional.ofNullable(namedVariable.getType());
            }
        } else if (firstEnclosing instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) firstEnclosing;
            if (assignment.getAssignment() == e) {
                return Optional.ofNullable(assignment.getType());
            }
        }
        return Optional.empty();
    }
}
