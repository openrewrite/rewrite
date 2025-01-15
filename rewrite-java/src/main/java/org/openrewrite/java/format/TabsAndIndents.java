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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import static java.util.Objects.requireNonNull;

public class TabsAndIndents extends Recipe {
    @Override
    public String getDisplayName() {
        return "Tabs and indents";
    }

    @Override
    public String getDescription() {
        return "Format tabs and indents in Java code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TabsAndIndentsFromCompilationUnitStyle();
    }

    private static class TabsAndIndentsFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                TabsAndIndentsStyle style = ((SourceFile) cu).getStyle(TabsAndIndentsStyle.class);
                if (style == null) {
                    style = IntelliJ.tabsAndIndents();
                }
                return new TabsAndIndentsVisitor<>(style).visit(tree, ctx);
            }
            return (J) tree;
        }
    }

    public static <J2 extends J> J2 formatTabsAndIndents(J j, Cursor cursor) {
        TabsAndIndentsStyle style = cursor.firstEnclosingOrThrow(SourceFile.class)
                .getStyle(TabsAndIndentsStyle.class);
        //noinspection unchecked
        return (J2) new TabsAndIndentsVisitor<>(style == null ? IntelliJ.tabsAndIndents() : style)
                .visitNonNull(j, 0, cursor);
    }
}
