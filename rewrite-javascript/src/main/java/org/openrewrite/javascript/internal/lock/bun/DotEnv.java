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
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Port of bun's src/env_loader.zig (bun 1.3.10): the .env files an install loads from the root over the process
 * environment, in its order, and their parser, with 1.4.0's src/dotenv/env_loader.rs beside it (the first closing
 * quote, ASCII whitespace, the byte order mark and left-to-right expansion).
 */
public final class DotEnv {

    /**
     * {@code loadDefaultFiles} for {@code .production}, the suffix an install loads with: an earlier file's value
     * is kept over a later one's, and the process environment over all of them.
     */
    public static final List<String> FILES = Collections.unmodifiableList(Arrays.asList(".env.production.local",
            ".env.local", ".env.production", ".env"));

    private static final String WHITESPACE = "\t\u000B\f \u00A0\n\r";

    // src/dotenv/env_loader.rs WHITESPACE_CHARS: ASCII only, as the input is UTF-8
    private static final String ASCII_WHITESPACE = "\t\u000B\f \n\r";

    // the UTF-8 byte order mark, as the loader's bytes
    private static final String BOM = "\u00EF\u00BB\u00BF";

    private DotEnv() {
    }

    static HostEnvironment load(HostEnvironment process, Map<String, String> files) {
        return load(BunRelease.PORTED, process, files);
    }

    /**
     * The process environment with the files' variables as {@code release} loads them; one whose value expands a
     * variable the host does not know is unknown.
     */
    static HostEnvironment load(BunRelease release, HostEnvironment process, Map<String, String> files) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> variable : process.known().entrySet()) {
            map.put(variable.getKey(), bytes(variable.getValue()));
        }
        Set<String> unknown = new LinkedHashSet<>();
        for (String file : FILES) {
            String contents = files.get(file);
            if (contents != null) {
                new Parser(bytes(contents), process, unknown, release).parse(map);
            }
        }
        Map<String, String> decoded = new LinkedHashMap<>();
        for (Map.Entry<String, String> variable : map.entrySet()) {
            decoded.put(variable.getKey(), new String(variable.getValue().getBytes(ISO_8859_1), UTF_8));
        }
        return process.with(decoded, unknown);
    }

    // the loader reads UTF-8 bytes, so its white space 0xa0 is a byte, never the c2 before it in U+00A0
    private static String bytes(String text) {
        return new String(text.getBytes(UTF_8), ISO_8859_1);
    }

    private static final class Parser {
        private final String src;
        private int pos;
        private final StringBuilder valueBuffer = new StringBuilder();
        private final HostEnvironment process;
        private final Set<String> unknown;
        private final BunRelease release;
        private final String whitespace;
        private boolean expandsUnknown;

        Parser(String src, HostEnvironment process, Set<String> unknown, BunRelease release) {
            this.src = src;
            this.process = process;
            this.unknown = unknown;
            this.release = release;
            this.whitespace = release.keepsNoBreakSpacesInEnvValues() ? ASCII_WHITESPACE : WHITESPACE;
            if (release.stripsTheByteOrderMarkOfEnvFiles() && src.startsWith(BOM)) {
                pos = BOM.length();
            }
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
                expandsUnknown = false;
                String expanded = release.expandsEnvValuesLeftToRight() ?
                        expandLeftToRight(map, entry.getValue()) : expandValue(map, entry.getValue());
                if (expanded != null) {
                    entry.setValue(expanded);
                }
                if (expandsUnknown) {
                    unknown.add(entry.getKey());
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
            while (i < src.length() && whitespace.indexOf(src.charAt(i)) >= 0) {
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
                            if (next < src.length() && whitespace.indexOf(src.charAt(next)) >= 0) {
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
                    boolean ends;
                    if (release.endsAQuotedEnvValueAtItsFirstClosingQuote()) {
                        // the rest of the line is dropped
                        skipLine();
                        ends = true;
                    } else {
                        skipWhitespaces();
                        String after = src.substring(end, pos);
                        ends = pos >= src.length() || src.charAt(pos) == '#' ||
                               after.indexOf('\n') >= 0 || after.indexOf('\r') >= 0;
                    }
                    if (ends) {
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
            return Strings.trim(src.substring(start, end), whitespace);
        }

        // env_loader.rs expand_value over expand_into (1.4.0): $NAME, ${NAME} and ${NAME:-default} left to right, the
        // default expanded in turn; \$ is $, and a $ starting neither is itself
        private @Nullable String expandLeftToRight(Map<String, String> map, String value) {
            if (value.length() < 2) {
                return null;
            }
            valueBuffer.setLength(0);
            return expandInto(map, value, valueBuffer, 0) ? valueBuffer.toString() : null;
        }

        private boolean expandInto(Map<String, String> map, String value, StringBuilder out, int depth) {
            int pos = 0;
            boolean changed = false;
            while (pos < value.length()) {
                char b = value.charAt(pos);
                if (b == '\\' && pos + 1 < value.length() && value.charAt(pos + 1) == '$') {
                    out.append('$');
                    pos += 2;
                    changed = true;
                    continue;
                }
                if (b != '$' || pos + 1 >= value.length()) {
                    out.append(b);
                    pos++;
                    continue;
                }
                char next = value.charAt(pos + 1);
                if (next == '{') {
                    int innerStart = pos + 2;
                    int close = -1;
                    int nest = 1;
                    int i = innerStart;
                    while (i < value.length()) {
                        char c = value.charAt(i);
                        if (c == '\\' && i + 1 < value.length()) {
                            i += 2;
                        } else if (c == '$' && i + 1 < value.length() && value.charAt(i + 1) == '{') {
                            nest++;
                            i += 2;
                        } else if (c == '}') {
                            nest--;
                            if (nest == 0) {
                                close = i;
                                break;
                            }
                            i++;
                        } else {
                            i++;
                        }
                    }
                    if (close < 0) {
                        out.append(value, pos, value.length());
                        pos = value.length();
                        continue;
                    }
                    changed = true;
                    String inner = value.substring(innerStart, close);
                    int keyEnd = 0;
                    while (keyEnd < inner.length() && isExpansionKeyChar(inner.charAt(keyEnd))) {
                        keyEnd++;
                    }
                    String key = inner.substring(0, keyEnd);
                    String rest = inner.substring(keyEnd);
                    if (rest.isEmpty()) {
                        String lookup = lookup(map, key);
                        if (lookup != null) {
                            out.append(lookup);
                        }
                    } else if (rest.startsWith(":-")) {
                        String lookup = lookup(map, key);
                        String defaultValue = rest.substring(2);
                        if (lookup != null) {
                            out.append(lookup);
                        } else if (depth < 200) {
                            expandInto(map, defaultValue, out, depth + 1);
                        } else {
                            out.append(defaultValue);
                        }
                    } else {
                        out.append(value, pos, close + 1);
                    }
                    pos = close + 1;
                    continue;
                }
                if (isExpansionKeyChar(next)) {
                    changed = true;
                    int k = pos + 1;
                    while (k < value.length() && isExpansionKeyChar(value.charAt(k))) {
                        k++;
                    }
                    String lookup = lookup(map, value.substring(pos + 1, k));
                    if (lookup != null) {
                        out.append(lookup);
                    }
                    pos = k;
                    continue;
                }
                out.append('$');
                pos++;
            }
            return changed;
        }

        // map.get, noting a variable whose value the host does not know
        private @Nullable String lookup(Map<String, String> map, String name) {
            String lookup = map.get(name);
            if (lookup == null ? !process.settles(name) : unknown.contains(name)) {
                expandsUnknown = true;
            }
            return lookup;
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
                        String name = value.substring(keyStart, end);
                        String lookup = map.get(name);
                        if (lookup == null ? !process.settles(name) : unknown.contains(name)) {
                            expandsUnknown = true;
                        }
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
