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

class ReplaceTableRowTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceRowRemovingUnspecifiedKeys() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "package.contributors",
            "name = \"Alice Smith\"\nemail = \"alice.new@example.com\"",
            "name"
          )),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "maintainer"
              active = true

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
    void replaceRowAddingNewKeys() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "users",
            "name = \"Alice\"\nemail = \"alice@example.com\"\nactive = true\nrole = \"admin\"",
            "name"
          )),
          toml(
            """
              [[users]]
              name = "Alice"
              email = "alice.old@example.com"

              [[users]]
              name = "Bob"
              email = "bob@example.com"
              """,
            """
              [[users]]
              name = "Alice"
              email = "alice@example.com"
              active = true
              role = "admin"

              [[users]]
              name = "Bob"
              email = "bob@example.com"
              """
          )
        );
    }

    @Test
    void noChangeWhenNoMatch() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "team",
            "name = \"Carol\"\nrole = \"developer\"",
            "name"
          )),
          toml(
            """
              [[team]]
              name = "Alice"
              role = "manager"

              [[team]]
              name = "Bob"
              role = "developer"
              """
          )
        );
    }

    @Test
    void replaceCompletelyDifferentStructure() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "dependencies",
            "name = \"lib-a\"\nversion = \"2.0.0\"\nfeatures = [\"default\"]",
            "name"
          )),
          toml(
            """
              [[dependencies]]
              name = "lib-a"
              git = "https://github.com/example/lib-a"
              branch = "main"

              [[dependencies]]
              name = "lib-b"
              version = "1.0.0"
              """,
            """
              [[dependencies]]
              name = "lib-a"
              version = "2.0.0"
              features = ["default"]

              [[dependencies]]
              name = "lib-b"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void noChangeWhenRowAlreadyMatches() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "servers",
            "id = \"server-1\"\nhost = \"localhost\"\nport = 8080",
            "id"
          )),
          toml(
            """
              [[servers]]
              id = "server-1"
              host = "localhost"
              port = 8080

              [[servers]]
              id = "server-2"
              host = "server2.example.com"
              port = 8081
              """
          )
        );
    }

    @Test
    void replaceMultipleMatchingRows() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "config",
            "type = \"cache\"\nenabled = true",
            "type"
          )),
          toml(
            """
              [[config]]
              type = "cache"
              enabled = false
              ttl = 3600
              size = "1GB"

              [[config]]
              type = "database"
              host = "localhost"
              port = 5432

              [[config]]
              type = "cache"
              enabled = false
              ttl = 7200
              """,
            """
              [[config]]
              type = "cache"
              enabled = true

              [[config]]
              type = "database"
              host = "localhost"
              port = 5432

              [[config]]
              type = "cache"
              enabled = true
              """
          )
        );
    }

    @Test
    void noChangeWhenTableDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "plugins",
            "name = \"plugin-a\"\nenabled = true",
            "name"
          )),
          toml(
            """
              [package]
              name = "my-project"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void replaceMinimalRowWithExtendedOne() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "endpoints",
            "name = \"api\"\nurl = \"https://api.example.com\"\nmethod = \"GET\"\ntimeout = 30",
            "name"
          )),
          toml(
            """
              [[endpoints]]
              name = "api"
              url = "https://old-api.example.com"

              [[endpoints]]
              name = "web"
              url = "https://example.com"
              """,
            """
              [[endpoints]]
              name = "api"
              url = "https://api.example.com"
              method = "GET"
              timeout = 30

              [[endpoints]]
              name = "web"
              url = "https://example.com"
              """
          )
        );
    }

    @Test
    void doesNotReplaceInDifferentTable() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceTableRow(
            "package.contributors",
            "name = \"Alice Smith\"\nemail = \"alice.new@example.com\"",
            "name"
          )),
          toml(
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "maintainer"

              [[package.maintainers]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "maintainer"

              [[team.members]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "developer"
              """,
            """
              [[package.contributors]]
              name = "Alice Smith"
              email = "alice.new@example.com"

              [[package.maintainers]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "maintainer"

              [[team.members]]
              name = "Alice Smith"
              email = "alice@example.com"
              role = "developer"
              """
          )
        );
    }
}
