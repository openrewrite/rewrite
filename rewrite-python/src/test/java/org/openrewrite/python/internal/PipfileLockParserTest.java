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

class PipfileLockParserTest {

    @Test
    void parseBasicPipfileLock() {
        String pipfileLock = """
          {
              "_meta": {
                  "hash": {"sha256": "abc"},
                  "pipfile-spec": 6,
                  "requires": {"python_version": "3.10"},
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "requests": {
                      "version": "==2.31.0",
                      "hashes": ["sha256:abc"]
                  },
                  "certifi": {
                      "version": "==2024.2.2"
                  }
              },
              "develop": {
                  "pytest": {
                      "version": "==7.4.0"
                  }
              }
          }
          """;

        List<ResolvedDependency> resolved = PipfileLockParser.parse(pipfileLock);

        assertThat(resolved).hasSize(3);

        ResolvedDependency requests = resolved.get(0);
        assertThat(requests.getName()).isEqualTo("requests");
        assertThat(requests.getVersion()).isEqualTo("2.31.0");
        assertThat(requests.getSource()).isEqualTo("https://pypi.org/simple");

        ResolvedDependency certifi = resolved.get(1);
        assertThat(certifi.getName()).isEqualTo("certifi");
        assertThat(certifi.getVersion()).isEqualTo("2024.2.2");

        ResolvedDependency pytest = resolved.get(2);
        assertThat(pytest.getName()).isEqualTo("pytest");
        assertThat(pytest.getVersion()).isEqualTo("7.4.0");
    }

    @Test
    void parseGitSource() {
        String pipfileLock = """
          {
              "_meta": {"sources": [{"url": "https://pypi.org/simple"}]},
              "default": {
                  "my-pkg": {
                      "git": "https://github.com/example/my-pkg.git",
                      "version": "==0.1.0"
                  }
              }
          }
          """;

        List<ResolvedDependency> resolved = PipfileLockParser.parse(pipfileLock);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).getSource()).isEqualTo("https://github.com/example/my-pkg.git");
    }

    @Test
    void parseEmptyLock() {
        String pipfileLock = """
          {
              "_meta": {"sources": [{"url": "https://pypi.org/simple"}]},
              "default": {},
              "develop": {}
          }
          """;
        assertThat(PipfileLockParser.parse(pipfileLock)).isEmpty();
    }

    @Test
    void packageWithoutVersionIsSkipped() {
        String pipfileLock = """
          {
              "_meta": {"sources": [{"url": "https://pypi.org/simple"}]},
              "default": {
                  "broken": {"hashes": []},
                  "ok": {"version": "==1.0.0"}
              }
          }
          """;

        List<ResolvedDependency> resolved = PipfileLockParser.parse(pipfileLock);
        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).getName()).isEqualTo("ok");
    }

    @Test
    void findLockFileInSameDirectory(@TempDir Path tempDir) throws IOException {
        Path lockFile = tempDir.resolve("Pipfile.lock");
        Files.write(lockFile, "{}".getBytes());

        Path found = PipfileLockParser.findLockFile(tempDir, null);
        assertThat(found).isEqualTo(lockFile);
    }

    @Test
    void findLockFileInParentDirectory(@TempDir Path tempDir) throws IOException {
        Path lockFile = tempDir.resolve("Pipfile.lock");
        Files.write(lockFile, "{}".getBytes());

        Path subDir = tempDir.resolve("subproject");
        Files.createDirectories(subDir);

        Path found = PipfileLockParser.findLockFile(subDir, null);
        assertThat(found).isEqualTo(lockFile);
    }

    @Test
    void findLockFileRespectsBoundary(@TempDir Path tempDir) throws IOException {
        Path lockFile = tempDir.resolve("Pipfile.lock");
        Files.write(lockFile, "{}".getBytes());

        Path boundary = tempDir.resolve("boundary");
        Files.createDirectories(boundary);
        Path subDir = boundary.resolve("subproject");
        Files.createDirectories(subDir);

        Path found = PipfileLockParser.findLockFile(subDir, boundary);
        assertThat(found).isNull();
    }

    @Test
    void findLockFileReturnsNullWhenNotFound(@TempDir Path tempDir) {
        Path found = PipfileLockParser.findLockFile(tempDir, tempDir);
        assertThat(found).isNull();
    }
}
