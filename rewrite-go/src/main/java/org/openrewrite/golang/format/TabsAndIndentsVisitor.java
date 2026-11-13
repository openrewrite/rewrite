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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

public class TabsAndIndentsVisitor<P> extends GolangVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @Nullable
    private String indentUnit;

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
        // Build indentation: newlines preserved, then one indent unit per depth level
        String ws = space.getWhitespace();
        int lastNewline = ws.lastIndexOf('\n');
        if (lastNewline >= 0) {
            String beforeLastNewline = ws.substring(0, lastNewline + 1);
            String unit = indentUnit();
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append(unit);
            }
            return space.withWhitespace(beforeLastNewline + indent);
        }
        return space;
    }

    /**
     * The indentation string used for a single level of nesting, detected from the
     * source file's existing style so that space-indented sources stay space-indented
     * and tab-indented (canonical gofmt) sources stay tab-indented. Defaults to a tab
     * when the source has no indentation to sample.
     */
    private String indentUnit() {
        if (indentUnit == null) {
            indentUnit = detectIndentUnit(getCursor().firstEnclosing(SourceFile.class));
        }
        return indentUnit;
    }

    private static String detectIndentUnit(@Nullable Tree cu) {
        if (cu == null) {
            return "\t";
        }
        IndentUnitCollector collector = new IndentUnitCollector();
        collector.visit(cu, 0);
        return collector.indentUnit();
    }

    private static class IndentUnitCollector extends GolangVisitor<Integer> {
        private boolean sawTab;
        private int spaceIndentGcd;

        @Override
        public Space visitSpace(Space space, Space.Location loc, Integer p) {
            String ws = space.getWhitespace();
            int lastNewline = ws.lastIndexOf('\n');
            if (lastNewline >= 0) {
                String indent = ws.substring(lastNewline + 1);
                if (!indent.isEmpty()) {
                    if (indent.indexOf('\t') >= 0) {
                        sawTab = true;
                    } else {
                        // Fold every observed space-indent width into a running GCD so a
                        // 4-space file (seen as 4, 8, 12, ...) resolves to 4 while an
                        // outlier line can't drag the unit below the true step, the way a
                        // plain minimum would.
                        spaceIndentGcd = gcd(spaceIndentGcd, indent.length());
                    }
                }
            }
            return space;
        }

        private String indentUnit() {
            if (sawTab || spaceIndentGcd == 0) {
                return "\t";
            }
            StringBuilder unit = new StringBuilder();
            for (int i = 0; i < normalizeIndentWidth(spaceIndentGcd); i++) {
                unit.append(' ');
            }
            return unit.toString();
        }

        private static int normalizeIndentWidth(int gcd) {
            if (gcd == 2 || gcd == 4 || gcd == 8) {
                return gcd;
            }
            if (gcd % 4 == 0) {
                return 4;
            }
            if (gcd % 2 == 0) {
                return 2;
            }
            return 4;
        }

        private static int gcd(int a, int b) {
            while (b != 0) {
                int t = b;
                b = a % b;
                a = t;
            }
            return a;
        }
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
