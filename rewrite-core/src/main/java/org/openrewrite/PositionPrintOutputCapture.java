/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Range;

public class PositionPrintOutputCapture<P> extends PrintOutputCapture<P> {
        private int pos = 0;
        private int line = 1;
        private int column = 0;
        private boolean lineBoundary;

        public PositionPrintOutputCapture(P p) {
            super(p);
        }

        public PositionPrintOutputCapture(P p, Range.Position pos) {
            this(p);
            this.pos = pos.getOffset();
            this.line = pos.getLine();
            this.column = pos.getColumn();
        }

        @Override
        public PrintOutputCapture<P> append(char c) {
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
            return super.append(c);
        }

        @Override
        public PrintOutputCapture<P> append(@Nullable String text) {
            if (text != null) {
                if (lineBoundary) {
                    line++;
                    column = 0;
                    lineBoundary = false;
                }
                pos += text.length();
                long numberOfLines = text.chars().filter(c -> c == '\n').count();
                if (numberOfLines > 0) {
                    line += numberOfLines;
                    column = text.length() - (text.lastIndexOf('\n') + 1);
                } else {
                    column += text.length();
                }
            }
            return super.append(text);
        }

        public Range.Position getPosition() {
            return new Range.Position(pos, line, column);
        }
    }
