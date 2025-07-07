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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.style.OmitParenthesesStyle;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class OmitParenthesesFormat extends Recipe {
    @Override
    public String getDisplayName() {
        return "Stylize Groovy code to omit parentheses";
    }

    @Override
    public String getDescription() {
        return "Omit parentheses for last argument lambdas in Groovy code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new OmitParenthesesFromCompilationUnitStyle();
    }

    private static class OmitParenthesesFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                SourceFile cu = (SourceFile) requireNonNull(tree);
                OmitParenthesesStyle style = Optional.ofNullable(cu.getStyle(OmitParenthesesStyle.class)).orElse(OmitParenthesesStyle.DEFAULT);
                if (style.getLastArgumentLambda()) {
                    doAfterVisit(new OmitParenthesesForLastArgumentLambda().getVisitor());
                }
            }
            return super.visit(tree, ctx);
        }
    }
}
