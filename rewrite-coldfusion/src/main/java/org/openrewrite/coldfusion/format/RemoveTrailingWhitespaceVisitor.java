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
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.internal.Layout;
import org.openrewrite.coldfusion.style.CFFormat;
import org.openrewrite.coldfusion.style.TabsAndIndentsStyle;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Space;

/**
 * Removes the spaces and tabs that end lines. A blank line keeps its indentation where the
 * {@link TabsAndIndentsStyle} keeps indents on empty lines. A template writes the whitespace between its tags to its
 * output, so that stays as it is.
 */
public class RemoveTrailingWhitespaceVisitor<P> extends FormattingVisitor<P> {
    private final boolean keepIndentsOnEmptyLines;

    public RemoveTrailingWhitespaceVisitor() {
        this(null);
    }

    public RemoveTrailingWhitespaceVisitor(@Nullable Tree stopAfter) {
        this(CFFormat.tabsAndIndents(), stopAfter);
    }

    public RemoveTrailingWhitespaceVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.keepIndentsOnEmptyLines = style.getKeepIndentsOnEmptyLines();
    }

    @Override
    protected Space whitespace(Space space, Object loc, boolean prefix, P p) {
        boolean eof = loc == Space.Location.COMPILATION_UNIT_EOF;
        if (!(eof || Layout.hasLineBreak(space)) || isWritten(loc, prefix) || isInString()) {
            return space;
        }
        Space s = space.withWhitespace(trimmed(space.getWhitespace()))
                .withComments(ListUtils.map(space.getComments(), comment -> comment.withSuffix(trimmed(comment.getSuffix()))));
        if (eof) {
            // what follows the last line break of the file ends its last line
            String last = s.getLastWhitespace();
            int lastLine = Layout.lastLineStart(last);
            String trimmed = lastLine < 0 ? "" : last.substring(0, lastLine);
            if (!trimmed.equals(last)) {
                s = s.getComments().isEmpty() ? s.withWhitespace(trimmed) :
                        s.withComments(ListUtils.mapLast(s.getComments(), c -> c.withSuffix(trimmed)));
            }
        }
        return s;
    }

    /**
     * Whitespace without what ends its lines. Only the line before its first line break ends with something other
     * than a blank line, so that is all that is trimmed when blank lines keep their indents.
     */
    private String trimmed(String whitespace) {
        if (!keepIndentsOnEmptyLines) {
            return Layout.withoutTrailingWhitespace(whitespace);
        }
        int firstBreak = 0;
        while (firstBreak < whitespace.length() && !Layout.isLineBreak(whitespace.charAt(firstBreak))) {
            firstBreak++;
        }
        if (firstBreak == whitespace.length()) {
            return whitespace;
        }
        return Layout.withoutTrailingWhitespace(whitespace.substring(0, firstBreak + 1)) + whitespace.substring(firstBreak + 1);
    }
}
