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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.tree.Go;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for {@code handleParseProject}'s module-context
 * inference (item 8 of the rewrite-go parity plan).
 * <p>
 * Each .go file in the discovered project tree must resolve against its
 * closest-ancestor go.mod, not the project root's. The owning
 * {@link GoResolutionResult} marker is attached to each compilation unit
 * so Java-side recipes can read module dependencies without re-parsing
 * go.mod themselves.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ParseProjectModuleContextTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path projectDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void singleModuleAttachesResolutionResult() throws Exception {
        // Single root go.mod + two .go files in different packages.
        write(projectDir.resolve("go.mod"), """
                module example.com/foo

                go 1.22

                require github.com/google/uuid v1.6.0
                """);
        write(projectDir.resolve("main.go"), """
                package main

                func main() {}
                """);
        write(projectDir.resolve("sub/sub.go"), """
                package sub

                func Hello() string { return "hi" }
                """);

        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        List<SourceFile> sources = rpc.parseProject(projectDir, new InMemoryExecutionContext()).collect(Collectors.toList());

        // Both .go files should be parsed and carry the same GoResolutionResult.
        List<Go.CompilationUnit> cus = sources.stream()
                .filter(s -> s instanceof Go.CompilationUnit)
                .map(s -> (Go.CompilationUnit) s)
                .collect(Collectors.toList());
        assertThat(cus).as("expected 2 .go compilation units").hasSize(2);

        for (Go.CompilationUnit cu : cus) {
            GoResolutionResult mrr = cu.getMarkers().findFirst(GoResolutionResult.class).orElseThrow(
                    () -> new AssertionError("missing GoResolutionResult on " + cu.getSourcePath()));
            assertThat(mrr.getModulePath()).isEqualTo("example.com/foo");
            assertThat(mrr.getGoVersion()).isEqualTo("1.22");
            assertThat(mrr.getRequires())
                    .extracting(GoResolutionResult.Require::getModulePath)
                    .contains("github.com/google/uuid");
        }
    }

    @Test
    void nestedSubmoduleResolvesAgainstClosestAncestor() throws Exception {
        // Root go.mod + nested submodule with its own go.mod. Each .go file
        // must resolve against its closest-ancestor go.mod, not the root.
        write(projectDir.resolve("go.mod"), """
                module example.com/root

                go 1.22
                """);
        write(projectDir.resolve("main.go"), """
                package main

                func main() {}
                """);
        write(projectDir.resolve("nested/go.mod"), """
                module example.com/nested

                go 1.22
                """);
        write(projectDir.resolve("nested/lib.go"), """
                package nested

                func Lib() string { return "nested" }
                """);

        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        List<SourceFile> sources = rpc.parseProject(projectDir, new InMemoryExecutionContext()).collect(Collectors.toList());

        Go.CompilationUnit rootCu = findBySuffix(sources, "main.go");
        Go.CompilationUnit nestedCu = findBySuffix(sources, "nested/lib.go");

        assertThat(rootCu.getMarkers().findFirst(GoResolutionResult.class).orElseThrow().getModulePath())
                .isEqualTo("example.com/root");
        assertThat(nestedCu.getMarkers().findFirst(GoResolutionResult.class).orElseThrow().getModulePath())
                .isEqualTo("example.com/nested");
    }

    @Test
    void noGoModLeavesCompilationUnitsUnattributed() throws Exception {
        // Without any go.mod the project still parses, but no module
        // resolution marker is attached.
        write(projectDir.resolve("main.go"), """
                package main

                func main() {}
                """);

        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        List<SourceFile> sources = rpc.parseProject(projectDir, new InMemoryExecutionContext()).collect(Collectors.toList());

        Go.CompilationUnit cu = findBySuffix(sources, "main.go");
        assertThat(cu.getMarkers().findFirst(GoResolutionResult.class)).isEmpty();
    }

    private static Go.CompilationUnit findBySuffix(List<SourceFile> sources, String suffix) {
        return sources.stream()
                .filter(s -> s instanceof Go.CompilationUnit)
                .map(s -> (Go.CompilationUnit) s)
                .filter(cu -> cu.getSourcePath().toString().replace('\\', '/').endsWith(suffix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no source ending with " + suffix +
                        "; got " + sources.stream().map(SourceFile::getSourcePath).collect(Collectors.toList())));
    }

    private static void write(Path path, String content) throws java.io.IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
