/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.Style;

import static java.util.Objects.requireNonNull;

public class NormalizeTabsOrSpaces extends Recipe {

    @Override
    public String getDisplayName() {
        return "Normalize to tabs or spaces";
    }

    @Override
    public String getDescription() {
        return "Consistently use either tabs or spaces in indentation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TabsAndIndentsFromCompilationUnitStyle();
    }

    private static class TabsAndIndentsFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents);
                return new NormalizeTabsOrSpacesVisitor<>(style).visit(cu, ctx);
            }
            return (J) tree;
        }
    }
}
