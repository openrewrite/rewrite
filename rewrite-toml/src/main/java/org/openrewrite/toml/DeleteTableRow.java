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
import org.openrewrite.toml.internal.TomlKeyParts;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteTableRow extends Recipe {
    @Option(displayName = "Table name",
            description = "The name of the TOML array table to delete (e.g., 'package.contributors').",
            example = "package.contributors")
    String tableName;

    @Option(displayName = "Key",
            description = "The key within a table row to match on.",
            example = "name")
    String identifyingKey;

    @Option(displayName = "Value",
            description = "The value to match. Can be a regular expression if useRegex is true.",
            example = "example-*")
    String identifyingValue;

    @Option(displayName = "Use regex",
            description = "Whether to interpret the value as a regular expression. Default is false.",
            required = false)
    @Nullable
    Boolean useRegex;

    String displayName = "Delete TOML table row";

    String description = "Delete a TOML table row when one of its values matches the specified matcher.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml visitDocument(Toml.Document document, ExecutionContext ctx) {
                Toml.Document doc = (Toml.Document) super.visitDocument(document, ctx);
                doc = doc.withValues(withoutMatchingRows(doc.getValues()));
                if (doc != document && !doc.getValues().isEmpty()) {
                    doc = doc.withValues(ListUtils.mapFirst(doc.getValues(), first -> {
                        TomlValue originalFirst = document.getValues().get(0);
                        if (first != originalFirst) {
                            return first.withPrefix(originalFirst.getPrefix());
                        }
                        return first;
                    }));
                }
                return doc;
            }

            /**
             * Removes each matching row with the subtables that follow it, such as {@code [bin.meta]} after
             * {@code [[bin]]}, up to the next row or a table that holds the rows.
             */
            private List<TomlValue> withoutMatchingRows(List<TomlValue> values) {
                List<String> rowName = TomlKeyParts.split(tableName);
                boolean[] inDeletedRow = {false};
                return ListUtils.map(values, value -> {
                    if (!(value instanceof Toml.Table) || ((Toml.Table) value).getName() == null) {
                        return value;
                    }
                    Toml.Table table = (Toml.Table) value;
                    List<String> name = TomlKeyParts.of(requireNonNull(table.getName()));
                    if (name.equals(rowName)) {
                        inDeletedRow[0] = TableRowMatcher.hasMatchingKeyValue(table, identifyingKey, identifyingValue, useRegex);
                    } else if (startsWith(rowName, name)) {
                        inDeletedRow[0] = false;
                    } else if (!startsWith(name, rowName)) {
                        return value;
                    }
                    return inDeletedRow[0] ? null : value;
                });
            }
        };
    }

    private static boolean startsWith(List<String> key, List<String> prefix) {
        return key.size() >= prefix.size() && key.subList(0, prefix.size()).equals(prefix);
    }
}
