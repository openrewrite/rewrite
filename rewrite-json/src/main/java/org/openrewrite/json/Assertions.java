/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.json;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.json.tree.Json;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    public static SourceSpecs json(@Language("json") @Nullable String before) {
        return json(before, s -> {
        });
    }

    public static SourceSpecs json(@Language("json") @Nullable String before, Consumer<SourceSpec<Json.Document>> spec) {
        SourceSpec<Json.Document> json = new SourceSpec<>(Json.Document.class, null, JsonParser.builder(), before, null);
        spec.accept(json);
        return json;
    }

    public static SourceSpecs json(@Language("json") @Nullable String before, @Language("json") @Nullable String after) {
        return json(before, after, s -> {
        });
    }

    public static SourceSpecs json(@Language("json") @Nullable String before, @Language("json") @Nullable String after,
                             Consumer<SourceSpec<Json.Document>> spec) {
        SourceSpec<Json.Document> json = new SourceSpec<>(Json.Document.class, null, JsonParser.builder(), before, s -> after);
        spec.accept(json);
        return json;
    }

}
