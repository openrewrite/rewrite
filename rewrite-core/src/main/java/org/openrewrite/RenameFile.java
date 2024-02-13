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
import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

@Value
@EqualsAndHashCode(callSuper = false)
public class RenameFile extends Recipe {
    @Option(displayName = "File matcher",
            description = "Matching files will be renamed. This is a glob expression.",
            example = "**/application-*.yml")
    String fileMatcher;

    @Option(displayName = "The renamed file name",
            description = "Just the file name without the folder path that precedes it.",
            example = "application.yml")
    String fileName;

    @Override
    public String getDisplayName() {
        return "Rename a file";
    }

    @Override
    public String getDescription() {
        return "Rename a file while keeping it in the same directory.";
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
                    PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher);
                    if(pathMatcher.matches(sourcePath)) {
                        return ((SourceFile) tree).withSourcePath(sourcePath.resolveSibling(fileName).normalize());
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }
}
