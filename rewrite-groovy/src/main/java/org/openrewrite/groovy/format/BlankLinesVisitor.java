/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.NamedStyles;

import java.util.List;

public class BlankLinesVisitor<P> extends org.openrewrite.java.format.BlankLinesVisitor<P> {
    public BlankLinesVisitor(List<NamedStyles> styles, @Nullable Tree stopAfter) {
        super(styles, stopAfter);
    }

    @Override
    public J visit(@Nullable Tree tree, P p) {
        J j = super.visit(tree, p);
        if (j instanceof G.AnnotatedImport) {
            G.AnnotatedImport annotated = (G.AnnotatedImport) j;
            JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
            if (!cu.getImports().isEmpty() && annotated.getImportDeclaration().equals(cu.getImports().get(0)) &&
                cu.getPackageDeclaration() == null && cu.getPrefix().equals(Space.EMPTY)) {
                return annotated.withPrefix(annotated.getPrefix().withWhitespace(""));
            }
            return annotated;
        }
        return j;
    }

    @Override
    public J.Import visitImport(J.Import import_, P p) {
        if (getCursor().getParentTreeCursor().getValue() instanceof G.AnnotatedImport) {
            // Blank lines belong to the wrapper. The import's children contain no declarations to format.
            return import_;
        }
        return super.visitImport(import_, p);
    }
}
