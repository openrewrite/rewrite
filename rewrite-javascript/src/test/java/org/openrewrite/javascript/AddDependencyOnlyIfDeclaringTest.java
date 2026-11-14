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
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

class AddDependencyOnlyIfDeclaringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        HttpSender http = request -> new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
        });
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
          new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
        spec.executionContext(ctx);
    }

    @Test
    void addsOnlyToAManifestDeclaringAMatch() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("vitest", "^4.1.12", "devDependencies",
            List.of("jest", "react-scripts", "@vue/cli-plugin-unit-*"))),
          packageJson(
            """
              {
                "name": "app",
                "version": "1.0.0",
                "scripts": {
                  "test": "react-scripts test"
                },
                "dependencies": {
                  "react-scripts": "5.0.1"
                }
              }
              """,
            """
              {
                "name": "app",
                "version": "1.0.0",
                "scripts": {
                  "test": "react-scripts test"
                },
                "dependencies": {
                  "react-scripts": "5.0.1"
                },
                "devDependencies": {
                  "vitest": "^4.1.12"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("react-scripts", "5.0.1")),
            s -> s.path("app/package.json")
          ),
          packageJson(
            """
              {
                "name": "web",
                "version": "1.0.0",
                "devDependencies": {
                  "@vue/cli-plugin-unit-jest": "^5.0.9"
                }
              }
              """,
            """
              {
                "name": "web",
                "version": "1.0.0",
                "devDependencies": {
                  "@vue/cli-plugin-unit-jest": "^5.0.9",
                  "vitest": "^4.1.12"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("@vue/cli-plugin-unit-jest", "^5.0.9")),
            s -> s.path("web/package.json")
          ),
          packageJson(
            """
              {
                "name": "docs",
                "version": "1.0.0",
                "devDependencies": {
                  "vitepress": "^1.0.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("vitepress", "^1.0.0")),
            s -> s.path("docs/package.json")
          )
        );
    }

    @Test
    void readsDeclarationsBeforeEarlierRecipesEditThem() {
        rewriteRun(
          spec -> spec.recipes(
            new RemoveDependency("jest", null),
            new AddDependency("vitest", "^4.1.12", "devDependencies", List.of("jest"))),
          packageJson(
            """
              {
                "name": "app",
                "version": "1.0.0",
                "devDependencies": {
                  "jest": "^29.7.0"
                }
              }
              """,
            """
              {
                "name": "app",
                "version": "1.0.0",
                "devDependencies": {
                  "vitest": "^4.1.12"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("jest", "^29.7.0"))
          )
        );
    }
}
