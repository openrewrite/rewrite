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
import org.openrewrite.coldfusion.style.TabsAndIndentsStyle;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Space;

/**
 * Indents lines with tabs or with spaces, as the style says, keeping how wide each indentation is. With tabs, spaces
 * after a line's tabs are alignment and stay. A template writes the whitespace between its tags, which keeps its
 * characters, and the text of comments is left as it is.
 */
public class NormalizeTabsOrSpacesVisitor<P> extends FormattingVisitor<P> {
    private final TabsAndIndentsStyle style;

    public NormalizeTabsOrSpacesVisitor(TabsAndIndentsStyle style) {
        this(style, null);
    }

    public NormalizeTabsOrSpacesVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.style = style;
    }

    @Override
    protected Space whitespace(Space space, Object loc, boolean prefix, P p) {
        if (!Layout.hasLineBreak(space) || isWritten(loc, prefix) || isInString()) {
            return space;
        }
        return space.withWhitespace(normalize(space.getWhitespace()))
                .withComments(ListUtils.map(space.getComments(), comment -> comment.withSuffix(normalize(comment.getSuffix()))));
    }

    private String normalize(String s) {
        StringBuilder out = null;
        int copied = 0;
        for (int i = 0; i < s.length(); i++) {
            if (!Layout.isLineBreak(s.charAt(i))) {
                continue;
            }
            int start = i + 1;
            if (s.charAt(i) == '\r' && start < s.length() && s.charAt(start) == '\n') {
                start++;
            }
            int end = start;
            while (end < s.length() && (s.charAt(end) == ' ' || s.charAt(end) == '\t')) {
                end++;
            }
            String indent = s.substring(start, end);
            String normalized = normalizeIndent(indent);
            if (!normalized.equals(indent)) {
                if (out == null) {
                    out = new StringBuilder(s.length());
                }
                out.append(s, copied, start).append(normalized);
                copied = end;
            }
            i = end - 1;
        }
        if (out == null) {
            return s;
        }
        return out.append(s, copied, s.length()).toString();
    }

    private String normalizeIndent(String indent) {
        int tabSize = style.getTabSize();
        if (style.getUseTabCharacter()) {
            int tabs = 0;
            while (tabs < indent.length() && indent.charAt(tabs) == '\t') {
                tabs++;
            }
            boolean onlySpacesAfterTabs = indent.indexOf('\t', tabs) < 0;
            int width = Layout.width(indent, tabSize);
            if (onlySpacesAfterTabs && (tabs > 0 || width < tabSize)) {
                return indent;
            }
            return Layout.indent(width, true, tabSize);
        }
        return indent.indexOf('\t') < 0 ? indent : Layout.indent(Layout.width(indent, tabSize), false, tabSize);
    }
}
