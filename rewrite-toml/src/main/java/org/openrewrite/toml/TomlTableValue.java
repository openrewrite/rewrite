/*
 * Copyright 2026 the original author or authors.
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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;

import java.util.List;
import java.util.function.BiFunction;

import static org.openrewrite.Tree.randomId;

/**
 * Utilities for reading and updating string-valued properties in TOML tables.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TomlTableValue {

    public static Toml.@Nullable KeyValue find(Toml.Table table, String key) {
        return find(table.getValues(), key);
    }

    public static Toml.@Nullable KeyValue find(List<? extends Toml> values, String key) {
        for (Toml value : values) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue keyValue = (Toml.KeyValue) value;
                if (keyValue.getKey() instanceof Toml.Identifier &&
                    key.equals(((Toml.Identifier) keyValue.getKey()).getName())) {
                    return keyValue;
                }
            }
        }
        return null;
    }

    public static @Nullable String getString(Toml.Table table, String key) {
        Toml.KeyValue keyValue = find(table, key);
        if (keyValue == null || !(keyValue.getValue() instanceof Toml.Literal)) {
            return null;
        }
        Object value = ((Toml.Literal) keyValue.getValue()).getValue();
        return value instanceof String ? (String) value : null;
    }

    public static String quoted(Toml.Literal literal, String value) {
        String source = literal.getSource();
        String delimiter = source.startsWith("\"\"\"") || source.startsWith("'''") ?
                source.substring(0, 3) :
                source.startsWith("\"") || source.startsWith("'") ? source.substring(0, 1) : "\"";
        if (delimiter.charAt(0) == '"') {
            value = value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
        return delimiter + value + delimiter;
    }

    /**
     * Replaces an existing string-valued property, keeping its quote style.
     *
     * @return the table unchanged if {@code key} is absent, not a string, or already {@code value}
     */
    public static Toml.Table withString(Toml.Table table, String key, String value) {
        return withStringProperty(table, key, (keyValue, literal) -> value.equals(literal.getValue()) ? keyValue :
                keyValue.withValue(literal.withSource(quoted(literal, value)).withValue(value)));
    }

    /**
     * Renames an existing string-valued property, keeping its place, padding and value.
     *
     * @return the table unchanged if {@code key} is absent or not a string
     */
    public static Toml.Table withKey(Toml.Table table, String key, String newKey) {
        return withStringProperty(table, key, (keyValue, literal) -> {
            Toml.Identifier identifier = (Toml.Identifier) keyValue.getKey();
            return keyValue.withKey(new Toml.Identifier(identifier.getId(), identifier.getPrefix(), identifier.getMarkers(), newKey, newKey));
        });
    }

    /**
     * Replaces an existing string-valued property or appends a new property when absent.
     * Existing comma and whitespace padding is preserved.
     */
    public static Toml.Table withStringOrAdd(Toml.Table table, String key, String value) {
        if (find(table, key) != null) {
            return withString(table, key, value);
        }
        Toml.Identifier identifier = new Toml.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, key, key);
        Toml.Literal literal = new Toml.Literal(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                TomlType.Primitive.String, "\"" + value + "\"", value);
        Toml.KeyValue keyValue = new Toml.KeyValue(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                new TomlRightPadded<>(identifier, Space.SINGLE_SPACE, Markers.EMPTY), literal);
        return table.getPadding().withValues(ListUtils.concat(
                ListUtils.mapLast(table.getPadding().getValues(), padded -> padded.withAfter(Space.EMPTY)),
                new TomlRightPadded<>(keyValue, Space.SINGLE_SPACE, Markers.EMPTY)));
    }

    private static Toml.Table withStringProperty(Toml.Table table, String key,
                                                 BiFunction<Toml.KeyValue, Toml.Literal, Toml.KeyValue> edit) {
        Toml.KeyValue keyValue = find(table, key);
        if (keyValue == null || !(keyValue.getValue() instanceof Toml.Literal) ||
            !(((Toml.Literal) keyValue.getValue()).getValue() instanceof String)) {
            return table;
        }
        Toml.KeyValue edited = edit.apply(keyValue, (Toml.Literal) keyValue.getValue());
        return edited == keyValue ? table : table.withValues(ListUtils.map(table.getValues(), value -> value == keyValue ? edited : value));
    }
}
