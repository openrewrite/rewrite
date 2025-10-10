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
import org.openrewrite.toml.tree.Toml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeKey extends Recipe {
    @Option(displayName = "Old key path",
            description = "A TOML path expression to locate a key.",
            example = "package.name")
    String oldKeyPath;

    @Option(displayName = "New key",
            description = "The new name for the key.",
            example = "project-name")
    String newKey;

    @Override
    public String getDisplayName() {
        return "Change TOML key";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKeyPath, newKey);
    }

    @Override
    public String getDescription() {
        return "Change a TOML key, while leaving the value intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TomlPathMatcher matcher = new TomlPathMatcher(oldKeyPath);
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);

                if (matcher.matches(getCursor())) {
                    String newKeyName = newKey;
                    if ((newKeyName.startsWith("\"") && newKeyName.endsWith("\"")) ||
                        (newKeyName.startsWith("'") && newKeyName.endsWith("'"))) {
                        newKeyName = newKeyName.substring(1, newKeyName.length() - 1);
                    }

                    if (kv.getKey() instanceof Toml.Identifier) {
                        Toml.Identifier currentKey = (Toml.Identifier) kv.getKey();
                        Toml.Identifier updatedKey = currentKey
                                .withName(newKeyName)
                                .withSource(formatKey(newKeyName));

                        kv = kv.withKey(updatedKey);
                    }
                }

                return kv;
            }

            private String formatKey(String key) {
                if (needsQuotes(key)) {
                    return "\"" + key + "\"";
                }
                return key;
            }

            private boolean needsQuotes(String key) {
                return !key.matches("^[A-Za-z0-9_-]+$"); // TOML bare key pattern
            }
        };
    }
}