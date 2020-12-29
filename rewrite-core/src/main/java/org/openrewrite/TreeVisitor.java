package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

public interface TreeVisitor<R, P> {
    @Nullable
    R defaultValue(@Nullable Tree tree, P p);

    @Nullable
    default R visit(@Nullable Tree tree, P p) {
        return tree == null ? defaultValue(null, p) : tree.accept(this, p);
    }
}
