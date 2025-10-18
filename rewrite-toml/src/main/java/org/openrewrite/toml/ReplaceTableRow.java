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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.toml.SemanticallyEqual.areEqual;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceTableRow extends Recipe {
    @Option(displayName = "Table name",
            description = "The name of the TOML array table to replace rows in (e.g., 'package.contributors').",
            example = "package.contributors")
    String tableName;

    @Option(displayName = "TOML row snippet",
            description = "The TOML key-value pairs to replace with. Should contain the objectIdentifyingProperty.",
            example = "name = \"Alice Smith\"\\nemail = \"alice@example.com\"")
    @Language("toml")
    String row;

    @Option(displayName = "Object identifying property",
            description = "The property name used to match existing rows. When a row with this property value exists, it will be replaced; otherwise, a new row will not be inserted (see MergeTableRow).",
            example = "name")
    String identifyingKey;

    @Override
    public String getDisplayName() {
        return "Replace TOML table row";
    }

    @Override
    public String getDescription() {
        return "Replace a TOML table row with new content. If a row with the same identifying property exists, replace it entirely.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TomlVisitor<ExecutionContext>() {

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
                    Space firstPrefix = table.getValues().isEmpty() ? Space.format("\n") : table.getValues().get(0).getPrefix();

                    return table.withValues(
                            ListUtils.mapFirst(
                                    ListUtils.concatAll(
                                            ListUtils.map(table.getValues(), value -> {
                                                if (!(value instanceof Toml.KeyValue)) {
                                                    return null; // Remove non-KeyValue entries in replace mode
                                                }
                                                Toml.KeyValue existingKv = (Toml.KeyValue) value;

                                                // Check if this key exists in the incoming values
                                                for (int i = 0; i < incomingKeyValues.size(); i++) {
                                                    Toml.KeyValue incomingKv = incomingKeyValues.get(i);
                                                    if (areEqual(existingKv.getKey(), incomingKv.getKey())) {
                                                        incomingKeyValues.remove(i);

                                                        // Replace if value is different
                                                        if (!areEqual(existingKv.getValue(), incomingKv.getValue())) {
                                                            return incomingKv.withPrefix(existingKv.getPrefix());
                                                        }
                                                        return existingKv; // No change needed
                                                    }
                                                }
                                                return null; // Remove key-value not in incoming
                                            }),
                                            incomingKeyValues
                                    ), first -> first.withPrefix(firstPrefix)
                            )
                    );
                }

                return super.visitTable(table, ctx);
            }

            private List<Toml.KeyValue> parseTomlRow(@Language("toml") String tomlContent) {
                try {
                    Toml.Document doc = new TomlParser().parse(tomlContent)
                            .findFirst()
                            .map(Toml.Document.class::cast)
                            .orElse(null);

                    if (doc != null && !doc.getValues().isEmpty()) {
                        List<Toml.KeyValue> result = new ArrayList<>();
                        for (TomlValue value : doc.getValues()) {
                            if (value instanceof Toml.KeyValue) {
                                result.add((Toml.KeyValue) value);
                            }
                        }
                        return result;
                    }

                } catch (Exception e) {
                    // Failed to parse, return empty list
                }
                return emptyList();
            }
        };
    }
}
