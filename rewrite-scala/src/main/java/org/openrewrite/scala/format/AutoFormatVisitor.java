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
package org.openrewrite.scala.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.format.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.style.NamedStyles;

import java.util.List;

public class AutoFormatVisitor<P> extends ScalaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @SuppressWarnings("unused")
    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        if (tree == null) {
            tree = cursor.getValue();
        }
        JavaSourceFile cu = (tree instanceof JavaSourceFile) ?
                (JavaSourceFile) tree :
                cursor.firstEnclosingOrThrow(JavaSourceFile.class);

        List<NamedStyles> activeStyles = cu.getMarkers().findAll(NamedStyles.class);

        // Format the tree in multiple passes to visitors that "enlarge" the space (Eg. first spaces, then wrapping, then indents...)
        J t = new NormalizeFormatVisitor<>(stopAfter).visitNonNull(tree, p, cursor.fork());
        t = new SpacesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new WrappingAndBracesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new NormalizeTabsOrSpacesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new TabsAndIndentsVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new MinimumViableSpacingVisitor<>(stopAfter).visitNonNull(t, p, cursor.fork());

        // With the updated tree, overwrite the original space with the newly computed space
        tree = new MergeSpacesVisitor(activeStyles).visitNonNull(tree, t, cursor.fork());

        // Then apply formatting that applies on line-endings / #lines / ...
        tree = new BlankLinesVisitor<>(activeStyles, stopAfter).visitNonNull(tree, p, cursor.fork());
        tree = new NormalizeLineBreaksVisitor<>(activeStyles, cu, stopAfter).visitNonNull(tree, p, cursor.fork());
        tree = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visitNonNull(tree, p, cursor.fork());

        return (J) tree;
    }
}
