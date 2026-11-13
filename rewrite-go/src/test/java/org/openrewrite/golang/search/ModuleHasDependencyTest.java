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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.golang.marker.GoProject;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.marker.GoResolutionResult.ModuleRef;
import org.openrewrite.golang.marker.GoResolutionResult.Require;
import org.openrewrite.golang.marker.GoResolutionResult.ResolvedDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;

class ModuleHasDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasDependency("github.com/gin-gonic/*", "1.9.x", null));
    }

    @DocumentExample
    @Test
    void marksAllSourcesOfModuleWithDependency() {
        GoProject web = project("example.com/web");
        GoProject api = project("example.com/api");
        rewriteRun(
          text(
            "module example.com/web",
            "~~(Module has dependency: github.com/gin-gonic/*@1.9.x)~~>module example.com/web",
            spec -> spec.path("web/go.mod").markers(web, resolution(emptyList(), new Require("github.com/gin-gonic/gin", "v1.9.1", false)))
          ),
          text(
            "package main",
            "~~(Module has dependency: github.com/gin-gonic/*@1.9.x)~~>package main",
            spec -> spec.path("web/main.go").markers(web)
          ),
          text(
            "module example.com/api",
            spec -> spec.path("api/go.mod").markers(api, resolution(emptyList(), new Require("github.com/gin-gonic/gin", "v1.10.0", false)))
          ),
          text(
            "package main",
            spec -> spec.path("api/main.go").markers(api)
          )
        );
    }

    @Test
    void transitiveDependencyMatchesUnlessOnlyDirect() {
        GoProject web = project("example.com/web");
        ResolvedDependency gin = new ResolvedDependency("github.com/gin-gonic/gin", "v1.9.1",
          null, null, false, false, null, null, null,
          singletonList(new ModuleRef("github.com/go-playground/validator/v10", "v10.14.0")));
        ResolvedDependency validator = new ResolvedDependency("github.com/go-playground/validator/v10", "v10.14.0",
          null, null, true, false, null, null, null, null);
        GoResolutionResult resolution = resolution(List.of(gin, validator), new Require("github.com/gin-gonic/gin", "v1.9.1", false));
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("github.com/go-playground/validator/*", "10.x", null)),
          text(
            "module example.com/web",
            "~~(Module has dependency: github.com/go-playground/validator/*@10.x)~~>module example.com/web",
            spec -> spec.path("go.mod").markers(web, resolution)
          ),
          text(
            "package main",
            "~~(Module has dependency: github.com/go-playground/validator/*@10.x)~~>package main",
            spec -> spec.path("main.go").markers(web)
          )
        );
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("github.com/go-playground/validator/*", "10.x", true)),
          text(
            "module example.com/web",
            spec -> spec.path("go.mod").markers(web, resolution)
          )
        );
    }

    @Test
    void incompatibleVersionSuffixIsIgnored() {
        GoProject web = project("example.com/web");
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("github.com/docker/docker", "20.x", null)),
          text(
            "module example.com/web",
            "~~(Module has dependency: github.com/docker/docker@20.x)~~>module example.com/web",
            spec -> spec.path("go.mod").markers(web, resolution(emptyList(),
              new Require("github.com/docker/docker", "v20.10.24+incompatible", false)))
          )
        );
    }

    @Test
    void sourcesWithoutProjectAreUntouched() {
        rewriteRun(
          text(
            "module example.com/web",
            spec -> spec.path("go.mod").markers(resolution(emptyList(), new Require("github.com/gin-gonic/gin", "v1.9.1", false)))
          ),
          text(
            "package main",
            spec -> spec.path("main.go")
          )
        );
    }

    private static GoProject project(String modulePath) {
        return new GoProject(randomId(), modulePath, modulePath);
    }

    private static GoResolutionResult resolution(List<ResolvedDependency> resolved, Require... requires) {
        return new GoResolutionResult(randomId(), "example.com/web", "1.22", null, "go.mod",
          List.of(requires), emptyList(), emptyList(), emptyList(), resolved, emptyList(),
          GoResolutionResult.ResolutionStatus.RESOLVED, null, null);
    }
}
