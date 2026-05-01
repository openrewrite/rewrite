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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.*;

class AddDependencyTest implements RewriteTest {

    @Test
    void addDependencyWithResolvedProject(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                    "flask>=2.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void twoAddDependenciesInSequence(@TempDir Path tempDir) {
        String pyprojectBefore = """
          [project]
          name = "myapp"
          version = "1.0.0"
          dependencies = [
              "requests>=2.28.0",
          ]
          """;

        rewriteRun(
          spec -> spec.recipe(new CompositeRecipe(List.of(
            new AddDependency("flask", ">=2.0", null, null),
            new AddDependency("click", ">=8.0", null, null)
          ))).afterRecipe(run -> {
              // Verify both recipes applied: pyproject has both flask and click in the changeset
              List<String> pyprojectContents = run.getChangeset().getAllResults().stream()
                      .filter(r -> r.getAfter() != null && r.getAfter().getSourcePath().endsWith("pyproject.toml"))
                      .map(r -> r.getAfter().printAll())
                      .collect(java.util.stream.Collectors.toList());
              assertThat(pyprojectContents).isNotEmpty();
              String pyprojectContent = pyprojectContents.get(0);
              assertThat(pyprojectContent).contains("flask>=2.0");
              assertThat(pyprojectContent).contains("click>=8.0");
          }),
          uv(tempDir,
            pyproject(
              pyprojectBefore,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                    "flask>=2.0",
                    "click>=8.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void addDependencyToExistingList() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", null, null, null)),
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
                  "requests>=2.28.0",
                  "click>=8.0",
                  "flask",
              ]
              """
          )
        );
    }

    @Test
    void addDependencyWithVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "flask>=2.0",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenAlreadyPresent() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("requests", null, null, null)),
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
    void addToEmptyDependencyList() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = []
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["flask>=2.0"]
              """
          )
        );
    }

    @Test
    void addToInlineDependencyList() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", null, null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0", "flask"]
              """
          )
        );
    }

    @Test
    void addToOptionalDependencies() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("pytest-cov", null, "project.optional-dependencies", "dev")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [project.optional-dependencies]
              dev = [
                  "pytest>=7.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [project.optional-dependencies]
              dev = [
                  "pytest>=7.0",
                  "pytest-cov",
              ]
              """
          )
        );
    }

    @Test
    void addToDependencyGroup() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("pytest-cov", ">=4.0", "dependency-groups", "test")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest>=7.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest>=7.0",
                  "pytest-cov>=4.0",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenAlreadyInScope() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("pytest", null, "dependency-groups", "test")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest>=7.0",
              ]
              """
          )
        );
    }

    @Test
    void markerResolvedDependenciesUpdatedAfterEdit(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                    "flask>=2.0",
                ]
                """,
              s -> s.afterRecipe(doc -> {
                  PythonResolutionResult marker = doc.getMarkers()
                          .findFirst(PythonResolutionResult.class).orElseThrow();
                  assertThat(marker.getResolvedDependencies())
                          .extracting(d -> PythonResolutionResult.normalizeName(d.getName()))
                          .as("regenerated uv.lock should contain flask among resolved dependencies")
                          .contains("flask");
                  assertThat(marker.getDependencies())
                          .filteredOn(d -> "flask".equals(PythonResolutionResult.normalizeName(d.getName())))
                          .singleElement()
                          .satisfies(d -> assertThat(d.getResolved())
                                  .as("declared `flask` dep should be linked to its resolved entry")
                                  .isNotNull());
              })
            )
          )
        );
    }

    @Test
    void uvLockRegenerationWorks() {
        String pyprojectWithFlask = """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "flask>=2.0",
              ]
              """;
        org.openrewrite.python.internal.LockFileRegeneration.Result result =
                org.openrewrite.python.internal.LockFileRegeneration.UV.regenerate(pyprojectWithFlask);
        assertThat(result.isSuccess()).as("uv lock should succeed: " + result.getErrorMessage()).isTrue();
        assertThat(result.getLockFileContent()).contains("name = \"flask\"");
    }

    @Test
    void addDependencyWithBareVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", "2.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "flask>=2.0",
              ]
              """
          )
        );
    }

    @Test
    void addDependencyToRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          requirementsTxt(
            "requests>=2.28.0",
            "requests>=2.28.0\nflask>=2.0"
          )
        );
    }

    @Test
    void skipWhenAlreadyPresentInRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("requests", null, null, null)),
          requirementsTxt("requests>=2.28.0")
        );
    }

    @Test
    void addDependencyToPipfile() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              """,
            """
              [packages]
              requests = ">=2.28.0"
              flask = ">=2.0"
              """
          )
        );
    }

    @Test
    void skipWhenAlreadyPresentInPipfileAsQuotedKey() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("urllib3", ">=2.0", null, null)),
          pipfile(
            """
              [packages]
              requests = "*"
              "urllib3" = "*"
              """
          )
        );
    }

    @Test
    void validateRequiresGroupName() {
        var recipe = new AddDependency("pytest", null, "project.optional-dependencies", null);
        assertThat(recipe.validate().isValid()).isFalse();
    }
}
