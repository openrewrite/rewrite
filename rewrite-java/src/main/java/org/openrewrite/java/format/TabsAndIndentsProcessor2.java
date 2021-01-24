package org.openrewrite.java.format;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;

public class TabsAndIndentsProcessor2<P> extends JavaIsoProcessor<P> {
    private final TabsAndIndentsStyle style;

    public TabsAndIndentsProcessor2(TabsAndIndentsStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
        int align = findIndent(parent.getPrefix());

        J.Block b = block;

        if (b.getPrefix().getWhitespace().contains("\n")) {
            b = b.withPrefix(indentTo(b.getPrefix(), align));
        }
        b = b.withEnd(indentTo(b.getEnd(), align));

        return super.visitBlock(b, p);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public Space visitSpace(Space space, P p) {
        if (!space.getWhitespace().contains("\n")) {
            return space;
        }

        int indent = findIndent(space);
        Integer lastIndent = getCursor().peekMessage("lastIndent");
        if (lastIndent == null) {
            lastIndent = 0;
        }

        Object value = getCursor().getValue();
        Object parent = getCursor().getParentOrThrow().getValue();
        Object grandparent = getCursor().getParentOrThrow().getParentOrThrow().getValue();
        if (value instanceof J.Block) {
            // handled in visitBlock
        } else if (parent instanceof JRightPadded) {
            // TODO if method argument, maybe align to method argument
            if (grandparent instanceof J.Block || grandparent instanceof J.If ||
                    grandparent instanceof J.If.Else || grandparent instanceof J.ForLoop ||
                    grandparent instanceof J.ForEachLoop || grandparent instanceof J.WhileLoop ||
                    grandparent instanceof J.DoWhileLoop) {
                // We are in a single statement child of a for loop, if statement, etc. where the
                // control statement has no block. Every other control statement requires a
                // block (e.g. try, switch, synchronized)
                indent = lastIndent + style.getIndentSize();
            }
        } else {
            indent = lastIndent + style.getContinuationIndent();
        }

        Space s = indentTo(space, indent);
        getCursor().putMessage("lastIndent", indent);
        return s;
    }

    private Space indentTo(Space space, int column) {
        if (!space.getWhitespace().contains("\n")) {
            return space;
        }

        int indent = findIndent(space);

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
}
