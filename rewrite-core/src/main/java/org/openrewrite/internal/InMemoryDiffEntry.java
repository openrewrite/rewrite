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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.jgit.attributes.AttributesNodeProvider;
import org.openrewrite.jgit.diff.DiffEntry;
import org.openrewrite.jgit.diff.DiffFormatter;
import org.openrewrite.jgit.diff.RawTextComparator;
import org.openrewrite.jgit.internal.storage.dfs.*;
import org.openrewrite.jgit.lib.*;
import org.openrewrite.marker.GitTreeEntry;
import org.openrewrite.quark.Quark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.joining;

public class InMemoryDiffEntry extends DiffEntry implements AutoCloseable {

    static final AbbreviatedObjectId A_ZERO = AbbreviatedObjectId
            .fromObjectId(ObjectId.zeroId());

    private final VirtualInMemoryRepository repo;
    private final Set<Recipe> recipesThatMadeChanges;

    private final boolean binaryPatch;

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges) {
        this(originalFilePath, filePath, relativeTo, oldSource, newSource, recipesThatMadeChanges, FileMode.REGULAR_FILE, FileMode.REGULAR_FILE);
    }

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges, FileMode oldMode, FileMode newMode) {
        this(originalFilePath, filePath, relativeTo, oldSource.getBytes(StandardCharsets.UTF_8), newSource.getBytes(StandardCharsets.UTF_8), recipesThatMadeChanges, oldMode, newMode, false);
    }

    @Incubating(since = "8.69.0")
    public InMemoryDiffEntry(@Nullable SourceFile before, @Nullable SourceFile after, @Nullable Path relativeTo, PrintOutputCapture.@Nullable MarkerPrinter markerPrinter, Set<Recipe> recipesThatMadeChanges, boolean binaryPatch) {
        if (before == null && after == null) {
            throw new NullPointerException("before and after can't be null");
        }

        this.recipesThatMadeChanges = recipesThatMadeChanges;
        this.binaryPatch = binaryPatch;

        try {
            this.repo = new VirtualInMemoryRepository(new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription()));

            try (VirtualObjectDatabase database = repo.getObjectDatabase()) {
                try (ObjectInserter inserter = repo.getObjectDatabase().newInserter()) {
                    if (before == null) {
                        this.oldId = A_ZERO;
                        this.oldMode = FileMode.MISSING;
                        this.oldPath = DEV_NULL;
                    } else {
                        Optional<GitTreeEntry> maybeGitTreeEntry = before.getMarkers().findFirst(GitTreeEntry.class);
                        if (maybeGitTreeEntry.isPresent()) {
                            GitTreeEntry entry = maybeGitTreeEntry.get();
                            this.oldId = database.insertVirtual(ObjectId.fromString(entry.getObjectId()), printAllAsBytes(before, markerPrinter)).abbreviate(40);
                            this.oldMode = FileMode.fromBits(entry.getFileMode());
                        } else {
                            this.oldId = inserter.insert(Constants.OBJ_BLOB, printAllAsBytes(before, markerPrinter)).abbreviate(40);
                            this.oldMode = before.getFileAttributes() != null && before.getFileAttributes().isExecutable() ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
                        }
                        this.oldPath = (relativeTo == null ? before.getSourcePath() : relativeTo.relativize(before.getSourcePath())).toString().replace("\\", "/");
                    }

                    if (after == null) {
                        this.newId = A_ZERO;
                        this.newMode = FileMode.MISSING;
                        this.newPath = DEV_NULL;
                    } else {
                        this.newId = inserter.insert(Constants.OBJ_BLOB, printAllAsBytes(after, markerPrinter)).abbreviate(40);
                        this.newMode = after.getMarkers().findFirst(GitTreeEntry.class)
                                .map(entry -> FileMode.fromBits(entry.getFileMode()))
                                .orElseGet(() -> after.getFileAttributes() != null && after.getFileAttributes().isExecutable() ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE);
                        this.newPath = (relativeTo == null ? after.getSourcePath() : relativeTo.relativize(after.getSourcePath())).toString().replace("\\", "/");
                    }
                    inserter.flush();
                }
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

    public InMemoryDiffEntry(
            @Nullable Path originalFilePath,
            @Nullable Path filePath,
            @Nullable Path relativeTo,
            byte[] oldSource,
            byte[] newSource,
            Set<Recipe> recipesThatMadeChanges,
            FileMode oldMode,
            FileMode newMode,
            boolean binaryPatch
    ) {

        this.recipesThatMadeChanges = recipesThatMadeChanges;
        this.binaryPatch = binaryPatch;

        try {
            this.repo = new VirtualInMemoryRepository(new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription()));

            try (ObjectInserter inserter = repo.getObjectDatabase().newInserter()) {

                if (originalFilePath != null) {
                    this.oldId = inserter.insert(Constants.OBJ_BLOB, oldSource).abbreviate(40);
                    this.oldMode = oldMode;
                    this.oldPath = (relativeTo == null ? originalFilePath : relativeTo.relativize(originalFilePath)).toString().replace("\\", "/");
                } else {
                    this.oldId = A_ZERO;
                    this.oldMode = FileMode.MISSING;
                    this.oldPath = DEV_NULL;
                }

                if (filePath != null) {
                    this.newId = inserter.insert(Constants.OBJ_BLOB, newSource).abbreviate(40);
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
            formatter.setBinary(binaryPatch);
            formatter.format(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String diff = patch.toString();

        AtomicBoolean addedComment = new AtomicBoolean(false);
        // NOTE: String.lines() would remove empty lines which we don't want
        // Use split with limit -1 to preserve trailing empty strings (important for binary patches)
        return Arrays.stream(diff.split("\n", -1))
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
                       .collect(joining("\n"));
    }

    @Override
    public void close() {
        this.repo.close();
    }

    private static byte[] printAllAsBytes(@Nullable SourceFile sourceFile, PrintOutputCapture.@Nullable MarkerPrinter markerPrinter) {
        if (sourceFile == null || sourceFile instanceof Quark) {
            return new byte[0];
        }

        PrintOutputCapture<Integer> out = markerPrinter == null ?
                new PrintOutputCapture<>(0) :
                new PrintOutputCapture<>(0, markerPrinter);

        Charset charset = sourceFile.getCharset() == null ? StandardCharsets.UTF_8 : sourceFile.getCharset();
        return sourceFile.printAll(out).getBytes(charset);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class VirtualInMemoryRepository extends Repository {
        InMemoryRepository repo;
        VirtualObjectDatabase objdb;

        public VirtualInMemoryRepository(InMemoryRepository.Builder builder) throws IOException {
            super(builder);
            repo = builder.build();
            objdb = new VirtualObjectDatabase(repo.getObjectDatabase());
        }

        @Override
        public void create(boolean bare) throws IOException {
            repo.create(bare);
        }

        @Override
        public String getIdentifier() {
            return repo.getIdentifier();
        }

        @Override
        public VirtualObjectDatabase getObjectDatabase() {
            return objdb;
        }

        @Override
        public RefDatabase getRefDatabase() {
            return repo.getRefDatabase();
        }

        @Override
        public StoredConfig getConfig() {
            return repo.getConfig();
        }

        @Override
        public AttributesNodeProvider createAttributesNodeProvider() {
            return repo.createAttributesNodeProvider();
        }

        @Override
        public void scanForRepoChanges() throws IOException {
            repo.scanForRepoChanges();
        }

        @Override
        public void notifyIndexChanged(boolean internal) {
            repo.notifyIndexChanged(internal);
        }

        @Override
        public ReflogReader getReflogReader(String refName) throws IOException {
            return repo.getReflogReader(refName);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class VirtualObjectDatabase extends ObjectDatabase implements AutoCloseable {
        ObjectDatabase delegate;
        Map<ObjectId, byte[]> virtualObjects = new HashMap<>();

        public ObjectId insertVirtual(ObjectId id, byte[] content) {
            virtualObjects.put(id, content);
            return id;
        }

        @Override
        public ObjectInserter newInserter() {
            return delegate.newInserter();
        }

        @Override
        public VirtualObjectReader newReader() {
            return new VirtualObjectReader(virtualObjects, delegate.newReader());
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class VirtualObjectReader extends ObjectReader {
        Map<ObjectId, byte[]> virtualObjects;
        ObjectReader delegate;

        @Override
        public ObjectReader newReader() {
            return delegate.newReader();
        }

        @Override
        public Collection<ObjectId> resolve(AbbreviatedObjectId id) throws IOException {
            return delegate.resolve(id);
        }

        @Override
        public ObjectLoader open(AnyObjectId objectId, int typeHint) throws IOException {
            byte[] virtual = virtualObjects.get(objectId.toObjectId());
            if (virtual != null) {
                return new ObjectLoader.SmallObject(typeHint, virtual);
            }
            return delegate.open(objectId, typeHint);
        }

        @Override
        public Set<ObjectId> getShallowCommits() {
            try {
                return delegate.getShallowCommits();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
