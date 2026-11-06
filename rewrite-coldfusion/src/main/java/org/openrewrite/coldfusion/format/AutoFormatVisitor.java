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
package org.openrewrite.coldfusion.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.FormattingListener;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.style.*;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Formats CFML by running each formatting visitor in turn, with the styles attached to the file, which the CLI
 * detects from the repository, or CFFormat's defaults. Each pass leaves alone the whitespace a template writes to its
 * output.
 */
public class AutoFormatVisitor<P> extends ColdFusionIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final List<NamedStyles> styles;

    public AutoFormatVisitor() {
        this(null);
    }

    /**
     * @param styles Styles that take precedence, for each style class, over those attached to the file.
     */
    public AutoFormatVisitor(@Nullable Tree stopAfter, NamedStyles... styles) {
        this.stopAfter = stopAfter;
        this.styles = Arrays.asList(styles);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p, Cursor cursor) {
        if (tree == null) {
            return null;
        }
        JavaSourceFile cu = tree instanceof JavaSourceFile ? (JavaSourceFile) tree : cursor.firstEnclosingOrThrow(JavaSourceFile.class);
        J t = new NormalizeFormatVisitor<P>(stopAfter).visit(tree, p, cursor.fork());
        t = new MinimumViableSpacingVisitor<P>(stopAfter).visit(t, p, cursor.fork());
        t = new WrappingAndBracesVisitor<P>(style(WrappingAndBracesStyle.class, cu, CFFormat::wrappingAndBraces), stopAfter).visit(t, p, cursor.fork());
        t = new SpacesVisitor<P>(style(SpacesStyle.class, cu, CFFormat::spaces), stopAfter).visit(t, p, cursor.fork());
        t = new BlankLinesVisitor<P>(style(BlankLinesStyle.class, cu, CFFormat::blankLines), stopAfter).visit(t, p, cursor.fork());
        TabsAndIndentsStyle tabsAndIndents = style(TabsAndIndentsStyle.class, cu, CFFormat::tabsAndIndents);
        t = new NormalizeTabsOrSpacesVisitor<P>(tabsAndIndents, stopAfter).visit(t, p, cursor.fork());
        t = new TabsAndIndentsVisitor<P>(tabsAndIndents, stopAfter).visit(t, p, cursor.fork());
        t = new NormalizeLineBreaksVisitor<P>(style(GeneralFormatStyle.class, cu, () -> GeneralFormatStyle.DEFAULT), stopAfter).visit(t, p, cursor.fork());
        return new RemoveTrailingWhitespaceVisitor<P>(tabsAndIndents, stopAfter).visit(t, p, cursor.fork());
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (!(tree instanceof Cf.CompilationUnit)) {
            //noinspection unchecked
            return (J) tree;
        }
        Cf.CompilationUnit cu = (Cf.CompilationUnit) tree;
        FormattingListener.beforeFormat(cu, stopAfter, getCursor());
        J t = new NormalizeFormatVisitor<P>(stopAfter).visit(cu, p);
        t = new MinimumViableSpacingVisitor<P>(stopAfter).visit(t, p);
        t = new WrappingAndBracesVisitor<P>(style(WrappingAndBracesStyle.class, cu, CFFormat::wrappingAndBraces), stopAfter).visit(t, p);
        t = new SpacesVisitor<P>(style(SpacesStyle.class, cu, CFFormat::spaces), stopAfter).visit(t, p);
        t = new BlankLinesVisitor<P>(style(BlankLinesStyle.class, cu, CFFormat::blankLines), stopAfter).visit(t, p);
        TabsAndIndentsStyle tabsAndIndents = style(TabsAndIndentsStyle.class, cu, CFFormat::tabsAndIndents);
        t = new NormalizeTabsOrSpacesVisitor<P>(tabsAndIndents, stopAfter).visit(t, p);
        t = new TabsAndIndentsVisitor<P>(tabsAndIndents, stopAfter).visit(t, p);
        t = new NormalizeLineBreaksVisitor<P>(style(GeneralFormatStyle.class, cu, () -> GeneralFormatStyle.DEFAULT), stopAfter).visit(t, p);
        t = new RemoveTrailingWhitespaceVisitor<P>(tabsAndIndents, stopAfter).visit(t, p);
        return t instanceof Cf.CompilationUnit ? StyleHelper.addStyleMarker((Cf.CompilationUnit) t, styles) : t;
    }

    private <S extends Style> S style(Class<S> styleClass, JavaSourceFile cu, Supplier<S> defaultStyle) {
        // later styles take precedence
        List<NamedStyles> all = new ArrayList<>(cu.getMarkers().findAll(NamedStyles.class));
        all.addAll(styles);
        S s = NamedStyles.merge(styleClass, all);
        return s == null ? defaultStyle.get() : s;
    }
}
