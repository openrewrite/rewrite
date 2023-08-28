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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.GeneralFormatStyle;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;

public class NormalizeLineBreaks extends Recipe {

    @Override
    public String getDisplayName() {
        return "Normalize line breaks";
    }

    @Override
    public String getDescription() {
        return "Consistently use either Windows style (CRLF) or Linux style (LF) line breaks. " +
               "If no `GeneralFormatStyle` is specified this will use whichever style of line endings are more common.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LineBreaksFromCompilationUnitStyle();
    }

    private static class LineBreaksFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                GeneralFormatStyle generalFormatStyle = ((SourceFile) cu).getStyle(GeneralFormatStyle.class);
                if (generalFormatStyle == null) {
                    generalFormatStyle = autodetectGeneralFormatStyle(cu);
                }
                doAfterVisit(new NormalizeLineBreaksVisitor<>(generalFormatStyle));
            }
            return (J) tree;
        }
    }
}
