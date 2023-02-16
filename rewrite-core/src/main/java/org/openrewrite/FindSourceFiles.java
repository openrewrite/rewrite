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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.SourcesFiles;


@Value
@EqualsAndHashCode(callSuper = true)
public class FindSourceFiles extends Recipe {
    transient SourcesFiles results = new SourcesFiles(this);

    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root).",
            example = ".github/workflows/*.yml")
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Find files";
    }

    @Override
    public String getDescription() {
        return "Find files by source path.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Nullable
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    String sourcePath = sourceFile.getSourcePath().toString();
                    if (StringUtils.matchesGlob(sourcePath.matches("^\\.?[/\\\\]") ? sourcePath :
                            "./" + sourcePath, filePattern)) {
                        results.insertRow(ctx, new SourcesFiles.Row(sourcePath));
                        return SearchResult.found(sourceFile);
                    }
                }
                return tree;
            }
        };
    }
}
