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
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.goMod;

class GoModParserTest implements RewriteTest {

    @Test
    void moduleAndGoDirective() {
        rewriteRun(
                goMod(
                        """
                                module github.com/foo/bar

                                go 1.21
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getModulePath()).isEqualTo("github.com/foo/bar");
                            assertThat(marker.getGoVersion()).isEqualTo("1.21");
                            assertThat(marker.getToolchain()).isNull();
                            assertThat(marker.getRequires()).isEmpty();
                        })
                )
        );
    }

    @Test
    void toolchainDirective() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.22

                                toolchain go1.22.0
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getToolchain()).isEqualTo("go1.22.0");
                        })
                )
        );
    }

    @Test
    void requireSingleLine() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require github.com/stretchr/testify v1.8.0
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getRequires()).hasSize(1);
                            GoResolutionResult.Require r = marker.getRequires().getFirst();
                            assertThat(r.getModulePath()).isEqualTo("github.com/stretchr/testify");
                            assertThat(r.getVersion()).isEqualTo("v1.8.0");
                            assertThat(r.isIndirect()).isFalse();
                        })
                )
        );
    }

    @Test
    void requireBlockWithIndirect() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                require (
                                	github.com/foo/bar v1.0.0
                                	github.com/baz/qux v2.1.3 // indirect
                                )
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getRequires()).hasSize(2);

                            GoResolutionResult.Require r1 = marker.getRequires().get(0);
                            assertThat(r1.getModulePath()).isEqualTo("github.com/foo/bar");
                            assertThat(r1.getVersion()).isEqualTo("v1.0.0");
                            assertThat(r1.isIndirect()).isFalse();

                            GoResolutionResult.Require r2 = marker.getRequires().get(1);
                            assertThat(r2.getModulePath()).isEqualTo("github.com/baz/qux");
                            assertThat(r2.getVersion()).isEqualTo("v2.1.3");
                            assertThat(r2.isIndirect()).isTrue();
                        })
                )
        );
    }

    @Test
    void replaceSingleLine() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                replace github.com/old/mod => github.com/new/mod v1.2.3
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getReplaces()).hasSize(1);
                            GoResolutionResult.Replace rep = marker.getReplaces().getFirst();
                            assertThat(rep.getOldPath()).isEqualTo("github.com/old/mod");
                            assertThat(rep.getOldVersion()).isNull();
                            assertThat(rep.getNewPath()).isEqualTo("github.com/new/mod");
                            assertThat(rep.getNewVersion()).isEqualTo("v1.2.3");
                        })
                )
        );
    }

    @Test
    void replaceWithLocalPath() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                replace github.com/old/mod v1.0.0 => ../local/path
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getReplaces()).hasSize(1);
                            GoResolutionResult.Replace rep = marker.getReplaces().getFirst();
                            assertThat(rep.getOldPath()).isEqualTo("github.com/old/mod");
                            assertThat(rep.getOldVersion()).isEqualTo("v1.0.0");
                            assertThat(rep.getNewPath()).isEqualTo("../local/path");
                            assertThat(rep.getNewVersion()).isNull();
                        })
                )
        );
    }

    @Test
    void excludeDirective() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                exclude github.com/bad/pkg v1.2.3
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getExcludes()).hasSize(1);
                            GoResolutionResult.Exclude exc = marker.getExcludes().getFirst();
                            assertThat(exc.getModulePath()).isEqualTo("github.com/bad/pkg");
                            assertThat(exc.getVersion()).isEqualTo("v1.2.3");
                        })
                )
        );
    }

    @Test
    void retractWithRationale() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                retract v1.0.0 // buggy release
                                retract [v1.1.0, v1.1.5]
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getRetracts()).hasSize(2);

                            GoResolutionResult.Retract r1 = marker.getRetracts().get(0);
                            assertThat(r1.getVersionRange()).isEqualTo("v1.0.0");
                            assertThat(r1.getRationale()).isEqualTo("buggy release");

                            GoResolutionResult.Retract r2 = marker.getRetracts().get(1);
                            assertThat(r2.getVersionRange()).isEqualTo("[v1.1.0, v1.1.5]");
                            assertThat(r2.getRationale()).isNull();
                        })
                )
        );
    }

    @Test
    void replaceBlock() {
        rewriteRun(
                goMod(
                        """
                                module example.com/m

                                go 1.21

                                replace (
                                	github.com/a/a => github.com/fork/a v1.0.0
                                	github.com/b/b v2.0.0 => ../b-local
                                )
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getReplaces()).hasSize(2);

                            GoResolutionResult.Replace r1 = marker.getReplaces().get(0);
                            assertThat(r1.getOldPath()).isEqualTo("github.com/a/a");
                            assertThat(r1.getOldVersion()).isNull();
                            assertThat(r1.getNewPath()).isEqualTo("github.com/fork/a");
                            assertThat(r1.getNewVersion()).isEqualTo("v1.0.0");

                            GoResolutionResult.Replace r2 = marker.getReplaces().get(1);
                            assertThat(r2.getOldPath()).isEqualTo("github.com/b/b");
                            assertThat(r2.getOldVersion()).isEqualTo("v2.0.0");
                            assertThat(r2.getNewPath()).isEqualTo("../b-local");
                            assertThat(r2.getNewVersion()).isNull();
                        })
                )
        );
    }

    @Test
    void noModuleDirectiveProducesNoMarker() {
        rewriteRun(
                goMod(
                        """
                                go 1.21
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            assertThat(doc.getMarkers().findFirst(GoResolutionResult.class)).isEmpty();
                        })
                )
        );
    }

    @Test
    void commentsDoNotBreakParsing() {
        rewriteRun(
                goMod(
                        """
                                // top-level comment
                                module example.com/m  // module comment

                                go 1.21  // toolchain pin

                                // list of deps
                                require (
                                	// important dep
                                	github.com/foo/bar v1.0.0
                                	github.com/baz/qux v2.0.0 // indirect
                                )
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            GoResolutionResult marker = extractMarker(doc);
                            assertThat(marker.getModulePath()).isEqualTo("example.com/m");
                            assertThat(marker.getGoVersion()).isEqualTo("1.21");
                            assertThat(marker.getRequires()).hasSize(2);
                        })
                )
        );
    }

    private static GoResolutionResult extractMarker(PlainText doc) {
        List<GoResolutionResult> found = doc.getMarkers().findAll(GoResolutionResult.class);
        assertThat(found).as("GoResolutionResult marker").hasSize(1);
        return found.getFirst();
    }
}
