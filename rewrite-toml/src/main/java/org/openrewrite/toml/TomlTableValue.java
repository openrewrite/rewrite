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

import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

/**
 * Utilities for reading and updating string-valued properties in TOML inline tables.
 */
public final class TomlTableValue {
    private TomlTableValue() {
    }

    /**
     * Checks whether an inline table contains a property with the supplied key.
     *
     * @param table the inline table to inspect
     * @param key the property key
     * @return {@code true} when the property exists
     */
    public static boolean has(Toml.Table table, String key) {
        return find(table, key) != null;
    }

    /**
     * Gets a string-valued property from an inline table.
     *
     * @param table the inline table to inspect
     * @param key the property key
     * @return the property value, or {@code null} when the property is absent or not a string
     */
    public static @Nullable String getString(Toml.Table table, String key) {
        Toml.KeyValue keyValue = find(table, key);
        if (keyValue != null && keyValue.getValue() instanceof Toml.Literal) {
            Object value = ((Toml.Literal) keyValue.getValue()).getValue();
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    /**
     * Replaces an existing string-valued property while preserving its source formatting.
     *
     * @param table the inline table to update
     * @param key the property key
     * @param value the replacement value
     * @return the updated table
     */
    public static Toml.Table withString(Toml.Table table, String key, String value) {
        return table.withValues(org.openrewrite.internal.ListUtils.map(table.getValues(), element -> {
            Toml.KeyValue keyValue = find(element, key);
            if (keyValue == null || !(keyValue.getValue() instanceof Toml.Literal)) {
                return element;
            }
            Toml.Literal literal = (Toml.Literal) keyValue.getValue();
            String source = literal.getSource();
            String quote = source.isEmpty() ? "\"" : source.substring(0, 1);
            return keyValue.withValue(literal.withSource(quote + value + quote).withValue(value));
        }));
    }

    /**
     * Replaces an existing string-valued property or appends a new property when absent.
     * Existing comma and whitespace padding is preserved.
     *
     * @param table the inline table to update
     * @param key the property key
     * @param value the replacement or new value
     * @return the updated table
     */
    public static Toml.Table withStringOrAdd(Toml.Table table, String key, String value) {
        if (has(table, key)) {
            return withString(table, key, value);
        }

        Toml.Identifier identifier = new Toml.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, key, key);
        Toml.Literal literal = new Toml.Literal(
                randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                TomlType.Primitive.String, "\"" + value + "\"", value);
        Toml.KeyValue keyValue = new Toml.KeyValue(
                randomId(), Space.EMPTY, Markers.EMPTY,
                new TomlRightPadded<>(identifier, Space.SINGLE_SPACE, Markers.EMPTY), literal);

        List<Toml> values = table.getValues();
        List<TomlRightPadded<Toml>> paddedValues = new ArrayList<>(table.getPadding().getValues());
        if (!paddedValues.isEmpty()) {
            int lastValue = paddedValues.size() - 1;
            paddedValues.set(lastValue, paddedValues.get(lastValue).withAfter(Space.EMPTY));
            table = table.getPadding().withValues(paddedValues);
        }
        keyValue = keyValue.withPrefix(Space.SINGLE_SPACE);
        table = table.withValues(org.openrewrite.internal.ListUtils.concat(values, keyValue));
        paddedValues = new ArrayList<>(table.getPadding().getValues());
        int lastValue = paddedValues.size() - 1;
        paddedValues.set(lastValue, paddedValues.get(lastValue).withAfter(Space.SINGLE_SPACE));
        return table.getPadding().withValues(paddedValues);
    }

    private static Toml.@Nullable KeyValue find(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            Toml.KeyValue keyValue = find(value, key);
            if (keyValue != null) {
                return keyValue;
            }
        }
        return null;
    }

    private static Toml.@Nullable KeyValue find(Toml value, String key) {
        if (value instanceof Toml.KeyValue) {
            Toml.KeyValue keyValue = (Toml.KeyValue) value;
            if (keyValue.getKey() instanceof Toml.Identifier &&
                key.equals(((Toml.Identifier) keyValue.getKey()).getName())) {
                return keyValue;
            }
        }
        return null;
    }
}
