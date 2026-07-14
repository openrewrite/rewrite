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
import org.openrewrite.python.internal.LockFileRegeneration;
import org.openrewrite.python.table.PythonLockFileRegenerationResults;
import org.openrewrite.python.table.PythonLockFileRegenerationResults.Status;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.python.Assertions.pipfile;
import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.uvLock;

class PythonLockFileRegenerationResultsTest implements RewriteTest {

    @Test
    void noLockPresentEmitsRow() {
        // A manifest change with no lock file to regenerate: couldn't try.
        rewriteRun(
          spec -> spec
            .recipe(new AddDependency("flask", ">=2.0", null, null))
            .dataTable(PythonLockFileRegenerationResults.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                PythonLockFileRegenerationResults.Row row = rows.getFirst();
                assertThat(row.getSourcePath()).isEqualTo("Pipfile");
                assertThat(row.getLockFile()).isEqualTo("Pipfile.lock");
                assertThat(row.getPackageManager()).isEqualTo("pipenv");
                assertThat(row.getStatus()).isEqualTo(Status.NO_LOCK_PRESENT);
                assertThat(row.getDetail()).isNull();
            }),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              """,
            """
              [packages]
              requests = ">=2.28.0"
              flask = ">=2.0"
              """
          )
        );
    }

    @Test
    void toolNotInstalledEmitsRow() {
        // Regeneration is attempted (a lock is present) but pipenv is absent.
        assumeTrue(!LockFileRegeneration.PIPENV.isToolAvailable(),
                "This test requires pipenv to NOT be installed");
        rewriteRun(
          spec -> spec
            .recipe(new AddDependency("flask", ">=2.0", null, null))
            .dataTable(PythonLockFileRegenerationResults.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                PythonLockFileRegenerationResults.Row row = rows.getFirst();
                assertThat(row.getSourcePath()).isEqualTo("Pipfile");
                assertThat(row.getLockFile()).isEqualTo("Pipfile.lock");
                assertThat(row.getPackageManager()).isEqualTo("pipenv");
                assertThat(row.getStatus()).isEqualTo(Status.TOOL_NOT_INSTALLED);
                assertThat(row.getDetail()).contains("pipenv is not installed");
            }),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              """,
            // The manifest change lands; the Markup.warn about the failed regeneration
            // is appended by the recipe, so accept whatever is rendered.
            spec -> spec.after(actual -> assertThat(actual).contains("flask = \">=2.0\"").actual())
          ),
          json(
            """
              {
                  "_meta": {},
                  "default": {},
                  "develop": {}
              }
              """,
            // The lock cannot be regenerated, so the recipe attaches the same warning to it.
            spec -> spec.path("Pipfile.lock")
              .after(actual -> assertThat(actual).contains("lock regeneration failed: pipenv is not installed").actual())
          )
        );
    }

    @Test
    void regeneratedEmitsRow() {
        // uv is available: regenerating from the stub lock produces different content.
        assumeTrue(LockFileRegeneration.UV.isToolAvailable(),
                "This test requires uv to be installed");
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeDependencyVersion("certifi", ">=2021.0.0", null, null))
            .dataTable(PythonLockFileRegenerationResults.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                PythonLockFileRegenerationResults.Row row = rows.getFirst();
                assertThat(row.getSourcePath()).isEqualTo("pyproject.toml");
                assertThat(row.getLockFile()).isEqualTo("uv.lock");
                assertThat(row.getPackageManager()).isEqualTo("uv");
                assertThat(row.getStatus()).isEqualTo(Status.REGENERATED);
                assertThat(row.getDetail()).isNull();
            }),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              requires-python = ">=3.8"
              dependencies = [
                  "certifi>=2020.0.0",
              ]

              [tool.uv]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              requires-python = ">=3.8"
              dependencies = [
                  "certifi>=2021.0.0",
              ]

              [tool.uv]
              """
          ),
          uvLock(
            """
              version = 1
              requires-python = ">=3.8"
              """,
            // uv rewrites the lock with resolved packages; accept whatever it produces.
            spec -> spec.noTrim().after(actual -> assertThat(actual).contains("certifi").actual())
          )
        );
    }
}
