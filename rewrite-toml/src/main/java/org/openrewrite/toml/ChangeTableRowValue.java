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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.internal.TomlString;
import org.openrewrite.toml.tree.Toml;

import java.util.regex.Pattern;

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

    @Option(displayName = "Add if missing",
            description = "Whether to add the property to the matched row when the row does not have it yet. Default is false.",
            required = false)
    @Nullable
    Boolean addIfMissing;

    @JsonCreator
    public ChangeTableRowValue(String tableName, String identifyingKey, String identifyingValue, @Nullable Boolean useRegex,
                               String propertyKey, @Nullable String newValue, @Nullable Boolean addIfMissing) {
        this.tableName = tableName;
        this.identifyingKey = identifyingKey;
        this.identifyingValue = identifyingValue;
        this.useRegex = useRegex;
        this.propertyKey = propertyKey;
        this.newValue = newValue;
        this.addIfMissing = addIfMissing;
    }

    @Deprecated
    public ChangeTableRowValue(String tableName, String identifyingKey, String identifyingValue, @Nullable Boolean useRegex,
                               String propertyKey, @Nullable String newValue) {
        this(tableName, identifyingKey, identifyingValue, useRegex, propertyKey, newValue, null);
    }

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

                if (newValue != null && Boolean.TRUE.equals(addIfMissing) && TomlTableValue.find(table.getValues(), propertyKey) == null) {
                    Toml.KeyValue added = parseKeyValue();
                    return added == null ? table : table.withValues(ListUtils.concat(table.getValues(),
                            TableRowMatcher.onItsOwnLine(added, table.getValues())));
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
                        Toml newValueNode = TomlSnippets.value(newValue.trim());
                        if (newValueNode != null && !SemanticallyEqual.areEqual(kv.getValue(), newValueNode)) {
                            return kv.withValue(newValueNode.withPrefix(kv.getValue().getPrefix()));
                        }
                    }
                    return value;
                }));
            }

            private Toml.@Nullable KeyValue parseKeyValue() {
                String key = BARE_KEY.matcher(propertyKey).matches() ? propertyKey : "\"" + TomlString.quoteBasic(propertyKey) + "\"";
                Toml.Document doc = TomlSnippets.parse(key + " = " + newValue.trim());
                return doc != null && doc.getValues().size() == 1 && doc.getValues().get(0) instanceof Toml.KeyValue ?
                        (Toml.KeyValue) doc.getValues().get(0) : null;
            }
        };
    }

    private static final Pattern BARE_KEY = Pattern.compile("[A-Za-z0-9_-]+");
}
