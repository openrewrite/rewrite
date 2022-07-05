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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.BlankLinesStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

public class BlankLines extends Recipe {
    @Override
    public String getDisplayName() {
        return "Blank lines";
    }

    @Override
    public String getDescription() {
        return "Add and/or remove blank lines.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new BlankLinesFromCompilationUnitStyle();
    }

    private static class BlankLinesFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
            BlankLinesStyle style = ((SourceFile) cu).getStyle(BlankLinesStyle.class);
            if(style == null) {
                style = IntelliJ.blankLines();
            }
            doAfterVisit(new BlankLinesVisitor<>(style));
            return cu;
        }
    }

    public static <J2 extends J> J2 formatBlankLines(J j, Cursor cursor) {
        BlankLinesStyle style = cursor.firstEnclosingOrThrow(SourceFile.class)
                .getStyle(BlankLinesStyle.class);
        //noinspection unchecked
        return (J2) new BlankLinesVisitor<>(style == null ? IntelliJ.blankLines() : style)
                .visitNonNull(j, 0, cursor);
    }
}
