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
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
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
    private final Collection<Stack<Recipe>> recipes;

    @Getter
    @Nullable
    private final Duration timeSavings;

    @Nullable
    private transient WeakReference<String> diff;

    @Nullable
    private Path relativeTo;

    @Deprecated
    public Set<Recipe> getRecipesThatMadeChanges() {
        return recipes.stream().map(Stack::peek).collect(Collectors.toSet());
    }

    public Result(@Nullable SourceFile before, @Nullable SourceFile after, Collection<Stack<Recipe>> recipes) {
        this.before = before;
        this.after = after;
        this.recipes = recipes;

        Duration timeSavings = null;
        for (Stack<Recipe> recipesStack : recipes) {
            Duration perOccurrence = recipesStack.peek().getEstimatedEffortPerOccurrence();
            if(perOccurrence != null) {
                timeSavings = timeSavings == null ? perOccurrence : timeSavings.plus(perOccurrence);
            }
        }

        this.timeSavings = timeSavings;
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff() {
        return diff(null);
    }

    /**
     * @param relativeTo  Optional relative path that is used to relativize file paths of reported differences.
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff(@Nullable Path relativeTo) {
        String d;
        if (this.diff == null) {
            d = computeDiff(relativeTo);
            this.diff = new WeakReference<>(d);
        } else {
            d = this.diff.get();
            if (d == null || !Objects.equals(this.relativeTo, relativeTo)) {
                d = computeDiff(relativeTo);
                this.diff = new WeakReference<>(d);
            }
        }
        return d;
    }

    private String computeDiff(@Nullable Path relativeTo) {
        Path sourcePath;
        if (after != null) {
            sourcePath = after.getSourcePath();
        } else if (before != null) {
            sourcePath = before.getSourcePath();
        } else {
            sourcePath = (relativeTo == null ? Paths.get(".") : relativeTo).resolve("partial-" + System.nanoTime());
        }

        Path originalSourcePath = sourcePath;
        if (before != null && after != null && !before.getSourcePath().equals(after.getSourcePath())) {
            originalSourcePath = before.getSourcePath();
        }

        try (InMemoryDiffEntry diffEntry = new InMemoryDiffEntry(
                originalSourcePath,
                sourcePath,
                relativeTo,
                before == null ? "" : before.printAll(),
                after == null ? "" : after.printAll(),
                recipes.stream().map(Stack::peek).collect(Collectors.toSet())
        )) {
            this.relativeTo = relativeTo;
            return diffEntry.getDiff();
        }
    }

    @Override
    public String toString() {
        return diff();
    }

    static class InMemoryDiffEntry extends DiffEntry implements AutoCloseable {
        private final InMemoryRepository repo;
        private final Set<Recipe> recipesThatMadeChanges;

        InMemoryDiffEntry(Path originalFilePath, Path filePath, @Nullable Path relativeTo, String oldSource,
                          String newSource, Set<Recipe> recipesThatMadeChanges) {
            this.changeType = originalFilePath.equals(filePath) ? ChangeType.MODIFY : ChangeType.RENAME;
            this.recipesThatMadeChanges = recipesThatMadeChanges;

            this.oldPath = (relativeTo == null ? originalFilePath : relativeTo.relativize(originalFilePath)).toString();
            this.newPath = (relativeTo == null ? filePath : relativeTo.relativize(filePath)).toString();

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
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String getDiff() {
            if (oldId.equals(newId) && oldPath.equals(newPath)) {
                return "";
            }

            ByteArrayOutputStream patch = new ByteArrayOutputStream();
            try (DiffFormatter formatter = new DiffFormatter(patch)) {
                formatter.setRepository(repo);
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

        @Override
        public void close() {
            this.repo.close();
        }
    }
}
