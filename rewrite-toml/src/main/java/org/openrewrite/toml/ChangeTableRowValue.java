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
import org.openrewrite.toml.internal.TomlKeyParts;
import org.openrewrite.toml.internal.TomlString;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;


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

    @Option(displayName = "Add if missing",
            description = "Whether to add the property to each matched row that does not have it yet. Default is false. " +
                          "Unlike `MergeTableRow`, this matches rows by a regular expression when asked to, and never adds a row.",
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
            final List<String> property = TomlKeyParts.split(propertyKey);

            @Override
            public Toml visitDocument(Toml.Document document, ExecutionContext ctx) {
                getCursor().putMessage(ROWS_WITH_PROPERTY_IN_SUBTABLE, rowsWithPropertyInSubtable(document));
                return TableRowMatcher.keepCommentsOnLastLines((Toml.Document) super.visitDocument(document, ctx));
            }

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

                if (newValue != null && Boolean.TRUE.equals(addIfMissing) && isMissing(table)) {
                    Toml.KeyValue added = parseKeyValue();
                    return added == null ? table : TableRowMatcher.append(table, table.getValues(), singletonList(added));
                }

                return table.withValues(ListUtils.map(table.getValues(), value -> {
                    if (!(value instanceof Toml.KeyValue)) {
                        return value;
                    }
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    if (kv.getKey() instanceof Toml.Identifier &&
                            TomlKeyParts.of((Toml.Identifier) kv.getKey()).equals(property)) {
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

            /**
             * Whether adding the property to the row keeps it valid TOML: no key-value of the row, and no subtable of it,
             * already defines the property, a table holding it, or a key under it.
             */
            private boolean isMissing(Toml.Table row) {
                Set<UUID> rows = getCursor().getNearestMessage(ROWS_WITH_PROPERTY_IN_SUBTABLE);
                if (rows != null && rows.contains(row.getId())) {
                    return false;
                }
                for (Toml value : row.getValues()) {
                    if (value instanceof Toml.KeyValue && ((Toml.KeyValue) value).getKey() instanceof Toml.Identifier &&
                        overlaps(TomlKeyParts.of((Toml.Identifier) ((Toml.KeyValue) value).getKey()))) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * The rows followed by a subtable, such as {@code [pkg.meta]} after {@code [[pkg]]}, that defines the
             * property, a table holding it, or a key under it.
             */
            private Set<UUID> rowsWithPropertyInSubtable(Toml.Document document) {
                List<String> rowName = TomlKeyParts.split(tableName);
                Set<UUID> rows = new HashSet<>();
                Toml.Table row = null;
                for (TomlValue value : document.getValues()) {
                    if (!(value instanceof Toml.Table) || ((Toml.Table) value).getName() == null) {
                        continue;
                    }
                    Toml.Table table = (Toml.Table) value;
                    List<String> name = TomlKeyParts.of(requireNonNull(table.getName()));
                    if (name.equals(rowName)) {
                        row = table;
                    } else if (row != null && startsWith(name, rowName) &&
                               overlaps(name.subList(rowName.size(), name.size()))) {
                        rows.add(row.getId());
                    }
                }
                return rows;
            }

            private boolean overlaps(List<String> key) {
                return startsWith(key, property) || startsWith(property, key);
            }

            /**
             * @return the property as a key-value, its key as TOML reads {@code propertyKey}, so {@code meta.url} is a
             * dotted key as it is when changed, and its value as {@link ChangeValue} reads the new value
             */
            private Toml.@Nullable KeyValue parseKeyValue() {
                Toml.Document doc = TomlSnippets.parse(propertyKey + " = 0");
                if (doc == null || doc.getValues().size() != 1 || !(doc.getValues().get(0) instanceof Toml.KeyValue)) {
                    doc = TomlSnippets.parse("\"" + TomlString.quoteBasic(propertyKey) + "\" = 0");
                }
                if (doc == null || newValue == null) {
                    return null;
                }
                Toml.KeyValue kv = (Toml.KeyValue) doc.getValues().get(0);
                return kv.withValue(TomlSnippets.valueOrString(newValue).withPrefix(kv.getValue().getPrefix()));
            }
        };
    }

    private static final String ROWS_WITH_PROPERTY_IN_SUBTABLE = "ROWS_WITH_PROPERTY_IN_SUBTABLE";

    private static boolean startsWith(List<String> key, List<String> prefix) {
        return key.size() >= prefix.size() && key.subList(0, prefix.size()).equals(prefix);
    }
}
