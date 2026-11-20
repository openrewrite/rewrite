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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.toml.internal.TomlKeyParts;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Marker;
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
     * Appends key-values to a table row, each on a line of its own, and marks the row so that
     * {@link #keepCommentsOnLastLines(Toml.Document)} keeps a comment on the row's last line there.
     *
     * @param table    the table as it was before the change
     * @param kept     the table's values after the change, but before the append
     * @param appended the key-values to append
     */
    static Toml.Table append(Toml.Table table, List<Toml> kept, List<Toml.KeyValue> appended) {
        List<Toml> values = ListUtils.concatAll(kept,
                ListUtils.map(appended, kv -> onItsOwnLine(kv, table.getValues())));
        Toml.Table t = table.withValues(values);
        if (appended.isEmpty() || kept.isEmpty()) {
            return t;
        }
        return t.withMarkers(t.getMarkers().add(new AppendedAfter(Tree.randomId(), kept.get(kept.size() - 1).getId())));
    }

    /**
     * A comment at the end of a table's last line is held by the prefix of what follows the table, or by the end of
     * the document. Once {@link #append(Toml.Table, List, List)} adds key-values to the table, that comment would
     * follow the last of them, so this moves it back after the key-value whose line it is on.
     *
     * @return the document with each such comment moved, and the marks {@code append} left removed
     */
    static Toml.Document keepCommentsOnLastLines(Toml.Document doc) {
        List<TomlValue> values = doc.getValues();
        for (int index = 0; index < values.size(); index++) {
            if (!(values.get(index) instanceof Toml.Table)) {
                continue;
            }
            Toml.Table table = (Toml.Table) values.get(index);
            AppendedAfter appendedAfter = table.getMarkers().findFirst(AppendedAfter.class).orElse(null);
            if (appendedAfter == null) {
                continue;
            }
            Toml.Table unmarked = table.withMarkers(table.getMarkers().removeByType(AppendedAfter.class));
            Space next = index + 1 < values.size() ? values.get(index + 1).getPrefix() : doc.getEof();
            if (!next.getComments().isEmpty() && !next.getWhitespace().contains("\n")) {
                Comment comment = next.getComments().get(0);
                Space after = Space.build(next.getWhitespace(), singletonList(comment.withSuffix("")));
                Space rest = Space.build(comment.getSuffix(), next.getComments().subList(1, next.getComments().size()));
                unmarked = unmarked.getPadding().withValues(ListUtils.map(unmarked.getPadding().getValues(),
                        v -> v.getElement().getId().equals(appendedAfter.getLastBeforeAppend()) ? v.withAfter(after) : v));
                if (index + 1 < values.size()) {
                    int nextIndex = index + 1;
                    values = ListUtils.map(values, (i, v) -> i == nextIndex ? v.withPrefix(rest) : v);
                } else {
                    doc = doc.withEof(rest);
                }
            }
            Toml.Table moved = unmarked;
            int tableIndex = index;
            values = ListUtils.map(values, (i, v) -> i == tableIndex ? moved : v);
        }
        return doc.withValues(values);
    }

    /**
     * Marks a table row {@link #append(Toml.Table, List, List)} added key-values to, with its last value before them.
     */
    @Value
    @With
    private static class AppendedAfter implements Marker {
        UUID id;
        UUID lastBeforeAppend;
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
