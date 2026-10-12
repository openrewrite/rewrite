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

import org.openrewrite.javascript.internal.lock.CannotKnow;

/**
 * Port of the parts of bun's src/string/immutable.zig ({@code strings}) and src/js_printer.zig
 * ({@code writePreQuotedString} for JSON) the lock code uses (bun 1.3.10), with 1.2.11's escape search
 * (src/bun.js/bindings/highway_strings.cpp) beside it.
 */
final class Strings {

    static final String WHITESPACE_CHARS = " \t\n\r\u000B\u000C";

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    private static final char[] LOWER_HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * The lane counts of a byte vector on the CPUs bun runs on, of which the escape search uses the widest the CPU
     * has: 16 (NEON, SSE), 32 (AVX2), 64 (AVX-512).
     */
    private static final int[] ESCAPE_SEARCH_LANES = {16, 32, 64};

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
        appendJsonString(out, text, quote, BunRelease.PORTED);
    }

    /**
     * {@code formatJSONStringUTF8} as {@code release} writes it: quoted, 1.4.0's {@code encode_json_string_chars}
     * ({@link BunRelease#escapesQuotedJsonStringsMinimally}); else {@code writePreQuotedString}.
     *
     * @throws CannotKnow from 1.2.11 until 1.3.0 for a tab that the CPU's vector width decides whether the escape
     *                    search flags ({@link BunRelease#leavesATabInTheEscapeSearchsTail()})
     */
    static void appendJsonString(StringBuilder out, String text, boolean quote, BunRelease release) {
        if (quote && release.escapesQuotedJsonStringsMinimally()) {
            out.append('"');
            encodeJsonStringChars(out, text);
            out.append('"');
            return;
        }
        if (quote) {
            out.append('"');
        }
        if (release.leavesATabInTheEscapeSearchsTail() && text.indexOf('\t') >= 0) {
            String written = writePreQuotedString(text, release, ESCAPE_SEARCH_LANES[0]);
            for (int k = 1; k < ESCAPE_SEARCH_LANES.length; k++) {
                if (!written.equals(writePreQuotedString(text, release, ESCAPE_SEARCH_LANES[k]))) {
                    throw new CannotKnow("what bun " + release + " writes for the string \"" + text + "\": whether " +
                                         "its escape search flags a tab depends on the CPU's vector width " +
                                         "(highway_strings.cpp)");
                }
            }
            out.append(written);
        } else {
            appendEscaped(out, text, release);
        }
        if (quote) {
            out.append('"');
        }
    }

    /**
     * {@code writePreQuotedString}'s loop over the UTF-8 text with 1.2.11's {@code IndexOfNeedsEscapeForJavaScriptString}
     * of {@code lanes} lanes: after a character it can print, the text up to the next one the search flags is written
     * as it is; the search's vectors flag a tab, its scalar loop over the last {@code len % lanes} bytes does not.
     */
    private static String writePreQuotedString(String text, BunRelease release, int lanes) {
        int[] cps = text.codePoints().toArray();
        StringBuilder out = new StringBuilder();
        int k = 0;
        while (k < cps.length) {
            int c = cps[k];
            if (!canPrintWithoutEscape(c)) {
                appendEscaped(out, new String(Character.toChars(c)), release);
                k++;
                continue;
            }
            int j = indexOfNeedsEscape(cps, k + 1, lanes);
            int end = j < 0 ? cps.length : k + 1 + j;
            for (; k < end; k++) {
                out.appendCodePoint(cps[k]);
            }
        }
        return out.toString();
    }

    // the code points after `from` up to the first byte the search flags, as a count of code points; -1 for none
    private static int indexOfNeedsEscape(int[] cps, int from, int lanes) {
        int len = 0;
        for (int k = from; k < cps.length; k++) {
            len += utf8Length(cps[k]);
        }
        int vectorLen = len - len % lanes;
        int offset = 0;
        for (int k = from; k < cps.length; k++) {
            int c = cps[k];
            boolean flagged = offset < vectorLen ?
                    c > 127 || c < 0x20 || c == '\\' || c == '"' :
                    c >= 127 || c < 0x20 && c != '\t' || c == '\\' || c == '"';
            if (flagged) {
                return k - from;
            }
            offset += utf8Length(c);
        }
        return -1;
    }

    private static int utf8Length(int c) {
        return c < 0x80 ? 1 : c < 0x800 ? 2 : c < 0x10000 ? 3 : 4;
    }

    private static void appendEscaped(StringBuilder out, String text, BunRelease release) {
        boolean json = release.escapesBelAndVtAsUnicodeInJson();
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
                    out.append(json ? "\\u0007" : "\\x07");
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
                    out.append(json ? "\\u000B" : "\\v");
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
    }

    /**
     * src/bun_core/fmt.rs {@code encode_json_string_chars} (1.4.0): only {@code "}, {@code \}, {@code \b \f \n \r \t}
     * and the other control characters, as <code>&#92;u00xx</code> in lowercase hex, are escaped; everything else, U+2028,
     * U+2029, U+FEFF and a lone surrogate too, is written as it is.
     */
    private static void encodeJsonStringChars(StringBuilder out, String text) {
        for (int i = 0; i < text.length(); i++) {
            char b = text.charAt(i);
            switch (b) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
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
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (b < 0x20) {
                        out.append("\\u00").append(LOWER_HEX_CHARS[b >> 4]).append(LOWER_HEX_CHARS[b & 0xF]);
                    } else {
                        out.append(b);
                    }
                    break;
            }
        }
    }

    private static boolean canPrintWithoutEscape(int c) {
        if (c <= 0x7E) {
            return c >= 0x20 && c != '\\' && c != '"' && c != '\'' && c != '`' && c != '$';
        }
        return c != 0xFEFF && c != 0x2028 && c != 0x2029 && (c < 0xD800 || c > 0xDFFF);
    }
}
