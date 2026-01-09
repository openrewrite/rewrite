/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.format;


import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.style.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;

import static java.util.Objects.requireNonNull;

public class AutoFormatVisitor<P> extends KotlinIsoVisitor<P> {
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
        JavaSourceFile cu = (tree instanceof JavaSourceFile) ?
                (JavaSourceFile) tree :
                cursor.firstEnclosingOrThrow(JavaSourceFile.class);

        tree = new ImportReorderingVisitor<>().visitNonNull(tree, p, cursor.fork());

        // Format the tree in multiple passes to visitors that "enlarge" the space (Eg. first spaces, then wrapping, then indents...)
        J t = new NormalizeFormatVisitor<>(stopAfter).visit(tree, p, cursor.fork());
        t = new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p, cursor.fork());
        t = new WrappingAndBracesVisitor<>(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter).visit(t, p, cursor.fork());
        t = new SpacesVisitor<>(Style.from(SpacesStyle.class, cu, IntelliJ::spaces), stopAfter).visit(t, p, cursor.fork());
        t = new NormalizeTabsOrSpacesVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), stopAfter).visit(t, p, cursor.fork());
        t = new TabsAndIndentsVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter).visit(t, p, cursor.fork());

        // With the updated tree, overwrite the original space with the newly computed space
        tree = new MergeSpacesVisitor(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces)).visit(tree, t, cursor.fork());
        tree = new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter).visit(tree, p, cursor.fork());
        tree = new NormalizeLineBreaksVisitor<>(Style.from(GeneralFormatStyle.class, cu, () -> new GeneralFormatStyle(false)), stopAfter).visit(tree, p, cursor.fork());
        tree = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(tree, p, cursor.fork());

        return (J) tree;
    }

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            if (!(cu instanceof K.CompilationUnit)) {
                return cu;
            }

            tree = new ImportReorderingVisitor<>().visitNonNull(tree, p);
            tree = new TrailingCommaVisitor<>(IntelliJ.other().getUseTrailingComma()).visitNonNull(tree, p);

            JavaSourceFile t = (JavaSourceFile) new NormalizeFormatVisitor<>(stopAfter).visit(tree, p);
            t = (JavaSourceFile) new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p);
            t = (JavaSourceFile) new WrappingAndBracesVisitor<>(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter).visit(t, p);
            t = (JavaSourceFile) new SpacesVisitor<>(Style.from(SpacesStyle.class, cu, IntelliJ::spaces), stopAfter).visit(t, p);
            t = (JavaSourceFile) new NormalizeTabsOrSpacesVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), stopAfter).visit(t, p);
            t = (JavaSourceFile) new TabsAndIndentsVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter).visit(t, p);

            tree = new MergeSpacesVisitor(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces)).visit(tree, t);
            tree = new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter).visit(tree, p);
            tree = new NormalizeLineBreaksVisitor<>(Style.from(GeneralFormatStyle.class, cu, () -> new GeneralFormatStyle(false)), stopAfter).visit(tree, p);
            tree = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(tree, p);

            // With the updated tree, overwrite the original space with the newly computed space
            return (J) tree;
        }
        return (J) tree;
    }
}
