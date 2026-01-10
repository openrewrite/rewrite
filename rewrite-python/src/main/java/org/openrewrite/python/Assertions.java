/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public final class Assertions {
    private Assertions() {
    }

    public static SourceSpecs python(@Language("py") @Nullable String before) {
        return python(before, s -> {
        });
    }

    public static SourceSpecs python(@Language("py") @Nullable String before, Consumer<SourceSpec<Py.CompilationUnit>> spec) {
        SourceSpec<Py.CompilationUnit> python = new SourceSpec<>(Py.CompilationUnit.class, null, PythonParser.builder(), before, null);
        spec.accept(python);
        return python;
    }

    public static SourceSpecs python(@Language("py") @Nullable String before, @Language("py") String after) {
        return python(before, after, s -> {
        });
    }

    public static SourceSpecs python(@Language("py") @Nullable String before, @Language("py") String after,
                                     Consumer<SourceSpec<Py.CompilationUnit>> spec) {
        SourceSpec<Py.CompilationUnit> python = new SourceSpec<>(Py.CompilationUnit.class, null, PythonParser.builder(), before, s -> after);
        spec.accept(python);
        return python;
    }
}
