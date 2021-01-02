/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import org.openrewrite.Cursor;
import org.openrewrite.EvalContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoEvalVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

public class TabsAndIndents extends JavaIsoEvalVisitor {
    TabsAndIndentsStyle style;

    public TabsAndIndents() {
        setCursoringOn();
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, EvalContext ctx) {
        style = cu.getStyle(TabsAndIndentsStyle.class);
        if (style == null) {
            style = IntelliJ.defaultTabsAndIndents();
        }
        return super.visitCompilationUnit(cu, ctx);
    }

    @Override
    public Statement visitStatement(Statement statement, EvalContext ctx) {
        Statement s = statement;

        if (s.getPrefix().getWhitespace().contains("\n")) {
            Cursor parentCursor = getCursor().getParentOrThrow();
            J parent = parentCursor.getTree();
            if (s instanceof J.Block) {
                s = indent(s, parent);
            } else {
                Tree p = parentCursor.getTree();
                Cursor cursor = parentCursor;

                // find the first cursor element that is indented further to the left
                for (;
                     p instanceof J.Block || p instanceof J.Label || p instanceof J.Try.Catch ||
                             p instanceof J.If && cursor.getParentOrThrow().getTree() instanceof J.If.Else ||
                             p instanceof J.If.Else;
                     p = cursor.getTree()) {
                    cursor = cursor.getParentOrThrow();
                }

                s = indent(s, p);
            }

            rebaseCursor(s);
        }
        return super.visitStatement(s, ctx);
    }

    @Override
    public J.Block visitBlock(J.Block block, EvalContext ctx) {
        J.Block j = super.visitBlock(block, ctx);
        if (j.getEnd().getWhitespace().contains("\n")) {
            Cursor cursor = getCursor().getParentOrThrow();
            Tree p = cursor.getTree();

            for (; p instanceof J.Try.Catch ||
                    p instanceof J.If && cursor.getParentOrThrow().getTree() instanceof J.If.Else ||
                    p instanceof J.If.Else; p = cursor.getTree()) {
                cursor = cursor.getParentOrThrow();
            }

            j = j.withEnd(align(j.getEnd(), cursor.getTree()));
        }
        return j;
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, EvalContext ctx) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, ctx);
        d = d.withWhileCondition(d.getWhileCondition().withBefore(
                align(d.getWhileCondition().getBefore(), d)));
        return d;
    }

    private <J2 extends J> J2 indent(J2 s, Tree parent) {
        return s.withPrefix(indent(s.getPrefix(), parent));
    }

    private Space indent(Space space, Tree parent) {
        int parentIndent = ((J) parent).getPrefix().getIndent().length();
        int indent = space.getIndent().length();

        if (indent - parentIndent < style.getContinuationIndent()) {
            int shiftRight = style.getIndentSize() + parentIndent - indent;
            space = space.withComments(ListUtils.map(space.getComments(), c ->
                    indentComment(c, shiftRight)));
            space = space.withWhitespace(indent(space.getWhitespace(), shiftRight));
        }

        return space;
    }

    private Space align(Space space, Tree parent) {
        int parentIndent = ((J) parent).getPrefix().getIndent().length();
        int indent = space.getIndent().length();

        if (indent - parentIndent < 0) {
            int shiftRight = parentIndent - indent;
            space = space.withComments(ListUtils.map(space.getComments(), c ->
                    indentComment(c, shiftRight)));
            space = space.withWhitespace(indent(space.getWhitespace(), shiftRight));
        }

        return space;
    }

    private Comment indentComment(Comment comment, int shiftRight) {
        StringBuilder newSuffix = new StringBuilder(comment.getSuffix());
        for (int i = 0; i < shiftRight; i++) {
            newSuffix.append(' ');
        }

        String newText = comment.getText();
        if (comment.getStyle() != Comment.Style.LINE) {
            StringBuilder newTextBuilder = new StringBuilder();
            for (char c : comment.getText().toCharArray()) {
                newTextBuilder.append(c);
                if (c == '\n') {
                    for (int i = 0; i < shiftRight; i++) {
                        newTextBuilder.append(' ');
                    }
                }
            }
            newText = newTextBuilder.toString();
        }

        return comment.withText(newText).withSuffix(newSuffix.toString());
    }

    private String indent(String whitespace, int shiftRight) {
        StringBuilder newWhitespace = new StringBuilder(whitespace);
        for (int i = 0; i < shiftRight; i++) {
            newWhitespace.append(' ');
        }
        return newWhitespace.toString();
    }
}
