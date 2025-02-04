/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.toml;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.toml.tree.Toml;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs toml(@Language("toml") @Nullable String before) {
        return Assertions.toml(before, s -> {
        });
    }

    public static SourceSpecs toml(@Language("toml") @Nullable String before, Consumer<SourceSpec<Toml.Document>> spec) {
        SourceSpec<Toml.Document> toml = new SourceSpec<>(Toml.Document.class, null, TomlParser.builder(), before, null);
        spec.accept(toml);
        return toml;
    }

    public static SourceSpecs toml(@Language("toml") @Nullable String before, @Language("toml") @Nullable String after) {
        return toml(before, after, s -> {
        });
    }

    public static SourceSpecs toml(@Language("toml") @Nullable String before, @Language("toml") @Nullable String after,
                                   Consumer<SourceSpec<Toml.Document>> spec) {
        SourceSpec<Toml.Document> toml = new SourceSpec<>(Toml.Document.class, null, TomlParser.builder(), before, s -> after);
        spec.accept(toml);
        return toml;
    }
}
