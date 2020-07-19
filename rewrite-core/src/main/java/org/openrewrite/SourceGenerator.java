package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

public interface SourceGenerator<S extends SourceFile> {
    /**
     * @return The source to generate. If an existing source file exists at the same destination,
     * it can be returned instead here to implement an "upsert" type of behavior in code generation. If
     * {@code null}, don't generate at all.
     */
    @Nullable
    S getGenerated();
}
