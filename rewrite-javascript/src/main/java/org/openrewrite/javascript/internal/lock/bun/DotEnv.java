/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of bun's src/env_loader.zig (bun 1.3.10): the .env files an install loads from the root over the process
 * environment, in its order, and their parser.
 */
public final class DotEnv {

    /**
     * {@code loadDefaultFiles} for {@code .production}, the suffix an install loads with: an earlier file's value
     * is kept over a later one's, and the process environment over all of them.
     */
    public static final List<String> FILES = Arrays.asList(".env.production.local", ".env.local", ".env.production",
        ".env");

    private static final String WHITESPACE = "\t\u000B\f  \n\r";

    private DotEnv() {
    }

    static Map<String, String> load(Map<String, String> process, Map<String, String> files) {
        Map<String, String> map = new LinkedHashMap<>(process);
        for (String file : FILES) {
            String contents = files.get(file);
            if (contents != null) {
                new Parser(contents).parse(map);
            }
        }
        return map;
    }

    /**
     * {@code Parser.parse} of one file: a key the map already holds keeps its value, a key the file sets twice takes
     * the later one, and the file's own values are expanded afterwards.
     */
    static void parse(String src, Map<String, String> map) {
        new Parser(src).parse(map);
    }

    private static final class Parser {
        private final String src;
        private int pos;
        private final StringBuilder valueBuffer = new StringBuilder();

        Parser(String src) {
            this.src = src;
        }

        void parse(Map<String, String> map) {
            int count = map.size();
            Set<String> before = new HashSet<>(map.keySet());
            while (pos < src.length()) {
                String key = parseKey(true);
                if (key == null) {
                    skipLine();
                    continue;
                }
                String value = parseValue();
                if (before.contains(key)) {
                    continue;
                }
                map.put(key, value);
            }
            int i = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (i++ < count) {
                    continue;
                }
                String expanded = expandValue(map, entry.getValue());
                if (expanded != null) {
                    entry.setValue(expanded);
                }
            }
        }

        private void skipLine() {
            int i = pos;
            while (i < src.length() && src.charAt(i) != '\n' && src.charAt(i) != '\r') {
                i++;
            }
            pos = i < src.length() ? i + 1 : src.length();
        }

        private void skipWhitespaces() {
            int i = pos;
            while (i < src.length() && WHITESPACE.indexOf(src.charAt(i)) >= 0) {
                i++;
            }
            pos = i;
        }

        private @Nullable String parseKey(boolean checkExport) {
            if (checkExport) {
                skipWhitespaces();
            }
            int start = pos;
            int end = start;
            while (end < src.length() && isKeyChar(src.charAt(end))) {
                end++;
            }
            if (end < src.length() && start < end) {
                pos = end;
                skipWhitespaces();
                if (pos < src.length()) {
                    if (checkExport && end < pos && "export".equals(src.substring(start, end))) {
                        String key = parseKey(false);
                        if (key != null) {
                            return key;
                        }
                    }
                    switch (src.charAt(pos)) {
                        case '=':
                            pos++;
                            return src.substring(start, end);
                        case ':':
                            int next = pos + 1;
                            if (next < src.length() && WHITESPACE.indexOf(src.charAt(next)) >= 0) {
                                pos += 2;
                                return src.substring(start, end);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            pos = start;
            return null;
        }

        private @Nullable String parseQuoted(char quote) {
            int start = pos;
            valueBuffer.setLength(0);
            for (int end = start + 1; end < src.length(); end++) {
                char c = src.charAt(end);
                if (c == '\\') {
                    end++;
                } else if (c == quote) {
                    end++;
                    pos = end;
                    skipWhitespaces();
                    String after = src.substring(end, pos);
                    if (pos >= src.length() || src.charAt(pos) == '#' ||
                        after.indexOf('\n') >= 0 || after.indexOf('\r') >= 0) {
                        int i = start;
                        while (i < end) {
                            char ch = src.charAt(i);
                            if (ch == '\\' && quote == '"') {
                                char escaped = src.charAt(i + 1);
                                if (escaped == 'n') {
                                    valueBuffer.append('\n');
                                } else if (escaped == 'r') {
                                    valueBuffer.append('\r');
                                } else {
                                    valueBuffer.append(ch).append(escaped);
                                }
                                i += 2;
                            } else if (ch == '\\') {
                                valueBuffer.append('\\');
                                i++;
                            } else if (ch == '\r') {
                                i++;
                                if (i >= end || src.charAt(i) != '\n') {
                                    valueBuffer.append('\n');
                                }
                            } else {
                                valueBuffer.append(ch);
                                i++;
                            }
                        }
                        return valueBuffer.toString();
                    }
                    pos = start;
                }
            }
            return null;
        }

        private String parseValue() {
            int start = pos;
            skipWhitespaces();
            int end = pos;
            if (end >= src.length()) {
                return "";
            }
            char c = src.charAt(end);
            if (c == '`' || c == '"' || c == '\'') {
                String value = parseQuoted(c);
                if (value != null) {
                    return value.substring(1, value.length() - 1);
                }
            }
            end = start;
            while (end < src.length() && src.charAt(end) != '#' && src.charAt(end) != '\r' && src.charAt(end) != '\n') {
                end++;
            }
            pos = end;
            return Strings.trim(src.substring(start, end), WHITESPACE);
        }

        private @Nullable String expandValue(Map<String, String> map, String value) {
            if (value.length() < 2) {
                return null;
            }
            valueBuffer.setLength(0);
            int pos = value.length() - 2;
            int last = value.length();
            while (true) {
                if (value.charAt(pos) == '$') {
                    if (pos > 0 && value.charAt(pos - 1) == '\\') {
                        valueBuffer.insert(0, value.substring(pos, last));
                        pos--;
                    } else {
                        int end = value.charAt(pos + 1) == '{' ? pos + 2 : pos + 1;
                        int keyStart = end;
                        while (end < value.length() && isExpansionKeyChar(value.charAt(end))) {
                            end++;
                        }
                        String lookup = map.get(value.substring(keyStart, end));
                        String defaultValue = "";
                        if (value.startsWith(":-", end)) {
                            end += 2;
                            int valueStart = end;
                            while (end < value.length() && value.charAt(end) != '}' && value.charAt(end) != '\\') {
                                end++;
                            }
                            defaultValue = value.substring(valueStart, end);
                        }
                        if (end < value.length() && value.charAt(end) == '}') {
                            end++;
                        }
                        valueBuffer.insert(0, value.substring(end, last));
                        valueBuffer.insert(0, lookup != null ? lookup : defaultValue);
                    }
                    last = pos;
                }
                if (pos == 0) {
                    if (last == value.length()) {
                        return null;
                    }
                    break;
                }
                pos--;
            }
            if (last > 0) {
                valueBuffer.insert(0, value.substring(0, last));
            }
            return valueBuffer.toString();
        }

        private static boolean isKeyChar(char c) {
            return isExpansionKeyChar(c) || c == '-' || c == '.';
        }

        private static boolean isExpansionKeyChar(char c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
        }
    }
}
