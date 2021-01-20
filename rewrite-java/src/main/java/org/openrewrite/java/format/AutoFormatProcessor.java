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
import org.openrewrite.java.JavaProcessor;
import org.openrewrite.java.style.*;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Optional;

public class AutoFormatProcessor<P> extends JavaProcessor<P> {
    @Nullable
    private final List<? extends J> limitToTrees;

    public AutoFormatProcessor(@Nullable List<? extends J> limitToTrees) {
        this.limitToTrees = limitToTrees;
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        J.CompilationUnit cu = cursor.firstEnclosingOrThrow(J.CompilationUnit.class);

        J t = new BlankLinesProcessor<>(Optional.ofNullable(cu.getStyle(BlankLinesStyle.class))
                .orElse(IntelliJ.blankLines()), limitToTrees)
                .visit(tree, p, cursor);

        t = new SpacesProcessor<>(Optional.ofNullable(cu.getStyle(SpacesStyle.class))
                .orElse(IntelliJ.spaces()), limitToTrees)
                .visit(t, p, cursor);

        t = new WrappingAndBracesProcessor<>(Optional.ofNullable(cu.getStyle(WrappingAndBracesStyle.class))
                .orElse(IntelliJ.wrappingAndBraces()), limitToTrees)
                .visit(t, p, cursor);

        t = new TabsAndIndentsProcessor<>(Optional.ofNullable(cu.getStyle(TabsAndIndentsStyle.class))
                .orElse(IntelliJ.tabsAndIndents()), limitToTrees)
                .visit(t, p, cursor);

        return t;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J t = new BlankLinesProcessor<>(Optional.ofNullable(cu.getStyle(BlankLinesStyle.class))
                .orElse(IntelliJ.blankLines()), limitToTrees)
                .visit(cu, p);

        t = new SpacesProcessor<>(Optional.ofNullable(cu.getStyle(SpacesStyle.class))
                .orElse(IntelliJ.spaces()), limitToTrees)
                .visit(t, p);

        t = new WrappingAndBracesProcessor<>(Optional.ofNullable(cu.getStyle(WrappingAndBracesStyle.class))
                .orElse(IntelliJ.wrappingAndBraces()), limitToTrees)
                .visit(t, p);

        t = new TabsAndIndentsProcessor<>(Optional.ofNullable(cu.getStyle(TabsAndIndentsStyle.class))
                .orElse(IntelliJ.tabsAndIndents()), limitToTrees)
                .visit(t, p);

        return t;
    }
}
