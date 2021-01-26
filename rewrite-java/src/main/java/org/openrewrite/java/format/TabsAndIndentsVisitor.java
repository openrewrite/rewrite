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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.Optional;

class TabsAndIndentsVisitor<P> extends JavaIsoVisitor<P> {
    private final TabsAndIndentsStyle style;

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public @Nullable J visitEach(J tree, P p) {
        if (tree instanceof J.CompilationUnit ||
                tree instanceof J.Package ||
                tree instanceof J.Import ||
                tree instanceof J.Label ||
                tree instanceof J.DoWhileLoop ||
                tree instanceof J.ArrayDimension ||
                tree instanceof J.ClassDecl) {
            getCursor().putMessage("indentType", IndentType.ALIGN);
        } else if (tree instanceof J.Block ||
                tree instanceof J.If ||
                tree instanceof J.If.Else ||
                tree instanceof J.ForLoop ||
                tree instanceof J.ForEachLoop ||
                tree instanceof J.WhileLoop ||
                tree instanceof J.Case ||
                tree instanceof J.EnumValueSet) {
            getCursor().putMessage("indentType", IndentType.INDENT);
        } else {
            getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
        }

        return super.visitEach(tree, p);
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, P p) {

        return super.visitMethod(method, p);
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, P p) {
        // FIXME fix formatting of control sections
        return control;
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, P p) {
        // FIXME fix formatting of control sections
        return control;
    }

    @Override
    public Space visitSpace(Space space, P p) {
        Cursor parent = getCursor().getParent();
        if (parent != null && parent.getValue() instanceof J.Annotation) {
            parent.getParentOrThrow().putMessage("afterAnnotation", true);
        }

        if (!space.getWhitespace().contains("\n") || parent == null) {
            return space;
        }

        int indent = Optional.ofNullable(getCursor().<Integer>peekNearestMessage("lastIndent")).orElse(0);

        IndentType indentType = Optional.ofNullable(getCursor().getParentOrThrow().
                <IndentType>peekNearestMessage("indentType")).orElse(IndentType.ALIGN);

        // block spaces are always aligned to their parent
        boolean alignBlockToParent = getCursor().getValue() instanceof J.Block;

        // when annotations are on their own line, other parts of the declaration that follow are aligned left to it
        boolean alignToAnnotation = getCursor().pollNearestMessage("afterAnnotation") != null &&
                !(getCursor().getParentOrThrow().getValue() instanceof J.Annotation);

        if (alignBlockToParent || alignToAnnotation) {
            indentType = IndentType.ALIGN;
        }

        switch (indentType) {
            case ALIGN:
                break;
            case INDENT:
                indent += style.getIndentSize();
                break;
            case CONTINUATION_INDENT:
                indent += style.getContinuationIndent();
                break;
        }

        Space s = indentTo(space, indent);
        if (!(getCursor().getValue() instanceof JLeftPadded)) {
            getCursor().putMessage("lastIndent", indent);
        }
        return s;
    }

    @Override
    public <J2 extends J> JRightPadded<J2> visitRightPadded(JRightPadded<J2> right, JRightPadded.Location loc, P p) {
        setCursor(new Cursor(getCursor(), right));

        J2 j;
        Space after;

        int indent = Optional.ofNullable(getCursor().<Integer>peekNearestMessage("lastIndent")).orElse(0);
        if (right.getAfter().getWhitespace().contains("\n") || right.getElem().getPrefix().getWhitespace().contains("\n")) {
            switch (loc) {
                case FOR_CONDITION:
                case FOR_UPDATE: {
                    J.ForLoop.Control control = getCursor().getParentOrThrow().getValue();
                    Space initPrefix = control.getInit().getElem().getPrefix();
                    if (!initPrefix.getWhitespace().contains("\n")) {
                        int initIndent = forInitColumn();
                        getCursor().getParentOrThrow().putMessage("lastIndent", initIndent - style.getContinuationIndent());
                        j = visitAndCast(right.getElem(), p);
                        getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                        after = indentTo(right.getAfter(), initIndent);
                    } else {
                        j = visitAndCast(right.getElem(), p);
                        after = visitSpace(right.getAfter(), p);
                    }
                    break;
                }
                case METHOD_DECL_ARGUMENT: {
                    JContainer<Expression> container = getCursor().getParentOrThrow().getValue();
                    Expression firstArg = container.getElem().iterator().next().getElem();
                    if (firstArg.getPrefix().getWhitespace().contains("\n")) {
                        // if the first argument is on its own line, align all arguments to be continuation indented
                        j = visitAndCast(right.getElem(), p);
                        after = indentTo(right.getAfter(), indent);
                    } else {
                        // align to first argument when the first argument isn't on its own line
                        int firstArgIndent = findIndent(firstArg.getPrefix());
                        getCursor().getParentOrThrow().putMessage("lastIndent", firstArgIndent);
                        j = visitAndCast(right.getElem(), p);
                        getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                        after = indentTo(right.getAfter(), firstArgIndent);
                    }
                    break;
                }
                case ARRAY_INDEX:
                case METHOD_INVOCATION_ARGUMENT:
                case NEW_CLASS_ARGS:
                case PARENTHESES:
                case TYPE_PARAMETER: {
                    j = visitAndCast(right.getElem(), p);
                    after = indentTo(right.getAfter(), indent);
                    break;
                }
                case METHOD_SELECT: {
                    for (Cursor cursor = getCursor(); ; cursor = cursor.getParentOrThrow()) {
                        if (cursor.getValue() instanceof JRightPadded) {
                            cursor = cursor.getParentOrThrow();
                        }
                        if (!(cursor.getValue() instanceof J.MethodInvocation)) {
                            break;
                        }
                        Integer methodIndent = cursor.peekNearestMessage("lastIndent");
                        if (methodIndent != null) {
                            indent = methodIndent;
                        }
                    }

                    getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                    j = visitAndCast(right.getElem(), p);
                    after = visitSpace(right.getAfter(), p);
                    getCursor().getParentOrThrow().putMessage("lastIndent", indent + style.getContinuationIndent());
                    break;
                }
                default:
                    j = visitAndCast(right.getElem(), p);
                    after = visitSpace(right.getAfter(), p);
            }
        } else {
            switch (loc) {
                case NEW_CLASS_ARGS:
                case METHOD_INVOCATION_ARGUMENT:
                    getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                    j = visitAndCast(right.getElem(), p);
                    after = visitSpace(right.getAfter(), p);
                    break;
                default:
                    j = visitAndCast(right.getElem(), p);
                    after = right.getAfter();
            }
        }

        setCursor(getCursor().getParent());
        return (after == right.getAfter() && j == right.getElem()) ? right : new JRightPadded<>(j, after, right.getMarkers());
    }

    @Override
    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, JContainer.Location loc, P p) {
        setCursor(new Cursor(getCursor(), container));

        Space before;
        List<JRightPadded<J2>> js;

        int indent = Optional.ofNullable(getCursor().<Integer>peekNearestMessage("lastIndent")).orElse(0);
        if (container.getBefore().getWhitespace().contains("\n")) {
            switch (loc) {
                case TYPE_PARAMETER:
                case IMPLEMENTS:
                case THROWS:
                case NEW_CLASS_ARGS:
                    before = indentTo(container.getBefore(), indent + style.getContinuationIndent());
                    getCursor().putMessage("indentType", IndentType.ALIGN);
                    getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                    js = ListUtils.map(container.getElem(), t -> visitRightPadded(t, loc.getElemLocation(), p));
                    break;
                default:
                    before = visitSpace(container.getBefore(), p);
                    js = ListUtils.map(container.getElem(), t -> visitRightPadded(t, loc.getElemLocation(), p));
            }
        } else {
            switch (loc) {
                case METHOD_INVOCATION_ARGUMENT:
                case IMPLEMENTS:
                case NEW_CLASS_ARGS:
                case TYPE_PARAMETER:
                case THROWS:
                    getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
                    before = visitSpace(container.getBefore(), p);
                    js = ListUtils.map(container.getElem(), t -> visitRightPadded(t, loc.getElemLocation(), p));
                    break;
                default:
                    before = visitSpace(container.getBefore(), p);
                    js = ListUtils.map(container.getElem(), t -> visitRightPadded(t, loc.getElemLocation(), p));
            }
        }

        setCursor(getCursor().getParent());
        return js == container.getElem() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    private Space indentTo(Space space, int column) {
        if (!space.getWhitespace().contains("\n")) {
            return space;
        }

        int indent = findIndent(space);
        if (!space.getComments().isEmpty()) {
            indent = findIndent(Space.format(space.getComments().get(space.getComments().size() - 1).getSuffix()));
        }

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

    private int findIndent(Space space) {
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

    private int forInitColumn() {
        Cursor forCursor = getCursor().dropParentUntil(J.ForLoop.class::isInstance);
        J.ForLoop forLoop = forCursor.getValue();
        Object parent = forCursor.getParentOrThrow().getValue();
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

    private enum IndentType {
        ALIGN,
        INDENT,
        CONTINUATION_INDENT
    }
}
