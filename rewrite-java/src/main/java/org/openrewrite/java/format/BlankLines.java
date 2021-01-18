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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.BlankLinesStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;

public class BlankLines extends Recipe {
    public BlankLines() {
        this.processor = BlankLinesFromCompilationUnitStyle::new;
    }

    private static class BlankLinesFromCompilationUnitStyle extends JavaIsoProcessor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            BlankLinesStyle style = cu.getStyle(BlankLinesStyle.class);
            if(style == null) {
                style = IntelliJ.blankLines();
            }
            doAfterVisit(new BlankLinesProcessor<>(style, null));
            return cu;
        }
    }
}
