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

import org.jspecify.annotations.Nullable;
import org.openrewrite.toml.internal.TomlKeyParts;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.tree.Comment;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;

/**
 * Utility methods for matching table rows and extracting values from TOML tables.
 */
public class TableRowMatcher {

    private TableRowMatcher() {
    }

    /**
     * Extracts the string value of a specific key from a TOML table.
     *
     * @param table The TOML table to search
     * @param key   The key name to find
     * @return The string value of the key, or null if not found or not a literal
     */
    public static @Nullable String getKeyValue(Toml.Table table, String key) {
        return getKeyValue(table.getValues(), key);
    }

    /**
     * Extracts the string value of a specific key from a list of TOML elements.
     *
     * @param keyValues The list of key-value pairs to search
     * @param key       The key name to find
     * @return The string value of the key, or null if not found or not a literal
     */
    public static @Nullable String getKeyValue(List<? extends Toml> keyValues, String key) {
        Toml.KeyValue keyValue = TomlTableValue.find(keyValues, key);
        if (keyValue == null || !(keyValue.getValue() instanceof Toml.Literal)) {
            return null;
        }
        return ((Toml.Literal) keyValue.getValue()).getValue().toString();
    }

    /**
     * Checks if a TOML table contains a key-value pair that matches the specified criteria.
     *
     * @param table    The TOML table to search
     * @param key      The key name to match
     * @param value    The value to match (exact match or regex pattern)
     * @param useRegex Whether to interpret the value as a regular expression
     * @return true if a matching key-value pair is found, false otherwise
     */
    public static boolean hasMatchingKeyValue(Toml.Table table, String key, String value, @Nullable Boolean useRegex) {
        String keyValue = getKeyValue(table.getValues(), key);
        return keyValue != null && (Boolean.TRUE.equals(useRegex) ? keyValue.matches(value) : keyValue.equals(value));
    }

    /**
     * Puts a key-value appended to a table row on a line of its own, indented as the row's last key-value is. A
     * key-value parsed from the start of the row's text has no newline before it, so it would print on the line of
     * the key-value it follows, and one parsed from a later line is not indented as the row is.
     */
    static Toml.KeyValue onItsOwnLine(Toml.KeyValue appended, List<? extends Toml> existing) {
        if (!appended.getPrefix().getComments().isEmpty()) {
            return appended; // keep the comment the row puts before it
        }
        for (int i = existing.size() - 1; i >= 0; i--) {
            Toml e = existing.get(i);
            String ws = e.getPrefix().getLastWhitespace();
            if (e instanceof Toml.KeyValue && ws.contains("\n")) {
                return appended.withPrefix(Space.format(ws.substring(ws.lastIndexOf('\n'))));
            }
        }
        return appended.withPrefix(Space.format("\n"));
    }

    /**
     * A comment at the end of a table's last line is held by the prefix of what follows the table, or by the end of
     * the document. Once key-values are appended to the table, that comment would follow the last of them, so this
     * moves it after {@code lastBeforeAppend}, the key-value whose line it is on.
     *
     * @param doc              the document holding the table
     * @param table            the table key-values were appended to
     * @param lastBeforeAppend the id of the table's last value before the append
     * @return the document with the comment moved, or unchanged if the table's last line has no comment
     */
    static Toml.Document keepCommentOnLastLine(Toml.Document doc, Toml.Table table, UUID lastBeforeAppend) {
        List<TomlValue> values = doc.getValues();
        int index = values.indexOf(table);
        if (index < 0) {
            return doc;
        }
        Space next = index + 1 < values.size() ? values.get(index + 1).getPrefix() : doc.getEof();
        if (next.getComments().isEmpty() || next.getWhitespace().contains("\n")) {
            return doc;
        }
        Comment comment = next.getComments().get(0);
        Space after = Space.build(next.getWhitespace(), singletonList(comment.withSuffix("")));
        Space rest = Space.build(comment.getSuffix(), next.getComments().subList(1, next.getComments().size()));
        Toml.Table moved = table.getPadding().withValues(ListUtils.map(table.getPadding().getValues(),
                v -> v.getElement().getId().equals(lastBeforeAppend) ? v.withAfter(after) : v));
        if (moved == table) {
            return doc;
        }
        doc = doc.withValues(ListUtils.map(values, (i, v) -> i == index ? moved :
                i == index + 1 ? v.withPrefix(rest) : v));
        return index + 1 < values.size() ? doc : doc.withEof(rest);
    }

    /**
     * Whether {@code table} is the table {@code tableName} names, part by part, so {@code tool.my-pkg} names
     * {@code [tool."my-pkg"]} and {@code [ tool . my-pkg ]} too.
     */
    static boolean isNamed(Toml.Table table, String tableName) {
        Toml.Identifier name = table.getName();
        return name != null && TomlKeyParts.of(name).equals(TomlKeyParts.split(tableName));
    }
}
