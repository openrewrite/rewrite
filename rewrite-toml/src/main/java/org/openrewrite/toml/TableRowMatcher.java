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
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlKey;

import java.util.List;
import java.util.regex.Pattern;

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
        for (Toml value : keyValues) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            TomlKey kvKey = kv.getKey();
            if (!(kvKey instanceof Toml.Identifier)) {
                continue;
            }
            if (key.equals(((Toml.Identifier) kvKey).getName())) {
                if (kv.getValue() instanceof Toml.Literal) {
                    Object val = ((Toml.Literal) kv.getValue()).getValue();
                    return val != null ? val.toString() : null;
                }
            }
        }
        return null;
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
        Pattern pattern = Boolean.TRUE.equals(useRegex) ? Pattern.compile(value) : null;
        for (Toml tableValue : table.getValues()) {
            if (!(tableValue instanceof Toml.KeyValue)) {
                continue;
            }

            Toml.KeyValue kv = (Toml.KeyValue) tableValue;
            TomlKey kvKey = kv.getKey();

            if (!(kvKey instanceof Toml.Identifier)) {
                continue;
            }

            Toml.Identifier identifier = (Toml.Identifier) kvKey;
            if (!key.equals(identifier.getName())) {
                continue;
            }

            // Found the matching key, now check the value
            if (matchesValue(kv.getValue(), value, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a TOML value matches the specified string value or regex pattern.
     *
     * @param valueNode The TOML value node to check
     * @param value     The value to match (exact match or regex pattern)
     * @param pattern   Optional compiled regex pattern for value matching
     * @return true if the value matches, false otherwise
     */
    private static boolean matchesValue(Toml valueNode, String value, @Nullable Pattern pattern) {
        if (!(valueNode instanceof Toml.Literal)) {
            return false;
        }

        Toml.Literal literal = (Toml.Literal) valueNode;
        String literalValue = literal.getValue() != null ? literal.getValue().toString() : "";

        if (pattern != null) {
            return pattern.matcher(literalValue).matches();
        } else {
            return value.equals(literalValue);
        }
    }
}