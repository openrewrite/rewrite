/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.toml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.toml.Assertions.toml;

class DeleteTableTest implements RewriteTest {

    @DocumentExample
    @Test
    void deleteArrayOfTables() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "package.contributors"
          )),
          toml(
            """
              [package]
              name = "example-package"
              version = "1.0.0"
              authors = ["Alice Smith"]

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"

              [[package.contributors]]
              name = "Carol White"
              email = "carol@example.com"
              """,
            """
              [package]
              name = "example-package"
              version = "1.0.0"
              authors = ["Alice Smith"]
              """
          )
        );
    }

    @Test
    void deleteTopLevelTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "dependencies"
          )),
          toml(
            """
              [package]
              name = "my-project"
              version = "1.0.0"

              [dependencies]
              library-a = "^2.0.0"
              library-b = "^1.5.0"

              [dev-dependencies]
              test-tool = "^3.0.0"
              """,
            """
              [package]
              name = "my-project"
              version = "1.0.0"

              [dev-dependencies]
              test-tool = "^3.0.0"
              """
          )
        );
    }

    @Test
    void deleteNestedTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "tool.formatter.options"
          )),
          toml(
            """
              [tool.linter]
              name = "test-project"
              version = "0.1.0"

              [tool.formatter.options]
              max-line-length = 100
              indent-size = 4

              [tool.builder]
              target = "dist"
              """,
            """
              [tool.linter]
              name = "test-project"
              version = "0.1.0"

              [tool.builder]
              target = "dist"
              """
          )
        );
    }

    @Test
    void deleteMiddleTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "database"
          )),
          toml(
            """
              [server]
              host = "localhost"
              port = 8080

              [database]
              host = "db.example.com"
              port = 5432
              name = "mydb"

              [logging]
              level = "info"
              """,
            """
              [server]
              host = "localhost"
              port = 8080

              [logging]
              level = "info"
              """
          )
        );
    }

    @Test
    void deleteFirstTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "metadata"
          )),
          toml(
            """
              [metadata]
              created = "2025-01-01"

              [application]
              name = "myapp"
              version = "2.0.0"
              """,
            """
              [application]
              name = "myapp"
              version = "2.0.0"
              """
          )
        );
    }

    @Test
    void deleteLastTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "extras"
          )),
          toml(
            """
              [package]
              name = "sample"

              [dependencies]
              core-lib = "^1.0.0"

              [extras]
              optional = ["extra-feature"]
              """,
            """
              [package]
              name = "sample"

              [dependencies]
              core-lib = "^1.0.0"
              """
          )
        );
    }

    @Test
    void doNotDeleteUnmatchedTables() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTable(
            "tool.analyzer"
          )),
          toml(
            """
              [tool.linter]
              name = "project"

              [tool.formatter.options]
              max-width = 80

              [tool.builder]
              target = "output"
              """
          )
        );
    }
}
