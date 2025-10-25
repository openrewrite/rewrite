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
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlType;

import java.util.UUID;

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

    @Override
    public String getDisplayName() {
        return "Change TOML value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", keyPath, newValue);
    }

    @Override
    public String getDescription() {
        return "Change the value of a TOML key.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TomlPathMatcher matcher = new TomlPathMatcher(keyPath);
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);

                if (matcher.matches(getCursor()) && !kv.getMarkers().findFirst(Changed.class).isPresent()) {
                    Toml newValueNode = parseValue(newValue, kv.getValue().getPrefix());
                    kv = kv.withValue(newValueNode)
                            .withMarkers(kv.getMarkers().add(new Changed(Tree.randomId())));
                }

                return kv;
            }

            private Toml parseValue(String value, Space prefix) {
                value = value.trim();

                if (value.startsWith("[") && value.endsWith("]")) {
                    // Array - parse as string literal for now
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.String,
                            value,
                            value
                    );
                }

                if (value.startsWith("{") && value.endsWith("}")) {
                    // Inline table - parse as string literal for now
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.String,
                            value,
                            value
                    );
                }

                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    String unquoted = value.substring(1, value.length() - 1);
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.String,
                            value,
                            unquoted
                    );
                }

                if (value.startsWith("\"\"\"") && value.endsWith("\"\"\"")) {
                    String unquoted = value.substring(3, value.length() - 3);
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.String,
                            value,
                            unquoted
                    );
                }

                if (value.startsWith("'''") && value.endsWith("'''")) {
                    String unquoted = value.substring(3, value.length() - 3);
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.String,
                            value,
                            unquoted
                    );
                }

                if ("true".equals(value) || "false".equals(value)) {
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.Boolean,
                            value,
                            Boolean.parseBoolean(value)
                    );
                }

                if ("inf".equals(value) || "+inf".equals(value) || "-inf".equals(value) ||
                    "nan".equals(value) || "+nan".equals(value) || "-nan".equals(value)) {
                    Double doubleValue;
                    switch (value) {
                        case "inf":
                        case "+inf":
                            doubleValue = Double.POSITIVE_INFINITY;
                            break;
                        case "-inf":
                            doubleValue = Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            doubleValue = Double.NaN;
                    }
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.Float,
                            value,
                            doubleValue
                    );
                }

                if (value.contains("T") || value.contains(":")) {
                    // Simplified datetime check
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.OffsetDateTime,
                            value,
                            value
                    );
                }

                try {
                    if (value.contains(".") || value.contains("e") || value.contains("E")) {
                        Double doubleValue = Double.parseDouble(value.replace("_", ""));
                        return new Toml.Literal(
                                Tree.randomId(),
                                prefix,
                                Markers.EMPTY,
                                TomlType.Primitive.Float,
                                value,
                                doubleValue
                        );
                    } else {
                        Long longValue;
                        if (value.startsWith("0x") || value.startsWith("0X")) {
                            longValue = Long.parseLong(value.substring(2).replace("_", ""), 16);
                        } else if (value.startsWith("0o") || value.startsWith("0O")) {
                            longValue = Long.parseLong(value.substring(2).replace("_", ""), 8);
                        } else if (value.startsWith("0b") || value.startsWith("0B")) {
                            longValue = Long.parseLong(value.substring(2).replace("_", ""), 2);
                        } else {
                            longValue = Long.parseLong(value.replace("_", ""));
                        }
                        return new Toml.Literal(
                                Tree.randomId(),
                                prefix,
                                Markers.EMPTY,
                                TomlType.Primitive.Integer,
                                value,
                                longValue
                        );
                    }
                } catch (NumberFormatException e) {
                    // Fallback to string for unparseable values
                    return new Toml.Literal(
                            Tree.randomId(),
                            prefix,
                            Markers.EMPTY,
                            TomlType.Primitive.String,
                            "\"" + value + "\"",
                            value
                    );
                }
            }
        };
    }

    @Value
    @With
    static class Changed implements Marker {
        UUID id;
    }
}
