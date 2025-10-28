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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;

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
     * @param cursor the cursor pointing to the element whose alignment position should be computed
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

        while (parent != null && !(parent.getValue() instanceof  SourceFile)) {
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
}
