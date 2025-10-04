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
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;

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

        J t = new NormalizeFormatVisitor<>(stopAfter)
                .visitNonNull(tree, p, cursor.fork());

        t = new MinimumViableSpacingVisitor<>(stopAfter)
                .visitNonNull(t, p, cursor.fork());

        t = new BlankLinesVisitor<>(getStyle(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter)
                .visitNonNull(t, p, cursor.fork());

        t = new WrappingAndBracesVisitor<>(getStyle(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter)
                .visitNonNull(t, p, cursor.fork());

        SpacesStyle spacesStyle = getStyle(SpacesStyle.class, cu, IntelliJ::spaces);
        TabsAndIndentsStyle tabsAndIndentsStyle = getStyle(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents);

        t = new SpacesVisitor<>(spacesStyle, getStyle(EmptyForInitializerPadStyle.class, cu), getStyle(EmptyForIteratorPadStyle.class, cu), stopAfter)
                .visitNonNull(t, p, cursor.fork());

        t = new NormalizeTabsOrSpacesVisitor<>(tabsAndIndentsStyle, stopAfter)
                .visitNonNull(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(tabsAndIndentsStyle, spacesStyle, stopAfter)
                .visitNonNull(t, p, cursor.fork());

        t = new NormalizeLineBreaksVisitor<>(getStyle(GeneralFormatStyle.class, cu, () -> autodetectGeneralFormatStyle(cu)), stopAfter)
                .visitNonNull(t, p, cursor.fork());

        return new RemoveTrailingWhitespaceVisitor<>(stopAfter)
                .visitNonNull(t, p, cursor.fork());
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
            JavaSourceFile t = (JavaSourceFile) new RemoveTrailingWhitespaceVisitor<>(stopAfter)
                    .visitNonNull(cu, p);

            t = (JavaSourceFile) new BlankLinesVisitor<>(getStyle(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter)
                    .visitNonNull(t, p);

            SpacesStyle spacesStyle = getStyle(SpacesStyle.class, cu, IntelliJ::spaces);
            TabsAndIndentsStyle tabsAndIndentsStyle = getStyle(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents);

            t = (JavaSourceFile) new SpacesVisitor<P>(spacesStyle, getStyle(EmptyForInitializerPadStyle.class, cu), getStyle(EmptyForIteratorPadStyle.class, cu), stopAfter)
                    .visitNonNull(t, p);

            t = (JavaSourceFile) new WrappingAndBracesVisitor<>(getStyle(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter)
                    .visitNonNull(t, p);

            t = (JavaSourceFile) new NormalizeTabsOrSpacesVisitor<>(tabsAndIndentsStyle, stopAfter)
                    .visitNonNull(t, p);

            return new TabsAndIndentsVisitor<>(tabsAndIndentsStyle, spacesStyle, stopAfter)
                    .visitNonNull(t, p);
        }
        return (J) tree;
    }

    private <S extends Style> @Nullable S getStyle(Class<S> styleClass, JavaSourceFile sourceFile) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return style;
        }
        return Style.from(styleClass, sourceFile);
    }

    private <S extends Style> S getStyle(Class<S> styleClass, JavaSourceFile sourceFile, Supplier<S> defaultStyle) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return style;
        }
        return Style.from(styleClass, sourceFile, defaultStyle);
    }
}
