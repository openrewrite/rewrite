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
import lombok.RequiredArgsConstructor;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

/**
 * Utilities for updating string-valued properties in TOML tables.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TomlTableValue {

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
     * Replaces an existing string-valued property while preserving its source formatting.
     *
     * @param table the inline table to update
     * @param key   the property key
     * @param value the replacement value
     * @return the updated table
     */
    public static Toml.Table withString(Toml.Table table, String key, String value) {
        Toml.KeyValue matchingKeyValue = table.find(key);
        if (matchingKeyValue == null || !(matchingKeyValue.getValue() instanceof Toml.Literal)) {
            return table;
        }
        Toml.Literal literal = (Toml.Literal) matchingKeyValue.getValue();
        if (!(literal.getValue() instanceof String)) {
            return table;
        }
        return table.withValues(org.openrewrite.internal.ListUtils.map(table.getValues(), element -> {
            if (element != matchingKeyValue) {
                return element;
            }
            return matchingKeyValue.withValue(literal.withSource(quoted(literal, value)).withValue(value));
        }));
    }

    /**
     * Replaces an existing string-valued property or appends a new property when absent.
     * Existing comma and whitespace padding is preserved.
     *
     * @param table the inline table to update
     * @param key   the property key
     * @param value the replacement or new value
     * @return the updated table
     */
    public static Toml.Table withStringOrAdd(Toml.Table table, String key, String value) {
        if (table.find(key) != null) {
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

}
