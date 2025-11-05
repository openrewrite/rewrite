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
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;

/**
 * Service for computing source code position metrics such as column alignment positions and tree element lengths.
 * <p>
 * This service is useful for formatting and layout calculations, particularly when determining how to align
 * elements in chained method calls, multi-line variable declarations, and method parameters.
 */
@Incubating(since = "8.63.0")
public class SourcePositionService {
    /**
     * Computes the column position where an element should be aligned to.
     * <p>
     * For elements that should align with a previous element (e.g., in method chains or parameter lists),
     * this calculates the column position of that alignment point. For elements that don't align,
     * it returns the parent's indentation plus the continuation indent.
     *
     * @param cursor       the cursor pointing to the element whose alignment position should be computed
     * @param continuation the continuation indent to add when the element doesn't align with another element
     * @return the column position (0-indexed) where the element should align to
     */
    public int computeColumnToAlignTo(Cursor cursor, int continuation) {
        Cursor alignWith = alignsWith(cursor);
        Cursor newLinedElementCursor;
        if (alignWith == null) {
            // Do not align, just calculate parents indentation
            newLinedElementCursor = computeNewLinedCursorElement(cursor.getParentTreeCursor());
            return ((J) newLinedElementCursor.getValue()).getPrefix().getIndent().length() + continuation;
        }
        newLinedElementCursor = computeNewLinedCursorElement(alignWith);
        if (alignWith == newLinedElementCursor) {
            //If they are the same element, it means that the first / indentation base is already on new line -> we should just indent with the continuation based on the previous correctly indented value
            Cursor parentCursor = computeNewLinedCursorElement(newLinedElementCursor.getParentTreeCursor());
            return ((J) parentCursor.getValue()).getPrefix().getIndent().length() + continuation;
        }
        if (newLinedElementCursor.getValue() instanceof J) {
            J j = newLinedElementCursor.getValue();
            AtomicInteger indentation = new AtomicInteger(-1);
            JavaPrinter<TreeVisitor<?, ?>> javaPrinter = new JavaPrinter<TreeVisitor<?, ?>>() {
                @Override
                public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                    if (multiVariable == alignWith.getValue() || SemanticallyEqual.areEqual(multiVariable, alignWith.getValue())) {
                        beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
                        indentation.set(p.getOut().length());
                        return multiVariable;
                    }
                    return super.visitVariableDeclarations(multiVariable, p);
                }

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                    if (method == alignWith.getValue() || SemanticallyEqual.areEqual(method, alignWith.getValue())) {
                        beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);
                        visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);
                        indentation.set(p.getOut().length());
                        return method;
                    }
                    return super.visitMethodInvocation(method, p);
                }
            };
            PrintOutputCapture<TreeVisitor<?, ?>> printLine = new PrintOutputCapture<TreeVisitor<?, ?>>(javaPrinter, PrintOutputCapture.MarkerPrinter.SANITIZED) {
                @Override
                public PrintOutputCapture<TreeVisitor<?, ?>> append(@Nullable String text) {
                    if (text != null && text.contains("\n")) {
                        out.setLength(0);
                        text = text.substring(text.lastIndexOf("\n") + 1);
                    }
                    return super.append(text);
                }
            };
            javaPrinter.visit(j, printLine, cursor.getParentOrThrow());

            return indentation.get();
        }
        throw new RuntimeException("Unable to calculate length due to unexpected cursor value: " + newLinedElementCursor.getValue().getClass());
    }

    /**
     * Computes the total length of a tree element from the first character after its newline prefix
     * to the end of the element, including any trailing semicolon if applicable.
     * <p>
     * This is useful for determining how much horizontal space an element occupies on its line,
     * which is important for line wrapping decisions.
     *
     * @param cursor the cursor pointing to the element whose length should be computed
     * @return the length in characters of the tree element
     */
    public int computeTreeLength(Cursor cursor) {
        Cursor newLinedElementCursor = computeNewLinedCursorElement(cursor);
        if (newLinedElementCursor.getValue() instanceof J) {
            J j = newLinedElementCursor.getValue();
            TreeVisitor<?, PrintOutputCapture<TreeVisitor<?, ?>>> printer = j.printer(cursor);
            PrintOutputCapture<TreeVisitor<?, ?>> capture = new PrintOutputCapture<>(printer, PrintOutputCapture.MarkerPrinter.SANITIZED);
            printer.visit(trimPrefix(j), capture, cursor.getParentOrThrow());

            return capture.getOut().length() + getSuffixLength(j);
        }
        throw new RuntimeException("Unable to calculate length due to unexpected cursor value: " + newLinedElementCursor.getValue().getClass());
    }

    /**
     * Computes the position span of the element at the given cursor.
     *
     * @see #positionOfChild(Cursor, Object)
     */
    public Span positionOf(Cursor cursor) {
        return positionOfChild(cursor, cursor.getValue());
    }

    /**
     * Computes the position span of a container element.
     *
     * @see #positionOfChild(Cursor, Object)
     */
    public Span positionOf(Cursor cursor, JContainer<? extends J> container) {
        return positionOfChild(cursor, container);
    }

    /**
     * Computes the position span of a right-padded element.
     *
     * @see #positionOfChild(Cursor, Object)
     */
    public Span positionOf(Cursor cursor, JRightPadded<J> rightPadded) {
        return positionOfChild(cursor, rightPadded);
    }

    /**
     * Computes the position span of a J element.
     *
     * @see #positionOfChild(Cursor, Object)
     */
    public Span positionOf(Cursor cursor, J child) {
        return positionOfChild(cursor, child);
    }

    private int getSuffixLength(J tree) {
        if (tree instanceof Statement && needsSemicolon((Statement) tree)) {
            return 1;
        }
        return 0;
    }

    private boolean needsSemicolon(Statement statement) {
        return statement instanceof J.MethodInvocation ||
                statement instanceof J.VariableDeclarations ||
                statement instanceof J.Assignment ||
                statement instanceof J.Package ||
                statement instanceof J.Return ||
                statement instanceof J.Import ||
                statement instanceof J.Assert;
    }

    private J trimPrefix(J tree) {
        return tree.withPrefix(Space.build(tree.getPrefix().getIndent(), emptyList()));
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
    private Cursor computeNewLinedCursorElement(Cursor cursor) {
        Object cursorValue = cursor.getValue();
        while (cursorValue instanceof J.MethodInvocation && ((J.MethodInvocation) cursorValue).getSelect() instanceof J.MethodInvocation) {
            cursorValue = ((J.MethodInvocation) cursorValue).getSelect();
        }
        if (cursorValue instanceof J) {
            J j = (J) cursorValue;
            boolean hasNewLine = j.getPrefix().getWhitespace().contains("\n") || j.getComments().stream().anyMatch(c -> c.getSuffix().contains("\n"));
            Cursor parent = cursor.getParentTreeCursor();
            boolean isCompilationUnit = parent.getValue() instanceof J.CompilationUnit;
            if (!hasNewLine && !isCompilationUnit) {
                return computeNewLinedCursorElement(parent);
            }
        }
        return cursor;
    }

    /**
     * Determines if the given cursor element should align with another element, and if so, returns
     * a cursor pointing to that alignment target.
     * <p>
     * Elements should align when they are part of a container (e.g., parameter list, method chain)
     * and the first element of that container is on the same line as the opening delimiter.
     * For example, in {@code method(param1, param2)}, param2 should align with param1.
     * But in {@code method(\n    param1,\n    param2)}, neither param should align since param1
     * is already on a new line.
     *
     * @param cursor the cursor to check for alignment
     * @return a cursor pointing to the element to align with, or null if no alignment is needed
     */
    private @Nullable Cursor alignsWith(Cursor cursor) {
        J cursorValue = cursor.getValue();
        Cursor parent = cursor;

        while (parent != null && !(parent.getValue() instanceof SourceFile)) {
            Object parentValue = parent.getValue();
            if (parentValue instanceof JContainer) {
                JContainer<J> container = parent.getValue();
                if (container.getElements().stream().anyMatch(e -> e == cursorValue || SemanticallyEqual.areEqual(e, cursorValue))) {
                    J firstElement = container.getElements().get(0);
                    if (!firstElement.getPrefix().getLastWhitespace().contains("\n")) {
                        if (firstElement == cursorValue || SemanticallyEqual.areEqual(firstElement, cursorValue)) {
                            return cursor;
                        } else {
                            return new Cursor(parent, firstElement);
                        }
                    }
                    return null; //do no align when not needed
                }
            } else if (parentValue instanceof J.MethodInvocation) {
                while (((J.MethodInvocation) parentValue).getSelect() instanceof J.MethodInvocation) {
                    parentValue = ((J.MethodInvocation) parentValue).getSelect();
                    parent = new Cursor(parent, parentValue);
                }
                J.MethodInvocation method = (J.MethodInvocation) parentValue;
                if (parent.getPathAsStream(o -> o instanceof J.MethodInvocation).anyMatch(value -> value == cursorValue || SemanticallyEqual.areEqual((J) value, cursorValue))) {
                    if (method.getPadding().getSelect() != null && !method.getPadding().getSelect().getAfter().getLastWhitespace().contains("\n")) {
                        return parent;
                    }
                    return null; //do no align when not needed
                }
            }
            parent = parent.getParent();
        }

        return null;
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
    private Span positionOfChild(Cursor cursor, Object child) {
        J findJ;
        JContainer<J> findJContainer;
        JRightPadded<J> findJRightPadded;
        if (child instanceof J) {
            findJContainer = null;
            findJ = (J) child;
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
        AtomicInteger startLine = new AtomicInteger(1);
        AtomicInteger startCol = new AtomicInteger(1);
        AtomicBoolean printing = new AtomicBoolean(false);
        AtomicBoolean found = new AtomicBoolean(false);
        JavaPrinter<TreeVisitor<?, ?>> javaPrinter = new JavaPrinter<TreeVisitor<?, ?>>() {
            @Nullable J foundJ = null;

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

                if (tree != cursor.getValue() && tree.getId().equals(((J) cursor.getValue()).getId())) {
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
                    } else if (c == '\r') {
                        // Skip \r in \r\n
                        if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') {
                            startLine.incrementAndGet();
                            startCol.set(1);
                        }
                    } else {
                        startCol.incrementAndGet();
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
                } else {
                    startCol.incrementAndGet();
                }
                return this;
            }
        };
        Cursor printCursor = cursor;
        if (!(cursor.getValue() instanceof JavaSourceFile)) {
            printCursor = cursor.dropParentUntil(c -> c instanceof JavaSourceFile);
        }
        javaPrinter.visit(printCursor.getValue(), printLine, printCursor.getParent());
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
                    .startLine(startLine.get())
                    .startColumn(startCol.get() + indent)
                    .endLine(startLine.get() + lines)
                    .endColumn(col)
                    .maxColumn(maxColumn)
                    .build();
        } else {
            throw new IllegalArgumentException("The child was not found in the sourceFile. Are you sure the passed in cursor's value contains the child and you are not searching for a mutated element in a non-mutated Cursor value?");
        }
    }
}
