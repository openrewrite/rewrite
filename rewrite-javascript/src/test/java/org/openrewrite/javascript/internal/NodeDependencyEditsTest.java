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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.yaml.YamlParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

/**
 * A dependency recipe's decisions read the catalogs each manifest's package manager reads, as the recipes before it
 * in the cycle left them.
 */
class NodeDependencyEditsTest {

    @Test
    void eachManifestGetsTheCatalogsItsPackageManagerReads() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        NodeDependencyEdits acc = pnpmWorkspace();
        NodeCatalogs catalogs = requireNonNull(acc.of(ctx, MEMBER));
        assertThat(catalogs.resolve("wrappy", "catalog:")).isEqualTo("1.0.2");
        assertThat(catalogs.resolve("ms", "catalog:legacy")).isEqualTo("2.1.3");
        SourceFile yarn = json("{\"name\":\"b\"}").withSourcePath(Paths.get("tools/package.json"));
        acc.scan(yarn.withMarkers(yarn.getMarkers().add(nodeResolutionResult(PackageManager.YarnBerry))));
        assertThat(acc.of(ctx, Paths.get("tools/package.json"))).as("Yarn reads no .yarnrc.yml here").isNull();
    }

    @Test
    void eachRecipeReadsTheCatalogsAsTheRecipesBeforeItLeftThem() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        NodeDependencyEdits first = pnpmWorkspace();
        NodeDependencyEdits second = pnpmWorkspace();
        first.decideAll(ctx, (decision, c) ->
          decision.editCatalogs(cs -> cs.withSpecifier("default", "wrappy", "1.0.3")));
        List<String> seen = new ArrayList<>();
        second.decideAll(ctx, (decision, c) -> {
            NodeCatalogs catalogs = decision.catalogs();
            if (catalogs != null) {
                seen.add(decision.getPath() + " " + catalogs.specifier("default", "wrappy"));
                decision.editCatalogs(cs -> cs.withSpecifier("legacy", "ms", "2.1.2"));
            }
        });
        assertThat(seen).containsExactly("packages/a/package.json 1.0.3");
        NodeCatalogs firstSees = requireNonNull(first.of(ctx, MEMBER));
        assertThat(firstSees.specifier("default", "wrappy")).isEqualTo("1.0.3");
        assertThat(firstSees.specifier("legacy", "ms")).as("a later recipe's edit").isEqualTo("2.1.3");
        NodeCatalogs secondSees = requireNonNull(second.of(ctx, MEMBER));
        assertThat(secondSees.specifier("default", "wrappy")).isEqualTo("1.0.3");
        assertThat(secondSees.specifier("legacy", "ms")).isEqualTo("2.1.2");
    }

    @Test
    void theWorkspaceIsLinkedOncePerScanThoughARunAsksForAVisitorPerFile() {
        NodeDependencyEdits acc = pnpmWorkspace();
        acc.decideAll(new InMemoryExecutionContext(), (decision, c) -> {
        });
        Path unlinked = Paths.get("not/linked/package.json");
        acc.scan.installRoots.put(unlinked, LockFileRegeneration.InstallRoot.HERE);
        acc.visitor((decision, c) -> {
        });
        acc.visitor((decision, c) -> {
        });
        assertThat(acc.scan.installRoots).containsKey(unlinked);
        acc.scan(PNPM_MEMBER);
        acc.visitor((decision, c) -> {
        });
        assertThat(acc.scan.installRoots).doesNotContainKey(unlinked).containsKey(MEMBER);
    }

    /**
     * corepack 0.35.0's reading (recorded): packageManager over devEngines.packageManager, which must name an exact
     * version, no array; a spec it rejects is its Rejected; bun, which corepack does not run, by its name.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{\"packageManager\":\"yarn@1.22.22+sha512.abc\"}                                | YarnClassic |",
      "{\"packageManager\":\"yarn@4.10.3\"}                                            | YarnBerry   |",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\",\"version\":\"1.22.22\"}}} | YarnClassic |",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\",\"version\":\"^1.22.0\"}}} |             |" +
      " expected a semver version",
      "{\"devEngines\":{\"packageManager\":[{\"name\":\"pnpm\",\"version\":\"10.x\"}]}} |             |",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"}},\"packageManager\":\"yarn@4.10.3\"} | |" +
      " does not match the \"devEngines.packageManager\" field",
      "{\"packageManager\":\"yarn@1\"}                                                 |             |" +
      " expected a semver version",
      "{\"packageManager\":\"bun@1.3.10\"}                                             | Bun         |",
      "{\"packageManager\":\"npm@11.17.0\"}                                            | Npm         |",
      "{\"packageManager\":\"deno@2.0.0\"}                                             |             |" +
      " Unsupported package manager specification (deno@2.0.0)"
    })
    void theManifestNamesThePackageManagerItsMarkerDoesNot(String manifest, @Nullable PackageManager named,
                                                           @Nullable String declined) {
        NodeDependencyEdits acc = new NodeDependencyEdits(new NodeDependencyEdits.Rows() {
            @Override
            public void failure(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Failure failure) {
            }

            @Override
            public void declined(ExecutionContext ctx, Path path, String reason) {
            }
        });
        SourceFile root = json(manifest);
        acc.scan(root.withMarkers(root.getMarkers().add(nodeResolutionResult(null))));
        List<@Nullable PackageManager> seen = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        acc.decideAll(new InMemoryExecutionContext(), (decision, c) -> {
            try {
                seen.add(decision.getPackageManager());
            } catch (Rejected e) {
                seen.add(null);
                reasons.add(String.valueOf(e.getMessage()));
            }
        });
        assertThat(seen).containsExactly(named);
        if (declined == null) {
            assertThat(reasons).isEmpty();
        } else {
            assertThat(reasons).hasSize(1);
            assertThat(reasons.get(0)).contains(declined).endsWith("(corepack)");
        }
    }

    private static final Path MEMBER = Paths.get("packages/a/package.json");

    private static NodeDependencyEdits pnpmWorkspace() {
        NodeDependencyEdits acc = new NodeDependencyEdits(new NodeDependencyEdits.Rows() {
            @Override
            public void failure(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Failure failure) {
            }

            @Override
            public void declined(ExecutionContext ctx, Path path, String reason) {
            }
        });
        acc.scan(WORKSPACE);
        acc.scan(PNPM_MEMBER);
        return acc;
    }

    private static final SourceFile WORKSPACE = yaml(
      "packages:\n  - packages/*\ncatalog:\n  wrappy: 1.0.2\ncatalogs:\n  legacy:\n    ms: 2.1.3\n");

    private static final SourceFile PNPM_MEMBER = pnpmMember();

    private static SourceFile pnpmMember() {
        SourceFile member = json("{\"name\":\"a\",\"dependencies\":{\"wrappy\":\"catalog:\"}}").withSourcePath(MEMBER);
        return member.withMarkers(member.getMarkers().add(nodeResolutionResult(PackageManager.Pnpm)));
    }

    private static SourceFile yaml(String text) {
        return YamlParser.builder().build().parse(text).findFirst().orElseThrow(IllegalStateException::new)
          .withSourcePath(Paths.get("pnpm-workspace.yaml"));
    }

    private static SourceFile json(String text) {
        return JsonParser.builder().build().parse(text).findFirst().orElseThrow(IllegalStateException::new)
          .withSourcePath(Paths.get("package.json"));
    }
}
