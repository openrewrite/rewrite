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

class FindKeyTest implements RewriteTest {

    @DocumentExample
    @Test
    void findSpecificKey() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "package.name"
          )),
          toml(
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              """,
            """
              [package]
              ~~>name = "my-package"
              version = "0.1.0"
              """
          )
        );
    }

    @Test
    void findMultipleMatchingKeys() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "**.name"
          )),
          toml(
            """
              name = "root-name"

              [package]
              name = "package-name"

              [author]
              name = "author-name"
              email = "author@example.com"
              """,
            """
              ~~>name = "root-name"

              [package]
              ~~>name = "package-name"

              [author]
              ~~>name = "author-name"
              email = "author@example.com"
              """
          )
        );
    }

    @Test
    void findWithSingleWildcard() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "*.version"
          )),
          toml(
            """
              version = "1.0.0"

              [package]
              version = "0.1.0"

              [dependencies.serde]
              version = "1.0"
              """,
            """
              version = "1.0.0"

              [package]
              ~~>version = "0.1.0"

              [dependencies.serde]
              version = "1.0"
              """
          )
        );
    }

    @Test
    void findWithMultipleWildcards() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "**.version"
          )),
          toml(
            """
              version = "1.0.0"

              [package]
              version = "0.1.0"

              [dependencies.serde]
              version = "1.0"
              """,
            """
              ~~>version = "1.0.0"

              [package]
              ~~>version = "0.1.0"

              [dependencies.serde]
              ~~>version = "1.0"
              """
          )
        );
    }

    @Test
    void findTopLevelKey() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "title"
          )),
          toml(
            """
              title = "TOML Example"
              description = "A sample TOML file"

              [owner]
              title = "Not this one"
              """,
            """
              ~~>title = "TOML Example"
              description = "A sample TOML file"

              [owner]
              title = "Not this one"
              """
          )
        );
    }

    @Test
    void findNestedKey() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "tool.poetry.version"
          )),
          toml(
            """
              [tool.poetry]
              name = "poetry-project"
              version = "0.1.0"

              [tool.black]
              version = "22.0.0"
              """,
            """
              [tool.poetry]
              name = "poetry-project"
              ~~>version = "0.1.0"

              [tool.black]
              version = "22.0.0"
              """
          )
        );
    }

    @Test
    void findNoMatches() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "nonexistent.key"
          )),
          toml(
            """
              [package]
              name = "my-package"
              version = "0.1.0"
              """
          )
        );
    }

    @Test
    void findKeysWithArrayValues() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "**.authors"
          )),
          toml(
            """
              [package]
              name = "project"
              authors = ["John Doe", "Jane Smith"]

              [metadata]
              authors = ["Alice", "Bob"]
              """,
            """
              [package]
              name = "project"
              ~~>authors = ["John Doe", "Jane Smith"]

              [metadata]
              ~~>authors = ["Alice", "Bob"]
              """
          )
        );
    }

    @Test
    void preserveFormatting() {
        rewriteRun(
          spec -> spec.recipe(new FindKey(
            "package.keywords"
          )),
          toml(
            """
              [package]
              name = "my-package"
              # Keywords for search
              keywords = [
                  "cli",
                  "tool"
              ]
              version = "0.1.0" # Version number
              """,
            """
              [package]
              name = "my-package"
              # Keywords for search
              ~~>keywords = [
                  "cli",
                  "tool"
              ]
              version = "0.1.0" # Version number
              """
          )
        );
    }
}
