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
import org.openrewrite.DocumentExample;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.toml.Assertions.toml;

class ModuleHasDependencyTest implements RewriteTest {

    private static final String PYPROJECT = """
      [project]
      name = "app"
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasDependency("Django", "4.2.x", null));
    }

    @DocumentExample
    @Test
    void marksAllSourcesOfProjectWithDependency() {
        rewriteRun(
          toml(
            PYPROJECT,
            """
              ~~(Module has dependency: Django:4.2.x)~~>[project]
              name = "app"
              """,
            spec -> spec.path("services/web/pyproject.toml").markers(project(direct("django", ">=4.2,<5", resolved("django", "4.2.11"))))
          ),
          text(
            "import django",
            "~~(Module has dependency: Django:4.2.x)~~>import django",
            spec -> spec.path("services/web/app/views.py")
          ),
          toml(
            PYPROJECT,
            spec -> spec.path("services/api/pyproject.toml").markers(project(direct("django", ">=5", resolved("django", "5.0.4"))))
          ),
          text(
            "import django",
            spec -> spec.path("services/api/app/views.py")
          ),
          text(
            "print('hi')",
            spec -> spec.path("scripts/run.py")
          )
        );
    }

    @Test
    void requirementsTxtManifest() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("requests", null, null)),
          text(
            "requests==2.31.0",
            "~~(Module has dependency: requests)~~>requests==2.31.0",
            spec -> spec.path("requirements.txt").markers(project(direct("requests", "==2.31.0", null)))
          ),
          text(
            "import requests",
            "~~(Module has dependency: requests)~~>import requests",
            spec -> spec.path("src/main.py")
          )
        );
    }

    @Test
    void unresolvedExactPinMatchesVersion() {
        rewriteRun(
          text(
            "Django==4.2.11",
            "~~(Module has dependency: Django:4.2.x)~~>Django==4.2.11",
            spec -> spec.path("requirements.txt").markers(project(direct("Django", "==4.2.11", null)))
          )
        );
    }

    @Test
    void unresolvedRangeDoesNotMatchVersion() {
        rewriteRun(
          text(
            "Django>=4.2",
            spec -> spec.path("requirements.txt").markers(project(direct("Django", ">=4.2", null)))
          )
        );
    }

    @Test
    void namesAreNormalized() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("zope.interface", null, null)),
          text(
            "Zope-Interface",
            "~~(Module has dependency: zope.interface)~~>Zope-Interface",
            spec -> spec.path("requirements.txt").markers(project(direct("Zope-Interface", null, null)))
          )
        );
    }

    @Test
    void transitiveDependencyMatchesUnlessOnlyDirect() {
        ResolvedDependency sqlparse = resolved("sqlparse", "0.5.0");
        ResolvedDependency django = new ResolvedDependency("django", "4.2.11", null, singletonList(sqlparse));
        PythonResolutionResult marker = project(direct("django", ">=4.2", django));
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("sqlparse", null, null)),
          toml(
            PYPROJECT,
            """
              ~~(Module has dependency: sqlparse)~~>[project]
              name = "app"
              """,
            spec -> spec.path("pyproject.toml").markers(marker)
          )
        );
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("sqlparse", null, true)),
          toml(
            PYPROJECT,
            spec -> spec.path("pyproject.toml").markers(marker)
          )
        );
    }

    private static PythonResolutionResult project(Dependency... dependencies) {
        return new PythonResolutionResult(randomId(), "app", "1.0.0", null, null, "pyproject.toml", null, null,
          emptyList(), List.of(dependencies), emptyMap(), emptyMap(), emptyList(), emptyList(), emptyList(), null, null);
    }

    private static Dependency direct(String name, String constraint, ResolvedDependency resolved) {
        return new Dependency(name, constraint, null, null, resolved);
    }

    private static ResolvedDependency resolved(String name, String version) {
        return new ResolvedDependency(name, version, null, null);
    }
}
