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
package org.openrewrite.java.internal;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;

import java.util.List;
import java.util.Objects;

import static org.openrewrite.internal.StringUtils.repeat;

/**
 * The start of a line whose code was changed or replaced in place: a formatter gives such code the indentation of a
 * file formatted from scratch, but the line keeps the indentation, and the comments above it, that it had.
 */
public final class LineStart {

    private LineStart() {
    }

    /**
     * The prefix for code that took the place of code whose prefix was {@code original}, given {@code given} and then
     * formatted to {@code formatted}. When the code was given the original's indentation, so that it stays where the
     * original was, and the formatted code still starts its line, the line keeps that indentation. When the code was
     * given the original's comments, they keep their text and indentation too; comments the code was given instead
     * keep their formatting. Code given another indentation, as code moved elsewhere or given a fresh line break is,
     * keeps the formatter's. Blank lines above the line, when the code was given them as they were, keep their bytes:
     * how many there are and the whitespace on them. Above a line that had none, the formatter chooses the blank lines.
     */
    public static Space keep(Space formatted, Space original, Space given) {
        if (!startsLine(original) || !startsLine(given) || !startsLine(formatted) ||
            !indentation(given).equals(indentation(original))) {
            return formatted;
        }
        if (Objects.equals(given.getComments(), original.getComments())) {
            String whitespace = original.getWhitespace();
            String formattedWhitespace = formatted.getWhitespace();
            int lastBreak = whitespace.lastIndexOf('\n');
            // blank lines the code was given as they were are the file's, not the recipe's
            boolean blankLinesAsTheyWere = whitespace.indexOf('\n') != lastBreak &&
                                           whitespace.equals(given.getWhitespace());
            if (lastBreak >= 0 && formattedWhitespace.indexOf('\n') >= 0 && !blankLinesAsTheyWere) {
                String lineBreak = whitespace.contains("\r\n") ? "\r\n" : "\n";
                int breaks = (int) formattedWhitespace.chars().filter(c -> c == '\n').count();
                whitespace = repeat(lineBreak, breaks) + whitespace.substring(lastBreak + 1);
            }
            return original.withWhitespace(whitespace);
        }
        return withIndentation(formatted, indentation(original));
    }

    /**
     * Whether code with this prefix starts a line, after any comments the prefix holds.
     */
    public static boolean startsLine(Space prefix) {
        if (prefix.getWhitespace().indexOf('\n') >= 0) {
            return true;
        }
        for (Comment comment : prefix.getComments()) {
            if (comment.getSuffix().indexOf('\n') >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The whitespace after the prefix's last line break, which indents the line the code starts.
     */
    private static String indentation(Space prefix) {
        String indentation = afterLastBreak(prefix.getWhitespace());
        for (Comment comment : prefix.getComments()) {
            String afterBreak = afterLastBreak(comment.getSuffix());
            if (comment.getSuffix().indexOf('\n') >= 0) {
                indentation = afterBreak;
            }
        }
        return indentation;
    }

    private static Space withIndentation(Space prefix, String indentation) {
        List<Comment> comments = prefix.getComments();
        for (int i = comments.size() - 1; i >= 0; i--) {
            String suffix = comments.get(i).getSuffix();
            if (suffix.indexOf('\n') >= 0) {
                int index = i;
                return prefix.withComments(ListUtils.map(comments, (j, c) ->
                        j == index ? c.withSuffix(reindent(suffix, indentation)) : c));
            }
        }
        return prefix.withWhitespace(reindent(prefix.getWhitespace(), indentation));
    }

    private static String reindent(String whitespace, String indentation) {
        int lastBreak = whitespace.lastIndexOf('\n');
        return lastBreak < 0 ? whitespace : whitespace.substring(0, lastBreak + 1) + indentation;
    }

    private static String afterLastBreak(String whitespace) {
        return whitespace.substring(whitespace.lastIndexOf('\n') + 1);
    }
}
