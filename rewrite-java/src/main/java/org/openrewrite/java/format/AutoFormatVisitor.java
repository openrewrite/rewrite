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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;

public class AutoFormatVisitor<P> extends JavaIsoVisitor<P> {
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
        return sourceFile instanceof J.CompilationUnit;
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        JavaSourceFile cu = (tree instanceof JavaSourceFile) ?
                (JavaSourceFile) tree :
                cursor.firstEnclosingOrThrow(JavaSourceFile.class);

        J t = new NormalizeFormatVisitor<>(stopAfter).visit(tree, p, cursor.fork());

        t = new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p, cursor.fork());

        t = new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter)
                .visit(t, p, cursor.fork());

        t = new WrappingAndBracesVisitor<>(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter)
                .visit(t, p, cursor.fork());

        t = new SpacesVisitor<>(
                Style.from(SpacesStyle.class, cu, IntelliJ::spaces),
                Style.from(EmptyForInitializerPadStyle.class, cu),
                Style.from(EmptyForIteratorPadStyle.class, cu),
                stopAfter
        ).visit(t, p, cursor.fork());

        t = new NormalizeTabsOrSpacesVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), stopAfter)
                .visit(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), stopAfter)
                .visit(t, p, cursor.fork());

        t = new NormalizeLineBreaksVisitor<>(Optional.ofNullable(Style.from(GeneralFormatStyle.class, cu))
                .orElse(autodetectGeneralFormatStyle(cu)), stopAfter)
                .visit(t, p, cursor.fork());

        t = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(t, p, cursor.fork());

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

            t = (JavaSourceFile) new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new SpacesVisitor<P>(Optional.ofNullable(
                    Style.from(SpacesStyle.class, cu)).orElse(IntelliJ.spaces()),
                    Style.from(EmptyForInitializerPadStyle.class, cu),
                    Style.from(EmptyForIteratorPadStyle.class, cu),
                    stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new WrappingAndBracesVisitor<>(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new NormalizeTabsOrSpacesVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), stopAfter)
                    .visit(t, p);

            t = (JavaSourceFile) new TabsAndIndentsVisitor<>(Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents), stopAfter)
                    .visit(t, p);

            assert t != null;
            return t;
        }
        return (J) tree;
    }
}
