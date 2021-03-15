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
package org.openrewrite;

import lombok.Getter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectInserter;
import org.openrewrite.internal.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Result {
    /**
     * Possible {@code null} if a new file is being created.
     */
    @Getter
    @Nullable
    private final SourceFile before;

    /**
     * Possibly {@code null} if the change results in the file being deleted.
     */
    @Getter
    @Nullable
    private final SourceFile after;

    @Getter
    private final Set<Recipe> recipesThatMadeChanges;

    public Result(@Nullable SourceFile before, @Nullable SourceFile after, Set<Recipe> recipesThatMadeChanges) {
        this.before = before;
        this.after = after;
        this.recipesThatMadeChanges = recipesThatMadeChanges;
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff() {
        return diff(TreePrinter.identity());
    }

    /**
     * @param treePrinter Influences the printing of individual tree elements.
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff(TreePrinter<?> treePrinter) {
        return diff(null, treePrinter);
    }

    /**
     * @param relativeTo  Optional relative path that is used to relativize file paths of reported differences.
     * @param treePrinter Influences the printing of individual tree elements.
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff(@Nullable Path relativeTo, TreePrinter<?> treePrinter) {
        Path sourcePath;
        if (after != null) {
            sourcePath = after.getSourcePath();
        } else if (before != null) {
            sourcePath = before.getSourcePath();
        } else {
            sourcePath = (relativeTo == null ? Paths.get(".") : relativeTo).resolve("partial-" + System.nanoTime());
        }

        //noinspection ConstantConditions
        return new InMemoryDiffEntry(sourcePath, relativeTo,
                before == null ? "" : before.print(),
                after == null ? "" : after.print(treePrinter, null),
                recipesThatMadeChanges).getDiff();
    }

    @Override
    public String toString() {
        return diff();
    }

    static class InMemoryDiffEntry extends DiffEntry {
        private final InMemoryRepository repo;
        private final Set<Recipe> recipesThatMadeChanges;

        InMemoryDiffEntry(Path filePath, @Nullable Path relativeTo, String oldSource, String newSource, Set<Recipe> recipesThatMadeChanges) {
            this.changeType = ChangeType.MODIFY;
            this.recipesThatMadeChanges = recipesThatMadeChanges;

            Path relativePath = relativeTo == null ? filePath : relativeTo.relativize(filePath);
            this.oldPath = relativePath.toString();
            this.newPath = relativePath.toString();

            try {
                this.repo = new InMemoryRepository.Builder()
                        .setRepositoryDescription(new DfsRepositoryDescription())
                        .build();

                ObjectInserter inserter = repo.getObjectDatabase().newInserter();
                oldId = inserter.insert(Constants.OBJ_BLOB, oldSource.getBytes()).abbreviate(40);
                newId = inserter.insert(Constants.OBJ_BLOB, newSource.getBytes()).abbreviate(40);
                inserter.flush();

                oldMode = FileMode.REGULAR_FILE;
                newMode = FileMode.REGULAR_FILE;
                repo.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String getDiff() {
            if (oldId.equals(newId)) {
                return "";
            }

            ByteArrayOutputStream patch = new ByteArrayOutputStream();
            DiffFormatter formatter = new DiffFormatter(patch);
            formatter.setRepository(repo);
            try {
                formatter.format(this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            String diff = patch.toString();

            AtomicBoolean addedComment = new AtomicBoolean(false);
            // NOTE: String.lines() would remove empty lines which we don't want
            return Arrays.stream(diff.split("\n"))
                    .map(l -> {
                        if (!addedComment.get() && l.startsWith("@@") && l.endsWith("@@")) {
                            addedComment.set(true);
                            return l + recipesThatMadeChanges.stream()
                                    .map(Recipe::getName)
                                    .sorted()
                                    .collect(Collectors.joining(", ", " ", ""));
                        }
                        return l;
                    })
                    .collect(Collectors.joining("\n")) + "\n";
        }
    }
}
