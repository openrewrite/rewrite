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

class CreateTomlFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void createNewTomlFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "pyproject.toml",
            """
              [tool.poetry]
              name = "my-project"
              version = "0.1.0"
              description = "A sample project"
              """,
            null,
            null
          )),
          toml(
            doesNotExist(),
            """
              [tool.poetry]
              name = "my-project"
              version = "0.1.0"
              description = "A sample project"
              """,
            spec -> spec.path("pyproject.toml")
          )
        );
    }

    @Test
    void createCargoToml() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "Cargo.toml",
            """
              [package]
              name = "hello_world"
              version = "0.1.0"
              edition = "2021"

              [dependencies]
              """,
            null,
            null
          )),
          toml(
            doesNotExist(),
            """
              [package]
              name = "hello_world"
              version = "0.1.0"
              edition = "2021"

              [dependencies]
              """,
            spec -> spec.path("Cargo.toml")
          )
        );
    }

    @Test
    void doNotOverwriteExistingByDefault() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "config.toml",
            """
              [new]
              value = "should not appear"
              """,
            null,
            null
          )),
          toml(
            """
              [existing]
              value = "keep this"
              """,
            spec -> spec.path("config.toml")
          )
        );
    }

    @Test
    void overwriteExistingWhenSpecified() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "config.toml",
            """
              [new]
              value = "replacement"
              """,
            null,
            true
          )),
          toml(
            """
              [existing]
              value = "will be replaced"
              """,
            """
              [new]
              value = "replacement"
              """,
            spec -> spec.path("config.toml")
          )
        );
    }

    @Test
    void createComplexTomlFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "complex.toml",
            """
              # This is a TOML document
              title = "TOML Example"

              [owner]
              name = "Tom Preston-Werner"
              dob = 1979-05-27T07:32:00-08:00

              [database]
              enabled = true
              ports = [ 8000, 8001, 8002 ]
              data = [ ["delta", "phi"], [3.14] ]
              temp_targets = { cpu = 79.5, case = 72.0 }

              [servers]

              [servers.alpha]
              ip = "10.0.0.1"
              role = "frontend"

              [servers.beta]
              ip = "10.0.0.2"
              role = "backend"
              """,
            null,
            null
          )),
          toml(
            doesNotExist(),
            """
              # This is a TOML document
              title = "TOML Example"

              [owner]
              name = "Tom Preston-Werner"
              dob = 1979-05-27T07:32:00-08:00

              [database]
              enabled = true
              ports = [ 8000, 8001, 8002 ]
              data = [ ["delta", "phi"], [3.14] ]
              temp_targets = { cpu = 79.5, case = 72.0 }

              [servers]

              [servers.alpha]
              ip = "10.0.0.1"
              role = "frontend"

              [servers.beta]
              ip = "10.0.0.2"
              role = "backend"
              """,
            spec -> spec.path("complex.toml")
          )
        );
    }

    @Test
    void doNotCreateEmptyFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "empty.toml",
            "",
            null,
            null
          ))
        );
    }

    @Test
    void createInSubdirectory() {
        rewriteRun(
          spec -> spec.recipe(new CreateTomlFile(
            "config/settings.toml",
            """
              [app]
              name = "MyApp"
              debug = false
              """,
            null,
            null
          )),
          toml(
            doesNotExist(),
            """
              [app]
              name = "MyApp"
              debug = false
              """,
            spec -> spec.path("config/settings.toml")
          )
        );
    }
}
