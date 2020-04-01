package org.openrewrite.java.tree;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;

/**
 * A tree representing a simple or fully qualified name
 */
public interface NameTree extends J {
    @Nullable
    JavaType getType();

    <T extends Tree> T withType(@Nullable JavaType type);
}
