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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.toml.Assertions.toml;

class DeleteKeyTest implements RewriteTest {

    @DocumentExample
    @Test
    void deleteTopLevelKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "description"
          )),
          toml(
            """
              name = "my-project"
              description = "A sample project"
              version = "1.0.0"
              """,
            """
              name = "my-project"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void deleteKeyFromTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.keywords"
          )),
          toml(
            """
              [package]
              name = "my-package"
              keywords = ["cli", "tool"]
              version = "0.1.0"
              """,
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              """
          )
        );
    }

    @Test
    void deleteKeyPreservesOtherKeys() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.license"
          )),
          toml(
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              license = "MIT"
              authors = ["John Doe"]
              """,
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              authors = ["John Doe"]
              """
          )
        );
    }

    @Test
    void deleteKeyFromNestedTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "tool.poetry.homepage"
          )),
          toml(
            """
              [tool.poetry]
              name = "poetry-project"
              version = "0.1.0"
              homepage = "https://example.com"
              repository = "https://github.com/example/repo"
              """,
            """
              [tool.poetry]
              name = "poetry-project"
              version = "0.1.0"
              repository = "https://github.com/example/repo"
              """
          )
        );
    }

    @Test
    void deleteFirstKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.name"
          )),
          toml(
            """
              [package]
              name = "first-key"
              version = "1.0.0"
              description = "test"
              """,
            """
              [package]
              version = "1.0.0"
              description = "test"
              """
          )
        );
    }

    @Test
    void deleteLastKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.description"
          )),
          toml(
            """
              [package]
              name = "test"
              version = "1.0.0"
              description = "last-key"
              """,
            """
              [package]
              name = "test"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void deleteOnlyKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.name"
          )),
          toml(
            """
              [package]
              name = "only-key"

              [dependencies]
              """,
            """
              [package]

              [dependencies]
              """
          )
        );
    }

    @Test
    void doNotDeleteUnmatchedKeys() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.name"
          )),
          toml(
            """
              [package]
              name = "my-package"

              [dependencies]
              name = "should-not-be-deleted"
              """,
            """
              [package]

              [dependencies]
              name = "should-not-be-deleted"
              """
          )
        );
    }

    @Test
    void deleteKeyWithArrayValue() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "authors"
          )),
          toml(
            """
              name = "project"
              authors = ["John Doe", "Jane Smith"]
              version = "1.0.0"
              """,
            """
              name = "project"
              version = "1.0.0"
              """
          )
        );
    }

    @Disabled
    @Test
    void preserveComments() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(
            "package.keywords"
          )),
          toml(
            """
              [package]
              # Package information
              name = "my-package"
              keywords = ["cli", "tool"] # To be deleted
              version = "0.1.0" # Version number
              """,
            """
              [package]
              # Package information
              name = "my-package"
              version = "0.1.0" # Version number
              """
          )
        );
    }
}
