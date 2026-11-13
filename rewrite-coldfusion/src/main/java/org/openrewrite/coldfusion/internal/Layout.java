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
package org.openrewrite.coldfusion.internal;

import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;

/**
 * Line breaks and indentation in whitespace. CFML files end their lines with {@code \n}, {@code \r\n} or, from
 * classic Mac OS, a lone {@code \r}, so each counts as a line break.
 */
public final class Layout {

    private Layout() {
    }

    public static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    public static boolean hasLineBreak(String s) {
        return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
    }

    public static boolean hasLineBreak(Space space) {
        if (hasLineBreak(space.getWhitespace())) {
            return true;
        }
        for (Comment comment : space.getComments()) {
            if (hasLineBreak(comment.getSuffix())) {
                return true;
            }
        }
        return false;
    }

    public static int lineBreaks(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' && (i + 1 == s.length() || s.charAt(i + 1) != '\n')) {
                count++;
            }
        }
        return count;
    }

    /**
     * Where the text after the last line break starts, or -1 when there is none.
     */
    public static int lastLineStart(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            if (isLineBreak(s.charAt(i))) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * The whitespace after the last line break, or all of it when there is no line break.
     */
    public static String lastLine(String s) {
        int start = lastLineStart(s);
        return start < 0 ? s : s.substring(start);
    }

    /**
     * The columns that leading whitespace spans, a tab advancing to the next tab stop.
     */
    public static int width(String indent, int tabSize) {
        int column = 0;
        for (int i = 0; i < indent.length(); i++) {
            char c = indent.charAt(i);
            if (c == '\t') {
                column += tabSize - column % tabSize;
            } else if (c == ' ' || c == '\f' || c == '\u000B') {
                column++;
            } else {
                break;
            }
        }
        return column;
    }

    /**
     * Whitespace spanning the given columns, in tabs and spaces or in spaces alone.
     */
    public static String indent(int width, boolean useTabs, int tabSize) {
        StringBuilder indent = new StringBuilder(width);
        int column = 0;
        if (useTabs && tabSize > 0) {
            for (; column + tabSize <= width; column += tabSize) {
                indent.append('\t');
            }
        }
        for (; column < width; column++) {
            indent.append(' ');
        }
        return indent.toString();
    }

    /**
     * The whitespace with the text after its last line break replaced.
     */
    public static String withLastLine(String s, String lastLine) {
        int start = lastLineStart(s);
        return start < 0 ? s : s.substring(0, start) + lastLine;
    }

    /**
     * Whether whitespace holds spaces or tabs just before a line break, which a line would end with.
     */
    public static boolean hasTrailingWhitespace(String s) {
        for (int i = 1; i < s.length(); i++) {
            if (isLineBreak(s.charAt(i)) && (s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\t')) {
                return true;
            }
        }
        return false;
    }

    /**
     * The whitespace without the spaces and tabs that end its lines.
     */
    public static String withoutTrailingWhitespace(String s) {
        if (!hasTrailingWhitespace(s)) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        int pending = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t') {
                pending++;
                continue;
            }
            if (!isLineBreak(c)) {
                out.append(s, i - pending, i);
            }
            pending = 0;
            out.append(c);
        }
        out.append(s, s.length() - pending, s.length());
        return out.toString();
    }

    /**
     * The whitespace with at most {@code max} blank lines, keeping its last line.
     */
    public static String keepMaximumBlankLines(String s, int max) {
        int breaks = lineBreaks(s);
        if (breaks - 1 <= max) {
            return s;
        }
        int[] starts = new int[breaks];
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isLineBreak(c)) {
                starts[n++] = i;
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }
        // what precedes the first line break ends the previous line
        return s.substring(0, starts[0]) + s.substring(starts[breaks - max - 1]);
    }

    /**
     * The whitespace with at least {@code min} blank lines, given the line break it is written with.
     */
    public static String minimumBlankLines(String s, int min, String lineBreak) {
        int breaks = lineBreaks(s);
        if (breaks == 0 || breaks - 1 >= min) {
            return s;
        }
        StringBuilder out = new StringBuilder();
        int first = 0;
        while (first < s.length() && !isLineBreak(s.charAt(first))) {
            first++;
        }
        out.append(s, 0, first);
        for (int i = 0; i < min - (breaks - 1); i++) {
            out.append(lineBreak);
        }
        out.append(s, first, s.length());
        return out.toString();
    }

    /**
     * The line break whitespace uses, or {@code \n} when it has none.
     */
    public static String lineBreakIn(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                return "\n";
            }
            if (c == '\r') {
                return i + 1 < s.length() && s.charAt(i + 1) == '\n' ? "\r\n" : "\r";
            }
        }
        return "\n";
    }
}
