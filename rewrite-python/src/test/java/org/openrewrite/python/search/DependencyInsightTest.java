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
package org.openrewrite.python.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.python.table.PythonDependenciesInUse;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.*;


class DependencyInsightTest implements RewriteTest {

    @Test
    void findDirectDependencyWithDataTable(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("requests", null, null))
            .dataTable(PythonDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                PythonDependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getProjectName()).isEqualTo("myapp");
                assertThat(row.getPackageName()).isEqualTo("requests");
                assertThat(row.getVersion()).isNotNull();
                assertThat(row.getVersionConstraint()).isEqualTo(">=2.28.0");
                assertThat(row.getScope()).isEqualTo("dependencies");
                assertThat(row.getDirect()).isTrue();
                assertThat(row.getCount()).isEqualTo(1);
            }),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                    "click>=8.0",
                ]
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    ~~>\"requests>=2.28.0",
                    "click>=8.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void findTransitiveDependencyWithDataTable(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("certifi", null, null))
            .dataTable(PythonDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
                // Should have the direct dep (requests) that leads to certifi
                assertThat(rows).anySatisfy(row -> {
                    assertThat(row.getPackageName()).isEqualTo("requests");
                    assertThat(row.getDirect()).isTrue();
                    assertThat(row.getScope()).isEqualTo("dependencies");
                });
                // Should have the transitive dep (certifi) itself
                assertThat(rows).anySatisfy(row -> {
                    assertThat(row.getPackageName()).isEqualTo("certifi");
                    assertThat(row.getDirect()).isFalse();
                    assertThat(row.getVersion()).isNotNull();
                });
            }),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                    "click>=8.0",
                ]
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    ~~>\"requests>=2.28.0",
                    "click>=8.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void findDirectDependencyByExactName() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("requests", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "click>=8.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  ~~>\"requests>=2.28.0",
                  "click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void findDependencyWithGlobPattern() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("req*", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "click>=8.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  ~~>\"requests>=2.28.0",
                  "click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void noMatchReturnsUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("nonexistent", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """
          )
        );
    }

    @Test
    void findInBuildRequires() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("hatchling", "buildRequires", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              
              [build-system]
              requires = ["hatchling"]
              build-backend = "hatchling.build"
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              
              [build-system]
              requires = [~~>\"hatchling"]
              build-backend = "hatchling.build"
              """
          )
        );
    }

    @Test
    void findInDependencyGroups() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("pytest*", "dependencyGroups", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              
              [dependency-groups]
              dev = ["pytest>=7.0", "mypy>=1.0"]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              
              [dependency-groups]
              dev = [~~>\"pytest>=7.0", "mypy>=1.0"]
              """
          )
        );
    }

    @Test
    void scopeFilterExcludesOtherScopes() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("requests", "buildRequires", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              
              [build-system]
              requires = ["hatchling"]
              build-backend = "hatchling.build"
              """
          )
        );
    }

    @Test
    void findMultipleMatchingDependencies() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("*", "dependencies", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "click>=8.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  ~~>\"requests>=2.28.0",
                  ~~>\"click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void normalizesDashesAndUnderscores() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("typing_extensions", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "typing-extensions>=4.0.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  ~~>\"typing-extensions>=4.0.0",
              ]
              """
          )
        );
    }

    @Test
    void noMatchInRequirementsTxtReturnsUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("nonexistent", null, null)),
          requirementsTxt(
            """
              requests>=2.28.0
              """
          )
        );
    }

    @Test
    void onlyDirectSkipsTransitive() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("certifi", null, true)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """
          )
        );
    }

    @Test
    void noMatchInSetupCfgReturnsUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("nonexistent", null, null)),
          setupCfg(
            """
              [metadata]
              name = myapp
              version = 1.0.0

              [options]
              install_requires =
                  requests>=2.28.0
              """
          )
        );
    }
}
