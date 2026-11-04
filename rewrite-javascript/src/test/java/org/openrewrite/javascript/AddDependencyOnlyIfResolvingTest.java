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
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

class AddDependencyOnlyIfResolvingTest implements RewriteTest {

    private static final List<String> SUPPORTED_REACT = List.of("react@>=16.9.0 <18", "react-dom@>=16.9.0 <18");

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
        spec.executionContext(ctx)
          .recipe(new AddDependency("@testing-library/react", "^12.1.5", "devDependencies", null, SUPPORTED_REACT));
    }

    @Test
    void addsWhereTheDeclaredRangeStaysWithinEveryRange() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "react17",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                }
              }
              """,
            """
              {
                "name": "react17",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                },
                "devDependencies": {
                  "@testing-library/react": "^12.1.5"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^17.0.2"), dependency("react-dom", "^17.0.2")),
            s -> s.path("react17/package.json")
          ),
          packageJson(
            """
              {
                "name": "react18",
                "dependencies": {
                  "react": "^18.2.0",
                  "react-dom": "^18.2.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^18.2.0"), dependency("react-dom", "^18.2.0")),
            s -> s.path("react18/package.json")
          ),
          packageJson(
            """
              {
                "name": "dom-missing",
                "dependencies": {
                  "react": "^17.0.2"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^17.0.2")),
            s -> s.path("dom-missing/package.json")
          )
        );
    }

    @Test
    void doesNotAddWhereTheDeclaredRangeAdmitsVersionsOutsideTheRange() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "whole-major",
                "dependencies": {
                  "react": "^16",
                  "react-dom": "^16"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^16"), dependency("react-dom", "^16")),
            s -> s.path("whole-major/package.json")
          ),
          packageJson(
            """
              {
                "name": "unbounded",
                "dependencies": {
                  "react": ">=16.9.0",
                  "react-dom": ">=16.9.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", ">=16.9.0"), dependency("react-dom", ">=16.9.0")),
            s -> s.path("unbounded/package.json")
          ),
          packageJson(
            """
              {
                "name": "catalog",
                "dependencies": {
                  "react": "catalog:",
                  "react-dom": "catalog:"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "catalog:"), dependency("react-dom", "catalog:")),
            s -> s.path("catalog/package.json")
          )
        );
    }

    @Test
    void theVersionTheLockResolvesWinsOverTheDeclaredRange() {
        rewriteRun(
          packageJson(
            """
              {
                "name": "resolves-16-14",
                "dependencies": {
                  "react": ">=16.0.0 <18",
                  "react-dom": ">=16.0.0 <18"
                }
              }
              """,
            """
              {
                "name": "resolves-16-14",
                "dependencies": {
                  "react": ">=16.0.0 <18",
                  "react-dom": ">=16.0.0 <18"
                },
                "devDependencies": {
                  "@testing-library/react": "^12.1.5"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm,
              resolved("react", ">=16.0.0 <18", "16.14.0"), resolved("react-dom", ">=16.0.0 <18", "16.14.0")),
            s -> s.path("resolves-16-14/package.json")
          ),
          packageJson(
            """
              {
                "name": "resolves-16-8",
                "dependencies": {
                  "react": "^16.9.0",
                  "react-dom": "^16.9.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm,
              resolved("react", "^16.9.0", "16.8.6"), resolved("react-dom", "^16.9.0", "16.8.6")),
            s -> s.path("resolves-16-8/package.json")
          )
        );
    }

    @Test
    void anUndeclaredPackageIsTakenFromTheNearestEnclosingManifestDeclaringIt() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("@testing-library/react", "^12.1.5", "devDependencies",
            List.of("enzyme"), SUPPORTED_REACT)),
          packageJson(
            """
              {
                "name": "root",
                "private": true,
                "workspaces": ["packages/*"],
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, List.of("packages/app/package.json", "packages/legacy/package.json"),
              dependency("react", "^17.0.2"), dependency("react-dom", "^17.0.2"))
          ),
          packageJson(
            """
              {
                "name": "app",
                "devDependencies": {
                  "enzyme": "^3.11.0"
                }
              }
              """,
            """
              {
                "name": "app",
                "devDependencies": {
                  "@testing-library/react": "^12.1.5",
                  "enzyme": "^3.11.0"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("enzyme", "^3.11.0")),
            s -> s.path("packages/app/package.json")
          ),
          packageJson(
            """
              {
                "name": "legacy",
                "dependencies": {
                  "react": "^16.4.0",
                  "react-dom": "^16.4.0"
                },
                "devDependencies": {
                  "enzyme": "^3.11.0"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm,
              dependency("react", "^16.4.0"), dependency("react-dom", "^16.4.0"), dependency("enzyme", "^3.11.0")),
            s -> s.path("packages/legacy/package.json")
          )
        );
    }

    @Test
    void addsOnlyWhereOnlyIfDeclaringHoldsToo() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("@testing-library/react", "^12.1.5", "devDependencies",
            List.of("enzyme"), SUPPORTED_REACT)),
          packageJson(
            """
              {
                "name": "with-enzyme",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                },
                "devDependencies": {
                  "enzyme": "^3.11.0"
                }
              }
              """,
            """
              {
                "name": "with-enzyme",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                },
                "devDependencies": {
                  "@testing-library/react": "^12.1.5",
                  "enzyme": "^3.11.0"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm,
              dependency("react", "^17.0.2"), dependency("react-dom", "^17.0.2"), dependency("enzyme", "^3.11.0")),
            s -> s.path("with-enzyme/package.json")
          ),
          packageJson(
            """
              {
                "name": "without-enzyme",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                }
              }
              """,
            null,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^17.0.2"), dependency("react-dom", "^17.0.2")),
            s -> s.path("without-enzyme/package.json")
          )
        );
    }

    @Test
    void scopedNamesSplitAtTheLastAt() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("@testing-library/react", "^12.1.5", "devDependencies", null,
            List.of("@types/react@^17"))),
          packageJson(
            """
              {
                "name": "typed",
                "devDependencies": {
                  "@types/react": "^17.0.80"
                }
              }
              """,
            """
              {
                "name": "typed",
                "devDependencies": {
                  "@testing-library/react": "^12.1.5",
                  "@types/react": "^17.0.80"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("@types/react", "^17.0.80"))
          )
        );
    }

    @Test
    void rejectsAnEntryWithoutAValidRange() {
        // given
        AddDependency recipe = new AddDependency("@testing-library/react", "^12.1.5", "devDependencies", null,
          Arrays.asList("react@>=16.9.0 <18", "@types/react", "react@not-a-range", null));

        // when
        List<?> failures = recipe.validate().failures();

        // then
        assertThat(failures).hasSize(3);
    }

    @Test
    void entriesAreTrimmed() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("@testing-library/react", "^12.1.5", "devDependencies", null,
            List.of(" react@>=16.9.0 <18", " react-dom@>=16.9.0 <18 "))),
          packageJson(
            """
              {
                "name": "react17",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                }
              }
              """,
            """
              {
                "name": "react17",
                "dependencies": {
                  "react": "^17.0.2",
                  "react-dom": "^17.0.2"
                },
                "devDependencies": {
                  "@testing-library/react": "^12.1.5"
                }
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("react", "^17.0.2"), dependency("react-dom", "^17.0.2"))
          )
        );
    }

    private static Dependency resolved(String name, String versionConstraint, String version) {
        return new Dependency(name, versionConstraint, new ResolvedDependency(name, version, null, null, null, null, null, null));
    }
}
