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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.*;

class UpgradeDependencyVersionTest implements RewriteTest {

    @Test
    void upgradesDevPackagesDependencyByDefault() {
        // pytube shape: coverage declared only in [dev-packages]; the default (null) scope must find it
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("coverage", "==6.5.0", null, null)),
          pipfile(
            """
              [[source]]
              url = "https://pypi.org/simple"
              verify_ssl = true
              name = "pypi"

              [packages]

              [dev-packages]
              coverage = "*"
              flake8 = "*"
              """,
            """
              [[source]]
              url = "https://pypi.org/simple"
              verify_ssl = true
              name = "pypi"

              [packages]

              [dev-packages]
              coverage = "==6.5.0"
              flake8 = "*"
              """
          )
        );
    }

    @Test
    void invalidNewVersionIsRejected() {
        // a fat-fingered trailing quote must fail validation, not corrupt the manifest
        assertThat(new UpgradeDependencyVersion("six", "==1.17.0\"", null, null).validate().isValid()).isFalse();
        assertThat(new UpgradeDependencyVersion("six", "==1.17.0", null, null).validate().isValid()).isTrue();
        assertThat(new UpgradeDependencyVersion("six", "1.17.0", null, null).validate().isValid()).isTrue();
    }

    @Test
    @Timeout(120)
    void warnsOnBothManifestAndLockWhenRegenerationFails() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion(
            "nonexistent-openrewrite-lock-test-package", ">=2.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "nonexistent-openrewrite-lock-test-package>=1.0",
              ]

              [tool.uv]
              """,
            s -> s.after(actual -> {
                assertThat(actual).contains("nonexistent-openrewrite-lock-test-package>=2.0");
                return actual;
            }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                    .as("manifest should carry the lock-regeneration-failure warning")
                    .isPresent())
          ),
          uvLock(
            """
              version = 1
              requires-python = ">=3.9"
              """,
            s -> s.after(actual -> actual)
                    .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                            .as("lock file should carry the lock-regeneration-failure warning")
                            .isPresent())
          )
        );
    }

    @Test
    void changeVersionWithResolvedProject(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
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
                    "requests>=2.31.0",
                    "click>=8.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void changeVersionConstraint() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
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
                  "requests>=2.31.0",
                  "click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionWithExtras() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests[security]>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests[security]>=2.31.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionWithMarker() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0; python_version>='3.8'",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.31.0; python_version>='3.8'",
              ]
              """
          )
        );
    }

    @Test
    void bareVersionNormalized() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", "2.31.0", null, null)),
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
                  "requests>=2.31.0",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenNotPresent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("nonexistent", ">=1.0", null, null)),
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
    void normalizeNameForMatching() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("typing_extensions", ">=4.8.0", null, null)),
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
                  "typing-extensions>=4.8.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionInInlineList() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0", "click>=8.0"]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.31.0", "click>=8.0"]
              """
          )
        );
    }

    @Test
    void changeVersionInOptionalDependencies() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("pytest", ">=8.0", "project.optional-dependencies", "dev")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [project.optional-dependencies]
              dev = [
                  "pytest>=7.0",
                  "mypy>=1.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [project.optional-dependencies]
              dev = [
                  "pytest>=8.0",
                  "mypy>=1.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionInDependencyGroup() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("pytest", ">=8.0", "dependency-groups", "test")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest>=7.0",
                  "coverage>=7.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest>=8.0",
                  "coverage>=7.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionInRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
          requirementsTxt(
            "requests>=2.28.0\nclick>=8.0",
            "requests>=2.31.0\nclick>=8.0"
          )
        );
    }

    @Test
    void skipWhenNotPresentInRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("flask", ">=3.0", null, null)),
          requirementsTxt("requests>=2.28.0")
        );
    }

    @Test
    void changeVersionInPipfile() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.31.0", null, null)),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              click = ">=8.0"
              """,
            """
              [packages]
              requests = ">=2.31.0"
              click = ">=8.0"
              """
          )
        );
    }

    @Test
    void upgradeQuotedKeyInPipfile() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("urllib3", ">=2.0", null, null)),
          pipfile(
            """
              [packages]
              "urllib3" = ">=1.26"
              """,
            """
              [packages]
              "urllib3" = ">=2.0"
              """
          )
        );
    }

    @Test
    void noChangeWhenVersionAlreadyTargetDespiteSpacing() {
        // Whitespace-only difference must not produce an edit.
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("six", "==1.17.0", null, null)),
          pyproject(
            """
              [project]
              name = "calibre"
              dependencies = [
                  "six == 1.17.0",
              ]
              """
          )
        );
    }

    @Test
    void spacePaddedRequirementRewritesCleanly() {
        // kovidgoyal/calibre shape: "six == 1.17.0" (spaces around ==) — only the
        // version token changes; the original spacing is preserved.
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("six", "==1.17.1", null, null)),
          pyproject(
            """
              [project]
              name = "calibre"
              dependencies = [
                  "six == 1.17.0",
                  "lxml == 6.1.1",
              ]
              """,
            """
              [project]
              name = "calibre"
              dependencies = [
                  "six == 1.17.1",
                  "lxml == 6.1.1",
              ]
              """
          )
        );
    }

    @Test
    void starConstraintPipfileRewritesCleanly() {
        // postmanlabs/httpbin shape: six = "*" alongside git inline-table entries
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("six", "==1.17.0", null, null)),
          pipfile(
            """
              [[source]]
              url = "https://pypi.python.org/simple"
              verify_ssl = true

              [packages]
              Flask = "*"
              six = "*"
              pyyaml = {git = "https://github.com/yaml/pyyaml.git"}

              [dev-packages]
              rope = "*"
              """,
            """
              [[source]]
              url = "https://pypi.python.org/simple"
              verify_ssl = true

              [packages]
              Flask = "*"
              six = "==1.17.0"
              pyyaml = {git = "https://github.com/yaml/pyyaml.git"}

              [dev-packages]
              rope = "*"
              """
          )
        );
    }
}
