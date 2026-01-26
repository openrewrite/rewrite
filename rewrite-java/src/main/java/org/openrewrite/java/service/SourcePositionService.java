/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.service;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.service.Span.ColSpan;
import org.openrewrite.java.tree.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for computing source code position metrics such as column alignment positions and tree element lengths.
 * <p>
 * This service is useful for formatting and layout calculations, particularly when determining how to align
 * elements in chained method calls, multi-line variable declarations, and method parameters.
 */
@Incubating(since = "8.63.0")
public class SourcePositionService {
    /**
     * Computes the position span of the element at the given cursor.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public Span positionOf(Cursor cursor) {
        return positionOfChild(cursor, cursor.getValue(), getSpanPrintCursor(cursor));
    }

    /**
     * Computes the position span of a container element.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public Span positionOf(Cursor cursor, JContainer<? extends J> container) {
        return positionOfChild(cursor, container, getSpanPrintCursor(cursor));
    }

    /**
     * Computes the position span of a right-padded element.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public Span positionOf(Cursor cursor, JRightPadded<J> rightPadded) {
        return positionOfChild(cursor, rightPadded, getSpanPrintCursor(cursor));
    }

    /**
     * Computes the position span of a J element.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public Span positionOf(Cursor cursor, J child) {
        return positionOfChild(cursor, child, getSpanPrintCursor(cursor));
    }

    /**
     * Computes the position span of the element at the given cursor.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public ColSpan columnsOf(Cursor cursor) {
        return positionOfChild(cursor, cursor.getValue(), getColSpanPrintCursor(cursor)).getColSpan();
    }

    /**
     * Computes the position span of a container element.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public ColSpan columnsOf(Cursor cursor, JContainer<? extends J> container) {
        return positionOfChild(cursor, container, getColSpanPrintCursor(cursor)).getColSpan();
    }

    /**
     * Computes the position span of a right-padded element.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public ColSpan columnsOf(Cursor cursor, JRightPadded<J> rightPadded) {
        return positionOfChild(cursor, rightPadded, getColSpanPrintCursor(cursor)).getColSpan();
    }

    /**
     * Computes the position span of a J element.
     *
     * @see #positionOfChild(Cursor, Object, Cursor)
     */
    public ColSpan columnsOf(Cursor cursor, J child) {
        return positionOfChild(cursor, child, getColSpanPrintCursor(cursor)).getColSpan();
    }

    /**
     * Finds the ancestor element in the cursor path that has a newline in its prefix.
     * <p>
     * For method chains, this navigates to the root of the chain. Then it walks up the cursor
     * path until it finds an element with a newline prefix (or reaches the compilation unit).
     * This is the element whose indentation forms the baseline for calculations.
     *
     * @param cursor the cursor to start searching from
     * @return the cursor pointing to the element with a newline prefix
     */
    public Cursor computeNewLinedCursorElement(Cursor cursor) {
        Object cursorValue = cursor.getValue();
        Cursor methodCursor = cursor;
        while (cursorValue instanceof J.MethodInvocation && ((J.MethodInvocation) cursorValue).getSelect() instanceof J.MethodInvocation) {
            boolean hasNewLine = ((J.MethodInvocation) cursorValue).getPadding().getSelect().getAfter().getWhitespace().contains("\n") || ((J.MethodInvocation) cursorValue).getPadding().getSelect().getAfter().getComments().stream().anyMatch(c -> c.getSuffix().contains("\n"));
            if (hasNewLine) {
                return methodCursor;
            }
            cursorValue = ((J.MethodInvocation) cursorValue).getSelect();
            methodCursor = new Cursor(methodCursor, ((J.MethodInvocation) cursorValue).getSelect());
        }
        cursorValue = cursor.getValue();
        if (cursorValue instanceof J) {
            J j = (J) cursorValue;
            boolean hasNewLine = j.getPrefix().getWhitespace().contains("\n") || j.getComments().stream().anyMatch(c -> c.getSuffix().contains("\n"));
            Cursor parent = cursor.dropParentUntil(it -> (!(it instanceof J.MethodInvocation || it instanceof JRightPadded || it instanceof JLeftPadded || (it instanceof J && ((J) it).getPrefix().getWhitespace().contains("\n"))))); // a newline in the method chain after the current cursor does not count as a newLinedCursorElement
            while (!(parent.getValue() instanceof Tree) && parent.getParent() != null) {
                parent = parent.getParent();
            }
            boolean isRootOrCompilationUnit = parent.getValue() instanceof JavaSourceFile || parent.getValue() == Cursor.ROOT_VALUE || parent.getParent() == null;
            if (!hasNewLine && !isRootOrCompilationUnit) {
                return computeNewLinedCursorElement(parent);
            }
        } else if (cursor.getParent() != null && cursor.getParent().getValue() != Cursor.ROOT_VALUE) {
            return computeNewLinedCursorElement(cursor.getParent());
        }

        return cursor;
    }

    /**
     * Computes the position span of a child element within the source code by printing the source
     * file and tracking line/column positions until the target element is reached.
     * <p>
     * This method uses a custom {@link JavaPrinter} to traverse the AST from the source file root,
     * tracking line and column positions as it prints. When the target child element is encountered,
     * it marks the position and calculates the span including:
     * <ul>
     *   <li>Start line and column (1-indexed)</li>
     *   <li>End line and column after the element's content</li>
     *   <li>Maximum column width if the element spans multiple lines</li>
     * </ul>
     * <p>
     * The method handles various AST element types including {@link J} elements, {@link JContainer}
     * containers, and {@link JRightPadded} wrapped elements.
     *
     * @param cursor the cursor providing context for the position calculation, must point to or contain
     *               a {@link JavaSourceFile} ancestor for printing
     * @param child  the child element to locate (can be same as cursor element) or any child({@link J} element, {@link JContainer} or {@link JRightPadded})
     * @return a {@link Span} containing the computed position information
     * @throws IllegalArgumentException if the child is a raw {@link Collection} (use padding accessor methods instead),
     *                                  if the child type is not supported, or if the child cannot be found in the source file
     */
    private Span positionOfChild(Cursor cursor, Object child, Cursor printCursor) {
        J findJ;
        JContainer<J> findJContainer;
        JRightPadded<J> findJRightPadded;
        if (child instanceof J) {
            findJContainer = null;
            //JavaPrinter does not call visitContainer for Try-resources so resources themselves are not visited.
            findJ = child instanceof J.Try.Resource ? ((J.Try.Resource) child).getVariableDeclarations() : (J) child;
            findJRightPadded = null;
        } else if (child instanceof JContainer) {
            findJContainer = (JContainer<J>) child;
            findJ = null;
            findJRightPadded = null;
        } else if (child instanceof JRightPadded) {
            findJContainer = null;
            findJ = null;
            findJRightPadded = (JRightPadded<J>) child;
        } else if (child instanceof Collection) {
            throw new IllegalArgumentException("Can only find J elements or their containers. Did you create a Cursor from a `get...()` method which should be done on the `getPadding().get...()` instead?");
        } else {
            throw new IllegalArgumentException("Can only find J elements or their containers. Not on " + child.getClass().getSimpleName() + ".");
        }
        AtomicBoolean indenting = new AtomicBoolean(true);
        AtomicInteger rowIndent = new AtomicInteger(0);
        AtomicInteger startLine = new AtomicInteger(1);
        AtomicInteger startCol = new AtomicInteger(1);
        AtomicBoolean printing = new AtomicBoolean(false);
        AtomicBoolean found = new AtomicBoolean(false);
        JavaPrinter<TreeVisitor<?, ?>> javaPrinter = new JavaPrinter<TreeVisitor<?, ?>>() {
            @Nullable
            J foundJ = null;

            @Override
            protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                if (findJContainer != null && nodes == findJContainer.getPadding().getElements()) {
                    for (int i = 0; i < nodes.size(); i++) {
                        JRightPadded<? extends J> node = nodes.get(i);
                        if (i == 0) {
                            visit((J) startFind(node.getElement(), p), p);
                        } else {
                            visit(node.getElement(), p);
                        }
                        visitSpace(node.getAfter(), location.getAfterLocation(), p);
                        visitMarkers(node.getMarkers(), p);
                        if (i < nodes.size() - 1) {
                            p.append(suffixBetween);
                        }
                    }
                    found.set(true);
                } else {
                    super.visitRightPadded(nodes, location, suffixBetween, p);
                }
            }

            @Override
            protected void visitRightPadded(@Nullable JRightPadded<? extends J> rightPadded, JRightPadded.Location location, @Nullable String suffix, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                if (findJRightPadded != null && rightPadded == findJRightPadded) {
                    super.visitRightPadded(rightPadded.withElement(startFind(rightPadded.getElement(), p)), location, suffix, p);
                    found.set(true);
                } else {
                    super.visitRightPadded(rightPadded, location, suffix, p);
                }
            }

            @Override
            public @Nullable J preVisit(@NonNull J tree, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                if (found.get()) {
                    stopAfterPreVisit();
                }

                if (tree != cursor.getValue() && cursor.getValue() instanceof J && tree.getId().equals(((J) cursor.getValue()).getId())) {
                    setCursor(cursor);
                    tree = cursor.getValue();
                }
                if (findJ != null && tree == findJ) {
                    foundJ = startFind(tree, p);
                    tree = foundJ;
                }
                return super.preVisit(tree, p);
            }

            @Override
            public @Nullable J postVisit(@NonNull J tree, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                if (tree == foundJ) {
                    found.set(true);
                }
                return super.postVisit(tree, p);
            }

            public <T extends J> T startFind(J tree, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                Space space = tree.getPrefix();
                p.append(space.getWhitespace());

                List<Comment> comments = space.getComments();
                for (int i = 0; i < comments.size(); i++) {
                    Comment comment = comments.get(i);
                    comment.printComment(getCursor(), p);
                    p.append(comment.getSuffix());
                }

                printing.set(true);
                return tree.withPrefix(Space.EMPTY);
            }
        };
        PrintOutputCapture<TreeVisitor<?, ?>> printLine = new PrintOutputCapture<TreeVisitor<?, ?>>(javaPrinter, PrintOutputCapture.MarkerPrinter.SANITIZED) {
            @Override
            public PrintOutputCapture<TreeVisitor<?, ?>> append(@Nullable String text) {
                if (text == null || text.isEmpty() || found.get()) {
                    return this;
                }
                if (printing.get()) {
                    return super.append(text);
                }
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '\n') {
                        startLine.incrementAndGet();
                        startCol.set(1);
                        rowIndent.set(0);
                        indenting.set(true);
                        ;
                    } else if (c == '\r') {
                        // Skip \r in \r\n
                        if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') {
                            startLine.incrementAndGet();
                            startCol.set(1);
                            rowIndent.set(0);
                            indenting.set(true);
                            ;
                        }
                    } else {
                        startCol.incrementAndGet();
                        if (indenting.get() && c == ' ') {
                            rowIndent.incrementAndGet();
                        } else {
                            indenting.set(false);
                        }
                    }
                }
                return this;
            }

            @Override
            public PrintOutputCapture<TreeVisitor<?, ?>> append(char c) {
                if (found.get()) {
                    return this;
                }
                if (printing.get()) {
                    return super.append(c);
                }
                if (c == '\n' || c == '\r') {
                    startLine.incrementAndGet();
                    startCol.set(1);
                    rowIndent.set(0);
                    indenting.set(true);
                } else {
                    startCol.incrementAndGet();
                    if (indenting.get() && c == ' ') {
                        rowIndent.incrementAndGet();
                    } else {
                        indenting.set(false);
                    }
                }
                return this;
            }
        };
        boolean printEntireSourceFile = printCursor.getValue() instanceof JavaSourceFile;
        javaPrinter.visit(printCursor.getValue(), printLine, Objects.requireNonNull(printCursor.getParent()));
        String content = printLine.getOut();
        if (found.get()) {
            int indent = StringUtils.indent(content).length();
            int col = startCol.get();
            int maxColumn = 1;
            int lines = 0;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if ('\n' == c) {
                    lines++;
                    if (maxColumn < col) {
                        maxColumn = col;
                    }
                    col = 1;
                } else if (c == '\r') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                        // Skip \r in \r\n
                        continue;
                    } else {
                        lines++;
                        if (maxColumn < col) {
                            maxColumn = col;
                        }
                        col = 1;
                    }
                } else {
                    col++;
                }
            }
            if (maxColumn < col) {
                maxColumn = col;
            }

            return Span.builder()
                    .startLine(printEntireSourceFile ? startLine.get() : -1)
                    .endLine(printEntireSourceFile ? startLine.get() + lines : -1)
                    .colSpan(
                            ColSpan.builder()
                                    .startColumn(startCol.get() + indent)
                                    .endColumn(col)
                                    .maxColumn(maxColumn)
                                    .indent(rowIndent.get())
                                    .build())
                    .build();
        }
        throw new IllegalArgumentException("The child was not found in the sourceFile. Are you sure the passed in cursor's value contains the child and you are not searching for a mutated element in a non-mutated Cursor value?");
    }

    private Cursor getSpanPrintCursor(Cursor cursor) {
        Cursor root = new Cursor(null, Cursor.ROOT_VALUE);
        while (cursor.getParent() != null && cursor.getParent().getValue() != Cursor.ROOT_VALUE && !(cursor.getValue() instanceof JavaSourceFile)) {
            cursor = cursor.getParent();
        }
        return new Cursor(root, cursor.getValue());
    }

    private Cursor getColSpanPrintCursor(Cursor cursor) {
        Cursor root = new Cursor(null, Cursor.ROOT_VALUE);
        try {
            return new Cursor(root, computeNewLinedCursorElement(cursor.dropParentUntil(it -> it instanceof Tree && !(it instanceof J.MethodInvocation))).getValue());
        } catch (IllegalStateException e) {
            return new Cursor(root, computeNewLinedCursorElement(cursor).getValue());
        }
    }
}
