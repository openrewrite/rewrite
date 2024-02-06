/*
 * Copyright 2022 the original author or authors.
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
public class SetFilePermissions extends Recipe {
    @Option(displayName = "File matcher",
            description = "Permissions will be applied to matching files. This is a glob expression.",
            example = "**/gradlew.bat")
    String fileMatcher;

    @Option(displayName = "Readable",
            description = "File read permission.")
    Boolean isReadable;

    @Option(displayName = "Writable",
            description = "File write permission.")
    Boolean isWritable;

    @Option(displayName = "Executable",
            description = "Files executable permission.")
    Boolean isExecutable;

    @Override
    public String getDisplayName() {
        return "Set file permission attributes";
    }

    @Override
    public String getDescription() {
        return "Set a file's read, write and executable permission attributes.";
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
                        if (sourceFile.getFileAttributes() == null) {
                            sourceFile = sourceFile.withFileAttributes(new FileAttributes(null, null, null, isReadable, isWritable, isExecutable, 0));
                        } else {
                            FileAttributes fileAttributes = sourceFile.getFileAttributes();
                            sourceFile = sourceFile.withFileAttributes(fileAttributes.withReadable(isReadable).withWritable(isWritable).withExecutable(isExecutable));
                        }
                        return sourceFile;
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }
}
