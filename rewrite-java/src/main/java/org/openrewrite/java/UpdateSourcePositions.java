/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Range;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.18.0")
public class UpdateSourcePositions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update source positions";
    }

    @Override
    public String getDescription() {
        return "Calculate start position and length for every AST element.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        Map<Tree, Range> positionMap = new IdentityHashMap<>();
        PositionPrintOutputCapture ppoc = new PositionPrintOutputCapture();

        JavaPrinter<ExecutionContext> printer = new JavaPrinter<ExecutionContext>() {
            final JavaPrinter<ExecutionContext> spacePrinter = new JavaPrinter<>();

            @Override
            public @Nullable J visit(@Nullable Tree tree, PrintOutputCapture<ExecutionContext> outputCapture) {
                if (tree == null) {
                    return null;
                }

                J t = (J) tree;

                PositionPrintOutputCapture prefix = new PositionPrintOutputCapture(ppoc.pos, ppoc.line, ppoc.column);
                spacePrinter.visitSpace(t.getPrefix(), Space.Location.ANY, prefix);

                Range.Position startPosition = new Range.Position(prefix.pos, prefix.line, prefix.column);
                t = super.visit(tree, outputCapture);
                Range.Position endPosition = new Range.Position(ppoc.pos, ppoc.line, ppoc.column);
                positionMap.put(t, new Range(randomId(), startPosition, endPosition));

                return t;
            }

            @Override
            protected void visitModifier(J.Modifier modifier, PrintOutputCapture<ExecutionContext> p) {
                if (modifier == null) {
                    return;
                }

                PositionPrintOutputCapture prefix = new PositionPrintOutputCapture(ppoc.pos, ppoc.line, ppoc.column);
                spacePrinter.visitSpace(modifier.getPrefix(), Space.Location.ANY, prefix);

                Range.Position startPosition = new Range.Position(prefix.pos, prefix.line, prefix.column);
                super.visitModifier(modifier, p);
                Range.Position endPosition = new Range.Position(ppoc.pos, ppoc.line, ppoc.column);
                positionMap.put(modifier, new Range(randomId(), startPosition, endPosition));
            }

        };

        return new JavaVisitor<ExecutionContext>() {
            boolean firstVisit = true;

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                if (firstVisit) {
                    tree = printer.visit(tree, ppoc);
                    firstVisit = false;
                }

                Range range = positionMap.get(tree);
                if (range != null) {
                    J t = tree.withMarkers(tree.getMarkers().add(range));
                    return super.visit(t, ctx);
                }
                return super.visit(tree, ctx);
            }
        };
    }

    private static class PositionPrintOutputCapture extends PrintOutputCapture<ExecutionContext> {
        int pos = 0;
        int line = 1;
        int column = 0;
        private boolean lineBoundary;

        public PositionPrintOutputCapture() {
            super(new InMemoryExecutionContext());
        }

        public PositionPrintOutputCapture(int pos, int line, int column) {
            this();
            this.pos = pos;
            this.line = line;
            this.column = column;
        }

        @Override
        public PrintOutputCapture<ExecutionContext> append(char c) {
            pos++;
            if (lineBoundary) {
                line++;
                column = 0;
                lineBoundary = false;
            } else {
                column++;
            }
            if (c == '\n') {
                lineBoundary = true;
            }
            return this;
        }

        @Override
        public PrintOutputCapture<ExecutionContext> append(@Nullable String text) {
            if (text != null) {
                if (lineBoundary) {
                    line++;
                    column = 0;
                    lineBoundary = false;
                }
                int length = text.length();
                pos += length;
                int numberOfLines = 0;
                int indexOfLastNewLine = -1;
                for (int i = 0; i < length; i++) {
                    if (text.charAt(i) == '\n') {
                        indexOfLastNewLine = i;
                        numberOfLines++;
                    }
                }
                if (numberOfLines > 0) {
                    line += numberOfLines;
                    column = length - indexOfLastNewLine;
                } else {
                    column += length;
                }
            }
            return this;
        }
    }
}
