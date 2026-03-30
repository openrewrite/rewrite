/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public final class Assertions {
    private Assertions() {
    }

    public static SourceSpecs go(@Nullable String before) {
        return go(before, s -> {
        });
    }

    public static SourceSpecs go(@Nullable String before, Consumer<SourceSpec<Go.CompilationUnit>> spec) {
        SourceSpec<Go.CompilationUnit> go = new SourceSpec<>(Go.CompilationUnit.class, null, GolangParser.builder(), before, null);
        spec.accept(go);
        return go;
    }

    public static SourceSpecs go(@Nullable String before, String after) {
        return go(before, after, s -> {
        });
    }

    public static SourceSpecs go(@Nullable String before, String after,
                                       Consumer<SourceSpec<Go.CompilationUnit>> spec) {
        SourceSpec<Go.CompilationUnit> go = new SourceSpec<>(Go.CompilationUnit.class, null, GolangParser.builder(), before, s -> after);
        spec.accept(go);
        return go;
    }
}
