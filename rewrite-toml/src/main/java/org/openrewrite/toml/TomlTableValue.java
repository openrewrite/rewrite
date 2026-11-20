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
import java.util.function.UnaryOperator;

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

    /**
     * @return the string at {@code nestedKey} of the table {@code key} holds, or {@code null} if
     * {@code key} holds something other than a table.
     */
    public static @Nullable String getString(Toml.Table table, String key, String nestedKey) {
        Toml.Table nested = getTable(table, key);
        return nested == null ? null : getString(nested, nestedKey);
    }

    public static Toml.@Nullable Table getTable(Toml.Table table, String key) {
        Toml.KeyValue keyValue = find(table, key);
        return keyValue == null || !(keyValue.getValue() instanceof Toml.Table) ? null : (Toml.Table) keyValue.getValue();
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
     * Replaces a string-valued property of the table {@code key} holds, leaving that table's other
     * properties alone.
     *
     * @return the table unchanged if {@code key} holds something other than a table, or that table
     * has no string at {@code nestedKey}
     */
    public static Toml.Table withString(Toml.Table table, String key, String nestedKey, String value) {
        return withNested(table, key, nested -> withString(nested, nestedKey, value));
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
     * Renames a string-valued property of the table {@code key} holds.
     *
     * @return the table unchanged if {@code key} holds something other than a table, or that table
     * has no string at {@code nestedKey}
     */
    public static Toml.Table withKey(Toml.Table table, String key, String nestedKey, String newNestedKey) {
        return withNested(table, key, nested -> withKey(nested, nestedKey, newNestedKey));
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
        List<TomlRightPadded<Toml>> padded = table.getPadding().getValues();
        // The space before `}`, which can hold a comment
        Space beforeClose = padded.isEmpty() ? Space.EMPTY : padded.get(padded.size() - 1).getAfter();
        // Drop the placeholder an empty `{ }` or a trailing comma leaves, so the new property follows a real one
        List<TomlRightPadded<Toml>> values = ListUtils.filter(padded, p -> !(p.getElement() instanceof Toml.Empty));

        Space prefix = Space.SINGLE_SPACE;
        Space after = Space.SINGLE_SPACE;
        String lastWhitespace = beforeClose.getLastWhitespace();
        int newline = lastWhitespace.lastIndexOf('\n');
        if (newline >= 0) {
            // A table spanning lines: the new property gets a line of its own, after any comment, and `}` keeps its line
            String closeIndent = lastWhitespace.substring(newline + 1);
            String indent;
            Space lastPrefix = values.isEmpty() ? Space.EMPTY : values.get(values.size() - 1).getElement().getPrefix();
            if (lastPrefix.getLastWhitespace().contains("\n")) {
                indent = lastPrefix.getIndent();
            } else if (values.isEmpty() && !beforeClose.getComments().isEmpty() &&
                       beforeClose.getWhitespace().contains("\n")) {
                // Indented like the comment that opens an otherwise empty table
                indent = Space.format(beforeClose.getWhitespace()).getIndent();
            } else {
                indent = closeIndent + "    ";
            }
            prefix = withLastWhitespace(beforeClose, lastWhitespace.substring(0, newline) + "\n" + indent);
            after = Space.format("\n" + closeIndent);
        }
        Toml.KeyValue keyValue = new Toml.KeyValue(randomId(), prefix, Markers.EMPTY,
                new TomlRightPadded<>(identifier, Space.SINGLE_SPACE, Markers.EMPTY), literal);
        return table.getPadding().withValues(ListUtils.concat(
                ListUtils.mapLast(values, p -> p.withAfter(Space.EMPTY)),
                new TomlRightPadded<>(keyValue, after, Markers.EMPTY)));
    }

    private static Space withLastWhitespace(Space space, String whitespace) {
        return space.getComments().isEmpty() ? space.withWhitespace(whitespace) :
                space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix(whitespace)));
    }

    private static Toml.Table withStringProperty(Toml.Table table, String key,
                                                 BiFunction<Toml.KeyValue, Toml.Literal, Toml.KeyValue> edit) {
        Toml.KeyValue keyValue = find(table, key);
        if (keyValue == null || !(keyValue.getValue() instanceof Toml.Literal) ||
            !(((Toml.Literal) keyValue.getValue()).getValue() instanceof String)) {
            return table;
        }
        return withProperty(table, keyValue, edit.apply(keyValue, (Toml.Literal) keyValue.getValue()));
    }

    private static Toml.Table withNested(Toml.Table table, String key, UnaryOperator<Toml.Table> edit) {
        Toml.KeyValue keyValue = find(table, key);
        if (keyValue == null || !(keyValue.getValue() instanceof Toml.Table)) {
            return table;
        }
        Toml.Table nested = (Toml.Table) keyValue.getValue();
        Toml.Table edited = edit.apply(nested);
        return edited == nested ? table : withProperty(table, keyValue, keyValue.withValue(edited));
    }

    private static Toml.Table withProperty(Toml.Table table, Toml.KeyValue keyValue, Toml.KeyValue edited) {
        return edited == keyValue ? table :
                table.withValues(ListUtils.map(table.getValues(), value -> value == keyValue ? edited : value));
    }
}
