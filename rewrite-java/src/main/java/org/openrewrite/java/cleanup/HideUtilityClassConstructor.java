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
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

@Incubating(since = "7.0.0")
public class HideUtilityClassConstructor extends Recipe {

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HideUtilityClassConstructorFromCompilationUnitStyle();
    }

    private static class HideUtilityClassConstructorFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            HideUtilityClassConstructorStyle style = cu.getStyle(HideUtilityClassConstructorStyle.class);
            if (style == null) {
                style = HideUtilityClassConstructorStyle.hideUtilityClassConstructorStyle();
            }
            doAfterVisit(new HideUtilityClassConstructorVisitor<>(style));
            return super.visitCompilationUnit(cu, executionContext);
        }
    }
}
