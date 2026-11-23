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
package org.openrewrite.hcl.internal;

/**
 * Converts between the text of an HCL template as written and the string it stands for.
 */
public final class HclString {

    private HclString() {
    }

    /**
     * The string that the text of a quoted template or string literal, without its quotes, stands for.
     * An escape HCL does not define is kept as written.
     */
    public static String decodeQuoted(String text) {
        return decode(text, true);
    }

    /**
     * The string that the text of a heredoc template stands for. A heredoc reads backslashes literally.
     */
    public static String decodeHeredoc(String text) {
        return decode(text, false);
    }

    /**
     * The text of a quoted template, without its quotes, that stands for {@code value}.
     */
    public static String quoted(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c == '$' || c == '%') && i + 1 < value.length() && value.charAt(i + 1) == '{') {
                out.append(c).append(c);
            } else if (c == '\\') {
                out.append("\\\\");
            } else if (c == '"') {
                out.append("\\\"");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String decode(String text, boolean backslashEscapes) {
        if (text.indexOf('\\') < 0 && text.indexOf("${") < 0 && text.indexOf("%{") < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c == '$' || c == '%') && text.startsWith(c == '$' ? "$${" : "%%{", i)) {
                out.append(c).append('{');
                i += 3;
            } else if (c == '\\' && backslashEscapes && i + 1 < text.length()) {
                i = unescape(text, i, out);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int unescape(String text, int backslash, StringBuilder out) {
        char escaped = text.charAt(backslash + 1);
        switch (escaped) {
            case 'n':
                out.append('\n');
                return backslash + 2;
            case 'r':
                out.append('\r');
                return backslash + 2;
            case 't':
                out.append('\t');
                return backslash + 2;
            case '"':
            case '\\':
                out.append(escaped);
                return backslash + 2;
            case 'u':
            case 'U':
                int end = backslash + 2 + (escaped == 'u' ? 4 : 8);
                if (end <= text.length()) {
                    try {
                        out.appendCodePoint(Integer.parseUnsignedInt(text.substring(backslash + 2, end), 16));
                        return end;
                    } catch (IllegalArgumentException ignored) {
                        // not hex, or beyond the last code point
                    }
                }
                break;
            default:
                break;
        }
        out.append('\\');
        return backslash + 1;
    }
}
