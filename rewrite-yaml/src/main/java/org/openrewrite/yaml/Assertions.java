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
package org.openrewrite.yaml;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    public static SourceSpecs yaml(@Language("yml") @Nullable String before) {
        return yaml(before, s -> {
        });
    }

    public static SourceSpecs yaml(@Language("yml") @Nullable String before, Consumer<SourceSpec<Yaml.Documents>> spec) {
        SourceSpec<Yaml.Documents> yaml = new SourceSpec<>(Yaml.Documents.class, null, YamlParser.builder(),  before, null);
        spec.accept(yaml);
        return yaml;
    }

    public static SourceSpecs yaml(@Language("yml") @Nullable String before, @Language("yml") @Nullable String after) {
        return yaml(before, after, s -> {
        });
    }

    public static SourceSpecs yaml(@Language("yml") @Nullable String before, @Language("yml") @Nullable String after,
                             Consumer<SourceSpec<Yaml.Documents>> spec) {
        SourceSpec<Yaml.Documents> yaml = new SourceSpec<>(Yaml.Documents.class, null, YamlParser.builder(), before, s -> after);
        spec.accept(yaml);
        return yaml;
    }



}
