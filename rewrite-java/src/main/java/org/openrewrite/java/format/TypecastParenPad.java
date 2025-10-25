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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.TypecastParenPadStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.Style;

import static java.util.Objects.requireNonNull;

public class TypecastParenPad extends Recipe {

    @Override
    public String getDisplayName() {
        return "Typecast parentheses padding";
    }

    @Override
    public String getDescription() {
        return "Fixes whitespace padding between a typecast type identifier and the enclosing left and right parenthesis. " +
               "For example, when configured to remove spacing, `( int ) 0L;` becomes `(int) 0L;`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TypecastParenPadVisitor();
    }

    private static class TypecastParenPadVisitor extends JavaIsoVisitor<ExecutionContext> {
        SpacesStyle spacesStyle;
        TypecastParenPadStyle typecastParenPadStyle;

        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                SourceFile cu = (SourceFile) requireNonNull(tree);
                spacesStyle = Style.from(SpacesStyle.class, cu, IntelliJ::spaces);
                typecastParenPadStyle = Style.from(TypecastParenPadStyle.class, cu, Checkstyle::typecastParenPadStyle);
                spacesStyle = spacesStyle.withWithin(spacesStyle.getWithin().withTypeCastParentheses(typecastParenPadStyle.getSpace()));
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
            J.TypeCast tc = super.visitTypeCast(typeCast, ctx);
            return (J.TypeCast) new SpacesVisitor<>(spacesStyle, null, null, tc)
                    .visitNonNull(tc, ctx, getCursor().getParentTreeCursor().fork());
        }
    }

}
