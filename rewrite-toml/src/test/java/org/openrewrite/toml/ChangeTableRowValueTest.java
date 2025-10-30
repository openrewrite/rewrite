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

class ChangeTableRowValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeValueInMatchingRow() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "package.contributors",
            "name",
            "Alice Smith",
            null,
            "email",
            "\"alice.new@example.com\""
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

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"
              """
          )
        );
    }

    @Test
    void changeValueWithRegexMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "users",
            "email",
            ".*@example\\.com",
            true,
            "active",
            "false"
          )),
          toml(
            """
              [[users]]
              name = "Alice"
              email = "alice@example.com"
              active = true

              [[users]]
              name = "Bob"
              email = "bob@company.org"
              active = true
              """,
            """
              [[users]]
              name = "Alice"
              email = "alice@example.com"
              active = false

              [[users]]
              name = "Bob"
              email = "bob@company.org"
              active = true
              """
          )
        );
    }

    @Test
    void changeNumericValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "dependencies",
            "name",
            "lib-a",
            null,
            "version",
            "\"2.0.0\""
          )),
          toml(
            """
              [[dependencies]]
              name = "lib-a"
              version = "1.0.0"

              [[dependencies]]
              name = "lib-b"
              version = "1.5.0"
              """,
            """
              [[dependencies]]
              name = "lib-a"
              version = "2.0.0"

              [[dependencies]]
              name = "lib-b"
              version = "1.5.0"
              """
          )
        );
    }

    @Test
    void changeBooleanValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "plugins",
            "name",
            "plugin-a",
            null,
            "enabled",
            "false"
          )),
          toml(
            """
              [[plugins]]
              name = "plugin-a"
              enabled = true

              [[plugins]]
              name = "plugin-b"
              enabled = true
              """,
            """
              [[plugins]]
              name = "plugin-a"
              enabled = false

              [[plugins]]
              name = "plugin-b"
              enabled = true
              """
          )
        );
    }

    @Test
    void noChangeWhenNoMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "team",
            "name",
            "NonExistent",
            null,
            "role",
            "\"manager\""
          )),
          toml(
            """
              [[team]]
              name = "Alice"
              role = "developer"

              [[team]]
              name = "Bob"
              role = "developer"
              """
          )
        );
    }

    @Test
    void changeMultipleMatchingRows() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "servers",
            "type",
            "web",
            null,
            "port",
            "8080"
          )),
          toml(
            """
              [[servers]]
              name = "server-1"
              type = "web"
              port = 80

              [[servers]]
              name = "server-2"
              type = "database"
              port = 5432

              [[servers]]
              name = "server-3"
              type = "web"
              port = 443
              """,
            """
              [[servers]]
              name = "server-1"
              type = "web"
              port = 8080

              [[servers]]
              name = "server-2"
              type = "database"
              port = 5432

              [[servers]]
              name = "server-3"
              type = "web"
              port = 8080
              """
          )
        );
    }

    @Test
    void noChangeWhenValueAlreadyMatches() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "config",
            "name",
            "setting-a",
            null,
            "value",
            "\"enabled\""
          )),
          toml(
            """
              [[config]]
              name = "setting-a"
              value = "enabled"

              [[config]]
              name = "setting-b"
              value = "disabled"
              """
          )
        );
    }

    @Test
    void removePropertyWhenNewValueIsNull() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "package.contributors",
            "name",
            "Alice Smith",
            null,
            "role",
            null
          )),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "maintainer"

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"
              role = "contributor"
              """,
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"

              [[package.contributors]]
              name = "Bob Johnson"
              email = "bob@example.com"
              role = "contributor"
              """
          )
        );
    }

    @Test
    void doesNotChangeValueInDifferentTable() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTableRowValue(
            "package.contributors",
            "name",
            "Alice Smith",
            null,
            "email",
            "\"alice.new@example.com\""
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
