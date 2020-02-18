package com.netflix.rewrite.refactor;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.Tr;
import lombok.Data;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Data
public class RefactorResult implements Serializable {
    private final Tr.CompilationUnit original;
    private final Tr.CompilationUnit fixed;
    private final Set<String> rulesThatMadeChanges;

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    public String diff() {
        return diff(null);
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    public String diff(@Nullable Path relativeTo) {
        return new InMemoryDiffEntry(Paths.get(original.getSourcePath()), relativeTo,
                original.print(), fixed.print()).getDiff();
    }

    static class InMemoryDiffEntry extends DiffEntry {
        InMemoryRepository repo;

        InMemoryDiffEntry(Path filePath, @Nullable Path relativeTo, String oldSource, String newSource) {
            this.changeType = ChangeType.MODIFY;

            var relativePath = relativeTo == null ? filePath : relativeTo.relativize(filePath);
            this.oldPath = relativePath.toString();
            this.newPath = relativePath.toString();

            try {
                this.repo = new InMemoryRepository.Builder().build();

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
            return new String(patch.toByteArray());
        }
    }
}
