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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.SpacesStyle;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.Style;

public class SpacesFromCompilationUnitStyle extends KotlinIsoVisitor<ExecutionContext> {
    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (!(tree instanceof K.CompilationUnit)) {
            return (J) tree;
        }
        K.CompilationUnit cu = (K.CompilationUnit) tree;
        SpacesStyle style = Style.from(SpacesStyle.class, cu, IntelliJ::spaces);
        return new SpacesVisitor<>(style).visitNonNull(cu, getCursor().fork());
    }
}
