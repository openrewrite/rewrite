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
package org.openrewrite.golang.search;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.marker.GoResolutionResult.ModuleRef;
import org.openrewrite.golang.marker.GoResolutionResult.ResolvedDependency;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.table.GoDependenciesInUse;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.goMod;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class DependencyInsightTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void findDirectRequireByExactName() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/stretchr/testify", null, null))
                        .dataTable(GoDependenciesInUse.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            GoDependenciesInUse.Row row = rows.getFirst();
                            assertThat(row.getModulePath()).isEqualTo("example.com/m");
                            assertThat(row.getDependencyModule()).isEqualTo("github.com/stretchr/testify");
                            assertThat(row.getVersionConstraint()).isEqualTo("v1.8.0");
                            assertThat(row.getDirect()).isTrue();
                            assertThat(row.getCount()).isEqualTo(1);
                        }),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/stretchr/testify v1.8.0
                                """,
                        """
                                module example.com/m

                                go 1.21

                                require /*~~>*/github.com/stretchr/testify v1.8.0
                                """
                )
        );
    }

    @Test
    void findWithGlobPattern() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/google/*", null, null)),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require (
                                	github.com/google/uuid v1.6.0
                                	github.com/stretchr/testify v1.8.0
                                )
                                """,
                        """
                                module example.com/m

                                go 1.21

                                require (
                                	/*~~>*/github.com/google/uuid v1.6.0
                                	github.com/stretchr/testify v1.8.0
                                )
                                """
                )
        );
    }

    @Test
    void versionSelectorFiltersMatches() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/stretchr/testify", "2.x", null)),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/stretchr/testify v1.8.0
                                """
                )
        );
    }

    @Test
    void versionSelectorMatches() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/stretchr/testify", "1.x", null)),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/stretchr/testify v1.8.0
                                """,
                        """
                                module example.com/m

                                go 1.21

                                require /*~~>*/github.com/stretchr/testify v1.8.0
                                """
                )
        );
    }

    @Test
    void noMatchLeavesSourceUnchanged() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/nonexistent/pkg", null, null)),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/stretchr/testify v1.8.0
                                """
                )
        );
    }

    @Test
    void markerLandsOnModulePathValue() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/stretchr/testify", null, null)),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/stretchr/testify v1.8.0
                                """,
                        """
                                module example.com/m

                                go 1.21

                                require /*~~>*/github.com/stretchr/testify v1.8.0
                                """,
                        spec -> spec.afterRecipe(doc -> assertThat(markedModulePaths(doc))
                                .containsExactly("github.com/stretchr/testify"))
                )
        );
    }

    @Test
    void findTransitiveDependency() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/transitive/target", null, null))
                        .dataTable(GoDependenciesInUse.Row.class, rows -> {
                            assertThat(rows).anySatisfy(row -> {
                                assertThat(row.getDependencyModule()).isEqualTo("github.com/direct/dep");
                                assertThat(row.getDirect()).isTrue();
                            });
                            assertThat(rows).anySatisfy(row -> {
                                assertThat(row.getDependencyModule()).isEqualTo("github.com/transitive/target");
                                assertThat(row.getDirect()).isFalse();
                                assertThat(row.getVersion()).isEqualTo("v2.0.0");
                            });
                        }),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/direct/dep v1.0.0
                                """,
                        """
                                module example.com/m

                                go 1.21

                                require /*~~>*/github.com/direct/dep v1.0.0
                                """,
                        spec -> spec.mapBeforeRecipe(DependencyInsightTest::withSyntheticGraph)
                )
        );
    }

    @Test
    void onlyDirectSkipsTransitive() {
        rewriteRun(
                spec -> spec.recipe(new DependencyInsight("github.com/transitive/target", null, true)),
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/direct/dep v1.0.0
                                """,
                        spec -> spec.mapBeforeRecipe(DependencyInsightTest::withSyntheticGraph)
                )
        );
    }

    private static GoMod withSyntheticGraph(GoMod goMod) {
        GoResolutionResult resolution = goMod.getMarkers().findFirst(GoResolutionResult.class).orElseThrow();
        ResolvedDependency target = new ResolvedDependency("github.com/transitive/target", "v2.0.0",
                null, null, true, false, null, null, null, null);
        ResolvedDependency direct = new ResolvedDependency("github.com/direct/dep", "v1.0.0",
                null, null, false, false, null, null, null,
                singletonList(new ModuleRef("github.com/transitive/target", "v2.0.0")));
        GoResolutionResult withGraph = resolution.withResolvedDependencies(List.of(direct, target));
        return goMod.withMarkers(goMod.getMarkers().computeByType(withGraph, (existing, replacement) -> replacement));
    }

    private static List<String> markedModulePaths(GoMod doc) {
        List<String> marked = new ArrayList<>();
        collectMarked(doc.getStatements(), marked);
        return marked;
    }

    private static void collectMarked(List<org.openrewrite.java.tree.JRightPadded<GoMod.GoModStatement>> statements,
                                      List<String> marked) {
        for (org.openrewrite.java.tree.JRightPadded<GoMod.GoModStatement> rp : statements) {
            GoMod.GoModStatement statement = rp.getElement();
            if (statement instanceof GoMod.Directive) {
                GoMod.Directive d = (GoMod.Directive) statement;
                for (GoMod.Value value : d.getValues()) {
                    if (value.getMarkers().findFirst(SearchResult.class).isPresent()) {
                        marked.add(value.getText());
                    }
                }
            } else if (statement instanceof GoMod.Block) {
                collectMarked(((GoMod.Block) statement).getEntries(), marked);
            }
        }
    }
}
