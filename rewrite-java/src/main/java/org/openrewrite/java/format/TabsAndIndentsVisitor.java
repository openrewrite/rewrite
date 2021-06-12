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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.Optional;

class TabsAndIndentsVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final TabsAndIndentsStyle style;
    private final String spacesForTab;

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style) {
        this(style, null);
    }

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < style.getTabSize(); i++) {
            s.append(' ');
        }
        spacesForTab = s.toString();
    }

    @Override
    @Nullable
    public J visit(@Nullable Tree tree, P p, Cursor parent) {
        setCursor(parent);
        for (Cursor c = parent; c != null; c = c.getParent()) {
            Object v = c.getValue();
            Space space = null;
            if (v instanceof J) {
                space = ((J) v).getPrefix();
            } else if (v instanceof JRightPadded) {
                space = ((JRightPadded<?>) v).getAfter();
            } else if (v instanceof JLeftPadded) {
                space = ((JLeftPadded<?>) v).getBefore();
            } else if (v instanceof JContainer) {
                space = ((JContainer<?>) v).getBefore();
            }

            if (space != null && space.getLastWhitespace().contains("\n")) {
                int indent = findIndent(space);
                if (indent != 0) {
                    c.putMessage("lastIndent", indent);
                }
            }
        }
        preVisit((J) parent.getPath(J.class::isInstance).next(), p);
        return visit(tree, p);
    }

    @Override
    @Nullable
    public J preVisit(J tree, P p) {
        if (tree instanceof J.CompilationUnit ||
                tree instanceof J.Package ||
                tree instanceof J.Import ||
                tree instanceof J.Label ||
                tree instanceof J.DoWhileLoop ||
                tree instanceof J.ArrayDimension ||
                tree instanceof J.ClassDeclaration) {
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

        return super.preVisit(tree, p);
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
    public Space visitSpace(Space space, Space.Location loc, P p) {
        boolean alignToAnnotation = false;
        Cursor parent = getCursor().getParent();
        if (parent != null && parent.getValue() instanceof J.Annotation) {
            parent.getParentOrThrow().putMessage("afterAnnotation", true);
        } else if (parent != null && !getCursor().getParentOrThrow().getPath(J.Annotation.class::isInstance).hasNext()) {
            // when annotations are on their own line, other parts of the declaration that follow are aligned left to it
            alignToAnnotation = getCursor().pollNearestMessage("afterAnnotation") != null &&
                    !(getCursor().getParentOrThrow().getValue() instanceof J.Annotation);
        }

        if (!space.getLastWhitespace().contains("\n") || parent == null) {
            return space;
        }

        int indent = Optional.ofNullable(getCursor().<Integer>getNearestMessage("lastIndent")).orElse(0);

        IndentType indentType = Optional.ofNullable(getCursor().getParentOrThrow().
                <IndentType>getNearestMessage("indentType")).orElse(IndentType.ALIGN);

        // block spaces are always aligned to their parent
        boolean alignBlockToParent = loc.equals(Space.Location.BLOCK_END) ||
                loc.equals(Space.Location.NEW_ARRAY_INITIALIZER_SUFFIX) ||
                loc.equals(Space.Location.ELSE_PREFIX);

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

        Space s = indentTo(space, indent, loc);
        if (!(getCursor().getValue() instanceof JLeftPadded) && !(getCursor().getValue() instanceof J.EnumValueSet)) {
            getCursor().putMessage("lastIndent", indent);
        }
        return s;
    }

    @Override
    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        Space after;

        int indent = Optional.ofNullable(getCursor().<Integer>getNearestMessage("lastIndent")).orElse(0);
        if (right.getElement() instanceof J) {
            J elem = (J) right.getElement();
            if ((right.getAfter().getLastWhitespace().contains("\n") ||
                    elem.getPrefix().getLastWhitespace().contains("\n"))) {
                switch (loc) {
                    case FOR_CONDITION:
                    case FOR_UPDATE: {
                        J.ForLoop.Control control = getCursor().getParentOrThrow().getValue();
                        Space initPrefix = control.getPadding().getInit().getElement().getPrefix();
                        if (!initPrefix.getLastWhitespace().contains("\n")) {
                            int initIndent = forInitColumn();
                            getCursor().getParentOrThrow().putMessage("lastIndent", initIndent - style.getContinuationIndent());
                            elem = visitAndCast(elem, p);
                            getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                            after = indentTo(right.getAfter(), initIndent, loc.getAfterLocation());
                        } else {
                            elem = visitAndCast(elem, p);
                            after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        }
                        break;
                    }
                    case METHOD_DECLARATION_PARAMETER: {
                        JContainer<J> container = getCursor().getParentOrThrow().getValue();
                        J firstArg = container.getElements().iterator().next();
                        if (firstArg.getPrefix().getWhitespace().isEmpty()) {
                            after = right.getAfter();
                        } else if (firstArg.getPrefix().getLastWhitespace().contains("\n")) {
                            // if the first argument is on its own line, align all arguments to be continuation indented
                            elem = visitAndCast(elem, p);
                            after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                        } else {
                            // align to first argument when the first argument isn't on its own line
                            int firstArgIndent = findIndent(firstArg.getPrefix());
                            getCursor().getParentOrThrow().putMessage("lastIndent", firstArgIndent);
                            elem = visitAndCast(elem, p);
                            getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                            after = indentTo(right.getAfter(), firstArgIndent, loc.getAfterLocation());
                        }
                        break;
                    }
                    case METHOD_INVOCATION_ARGUMENT:
                        elem = visitAndCast(elem, p);
                        after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                        break;
                    case NEW_CLASS_ARGUMENTS:
                    case ARRAY_INDEX:
                    case PARENTHESES:
                    case TYPE_PARAMETER: {
                        elem = visitAndCast(elem, p);
                        after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
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
                            Integer methodIndent = cursor.getNearestMessage("lastIndent");
                            if (methodIndent != null) {
                                indent = methodIndent;
                            }
                        }

                        getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                        elem = visitAndCast(elem, p);
                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        getCursor().getParentOrThrow().putMessage("lastIndent", indent + style.getContinuationIndent());
                        break;
                    }
                    case ANNOTATION_ARGUMENT:
                        JContainer<J> args = getCursor().getParentOrThrow().getValue();
                        elem = visitAndCast(elem, p);

                        // the end parentheses on an annotation is aligned to the annotation
                        if (args.getPadding().getElements().get(args.getElements().size() - 1) == right) {
                            getCursor().getParentOrThrow().putMessage("indentType", IndentType.ALIGN);
                        }

                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        break;
                    default:
                        elem = visitAndCast(elem, p);
                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                }
            } else {
                switch (loc) {
                    case NEW_CLASS_ARGUMENTS:
                    case METHOD_INVOCATION_ARGUMENT:
                        if (!elem.getPrefix().getLastWhitespace().contains("\n")) {
                            JContainer<J> args = getCursor().getParentOrThrow().getValue();
                            boolean seenArg = false;
                            boolean anyOtherArgOnOwnLine = false;
                            for (JRightPadded<J> arg : args.getPadding().getElements()) {
                                if (arg == getCursor().getValue()) {
                                    seenArg = true;
                                    continue;
                                }
                                if (seenArg) {
                                    if (arg.getElement().getPrefix().getLastWhitespace().contains("\n")) {
                                        anyOtherArgOnOwnLine = true;
                                        break;
                                    }
                                }
                            }
                            if (!anyOtherArgOnOwnLine) {
                                elem = visitAndCast(elem, p);
                                after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                                break;
                            }
                        }
                        if (!(elem instanceof J.Binary)) {
                            if (!(elem instanceof J.MethodInvocation)) {
                                getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                            } else if (elem.getPrefix().getLastWhitespace().contains("\n")) {
                                getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                            } else {
                                J.MethodInvocation methodInvocation = (J.MethodInvocation) elem;
                                Expression select = methodInvocation.getSelect();
                                if (select instanceof J.FieldAccess || select instanceof J.Identifier || select instanceof J.MethodInvocation) {
                                    getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                                }
                            }
                        }
                        elem = visitAndCast(elem, p);
                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        break;
                    default:
                        elem = visitAndCast(elem, p);
                        after = right.getAfter();
                }
            }

            //noinspection unchecked
            t = (T) elem;
        } else {
            after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        }

        setCursor(getCursor().getParent());
        return (after == right.getAfter() && t == right.getElement()) ? right : new JRightPadded<>(t, after, right.getMarkers());
    }

    @Override
    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, JContainer.Location loc, P p) {
        setCursor(new Cursor(getCursor(), container));

        Space before;
        List<JRightPadded<J2>> js;

        int indent = Optional.ofNullable(getCursor().<Integer>getNearestMessage("lastIndent")).orElse(0);
        if (container.getBefore().getLastWhitespace().contains("\n")) {
            switch (loc) {
                case TYPE_PARAMETERS:
                case IMPLEMENTS:
                case THROWS:
                case NEW_CLASS_ARGUMENTS:
                    before = indentTo(container.getBefore(), indent + style.getContinuationIndent(), loc.getBeforeLocation());
                    getCursor().putMessage("indentType", IndentType.ALIGN);
                    getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
                    break;
                default:
                    before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
            }
        } else {
            switch (loc) {
                case IMPLEMENTS:
                case METHOD_INVOCATION_ARGUMENTS:
                case NEW_CLASS_ARGUMENTS:
                case TYPE_PARAMETERS:
                case THROWS:
                    getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
                    before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
                    break;
                default:
                    before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
            }
        }

        setCursor(getCursor().getParent());
        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    private Space indentTo(Space space, int column, Space.Location spaceLocation) {
        if (!space.getLastWhitespace().contains("\n")) {
            return space;
        }
        if (style.getUseTabCharacter()) {
            space = space.withWhitespace(space.getWhitespace().replaceAll(" ", ""));
        } else {
            space = space.withWhitespace(space.getWhitespace().replaceAll("\t", ""));
        }

        if (space.getComments().isEmpty()) {
            int indent = getLengthOfWhitespace(space.getWhitespace());
            if (indent != column) {
                int shift = column - indent;
                space = space.withWhitespace(indent(space.getWhitespace(), shift));
            }
        } else {
            Comment lastElement = space.getComments().get(space.getComments().size() - 1);
            space = space.withComments(ListUtils.map(space.getComments(), c -> {
                // The suffix of the last element in the comment list sets the whitespace for the end of the block.
                // The column for comments that come before the last element are incremented.
                int incrementBy = spaceLocation.equals(Space.Location.BLOCK_END) && !c.equals(lastElement) ? style.getIndentSize() : 0;
                if (Comment.Style.LINE.equals(c.getStyle())) {
                    return indentSingleLineComment(c, column + incrementBy);
                } else {
                    return indentMultilineComment(c, column + incrementBy);
                }
            }));

            // Prevent formatting trailing comments, the whitespace in a trailing comment won't have a new line character.
            // Compilation unit prefixes are an exception, since they do not exist in a block.
            if ((space.getWhitespace().contains("\n") || space.getWhitespace().contains("\r")) || spaceLocation.equals(Space.Location.COMPILATION_UNIT_PREFIX)) {
                int indent = getLengthOfWhitespace(space.getWhitespace());
                int incrementBy = spaceLocation.equals(Space.Location.BLOCK_END) ? style.getIndentSize() : 0;
                if (indent != (column + incrementBy)) {
                    int shift = column + incrementBy - indent;
                    space = space.withWhitespace(indent(space.getWhitespace(), shift));
                }
            }
        }

        return space;
    }

    private Comment indentSingleLineComment(Comment comment, int column) {
        int indent = getLengthOfWhitespace(Space.format(comment.getSuffix()).getWhitespace());
        if (column == indent) {
            return comment;
        }

        StringBuilder newSuffix = new StringBuilder(comment.getSuffix());
        int shift = column - indent;
        shift(newSuffix, shift);
        return comment.withSuffix(newSuffix.toString());
    }

    /**
     * Aligns JavaDoc and Block comments to the column.
     *
     * The text for a JavaDoc/Block comment is a string delimited by `\n` or `\r'.
     * The whitespace and text are built separately to apply the appropriate shifts.
     *
     * isFirstLine is used to maintain the formatting for text that is in line with `/**` or `/*`.
     */
    private Comment indentMultilineComment(Comment comment, int column) {
        StringBuilder newTextBuilder = new StringBuilder();
        StringBuilder currentText = new StringBuilder();
        StringBuilder whiteSpace = new StringBuilder();
        boolean hasChanged = false;
        boolean isWhitespace = true;
        boolean isFirstLine = true;
        for (char c : comment.getText().toCharArray()) {
            switch (c) {
                case ' ':
                    if (!isFirstLine && isWhitespace) {
                        whiteSpace.append(c);
                    } else {
                        currentText.append(c);
                    }
                    break;

                case '\r':
                case '\n':
                    if (isFirstLine) {
                        isFirstLine = false;
                    } else {
                        int indent = getLengthOfWhitespace(whiteSpace.toString());
                        if (indent != column) {
                            int shift = column - indent;
                            shift(whiteSpace, shift);
                            hasChanged = true;
                        }
                    }

                    newTextBuilder.append(whiteSpace.append(currentText));
                    whiteSpace.setLength(0);
                    currentText.setLength(0);

                    whiteSpace.append(c);
                    isWhitespace = true;
                    break;

                case '*':
                    if (!isFirstLine && isWhitespace) {
                        // Moves a space character to the current text, so that the '*' are in line with each other.
                        int whitespaceLength = Math.max(whiteSpace.length() - 1, 0);
                        whiteSpace.setLength(whitespaceLength);
                        currentText.append(' ');
                    }

                default:
                    if (!isFirstLine && isWhitespace) {
                        isWhitespace = false;
                    }
                    currentText.append(c);
                    break;
            }
        }

        int indent = getLengthOfWhitespace(whiteSpace.toString());
        if (!isFirstLine) {
            if (indent != column) {
                int shift = column - indent;
                // Adjust the shift so that the */ will line up under /*.
                if (currentText.length() == 0) {
                    shift += 1;
                }
                shift(whiteSpace, shift);
                hasChanged = true;
            }
        }

        StringBuilder newSuffix = new StringBuilder(comment.getSuffix());
        indent = getLengthOfWhitespace(newSuffix.toString());
        if (indent != column && ((newSuffix.toString().contains("\n") || newSuffix.toString().contains("\r")))) {
            int shift = column - indent;
            shift(newSuffix, shift);
            hasChanged = true;
        }

        if (!hasChanged) {
            return comment;
        }

        String newText = newTextBuilder.append(whiteSpace.append(currentText)).toString();
        return comment.withText(newText).withSuffix(newSuffix.toString());
    }

    private String indent(String whitespace, int shift) {
        if (!style.getUseTabCharacter() && whitespace.contains("\t")) {
            whitespace = whitespace.replaceAll("\t", spacesForTab);
        }
        StringBuilder newWhitespace = new StringBuilder(whitespace);
        shift(newWhitespace, shift);
        return newWhitespace.toString();
    }

    private void shift(StringBuilder text, int shift) {
        int tabIndent = style.getTabSize();
        if (!style.getUseTabCharacter()) {
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
            int len;
            if (style.getUseTabCharacter()) {
                len = text.length() + (shift / tabIndent);
            } else {
                len = text.length() + shift;
            }
            if (len >= 0) {
                text.delete(len, text.length());
            }
        }
    }

    private int findIndent(Space space) {
        String indent = space.getIndent();
        return getLengthOfWhitespace(indent);
    }

    private int getLengthOfWhitespace(@Nullable String whitespace) {
        if (whitespace == null) {
            return 0;
        }

        int size = 0;
        for (char c : whitespace.toCharArray()) {
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
        @SuppressWarnings("ConstantConditions") J alignTo = parent instanceof J.Label ?
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

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }

    private enum IndentType {
        ALIGN,
        INDENT,
        CONTINUATION_INDENT
    }
}
