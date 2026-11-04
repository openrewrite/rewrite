/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/**
 * Applies gofmt blank line rules:
 * <ul>
 *   <li>Collapse 2+ consecutive blank lines to 1</li>
 *   <li>No blank line after opening brace</li>
 *   <li>No blank line before closing brace</li>
 * </ul>
 */
public class BlankLinesVisitor<P> extends GolangVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public BlankLinesVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        return collapseBlankLines(space);
    }

    /**
     * Collapse runs of 2+ blank lines (3+ newlines) to 1 blank line (2 newlines),
     * preserving the indentation after the last newline.
     */
    static Space collapseBlankLines(Space space) {
        String ws = space.getWhitespace();
        if (!ws.contains("\n\n\n")) {
            return space;
        }
        // Count newlines and collapse
        StringBuilder sb = new StringBuilder();
        int consecutiveNewlines = 0;
        for (int i = 0; i < ws.length(); i++) {
            char c = ws.charAt(i);
            if (c == '\n') {
                consecutiveNewlines++;
                if (consecutiveNewlines <= 2) {
                    sb.append(c);
                }
                // skip additional newlines and any whitespace between them
            } else if (c == '\r') {
                // skip \r (handled by NormalizeLineBreaks)
            } else if (c == ' ' || c == '\t') {
                if (consecutiveNewlines > 2) {
                    // skip whitespace in collapsed blank lines
                } else {
                    sb.append(c);
                }
            } else {
                consecutiveNewlines = 0;
                sb.append(c);
            }
        }
        // Preserve original indentation after last newline
        String collapsed = sb.toString();
        int lastNewline = collapsed.lastIndexOf('\n');
        int origLastNewline = ws.lastIndexOf('\n');
        if (lastNewline >= 0 && origLastNewline >= 0) {
            String origIndent = ws.substring(origLastNewline + 1);
            collapsed = collapsed.substring(0, lastNewline + 1) + origIndent;
        }
        return collapsed.equals(ws) ? space : space.withWhitespace(collapsed);
    }
}
