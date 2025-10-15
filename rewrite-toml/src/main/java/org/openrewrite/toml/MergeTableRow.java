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
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.toml.SemanticallyEqual.areEqual;

@Value
@EqualsAndHashCode(callSuper = false)
public class MergeTableRow extends Recipe {
    @Option(displayName = "Table name",
            description = "The name of the TOML array table to merge into (e.g., 'package.contributors').",
            example = "package.contributors")
    String tableName;

    @Option(displayName = "TOML row snippet",
            description = "The TOML key-value pairs to merge. Should contain the objectIdentifyingProperty.",
            example = "name = \"Alice Smith\"\\nemail = \"alice@example.com\"")
    @Language("toml")
    String row;

    @Option(displayName = "Object identifying property",
            description = "The property name used to match existing rows. When a row with this property value exists, it will be merged; otherwise, a new row is inserted. When the original row has more properties than the incoming row, these original properties are preserved. Entries with null values in the incoming row will result in the removal of the property from the original row.",
            example = "name")
    String identifyingKey;

    @Override
    public String getDisplayName() {
        return "Merge TOML table row";
    }

    @Override
    public String getDescription() {
        return "Merge a TOML row into an array table. If a row with the same identifying property exists, merge the values. Otherwise, insert a new row.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(Validated.required("tableName", tableName))
                .and(Validated.required("row", row))
                .and(Validated.required("identifyingKey", identifyingKey));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml visitDocument(Toml.Document document, ExecutionContext ctx) {
                Toml.Document doc = (Toml.Document) super.visitDocument(document, ctx);

                // Parse the incoming row to get identifying value
                List<Toml.KeyValue> keyValues = parseTomlRow(row);
                if (keyValues.isEmpty()) {
                    return doc;
                }

                String identifyingValue = TableRowMatcher.getKeyValue(keyValues, identifyingKey);
                if (identifyingValue == null) {
                    return doc;
                }

                // Check if a matching table row exists (table with same name AND identifying value)
                boolean hasMatchingRow = false;
                for (TomlValue value : doc.getValues()) {
                    if (!(value instanceof Toml.Table)) {
                        continue;
                    }
                    Toml.Table table = (Toml.Table) value;
                    if (table.getName() != null && tableName.equals(table.getName().getName())) {
                        String tableIdentifyingValue = TableRowMatcher.getKeyValue(table, identifyingKey);
                        if (identifyingValue.equals(tableIdentifyingValue)) {
                            hasMatchingRow = true;
                            break;
                        }
                    }
                }

                // If no matching row exists, we need to insert one
                if (!hasMatchingRow) {
                    Toml.Identifier identifier = new Toml.Identifier(
                            Tree.randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            MergeTableRow.this.tableName,
                            MergeTableRow.this.tableName
                    );

                    List<TomlRightPadded<Toml>> values = new ArrayList<>();
                    for (Toml.KeyValue kv : keyValues) {
                        Toml.KeyValue kvWithPrefix = kv.withPrefix(Space.format("\n"));
                        values.add(new TomlRightPadded<>(kvWithPrefix, Space.EMPTY, Markers.EMPTY));
                    }

                    // Add ArrayTable marker to make it render as [[name]] instead of [name]
                    Markers markers = Markers.EMPTY.add(new ArrayTable(Tree.randomId()));

                    Toml.Table newTable = new Toml.Table(
                            Tree.randomId(),
                            Space.format("\n\n"),
                            markers,
                            new TomlRightPadded<>(identifier, Space.EMPTY, Markers.EMPTY),
                            values
                    );

                    // Add the new table to the document
                    List<TomlValue> newValues = new ArrayList<>(doc.getValues());
                    newValues.add(newTable);
                    return doc.withValues(newValues);
                }

                return doc;
            }

            @Override
            public Toml visitTable(Toml.Table table, ExecutionContext ctx) {
                if (table.getName() == null || !tableName.equals(table.getName().getName())) {
                    return super.visitTable(table, ctx);
                }

                // Parse the incoming row
                List<Toml.KeyValue> incomingKeyValues = parseTomlRow(row);
                if (incomingKeyValues.isEmpty()) {
                    return super.visitTable(table, ctx);
                }

                // Get the identifying property value from the incoming row
                String identifyingValue = TableRowMatcher.getKeyValue(incomingKeyValues, identifyingKey);
                if (identifyingValue == null) {
                    return super.visitTable(table, ctx);
                }

                // Check if this table has the matching identifying property
                String tableIdentifyingValue = TableRowMatcher.getKeyValue(table, identifyingKey);

                if (identifyingValue.equals(tableIdentifyingValue)) {
                    // Merge the values
                    return mergeTable(table, incomingKeyValues);
                }

                return super.visitTable(table, ctx);
            }

            private Toml.Table mergeTable(Toml.Table table, List<Toml.KeyValue> incomingKeyValues) {
                List<Toml> currentValues = table.getValues();
                List<Toml> mergedValues = new ArrayList<>(currentValues);
                boolean hasChanges = false;

                // Merge or add each incoming key-value
                for (Toml.KeyValue incomingKv : incomingKeyValues) {
                    boolean found = false;
                    for (int i = 0; i < mergedValues.size(); i++) {
                        Toml value = mergedValues.get(i);
                        if (!(value instanceof Toml.KeyValue)) {
                            continue;
                        }
                        Toml.KeyValue existingKv = (Toml.KeyValue) value;
                        if (areEqual(existingKv.getKey(), incomingKv.getKey())) {
                            //noinspection ConstantConditions
                            if (incomingKv.getValue() == null) {
                                mergedValues.remove(i);
                                hasChanges = true;
                            } else if (!areEqual(existingKv.getValue(), incomingKv.getValue())) {
                                // Check if values are actually different
                                mergedValues.set(i, existingKv.withValue(incomingKv.getValue()));
                                hasChanges = true;
                            }
                            found = true;
                            break;
                        }
                    }

                    //noinspection ConstantConditions
                    if (!found && incomingKv.getValue() != null) {
                        // Add new key-value with proper formatting (but not if it's null)
                        Toml.KeyValue kvToAdd = incomingKv.withPrefix(Space.format("\n"));
                        mergedValues.add(kvToAdd);
                        hasChanges = true;
                    }
                }

                return hasChanges ? table.withValues(mergedValues) : table;
            }

            private List<Toml.KeyValue> parseTomlRow(String tomlContent) {
                List<Toml.KeyValue> result = new ArrayList<>();

                try {
                    Toml.Document doc = new TomlParser().parse(tomlContent)
                            .findFirst()
                            .map(Toml.Document.class::cast)
                            .orElse(null);

                    if (doc != null && !doc.getValues().isEmpty()) {
                        for (TomlValue value : doc.getValues()) {
                            if (value instanceof Toml.KeyValue) {
                                result.add((Toml.KeyValue) value);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Failed to parse, return empty list
                }

                return result;
            }
        };
    }
}
