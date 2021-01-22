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
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;

import java.util.List;

class TabsAndIndentsProcessor<P> extends JavaIsoProcessor<P> {
    private final TabsAndIndentsStyle style;

    public TabsAndIndentsProcessor(TabsAndIndentsStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement s = statement;
        Cursor parentCursor = getCursor().getParentOrThrow();
        J parent = parentCursor.getValue();

        if (!(s instanceof J.Block)) {
            parent = parentCursor.getValue();
            Cursor cursor = parentCursor;

            // find the first cursor element that is indented further to the left
            for (;
                 parent instanceof J.Block || parent instanceof J.Label || parent instanceof J.Try.Catch ||
                         parent instanceof J.If && cursor.getParentOrThrow().getValue() instanceof J.If.Else ||
                         parent instanceof J.If.Else;
                 parent = cursor.getValue()) {
                cursor = cursor.getParentOrThrow();
            }
        }

        s = indent(s, parent);
        rebaseCursor(s);

        return super.visitStatement(s, p);
    }

    @Override
    public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        J.ArrayDimension arrDim = super.visitArrayDimension(arrayDimension, p);
        J.ArrayDimension a = continuationIndent(arrDim, enclosingStatement());
        a = a.withIndex(a.getIndex().withAfter(continuationIndent(a.getIndex().getAfter(), enclosingStatement())));
        return a;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block j = super.visitBlock(block, p);
        Cursor cursor = getCursor().getParentOrThrow();
        Tree parent = cursor.getValue();

        for (; parent instanceof J.Try.Catch ||
                parent instanceof J.If && cursor.getParentOrThrow().getValue() instanceof J.If.Else ||
                parent instanceof J.If.Else; parent = cursor.getValue()) {
            cursor = cursor.getParentOrThrow();
        }

        return j.withEnd(alignToParentStatement(j.getEnd(), cursor));
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        return d.withWhileCondition(d.getWhileCondition().withBefore(
                alignToParentStatement(d.getWhileCondition().getBefore(), getCursor())));
    }

    @Override
    public Expression visitExpression(Expression expression, P p) {
        Expression e = super.visitExpression(expression, p);
        if (expression instanceof J.Annotation) {
            e = e.withPrefix(alignTo(e.getPrefix(),
                    indent(getCursor().getParentOrThrow().<J>getValue().getPrefix())));
        } else if (!(getCursor().getParentOrThrow().getValue() instanceof J.Block) &&
                !(getCursor().getParentOrThrow().getValue() instanceof J.Case) &&
                !(getCursor().getParentOrThrow().getValue() instanceof J.MethodDecl) &&
                !(getCursor().getParentOrThrow().getValue() instanceof J.VariableDecls)) {
            e = continuationIndent(e, enclosingStatement());
        }
        return e;
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = (J.ForLoop) visitStatement(forLoop, p);
        f = f.withBody(visitRightPadded(f.getBody(), JRightPadded.Location.FOR_BODY, p));

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
        J.ForLoop forLoop = getCursor().getValue();
        J parent = getCursor().getParentOrThrow().getValue();
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
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = super.visitMemberReference(memberRef, p);
        m = m.withReference(continuationIndent(m.getReference(), enclosingStatement()));
        return m;
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = super.visitMethod(method, p);
        List<JRightPadded<Statement>> params = m.getParams().getElem();
        if (!params.isEmpty()) {
            if (params.get(0).getElem().getPrefix().getWhitespace().contains("\n")) {
                J.MethodDecl parent = m;
                m = m.withParams(m.getParams().withElem(ListUtils.map(m.getParams().getElem(),
                        j -> continuationIndent(j, parent))));
            } else if (params.stream().anyMatch(param -> param.getElem().getPrefix().getWhitespace().contains("\n"))) {
                String print = m.withBody(null).print();
                int nameStart = print.lastIndexOf(m.getSimpleName() + "(");
                int lastNl = -1;
                while (true) {
                    int nl = print.indexOf('\n', ++lastNl);
                    if (nl < 0 || nl > nameStart) {
                        break;
                    }
                    lastNl = nl;
                }
                int column = nameStart - lastNl + m.getSimpleName().length() + 1;
                m = m.withParams(m.getParams().withElem(ListUtils.map(m.getParams().getElem(), (i, param) ->
                        i == 0 ? param : param.withElem(alignTo(param.getElem(), column)))));
            }
        }

        m = m.withBody(call(m.getBody(), p));

        return m;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        if (m.getSelect() != null && m.getSelect().getAfter().getWhitespace().contains("\n")) {
            m = m.withSelect(m.getSelect().withAfter(continuationIndent(m.getSelect().getAfter(), enclosingStatement())));
        }

        if (m.getArgs().getLastSpace().getWhitespace().contains("\n")) {
            // align the closing ')' with the first enclosing statement on its own line
//            m = m.withArgs(m.getArgs().withElem(ListUtils.mapLast(m.getArgs().getElem(),
//                    arg -> {
//                        J enc = enclosingStatement(new Cursor(getCursor(), arg.getElem()));
//                        assert enc != null;
//                        return arg.withElem(alignTo(arg.getElem(), indent(enc.getPrefix())));
//                    })));
        }
        return m;
    }

    @Override
    public @Nullable <J2 extends J> JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        JContainer<J2> j = super.visitContainer(container, loc, p);
        if (j == null) {
            return null;
        }

        J current = getCursor().getTree();
        switch (loc) {
            case IMPLEMENTS:
            case METHOD_ARGUMENT:
            case NEW_ARRAY_INITIALIZER:
            case NEW_CLASS_ARGS:
            case THROWS:
            case TRY_RESOURCES:
            case TYPE_PARAMETER:
                j = j.withBefore(continuationIndent(j.getBefore(), current.getPrefix().getWhitespace().contains("\n") ?
                        current : enclosingStatement()));
                break;
            case TYPE_BOUND:
                // need TYPE_PARAMETER JContainer outside the TYPE_BOUND JContainer...
                break;
            case ANNOTATION_ARGUMENT:
                // any prefix will be on the parent MethodDecl/ClassDecl/VariableDecls
                j = j.withBefore(continuationIndent(j.getBefore(), getCursor().getParentOrThrow().getTree()));
                break;
            case CASE:
                // for some reason not needed?
                break;
        }
        return j;
    }

    @Nullable
    @Override
    public <T> JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, P p) {
        JLeftPadded<T> t = super.visitLeftPadded(left, loc, p);
        if (t == null) {
            return null;
        }

        switch (loc) {
            case ASSIGNMENT:
            case BINARY_OPERATOR:
            case MEMBER_REFERENCE:
            case TERNARY_TRUE:
            case TERNARY_FALSE:
                J current = getCursor().getTree();
                t = t.withBefore(continuationIndent(t.getBefore(), current.getPrefix().getWhitespace().contains("\n") ?
                        current : enclosingStatement()));
                break;
            case EXTENDS:
            case FIELD_ACCESS_NAME:
            case TRY_FINALLY:
            case VARIABLE_INITIALIZER:
            case WHILE_CONDITION:
                break;
            case UNARY_OPERATOR:
                // handled in visitUnary
                break;
        }
        return t;
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        J.Unary u = super.visitUnary(unary, p);
        switch (u.getOperator().getElem()) {
            case PreIncrement:
            case PreDecrement:
            case Negative:
            case Complement:
            case Not:
            case Positive:
                u = u.withOperator(continuationIndent(u.getOperator(), enclosingStatement()));
                break;
            case PostIncrement:
            case PostDecrement:
                u = u.withOperator(continuationIndent(u.getOperator(),
                        u.getPrefix().getWhitespace().contains("\n") ? u : enclosingStatement()));
                break;
        }
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

    private <T> JLeftPadded<T> continuationIndent(JLeftPadded<T> t, @Nullable Tree parent) {
        return t.withBefore(continuationIndent(t.getBefore(), parent));
    }

    private <J2 extends J> J2 continuationIndent(J2 j, @Nullable Tree parent) {
        return j.withPrefix(indent(j.getPrefix(), parent, style.getContinuationIndent()));
    }

    private <J2 extends J> J2 indent(J2 j, Tree parent) {
        if (parent instanceof J.CompilationUnit) {
            return j;
        }
        return j.withPrefix(indent(j.getPrefix(), parent, style.getIndentSize()));
    }

    @Nullable
    private J enclosingStatement() {
        return enclosingStatement(getCursor());
    }

    /**
     * @return The first enclosing statement on its own line, used for continuation indenting.
     */
    @Nullable
    private J enclosingStatement(Cursor cursor) {
        cursor = cursor.getParent();
        while (cursor != null) {
            J tree = cursor.getValue();
            if (tree.getPrefix().getWhitespace().contains("\n") &&
                    (tree instanceof Statement || tree instanceof Expression)) {
                return tree;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private Space continuationIndent(Space space, @Nullable Tree parent) {
        return indent(space, parent, style.getContinuationIndent());
    }

    private Space indent(Space space, @Nullable Tree parent, int indentSize) {
        if (parent == null || !space.getWhitespace().contains("\n")) {
            return space;
        }

        int parentIndent = indent(((J) parent).getPrefix());
        int indent = indent(space);

        if (indent != parentIndent + indentSize) {
            int shift = indentSize + parentIndent - indent;
            space = space.withComments(ListUtils.map(space.getComments(), c ->
                    indentComment(c, shift)));
            space = space.withWhitespace(indent(space.getWhitespace(), shift));
        }
        return space;
    }

    private Space alignToParentStatement(Space space, Cursor parent) {
        J alignTo = parent.getParentOrThrow().getValue() instanceof J.Label ?
                parent.getParentOrThrow().getValue() :
                parent.getValue();

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

        if (indent != column) {
            int shift = column - indent;
            space = space.withComments(ListUtils.map(space.getComments(), c ->
                    indentComment(c, shift)));
            space = space.withWhitespace(indent(space.getWhitespace(), shift));
        }

        return space;
    }

    private Comment indentComment(Comment comment, int shift) {
        StringBuilder newSuffix = new StringBuilder(comment.getSuffix());
        shift(newSuffix, shift);

        String newText = comment.getText();
        if (comment.getStyle() != Comment.Style.LINE) {
            StringBuilder newTextBuilder = new StringBuilder();
            for (char c : comment.getText().toCharArray()) {
                newTextBuilder.append(c);
                if (c == '\n') {
                    shift(newTextBuilder, shift);
                }
            }
            newText = newTextBuilder.toString();
        }

        return comment.withText(newText).withSuffix(newSuffix.toString());
    }

    private String indent(String whitespace, int shift) {
        StringBuilder newWhitespace = new StringBuilder(whitespace);
        shift(newWhitespace, shift);
        return newWhitespace.toString();
    }

    private void shift(StringBuilder text, int shift) {
        int tabIndent = style.getTabSize();
        if (!style.isUseTabCharacter()) {
            tabIndent = Integer.MAX_VALUE;
        }

        if (shift > 0) {
            for (int i = 0; i < shift / tabIndent; i++) {
                text.append('\t');
            }

            for (int i = 0; i < shift % tabIndent; i++) {
                text.append(' ');
            }
        } else {
            if (style.isUseTabCharacter()) {
                text.delete(text.length() + (shift / tabIndent), text.length());
            } else {
                text.delete(text.length() + shift, text.length());
            }
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
