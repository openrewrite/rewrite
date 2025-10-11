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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteKey extends Recipe {
    @Option(displayName = "Key path",
            description = "A TOML path expression to locate a key.",
            example = "package.keywords")
    String keyPath;

    @Override
    public String getDisplayName() {
        return "Delete TOML key";
    }

    @Override
    public String getDescription() {
        return "Delete a TOML key-value pair.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TomlPathMatcher matcher = new TomlPathMatcher(keyPath);
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                Toml.Document doc = super.visitDocument(document, ctx);

                return doc.withValues(ListUtils.map(doc.getValues(), value -> {
                    if (value instanceof Toml.KeyValue) {
                        Cursor valueCursor = new Cursor(getCursor(), value);
                        if (matcher.matches(valueCursor)) {
                            return null;
                        }
                    }
                    return value;
                }));
            }

            @Override
            public Toml.Table visitTable(Toml.Table table, ExecutionContext ctx) {
                Toml.Table t = super.visitTable(table, ctx);

                return t.withValues(ListUtils.map(t.getValues(), value -> {
                    if (value instanceof Toml.KeyValue) {
                        Cursor valueCursor = new Cursor(getCursor(), value);
                        if (matcher.matches(valueCursor)) {
                            return null;
                        }
                    }
                    return value;
                }));
            }
        };
    }
}
