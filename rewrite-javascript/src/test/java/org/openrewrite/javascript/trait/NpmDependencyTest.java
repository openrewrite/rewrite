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
package org.openrewrite.javascript.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.json.Assertions.json;

class NpmDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new NpmDependency.Matcher().asVisitor((dep, ctx) ->
          SearchResult.found(dep.getTree(), dep.getName() + ":" + dep.getVersion()))));
    }

    private static NodeResolutionResult marker(List<NodeResolutionResult.Dependency> dependencies,
                                               List<NodeResolutionResult.Dependency> devDependencies,
                                               List<NodeResolutionResult.ResolvedDependency> resolved) {
        return new NodeResolutionResult(Tree.randomId(), "demo", "1.0.0", null, "package.json", null,
          dependencies, devDependencies, emptyList(), emptyList(), emptyList(),
          resolved, NodeResolutionResult.PackageManager.Npm, null, null);
    }

    @Test
    void matchesDeclarationsAcrossSectionsWithResolvedVersions() {
        NodeResolutionResult.ResolvedDependency angularCore = new NodeResolutionResult.ResolvedDependency(
          "@angular/core", "15.2.10", null, null, null, null, null, null);
        NodeResolutionResult.ResolvedDependency express = new NodeResolutionResult.ResolvedDependency(
          "express", "4.19.2", null, null, null, null, null, null);
        rewriteRun(
          //language=json
          json(
            """
              {
                "dependencies": {
                  "@angular/core": "^15.2.0"
                },
                "devDependencies": {
                  "express": "^4.18.0"
                }
              }
              """,
            """
              {
                "dependencies": {
                  /*~~(@angular/core:15.2.10)~~>*/"@angular/core": "^15.2.0"
                },
                "devDependencies": {
                  /*~~(express:4.19.2)~~>*/"express": "^4.18.0"
                }
              }
              """,
            spec -> spec.path("package.json").markers(marker(
              List.of(new NodeResolutionResult.Dependency("@angular/core", "^15.2.0", angularCore)),
              List.of(new NodeResolutionResult.Dependency("express", "^4.18.0", express)),
              List.of(angularCore, express)))
          )
        );
    }

    @Test
    void fallsBackToConstraintFloorWithoutResolution() {
        rewriteRun(
          //language=json
          json(
            """
              {
                "dependencies": {
                  "express": "^3.21.0"
                }
              }
              """,
            """
              {
                "dependencies": {
                  /*~~(express:3.21.0)~~>*/"express": "^3.21.0"
                }
              }
              """,
            spec -> spec.path("package.json").markers(marker(
              List.of(new NodeResolutionResult.Dependency("express", "^3.21.0", null)),
              emptyList(), emptyList()))
          )
        );
    }

    @Test
    void noVersionFloorForProtocolAndOpenRangeConstraints() {
        rewriteRun(
          //language=json
          json(
            """
              {
                "dependencies": {
                  "bootstrap": "file:../vendor/bootstrap",
                  "react": ">=17 <19"
                }
              }
              """,
            """
              {
                "dependencies": {
                  /*~~(bootstrap:null)~~>*/"bootstrap": "file:../vendor/bootstrap",
                  /*~~(react:null)~~>*/"react": ">=17 <19"
                }
              }
              """,
            spec -> spec.path("package.json").markers(marker(
              List.of(new NodeResolutionResult.Dependency("bootstrap", "file:../vendor/bootstrap", null),
                new NodeResolutionResult.Dependency("react", ">=17 <19", null)),
              emptyList(), emptyList()))
          )
        );
    }

    @Test
    void doesNotMatchSameNamedSectionsNestedInOverrides() {
        NodeResolutionResult.ResolvedDependency express = new NodeResolutionResult.ResolvedDependency(
          "express", "3.21.2", null, null, null, null, null, null);
        rewriteRun(
          //language=json
          json(
            """
              {
                "overrides": {
                  "some-lib": {
                    "dependencies": {
                      "express": "^3.21.0"
                    }
                  }
                }
              }
              """,
            spec -> spec.path("package.json").markers(marker(
              List.of(new NodeResolutionResult.Dependency("express", "^3.21.0", express)),
              emptyList(), List.of(express)))
          )
        );
    }

    @Test
    void doesNotMatchWithoutResolutionMarker() {
        rewriteRun(
          //language=json
          json(
            """
              {
                "dependencies": {
                  "express": "^3.21.0"
                }
              }
              """,
            spec -> spec.path("package.json")
          )
        );
    }
}
