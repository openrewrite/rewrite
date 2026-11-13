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
import org.openrewrite.coldfusion.marker.AttributeClause;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.coldfusion.style.TabsAndIndentsStyle;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.CfContainer;
import org.openrewrite.coldfusion.tree.CfSpace;
import org.openrewrite.coldfusion.tree.TagComment;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Indents statements, tag bodies and the lines that continue them.
 * <ul>
 *     <li>A statement starting a line is indented a level from the line its block's owner starts on: a function, an
 *     {@code if}, a tag. A <code>}</code> or an end tag is aligned with that line, and so are {@code else}, {@code catch}
 *     and {@code finally} on a line of their own, and {@code <cfelse>} and {@code <cfelseif>}.</li>
 *     <li>A line that continues a statement keeps its alignment, moved as far as the statement moved. One that would
 *     then be no further in than the line it continues is indented the style's continuation indent from it. A line
 *     starting with the parenthesis, bracket or brace that closes a list is aligned with the line that opens the list,
 *     or indented like the list where the style says so.</li>
 *     <li>A template writes the whitespace between its tags, so lines there keep their indentation. Only the bodies
 *     the template is silent in are indented, and of those only bodies with tags alone: text lays out the body it is
 *     in as a page, which these rules do not know.</li>
 * </ul>
 */
public class TabsAndIndentsVisitor<P> extends FormattingVisitor<P> {
    private static final String LINE = "org.openrewrite.coldfusion.format.line";
    private static final String ANCHOR = "org.openrewrite.coldfusion.format.anchor";
    private static final String DELTA = "org.openrewrite.coldfusion.format.delta";
    private static final String TARGET = "org.openrewrite.coldfusion.format.target";
    private static final String HAS_TEXT = "org.openrewrite.coldfusion.format.hasText";

    private final TabsAndIndentsStyle style;

    /**
     * The indentation of the line being written, in columns, which an element that does not start a line is on.
     */
    private int lineIndent;

    /**
     * The indentation of the line each list being visited opens on, which what closes it is aligned with.
     */
    private final Deque<Integer> openers = new ArrayDeque<>();

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style) {
        this(style, null);
    }

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.style = style;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p, Cursor parent) {
        // the elements around a tree formatted on its own keep their lines, which it is indented from
        List<Cursor> path = new ArrayList<>();
        for (Cursor c = parent; c != null; c = c.getParent()) {
            path.add(c);
        }
        int line = 0;
        for (int i = path.size() - 1; i >= 0; i--) {
            Cursor c = path.get(i);
            if (c.getValue() instanceof J) {
                Space prefix = ((J) c.getValue()).getPrefix();
                if (Layout.hasLineBreak(prefix)) {
                    line = oldIndent(prefix);
                    c.putMessage(ANCHOR, line);
                    c.putMessage(DELTA, 0);
                }
                c.putMessage(LINE, line);
            }
        }
        lineIndent = line;
        return super.visit(tree, p, parent);
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        J j = super.preVisit(tree, p);
        if (tree instanceof Cf.ExpressionStatement) {
            // its expression holds its prefix
            return j;
        }
        Cursor cursor = getCursor();
        Space prefix = tree.getPrefix();
        if (Layout.hasLineBreak(prefix) && !isInInterpolation()) {
            int old = oldIndent(prefix);
            Integer target = prefixIndent(tree, old);
            int indent = target == null ? old : target;
            if (target != null) {
                cursor.putMessage(TARGET, target);
            }
            cursor.putMessage(ANCHOR, indent);
            cursor.putMessage(DELTA, indent - old);
            lineIndent = indent;
        }
        cursor.putMessage(LINE, lineIndent);
        return j;
    }

    @Override
    protected Space whitespace(Space space, Object loc, boolean prefix, P p) {
        if (!Layout.hasLineBreak(space)) {
            return space;
        }
        Cursor cursor = getCursor();
        if (prefix) {
            Integer target = cursor.getMessage(TARGET);
            return target == null ? space : indentTo(space, target, target);
        }
        if (isInInterpolation() || isWritten(loc, prefix)) {
            lineIndent = oldIndent(space);
            return space;
        }
        if (loc == Space.Location.BLOCK_END || loc == Space.Location.COMPILATION_UNIT_EOF) {
            Integer end = endIndent(cursor);
            if (end == null) {
                lineIndent = oldIndent(space);
                return space;
            }
            lineIndent = end;
            return indentTo(space, end, end + statementLevel(cursor));
        }
        int target;
        if (loc == Space.Location.TRY_FINALLY || loc == Space.Location.WHILE_CONDITION) {
            target = line(cursor.getParentTreeCursor());
        } else if (loc == CfSpace.Location.TAG_BEFORE_END || loc == CfSpace.Location.TAG_CLOSING_BEFORE_END) {
            target = line(cursor);
        } else {
            target = continuation(cursor, oldIndent(space));
        }
        lineIndent = target;
        return indentTo(space, target, target);
    }

    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null || !closesList(right)) {
            return super.visitRightPadded(right, loc, p);
        }
        // as the superclass visits it, but the space after the last element is before what closes the list
        setCursor(new Cursor(getCursor(), right));
        T t = right.getElement();
        if (t instanceof J) {
            t = visitAndCast((J) t, p);
        }
        setCursor(getCursor().getParent());
        if (t == null) {
            return null;
        }
        Space after = right.getAfter();
        Markers markers = right.getMarkers();
        TrailingComma trailingComma = markers.findFirst(TrailingComma.class).orElse(null);
        if (getCursor().getNearestMessage("stop") == null) {
            if (trailingComma != null && Layout.hasLineBreak(trailingComma.getSuffix())) {
                Space suffix = closing(trailingComma.getSuffix());
                if (suffix != trailingComma.getSuffix()) {
                    markers = markers.setByType(trailingComma.withSuffix(suffix));
                }
            } else if (Layout.hasLineBreak(after) && !isInInterpolation()) {
                after = closing(after);
            }
        }
        return after == right.getAfter() && t == right.getElement() && markers == right.getMarkers() ?
                right : new JRightPadded<>(t, after, markers);
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        openers.push(lineIndent);
        try {
            return super.visitContainer(container, loc, p);
        } finally {
            openers.pop();
        }
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, CfContainer.Location loc, P p) {
        openers.push(lineIndent);
        try {
            return super.visitContainer(container, loc, p);
        } finally {
            openers.pop();
        }
    }

    @Override
    public Cf.Text visitText(Cf.Text text, P p) {
        Cf.Text t = super.visitText(text, p);
        if (Layout.hasLineBreak(t.getText())) {
            lineIndent = Layout.width(Layout.lastLine(t.getText()), style.getTabSize());
        }
        return t;
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal, P p) {
        J.Literal l = super.visitLiteral(literal, p);
        String source = l.getValueSource();
        if (source != null && Layout.hasLineBreak(source)) {
            lineIndent = Layout.width(Layout.lastLine(source), style.getTabSize());
        }
        return l;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        return m.withMarkers(markers(m.getMarkers()));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        c = c.withMarkers(markers(c.getMarkers()));
        JLeftPadded<TypeTree> extendings = c.getPadding().getExtends();
        if (extendings != null) {
            c = c.getPadding().withExtends(extendings.withMarkers(markers(extendings.getMarkers())));
        }
        JContainer<TypeTree> implementings = c.getPadding().getImplements();
        if (implementings != null) {
            c = c.getPadding().withImplements(implementings.withMarkers(markers(implementings.getMarkers())));
        }
        return c;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);
        return v.withMarkers(markers(v.getMarkers()));
    }

    /**
     * The spaces markers hold for a declaration's attributes: those it holds as parts of itself, and the end of its
     * tag when it is written as a tag.
     */
    private Markers markers(Markers markers) {
        if (getCursor().getNearestMessage("stop") != null) {
            return markers;
        }
        return markers.withMarkers(ListUtils.map(markers.getMarkers(), (Marker marker) -> {
            if (marker instanceof AttributeClause) {
                AttributeClause clause = (AttributeClause) marker;
                if (Layout.hasLineBreak(clause.getPrefix())) {
                    int target = continuation(getCursor(), oldIndent(clause.getPrefix()));
                    lineIndent = target;
                    return clause.withPrefix(indentTo(clause.getPrefix(), target, target));
                }
            } else if (marker instanceof TagSyntax) {
                TagSyntax tag = (TagSyntax) marker;
                int line = line(getCursor());
                if (Layout.hasLineBreak(tag.getBeforeEnd())) {
                    tag = tag.withBeforeEnd(indentTo(tag.getBeforeEnd(), line, line));
                }
                Cf.Tag.Closing closing = tag.getClosing();
                if (closing != null && Layout.hasLineBreak(closing.getBeforeEnd())) {
                    tag = tag.withClosing(closing.withBeforeEnd(indentTo(closing.getBeforeEnd(), line, line)));
                }
                return tag;
            }
            return marker;
        }));
    }

    /**
     * The indentation of an element's prefix, which has a line break, or null to keep it.
     */
    private @Nullable Integer prefixIndent(J tree, int old) {
        Cursor cursor = getCursor();
        Cursor parent = cursor.getParentTreeCursor();
        Object owner = parent.getValue();
        if (owner instanceof Cf.ExpressionStatement) {
            // the expression a statement holds is the statement
            cursor = parent;
            parent = parent.getParentTreeCursor();
            owner = parent.getValue();
        }
        if (tree instanceof Statement && TemplateOutput.isInTemplateList(cursor)) {
            return tagIndent(tree, cursor, parent);
        }
        if (owner instanceof J.Block && isStatementOf((J.Block) owner, tree)) {
            return blockBase(parent) + statementLevel(parent, tree);
        }
        if (owner instanceof J.Case && ((J.Case) owner).getStatements().contains(tree)) {
            return line(parent) + style.getIndentSize();
        }
        if (owner instanceof Cf.CompilationUnit) {
            return 0;
        }
        if (isBodyOf(owner, tree)) {
            if (tree instanceof J.Block) {
                // a brace on a line of its own is aligned with its owner, unless it is written further in
                int moved = old + parent.getNearestMessage(DELTA, 0);
                return moved > line(parent) ? moved : line(parent);
            }
            return line(parent) + style.getIndentSize();
        }
        if (tree instanceof J.If.Else || tree instanceof J.Try.Catch) {
            return line(parent);
        }
        if (tree instanceof J.Empty && cursor.getParentOrThrow().getValue() instanceof JRightPadded &&
            closesList(cursor.getParentOrThrow().getValue(), cursor.getParentOrThrow().getParentOrThrow())) {
            // the space inside an empty list is before what closes it
            Cursor list = cursor.getParentOrThrow().getParentOrThrow();
            return (list.getValue() instanceof JContainer && !openers.isEmpty() ? openers.peek() : line(list)) + closingIndent();
        }
        return continuation(cursor.getParentOrThrow(), old);
    }

    /**
     * The indentation of a statement between tags, or null to keep it: where a template writes it, where text lays
     * out the body, or where the tag's end tag keeps its place.
     */
    private @Nullable Integer tagIndent(J tree, Cursor statement, Cursor list) {
        if (tree instanceof Cf.Text || hasText(list) || TemplateOutput.writesBefore(statement) || endIsFixed(tree, statement)) {
            return null;
        }
        Object owner = list.getValue();
        if (owner instanceof Cf.CompilationUnit) {
            return 0;
        }
        if (owner instanceof J.MethodDeclaration) {
            // a <cfargument> of a function written as a tag
            return line(list) + style.getIndentSize();
        }
        Cursor ownerCursor = list.getParentTreeCursor();
        int base = line(ownerCursor);
        Object tag = ownerCursor.getValue();
        TabsAndIndentsStyle.Tags tags = style.getTags();
        if (tag instanceof J.ClassDeclaration) {
            return base + (tags.getIndentComponentBody() ? style.getIndentSize() : 0);
        }
        if (tag instanceof Cf.Tag && ((Cf.Tag) tag).isNamed("cfsilent")) {
            return base + (tags.getIndentSilentBody() ? style.getIndentSize() : 0);
        }
        if (tag instanceof Cf.Tag && tree instanceof Cf.Tag) {
            Cf.Tag owning = (Cf.Tag) tag;
            Cf.Tag child = (Cf.Tag) tree;
            if (owning.isNamed("cfif") && (child.isNamed("cfelse") || child.isNamed("cfelseif"))) {
                return base;
            }
            if (owning.isNamed("cftry") && (child.isNamed("cfcatch") || child.isNamed("cffinally"))) {
                return base + (tags.getIndentCatchFromTry() ? style.getIndentSize() : 0);
            }
            if (owning.isNamed("cfswitch") && (child.isNamed("cfcase") || child.isNamed("cfdefaultcase"))) {
                return base + (tags.getIndentCaseFromSwitch() ? style.getIndentSize() : 0);
            }
        }
        return base + style.getIndentSize();
    }

    /**
     * Whether a tag's end tag stays where it is, as when the template writes the whitespace before it, so its start
     * tag stays aligned with it.
     */
    private boolean endIsFixed(J tree, Cursor statement) {
        J.Block body = tree instanceof Cf.Tag ? ((Cf.Tag) tree).getBody() :
                tree instanceof J.MethodDeclaration ? ((J.MethodDeclaration) tree).getBody() :
                        tree instanceof J.ClassDeclaration ? ((J.ClassDeclaration) tree).getBody() : null;
        if (body == null || !Layout.hasLineBreak(body.getEnd()) || tree instanceof Cf.Tag && ((Cf.Tag) tree).isNamed("cfscript")) {
            return false;
        }
        Cursor bodyCursor = new Cursor(statement, body);
        return hasText(bodyCursor) || TemplateOutput.writesAtEnd(bodyCursor);
    }

    /**
     * The indentation of the end of a body, before its <code>}</code> or end tag, or of the end of a file, or null to keep
     * it.
     */
    private @Nullable Integer endIndent(Cursor cursor) {
        Object value = cursor.getValue();
        if (value instanceof Cf.CompilationUnit) {
            return TemplateOutput.isTemplate((Cf.CompilationUnit) value) && hasText(cursor) ? null : 0;
        }
        if (TemplateOutput.isTemplateBody(cursor)) {
            return hasText(cursor) ? null : line(cursor.getParentTreeCursor());
        }
        return blockBase(cursor);
    }

    /**
     * How far the comments at the end of a body are indented from its end: as far as its statements.
     */
    private int statementLevel(Cursor cursor) {
        Object value = cursor.getValue();
        if (value instanceof Cf.CompilationUnit) {
            return 0;
        }
        if (value instanceof J.Block && cursor.getParentTreeCursor().getValue() instanceof J.ClassDeclaration &&
            TemplateOutput.isTagSyntax(cursor.getParentTreeCursor().getValue())) {
            return style.getTags().getIndentComponentBody() ? style.getIndentSize() : 0;
        }
        return statementLevel(cursor, null);
    }

    /**
     * How far a statement of the block at this cursor is indented from the block's base.
     */
    private int statementLevel(Cursor block, @Nullable J statement) {
        Object owner = block.getParentTreeCursor().getValue();
        if (owner instanceof Cf.Tag && ((Cf.Tag) owner).isNamed("cfscript")) {
            return style.getTags().getIndentScriptBody() ? style.getIndentSize() : 0;
        }
        if (owner instanceof J.Switch) {
            return style.getIndentCaseFromSwitch() ? style.getIndentSize() : 0;
        }
        if (owner instanceof Cf.Tag && ((Cf.Tag) owner).isNamed("cfsilent")) {
            return style.getTags().getIndentSilentBody() ? style.getIndentSize() : 0;
        }
        return style.getIndentSize();
    }

    /**
     * The indentation of the line a block's owner starts on, which its statements are indented from.
     */
    private int blockBase(Cursor block) {
        Cursor owner = block.getParentTreeCursor();
        Object value = owner.getValue();
        if (value instanceof J.Block || value instanceof Cf.CompilationUnit || !(value instanceof J)) {
            // a block written as a statement is its own owner
            return line(block);
        }
        return line(owner);
    }

    private static boolean isStatementOf(J.Block block, J tree) {
        for (JRightPadded<Statement> statement : block.getPadding().getStatements()) {
            if (statement.getElement() == tree) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a statement is the body an {@code if}, {@code else} or loop owns, as {@code if (x)} owns
     * {@code return;} on the next line, or its block.
     */
    private static boolean isBodyOf(Object owner, J tree) {
        if (owner instanceof J.If) {
            return ((J.If) owner).getThenPart() == tree;
        }
        if (owner instanceof J.If.Else) {
            return ((J.If.Else) owner).getBody() == tree;
        }
        if (owner instanceof J.ForLoop) {
            return ((J.ForLoop) owner).getBody() == tree;
        }
        if (owner instanceof J.ForEachLoop) {
            return ((J.ForEachLoop) owner).getBody() == tree;
        }
        if (owner instanceof J.WhileLoop) {
            return ((J.WhileLoop) owner).getBody() == tree;
        }
        if (owner instanceof J.DoWhileLoop) {
            return ((J.DoWhileLoop) owner).getBody() == tree;
        }
        if (owner instanceof J.Block) {
            return false;
        }
        // the body of a function, a component, a closure, a try, a catch, a switch or a tag written as a statement
        return tree instanceof J.Block;
    }

    /**
     * Whether the element at this cursor's value, the right-padded element being visited, is the last of a list
     * closed by a parenthesis, bracket or brace.
     */
    private boolean closesList(JRightPadded<?> right) {
        return closesList(right, getCursor());
    }

    private static boolean closesList(JRightPadded<?> right, Cursor list) {
        Object value = list.getValue();
        if (value instanceof JContainer) {
            JContainer<?> container = (JContainer<?>) value;
            List<? extends JRightPadded<?>> elements = container.getPadding().getElements();
            if (elements.isEmpty() || elements.get(elements.size() - 1) != right ||
                container.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                return false;
            }
            Object owner = list.getParentTreeCursor().getValue();
            if (owner instanceof J.MethodInvocation) {
                return ((J.MethodInvocation) owner).getPadding().getArguments() == container;
            }
            if (owner instanceof J.NewClass) {
                return ((J.NewClass) owner).getPadding().getArguments() == container;
            }
            if (owner instanceof J.MethodDeclaration) {
                return !TemplateOutput.isTagSyntax((J) owner) && ((J.MethodDeclaration) owner).getPadding().getParameters() == container;
            }
            return owner instanceof J.NewArray || owner instanceof Cf.StructLiteral || owner instanceof Cf.Invocation;
        }
        if (value instanceof J.Lambda.Parameters) {
            J.Lambda.Parameters parameters = (J.Lambda.Parameters) value;
            List<JRightPadded<J>> elements = parameters.getPadding().getParameters();
            return parameters.isParenthesized() && !elements.isEmpty() && elements.get(elements.size() - 1) == right;
        }
        if (value instanceof J.ForEachLoop.Control) {
            return ((J.ForEachLoop.Control) value).getPadding().getIterable() == right;
        }
        if (value instanceof J.ForLoop.Control) {
            List<JRightPadded<Statement>> update = ((J.ForLoop.Control) value).getPadding().getUpdate();
            return !update.isEmpty() && update.get(update.size() - 1) == right;
        }
        return value instanceof J.Parentheses || value instanceof J.ControlParentheses || value instanceof J.ArrayDimension;
    }

    /**
     * The space before what closes a list, aligned with the line that opens it.
     */
    private Space closing(Space space) {
        int target = (getCursor().getValue() instanceof JContainer && !openers.isEmpty() ? openers.peek() : line(getCursor())) +
                     closingIndent();
        lineIndent = target;
        return indentTo(space, target, target + style.getContinuationIndent());
    }

    private int closingIndent() {
        return style.getIndentClosingBrackets() ? style.getContinuationIndent() : 0;
    }

    /**
     * The indentation of a line that continues what the nearest element starting a line above the cursor started:
     * where it is, moved as that element moved. A line no further in than that element is indented a continuation
     * indent from it. The operands of an operator spread over lines continue what the operator's expression
     * continues, so they line up with its first operand.
     */
    private int continuation(Cursor cursor, int old) {
        int anchor = anchor(cursor);
        int moved = old + cursor.getNearestMessage(DELTA, 0);
        return moved > anchor ? moved : anchor + style.getContinuationIndent();
    }

    private int anchor(Cursor cursor) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            Object value = c.getValue();
            Integer anchor = c.getMessage(ANCHOR);
            if (anchor != null && !(value instanceof J.Binary || value instanceof Cf.Binary || value instanceof J.Ternary)) {
                return anchor;
            }
        }
        return cursor.getNearestMessage(LINE, 0);
    }

    private static int line(Cursor cursor) {
        Integer line = cursor.getMessage(LINE);
        return line == null ? cursor.getNearestMessage(LINE, 0) : line;
    }

    private boolean hasText(Cursor list) {
        return list.computeMessageIfAbsent(HAS_TEXT, k -> {
            Object value = list.getValue();
            List<Statement> statements = value instanceof J.Block ? ((J.Block) value).getStatements() :
                    value instanceof Cf.CompilationUnit ? ((Cf.CompilationUnit) value).getStatements() : null;
            if (statements != null) {
                for (Statement statement : statements) {
                    if (statement instanceof Cf.Text) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private int oldIndent(Space space) {
        return Layout.width(Layout.lastLine(lastLineBreakSegment(space)), style.getTabSize());
    }

    /**
     * The last part of a space with a line break in it: its whitespace or a comment's suffix.
     */
    private static String lastLineBreakSegment(Space space) {
        List<Comment> comments = space.getComments();
        for (int i = comments.size() - 1; i >= 0; i--) {
            if (Layout.hasLineBreak(comments.get(i).getSuffix())) {
                return comments.get(i).getSuffix();
            }
        }
        return space.getWhitespace();
    }

    /**
     * A space whose last line is indented to {@code target} and whose comments on lines of their own are indented to
     * {@code commentTarget}. A comment in the first column stays there, as commented-out code and banners are
     * written. A block comment moves its lines with it when they are written as a column of {@code *}.
     */
    private Space indentTo(Space space, int target, int commentTarget) {
        // the parts that end in a line's indentation are the whitespace and each comment's suffix
        List<Comment> comments = space.getComments();
        int last = Layout.hasLineBreak(space.getWhitespace()) ? 0 : -1;
        for (int i = 0; i < comments.size(); i++) {
            if (Layout.hasLineBreak(comments.get(i).getSuffix())) {
                last = i + 1;
            }
        }
        if (last < 0) {
            return space;
        }
        // how far the line each comment starts moves, or 0 for a comment that does not start a line
        int[] moved = new int[comments.size()];
        String whitespace = space.getWhitespace();
        if (Layout.hasLineBreak(whitespace) && (last == 0 || !Layout.lastLine(whitespace).isEmpty())) {
            int indent = last == 0 ? target : commentTarget;
            if (!comments.isEmpty()) {
                moved[0] = indent - Layout.width(Layout.lastLine(whitespace), style.getTabSize());
            }
            whitespace = withIndent(whitespace, indent);
        }
        List<String> suffixes = new ArrayList<>(comments.size());
        for (int i = 0; i < comments.size(); i++) {
            String suffix = comments.get(i).getSuffix();
            if (Layout.hasLineBreak(suffix) && (i + 1 == last || !Layout.lastLine(suffix).isEmpty())) {
                int indent = i + 1 == last ? target : commentTarget;
                if (i + 1 < comments.size()) {
                    moved[i + 1] = indent - Layout.width(Layout.lastLine(suffix), style.getTabSize());
                }
                suffix = withIndent(suffix, indent);
            }
            suffixes.add(suffix);
        }
        return space.withWhitespace(whitespace).withComments(ListUtils.map(comments, (i, comment) -> {
            Comment c = moved[i] == 0 ? comment : shiftLines(comment, moved[i]);
            return c.withSuffix(suffixes.get(i));
        }));
    }

    /**
     * The whitespace with its last line indented to a width. A line already that wide keeps its tabs and spaces.
     */
    private String withIndent(String whitespace, int width) {
        if (Layout.width(Layout.lastLine(whitespace), style.getTabSize()) == width) {
            return whitespace;
        }
        return Layout.withLastLine(whitespace, Layout.indent(width, style.getUseTabCharacter(), style.getTabSize()));
    }

    /**
     * A block comment moved by {@code shift} columns, when every line after its first starts with {@code *}, as a
     * doc comment's do. The text of any other comment stays as it is.
     */
    private Comment shiftLines(Comment comment, int shift) {
        String text;
        if (comment instanceof TagComment) {
            text = ((TagComment) comment).getText();
        } else if (comment instanceof TextComment && comment.isMultiline()) {
            text = ((TextComment) comment).getText();
        } else {
            return comment;
        }
        if (!Layout.hasLineBreak(text) || !isStarred(text)) {
            return comment;
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            out.append(c);
            i++;
            if (Layout.isLineBreak(c)) {
                if (c == '\r' && i < text.length() && text.charAt(i) == '\n') {
                    out.append('\n');
                    i++;
                }
                int end = i;
                while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
                    end++;
                }
                if (end < text.length() && Layout.isLineBreak(text.charAt(end))) {
                    // a blank line gets no indentation
                    continue;
                }
                int width = Layout.width(text.substring(i, end), style.getTabSize());
                out.append(Layout.indent(Math.max(0, width + shift), style.getUseTabCharacter(), style.getTabSize()));
                i = end;
            }
        }
        String shifted = out.toString();
        if (shifted.equals(text)) {
            return comment;
        }
        return comment instanceof TagComment ? ((TagComment) comment).withText(shifted) : ((TextComment) comment).withText(shifted);
    }

    private static boolean isStarred(String text) {
        boolean afterBreak = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Layout.isLineBreak(c)) {
                afterBreak = true;
            } else if (afterBreak && c != ' ' && c != '\t') {
                if (c != '*') {
                    return false;
                }
                afterBreak = false;
            }
        }
        return true;
    }
}
