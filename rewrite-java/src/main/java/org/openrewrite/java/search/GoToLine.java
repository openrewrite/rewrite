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
package org.openrewrite.java.search;

import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.SearchResult;

import java.util.*;

@Incubating(since = "8.12.1")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoToLine {
    /**
     * Set to true to print the file as it is being searched. Useful for debugging.
     * The JIT will completely remove code flagged behind this when set to false because it is unreachable,
     * and this is a constant.
     */
    private static final boolean debug = false;

    /**
     * Find all elements in the LST at the given line.
     * <p>
     * <strong>NOTE:</strong> line number is 1-based, which matches the behavior of most editors.
     * </p>
     *
     * @param sourceFile The source file to search.
     * @param line       The line number to search. 1-based.
     * @return The elements found at the given line, or an empty collection if no elements were found.
     */
    public static Collection<J> findLine(JavaSourceFile sourceFile, int line) {
        if (line < 1) {
            throw new IllegalArgumentException("Line numbers must be 1-based");
        }
        Set<J> found = Collections.newSetFromMap(new IdentityHashMap<>());
        LineColumnLocatorVisitor<Integer> locatorVisitor = new LineColumnLocatorVisitor<>(line, null, found);
        locatorVisitor.visit(
                sourceFile,
                locatorVisitor.new LineColumnLocatorPrinter(0), new Cursor(null, "root")
        );
        return Collections.unmodifiableSet(found);
    }

    /**
     * Find all element in the LST at the given line and column.
     * <p>
     * <strong>NOTE:</strong> line and column numbers are 1-based, which matches the behavior of most editors.
     * </p>
     *
     * @param sourceFile The source file to search.
     * @param line       The line number to search. 1-based.
     * @param column     The column number to search. 1-based.
     * @return The elements found at the given line and column, or an empty collection if no elements were found.
     */
    public static Collection<J> findLineColumn(JavaSourceFile sourceFile, int line, int column) {
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("Line and column numbers must be 1-based");
        }
        Set<J> found = Collections.newSetFromMap(new IdentityHashMap<>());
        LineColumnLocatorVisitor<Integer> locatorVisitor = new LineColumnLocatorVisitor<>(line, column, found);
        locatorVisitor.visit(
                sourceFile,
                locatorVisitor.new LineColumnLocatorPrinter(0), new Cursor(null, "root")
        );
        return Collections.unmodifiableSet(found);
    }


    @RequiredArgsConstructor
    private static class LineColumnLocatorVisitor<P> extends JavaPrinter<P> {
        private final int line;
        @Nullable
        private final Integer column;
        private final Set<J> found;
        private int foundLineNumber = 1;
        private int foundColumnNumber = 1;

        private boolean foundLine() {
            return foundLineNumber == line;
        }

        private boolean beyondLine() {
            return foundLineNumber > line;
        }

        private boolean foundColumn() {
            return column == null || foundColumnNumber == column;
        }

        private boolean foundLineColumn() {
            return foundLine() && foundColumn();
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
            Space s = super.visitSpace(space, loc, p);
            if (foundLineColumn() && loc.isPrefix()) {
                found.add(getCursor().getValue());
                return s;
            }
            if (beyondLine()) {
                // Optimization to avoid visiting the rest of the file once we've found the element(s)
                stopAfterPreVisit();
            }
            return s;
        }

        class LineColumnLocatorPrinter extends PrintOutputCapture<P> {

            public LineColumnLocatorPrinter(P p) {
                super(p);
            }

            @Override
            public PrintOutputCapture<P> append(@Nullable String text) {
                if (text == null) {
                    return this;
                }
                for (int i = 0; i < text.length(); i++) {
                    append(text.charAt(i));
                }
                return this;
            }

            @Override
            public PrintOutputCapture<P> append(char c) {
                if (isNewLine(c)) {
                    foundColumnNumber = 1;
                    foundLineNumber++;
                } else if (foundLine()) {
                    foundColumnNumber++;
                }
                // Actually appending isn't necessary
                return debug ? super.append(c) : this;
            }
        }
    }

    private static boolean isNewLine(int c) {
        return c == '\n';
    }
}
