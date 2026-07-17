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
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markup;
import org.openrewrite.python.internal.LockFileRegeneration;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
    void markerRefreshedAndFailureSurfacedWhenAdditionNeedsResolution() {
        // The native uv engine cannot add a package without delta resolution; the edit
        // still lands, the marker reflects it, and the failure is surfaced on both files.
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              requires-python = ">=3.12"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.uv]
              """,
            s -> s.after(actual -> {
                assertThat(actual).contains("flask>=2.0");
                return actual;
            }).afterRecipe(doc -> {
                PythonResolutionResult marker = doc.getMarkers()
                        .findFirst(PythonResolutionResult.class).orElseThrow();
                assertThat(marker.getDependencies())
                        .extracting(d -> PythonResolutionResult.normalizeName(d.getName()))
                        .as("refreshed marker should declare the added dependency")
                        .contains("flask");
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                        .as("manifest should carry the lock-regeneration-failure warning")
                        .isPresent();
            })
          ),
          uvLock(
            """
              version = 1
              revision = 3
              requires-python = ">=3.12"

              [[package]]
              name = "myapp"
              version = "1.0.0"
              source = { virtual = "." }
              dependencies = [
                  { name = "requests" },
              ]

              [package.metadata]
              requires-dist = [{ name = "requests", specifier = ">=2.28.0" }]

              [[package]]
              name = "requests"
              version = "2.32.4"
              source = { registry = "https://pypi.org/simple" }
              sdist = { url = "https://files.pythonhosted.org/packages/aa/requests-2.32.4.tar.gz", hash = "sha256:aaaa", size = 1, upload-time = "2024-01-01T00:00:00Z" }
              wheels = [
                  { url = "https://files.pythonhosted.org/packages/aa/requests-2.32.4-py3-none-any.whl", hash = "sha256:bbbb", size = 1, upload-time = "2024-01-01T00:00:00Z" },
              ]
              """,
            s -> s.after(actual -> actual)
                    .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                            .as("lock file should carry the lock-regeneration-failure warning")
                            .isPresent())
          )
        );
    }

    @Test
    void uvLockRegenerationWorks() {
        // Native minimal update of a recorded uv lock: bump the six pin from the fixtures.
        Map<String, String> routes = Map.of(
          "https://pypi.org/simple/six/", uvResource("http/six-listing-json"),
          "https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          uvResource("http/six-1.17.0-py2.py3-none-any.whl.metadata"));
        HttpSender http = request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
              new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));

        LockFileRegeneration.Result result =
                LockFileRegeneration.UV.regenerate(
                  uvResource("i-minimal-update/pyproject.toml"),
                  uvResource("i-minimal-update/uv.lock.v1"),
                  ctx);
        assertThat(result.isSuccess()).as("native uv regeneration should succeed: " + result.getErrorMessage()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(uvResource("i-minimal-update/uv.lock.v2"));
    }

    private static String uvResource(String name) {
        try (InputStream is = AddDependencyTest.class.getResourceAsStream("/uvlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
