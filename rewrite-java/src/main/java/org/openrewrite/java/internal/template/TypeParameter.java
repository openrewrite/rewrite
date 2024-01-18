package org.openrewrite.java.internal.template;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;

public class TypeParameter {

    public static String toFullyQualifiedName(@Nullable TemplateParameterParser.TypeContext type) {
        if (type == null) {
            return "java.lang.Object";
        }

        String fqn = "";
        TemplateParameterParser.TypeNameContext typeName = type.typeName();

        if (typeName.Identifier() != null) {
            fqn = typeName.Identifier().getText();
        } else {
            fqn = typeName.FullyQualifiedName().getText();
        }

        TemplateParameterParser.TypeParameterContext typeParam = type.typeParameter();
        if (typeParam != null) {
            fqn += "<";
            if (typeParam.variance() != null) {
                fqn += "? " + typeParam.variance().getText();
            }
            fqn += toFullyQualifiedName(typeParam.type());
            fqn += ">";
        }

        return fqn.replace("$", ".");
    }
}
