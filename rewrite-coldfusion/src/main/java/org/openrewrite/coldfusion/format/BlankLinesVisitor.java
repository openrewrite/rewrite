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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.internal.Layout;
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.style.BlankLinesStyle;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Keeps the blank lines between statements and declarations within the {@link BlankLinesStyle}'s maximums, and puts
 * at least its minimum around a component's functions. Between tags, only where a template is silent and holds
 * tags alone, since a template writes the blank lines between its tags to its output.
 */
public class BlankLinesVisitor<P> extends FormattingVisitor<P> {
    private final BlankLinesStyle style;

    public BlankLinesVisitor(BlankLinesStyle style) {
        this(style, null);
    }

    public BlankLinesVisitor(BlankLinesStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.style = style;
    }

    @Override
    protected Space whitespace(Space space, Object loc, boolean prefix, P p) {
        if (!Layout.hasLineBreak(space) || isInInterpolation() || isWritten(loc, prefix)) {
            return space;
        }
        Cursor cursor = getCursor();
        if (loc == Space.Location.BLOCK_END) {
            if (TemplateOutput.isTemplateBody(cursor) && hasText(cursor)) {
                return space;
            }
            return keepMaximum(space, style.getKeepMaximum().getBeforeEndOfBlock());
        }
        if (!prefix) {
            return space;
        }
        Cursor list = cursor.getParentTreeCursor();
        if (list.getValue() instanceof Cf.ExpressionStatement) {
            cursor = list;
            list = list.getParentTreeCursor();
        }
        List<? extends J> siblings = statements(list.getValue());
        int index = indexOf(siblings, cursor.getValue());
        if (index < 0 || TemplateOutput.isTemplateList(list) && hasText(list)) {
            return space;
        }
        boolean declarations = isComponentBody(list);
        Space s = keepMaximum(space, declarations ? style.getKeepMaximum().getInDeclarations() : style.getKeepMaximum().getInCode());
        if (declarations && index > 0 &&
            (siblings.get(index) instanceof J.MethodDeclaration || siblings.get(index - 1) instanceof J.MethodDeclaration)) {
            s = minimum(s, style.getMinimum().getAroundFunction());
        }
        return s;
    }

    private static List<? extends J> statements(Object list) {
        if (list instanceof J.Block) {
            return ((J.Block) list).getStatements();
        }
        if (list instanceof J.Case) {
            return ((J.Case) list).getStatements();
        }
        if (list instanceof Cf.CompilationUnit) {
            return ((Cf.CompilationUnit) list).getStatements();
        }
        if (list instanceof J.MethodDeclaration && TemplateOutput.isTagSyntax((J) list)) {
            return ((J.MethodDeclaration) list).getParameters();
        }
        return emptyList();
    }

    private static int indexOf(List<? extends J> siblings, Object element) {
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isComponentBody(Cursor list) {
        return list.getValue() instanceof J.Block && list.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
    }

    private static boolean hasText(Cursor list) {
        for (J statement : statements(list.getValue())) {
            if (statement instanceof Cf.Text) {
                return true;
            }
        }
        return false;
    }

    private static Space keepMaximum(Space space, int max) {
        return space.withWhitespace(Layout.keepMaximumBlankLines(space.getWhitespace(), max))
                .withComments(ListUtils.map(space.getComments(), c -> c.withSuffix(Layout.keepMaximumBlankLines(c.getSuffix(), max))));
    }

    /**
     * At least {@code min} blank lines before a declaration, and before the comments on lines of their own that
     * belong to it.
     */
    private static Space minimum(Space space, int min) {
        if (min <= 0) {
            return space;
        }
        List<Comment> comments = space.getComments();
        if (comments.isEmpty() || Layout.hasLineBreak(space.getWhitespace())) {
            String whitespace = space.getWhitespace();
            return space.withWhitespace(Layout.minimumBlankLines(whitespace, min, Layout.lineBreakIn(whitespace)));
        }
        // the first comment ends the line before, so the blank lines go after it
        return space.withComments(ListUtils.mapFirst(comments, c ->
                c.withSuffix(Layout.minimumBlankLines(c.getSuffix(), min, Layout.lineBreakIn(c.getSuffix())))));
    }
}
