package org.openrewrite.python;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.python.tree.PythonFile;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {}

    public static SourceSpecs python(
            @Language("py") @Nullable String before
    ) {
        return python(before, s -> {});
    }

    public static SourceSpecs python(
            @Language("py") @Nullable String before,
            Consumer<SourceSpec<PythonFile>> spec
    ) {
        SourceSpec<PythonFile> python = new SourceSpec<>(
                PythonFile.class,
                null,
                PythonParser.builder(),
                before,
                null
        );
        spec.accept(python);
        return python;
    }

    public static SourceSpecs python(
            @Language("py") @Nullable String before,
            @Language("py") @Nullable String after
    ) {
        return python(before, after, s -> {});
    }

    public static SourceSpecs python(
            @Language("py") @Nullable String before,
            @Language("py") @Nullable String after,
            Consumer<SourceSpec<PythonFile>> spec
    ) {
        SourceSpec<PythonFile> python = new SourceSpec<>(
                PythonFile.class,
                null,
                PythonParser.builder(),
                before,
                s -> after
        );
        spec.accept(python);
        return python;
    }
}
