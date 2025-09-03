package org.openrewrite.java.tree;

import org.jspecify.annotations.Nullable;

public interface Import extends J, Statement {

    @Nullable
    FieldAccess getQualid();

    org.openrewrite.java.tree.Import withQualid(FieldAccess qualid);

    default boolean isStatic() {
        return false;
    }

    @Nullable
    default String getTypeName() {
        return null;
    }
    @Nullable
    default String getPackageName() {
        return null;
    }
    @Nullable
    default String getClassName() {
        return null;
    }
    @Nullable
    default String getModuleName() {
        return null;
    }

}
