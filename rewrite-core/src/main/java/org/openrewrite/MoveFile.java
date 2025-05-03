/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

@Value
@EqualsAndHashCode(callSuper = false)
public class MoveFile extends Recipe {
    @Option(displayName = "File matcher",
            description = "Matching files will be renamed. This is a glob expression.",
            example = "**/*.yml")
    String fileMatcher;

    @Option(displayName = "Move to",
            description = "Either a relative or absolute path. If relative, it is relative to the current file's directory.",
            example = "../yamls/")
    String moveTo;

    @Override
    public String getDisplayName() {
        return "Move a file";
    }

    @Override
    public String getDescription() {
        return "Move a file to a different directory. The file name will remain the same.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    Path sourcePath = sourceFile.getSourcePath();
                    Path currentFolder = sourcePath.getParent();
                    String fileName = sourcePath.getFileName().toString();
                    PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher);
                    if (pathMatcher.matches(sourcePath)) {
                        Path moveToSourcePath = null;
                        String moveToPath = moveTo;
                        if (moveTo.startsWith("/")) {
                            moveToSourcePath = Paths.get("");
                            moveToPath = moveTo.substring(1);
                        } else if (moveTo.startsWith("../")) {
                            moveToSourcePath = currentFolder;
                            while (moveToPath.startsWith("../")) {
                                moveToSourcePath = moveToSourcePath.getParent();
                                if (moveToSourcePath == null) {
                                    break;
                                }
                                moveToPath = moveToPath.substring(3);
                            }
                        } else if (!currentFolder.endsWith(moveTo)) {
                            moveToSourcePath = currentFolder;
                        }
                        if (moveToSourcePath != null) {
                            moveToSourcePath = moveToSourcePath.resolve(moveToPath).resolve(fileName);
                            if (!moveToSourcePath.equals(sourcePath)) {
                                return ((SourceFile) tree).withSourcePath(moveToSourcePath.normalize());
                            }
                        }
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }
}
