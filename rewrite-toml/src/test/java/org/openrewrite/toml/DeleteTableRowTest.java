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

class DeleteTableRowTest implements RewriteTest {

    @DocumentExample
    @Test
    void deleteMatchingTableRow() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("package.contributors", "name", "Bob Johnson", null)),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"

              [[package.contributors]]
              name = "Carol White"
              email = "carol@example.com"
              """,
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[package.contributors]]
              name = "Carol White"
              email = "carol@example.com"
              """
          )
        );
    }

    @Test
    void deleteMultipleMatchingRows() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("team", "role", "developer", null)),
          toml(
            """
              [[team]]
              name = "Alice"
              role = "developer"

              [[team]]
              name = "Bob"
              role = "manager"

              [[team]]
              name = "Carol"
              role = "developer"
              """,
            """
              [[team]]
              name = "Bob"
              role = "manager"
              """
          )
        );
    }

    @Test
    void deleteWithRegexPattern() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("users","email", ".*@example\\.com", true)),
          toml(
            """
              [[users]]
              name = "Alice"
              email = "alice@example.com"

              [[users]]
              name = "Bob"
              email = "bob@company.org"

              [[users]]
              name = "Carol"
              email = "carol@example.com"
              """,
            """
              [[users]]
              name = "Bob"
              email = "bob@company.org"
              """
          )
        );
    }

    @Test
    void noMatchDoesNotDelete() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("contributors", "name", "NonExistent", null)),
          toml(
            """
              [[contributors]]
              name = "Alice"
              email = "alice@example.com"

              [[contributors]]
              name = "Bob"
              email = "bob@example.com"
              """
          )
        );
    }

    @Test
    void deleteFirstTableRow() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("dependencies", "version", "1.0.0", null)),
          toml(
            """
              [[dependencies]]
              name = "lib-a"
              version = "1.0.0"

              [[dependencies]]
              name = "lib-b"
              version = "2.0.0"
              """,
            """
              [[dependencies]]
              name = "lib-b"
              version = "2.0.0"
              """
          )
        );
    }

    @Test
    void deleteLastTableRow() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("dependencies", "version", "2.0.0", null)),
          toml(
            """
              [[dependencies]]
              name = "lib-a"
              version = "1.0.0"

              [[dependencies]]
              name = "lib-b"
              version = "2.0.0"
              """,
            """
              [[dependencies]]
              name = "lib-a"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void regexMatchesPartialString() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("plugins", "description", ".*deprecated.*", true)),
          toml(
            """
              [[plugins]]
              name = "plugin-a"
              description = "Active plugin"

              [[plugins]]
              name = "plugin-b"
              description = "This is deprecated and will be removed"

              [[plugins]]
              name = "plugin-c"
              description = "Another active plugin"
              """,
            """
              [[plugins]]
              name = "plugin-a"
              description = "Active plugin"

              [[plugins]]
              name = "plugin-c"
              description = "Another active plugin"
              """
          )
        );
    }

    @Test
    void doesNotDeleteFromDifferentTable() {
        rewriteRun(
          spec -> spec.recipe(new DeleteTableRow("package.contributors", "name", "Alice Smith", null)),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[package.maintainers]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[team.members]]
              name = "Alice Smith"
              role = "developer"
              """,
            """
              [[package.maintainers]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[team.members]]
              name = "Alice Smith"
              role = "developer"
              """
          )
        );
    }
}
