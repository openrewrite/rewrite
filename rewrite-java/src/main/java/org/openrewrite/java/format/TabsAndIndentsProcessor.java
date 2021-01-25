package org.openrewrite.java.format;

import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.Optional;

public class TabsAndIndentsProcessor<P> extends JavaIsoProcessor<P> {
    private final TabsAndIndentsStyle style;

    public TabsAndIndentsProcessor(TabsAndIndentsStyle style) {
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
                tree instanceof J.ArrayDimension) {
            getCursor().putMessage("indentType", IndentType.ALIGN);
        } else if (tree instanceof J.Block ||
                tree instanceof J.If ||
                tree instanceof J.If.Else ||
                tree instanceof J.ForLoop ||
                tree instanceof J.ForEachLoop ||
                tree instanceof J.WhileLoop ||
                tree instanceof J.Case) {
            getCursor().putMessage("indentType", IndentType.INDENT);
        } else {
            getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
        }

        if (tree instanceof J.Annotation) {
            getCursor().getParentOrThrow().putMessage("afterAnnotation", true);
        }

        return super.visitEach(tree, p);
    }

    @Override
    public Space visitSpace(Space space, P p) {
        if (!space.getWhitespace().contains("\n") || getCursor().getParent() == null) {
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
        if(!(getCursor().getValue() instanceof JLeftPadded)) {
            getCursor().putMessage("lastIndent", indent);
        }
        return s;
    }

    @Override
    public <J2 extends J> JRightPadded<J2> visitRightPadded(JRightPadded<J2> right, JRightPadded.Location type, P p) {
        setCursor(new Cursor(getCursor(), right));

        J2 j;
        Space after;

        int indent = Optional.ofNullable(getCursor().<Integer>peekNearestMessage("lastIndent")).orElse(0);

        switch (type) {
            case FOR_CONDITION:
            case FOR_INIT:
            case FOREACH_VARIABLE:
                getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
                j = call(right.getElem(), p);
                getCursor().putMessage("indentType", IndentType.INDENT);
                after = indentTo(right.getAfter(), indent + style.getContinuationIndent());
                break;
            case FOR_UPDATE:
            case FOREACH_ITERABLE:
                getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
                j = call(right.getElem(), p);
                getCursor().putMessage("indentType", IndentType.INDENT);
                after = indentTo(right.getAfter(), indent);
                break;
            case ARRAY_INDEX:
            case METHOD_DECL_ARGUMENT:
            case NEW_CLASS_ARGS:
            case PARENTHESES:
            case TYPE_PARAMETER:
                j = call(right.getElem(), p);
                after = indentTo(right.getAfter(), indent);
                break;
            case METHOD_INVOCATION_ARGUMENT:
                getCursor().getParentOrThrow().putMessage("lastIndent", indent + style.getContinuationIndent());
                j = call(right.getElem(), p);
                getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                after = visitSpace(right.getAfter(), p);
                break;
            case ANNOTATION_ARGUMENT:
            case BLOCK_STATEMENT:
            case CASE:
            case CATCH_ALTERNATIVE:
            case ENUM_VALUE:
            case FOR_BODY:
            case IF_ELSE:
            case IF_THEN:
            case IMPLEMENTS:
            case IMPORT:
            case INSTANCEOF:
            case NAMED_VARIABLE:
            case NEW_ARRAY_INITIALIZER:
            case THROWS:
            case TRY_RESOURCES:
            case TYPE_BOUND:
            case WHILE_BODY:
            default:
            case METHOD_SELECT:
            case PACKAGE:
                j = call(right.getElem(), p);
                after = visitSpace(right.getAfter(), p);
        }

        setCursor(getCursor().getParent());
        return (after == right.getAfter() && j == right.getElem()) ? right : new JRightPadded<>(j, after, right.getMarkers());
    }

    @Override
    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, JContainer.Location loc, P p) {
        setCursor(new Cursor(getCursor(), container));
        Space before = visitSpace(container.getBefore(), p);
        switch (loc) {
            case IMPLEMENTS:
            case THROWS:
                getCursor().putMessage("indentType", IndentType.ALIGN);
                break;
        }

        List<JRightPadded<J2>> js = ListUtils.map(container.getElem(), t -> visitRightPadded(t, loc.getElemLocation(), p));
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

    private enum IndentType {
        ALIGN,
        INDENT,
        CONTINUATION_INDENT
    }
}
