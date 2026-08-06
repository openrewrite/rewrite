/*
 * Copyright 2026 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class CopyFile extends ScanningRecipe<CopyFile.Accumulator> {

    @Nullable
    @Option(displayName = "Folder",
            description = "When using the folder option, all files / subfolders in the folder will be copied to the copyTo source path. " +
                    "Folder should be starting at root",
            required = false,
            example = "src/main/resources/")
    String folder;

    @Nullable
    @Option(displayName = "File matcher",
            description = "Matching files will be copied. This is a glob expression.",
            required = false,
            example = "**/*.yml")
    String fileMatcher;

    @Option(displayName = "Copy to",
            description = "Either a relative or absolute path. If relative, it is relative to the current file's directory. " +
                    "The original file is left in place.",
            example = "../yamls/")
    String copyTo;

    @Nullable
    @Option(displayName = "Destination filename",
            description = "Optional new filename for the copy. When set, the copy is written with this name in the resolved destination " +
                    "folder instead of keeping the source file's name. Only applies when `fileMatcher` is used.",
            required = false,
            example = "test.yml")
    String destinationFilename;

    String displayName = "Copy a file";

    String description = "Copy a file to a different directory, preserving the original. " +
            "The file name will remain the same unless `destinationFilename` is set. " +
            "The copy inherits the source file's markers (including type attribution for language sources) with only its id and path changed.";

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
        if (!StringUtils.isNullOrEmpty(destinationFilename) && !StringUtils.isNullOrEmpty(folder)) {
            return validated.and(Validated.invalid("destinationFilename", destinationFilename,
                    "destinationFilename can only be used with fileMatcher, not folder"));
        }
        return validated;
    }

    public static class Accumulator {
        final Map<Path, SourceFile> copies = new LinkedHashMap<>();
        final Set<Path> existingPaths = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    acc.existingPaths.add(sourceFile.getSourcePath());
                    Path destination = MoveFile.computeNewSourcePath(sourceFile.getSourcePath().toString(), folder, fileMatcher, copyTo, destinationFilename);
                    if (destination != null) {
                        acc.copies.putIfAbsent(destination, (SourceFile) sourceFile.withSourcePath(destination).withId(Tree.randomId()));
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        List<SourceFile> generated = new ArrayList<>(acc.copies.size());
        for (Map.Entry<Path, SourceFile> entry : acc.copies.entrySet()) {
            if (!acc.existingPaths.contains(entry.getKey())) {
                generated.add(entry.getValue());
            }
        }
        return generated;
    }
}
