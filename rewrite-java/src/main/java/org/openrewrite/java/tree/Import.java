package org.openrewrite.java.tree;

import org.jspecify.annotations.Nullable;

public interface Import extends J, Statement {

    @Nullable
    FieldAccess getQualid();

}
