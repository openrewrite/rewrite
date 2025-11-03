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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.jgit.diff.DiffEntry;
import org.openrewrite.jgit.diff.DiffFormatter;
import org.openrewrite.jgit.diff.RawTextComparator;
import org.openrewrite.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.openrewrite.jgit.internal.storage.dfs.InMemoryRepository;
import org.openrewrite.jgit.lib.*;
import org.openrewrite.jgit.util.Base85;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;

import static java.util.stream.Collectors.joining;

public class InMemoryDiffEntry extends DiffEntry implements AutoCloseable {

    static final AbbreviatedObjectId A_ZERO = AbbreviatedObjectId
            .fromObjectId(ObjectId.zeroId());

    private final InMemoryRepository repo;
    private final Set<Recipe> recipesThatMadeChanges;

    // Store bytes for binary patch generation (only when using ISO_8859_1)
    private final byte[] newFileBytes;

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges) {
        this(originalFilePath, filePath, relativeTo, oldSource, newSource, recipesThatMadeChanges, FileMode.REGULAR_FILE, FileMode.REGULAR_FILE);
    }

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges, FileMode oldMode, FileMode newMode) {
        this(originalFilePath, filePath, relativeTo, oldSource, newSource, recipesThatMadeChanges, oldMode, newMode, StandardCharsets.UTF_8);
    }

    public InMemoryDiffEntry(@Nullable Path originalFilePath, @Nullable Path filePath, @Nullable Path relativeTo, String oldSource,
                             String newSource, Set<Recipe> recipesThatMadeChanges, FileMode oldMode, FileMode newMode, Charset charset) {

        this.recipesThatMadeChanges = recipesThatMadeChanges;

        // Store bytes for binary patch generation (only when using ISO_8859_1 for binary files)
        if (charset.equals(StandardCharsets.ISO_8859_1) && newSource != null && !newSource.isEmpty()) {
            this.newFileBytes = newSource.getBytes(charset);
        } else {
            this.newFileBytes = null;
        }

        try {
            this.repo = new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription())
                    .build();

            try (ObjectInserter inserter = repo.getObjectDatabase().newInserter()) {

                if (originalFilePath != null) {
                    this.oldId = inserter.insert(Constants.OBJ_BLOB, oldSource.getBytes(charset)).abbreviate(40);
                    this.oldMode = oldMode;
                    this.oldPath = (relativeTo == null ? originalFilePath : relativeTo.relativize(originalFilePath)).toString().replace("\\", "/");
                } else {
                    this.oldId = A_ZERO;
                    this.oldMode = FileMode.MISSING;
                    this.oldPath = DEV_NULL;
                }

                if (filePath != null) {
                    this.newId = inserter.insert(Constants.OBJ_BLOB, newSource.getBytes(charset)).abbreviate(40);
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

        // If this is a binary file and we have bytes, replace "Binary files differ" with base85 patch
        if (newFileBytes != null && diff.contains("Binary files differ")) {
            // Binary patches require full 40-character object IDs in the index line
            // Replace abbreviated IDs with full IDs
            String oldIdFull = oldId.toObjectId() != null ? oldId.toObjectId().name() : oldId.name();
            String newIdFull = newId.toObjectId() != null ? newId.toObjectId().name() : newId.name();
            diff = diff.replaceFirst("index [0-9a-f]+\\.\\.[0-9a-f]+",
                                    "index " + oldIdFull + ".." + newIdFull);
            diff = generateBinaryPatch(diff);
        }

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

    /**
     * Generate a base85-encoded binary patch in Git's binary patch format.
     * Replaces "Binary files differ" with actual patch data that can be applied.
     *
     * @param diffHeader The diff header from JGit (includes file paths, index, etc.)
     * @return Complete diff with base85-encoded binary patch
     */
    private String generateBinaryPatch(String diffHeader) {
        // Remove "Binary files differ" line
        String header = diffHeader.replace("Binary files differ\n", "");

        // Deflate (compress) the binary data as Git does
        Deflater deflater = new Deflater();
        deflater.setInput(newFileBytes);
        deflater.finish();

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            compressed.write(buffer, 0, count);
        }
        deflater.end();
        byte[] compressedBytes = compressed.toByteArray();

        // Encode in base85 (matching Git's implementation in diff.c)
        StringBuilder patch = new StringBuilder(header);
        patch.append("GIT binary patch\n");
        patch.append("literal ").append(newFileBytes.length).append("\n");

        // Emit data encoded in base85
        int offset = 0;
        int dataSize = compressedBytes.length;
        while (dataSize > 0) {
            // Git: int bytes = (52 < data_size) ? 52 : data_size;
            int bytes = (52 < dataSize) ? 52 : dataSize;
            dataSize -= bytes;

            // Git: if (bytes <= 26) line[0] = bytes + 'A' - 1; else line[0] = bytes - 26 + 'a' - 1;
            char lengthChar;
            if (bytes <= 26) {
                lengthChar = (char) (bytes + 'A' - 1);
            } else {
                lengthChar = (char) (bytes - 26 + 'a' - 1);
            }

            // Git: encode_85(line + 1, cp, bytes);
            byte[] encodedLine = Base85.encode(compressedBytes, offset, bytes);

            // Build line: <length-char><base85-data>\n
            patch.append(lengthChar);
            patch.append(new String(encodedLine, StandardCharsets.US_ASCII));
            patch.append('\n');

            offset += bytes;
        }

        // Add terminating marker (one blank line, then literal 0 section, then final blank line)
        patch.append("\n");
        patch.append("literal 0\n");
        patch.append("HcmV?d00001\n");
        patch.append("\n");

        return patch.toString();
    }

    @Override
    public void close() {
        this.repo.close();
    }
}
