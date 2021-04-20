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

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.*;
import org.openrewrite.java.tree.J;

import java.util.Optional;

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
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        J.CompilationUnit cu = cursor.firstEnclosingOrThrow(J.CompilationUnit.class);

        J t = new NormalizeFormatVisitor<>().visit(tree, p, cursor);

        t = new MinimumViableSpacingVisitor<>().visit(t, p, cursor);

        t = new RemoveTrailingWhitespaceVisitor<>().visit(t, p, cursor);

        t = new BlankLinesVisitor<>(Optional.ofNullable(cu.getStyle(BlankLinesStyle.class))
                .orElse(IntelliJ.blankLines()), stopAfter)
                .visit(t, p, cursor);

        t = new SpacesVisitor<>(Optional.ofNullable(cu.getStyle(SpacesStyle.class))
                .orElse(IntelliJ.spaces()), stopAfter)
                .visit(t, p, cursor);

        t = new WrappingAndBracesVisitor<>(Optional.ofNullable(cu.getStyle(WrappingAndBracesStyle.class))
                .orElse(IntelliJ.wrappingAndBraces()), stopAfter)
                .visit(t, p, cursor);

        t = new TabsAndIndentsVisitor<>(Optional.ofNullable(cu.getStyle(TabsAndIndentsStyle.class))
                .orElse(IntelliJ.tabsAndIndents()), stopAfter)
                .visit(t, p, cursor);

        return t;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit t = (J.CompilationUnit) new BlankLinesVisitor<>(Optional.ofNullable(cu.getStyle(BlankLinesStyle.class))
                .orElse(IntelliJ.blankLines()), stopAfter)
                .visit(cu, p);

        t = (J.CompilationUnit) new SpacesVisitor<>(Optional.ofNullable(cu.getStyle(SpacesStyle.class))
                .orElse(IntelliJ.spaces()), stopAfter)
                .visit(t, p);

        t = (J.CompilationUnit) new WrappingAndBracesVisitor<>(Optional.ofNullable(cu.getStyle(WrappingAndBracesStyle.class))
                .orElse(IntelliJ.wrappingAndBraces()), stopAfter)
                .visit(t, p);

        t = (J.CompilationUnit) new TabsAndIndentsVisitor<>(Optional.ofNullable(cu.getStyle(TabsAndIndentsStyle.class))
                .orElse(IntelliJ.tabsAndIndents()), stopAfter)
                .visit(t, p);

        assert t != null;
        return t;
    }
}
