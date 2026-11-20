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
import org.openrewrite.DocumentExample;
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
import static org.openrewrite.test.SourceSpecs.text;

class RemoveDependencyUnlessImportingTest implements RewriteTest {

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

    @DocumentExample
    @Test
    void keepsTheDependencyOfAProjectThatStillImportsIt() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("ng2-cookies", null, List.of("ng2-cookies"))),
          packageJson(
            """
              {
                "name": "admin",
                "dependencies": {
                  "@angular/core": "^17.3.0",
                  "ng2-cookies": "^1.0.12"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("@angular/core", "^17.3.0"),
              dependency("ng2-cookies", "^1.0.12")),
            s -> s.path("admin/package.json")
          ),
          text(
            """
              import { Cookie } from 'ng2-cookies/ng2-cookies';

              export function readLocale(): string {
                  return Cookie.get('locale');
              }
              """,
            s -> s.path("admin/src/app/locale.ts")
          ),
          packageJson(
            """
              {
                "name": "shop",
                "dependencies": {
                  "@angular/core": "^17.3.0",
                  "ng2-cookies": "^1.0.12"
                }
              }
              """,
            """
              {
                "name": "shop",
                "dependencies": {
                  "@angular/core": "^17.3.0"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("@angular/core", "^17.3.0"),
              dependency("ng2-cookies", "^1.0.12")),
            s -> s.path("shop/package.json")
          ),
          text(
            """
              import { CookieService } from 'ngx-cookie-service';
              export const service = CookieService;
              """,
            s -> s.path("shop/src/app/session.ts")
          )
        );
    }

    @Test
    void keepsTheManifestNodeResolvesTheImportFrom() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("lodash", null, List.of("lodash"))),
          packageJson(
            """
              {
                "name": "root",
                "workspaces": ["packages/*"],
                "dependencies": {
                  "lodash": "^4.17.21"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("lodash", "^4.17.21"))
          ),
          packageJson(
            """
              {
                "name": "web",
                "dependencies": {
                  "react": "^18.2.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^18.2.0")),
            s -> s.path("packages/web/package.json")
          ),
          text(
            "const fp = require('lodash/fp');\n",
            s -> s.path("packages/web/src/index.js")
          )
        );
    }

    @Test
    void readsAnImportedTypePackageByGlob() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("@types/lodash", null, List.of("lodash", "lodash.*"))),
          packageJson(
            """
              {
                "name": "app",
                "devDependencies": {
                  "@types/lodash": "^4.17.0",
                  "@types/node": "^22.0.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("@types/lodash", "^4.17.0"),
              dependency("@types/node", "^22.0.0"))
          ),
          text(
            "export const debounce = () => import(\"lodash.debounce\");\n",
            s -> s.path("src/lazy.ts")
          )
        );
    }

    @Test
    void ignoresSourcesThatAreNotCodeAndRelativeImports() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("lodash", null, List.of("lodash"))),
          packageJson(
            """
              {
                "name": "app",
                "dependencies": {
                  "lodash": "^4.17.21"
                }
              }
              """,
            """
              {
                "name": "app"
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("lodash", "^4.17.21"))
          ),
          text(
            "import { chunk } from 'lodash';\n",
            s -> s.path("README.md")
          ),
          text(
            "import { chunk } from './lodash';\n",
            s -> s.path("src/index.ts")
          )
        );
    }

    @Test
    void removesTheDependencyOnceAnEarlierRecipeRemovesTheLastImport() {
        rewriteRun(
          spec -> spec
            .recipeFromYaml(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.MigrateNg2Cookies
                displayName: Migrate ng2-cookies
                description: Rewrites the imports, then removes the dependency in the cycle after.
                causesAnotherCycle: true
                recipeList:
                  - org.openrewrite.text.FindAndReplace:
                      find: "'ng2-cookies'"
                      replace: "'ngx-cookie-service'"
                  - org.openrewrite.javascript.RemoveDependency:
                      packageName: ng2-cookies
                      unlessImporting:
                        - ng2-cookies
                """,
              "test.MigrateNg2Cookies")
            .cycles(3)
            .expectedCyclesThatMakeChanges(2),
          packageJson(
            """
              {
                "name": "app",
                "dependencies": {
                  "ng2-cookies": "^1.0.12",
                  "ngx-cookie-service": "^17.1.0"
                }
              }
              """,
            """
              {
                "name": "app",
                "dependencies": {
                  "ngx-cookie-service": "^17.1.0"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("ng2-cookies", "^1.0.12"),
              dependency("ngx-cookie-service", "^17.1.0"))
          ),
          text(
            "import { CookieService } from 'ng2-cookies';\n",
            "import { CookieService } from 'ngx-cookie-service';\n",
            s -> s.path("src/app/prefs.ts")
          )
        );
    }
}
