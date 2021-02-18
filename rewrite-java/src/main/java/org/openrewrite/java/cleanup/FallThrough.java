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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

public class FallThrough extends Recipe {
    @Override
    public String getDisplayName() {
        return "Fall through";
    }

    @Override
    public String getDescription() {
        return "Checks for fall-through in switch statements, adding `break` statements in locations where a case contains Java code but does not have a `break`, `return`, `throw`, or `continue` statement.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FallThroughFromCompilationUnitStyle();
    }

    private static class FallThroughFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            FallThroughStyle style = cu.getStyle(FallThroughStyle.class);
            if (style == null) {
                style = FallThroughStyle.fallThroughStyle();
            }
            doAfterVisit(new FallThroughVisitor<>(style));
            return cu;
        }
    }
}
