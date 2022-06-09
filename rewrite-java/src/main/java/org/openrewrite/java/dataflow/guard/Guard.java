package org.openrewrite.java.dataflow.guard;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

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

        return getTypeSafe(e)
                .map(type -> {
                    if (TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, type)) {
                        return new Guard(cursor, e);
                    } else {
                        return null;
                    }
                });
    }

    private static Optional<JavaType> getTypeSafe(Expression e) {
        return Optional.ofNullable(e.getType());
    }
}
