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

class ChangeValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeStringValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "name",
            "\"new-name\""
          )),
          toml(
            """
              name = "old-name"
              version = "1.0.0"
              """,
            """
              name = "new-name"
              version = "1.0.0"
              """
          )
        );
    }

    @Test
    void changeVersionNumber() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "package.version",
            "\"2.0.0\""
          )),
          toml(
            """
              [package]
              name = "my-package"
              version = "1.0.0"
              """,
            """
              [package]
              name = "my-package"
              version = "2.0.0"
              """
          )
        );
    }

    @Test
    void changeBooleanValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "package.publish",
            "false"
          )),
          toml(
            """
              [package]
              name = "my-package"
              publish = true
              """,
            """
              [package]
              name = "my-package"
              publish = false
              """
          )
        );
    }

    @Test
    void changeIntegerValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "server.port",
            "8080"
          )),
          toml(
            """
              [server]
              host = "localhost"
              port = 3000
              """,
            """
              [server]
              host = "localhost"
              port = 8080
              """
          )
        );
    }

    @Test
    void changeFloatValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "math.pi",
            "3.14159"
          )),
          toml(
            """
              [math]
              pi = 3.14
              e = 2.718
              """,
            """
              [math]
              pi = 3.14159
              e = 2.718
              """
          )
        );
    }

    @Test
    void changeToMultilineString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "description",
            "\"\"\"A multi-line\ndescription\"\"\""
          )),
          toml(
            """
              description = "single line"
              name = "project"
              """,
            """
              description = \"""A multi-line
              description\"""
              name = "project"
              """
          )
        );
    }

    @Test
    void doNotChangeUnmatchedKeys() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "package.version",
            "\"2.0.0\""
          )),
          toml(
            """
              [package]
              version = "1.0.0"

              [dependencies]
              version = "should-not-change"
              """,
            """
              [package]
              version = "2.0.0"

              [dependencies]
              version = "should-not-change"
              """
          )
        );
    }

    @Test
    void changeHexadecimalInteger() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "color",
            "0xFF00FF"
          )),
          toml(
            """
              color = 0xDEADBEEF
              name = "test"
              """,
            """
              color = 0xFF00FF
              name = "test"
              """
          )
        );
    }

    @Test
    void changeToInfinity() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "max_value",
            "inf"
          )),
          toml(
            """
              max_value = 1000.0
              min_value = -1000.0
              """,
            """
              max_value = inf
              min_value = -1000.0
              """
          )
        );
    }

    @Test
    void preserveComments() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "package.version",
            "\"2.0.0\""
          )),
          toml(
            """
              [package]
              # The package version
              version = "1.0.0" # inline comment
              name = "test"
              """,
            """
              [package]
              # The package version
              version = "2.0.0" # inline comment
              name = "test"
              """
          )
        );
    }
}
