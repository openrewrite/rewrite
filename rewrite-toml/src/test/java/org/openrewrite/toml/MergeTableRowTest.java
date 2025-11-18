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

class MergeTableRowTest implements RewriteTest {

    @DocumentExample
    @Test
    void mergeExistingRow() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "package.contributors",
            "name = \"Alice Smith\"\nemail = \"alice.new@example.com\"\nrole = \"maintainer\"",
            "name"
          )),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"
              """,
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice.new@example.com"
              role = "maintainer"

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"
              """
          )
        );
    }

    @Test
    void insertNewRow() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "package.contributors",
            "name = \"Carol White\"\nemail = \"carol@example.com\"",
            "name"
          )),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"
              """,
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
              """
          )
        );
    }

    @Test
    void createNewTableWhenNoneExists() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "dependencies",
            "name = \"lib-a\"\nversion = \"1.0.0\"",
            "name"
          )),
          toml(
            """
              [package]
              name = "my-project"
              version = "1.0.0"
              """,
            """
              [package]
              name = "my-project"
              version = "1.0.0"

              [[dependencies]]
              name = "lib-a"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void mergeAddsNewProperty() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "team",
            "name = \"Alice\"\nrole = \"developer\"\ndepartment = \"engineering\"",
            "name"
          )),
          toml(
            """
              [[team]]
              name = "Alice"
              role = "developer"

              [[team]]
              name = "Bob"
              role = "manager"
              """,
            """
              [[team]]
              name = "Alice"
              role = "developer"
              department = "engineering"

              [[team]]
              name = "Bob"
              role = "manager"
              """
          )
        );
    }

    @Test
    void mergeUpdatesExistingProperty() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "plugins",
            "name = \"plugin-a\"\nversion = \"2.0.0\"",
            "name"
          )),
          toml(
            """
              [[plugins]]
              name = "plugin-a"
              version = "1.0.0"
              enabled = true

              [[plugins]]
              name = "plugin-b"
              version = "1.5.0"
              """,
            """
              [[plugins]]
              name = "plugin-a"
              version = "2.0.0"
              enabled = true

              [[plugins]]
              name = "plugin-b"
              version = "1.5.0"
              """
          )
        );
    }

    @Test
    void noChangeWhenIdenticalRowExists() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "users",
            "name = \"Alice\"\nemail = \"alice@example.com\"",
            "name"
          )),
          toml(
            """
              [[users]]
              name = "Alice"
              email = "alice@example.com"

              [[users]]
              name = "Bob"
              email = "bob@example.com"
              """
          )
        );
    }

    @Test
    void mergeWithDifferentIdentifyingProperty() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "servers",
            "id = \"server-1\"\nhost = \"new-host.example.com\"\nport = 9090",
            "id"
          )),
          toml(
            """
              [[servers]]
              id = "server-1"
              host = "old-host.example.com"
              port = 8080

              [[servers]]
              id = "server-2"
              host = "server2.example.com"
              port = 8081
              """,
            """
              [[servers]]
              id = "server-1"
              host = "new-host.example.com"
              port = 9090

              [[servers]]
              id = "server-2"
              host = "server2.example.com"
              port = 8081
              """
          )
        );
    }

    @Test
    void mergeRemovesPropertyWithNullValue() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "plugins",
            "name = \"plugin-a\"\nversion = \"2.0.0\"\nenabled = null",
            "name"
          )),
          toml(
            """
              [[plugins]]
              name = "plugin-a"
              version = "1.0.0"
              enabled = true

              [[plugins]]
              name = "plugin-b"
              version = "1.5.0"
              """,
            """
              [[plugins]]
              name = "plugin-a"
              version = "2.0.0"

              [[plugins]]
              name = "plugin-b"
              version = "1.5.0"
              """
          )
        );
    }

    @Test
    void doesNotMergeIntoDifferentTable() {
        rewriteRun(
          spec -> spec.recipe(new MergeTableRow(
            "package.contributors",
            "name = \"Alice Smith\"\nemail = \"alice.new@example.com\"",
            "name"
          )),
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
              email = "alice@example.com"
              """,
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice.new@example.com"

              [[package.maintainers]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[team.members]]
              name = "Alice Smith"
              email = "alice@example.com"
              """
          )
        );
    }
}
