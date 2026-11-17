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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.internal.TomlKeyParts;
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
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

    String displayName = "Merge TOML table row";

    String description = "Merge a TOML row into an array table. If a row with the same identifying property exists, merge the values. Otherwise, insert a new row.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Set<String> removedKeys = nullEntries(row);
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml visitDocument(Toml.Document document, ExecutionContext ctx) {
                Toml.Document doc = (Toml.Document) super.visitDocument(document, ctx);
                if (doc != document) {
                    return doc;
                }

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
                for (TomlValue value : doc.getValues()) {
                    if (!(value instanceof Toml.Table)) {
                        continue;
                    }
                    Toml.Table table = (Toml.Table) value;
                    if (TableRowMatcher.isNamed(table, tableName)) {
                        String tableIdentifyingValue = TableRowMatcher.getKeyValue(table, identifyingKey);
                        if (identifyingValue.equals(tableIdentifyingValue)) {
                            return doc;
                        }
                    }
                }

                // If no matching row exists, we need to insert one
                Toml.Identifier identifier = new Toml.Identifier(
                        Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        MergeTableRow.this.tableName,
                        MergeTableRow.this.tableName
                );

                List<TomlRightPadded<Toml>> values = new ArrayList<>();
                for (Toml.KeyValue kv : ListUtils.filter(keyValues, kv -> !removedKeys.contains(keyName(kv)))) {
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
                return doc.withValues(ListUtils.concat(doc.getValues(), newTable));

            }

            @Override
            public Toml visitTable(Toml.Table table, ExecutionContext ctx) {
                if (!TableRowMatcher.isNamed(table, tableName)) {
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
                    return table.withValues(
                            ListUtils.concatAll(
                                    ListUtils.map(table.getValues(), value -> {
                                        if (!(value instanceof Toml.KeyValue)) {
                                            return value;
                                        }
                                        Toml.KeyValue existingKv = (Toml.KeyValue) value;

                                        // Check if any incoming key-value matches this one
                                        for (int i = 0; i < incomingKeyValues.size(); i++) {
                                            Toml.KeyValue incomingKv = incomingKeyValues.get(i);
                                            if (areEqual(existingKv.getKey(), incomingKv.getKey())) {
                                                incomingKeyValues.remove(i);

                                                if (removedKeys.contains(keyName(incomingKv))) {
                                                    return null; // Remove the key-value pair
                                                } else if (!areEqual(existingKv.getValue(), incomingKv.getValue())) {
                                                    return existingKv.withValue(incomingKv.getValue());
                                                }
                                                return existingKv; // No change needed
                                            }
                                        }
                                        return existingKv;
                                    }),
                                    ListUtils.map(ListUtils.filter(incomingKeyValues, kv -> !removedKeys.contains(keyName(kv))),
                                            kv -> TableRowMatcher.onItsOwnLine(kv, table.getValues()))
                            )
                    );
                }

                return super.visitTable(table, ctx);
            }

            private List<Toml.KeyValue> parseTomlRow(@Language("toml") String tomlContent) {
                // A row that is not TOML changes nothing, rather than what error recovery made of part of it
                Toml.Document doc = TomlSnippets.parse(NULL_ENTRY.matcher(tomlContent).replaceAll("$1\"\"$3"));
                if (doc == null) {
                    return emptyList();
                }
                List<Toml.KeyValue> result = new ArrayList<>();
                for (TomlValue value : doc.getValues()) {
                    if (value instanceof Toml.KeyValue) {
                        result.add((Toml.KeyValue) value);
                    }
                }
                return result;
            }
        };
    }

    /**
     * An entry such as {@code enabled = null}, which is no TOML but how a row says to remove a property.
     */
    private static final Pattern NULL_ENTRY = Pattern.compile(
            "(?m)^([ \\t]*([A-Za-z0-9_-]+|\"[^\"\\n]*\"|'[^'\\n]*')[ \\t]*=[ \\t]*)null([ \\t]*(?:#.*)?)$");

    private static Set<String> nullEntries(String row) {
        Set<String> keys = new HashSet<>();
        Matcher m = NULL_ENTRY.matcher(row);
        while (m.find()) {
            keys.addAll(TomlKeyParts.split(m.group(2)));
        }
        return keys;
    }

    private static String keyName(Toml.KeyValue kv) {
        return kv.getKey() instanceof Toml.Identifier ? ((Toml.Identifier) kv.getKey()).getName() : "";
    }
}
