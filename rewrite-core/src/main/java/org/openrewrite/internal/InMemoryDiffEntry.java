/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.internal;

import org.openrewrite.jgit.diff.DiffEntry;
import org.openrewrite.jgit.diff.DiffFormatter;
import org.openrewrite.jgit.diff.RawTextComparator;
import org.openrewrite.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.openrewrite.jgit.internal.storage.dfs.InMemoryRepository;
import org.openrewrite.jgit.lib.*;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class InMemoryDiffEntry extends DiffEntry implements AutoCloseable {

    static final AbbreviatedObjectId A_ZERO = AbbreviatedObjectId
            .fromObjectId(ObjectId.zeroId());

    private final InMemoryRepository repo;
    private final Set<Recipe> recipesThatMadeChanges;

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges) {
        this(originalFilePath, filePath, relativeTo, oldSource, newSource, recipesThatMadeChanges, FileMode.REGULAR_FILE, FileMode.REGULAR_FILE);
    }

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges, FileMode oldMode, FileMode newMode) {

        this.recipesThatMadeChanges = recipesThatMadeChanges;

        try {
            this.repo = new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription())
                    .build();

            try (ObjectInserter inserter = repo.getObjectDatabase().newInserter()) {

                if (originalFilePath != null) {
                    this.oldId = inserter.insert(Constants.OBJ_BLOB, oldSource.getBytes(StandardCharsets.UTF_8)).abbreviate(40);
                    this.oldMode = oldMode;
                    this.oldPath = (relativeTo == null ? originalFilePath : relativeTo.relativize(originalFilePath)).toString().replace("\\", "/");
                } else {
                    this.oldId = A_ZERO;
                    this.oldMode = FileMode.MISSING;
                    this.oldPath = DEV_NULL;
                }

                if (filePath != null) {
                    this.newId = inserter.insert(Constants.OBJ_BLOB, newSource.getBytes(StandardCharsets.UTF_8)).abbreviate(40);
                    this.newMode = newMode;
                    this.newPath = (relativeTo == null ? filePath : relativeTo.relativize(filePath)).toString().replace("\\", "/");
                } else {
                    this.newId = A_ZERO;
                    this.newMode = FileMode.MISSING;
                    this.newPath = DEV_NULL;
                }
                inserter.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (this.oldMode == FileMode.MISSING && this.newMode != FileMode.MISSING) {
            this.changeType = ChangeType.ADD;
        } else if (this.oldMode != FileMode.MISSING && this.newMode == FileMode.MISSING) {
            this.changeType = ChangeType.DELETE;
        } else if (!oldPath.equals(newPath)) {
            this.changeType = ChangeType.RENAME;
        } else {
            this.changeType = ChangeType.MODIFY;
        }
    }

    public String getDiff() {
        return getDiff(false);
    }

    public String getDiff(@Nullable Boolean ignoreAllWhitespace) {
        if (ignoreAllWhitespace == null) {
            ignoreAllWhitespace = false;
        }

        if (oldId.equals(newId) && oldPath.equals(newPath)) {
            return "";
        }

        ByteArrayOutputStream patch = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(patch)) {
            formatter.setDiffComparator(ignoreAllWhitespace ? RawTextComparator.WS_IGNORE_ALL : RawTextComparator.DEFAULT);
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

                               Set<String> sortedRecipeNames = new LinkedHashSet<>();
                               for (Recipe recipesThatMadeChange : recipesThatMadeChanges) {
                                   sortedRecipeNames.add(recipesThatMadeChange.getName());
                               }
                               StringJoiner joinedRecipeNames = new StringJoiner(", ", " ", "");
                               for (String name : sortedRecipeNames) {
                                   joinedRecipeNames.add(name);
                               }

                               return l + joinedRecipeNames;
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
