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

import static org.openrewrite.PathUtils.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class MoveFile extends Recipe {
    @Nullable
    @Option(displayName = "Folder",
            description = "When using the folder option, all files / subfolders in the folder will be moved to the moveTo source path. " +
                    "Folder should be starting at root",
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

    String displayName = "Move a file";

    String description = "Move a file to a different directory. The file name will remain the same.";

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
                    Path newPath = computeNewSourcePath(((SourceFile) tree).getSourcePath().toString(), folder, fileMatcher, moveTo);
                    if (newPath != null) {
                        return ((SourceFile) tree).withSourcePath(newPath);
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }

    /**
     * Resolve the destination path for a source file given the same options that {@link MoveFile} accepts.
     * Returns {@code null} when the source file does not match, when the destination would equal the source,
     * or when the move would escape the project root. Shared with {@link CopyFile}.
     */
    static @Nullable Path computeNewSourcePath(String originalSourcePath, @Nullable String folder, @Nullable String fileMatcher, String moveTo) {
        Path sourcePath = Paths.get(separatorsToSystem(originalSourcePath));
        boolean isWindowsPath = originalSourcePath.equals(separatorsToWindows(originalSourcePath));
        boolean isUnixPath = originalSourcePath.equals(separatorsToUnix(originalSourcePath));
        boolean isFileOnRoot = sourcePath.getParent() == null;
        if (!isFileOnRoot && (isWindowsPath && isUnixPath || (!isWindowsPath && !isUnixPath))) {
            // This should never happen, but just in case
            return null;
        }

        String normalizedFolder = normalizeFolder(folder);
        String normalizedMoveTo = separatorsToUnix(moveTo);

        Path destination;
        if (normalizedFolder != null) {
            destination = getFolderTarget(sourcePath, normalizedFolder, normalizedMoveTo);
        } else {
            destination = getFilePatternTarget(sourcePath, fileMatcher, normalizedMoveTo);
        }
        if (destination != null && !destination.equals(sourcePath)) {
            destination = destination.resolve(sourcePath.getFileName().toString());
            if (!destination.equals(sourcePath)) {
                if (isFileOnRoot) {
                    return Paths.get(separatorsToSystem(destination.toString()));
                } else if (isWindowsPath) {
                    return Paths.get(separatorsToWindows(destination.toString()));
                } else {
                    return Paths.get(separatorsToUnix(destination.toString()));
                }
            }
        }
        return null;
    }

    private static @Nullable String normalizeFolder(@Nullable String folder) {
        if (StringUtils.isNullOrEmpty(folder)) {
            return null;
        }
        return separatorsToUnix(folder.startsWith("/") || folder.startsWith("\\") ? folder.substring(1) : folder);
    }

    private static @Nullable Path getFolderTarget(Path sourcePath, String folder, String destinationPattern) {
        String folderPrefix = folder;
        if (!folderPrefix.endsWith("/**")) {
            if (folderPrefix.endsWith("/")) {
                folderPrefix += "**";
            } else {
                folderPrefix += "/**";
            }
        }

        Path currentFolder = sourcePath.getParent();
        if (PathUtils.matchesGlob(sourcePath, folderPrefix) || "/**".equals(folderPrefix)) {
            String subFolders = currentFolder == null ? "" : separatorsToUnix(currentFolder.toString().substring(folder.length()));
            if (subFolders.startsWith("/") || subFolders.startsWith("\\")) {
                subFolders = subFolders.substring(1);
            }
            Path destination = null;
            if (destinationPattern.startsWith("../")) {
                destination = moveToRelativePath(Paths.get(folderPrefix.substring(0, folderPrefix.length() - 3)), destinationPattern);
            } else if (!(subFolders.equals(destinationPattern.startsWith("/") ? destinationPattern.substring(1) : destinationPattern) ||
                    subFolders.startsWith(destinationPattern.startsWith("/") ? destinationPattern.substring(1) : destinationPattern + "/"))) {
                if (destinationPattern.startsWith("/")) {
                    destination = moveToAbsolutePath(destinationPattern);
                } else {
                    destination = Paths.get(folder, destinationPattern);
                }
            }
            if (destination != null) {
                return destination.resolve(subFolders);
            } else {
                return null;
            }
        }
        return sourcePath;
    }

    private static @Nullable Path getFilePatternTarget(Path sourcePath, @Nullable String fileMatcher, String destinationPattern) {
        if (fileMatcher == null) {
            return null;
        }
        if (sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher).matches(sourcePath)) {
            Path currentFolder = sourcePath.getParent();
            if (destinationPattern.startsWith("/")) {
                return moveToAbsolutePath(destinationPattern);
            } else if (destinationPattern.startsWith("../")) {
                return moveToRelativePath(currentFolder, destinationPattern);
            } else if (currentFolder != null && !currentFolder.endsWith(destinationPattern)) {
                return currentFolder.resolve(destinationPattern);
            } else if (currentFolder == null) {
                return Paths.get(destinationPattern);
            }
        }

        return sourcePath;
    }

    private static Path moveToAbsolutePath(String moveTo) {
        return Paths.get(moveTo.substring(1));
    }

    private static @Nullable Path moveToRelativePath(@Nullable Path sourcePath, String relativePath) {
        Path moveToSourcePath = sourcePath;
        String moveToPath = relativePath;
        while (moveToPath.startsWith("../")) {
            moveToSourcePath = moveToSourcePath != null ? moveToSourcePath.getParent() : null;
            if (moveToSourcePath == null) {
                return null;
            }
            moveToPath = moveToPath.substring(3);
        }

        return moveToSourcePath != null ? moveToSourcePath.resolve(moveToPath) : null;
    }
}
