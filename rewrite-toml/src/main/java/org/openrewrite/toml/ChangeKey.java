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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.toml.internal.TomlKeyParts;
import org.openrewrite.toml.internal.TomlString;
import org.openrewrite.toml.tree.Toml;

import java.util.List;

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

    String displayName = "Change TOML key";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKeyPath, newKey);
    }

    String description = "Change a TOML key, while leaving the value intact.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TomlPathMatcher matcher = new TomlPathMatcher(oldKeyPath);
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);

                if (matcher.matches(getCursor()) && kv.getKey() instanceof Toml.Identifier) {
                    String newKeyName = TomlKeyParts.unquote(newKey);

                    String formattedKey = newKeyName.matches("^[A-Za-z0-9_-]+$") ? newKeyName :
                            "\"" + TomlString.quoteBasic(newKeyName) + "\"";
                    Toml.Identifier key = (Toml.Identifier) kv.getKey();
                    List<int[]> parts = TomlKeyParts.ranges(key.getSource());
                    if (parts.size() == 1) {
                        return kv.withKey(key.withName(newKeyName).withSource(formattedKey));
                    }
                    // Rename the last part of a dotted key, so `package.name` stays in `package`
                    String source = key.getSource();
                    int[] last = parts.get(parts.size() - 1);
                    StringBuilder name = new StringBuilder();
                    for (int i = 0; i < parts.size() - 1; i++) {
                        name.append(source, parts.get(i)[0], parts.get(i)[1]).append('.');
                    }
                    return kv.withKey(key
                            .withName(name.append(formattedKey).toString())
                            .withSource(source.substring(0, last[0]) + formattedKey + source.substring(last[1])));
                }

                return kv;
            }
        };
    }
}
