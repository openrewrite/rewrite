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

@Incubating(since = "8.63.0")
public class SourcePositionService {

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
                    if (SemanticallyEqual.areEqual(multiVariable, alignWith.getValue())) {
                        beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
                        indentation.set(p.getOut().length());
                        return multiVariable;
                    }
                    return super.visitVariableDeclarations(multiVariable, p);
                }

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<TreeVisitor<?, ?>> p) {
                    if (SemanticallyEqual.areEqual(method, alignWith.getValue())) {
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

    private @Nullable Cursor alignsWith(Cursor cursor) {
        J cursorValue = cursor.getValue();
        Cursor parent = cursor;

        while (parent != null && !(parent.getValue() instanceof  SourceFile)) {
            Object parentValue = parent.getValue();
            if (parentValue instanceof JContainer) {
                JContainer<J> container = parent.getValue();
                if (container.getElements().stream().anyMatch(e -> SemanticallyEqual.areEqual(e, cursorValue))) {
                    J firstElement = container.getElements().get(0);
                    if (!firstElement.getPrefix().getLastWhitespace().contains("\n")) {
                        if (SemanticallyEqual.areEqual(firstElement, cursorValue)) {
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
                if (parent.getPathAsStream(o -> o instanceof J.MethodInvocation).anyMatch(value -> SemanticallyEqual.areEqual((J) value, cursorValue))) {
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
