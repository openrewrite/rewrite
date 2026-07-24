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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.zig.ZigVisitor;
import org.openrewrite.zig.tree.Zig;

/**
 * Applies zig fmt-style formatting to Zig source code.
 * Chains specialized sub-visitors in sequence: line break normalization,
 * blank lines, indentation, and trailing whitespace removal.
 */
public class AutoFormatVisitor<P> extends ZigVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Zig.CompilationUnit;
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        return doFormat(tree, p, cursor);
    }

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return (J) defaultValue(null, p);
        }
        return doFormat(tree, p, new Cursor(getCursor(), tree));
    }

    private J doFormat(Tree tree, P p, Cursor cursor) {
        // zig fmt formatting passes (order matters):
        // 1. Normalize line endings first so subsequent visitors see consistent \n
        J t = new NormalizeLineBreaksVisitor<P>(stopAfter).visitNonNull(tree, p, cursor.fork());
        // 2. Collapse blank lines (now that \r\n is normalized)
        t = new BlankLinesVisitor<P>(stopAfter).visitNonNull(t, p, cursor.fork());
        // 3. Fix indentation (4 spaces at correct depth)
        t = new TabsAndIndentsVisitor<P>(stopAfter).visitNonNull(t, p, cursor.fork());
        // 4. Strip trailing whitespace
        t = new RemoveTrailingWhitespaceVisitor<P>(stopAfter).visitNonNull(t, p, cursor.fork());
        return t;
    }
}
