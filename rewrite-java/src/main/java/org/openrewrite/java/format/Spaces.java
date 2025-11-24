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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.EmptyForInitializerPadStyle;
import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.Style;

public class Spaces extends Recipe {

    @Override
    public String getDisplayName() {
        return "Spaces";
    }

    @Override
    public String getDescription() {
        return "Format whitespace in Java code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof J.CompilationUnit;
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    return new SpacesVisitor<>(
                            Style.from(SpacesStyle.class, cu, IntelliJ::spaces),
                            Style.from(EmptyForInitializerPadStyle.class, cu),
                            Style.from(EmptyForIteratorPadStyle.class, cu))
                            .visit(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        };
    }

    public static <J2 extends J> J2 formatSpaces(J j, Cursor cursor) {
        SourceFile cu = cursor.firstEnclosingOrThrow(SourceFile.class);
        SpacesStyle style = Style.from(SpacesStyle.class, cu);
        //noinspection unchecked
        return (J2) new SpacesVisitor<>(style == null ? IntelliJ.spaces() : style,
                Style.from(EmptyForInitializerPadStyle.class, cu),
                Style.from(EmptyForIteratorPadStyle.class, cu)).visitNonNull(j, 0, cursor);
    }
}
