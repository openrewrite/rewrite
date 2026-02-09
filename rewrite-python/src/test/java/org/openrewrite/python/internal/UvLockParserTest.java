/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UvLockParserTest {

    @Test
    void parseBasicUvLock() {
        String uvLock = "" +
                "version = 1\n" +
                "requires-python = \">=3.10\"\n" +
                "\n" +
                "[[package]]\n" +
                "name = \"requests\"\n" +
                "version = \"2.31.0\"\n" +
                "source = { registry = \"https://pypi.org/simple\" }\n" +
                "dependencies = [\n" +
                "    { name = \"certifi\", specifier = \">=2017.4.17\" },\n" +
                "    { name = \"charset-normalizer\", specifier = \">=2,<4\" },\n" +
                "]\n" +
                "\n" +
                "[[package]]\n" +
                "name = \"certifi\"\n" +
                "version = \"2024.2.2\"\n" +
                "source = { registry = \"https://pypi.org/simple\" }\n" +
                "\n" +
                "[[package]]\n" +
                "name = \"charset-normalizer\"\n" +
                "version = \"3.3.2\"\n" +
                "source = { registry = \"https://pypi.org/simple\" }\n";

        List<ResolvedDependency> resolved = UvLockParser.parse(uvLock);

        assertThat(resolved).hasSize(3);

        ResolvedDependency requests = resolved.get(0);
        assertThat(requests.getName()).isEqualTo("requests");
        assertThat(requests.getVersion()).isEqualTo("2.31.0");
        assertThat(requests.getSource()).isEqualTo("https://pypi.org/simple");
        assertThat(requests.getDependencies()).hasSize(2);
        assertThat(requests.getDependencies().get(0).getName()).isEqualTo("certifi");
        assertThat(requests.getDependencies().get(0).getVersionConstraint()).isEqualTo(">=2017.4.17");

        ResolvedDependency certifi = resolved.get(1);
        assertThat(certifi.getName()).isEqualTo("certifi");
        assertThat(certifi.getVersion()).isEqualTo("2024.2.2");
        assertThat(certifi.getDependencies()).isNull();
    }

    @Test
    void parseEmptyLock() {
        List<ResolvedDependency> resolved = UvLockParser.parse("version = 1\n");
        assertThat(resolved).isEmpty();
    }

    @Test
    void findLockFileInSameDirectory(@TempDir Path tempDir) throws IOException {
        Path lockFile = tempDir.resolve("uv.lock");
        Files.write(lockFile, "version = 1\n".getBytes());

        Path found = UvLockParser.findLockFile(tempDir, null);
        assertThat(found).isEqualTo(lockFile);
    }

    @Test
    void findLockFileInParentDirectory(@TempDir Path tempDir) throws IOException {
        Path lockFile = tempDir.resolve("uv.lock");
        Files.write(lockFile, "version = 1\n".getBytes());

        Path subDir = tempDir.resolve("subproject");
        Files.createDirectories(subDir);

        Path found = UvLockParser.findLockFile(subDir, null);
        assertThat(found).isEqualTo(lockFile);
    }

    @Test
    void findLockFileRespectsResourceBoundary(@TempDir Path tempDir) throws IOException {
        // Lock file above boundary should not be found
        Path lockFile = tempDir.resolve("uv.lock");
        Files.write(lockFile, "version = 1\n".getBytes());

        Path boundary = tempDir.resolve("boundary");
        Files.createDirectories(boundary);
        Path subDir = boundary.resolve("subproject");
        Files.createDirectories(subDir);

        Path found = UvLockParser.findLockFile(subDir, boundary);
        assertThat(found).isNull();
    }

    @Test
    void findLockFileReturnsNullWhenNotFound(@TempDir Path tempDir) {
        Path found = UvLockParser.findLockFile(tempDir, tempDir);
        assertThat(found).isNull();
    }
}
