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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.lib.Repository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GitIgnoreTest {

    @Test
    void untrackedGitIgnoredFileIsIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve(".gitignore"), "generated.txt\n");
            writeFile(tempDir.resolve("generated.txt"), "untracked content");

            git.add().addFilepattern(".gitignore").call();
            git.commit().setMessage("initial").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "generated.txt"))
                    .as("untracked gitignored file should be ignored")
                    .isTrue();
        }
    }

    @Test
    void trackedGitIgnoredFileIsNotIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve("tracked-ignored.txt"), "content");
            git.add().addFilepattern("tracked-ignored.txt").call();
            git.commit().setMessage("initial").call();

            writeFile(tempDir.resolve(".gitignore"), "tracked-ignored.txt\n");
            git.add().addFilepattern(".gitignore").call();
            git.commit().setMessage("add gitignore").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "tracked-ignored.txt"))
                    .as("tracked gitignored file should NOT be ignored")
                    .isFalse();
        }
    }

    @Test
    void normalTrackedFileIsNotIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve("normal.txt"), "content");
            git.add().addFilepattern("normal.txt").call();
            git.commit().setMessage("initial").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "normal.txt"))
                    .as("normal tracked file should NOT be ignored")
                    .isFalse();
        }
    }

    @Test
    void untrackedFileInGitIgnoredDirectoryIsIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve(".gitignore"), "generated/\n");
            writeFile(tempDir.resolve("generated/output.txt"), "untracked content");

            git.add().addFilepattern(".gitignore").call();
            git.commit().setMessage("initial").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "generated/output.txt"))
                    .as("untracked file in gitignored directory should be ignored")
                    .isTrue();
        }
    }

    @Test
    void trackedFileInGitIgnoredDirectoryIsNotIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve("generated/output.txt"), "tracked content");
            git.add().addFilepattern("generated/output.txt").call();
            git.commit().setMessage("initial").call();

            writeFile(tempDir.resolve(".gitignore"), "generated/\n");
            git.add().addFilepattern(".gitignore").call();
            git.commit().setMessage("add gitignore").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "generated/output.txt"))
                    .as("tracked file in gitignored directory should NOT be ignored")
                    .isFalse();
        }
    }

    @Test
    void deeplyNestedTrackedFileInGitIgnoredPathIsNotIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve("src/main/java/com/example/Generated.java"),
                    "package com.example;\npublic class Generated {}");
            git.add().addFilepattern("src/main/java/com/example/Generated.java").call();
            git.commit().setMessage("initial").call();

            writeFile(tempDir.resolve(".gitignore"), "src/main/java/com/example/Generated.java\n");
            git.add().addFilepattern(".gitignore").call();
            git.commit().setMessage("add gitignore").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "src/main/java/com/example/Generated.java"))
                    .as("tracked Java source matching gitignore should NOT be ignored")
                    .isFalse();
        }
    }

    @Test
    void pathOverloadNormalizesPlatformSeparators(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve(".gitignore"), "untracked.txt\n");
            writeFile(tempDir.resolve("untracked.txt"), "content");

            git.add().addFilepattern(".gitignore").call();
            git.commit().setMessage("initial").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, Path.of("untracked.txt")))
                    .as("Path overload should work the same as String overload")
                    .isTrue();
        }
    }

    @Test
    void emptyPathIsNotIgnored(@TempDir Path tempDir) throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Repository repo = git.getRepository();

            writeFile(tempDir.resolve("file.txt"), "content");
            git.add().addFilepattern("file.txt").call();
            git.commit().setMessage("initial").call();

            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "")).isFalse();
            assertThat(GitIgnore.isIgnoredAndUntracked(repo, "/")).isFalse();
        }
    }

    private static void writeFile(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
