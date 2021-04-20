package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = true)
public class ExplicitInitializationVisitor<P> extends JavaIsoVisitor<P> {
    ExplicitInitializationStyle style;

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);

        Cursor variableDeclsCursor = getCursor().dropParentUntil(J.class::isInstance);
        if (!(variableDeclsCursor // J.VariableDecls
                .dropParentUntil(J.class::isInstance) // maybe J.Block
                .dropParentUntil(J.class::isInstance) // maybe J.ClassDecl
                .getValue() instanceof J.ClassDeclaration)) {
            return v;
        }

        JavaType.Primitive primitive = TypeUtils.asPrimitive(variable.getType());
        JavaType.Array array = TypeUtils.asArray(variable.getType());

        J tree = variableDeclsCursor.getValue();
        if(!(tree instanceof J.VariableDeclarations)) {
            return v;
        }

        J.VariableDeclarations variableDecls = (J.VariableDeclarations) tree;

        J.Literal literalInit = variable.getInitializer() instanceof J.Literal ? (J.Literal) variable.getInitializer() : null;

        if (literalInit != null && !variableDecls.hasModifier(J.Modifier.Type.Final)) {
            if (TypeUtils.asClass(variable.getType()) != null && JavaType.Primitive.Null.equals(literalInit.getType())) {
                v = v.withInitializer(null);
            } else if (primitive != null && !Boolean.TRUE.equals(style.getOnlyObjectReferences())) {
                switch (primitive) {
                    case Boolean:
                        if (literalInit.getValue() == Boolean.valueOf(false)) {
                            v = v.withInitializer(null);
                        }
                        break;
                    case Char:
                        if (literalInit.getValue() != null && (Character) literalInit.getValue() == 0) {
                            v = v.withInitializer(null);
                        }
                        break;
                    case Int:
                    case Long:
                    case Short:
                        if (literalInit.getValue() != null && ((Number) literalInit.getValue()).intValue() == 0) {
                            v = v.withInitializer(null);
                        }
                        break;
                }
            } else if (array != null && JavaType.Primitive.Null.equals(literalInit.getType())) {
                v = v.withInitializer(null)
                        .withDimensionsAfterName(ListUtils.map(v.getDimensionsAfterName(), (i, dim) ->
                                i == 0 ? dim.withBefore(Space.EMPTY) : dim));
            }
        }

        return v;
    }
}
