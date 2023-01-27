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
package org.openrewrite.hcl.format;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.style.TabsAndIndentsStyle;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public class TabsAndIndentsVisitor<P> extends HclIsoVisitor<P> {
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
    public Hcl visit(@Nullable Tree tree, P p, Cursor parent) {
        setCursor(parent);
        for (Cursor c = parent; c != null; c = c.getParent()) {
            Object v = c.getValue();
            Space space = null;
            if (v instanceof Hcl) {
                space = ((Hcl) v).getPrefix();
            } else if (v instanceof HclRightPadded) {
                space = ((HclRightPadded<?>) v).getAfter();
            } else if (v instanceof HclLeftPadded) {
                space = ((HclLeftPadded<?>) v).getBefore();
            } else if (v instanceof HclContainer) {
                space = ((HclContainer<?>) v).getBefore();
            }

            if (space != null && space.getLastWhitespace().contains("\n")) {
                int indent = findIndent(space);
                if (indent != 0) {
                    c.putMessage("lastIndent", indent);
                }
            }
        }
        preVisit((Hcl) parent.getPath(Hcl.class::isInstance).next(), p);
        return visit(tree, p);
    }

    @Override
    @Nullable
    public Hcl preVisit(Hcl tree, P p) {
        if (tree instanceof Hcl.Block || tree instanceof Hcl.ObjectValue) {
            getCursor().putMessage("indentType", IndentType.INDENT);
        } else {
            getCursor().putMessage("indentType", IndentType.ALIGN);
        }

        return super.preVisit(tree, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        Cursor parent = getCursor().getParent();

        if (!space.getLastWhitespace().contains("\n") || parent == null) {
            return space;
        }

        int indent = getCursor().getNearestMessage("lastIndent", 0);

        IndentType indentType = getCursor().getParentOrThrow().getNearestMessage("indentType", IndentType.ALIGN);

        // block spaces are always aligned to their parent
        boolean alignBlockToParent = loc.equals(Space.Location.BLOCK_CLOSE)|| loc.equals(Space.Location.OBJECT_VALUE_ATTRIBUTE_SUFFIX);

        if (alignBlockToParent) {
            indentType = IndentType.ALIGN;
        }

        switch (indentType) {
            case ALIGN:
                break;
            case INDENT:
                indent += style.getIndentSize();
                break;
        }

        Space s = indentTo(space, indent, loc);
        if (!(getCursor().getValue() instanceof HclLeftPadded)) {
            getCursor().putMessage("lastIndent", indent);
        }
        return s;
    }

    @Override
    public <T> HclRightPadded<T> visitRightPadded(@Nullable HclRightPadded<T> right, HclRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        Space after;

        int indent = getCursor().getNearestMessage("lastIndent", 0);
        if (right.getElement() instanceof Hcl) {
            Hcl elem = (Hcl) right.getElement();
            if ((right.getAfter().getLastWhitespace().contains("\n") ||
                    elem.getPrefix().getLastWhitespace().contains("\n"))) {
                switch (loc) {
                    case FUNCTION_CALL_ARGUMENT:
                    case PARENTHESES: {
                        elem = visitAndCast(elem, p);
                        after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                        break;
                    }
                    default:
                        elem = visitAndCast(elem, p);
                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                }
            } else {
                switch (loc) {
                    case FUNCTION_CALL_ARGUMENT:
                        if (!elem.getPrefix().getLastWhitespace().contains("\n")) {
                            HclContainer<Expression> args = getCursor().getParentOrThrow().getValue();
                            boolean seenArg = false;
                            boolean anyOtherArgOnOwnLine = false;
                            for (HclRightPadded<Expression> arg : args.getPadding().getElements()) {
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
        return (after == right.getAfter() && t == right.getElement()) ? right : new HclRightPadded<>(t, after, right.getMarkers());
    }

    @Override
    public <H extends Hcl> HclContainer<H> visitContainer(HclContainer<H> container, HclContainer.Location loc, P p) {
        setCursor(new Cursor(getCursor(), container));

        Space before;
        List<HclRightPadded<H>> js;

        int indent = getCursor().getNearestMessage("lastIndent", 0);
        if (container.getBefore().getLastWhitespace().contains("\n")) {
            if (loc == HclContainer.Location.FUNCTION_CALL_ARGUMENTS) {
                before = indentTo(container.getBefore(), indent + style.getIndentSize(), loc.getBeforeLocation());
                getCursor().putMessage("indentType", IndentType.ALIGN);
                getCursor().putMessage("lastIndent", indent + style.getIndentSize());
                js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
            } else {
                before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
            }
        } else {
            if (loc == HclContainer.Location.FUNCTION_CALL_ARGUMENTS) {
                getCursor().putMessage("indentType", IndentType.INDENT);
            }
            before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
            js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
        }

        setCursor(getCursor().getParent());
        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                HclContainer.build(before, js, container.getMarkers());
    }


    @Override
    public Hcl.Attribute visitAttribute(final Hcl.Attribute attribute, final P p) {
        Hcl.Attribute a = attribute;
        if (attribute.getComma() != null) {
            a =  attribute.withComma(attribute.getComma().withPrefix(Space.EMPTY));
        }
        return super.visitAttribute(a, p);
    }

    private Space indentTo(Space space, int column, Space.Location spaceLocation) {
        if (!space.getLastWhitespace().contains("\n")) {
            return space;
        }

        if (space.getComments().isEmpty()) {
            int indent = findIndent(space);
            if (indent != column) {
                int shift = column - indent;
                space = space.withWhitespace(indent(space.getWhitespace(), shift));
            }
        } else {
            if (!StringUtils.isNullOrEmpty(space.getWhitespace()) &&
                    // Preserve whitespace of trailing line comments.
                    (Comment.Style.INLINE.equals(space.getComments().get(0).getStyle()) ||
                            (!Comment.Style.INLINE.equals(space.getComments().get(0).getStyle()) &&
                                    (space.getWhitespace().contains("\n") || space.getWhitespace().contains("\r"))))) {
                if (style.getUseTabCharacter()) {
                    space = space.withWhitespace(space.getWhitespace().replaceAll(" ", ""));
                } else {
                    space = space.withWhitespace(space.getWhitespace().replaceAll("\t", ""));
                }
            }

            Comment lastElement = space.getComments().get(space.getComments().size() - 1);
            space = space.withComments(ListUtils.map(space.getComments(), c -> {
                // The suffix of the last element in the comment list sets the whitespace for the end of the block.
                // The column for comments that come before the last element are incremented.
                int incrementBy = spaceLocation.equals(Space.Location.BLOCK_CLOSE) && !c.equals(lastElement) ? style.getIndentSize() : 0;
                return c.getStyle() == Comment.Style.INLINE ?
                        indentMultilineComment(c, column + incrementBy) :
                        indentSingleLineComment(c, column + incrementBy);
            }));

            // Prevent formatting trailing comments, the whitespace in a trailing comment won't have a new line character.
            // Compilation unit prefixes are an exception, since they do not exist in a block.
            if (space.getWhitespace().contains("\n") || spaceLocation.equals(Space.Location.CONFIG_FILE)) {
                int incrementBy = spaceLocation.equals(Space.Location.BLOCK_CLOSE) ? style.getIndentSize() : 0;
                int indent = getLengthOfWhitespace(space.getWhitespace());
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
     * Normalizes the whitespace in JavaDoc and Block style comments.
     * Text for a JavaDoc/Block comment is a string delimited by `\n` or `\r'.
     * Whitespace and text are built separately to apply the appropriate shifts to the whitespace.
     * The length of the whitespace in a block comment will be preserved if any of the lines do not start with a *.
     * <p>
     * indentSize:
     * - determines the number of spaces each indent is equivalent to.
     * <p>
     * tabSize:
     * - is only applicable if useTabCharacter is true.
     * - tabSize sets the number of spaces each tab character is equivalent to.
     */
    private Comment indentMultilineComment(Comment comment, int column) {
        StringBuilder newTextBuilder = new StringBuilder();
        StringBuilder currentText = new StringBuilder();
        StringBuilder whitespace = new StringBuilder();

        boolean hasChanged = false; // Preserves referential equality if no changes are necessary in the comment.
        boolean isWhitespace = true; // Determines where whitespace starts and ends.
        boolean isFirstLine = true;  // Preserves whitespace if it immediately follows the comment prefix /* or /**.
        int indent = 0; // Track the indent of the current line in the block comment.
        int tabLength = 0; // Only applies to BLOCK style comments that are not being aligned. Track the current tabLength to normalize and preserve whitespace.

        boolean alignToColumn = shouldAlignBlockComment(comment);
        char prev = '$';
        for (int i = 0; i < comment.getText().length(); ++i) {
            char c = comment.getText().charAt(i);
            switch (c) {
                case '\t':
                    if (!isFirstLine && isWhitespace) {
                        // Normalizes the whitespace char to match the `style`.
                        // The character count is updated appropriately in `shift()` after a new line char is found.
                        if (style.getUseTabCharacter()) {
                            whitespace.append(c);
                            indent += style.getTabSize();
                        } else {
                            /* Visualization of conversion for style.getTabSize() == 4:
                             * s s s t => s s s s
                             * s s t   => s s s s
                             * s t     => s s s s
                             * t       => s s s s
                             */
                            for (int j = tabLength % style.getIndentSize(); j < style.getIndentSize(); ++j) {
                                whitespace.append(' ');
                                indent++;
                                hasChanged = true;
                            }
                        }
                        tabLength = 0;
                    } else {
                        currentText.append(c);
                    }
                    break;

                case ' ':
                    if (!isFirstLine && isWhitespace) {
                        // Normalizes the whitespace char to match the `style`.
                        // The character count is updated appropriately in `shift()` after a new line char is found.
                        if (style.getUseTabCharacter()) {
                            tabLength++;
                            // Convert the previous spaces to a tab once the tabSize is reached.
                            if (tabLength == style.getTabSize()) {
                                if (!alignToColumn || (i + 1 < comment.getText().length() - 1 && comment.getText().charAt(i + 1) != '*')) {
                                    whitespace.append('\t');
                                    indent += style.getTabSize();
                                    tabLength = 0;
                                    hasChanged = true;
                                }
                            }
                        } else {
                            whitespace.append(c);
                            indent++;
                            tabLength++;
                        }

                        if (tabLength == (style.getUseTabCharacter() ? style.getTabSize() : style.getIndentSize())) {
                            tabLength = 0;
                        }
                    } else {
                        currentText.append(c);
                    }
                    break;

                case '\r':
                    // Check for Windows OS CRLF \r\n.
                    if ((i + 1 <= comment.getText().length() - 1) && comment.getText().charAt(i + 1) == '\n') {
                        whitespace.append(c);
                        continue;
                    }

                case '\n':
                    if (isFirstLine) {
                        isFirstLine = false;
                    } else {
                        if (alignToColumn && indent != column) {
                            int shift = column - indent;
                            shift(whitespace, shift);
                            hasChanged = true;
                        }
                    }

                    newTextBuilder.append(whitespace.append(currentText));
                    whitespace.setLength(0);
                    currentText.setLength(0);
                    indent = 0;
                    tabLength = 0;

                    whitespace.append(c);
                    isWhitespace = true;
                    break;

                case '*':
                    if (alignToColumn && !isFirstLine && isWhitespace) {
                        // Moves a space character from whitespace to the current text,
                        // so that the '*' in blocks comments are in line with each other.
                        if (style.getUseTabCharacter()) {
                            if (prev != ' ') {
                                hasChanged = true;
                            }
                        } else {
                            if (whitespace.length() <= 1) {
                                hasChanged = true;
                            } else {
                                whitespace.setLength(whitespace.length() - 1);
                                indent--;
                            }
                        }
                        currentText.append(' ');
                    }

                default:
                    if (!isFirstLine && isWhitespace) {
                        isWhitespace = false;
                    }
                    currentText.append(c);
                    break;
            }
            prev = c;
        }

        // Process the final whitespace in the comment.
        if (!isFirstLine) {
            int incrementBy = !style.getUseTabCharacter() && currentText.length() == 0 ? 1 : 0;
            if (alignToColumn && indent != column + incrementBy) {
                int shift = column - indent;
                shift(whitespace, shift);
                hasChanged = true;
            }
            // The currentText will be length 0 if the comment ends with a `*/` on a new line.
            if (currentText.length() == 0) {
                // Add a space to the whitespace that precedes the `*/` so that it is aligned to the prefix `*/`.
                if (style.getUseTabCharacter()) {
                    if (whitespace.charAt(whitespace.length() - 1) != ' ') {
                        whitespace.append(' ');
                        if (!hasChanged && prev != ' ') {
                            hasChanged = true;
                        }
                    }
                } else {
                    if (whitespace.length() - 1 == column) {
                        whitespace.append(' ');
                        hasChanged = true;
                    }
                }
            }
        }

        // Normalizes the whitespace in the suffix to match the `style`.
        // The suffix of the comment is the whitespace that precedes the AST element.
        String suffix = null;
        if (style.getUseTabCharacter()) {
            if (comment.getSuffix().contains(" ")) {
                suffix = comment.getSuffix().replace(" ", "\t");
                hasChanged = true;
            }
        } else {
            if (comment.getSuffix().contains("\t")) {
                suffix = comment.getSuffix().replace("\t", " ");
                hasChanged = true;
            }
        }

        suffix = suffix == null ? comment.getSuffix() : suffix;

        StringBuilder newSuffix = new StringBuilder(suffix);
        indent = getLengthOfWhitespace(newSuffix.toString());
        if (indent != column && newSuffix.toString().contains("\n")) {
            int shift = column - indent;
            shift(newSuffix, shift);
            hasChanged = true;
        }

        if (!hasChanged) {
            return comment;
        }

        String newText = newTextBuilder.append(whitespace.append(currentText)).toString();
        return comment.withText(newText).withSuffix(newSuffix.toString());
    }

    /* Block comments may be aligned to the column if all the lines start with an asterisk.
     * Returns false if any line doesn't start with a *.
     */
    private boolean shouldAlignBlockComment(Comment comment) {
        boolean alignComment = true;
        boolean isFirstLine = true;
        boolean isWhitespace = true;

        OUTER:
        for (char c : comment.getText().toCharArray()) {
            switch (c) {
                case '\t':
                case ' ':
                    break;

                case '\n':
                case '\r':
                    if (isFirstLine) {
                        isFirstLine = false;
                    }
                    isWhitespace = true;
                    break;

                default:
                    if (!isFirstLine && isWhitespace) {
                        isWhitespace = false;
                        if (c != '*') {
                            alignComment = false;
                            break OUTER;
                        }
                    }
                    break;
            }
        }
        return alignComment;
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

    @Nullable
    @Override
    public Hcl postVisit(Hcl tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Hcl.ConfigFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public Hcl visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Hcl) tree;
        }
        return super.visit(tree, p);
    }

    private enum IndentType {
        ALIGN,
        INDENT,
    }
}
