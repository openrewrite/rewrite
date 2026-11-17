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

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

/**
 * Parses the TOML a recipe is given as an option, accepting only what parses without error.
 */
final class TomlSnippets {
    private TomlSnippets() {
    }

    /**
     * @return the document, or null if the text is not TOML, including when error recovery read past what is not
     */
    static Toml.@Nullable Document parse(String toml) {
        SourceFile parsed = new TomlParser().parse(new InMemoryExecutionContext(t -> {
        }), toml).findFirst().orElse(null);
        if (!(parsed instanceof Toml.Document) || parsed.getMarkers().findFirst(ParseExceptionResult.class).isPresent()) {
            return null;
        }
        return (Toml.Document) parsed;
    }

    /**
     * @return the value {@code value} is written as, such as an array for {@code ["a", "b"]}, or null if it is no
     * TOML value
     */
    static @Nullable Toml value(String value) {
        Toml.Document doc = parse("value = " + value);
        if (doc == null || doc.getValues().size() != 1) {
            return null;
        }
        TomlValue kv = doc.getValues().get(0);
        return kv instanceof Toml.KeyValue ? ((Toml.KeyValue) kv).getValue() : null;
    }
}
