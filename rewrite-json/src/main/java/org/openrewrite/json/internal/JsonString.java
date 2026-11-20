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
package org.openrewrite.json.internal;

/**
 * Converts between a JSON5 string literal as written and the string it stands for.
 */
public final class JsonString {

    private JsonString() {
    }

    /**
     * The string that a single- or double-quoted JSON5 literal stands for.
     * An {@code x} or {@code u} escape without its hex digits is kept as written, since recipe options reach here
     * without passing the grammar. A surrogate with no partner stays a lower-case {@code u} escape, because the LST
     * writer can't encode it.
     */
    public static String decode(String literal) {
        String body = literal.substring(1, literal.length() - 1);
        if (body.indexOf('\\') < 0) {
            return body;
        }
        StringBuilder out = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c != '\\' || i + 1 == body.length()) {
                out.append(c);
                continue;
            }
            char escaped = body.charAt(++i);
            switch (escaped) {
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'v':
                    out.append('\u000B');
                    break;
                case '0':
                    out.append('\0');
                    break;
                case 'x':
                case 'u': {
                    int digits = escaped == 'x' ? 2 : 4;
                    int code = hex(body, i + 1, digits);
                    if (code < 0) {
                        out.append('\\').append(escaped);
                    } else {
                        out.append((char) code);
                        i += digits;
                    }
                    break;
                }
                case '\r':
                    if (i + 1 < body.length() && body.charAt(i + 1) == '\n') {
                        i++;
                    }
                    break;
                case '\n':
                case '\u2028':
                case '\u2029':
                    break;
                default:
                    out.append(escaped);
            }
        }
        return escapeUnpairedSurrogates(out);
    }

    private static String escapeUnpairedSurrogates(CharSequence decoded) {
        StringBuilder out = new StringBuilder(decoded.length());
        decoded.codePoints().forEach(codePoint -> {
            if (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) {
                out.append(String.format("\\u%04x", codePoint));
            } else {
                out.appendCodePoint(codePoint);
            }
        });
        return out.toString();
    }

    private static int hex(String s, int from, int digits) {
        if (from + digits > s.length()) {
            return -1;
        }
        int code = 0;
        for (int i = from; i < from + digits; i++) {
            int digit = Character.digit(s.charAt(i), 16);
            if (digit < 0) {
                return -1;
            }
            code = code * 16 + digit;
        }
        return code;
    }

    /**
     * A double-quoted literal that standard JSON reads as {@code value}.
     */
    public static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
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
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }
}
