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
package org.openrewrite.coldfusion.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.internal.Layout;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.GeneralFormatStyle;

/**
 * Ends every line of whitespace the way most of the file's lines end. A file's text, its strings and the whitespace
 * a template writes keep the line breaks they have, so a whole file can only be made consistent with its own
 * majority, and lines a recipe adds follow it. A file with no line break yet follows the {@link GeneralFormatStyle}.
 * A lone carriage return, as classic Mac OS ended lines, counts as a line break.
 */
public class NormalizeLineBreaksVisitor<P> extends FormattingVisitor<P> {
    private final GeneralFormatStyle style;

    @Nullable
    private String lineBreak;

    public NormalizeLineBreaksVisitor(GeneralFormatStyle style) {
        this(style, null);
    }

    public NormalizeLineBreaksVisitor(GeneralFormatStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.style = style;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (lineBreak == null && tree != null) {
            SourceFile file = tree instanceof SourceFile ? (SourceFile) tree : getCursor().firstEnclosing(SourceFile.class);
            lineBreak = file == null ? style.newLine() : mostCommon(file.printAll(), style.newLine());
        }
        return super.visit(tree, p);
    }

    @Override
    protected Space whitespace(Space space, Object loc, boolean prefix, P p) {
        if (!Layout.hasLineBreak(space) || lineBreak == null || isWritten(loc, prefix) || isInString()) {
            return space;
        }
        String target = lineBreak;
        return space.withWhitespace(normalize(space.getWhitespace(), target))
                .withComments(ListUtils.map(space.getComments(), comment ->
                        comment.withSuffix(normalize(comment.getSuffix(), target))));
    }

    static String mostCommon(String source, String otherwise) {
        int lf = 0;
        int crlf = 0;
        int cr = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                lf++;
            } else if (c == '\r') {
                if (i + 1 < source.length() && source.charAt(i + 1) == '\n') {
                    crlf++;
                    i++;
                } else {
                    cr++;
                }
            }
        }
        if (lf == 0 && crlf == 0 && cr == 0) {
            return otherwise;
        }
        if (crlf > lf && crlf >= cr) {
            return "\r\n";
        }
        return cr > lf && cr > crlf ? "\r" : "\n";
    }

    private static String normalize(String s, String lineBreak) {
        if (!Layout.hasLineBreak(s)) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append(lineBreak);
            } else if (c == '\n') {
                out.append(lineBreak);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
