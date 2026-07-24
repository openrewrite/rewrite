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
package org.openrewrite.zig.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.zig.ZigVisitor;
import org.openrewrite.zig.tree.Zig;

public class TabsAndIndentsVisitor<P> extends ZigVisitor<P> {
    private static final String INDENT = "    "; // Zig uses 4 spaces

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
        // Rules:
        // - J.Block adds depth, EXCEPT switch body blocks
        // - J.Case adds depth (for case body statements)
        // - Zig.SwitchProng adds depth
        //
        // This produces:
        //   fn body:        4 spaces  (fn body Block)
        //   switch prong:   4 spaces  (fn body Block, skip switch body Block)
        //   prong body:     8 spaces  (fn body Block + SwitchProng)
        //   nested blocks:  +4 per Block
        int depth = 0;
        // Start from PARENT -- the current cursor value is the node whose prefix we're
        // formatting, so it shouldn't count toward its own indentation.
        for (Cursor c = getCursor().getParent(); c != null && c.getParent() != null; c = c.getParent()) {
            Object val = c.getValue();
            if (val instanceof J.Block) {
                // Skip the switch body block -- zig fmt does not indent case labels
                // relative to the switch body brace.
                if (isSwitchBody(c)) {
                    continue;
                }
                depth++;
            } else if (val instanceof J.Case) {
                depth++;
            } else if (val instanceof Zig.SwitchProng) {
                depth++;
            }
        }
        // Build indentation: newlines preserved, then 4 spaces per depth level
        String ws = space.getWhitespace();
        int lastNewline = ws.lastIndexOf('\n');
        if (lastNewline >= 0) {
            String beforeLastNewline = ws.substring(0, lastNewline + 1);
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append(INDENT);
            }
            return space.withWhitespace(beforeLastNewline + indent);
        }
        return space;
    }

    /**
     * Checks if the Block at the given cursor position is the body of a J.Switch.
     * The cursor may have padding entries between Block and Switch, so we check
     * up to several levels of parents.
     */
    private static boolean isSwitchBody(Cursor blockCursor) {
        // Walk up to find if ANY ancestor is a J.Switch -- but stop at the first
        // J.Block (another block means we've left the switch body scope)
        for (Cursor c = blockCursor.getParent(); c != null && c.getParent() != null; c = c.getParent()) {
            Object val = c.getValue();
            if (val instanceof J.Switch) {
                return true;
            }
            if (val instanceof J.Block) {
                // Hit another block -- we're past the switch
                return false;
            }
        }
        return false;
    }
}
