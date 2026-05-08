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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.marker.GoProject;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-language round-trip for {@link GoProject} and {@link GoResolutionResult}.
 * <p>
 * Both markers implement {@code RpcCodec} on the Java side and have matching
 * dispatch in the Go {@code pkg/rpc} codec (see
 * {@code go_resolution_result_codec.go}). This test exercises the
 * Java → Go → Java path by:
 * <ol>
 *   <li>Parsing a Go source via the RPC subprocess.</li>
 *   <li>Attaching populated marker instances to the resulting LST.</li>
 *   <li>Sending the LST back to Go via {@code rpc.print(...)} — Go must
 *       deserialize the markers without error.</li>
 *   <li>Asserting the printed source matches the input, proving the
 *       round-trip didn't truncate or lose data.</li>
 * </ol>
 * Field-order or name-mapping divergence between the two languages causes
 * receive-side panics or empty markers, both of which fail this test.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class MarkerRoundTripTest {

    @TempDir
    Path tempDir;

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
    void goProjectMarkerRoundTripsViaPrint() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc main() {\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        GoProject marker = new GoProject(UUID.randomUUID(), "example/foo");
        cu = cu.withMarkers(cu.getMarkers().addIfAbsent(marker));

        // Force the marker through Go's receive codec.
        String printed = rpc.print(cu);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void goResolutionResultMarkerRoundTripsViaPrint() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc main() {\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        GoResolutionResult marker = new GoResolutionResult(
                UUID.randomUUID(),
                "example.com/foo",
                "1.22",
                "go1.22.5",
                "/tmp/go.mod",
                Arrays.asList(
                        new GoResolutionResult.Require("github.com/google/uuid", "v1.6.0", false),
                        new GoResolutionResult.Require("golang.org/x/mod", "v0.35.0", true)
                ),
                Arrays.asList(
                        new GoResolutionResult.Replace("github.com/x/y", null, "../local/y", null),
                        new GoResolutionResult.Replace("github.com/a/b", "v1.0.0", "github.com/forked/b", "v1.0.1")
                ),
                Collections.singletonList(
                        new GoResolutionResult.Exclude("github.com/bad", "v0.0.1")
                ),
                Arrays.asList(
                        new GoResolutionResult.Retract("v0.0.5", "deleted main.go"),
                        new GoResolutionResult.Retract("[v1.0.0, v1.0.5]", null)
                ),
                Collections.singletonList(
                        new GoResolutionResult.ResolvedDependency(
                                "github.com/google/uuid", "v1.6.0",
                                "h1:NIvaJDMOsjHA8n1jAhLSgzrAzy1Hgr+hNrb57e+94F0=",
                                "h1:TIyPZe4MgqvfeYDBFedMoGGpEw/LqOeaOT+nhxU+yHo=")
                )
        );
        cu = cu.withMarkers(cu.getMarkers().addIfAbsent(marker));

        String printed = rpc.print(cu);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void emptyGoResolutionResultRoundTripsViaPrint() {
        // Mirrors the descriptor empty-list fix: collections that are empty
        // must serialize as empty arrays so the receive side doesn't read
        // null and panic.
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc main() {\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        GoResolutionResult marker = new GoResolutionResult(
                UUID.randomUUID(),
                "example.com/empty",
                null,
                null,
                "go.mod",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        cu = cu.withMarkers(cu.getMarkers().addIfAbsent(marker));

        String printed = rpc.print(cu);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void bothMarkersTogetherRoundTripViaPrint() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc main() {\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        Markers markers = cu.getMarkers()
                .addIfAbsent(new GoProject(UUID.randomUUID(), "example/foo"))
                .addIfAbsent(new GoResolutionResult(
                        UUID.randomUUID(),
                        "example.com/foo",
                        "1.22",
                        null,
                        "go.mod",
                        Collections.singletonList(
                                new GoResolutionResult.Require("github.com/google/uuid", "v1.6.0", false)
                        ),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
        cu = cu.withMarkers(markers);

        String printed = rpc.print(cu);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    @SuppressWarnings("unused")
    void roundTripPreservesGoResolutionResultFieldsViaVisit() {
        // Exercises the full Java → Go → Java path: Visit RPC sends the LST
        // (with attached markers) to Go, runs a no-op recipe, and ships the
        // result back. Markers on the input must be preserved.
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc f() {\n\tvar x = true\n\t_ = x\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        UUID projectId = UUID.randomUUID();
        UUID gomodId = UUID.randomUUID();
        cu = cu.withMarkers(cu.getMarkers()
                .addIfAbsent(new GoProject(projectId, "example/foo"))
                .addIfAbsent(new GoResolutionResult(
                        gomodId, "example.com/foo", "1.22", null, "go.mod",
                        Collections.singletonList(
                                new GoResolutionResult.Require("github.com/google/uuid", "v1.6.0", false)),
                        Collections.emptyList(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList())));

        var recipe = rpc.prepareRecipe("org.openrewrite.golang.test.RenameXToFlag");
        Tree result = recipe.getVisitor().visit(cu, new org.openrewrite.InMemoryExecutionContext());
        assertThat(result).isInstanceOf(SourceFile.class);

        Markers resultMarkers = ((SourceFile) result).getMarkers();
        GoProject project = resultMarkers.findFirst(GoProject.class).orElseThrow(
                () -> new AssertionError("GoProject marker missing from round-trip result"));
        assertThat(project.getId()).isEqualTo(projectId);
        assertThat(project.getProjectName()).isEqualTo("example/foo");

        GoResolutionResult mrr = resultMarkers.findFirst(GoResolutionResult.class).orElseThrow(
                () -> new AssertionError("GoResolutionResult marker missing from round-trip result"));
        assertThat(mrr.getId()).isEqualTo(gomodId);
        assertThat(mrr.getModulePath()).isEqualTo("example.com/foo");
        assertThat(mrr.getRequires()).hasSize(1);
        assertThat(mrr.getRequires().get(0).getModulePath()).isEqualTo("github.com/google/uuid");
    }
}
