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

/**
 * Port of the parts of bun's src/string/immutable.zig ({@code strings}) and src/js_printer.zig
 * ({@code writePreQuotedString} for JSON) the lock code uses (bun 1.3.10).
 */
final class Strings {

    static final String WHITESPACE_CHARS = " \t\n\r\u000B\u000C";

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private Strings() {
    }

    /**
     * {@code strings.order}: byte order of the UTF-8 encodings, which is code point order.
     */
    static int order(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            int ca = a.codePointAt(i);
            int cb = b.codePointAt(j);
            if (ca != cb) {
                return ca < cb ? -1 : 1;
            }
            i += Character.charCount(ca);
            j += Character.charCount(cb);
        }
        int remainA = a.length() - i;
        int remainB = b.length() - j;
        return Integer.compare(remainA == 0 ? 0 : 1, remainB == 0 ? 0 : 1);
    }

    static String trim(String s, String chars) {
        return trimRight(trimLeft(s, chars), chars);
    }

    static String trimLeft(String s, String chars) {
        int i = 0;
        while (i < s.length() && chars.indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return s.substring(i);
    }

    static String trimRight(String s, String chars) {
        int i = s.length();
        while (i > 0 && chars.indexOf(s.charAt(i - 1)) >= 0) {
            i--;
        }
        return s.substring(0, i);
    }

    static String withoutTrailingSlash(String s) {
        int end = s.length();
        while (end > 1 && (s.charAt(end - 1) == '/' || s.charAt(end - 1) == '\\')) {
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * {@code bun.fmt.formatJSONStringUTF8}: {@code js_printer.writePreQuotedString} with a double quote, for JSON.
     */
    static void appendJsonString(StringBuilder out, String text, boolean quote) {
        if (quote) {
            out.append('"');
        }
        int i = 0;
        while (i < text.length()) {
            int c = text.codePointAt(i);
            int width = Character.charCount(c);
            if (canPrintWithoutEscape(c)) {
                out.appendCodePoint(c);
                i += width;
                continue;
            }
            i += width;
            switch (c) {
                case 0x07:
                    out.append("\\x07");
                    break;
                case 0x08:
                    out.append("\\b");
                    break;
                case 0x0C:
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case 0x0B:
                    out.append("\\v");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\'':
                    out.append('\'');
                    break;
                case '`':
                    out.append('`');
                    break;
                case '$':
                    out.append('$');
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c <= 0xFFFF) {
                        out.append("\\u").append(HEX_CHARS[(c >> 12) & 0xF]).append(HEX_CHARS[(c >> 8) & 0xF])
                                .append(HEX_CHARS[(c >> 4) & 0xF]).append(HEX_CHARS[c & 0xF]);
                    } else {
                        int k = c - 0x10000;
                        int lo = 0xD800 + ((k >> 10) & 0x3FF);
                        int hi = 0xDC00 + (k & 0x3FF);
                        out.append("\\u").append(HEX_CHARS[lo >> 12]).append(HEX_CHARS[(lo >> 8) & 15])
                                .append(HEX_CHARS[(lo >> 4) & 15]).append(HEX_CHARS[lo & 15])
                                .append("\\u").append(HEX_CHARS[hi >> 12]).append(HEX_CHARS[(hi >> 8) & 15])
                                .append(HEX_CHARS[(hi >> 4) & 15]).append(HEX_CHARS[hi & 15]);
                    }
                    break;
            }
        }
        if (quote) {
            out.append('"');
        }
    }

    private static boolean canPrintWithoutEscape(int c) {
        if (c <= 0x7E) {
            return c >= 0x20 && c != '\\' && c != '"' && c != '\'' && c != '`' && c != '$';
        }
        return c != 0xFEFF && c != 0x2028 && c != 0x2029 && (c < 0xD800 || c > 0xDFFF);
    }
}
