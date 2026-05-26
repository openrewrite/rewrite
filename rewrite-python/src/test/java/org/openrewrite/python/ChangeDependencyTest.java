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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.*;

class ChangeDependencyTest implements RewriteTest {

    @Test
    void renamePackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", null)),
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
                  "httpx>=2.28.0",
                  "click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void renameWithNewVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", ">=0.24.0")),
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
                  "httpx>=0.24.0",
              ]
              """
          )
        );
    }

    @Test
    void renamePreservesExtrasAndMarkers() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests[security]>=2.28.0; python_version>='3.8'",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "httpx[security]>=2.28.0; python_version>='3.8'",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenNotFound() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("nonexistent", "replacement", null)),
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
    void bareVersionNormalized() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", "0.24.0")),
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
                  "httpx>=0.24.0",
              ]
              """
          )
        );
    }

    @Test
    void renameAcrossScopes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("pytest", "pytest-ng", null)),
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
                  "pytest-ng>=7.0",
              ]
              """
          )
        );
    }

    @Test
    void renamePackageInRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", null)),
          requirementsTxt(
            "requests>=2.28.0\nclick>=8.0",
            "httpx>=2.28.0\nclick>=8.0"
          )
        );
    }

    @Test
    void renameWithNewVersionInRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", ">=0.24.0")),
          requirementsTxt(
            "requests>=2.28.0\nclick>=8.0",
            "httpx>=0.24.0\nclick>=8.0"
          )
        );
    }

    @Test
    void skipWhenNotFoundInRequirementsTxt() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("flask", "quart", null)),
          requirementsTxt("requests>=2.28.0")
        );
    }

    @Test
    void renamePackageInPipfile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", ">=0.24.0")),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              click = ">=8.0"
              """,
            """
              [packages]
              httpx = ">=0.24.0"
              click = ">=8.0"
              """
          )
        );
    }

    @Test
    void renameQuotedKeyInPipfile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("urllib3", "httpx", ">=0.24.0")),
          pipfile(
            """
              [packages]
              "urllib3" = "*"
              """,
            """
              [packages]
              httpx = ">=0.24.0"
              """
          )
        );
    }

    @Test
    void chainAddThenUpgradeAcrossRecipes() {
        // The second recipe must see what the first recipe added, in the same cycle.
        // Without that within-cycle propagation, UpgradeDependencyVersion would not
        // see httpx (added by AddDependency) and the chain would not converge.
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: com.example.Chain
              displayName: chain
              description: Cross-recipe state-carryover smoke test.
              recipeList:
                - org.openrewrite.python.AddDependency:
                    packageName: httpx
                    version: ">=0.27"
                - org.openrewrite.python.UpgradeDependencyVersion:
                    packageName: httpx
                    newVersion: ">=0.28"
              """,
            "com.example.Chain"
          ),
          pipfile(
            """
              [packages]
              requests = "*"
              """,
            """
              [packages]
              requests = "*"
              httpx = ">=0.28"
              """
          )
        );
    }
}
