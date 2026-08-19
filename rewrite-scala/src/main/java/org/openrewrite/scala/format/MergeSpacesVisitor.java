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
package org.openrewrite.scala.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.tree.S;
import org.openrewrite.style.NamedStyles;

import java.util.List;

/**
 * Merges the spaces from a formatted Scala tree into its original counterpart.
 */
public class MergeSpacesVisitor extends org.openrewrite.java.format.MergeSpacesVisitor {

    public MergeSpacesVisitor(List<NamedStyles> styles) {
        super(styles);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, Object o) {
        if (o instanceof S.ExpressionStatement && !(tree instanceof S.ExpressionStatement)) {
            // S.ExpressionStatement#acceptScala unwraps the visited tree but forwards its wrapper as context.
            o = ((S.ExpressionStatement) o).getExpression();
        }
        return super.visit(tree, o);
    }
}
