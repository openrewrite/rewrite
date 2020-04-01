package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.internal.lang.Nullable;

public interface Statement extends J {
    @JsonIgnore
    default boolean isSemicolonTerminated() {
        return false;
    }

    @JsonIgnore
    default boolean hasClassType(@Nullable JavaType.Class classType) {
        if(classType == null) {
            return false;
        }

        if (!(this instanceof J.VariableDecls)) {
            return false;
        }

        J.VariableDecls variable = (J.VariableDecls) this;

        if (variable.getTypeExpr() == null) {
            return false;
        }

        return TypeUtils.isOfClassType(variable.getTypeExpr().getType(), classType.getFullyQualifiedName());
    }
}