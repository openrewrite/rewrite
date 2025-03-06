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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.style.*;
import org.openrewrite.style.GeneralFormatStyle;

import java.util.Optional;

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

        J t = new NormalizeFormatVisitor<>(stopAfter).visit(tree, p, cursor.fork());

        t = new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p, cursor.fork());

        t = new BlankLinesVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(BlankLinesStyle.class))
                .orElse(IntelliJ.blankLines()), stopAfter)
                .visit(t, p, cursor.fork());

        t = new WrappingAndBracesVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(WrappingAndBracesStyle.class))
                .orElse(IntelliJ.wrappingAndBraces()), stopAfter)
                .visit(t, p, cursor.fork());

        t = new SpacesVisitor<>(
                Optional.ofNullable(((SourceFile) cu).getStyle(SpacesStyle.class)).orElse(IntelliJ.spaces()),
                stopAfter
        ).visit(t, p, cursor.fork());

        t = new NormalizeTabsOrSpacesVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(TabsAndIndentsStyle.class))
                .orElse(IntelliJ.tabsAndIndents()), stopAfter)
                .visit(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(
                Optional.ofNullable(((SourceFile) cu).getStyle(TabsAndIndentsStyle.class)).orElse(IntelliJ.tabsAndIndents()),
                Optional.ofNullable(((SourceFile) cu).getStyle(WrappingAndBracesStyle.class)).orElse(IntelliJ.wrappingAndBraces()),
                stopAfter
        ).visit(t, p, cursor.fork());

        t = new NormalizeLineBreaksVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(GeneralFormatStyle.class))
                .orElse(new GeneralFormatStyle(false)), stopAfter)
                .visit(t, p, cursor.fork());

        t = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(t, p, cursor.fork());

        t = new ImportReorderingVisitor<>().visit(t, p, cursor.fork());
        return t;
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

            JavaSourceFile t = (JavaSourceFile) new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(cu, p);

            t = (JavaSourceFile) new BlankLinesVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(BlankLinesStyle.class))
                    .orElse(IntelliJ.blankLines()), stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new SpacesVisitor<P>(Optional.ofNullable(
                    ((SourceFile) cu).getStyle(SpacesStyle.class)).orElse(IntelliJ.spaces()),
                    stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new WrappingAndBracesVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(WrappingAndBracesStyle.class))
                    .orElse(IntelliJ.wrappingAndBraces()), stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new NormalizeTabsOrSpacesVisitor<>(Optional.ofNullable(((SourceFile) cu).getStyle(TabsAndIndentsStyle.class))
                    .orElse(IntelliJ.tabsAndIndents()), stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new TabsAndIndentsVisitor<>(
                    Optional.ofNullable(((SourceFile) cu).getStyle(TabsAndIndentsStyle.class)).orElse(IntelliJ.tabsAndIndents()),
                    Optional.ofNullable(((SourceFile) cu).getStyle(WrappingAndBracesStyle.class)).orElse(IntelliJ.wrappingAndBraces()),
                    stopAfter
            ).visit(t, p);

            t = (JavaSourceFile) new TrailingCommaVisitor<>(IntelliJ.other().getUseTrailingComma()).visit(t, p);

            assert t != null;
            return t;
        }
        return (J) tree;
    }
}
