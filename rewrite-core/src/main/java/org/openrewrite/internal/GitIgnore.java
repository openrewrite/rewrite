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
package org.openrewrite.internal;

import org.openrewrite.jgit.dircache.DirCacheIterator;
import org.openrewrite.jgit.lib.FileMode;
import org.openrewrite.jgit.lib.Repository;
import org.openrewrite.jgit.treewalk.FileTreeIterator;
import org.openrewrite.jgit.treewalk.TreeWalk;
import org.openrewrite.jgit.treewalk.WorkingTreeIterator;
import org.openrewrite.jgit.treewalk.filter.PathFilterGroup;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.openrewrite.PathUtils.separatorsToUnix;

/**
 * Utility for checking whether a file is excluded by {@code .gitignore} rules,
 * taking the git index (tracking status) into account.
 * <p>
 * A file is considered "ignored" only if it matches a {@code .gitignore} rule
 * <b>and</b> is not tracked in the git index. This matches git's own behavior:
 * {@code .gitignore} has no effect on files that are already tracked.
 */
public final class GitIgnore {

    private GitIgnore() {
    }

    /**
     * Returns {@code true} if the given path is matched by a {@code .gitignore}
     * rule and is <b>not</b> tracked in the git index.
     *
     * @param repository      the JGit repository
     * @param repoRelativePath path relative to the repository root, using forward slashes
     *                         (e.g. {@code "src/main/java/Foo.java"})
     * @return {@code true} if the path should be treated as ignored
     */
    public static boolean isIgnoredAndUntracked(Repository repository, String repoRelativePath) {
        if (repoRelativePath.isEmpty()) {
            return false;
        }
        // Strip leading slash if present; JGit TreeWalk paths never have one
        if (repoRelativePath.startsWith("/")) {
            repoRelativePath = repoRelativePath.substring(1);
        }
        if (repoRelativePath.isEmpty()) {
            return false;
        }

        try (TreeWalk walk = new TreeWalk(repository)) {
            walk.addTree(new FileTreeIterator(repository));
            walk.addTree(new DirCacheIterator(repository.readDirCache()));
            walk.setFilter(PathFilterGroup.createFromStrings(repoRelativePath));
            while (walk.next()) {
                WorkingTreeIterator workingTreeIterator = walk.getTree(0, WorkingTreeIterator.class);
                DirCacheIterator dirCacheIterator = walk.getTree(1, DirCacheIterator.class);
                if (workingTreeIterator == null) {
                    // Entry exists only in the index (tracked but not on disk)
                    if (walk.getPathString().equals(repoRelativePath)) {
                        return false;
                    }
                    continue;
                }
                if (walk.getPathString().equals(repoRelativePath)) {
                    return workingTreeIterator.isEntryIgnored() && dirCacheIterator == null;
                }
                if (workingTreeIterator.getEntryFileMode().equals(FileMode.TREE)) {
                    if (workingTreeIterator.isEntryIgnored() && dirCacheIterator == null) {
                        return true;
                    }
                    walk.enterSubtree();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }

    /**
     * Convenience overload that normalizes a platform path to a
     * forward-slash-separated, repository-relative path.
     *
     * @param repository the JGit repository
     * @param platformPath path that may use platform separators
     * @return {@code true} if the path should be treated as ignored
     * @see #isIgnoredAndUntracked(Repository, String)
     */
    public static boolean isIgnoredAndUntracked(Repository repository, java.nio.file.Path platformPath) {
        return isIgnoredAndUntracked(repository, separatorsToUnix(platformPath.toString()));
    }
}
