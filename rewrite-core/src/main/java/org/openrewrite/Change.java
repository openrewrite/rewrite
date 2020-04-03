package org.openrewrite;

import lombok.Getter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
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

public class Change<S extends SourceFile> {
    @Getter
    @Nullable
    private final S original;

    @Getter
    private final S fixed;

    @Getter
    private final Set<String> rulesThatMadeChanges;

    public Change(S original, S fixed, Set<String> rulesThatMadeChanges) {
        this.original = original;
        this.fixed = fixed;
        this.rulesThatMadeChanges = rulesThatMadeChanges;
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    public String diff() {
        return diff(null);
    }

    /**
     * @param relativeTo Optional relative path that is used to relativize file paths of reported differences.
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    public String diff(@Nullable Path relativeTo) {
        return new InMemoryDiffEntry(Paths.get(fixed.getSourcePath()), relativeTo,
                original == null ? "" : original.print(), fixed.print(), rulesThatMadeChanges).getDiff();
    }

    static class InMemoryDiffEntry extends DiffEntry {
        InMemoryRepository repo;
        Set<String> rulesThatMadeChanges;

        InMemoryDiffEntry(Path filePath, @Nullable Path relativeTo, String oldSource, String newSource, Set<String> rulesThatMadeChanges) {
            this.changeType = ChangeType.MODIFY;
            this.rulesThatMadeChanges = rulesThatMadeChanges;

            var relativePath = relativeTo == null ? filePath : relativeTo.relativize(filePath);
            this.oldPath = relativePath.toString();
            this.newPath = relativePath.toString();

            try {
                this.repo = new InMemoryRepository.Builder()
                        .setRepositoryDescription(new DfsRepositoryDescription())
                        .build();

                var inserter = repo.getObjectDatabase().newInserter();
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

            var patch = new ByteArrayOutputStream();
            var formatter = new DiffFormatter(patch);
            formatter.setRepository(repo);
            try {
                formatter.format(this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            String diff = new String(patch.toByteArray());

            AtomicBoolean addedComment = new AtomicBoolean(false);
            // NOTE: String.lines() would remove empty lines which we don't want
            return Arrays.stream(diff.split("\n"))
                    .map(l -> {
                        if (!addedComment.get() && l.startsWith("@@") && l.endsWith("@@")) {
                            addedComment.set(true);
                            return l + rulesThatMadeChanges.stream()
                                    .sorted()
                                    .collect(Collectors.joining(", ", " ", ""));
                        }
                        return l;
                    })
                    .collect(Collectors.joining("\n")) + "\n";
        }
    }
}
