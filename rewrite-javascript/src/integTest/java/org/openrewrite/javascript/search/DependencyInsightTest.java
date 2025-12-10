/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.DocumentExample;
import org.openrewrite.javascript.table.NodeDependenciesInUse;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.npm;
import static org.openrewrite.javascript.Assertions.packageJson;

class DependencyInsightTest implements RewriteTest {

    @DocumentExample
    @Test
    void findDirectDependency(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("lodash", null, null))
            .dataTable(NodeDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                NodeDependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getPackageName()).isEqualTo("lodash");
                assertThat(row.getScope()).isEqualTo("dependencies");
                assertThat(row.getDirect()).isTrue();
                assertThat(row.getCount()).isEqualTo(1);
                assertThat(row.getLicense()).isEqualTo("MIT");
            }),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """,
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "dependencies": {
                    /*~~>*/"lodash": "^4.17.21"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void findDevDependency(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("jest", "devDependencies", null))
            .dataTable(NodeDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                NodeDependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getPackageName()).isEqualTo("jest");
                assertThat(row.getScope()).isEqualTo("devDependencies");
                assertThat(row.getDirect()).isTrue();
                assertThat(row.getCount()).isEqualTo(1);
            }),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    "jest": "^29.7.0"
                  }
                }
                """,
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    /*~~>*/"jest": "^29.7.0"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void findDependencyWithGlobPattern(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("@types/*", null, true))
            .dataTable(NodeDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(2);
                assertThat(rows).anyMatch(row -> "@types/node".equals(row.getPackageName()));
                assertThat(rows).anyMatch(row -> "@types/jest".equals(row.getPackageName()));
            }),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    "@types/node": "^20.0.0",
                    "@types/jest": "^29.5.0"
                  }
                }
                """,
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    /*~~>*/"@types/node": "^20.0.0",
                    /*~~>*/"@types/jest": "^29.5.0"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void scopeFiltersResults(@TempDir Path tempDir) {
        // When searching only in devDependencies, should not find lodash (which is in dependencies)
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("lodash", "devDependencies", null)),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  },
                  "devDependencies": {
                    "jest": "^29.7.0"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void findTransitiveDependency(@TempDir Path tempDir) {
        // chalk is a transitive dependency of jest
        // The marker should be on "jest" since it's the direct dependency that brings in chalk
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("chalk", null, null))
            .dataTable(NodeDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSizeGreaterThanOrEqualTo(1);
                assertThat(rows).anyMatch(row ->
                  "chalk".equals(row.getPackageName()) && !row.getDirect()
                );
            }),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    "jest": "^29.7.0"
                  }
                }
                """,
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    /*~~>*/"jest": "^29.7.0"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void onlyDirectExcludesTransitiveDependencies(@TempDir Path tempDir) {
        // With onlyDirect=true, should not find chalk (transitive dependency of jest)
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("chalk", null, true)),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "devDependencies": {
                    "jest": "^29.7.0"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void noMatchReturnsNoSearchResult(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("nonexistent-package-xyz", null, null)),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void wildcardMatchesAllDirectDependencies(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("*", "dependencies", true))
            .dataTable(NodeDependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(2);
                assertThat(rows).anyMatch(row -> "lodash".equals(row.getPackageName()));
                assertThat(rows).anyMatch(row -> "is-odd".equals(row.getPackageName()));
            }),
          npm(tempDir,
            packageJson(
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "dependencies": {
                    "lodash": "^4.17.21",
                    "is-odd": "^3.0.1"
                  }
                }
                """,
              """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "dependencies": {
                    /*~~>*/"lodash": "^4.17.21",
                    /*~~>*/"is-odd": "^3.0.1"
                  }
                }
                """
            )
          )
        );
    }
}
