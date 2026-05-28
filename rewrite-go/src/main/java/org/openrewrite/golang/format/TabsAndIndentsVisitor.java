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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

public class TabsAndIndentsVisitor<P> extends GolangVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public TabsAndIndentsVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        if (!space.getWhitespace().contains("\n")) {
            return space;
        }
        // Count nesting depth from cursor.
        // Rules matching gofmt:
        // - J.Block adds depth, EXCEPT switch body blocks
        // - J.Case adds depth (for case body statements)
        // - Go.Composite adds depth
        //
        // This produces:
        //   func body:     1 tab  (func body Block)
        //   switch case:   1 tab  (func body Block, skip switch body Block)
        //   case body:     2 tabs (func body Block + Case)
        //   nested blocks: +1 per Block
        int depth = 0;
        // Start from PARENT — the current cursor value is the node whose prefix we're
        // formatting, so it shouldn't count toward its own indentation.
        for (Cursor c = getCursor().getParent(); c != null && c.getParent() != null; c = c.getParent()) {
            Object val = c.getValue();
            if (val instanceof J.Block) {
                // Skip the switch body block — gofmt does not indent case labels
                // relative to the switch body brace.
                if (isSwitchBody(c)) {
                    continue;
                }
                depth++;
            } else if (val instanceof J.Case) {
                depth++;
            } else if (val instanceof Go.Composite) {
                depth++;
            }
        }
        // Build indentation: newlines preserved, then tabs for depth
        String ws = space.getWhitespace();
        int lastNewline = ws.lastIndexOf('\n');
        if (lastNewline >= 0) {
            String beforeLastNewline = ws.substring(0, lastNewline + 1);
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append('\t');
            }
            return space.withWhitespace(beforeLastNewline + indent);
        }
        return space;
    }

    /**
     * Checks if the Block at the given cursor position is the body of a J.Switch.
     * The cursor may have padding entries between Block and Switch, so we check
     * up to 3 levels of parents.
     */
    private static boolean isSwitchBody(Cursor blockCursor) {
        // Walk up to find if ANY ancestor is a J.Switch — but stop at the first
        // J.Block (another block means we've left the switch body scope)
        for (Cursor c = blockCursor.getParent(); c != null && c.getParent() != null; c = c.getParent()) {
            Object val = c.getValue();
            if (val instanceof J.Switch) {
                return true;
            }
            if (val instanceof J.Block) {
                // Hit another block — we're past the switch
                return false;
            }
        }
        return false;
    }
}
