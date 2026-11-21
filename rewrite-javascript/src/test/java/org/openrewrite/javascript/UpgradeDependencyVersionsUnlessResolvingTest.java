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
package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

class UpgradeDependencyVersionsUnlessResolvingTest implements RewriteTest {

    private static final Map<String, String> GRID_36 = Map.of("ag-grid-*", "36.x");

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeDependencyVersions(GRID_36, null, List.of("ag-grid-*@<23", "@ag-grid-community/*@*")));
    }

    @Test
    void leavesAManifestWhoseDeclaredRangeAdmitsAVersionInTheRange() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "too-old",
                "dependencies": {
                  "ag-grid-angular": "20.0.0",
                  "ag-grid-community": "^25.2.1"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("ag-grid-angular", "20.0.0"),
              dependency("ag-grid-community", "^25.2.1")),
            s -> s.path("too-old/package.json")
          ),
          packageJson(
            """
              {
                "name": "supported",
                "dependencies": {
                  "ag-grid-angular": "~23.2.1",
                  "ag-grid-community": "^25.2.1"
                }
              }
              """,
            """
              {
                "name": "supported",
                "dependencies": {
                  "ag-grid-angular": "36.x",
                  "ag-grid-community": "36.x"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("ag-grid-angular", "~23.2.1"),
              dependency("ag-grid-community", "^25.2.1")),
            s -> s.path("supported/package.json")
          )
        );
    }

    @Test
    void leavesAManifestDeclaringAPackageTheGlobMatchesAtAnyVersion() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "modules",
                "dependencies": {
                  "ag-grid-community": "^25.2.1"
                },
                "devDependencies": {
                  "@ag-grid-community/core": "^25.2.1"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("ag-grid-community", "^25.2.1")),
            s -> s.path("modules/package.json")
          )
        );
    }

    @Test
    void theVersionTheLockResolvesDecidesOverTheDeclaredRange() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "resolved-old",
                "dependencies": {
                  "ag-grid-community": ">=25.0.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, resolved("ag-grid-community", ">=25.0.0", "22.1.1")),
            s -> s.path("resolved-old/package.json")
          ),
          packageJson(
            """
              {
                "name": "resolved-supported",
                "dependencies": {
                  "ag-grid-community": ">=20.0.0"
                }
              }
              """,
            """
              {
                "name": "resolved-supported",
                "dependencies": {
                  "ag-grid-community": "36.x"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, resolved("ag-grid-community", ">=20.0.0", "25.3.0")),
            s -> s.path("resolved-supported/package.json")
          )
        );
    }

    @Test
    void aDependencyThatIsNotAVersionHoldsNoEntry() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "tagged",
                "dependencies": {
                  "ag-grid-react": "file:../react",
                  "ag-grid-community": "^25.2.1"
                }
              }
              """,
            """
              {
                "name": "tagged",
                "dependencies": {
                  "ag-grid-react": "file:../react",
                  "ag-grid-community": "36.x"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("ag-grid-react", "file:../react"),
              dependency("ag-grid-community", "^25.2.1"))
          )
        );
    }

    @Test
    void rejectsAnEntryThatIsNotANameAndARange() {
        assertThat(new UpgradeDependencyVersions(GRID_36, null, List.of("ag-grid-*@<23")).validate().isValid()).isTrue();
        assertThat(new UpgradeDependencyVersions(GRID_36, null, null).validate().isValid()).isTrue();
        for (String entry : List.of("ag-grid-community", "@ag-grid-community/core", "ag-grid-*@", "ag-grid-*@not a range")) {
            assertThat(new UpgradeDependencyVersions(GRID_36, null, List.of(entry)).validate().isInvalid()).as(entry).isTrue();
        }
    }

    private static Dependency resolved(String name, String versionConstraint, String version) {
        return new Dependency(name, versionConstraint, new ResolvedDependency(name, version, null, null, null, null, null, null));
    }
}
