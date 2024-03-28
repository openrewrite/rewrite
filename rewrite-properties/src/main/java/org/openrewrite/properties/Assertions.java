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
package org.openrewrite.properties;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public final class Assertions {
    private Assertions() {}

    public static SourceSpecs properties(@Language("properties") @Nullable String before) {
        return properties(before, s -> {
        });
    }

    public static SourceSpecs properties(@Language("properties") @Nullable String before, Consumer<SourceSpec<Properties.File>> spec) {
        SourceSpec<Properties.File> properties = new SourceSpec<>(Properties.File.class, null, PropertiesParser.builder(), before, null);
        spec.accept(properties);
        return properties;
    }

    public static SourceSpecs properties(@Language("properties") @Nullable String before, @Language("properties") @Nullable String after) {
        return properties(before, after, s -> {
        });
    }

    public static SourceSpecs properties(@Language("properties") @Nullable String before, @Language("properties") @Nullable String after,
                                   Consumer<SourceSpec<Properties.File>> spec) {
        SourceSpec<Properties.File> properties = new SourceSpec<>(Properties.File.class, null, PropertiesParser.builder(), before, s -> after);
        spec.accept(properties);
        return properties;
    }
}
