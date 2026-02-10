/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.uv;

class ChangeDependencyVersionTest implements RewriteTest {

    @Test
    void changeVersionWithResolvedProject(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("requests", ">=2.31.0")),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                    "click>=8.0",
                ]
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.31.0",
                    "click>=8.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void changeVersionConstraint() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("requests", ">=2.31.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "click>=8.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.31.0",
                  "click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionWithExtras() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("requests", ">=2.31.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests[security]>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests[security]>=2.31.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionWithMarker() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("requests", ">=2.31.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0; python_version>='3.8'",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.31.0; python_version>='3.8'",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenNotPresent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("nonexistent", ">=1.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """
          )
        );
    }

    @Test
    void normalizeNameForMatching() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("typing_extensions", ">=4.8.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "typing-extensions>=4.0.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "typing-extensions>=4.8.0",
              ]
              """
          )
        );
    }

    @Test
    void changeVersionInInlineList() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("requests", ">=2.31.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0", "click>=8.0"]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.31.0", "click>=8.0"]
              """
          )
        );
    }
}
