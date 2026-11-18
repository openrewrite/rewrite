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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.tree.Toml;

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
            description = "The new value to set for the property, read as `ChangeValue` reads it: as TOML, or else as a string. If null, the property will be removed.",
            example = "\"alice.new@example.com\"",
            required = false)
    @Nullable
    String newValue;

    String displayName = "Change TOML table row value";

    String description = "Change a value in a TOML table row when the identifying property matches the specified matcher.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml visitTable(Toml.Table table, ExecutionContext ctx) {
                // Check if this is the correct table
                if (!TableRowMatcher.isNamed(table, tableName)) {
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
                        // Replace only the value, so the key keeps how it is written and aligned
                        Toml newValueNode = TomlSnippets.valueOrString(newValue);
                        if (!SemanticallyEqual.areEqual(kv.getValue(), newValueNode)) {
                            return kv.withValue(newValueNode.withPrefix(kv.getValue().getPrefix()));
                        }
                    }
                    return value;
                }));
            }
        };
    }
}
