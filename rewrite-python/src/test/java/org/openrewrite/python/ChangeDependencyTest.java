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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.pyproject;

class ChangeDependencyTest implements RewriteTest {

    @Test
    void renamePackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", null)),
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
                  "httpx>=2.28.0",
                  "click>=8.0",
              ]
              """
          )
        );
    }

    @Test
    void renameWithNewVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", ">=0.24.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "httpx>=0.24.0",
              ]
              """
          )
        );
    }

    @Test
    void renamePreservesExtrasAndMarkers() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests[security]>=2.28.0; python_version>='3.8'",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "httpx[security]>=2.28.0; python_version>='3.8'",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenNotFound() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("nonexistent", "replacement", null)),
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
    void bareVersionNormalized() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("requests", "httpx", "0.24.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "httpx>=0.24.0",
              ]
              """
          )
        );
    }

    @Test
    void renameAcrossScopes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("pytest", "pytest-ng", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest>=7.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [dependency-groups]
              test = [
                  "pytest-ng>=7.0",
              ]
              """
          )
        );
    }
}
