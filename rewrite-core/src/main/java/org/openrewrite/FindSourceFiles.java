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

import java.nio.file.Path;


@Value
@EqualsAndHashCode(callSuper = false)
public class FindSourceFiles extends Recipe {
    transient SourcesFiles results = new SourcesFiles(this);

    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
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
                    Path sourcePath = sourceFile.getSourcePath();
                    if (StringUtils.isBlank(filePattern) || PathUtils.matchesGlob(sourcePath, normalize(filePattern))) {
                        results.insertRow(ctx, new SourcesFiles.Row(sourcePath.toString(),
                                tree.getClass().getSimpleName()));
                        return SearchResult.found(sourceFile);
                    }
                }
                return tree;
            }
        };
    }

    private static String normalize(String filePattern) {
        if (filePattern.startsWith("./") || filePattern.startsWith(".\\")) {
            return filePattern.substring(2);
        } else if (filePattern.startsWith("/") || filePattern.startsWith("\\")) {
            return filePattern.substring(1);
        }
        return filePattern;
    }
}
