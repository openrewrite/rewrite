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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TabsAndIndents extends Recipe {
    public TabsAndIndents() {
        super(TabsAndIndentsProcessor::new);
    }

    private static class TabsAndIndentsProcessor extends JavaIsoProcessor<ExecutionContext> {
        TabsAndIndentsStyle style;

        public TabsAndIndentsProcessor() {
            setCursoringOn();
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            style = cu.getStyle(TabsAndIndentsStyle.class);
            if (style == null) {
                style = IntelliJ.defaultTabsAndIndents();
            }
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public Statement visitStatement(Statement statement, ExecutionContext ctx) {
            Statement s = statement;

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

            return super.visitStatement(s, ctx);
        }

        @Override
        public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, ExecutionContext ctx) {
            J.ArrayDimension a = continuationIndent(super.visitArrayDimension(arrayDimension, ctx), enclosingStatement());
            a = a.withIndex(continuationIndent(a.getIndex(), enclosingStatement()));
            a = a.withIndex(a.getIndex().withAfter(continuationIndent(a.getIndex().getAfter(), enclosingStatement())));
            return a;
        }

        @Override
        public J.Assign visitAssign(J.Assign assign, ExecutionContext ctx) {
            J.Assign a = super.visitAssign(assign, ctx);
            a = a.withAssignment(continuationIndent(a.getAssignment(), enclosingStatement()));
            return a;
        }

        @Override
        public J.AssignOp visitAssignOp(J.AssignOp assignOp, ExecutionContext ctx) {
            J.AssignOp a = super.visitAssignOp(assignOp, ctx);
            a = a.withAssignment(continuationIndent(a.getAssignment(), enclosingStatement()));
            return a;
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
            J.Binary b = super.visitBinary(binary, ctx);
            b = b.withOperator(continuationIndent(b.getOperator(), enclosingStatement()));
            return b;
        }

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            J.Block j = super.visitBlock(block, ctx);
            Cursor cursor = getCursor().getParentOrThrow();
            Tree p = cursor.getTree();

            for (; p instanceof J.Try.Catch ||
                    p instanceof J.If && cursor.getParentOrThrow().getTree() instanceof J.If.Else ||
                    p instanceof J.If.Else; p = cursor.getTree()) {
                cursor = cursor.getParentOrThrow();
            }

            return j.withEnd(alignToParentStatement(j.getEnd(), cursor));
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, ExecutionContext ctx) {
            J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, ctx);
            return d.withWhileCondition(d.getWhileCondition().withBefore(
                    alignToParentStatement(d.getWhileCondition().getBefore(), getCursor())));
        }

        @Override
        public Expression visitExpression(Expression expression, ExecutionContext ctx) {
            Expression e = super.visitExpression(expression, ctx);
            if (!(getCursor().getParentOrThrow().getTree() instanceof J.Block) &&
                    !(getCursor().getParentOrThrow().getTree() instanceof J.Case)) {
                e = continuationIndent(e, enclosingStatement());
            }
            return e;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
            J.ForLoop f = (J.ForLoop) visitStatement(forLoop, ctx);
            f = f.withBody(call(f.getBody(), ctx));

            J.ForLoop.Control control = forLoop.getControl();
            JRightPadded<Statement> init = control.getInit();
            JRightPadded<Expression> condition = control.getCondition();
            List<JRightPadded<Statement>> update = control.getUpdate();
            if (init.getElem().getPrefix().getWhitespace().contains("\n")) {
                f = f.withControl(control
                        .withInit(continuationIndent(init, f))
                        .withCondition(continuationIndent(condition, f))
                        .withUpdate(continuationIndent(update, f)));
            } else {
                f = f.withControl(f.getControl().withCondition(condition.withElem(
                        alignTo(condition.getElem(), forInitColumn()))));

                int column = forInitColumn();
                f = f.withControl(f.getControl().withUpdate(ListUtils.map(update, (i, j) ->
                        j.withElem(alignTo(j.getElem(), i == 0 ? column :
                                column + style.getContinuationIndent())))));
            }

            return f;
        }

        private int forInitColumn() {
            J.ForLoop forLoop = getCursor().getTree();
            J parent = getCursor().getParentOrThrow().getTree();
            J alignTo = parent instanceof J.Label ?
                    ((J.Label) parent).withStatement(forLoop.withBody(null)) :
                    forLoop.withBody(null);

            int column = 0;
            boolean afterInitStart = false;
            for (char c : alignTo.print().toCharArray()) {
                if (c == '(') {
                    afterInitStart = true;
                } else if (afterInitStart && !Character.isWhitespace(c)) {
                    return column - 1;
                }
                column++;
            }
            throw new IllegalStateException("For loops must have a control section");
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference m = super.visitMemberReference(memberRef, ctx);
            m = m.withReference(continuationIndent(m.getReference(), enclosingStatement()));
            return m;
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method, ExecutionContext ctx) {
            J.MethodDecl m = (J.MethodDecl) visitStatement(method, ctx);
            m = m.withBody(call(method.getBody(), ctx));

            List<JRightPadded<Statement>> params = m.getParams().getElem();
            if (!params.isEmpty()) {
                if (params.get(0).getElem().getPrefix().getWhitespace().contains("\n")) {
                    J.MethodDecl parent = m;
                    m = m.withParams(m.getParams().withElem(ListUtils.map(m.getParams().getElem(),
                            j -> continuationIndent(j, parent))));
                } else if (params.stream().anyMatch(p -> p.getElem().getPrefix().getWhitespace().contains("\n"))) {
                    AtomicInteger column = new AtomicInteger(0);
                    boolean afterParamsStart = false;
                    for (char c : m.withBody(null).print().toCharArray()) {
                        if (c == '(') {
                            afterParamsStart = true;
                        } else if (afterParamsStart && !Character.isWhitespace(c)) {
                            break;
                        }
                        column.incrementAndGet();
                    }
                    m = m.withParams(m.getParams().withElem(ListUtils.map(m.getParams().getElem(), (i, p) ->
                            i == 0 ? p : p.withElem(alignTo(p.getElem(), column.get() - 1)))));
                }
            }

            return m;
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, ExecutionContext ctx) {
            J.Ternary t = super.visitTernary(ternary, ctx);
            t = t.withTruePart(continuationIndent(t.getTruePart(), enclosingStatement()));
            t = t.withFalsePart(continuationIndent(t.getFalsePart(), enclosingStatement()));
            return t;
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
            J.Unary u = super.visitUnary(unary, ctx);
            u = u.withOperator(continuationIndent(u.getOperator(), enclosingStatement()));
            return u;
        }

        private <J2 extends J> List<JRightPadded<J2>> continuationIndent(List<JRightPadded<J2>> js, Tree parent) {
            return ListUtils.map(js, (i, j) -> {
                if (i == 0) {
                    return continuationIndent(j, parent);
                } else {
                    return j.withElem(j.getElem().withPrefix(indent(j.getElem().getPrefix(),
                            parent, style.getContinuationIndent() * 2)));
                }
            });
        }

        private <J2 extends J> JRightPadded<J2> continuationIndent(JRightPadded<J2> j, @Nullable Tree parent) {
            return j.withElem(j.getElem().withPrefix(indent(j.getElem().getPrefix(),
                    parent, style.getContinuationIndent())));
        }

        private <T> JLeftPadded<T> continuationIndent(JLeftPadded<T> t, Tree parent) {
            return t.withBefore(continuationIndent(t.getBefore(), parent));
        }

        private <J2 extends J> J2 continuationIndent(J2 j, @Nullable Tree parent) {
            return j.withPrefix(indent(j.getPrefix(), parent, style.getContinuationIndent()));
        }

        private <J2 extends J> JRightPadded<J2> indent(JRightPadded<J2> j, Tree parent) {
            return j.withElem(j.getElem().withPrefix(indent(j.getElem().getPrefix(),
                    parent, style.getIndentSize())));
        }

        private <J2 extends J> J2 indent(J2 j, Tree parent) {
            return j.withPrefix(indent(j.getPrefix(), parent, style.getIndentSize()));
        }

        @Nullable
        private Statement enclosingStatement() {
            Cursor cursor = getCursor();
            //noinspection StatementWithEmptyBody
            for (; cursor != null && !(cursor.getTree() instanceof Statement);
                 cursor = cursor.getParent())
                ;
            return cursor == null ? null : cursor.getTree();
        }

        private Space continuationIndent(Space space, @Nullable Tree parent) {
            return indent(space, parent, style.getContinuationIndent());
        }

        private Space indent(Space space, @Nullable Tree parent) {
            return indent(space, parent, style.getIndentSize());
        }

        private Space indent(Space space, @Nullable Tree parent, int indentSize) {
            if (parent == null || !space.getWhitespace().contains("\n")) {
                return space;
            }

            int parentIndent = indent(((J) parent).getPrefix());
            int indent = indent(space);

            if (indent - parentIndent < indentSize) {
                int shiftRight = indentSize + parentIndent - indent;
                space = space.withComments(ListUtils.map(space.getComments(), c ->
                        indentComment(c, shiftRight)));
                space = space.withWhitespace(indent(space.getWhitespace(), shiftRight));
            }

            return space;
        }

        private Space alignToParentStatement(Space space, Cursor parent) {
            J alignTo = parent.getParentOrThrow().getTree() instanceof J.Label ?
                    parent.getParentOrThrow().getTree() :
                    parent.getTree();

            return alignTo(space, indent(alignTo.getPrefix()));
        }

        private <J2 extends J> J2 alignTo(J2 j, int column) {
            return j.withPrefix(alignTo(j.getPrefix(), column));
        }

        private Space alignTo(Space space, int column) {
            if (!space.getWhitespace().contains("\n")) {
                return space;
            }

            int indent = indent(space);

            if (indent - column < 0) {
                int shiftRight = column - indent;
                space = space.withComments(ListUtils.map(space.getComments(), c ->
                        indentComment(c, shiftRight)));
                space = space.withWhitespace(indent(space.getWhitespace(), shiftRight));
            }

            return space;
        }

        private Comment indentComment(Comment comment, int shiftRight) {
            StringBuilder newSuffix = new StringBuilder(comment.getSuffix());
            shiftRight(newSuffix, shiftRight);

            String newText = comment.getText();
            if (comment.getStyle() != Comment.Style.LINE) {
                StringBuilder newTextBuilder = new StringBuilder();
                for (char c : comment.getText().toCharArray()) {
                    newTextBuilder.append(c);
                    if (c == '\n') {
                        shiftRight(newTextBuilder, shiftRight);
                    }
                }
                newText = newTextBuilder.toString();
            }

            return comment.withText(newText).withSuffix(newSuffix.toString());
        }

        private String indent(String whitespace, int shiftRight) {
            StringBuilder newWhitespace = new StringBuilder(whitespace);
            shiftRight(newWhitespace, shiftRight);
            return newWhitespace.toString();
        }

        private void shiftRight(StringBuilder text, int shiftRight) {
            int tabIndent = style.getTabSize();
            if (!style.isUseTabCharacter()) {
                tabIndent = Integer.MAX_VALUE;
            }

            for (int i = 0; i < shiftRight / tabIndent; i++) {
                text.append('\t');
            }

            for (int i = 0; i < shiftRight % tabIndent; i++) {
                text.append(' ');
            }
        }

        private int indent(Space space) {
            String indent = space.getIndent();
            int size = 0;
            for (char c : indent.toCharArray()) {
                size += c == '\t' ? style.getTabSize() : 1;
                if (c == '\n' || c == '\r') {
                    size = 0;
                }
            }
            return size;
        }
    }
}
