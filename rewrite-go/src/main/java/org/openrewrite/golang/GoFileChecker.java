/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.marker.SearchResult;

/**
 * A precondition that matches Go source files ({@code .go}). Because
 * {@link Go.CompilationUnit} also implements {@code JavaSourceFile}, the generic
 * {@code JavaFileChecker} would match Go files too; this checker gates strictly
 * on the Go compilation unit so a recipe runs only on Go source.
 * <p>
 * Note this does not match {@code go.mod} or {@code go.sum}, which are modeled by
 * their own trees ({@code GoMod} / {@code GoSum}).
 */
public class GoFileChecker<P> extends TreeVisitor<Tree, P> {

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, P p) {
        if (tree instanceof Go.CompilationUnit) {
            return SearchResult.found(tree);
        }
        return tree;
    }
}
