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
import org.openrewrite.toml.tree.TomlValue;

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

    @Override
    public String getDisplayName() {
        return "Delete TOML table row";
    }

    @Override
    public String getDescription() {
        return "Delete a TOML table row when one of its values matches the specified matcher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml visitDocument(Toml.Document document, ExecutionContext ctx) {
                Toml.Document doc = (Toml.Document) super.visitDocument(document, ctx);
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

            @Override
            public @Nullable Toml visitTable(Toml.Table table, ExecutionContext ctx) {
                Toml.Table t = (Toml.Table) super.visitTable(table, ctx);

                if (t.getName() != null && tableName.equals(t.getName().getName()) && TableRowMatcher.hasMatchingKeyValue(t, identifyingKey, identifyingValue, useRegex)) {
                    return null; // Delete the table
                }

                return t;
            }
        };
    }
}
