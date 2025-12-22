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

class ChangeKeyTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeSimpleKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "name",
            "project-name"
          )),
          toml(
            """
              name = "my-project"
              version = "1.0.0"
              """,
            """
              project-name = "my-project"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void changeKeyInTable() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "package.name",
            "project-name"
          )),
          toml(
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              """,
            """
              [package]
              project-name = "my-package"
              version = "0.1.0"
              """
          )
        );
    }

    @Test
    void changeKeyWithSpecialCharacters() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "package.name",
            "project name"
          )),
          toml(
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              """,
            """
              [package]
              "project name" = "my-package"
              version = "0.1.0"
              """
          )
        );
    }

    @Test
    void doNotChangeUnmatchedKeys() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "package.name",
            "project-name"
          )),
          toml(
            """
              [package]
              name = "my-package"

              [dependencies]
              name = "should-not-change"
              """,
            """
              [package]
              project-name = "my-package"

              [dependencies]
              name = "should-not-change"
              """
          )
        );
    }

    @Test
    void changeKeyInNestedTable() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "tool.poetry.name",
            "project"
          )),
          toml(
            """
              [tool.poetry]
              name = "my-poetry-project"
              version = "0.1.0"
              """,
            """
              [tool.poetry]
              project = "my-poetry-project"
              version = "0.1.0"
              """
          )
        );
    }

    @Test
    void preserveComments() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "package.name",
            "project-name"
          )),
          toml(
            """
              [package]
              # The name of the package
              name = "my-package"
              version = "0.1.0" # The version
              """,
            """
              [package]
              # The name of the package
              project-name = "my-package"
              version = "0.1.0" # The version
              """
          )
        );
    }

    @Test
    void changeKeyWithArrayValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "authors",
            "contributors"
          )),
          toml(
            """
              authors = ["John Doe", "Jane Smith"]
              version = "1.0.0"
              """,
            """
              contributors = ["John Doe", "Jane Smith"]
              version = "1.0.0"
              """
          )
        );
    }
}
