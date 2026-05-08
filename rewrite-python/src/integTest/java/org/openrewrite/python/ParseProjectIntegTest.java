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
package org.openrewrite.python;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.json.tree.Json;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.text.PlainText;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ParseProject RPC functionality.
 * <p>
 * This test verifies that parseProject can:
 * 1. Find Python files in a directory
 * 2. Parse them correctly via RPC
 * 3. Return valid SourceFile objects
 */
class ParseProjectIntegTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log"))
                .traceRpcMessages()
        );
    }

    @AfterEach
    void after() throws IOException {
        PythonRewriteRpc.shutdownCurrent();
        // Reset factory to default so other tests don't inherit a log path
        // pointing at this test's (soon-to-be-deleted) temp directory
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
        if (Files.exists(tempDir.resolve("python-rpc.log"))) {
            System.out.println("=== Python RPC Log ===");
            System.out.println(Files.readString(tempDir.resolve("python-rpc.log")));
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void parsesProjectDirectory() throws IOException {
        // Create a project structure
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("main.py"), """
                def main():
                    print("Hello")
                """);

        Files.writeString(projectDir.resolve("utils.py"), """
                def helper():
                    return 42
                """);

        // Parse the project
        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources).hasSize(2);
        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().getFileName().toString())
                .containsExactlyInAnyOrder("main.py", "utils.py");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void parsesNestedDirectories() throws IOException {
        // Create nested structure
        Path projectDir = tempDir.resolve("nested");
        Path subDir = projectDir.resolve("subpackage");
        Files.createDirectories(subDir);

        Files.writeString(projectDir.resolve("top.py"), "x = 1");
        Files.writeString(subDir.resolve("nested.py"), "y = 2");

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources).hasSize(2);
        // Source paths must be relative to the project directory
        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().toString())
                .containsExactlyInAnyOrder("top.py", "subpackage/nested.py");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void excludesPycacheByDefault() throws IOException {
        Path projectDir = tempDir.resolve("with_cache");
        Path cacheDir = projectDir.resolve("__pycache__");
        Files.createDirectories(cacheDir);

        Files.writeString(projectDir.resolve("main.py"), "x = 1");
        Files.writeString(cacheDir.resolve("main.cpython-312.pyc"), "# compiled");

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getSourcePath().toString()).doesNotContain("__pycache__");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void parsesEmptyDirectory() throws IOException {
        Path projectDir = tempDir.resolve("empty");
        Files.createDirectories(projectDir);

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources).isEmpty();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void parsesAbsolutePath() throws IOException {
        Path projectDir = tempDir.resolve("absolute");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("test.py"), "pass");

        // Use absolute path explicitly
        Path absolutePath = projectDir.toAbsolutePath();

        List<SourceFile> sources = client()
                .parseProject(absolutePath, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources).hasSize(1);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void includesPyprojectToml() throws IOException {
        Path projectDir = tempDir.resolve("with_pyproject");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("main.py"), "x = 1");
        Files.writeString(projectDir.resolve("pyproject.toml"), """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = ["requests>=2.28.0"]
                """);

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().getFileName().toString())
                .contains("main.py", "pyproject.toml");

        SourceFile pyproject = sources.stream()
                .filter(sf -> sf.getSourcePath().getFileName().toString().equals("pyproject.toml"))
                .findFirst()
                .orElseThrow();
        assertThat(pyproject).isInstanceOf(Toml.Document.class);
        assertThat(pyproject.getMarkers().findFirst(PythonResolutionResult.class)).isPresent();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void includesRequirementsTxt() throws IOException {
        Path projectDir = tempDir.resolve("with_requirements");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("main.py"), "x = 1");
        Files.writeString(projectDir.resolve("requirements.txt"), """
                requests>=2.28.0
                click>=8.0
                """);

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().getFileName().toString())
                .contains("main.py", "requirements.txt");

        SourceFile reqsTxt = sources.stream()
                .filter(sf -> sf.getSourcePath().getFileName().toString().equals("requirements.txt"))
                .findFirst()
                .orElseThrow();
        assertThat(reqsTxt).isInstanceOf(PlainText.class);
        assertThat(reqsTxt.getMarkers().findFirst(PythonResolutionResult.class)).isPresent();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void includesPipfile() throws IOException {
        Path projectDir = tempDir.resolve("with_pipfile");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("main.py"), "x = 1");
        Files.writeString(projectDir.resolve("Pipfile"), """
                [packages]
                requests = ">=2.28.0"
                """);
        Files.writeString(projectDir.resolve("Pipfile.lock"), """
                {
                    "_meta": {"sources": [{"url": "https://pypi.org/simple"}]},
                    "default": {"requests": {"version": "==2.31.0"}},
                    "develop": {}
                }
                """);

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().getFileName().toString())
                .contains("main.py", "Pipfile", "Pipfile.lock");

        SourceFile pipfile = sources.stream()
                .filter(sf -> sf.getSourcePath().getFileName().toString().equals("Pipfile"))
                .findFirst()
                .orElseThrow();
        assertThat(pipfile).isInstanceOf(Toml.Document.class);
        assertThat(pipfile.getMarkers().findFirst(PythonResolutionResult.class)).isPresent();

        SourceFile pipfileLock = sources.stream()
                .filter(sf -> sf.getSourcePath().getFileName().toString().equals("Pipfile.lock"))
                .findFirst()
                .orElseThrow();
        assertThat(pipfileLock).isInstanceOf(Json.Document.class);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void pyprojectTomlTakesPriorityOverRequirementsTxt() throws IOException {
        Path projectDir = tempDir.resolve("both_manifests");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("main.py"), "x = 1");
        Files.writeString(projectDir.resolve("pyproject.toml"), """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = ["requests>=2.28.0"]
                """);
        Files.writeString(projectDir.resolve("requirements.txt"), """
                requests>=2.28.0
                """);

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        // pyproject.toml should be included but not requirements.txt
        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().getFileName().toString())
                .contains("pyproject.toml")
                .doesNotContain("requirements.txt");
    }


    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void includesAllRequirementsTxtFiles() throws IOException {
        Path projectDir = tempDir.resolve("multi_requirements");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("main.py"), "x = 1");
        Files.writeString(projectDir.resolve("requirements.txt"), """
                requests>=2.28.0
                """);
        Files.writeString(projectDir.resolve("requirements-dev.txt"), """
                pytest>=7.0
                """);

        List<SourceFile> sources = client()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        assertThat(sources)
                .extracting(sf -> sf.getSourcePath().getFileName().toString())
                .contains("main.py", "requirements.txt", "requirements-dev.txt");

        SourceFile reqsTxt = sources.stream()
                .filter(s -> s.getSourcePath().getFileName().toString().equals("requirements.txt"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing requirements.txt"));
        SourceFile reqsDevTxt = sources.stream()
                .filter(s -> s.getSourcePath().getFileName().toString().equals("requirements-dev.txt"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing requirements-dev.txt"));

        assertThat(reqsTxt).isInstanceOf(PlainText.class);
        assertThat(reqsDevTxt).isInstanceOf(PlainText.class);

        PythonResolutionResult baseMarker = reqsTxt.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        PythonResolutionResult devMarker = reqsDevTxt.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(baseMarker).as("PythonResolutionResult marker on requirements.txt").isNotNull();
        assertThat(devMarker).as("PythonResolutionResult marker on requirements-dev.txt").isNotNull();

        // Each file should have its own distinct marker pointing to its own path
        assertThat(baseMarker.getPath()).isEqualTo("requirements.txt");
        assertThat(devMarker.getPath()).isEqualTo("requirements-dev.txt");
    }

    private PythonRewriteRpc client() {
        return PythonRewriteRpc.getOrStart();
    }
}
