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
package org.openrewrite.golang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.golang.marker.GoResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class GoModParserSumHashesTest {

    private static final String GO_SUM =
            "github.com/google/uuid v1.6.0 h1:zip=\n" +
            "github.com/google/uuid v1.6.0/go.mod h1:mod=\n" +
            "golang.org/x/mod v0.27.0 h1:old=\n";

    private static ResolvedDependency selected(String path, String version) {
        return new ResolvedDependency(path, version, null, null, false, false, null, null, null, true, null);
    }

    @Test
    void relativeSourcePathWithoutProjectRootReadsNothing() {
        assertThat(GoModParser.parseSumSibling(Paths.get("go.mod"), null))
                .as("a repo-relative path must not be resolved against the working directory")
                .isEmpty();
    }

    @Test
    void projectRootTurnsRelativePathIntoLocation(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("app"));
        Files.write(root.resolve("app/go.sum"), GO_SUM.getBytes(StandardCharsets.UTF_8));

        List<ResolvedDependency> resolved = GoModParser.parseSumSibling(Paths.get("app/go.mod"), root);

        assertThat(resolved).extracting(ResolvedDependency::getModulePath)
                .containsExactlyInAnyOrder("github.com/google/uuid", "golang.org/x/mod");
        assertThat(resolved).allSatisfy(d ->
                assertThat(d.isSelected()).as("go.sum rows are hashes, not a build list").isFalse());
    }

    @Test
    void absoluteSourcePathNeedsNoProjectRoot(@TempDir Path root) throws IOException {
        Files.write(root.resolve("go.sum"), GO_SUM.getBytes(StandardCharsets.UTF_8));

        assertThat(GoModParser.parseSumSibling(root.resolve("go.mod"), null)).hasSize(2);
    }

    @Test
    void mergeJoinsHashesOntoBuildListAndKeepsItSelected() {
        List<ResolvedDependency> buildList = singletonList(selected("github.com/google/uuid", "v1.6.0"));

        List<ResolvedDependency> merged = GoModParser.mergeSumHashes(buildList, GoModParser.parseSumContent(GO_SUM));

        assertThat(merged).filteredOn(ResolvedDependency::isSelected).singleElement().satisfies(d -> {
            assertThat(d.getModulePath()).isEqualTo("github.com/google/uuid");
            assertThat(d.getModuleHash()).isEqualTo("h1:zip=");
            assertThat(d.getGoModHash()).isEqualTo("h1:mod=");
        });
    }

    @Test
    void mergeKeepsUnmatchedSumRowsUnselected() {
        List<ResolvedDependency> buildList = Arrays.asList(
                selected("github.com/google/uuid", "v1.6.0"),
                selected("golang.org/x/mod", "v0.35.0"));

        List<ResolvedDependency> merged = GoModParser.mergeSumHashes(buildList, GoModParser.parseSumContent(GO_SUM));

        assertThat(merged).filteredOn(d -> !d.isSelected()).singleElement().satisfies(d -> {
            assertThat(d.getModulePath()).isEqualTo("golang.org/x/mod");
            assertThat(d.getVersion()).isEqualTo("v0.27.0");
        });
    }

    @Test
    void mergeWithoutBuildListYieldsHashInventory() {
        List<ResolvedDependency> fromSum = GoModParser.parseSumContent(GO_SUM);

        assertThat(GoModParser.mergeSumHashes(emptyList(), fromSum)).isEqualTo(fromSum);
        assertThat(GoModParser.mergeSumHashes(null, fromSum)).isEqualTo(fromSum);
    }
}
