/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kotlin.table.KotlinSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindKotlinSources extends Recipe {
    transient KotlinSourceFile kotlinSourceFile = new KotlinSourceFile(this);

    @Option(displayName = "Find Kotlin compilation units",
            description = "Limit the search results to Kotlin CompilationUnits.",
            required = false)
    @Nullable
    Boolean markCompilationUnits;

    @Override
    public String getDisplayName() {
        return "Find Kotlin sources and collect data metrics";
    }

    @Override
    public String getDescription() {
        return "Use data table to collect source files types and counts of files with extensions `.kt`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    if (Boolean.TRUE.equals(markCompilationUnits) && !(tree instanceof K.CompilationUnit)) {
                        return tree;
                    }
                    SourceFile sourceFile = (SourceFile) tree;
                    if (sourceFile.getSourcePath().toString().endsWith(".kt")) {
                        KotlinSourceFile.SourceFileType sourceFileType = getSourceFileType(sourceFile);
                        kotlinSourceFile.insertRow(ctx, new KotlinSourceFile.Row(sourceFile.getSourcePath().toString(), sourceFileType));
                        return SearchResult.found(sourceFile);
                    }
                }
                return tree;
            }

            private @Nullable KotlinSourceFile.SourceFileType getSourceFileType(SourceFile sourceFile) {
                KotlinSourceFile.SourceFileType sourceFileType = null;
                if (sourceFile instanceof K.CompilationUnit) {
                    sourceFileType = KotlinSourceFile.SourceFileType.Kotlin;
                } else if (sourceFile instanceof Quark) {
                    sourceFileType = KotlinSourceFile.SourceFileType.Quark;
                } else if (sourceFile instanceof PlainText) {
                    sourceFileType = KotlinSourceFile.SourceFileType.PlainText;
                }
                return sourceFileType;
            }
        };
    }
}
