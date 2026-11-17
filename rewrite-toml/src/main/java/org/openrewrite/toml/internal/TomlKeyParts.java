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
package org.openrewrite.toml.internal;

import org.openrewrite.toml.tree.Toml;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of a key, such as the three of {@code tool."my.pkg".version}: split on the dots outside quotes,
 * each part unquoted, and a basic-string part's escapes decoded.
 */
public final class TomlKeyParts {
    private TomlKeyParts() {
    }

    public static List<String> of(Toml.Identifier key) {
        return split(key.getSource());
    }

    public static List<String> split(String key) {
        List<String> parts = new ArrayList<>();
        for (int[] range : ranges(key)) {
            parts.add(unquote(key.substring(range[0], range[1])));
        }
        return parts;
    }

    /**
     * Where each part is in {@code key}, from its first character to after its last, without the
     * whitespace around it.
     */
    public static List<int[]> ranges(String key) {
        List<int[]> ranges = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= key.length(); i++) {
            char c = i < key.length() ? key.charAt(i) : '.';
            if (c == '"' || c == '\'') {
                i = closingQuote(key, i);
            } else if (c == '.') {
                int from = start, to = i;
                while (from < to && Character.isWhitespace(key.charAt(from))) {
                    from++;
                }
                while (to > from && Character.isWhitespace(key.charAt(to - 1))) {
                    to--;
                }
                ranges.add(new int[]{from, to});
                start = i + 1;
            }
        }
        return ranges;
    }

    private static int closingQuote(String key, int open) {
        char quote = key.charAt(open);
        for (int i = open + 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '\\' && quote == '"') {
                i++;
            } else if (c == quote) {
                return i;
            }
        }
        return key.length() - 1;
    }

    private static String unquote(String part) {
        if (part.length() >= 2 && part.charAt(0) == '"' && part.endsWith("\"")) {
            return TomlString.decodeBasic(part.substring(1, part.length() - 1), false);
        } else if (part.length() >= 2 && part.charAt(0) == '\'' && part.endsWith("'")) {
            return part.substring(1, part.length() - 1);
        }
        return part;
    }
}
