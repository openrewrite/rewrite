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
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.Style;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class WrappingAndBraces extends Recipe {
    @Override
    public String getDisplayName() {
        return "Wrapping and braces";
    }

    @Override
    public String getDescription() {
        return "Format line wraps and braces in Java code.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("RSPEC-S121", "RSPEC-S2681", "RSPEC-S3972", "RSPEC-S3973"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new WrappingAndBracesCompilationUnitStyle();
    }

    private static class WrappingAndBracesCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                WrappingAndBracesStyle style = Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces);
                return new WrappingAndBracesVisitor<>(style).visit(cu, ctx);
            }
            return (J) tree;
        }
    }

    public static <J2 extends J> J2 formatWrappingAndBraces(J j, Cursor cursor) {
        SourceFile sourceFile = cursor.firstEnclosingOrThrow(SourceFile.class);
        WrappingAndBracesStyle style = Style.from(WrappingAndBracesStyle.class, sourceFile);
        //noinspection unchecked
        return (J2) new WrappingAndBracesVisitor<>(style == null ? IntelliJ.wrappingAndBraces() : style)
                .visitNonNull(j, 0, cursor);
    }
}
