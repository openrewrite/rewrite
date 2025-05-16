package org.openrewrite.java;

import lombok.AllArgsConstructor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@AllArgsConstructor
public class FullyQualifyTypeReference<P> extends JavaVisitor<P> {
    private final JavaType.FullyQualified typeToFullyQualify;

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        if (fieldAccess.isFullyQualifiedClassReference(typeToFullyQualify.getFullyQualifiedName())) {
            return fieldAccess;
        }
        return super.visitFieldAccess(fieldAccess, p);
    }

    @Override
    public J visitIdentifier(J.Identifier identifier, P p) {
        if (identifier.getFieldType() != null) {
            return super.visitIdentifier(identifier, p);
        }
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(identifier.getType());
        if (fullyQualified != null && typeToFullyQualify.getFullyQualifiedName().equals(fullyQualified.getFullyQualifiedName())) {
            return identifier.withSimpleName(typeToFullyQualify.getFullyQualifiedName());
        }
        return super.visitIdentifier(identifier, p);
    }
}
