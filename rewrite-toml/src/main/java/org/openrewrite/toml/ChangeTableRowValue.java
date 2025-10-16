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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTableRowValue extends Recipe {
    @Option(displayName = "Table name",
            description = "The name of the TOML array table containing the row to update.",
            example = "package.contributors")
    String tableName;

    @Option(displayName = "Identifying key",
            description = "The key within a table row to match on.",
            example = "name")
    String identifyingKey;

    @Option(displayName = "Identifying value",
            description = "The value to match. Can be a regular expression if useRegex is true.",
            example = "Alice Smith")
    String identifyingValue;

    @Option(displayName = "Use regex",
            description = "Whether to interpret the identifying value as a regular expression. Default is false.",
            required = false)
    @Nullable
    Boolean useRegex;

    @Option(displayName = "Property key",
            description = "The key of the property to update within the matched row.",
            example = "email")
    String propertyKey;

    @Option(displayName = "New value",
            description = "The new value to set for the property. If null, the property will be removed.",
            example = "\"alice.new@example.com\"",
            required = false)
    @Nullable
    String newValue;

    @Override
    public String getDisplayName() {
        return "Change TOML table row value";
    }

    @Override
    public String getDescription() {
        return "Change a value in a TOML table row when the identifying property matches the specified matcher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml visitTable(Toml.Table table, ExecutionContext ctx) {
                // Check if this is the correct table
                if (table.getName() == null || !tableName.equals(table.getName().getName())) {
                    return super.visitTable(table, ctx);
                }

                // Check if this table row matches the identifying criteria
                if (!TableRowMatcher.hasMatchingKeyValue(table, identifyingKey, identifyingValue, useRegex)) {
                    return super.visitTable(table, ctx);
                }

                return table.withValues(ListUtils.map(table.getValues(), value -> {
                    if (!(value instanceof Toml.KeyValue)) {
                        return value;
                    }
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    if (kv.getKey() instanceof Toml.Identifier &&
                            propertyKey.equals(((Toml.Identifier) kv.getKey()).getName())) {
                        if (newValue == null) {
                            return null; // Remove the key-value pair
                        }
                        // Parse the complete key-value pair with the new value
                        Toml.KeyValue newKv = parseKeyValue(propertyKey, newValue);

                        // Check if the key-value is actually different
                        if (newKv != null && !SemanticallyEqual.areEqual(kv, newKv)) {
                            return newKv.withPrefix(kv.getPrefix());
                        }
                    }
                    return value;
                }));
            }

            // Parse a complete TOML key-value pair using TomlParser.
            private Toml.@Nullable KeyValue parseKeyValue(String key, String value) {
                try {
                    // Create a minimal TOML document with the key-value pair
                    @Language("toml") String tomlDoc = key + " = " + value.trim();
                    Toml.Document doc = new TomlParser().parse(tomlDoc)
                            .findFirst()
                            .map(Toml.Document.class::cast)
                            .orElse(null);

                    if (doc != null && !doc.getValues().isEmpty()) {
                        TomlValue firstValue = doc.getValues().get(0);
                        if (firstValue instanceof Toml.KeyValue) {
                            return (Toml.KeyValue) firstValue;
                        }
                    }
                } catch (Exception e) {
                    // If parsing fails, return null
                }

                return null;
            }
        };
    }
}
