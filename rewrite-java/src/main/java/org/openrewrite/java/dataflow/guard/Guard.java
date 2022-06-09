package org.openrewrite.java.dataflow.guard;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.*;

import java.util.Optional;

@Incubating(since = "7.25.0")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Guard {
    private final Cursor cursor;
    private final Expression exp;


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
        return getTypeSafe(e)
                .map(type -> {
                    if (TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, type)) {
                        return new Guard(cursor, e);
                    } else {
                        return null;
                    }
                });
    }

    private static Optional<J.ControlParentheses<?>> getControlParenthesesFromParent(Cursor cursor) {
        Statement parent = cursor.dropParentUntil(v -> v instanceof J.If || v instanceof Loop).getValue();
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
                e instanceof J.Unary && ((J.Unary) e).getOperator() == J.Unary.Type.Not;
    }

    private static Optional<JavaType> getTypeSafe(Expression e) {
        return Optional.ofNullable(e.getType());
    }
}
