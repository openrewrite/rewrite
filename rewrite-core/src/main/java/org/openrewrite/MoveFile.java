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
import org.openrewrite.internal.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@Value
@EqualsAndHashCode(callSuper = false)
public class MoveFile extends Recipe {
    @Nullable
    @Option(displayName = "Folder",
            description = "When using the folder option, all files / subfolders will be moved to the moveTo source path.",
            required = false,
            example = "src/main/resources/")
    String folder;

    @Nullable
    @Option(displayName = "File matcher",
            description = "Matching files will be moved. This is a glob expression.",
            required = false,
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
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (StringUtils.isNullOrEmpty(folder) && StringUtils.isNullOrEmpty(fileMatcher)) {
            return validated
                    .and(Validated.invalid("folder", folder, "folder or fileMatcher must be set"))
                    .and(Validated.invalid("fileMatcher", fileMatcher, "folder or fileMatcher must be set"));
        } else if (!(StringUtils.isNullOrEmpty(folder)) && !(StringUtils.isNullOrEmpty(fileMatcher))) {
            return validated
                    .and(Validated.invalid("folder", folder, "folder and fileMatcher cannot both be set"))
                    .and(Validated.invalid("fileMatcher", fileMatcher, "folder and fileMatcher cannot both be set"));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    String originalSourcePath = ((SourceFile) tree).getSourcePath().toString();
                    Path sourcePath = Paths.get(PathUtils.separatorsToSystem(originalSourcePath));
                    boolean isWindowsPath = originalSourcePath.equals(PathUtils.separatorsToWindows(originalSourcePath));
                    boolean isUnixPath = originalSourcePath.equals(PathUtils.separatorsToUnix(originalSourcePath));
                    if (isWindowsPath && isUnixPath || (!isWindowsPath && !isUnixPath)) {
                        // This should never happen, but just in case
                        return tree;
                    }

                    Path destination;
                    if (!StringUtils.isNullOrEmpty(folder)) {
                        destination = getFolderTarget(sourcePath);
                    } else {
                        destination = getFilePatternTarget(sourcePath);
                    }
                    if (destination != null && !destination.equals(sourcePath)) {
                        destination = destination.resolve(sourcePath.getFileName().toString());
                        if (!destination.equals(sourcePath)) {
                            if (isWindowsPath) {
                                return ((SourceFile) tree).withSourcePath(Paths.get(PathUtils.separatorsToWindows(destination.toString())));
                            } else {
                                return ((SourceFile) tree).withSourcePath(Paths.get(PathUtils.separatorsToUnix(destination.toString())));
                            }
                        }
                    }
                }
                return super.visit(tree, ctx);
            }

            private @Nullable Path getFolderTarget(Path sourcePath) {
                if (StringUtils.isNullOrEmpty(folder)) {
                    return null;
                }
                String folderPrefix = folder;
                if (!folderPrefix.endsWith("/**")) {
                    if (folderPrefix.endsWith("/")) {
                        folderPrefix = folderPrefix + "**";
                    } else {
                        folderPrefix = folderPrefix + "/**";
                    }
                }

                Path currentFolder = sourcePath.getParent();
                if (PathUtils.matchesGlob(sourcePath, folderPrefix)) {
                    String subFolders = currentFolder.toString().substring(folder.length());
                    if (subFolders.startsWith("/") || subFolders.startsWith("\\")) {
                        subFolders = subFolders.substring(1);
                    }
                    Path destination = null;
                    if (moveTo.startsWith("/")) {
                        destination = moveToAbsolutePath();
                    } else if (moveTo.startsWith("../")) {
                        destination = moveToRelativePath(Paths.get(folderPrefix.substring(0, folderPrefix.length() - 3)), moveTo);
                    } else if (!(subFolders.equals(moveTo) || subFolders.startsWith(moveTo + "/") || subFolders.startsWith(moveTo + "\\"))) {
                        destination = Paths.get(folder, moveTo);
                    }
                    if (destination != null) {
                        return destination.resolve(subFolders);
                    } else {
                        return null;
                    }
                }
                return sourcePath;
            }

            private @Nullable Path getFilePatternTarget(Path sourcePath) {
                if (fileMatcher == null) {
                    return null;
                }
                if (sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher).matches(sourcePath)) {
                    Path currentFolder = sourcePath.getParent();
                    if (moveTo.startsWith("/")) {
                        return moveToAbsolutePath();
                    } else if (moveTo.startsWith("../")) {
                        return moveToRelativePath(currentFolder, moveTo);
                    } else if (!currentFolder.endsWith(moveTo)) {
                        return currentFolder.resolve(moveTo);
                    }
                }

                return sourcePath;
            }

            private Path moveToAbsolutePath() {
                return Paths.get(moveTo.substring(1));
            }

            private @Nullable Path moveToRelativePath(Path sourcePath, String relativePath) {
                Path moveToSourcePath = sourcePath;
                String moveToPath = relativePath;
                while (moveToPath.startsWith("../")) {
                    moveToSourcePath = moveToSourcePath.getParent();
                    if (moveToSourcePath == null) {
                        return null;
                    }
                    moveToPath = moveToPath.substring(3);
                }

                return moveToSourcePath.resolve(moveToPath);
            }
        };
    }
}
