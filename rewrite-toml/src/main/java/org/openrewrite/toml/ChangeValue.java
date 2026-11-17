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
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.internal.TomlString;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlType;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A TOML path expression to locate a key.",
            example = "package.version")
    String keyPath;

    @Option(displayName = "New value",
            description = "The new value for the key.",
            example = "\"2.0.0\"")
    String newValue;

    String displayName = "Change TOML value";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", keyPath, newValue);
    }

    String description = "Change the value of a TOML key.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TomlPathMatcher matcher = new TomlPathMatcher(keyPath);
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);

                if (matcher.matches(getCursor())) {
                    Toml newValueNode = parseValue(newValue, kv.getValue().getPrefix());
                    if (!newValueNode.print(getCursor()).equals(kv.getValue().print(getCursor()))) {
                        kv = kv.withValue(newValueNode);
                    }
                }

                return kv;
            }

            private Toml parseValue(String value, Space prefix) {
                // Read the value as TOML does, so a date stays a date, an array an array, and escapes decode
                Toml parsed = TomlSnippets.value(value.trim());
                if (parsed != null) {
                    return parsed.withPrefix(prefix);
                }
                // What is not a TOML value, such as 2.0.0, is meant as a string
                String string = value.trim();
                return new Toml.Literal(
                        Tree.randomId(),
                        prefix,
                        Markers.EMPTY,
                        TomlType.Primitive.String,
                        "\"" + TomlString.quoteBasic(string) + "\"",
                        string
                );
            }
        };
    }
}
