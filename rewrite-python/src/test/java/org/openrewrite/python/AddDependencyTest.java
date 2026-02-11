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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.uv;

class AddDependencyTest implements RewriteTest {

    @Test
    void addDependencyWithResolvedProject(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          uv(tempDir,
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
                    "requests>=2.28.0",
                    "flask>=2.0",
                ]
                """
            )
          )
        );
    }

    @Test
    void addDependencyToExistingList() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", null, null, null)),
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
                  "requests>=2.28.0",
                  "click>=8.0",
                  "flask",
              ]
              """
          )
        );
    }

    @Test
    void addDependencyWithVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
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
                  "requests>=2.28.0",
                  "flask>=2.0",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenAlreadyPresent() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("requests", null, null, null)),
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
    void addToEmptyDependencyList() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", ">=2.0", null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = []
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["flask>=2.0"]
              """
          )
        );
    }

    @Test
    void addToInlineDependencyList() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("flask", null, null, null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0", "flask"]
              """
          )
        );
    }

    @Test
    void addToOptionalDependencies() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("pytest-cov", null, "project.optional-dependencies", "dev")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [project.optional-dependencies]
              dev = [
                  "pytest>=7.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [project.optional-dependencies]
              dev = [
                  "pytest>=7.0",
                  "pytest-cov",
              ]
              """
          )
        );
    }

    @Test
    void addToDependencyGroup() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("pytest-cov", ">=4.0", "dependency-groups", "test")),
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
                  "pytest>=7.0",
                  "pytest-cov>=4.0",
              ]
              """
          )
        );
    }

    @Test
    void skipWhenAlreadyInScope() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("pytest", null, "dependency-groups", "test")),
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
              """
          )
        );
    }

    @Test
    void validateRequiresGroupName() {
        AddDependency recipe = new AddDependency("pytest", null, "project.optional-dependencies", null);
        assertThat(recipe.validate().isValid()).isFalse();
    }
}
