/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ToBeRemoved;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class AutoFormatVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final List<NamedStyles> styles;

    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter, NamedStyles ... style) {
        this.stopAfter = stopAfter;
        this.styles = Arrays.stream(style).collect(toList());
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof J.CompilationUnit;
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        JavaSourceFile cu = (tree instanceof JavaSourceFile) ? (JavaSourceFile) tree : cursor.firstEnclosingOrThrow(JavaSourceFile.class);
        if (tree == null) {
            tree = cursor.getValue();
        }
        List<NamedStyles> activeStyles = new ArrayList<>(styles);
        activeStyles.addAll(cu.getMarkers().findAll(NamedStyles.class));

        // Format the tree in multiple passes to visitors that "enlarge" the space (Eg. first spaces, then wrapping, then indents...)
        J t = new NormalizeFormatVisitor<>(stopAfter).visitNonNull(tree, p, cursor.fork());
        t = new MinimumViableSpacingVisitor<>(stopAfter).visitNonNull(t, p, cursor.fork());
        t = new SpacesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new WrappingAndBracesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new NormalizeTabsOrSpacesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());
        t = new TabsAndIndentsVisitor<>(activeStyles, stopAfter).visitNonNull(t, p, cursor.fork());

        // With the updated tree, overwrite the original space with the newly computed space
        tree = new MergeSpacesVisitor(activeStyles).visit(tree, t, cursor.fork());

        // Then apply formatting that applies on line-endings / #lines / ...
        tree = new BlankLinesVisitor<>(activeStyles, stopAfter).visitNonNull(tree, p, cursor.fork());
        tree = new NormalizeLineBreaksVisitor<>(activeStyles, cu, stopAfter).visitNonNull(tree, p, cursor.fork());
        tree = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visitNonNull(tree, p, cursor.fork());

        if (tree instanceof JavaSourceFile) {
            return addStyleMarker((JavaSourceFile) tree, styles);
        }

        return (J) tree;
    }

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            // Avoid reformatting entire Groovy source files, or other J-derived ASTs
            // Java AutoFormat does OK for a snippet of Groovy, But whole-file reformatting is inadvisable and there is
            // currently no easy way to customize or fine-tune for Groovy
            if (!(cu instanceof J.CompilationUnit)) {
                return cu;
            }
            List<NamedStyles> activeStyles = new ArrayList<>(styles);
            activeStyles.addAll(cu.getMarkers().findAll(NamedStyles.class));

            // Format the tree in multiple passes to visitors that "enlarge" the space (Eg. first spaces, then wrapping, then indents...)
            JavaSourceFile t = (JavaSourceFile) new NormalizeFormatVisitor<>(stopAfter).visitNonNull(tree, p);
            t = (JavaSourceFile) new MinimumViableSpacingVisitor<>(stopAfter).visitNonNull(t, p);
            t = (JavaSourceFile) new SpacesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p);
            t = (JavaSourceFile) new WrappingAndBracesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p);
            t = (JavaSourceFile) new NormalizeTabsOrSpacesVisitor<>(activeStyles, stopAfter).visitNonNull(t, p);
            t = (JavaSourceFile) new TabsAndIndentsVisitor<>(activeStyles, stopAfter).visitNonNull(t, p);

            // With the updated tree, overwrite the original space with the newly computed space
            tree = new MergeSpacesVisitor(activeStyles).visit(tree, t);

            // Then apply formatting that applies on line-endings / #lines / ...
            tree = new BlankLinesVisitor<>(activeStyles, stopAfter).visitNonNull(tree, p);
            tree = new NormalizeLineBreaksVisitor<>(activeStyles, cu, stopAfter).visitNonNull(tree, p);
            tree = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visitNonNull(tree, p);

            if (tree instanceof J.CompilationUnit) {
                return addStyleMarker((JavaSourceFile) tree, styles);
            }
        }
        return (J) tree;
    }

    @ToBeRemoved(after = "30-02-2026", reason = "Replace me with org.openrewrite.style.StyleHelper.addStyleMarker now available in parent runtime")
    private static <T extends SourceFile> T addStyleMarker(T t, List<NamedStyles> styles) {
        if (!styles.isEmpty()) {
            Set<NamedStyles> newNamedStyles = new HashSet<>(styles);
            boolean styleAlreadyPresent = false;
            for (NamedStyles namedStyle : t.getMarkers().findAll(NamedStyles.class)) {
                styleAlreadyPresent = !newNamedStyles.add(namedStyle) || styleAlreadyPresent;
            }
            // As the order or NamedStyles matters, we cannot simply use addIfAbsent.
            if (!styleAlreadyPresent) {
                Markers markers = t.getMarkers().removeByType(NamedStyles.class);
                for (NamedStyles namedStyle : newNamedStyles) {
                    markers = markers.add(namedStyle);
                }

                return t.withMarkers(markers);
            }
        }
        return t;
    }
}
