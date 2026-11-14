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

/**
 * Converts between the text of a TOML basic string as written and the string it stands for.
 */
public final class TomlString {

    private TomlString() {
    }

    /**
     * The string that the text of a basic string, without its quotes, stands for.
     * A backslash ending a line of a multi-line basic string swallows that newline and the following whitespace.
     * An escape TOML does not define is kept as written.
     */
    public static String decodeBasic(String raw, boolean multiline) {
        if (raw.indexOf('\\') < 0) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                out.append(c);
                i++;
                continue;
            }
            char e = raw.charAt(i + 1);
            switch (e) {
                case 'b': out.append('\b'); i += 2; break;
                case 't': out.append('\t'); i += 2; break;
                case 'n': out.append('\n'); i += 2; break;
                case 'f': out.append('\f'); i += 2; break;
                case 'r': out.append('\r'); i += 2; break;
                case 'e': out.append('\u001b'); i += 2; break;
                case '"': out.append('"'); i += 2; break;
                case '\\': out.append('\\'); i += 2; break;
                case 'u':
                case 'U': {
                    int digits = e == 'u' ? 4 : 8;
                    if (i + 2 + digits <= raw.length() && isHex(raw, i + 2, i + 2 + digits)) {
                        try {
                            out.appendCodePoint(Integer.parseInt(raw.substring(i + 2, i + 2 + digits), 16));
                            i += 2 + digits;
                            break;
                        } catch (IllegalArgumentException ignored) {
                            // beyond U+10FFFF, so the text is kept as written
                        }
                    }
                    out.append(c);
                    i++;
                    break;
                }
                default:
                    if (multiline && (e == '\n' || e == '\r' || e == ' ' || e == '\t')) {
                        int j = i + 1;
                        while (j < raw.length() && (raw.charAt(j) == ' ' || raw.charAt(j) == '\t')) {
                            j++;
                        }
                        if (j < raw.length() && (raw.charAt(j) == '\n' || raw.charAt(j) == '\r')) {
                            while (j < raw.length() && Character.isWhitespace(raw.charAt(j))) {
                                j++;
                            }
                            i = j;
                            break;
                        }
                    }
                    out.append(c);
                    i++;
            }
        }
        return out.toString();
    }

    private static boolean isHex(String s, int from, int to) {
        for (int i = from; i < to; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * The text of a basic string, without its quotes, that stands for {@code value}.
     */
    public static String quoteBasic(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    if (c < 0x20 || c == 0x7F) {
                        out.append(String.format("\\u%04X", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }
}
