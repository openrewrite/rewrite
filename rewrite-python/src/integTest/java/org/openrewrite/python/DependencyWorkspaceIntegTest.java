/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.*;

/**
 * Integration tests for Python dependency workspace with external packages.
 * <p>
 * These tests verify that:
 * 1. The uv() helper creates a cached workspace with dependencies
 * 2. Type attribution works for external packages via ty LSP
 * 3. Java recipes can match methods from external packages
 */
@SuppressWarnings("PyUnresolvedReferences")
class DependencyWorkspaceIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @BeforeEach
    void before() {
        DependencyWorkspace.clearCache();
    }

    @Test
    void findMethodFromExternalPackage() {
        // Test that we can find methods from an external package (requests)
        // This requires:
        // 1. Creating a workspace with the requests package installed
        // 2. ty using that workspace for type resolution
        // 3. MethodMatcher matching requests.get()
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("requests get(..)", false)),
          uv(
            tempDir,
            python(
              """
                import requests
                response = requests.get("https://example.com")
                """,
              """
                import requests
                response = /*~~>*/requests.get("https://example.com")
                """
            ),
            pyproject(
              """
                [project]
                name = "test-project"
                version = "0.1.0"
                dependencies = ["requests>=2.28.0"]
                """
            )
          )
        );
    }

    @Test
    void pyprojectHasResolvedDependenciesFromUvLock() {
        rewriteRun(
          spec -> spec.recipe(Recipe.noop()),
          uv(
            tempDir,
            pyproject(
              """
                [project]
                name = "test-project"
                version = "0.1.0"
                requires-python = ">=3.10"
                dependencies = ["requests>=2.28.0"]
                """,
              spec2 -> spec2.afterRecipe(doc -> {
                  PythonResolutionResult marker = doc.getMarkers()
                          .findFirst(PythonResolutionResult.class)
                          .orElse(null);
                  assertThat(marker).isNotNull();
                  assertThat(marker.getName()).isEqualTo("test-project");
                  assertThat(marker.getPackageManager())
                          .isEqualTo(PythonResolutionResult.PackageManager.Uv);
                  assertThat(marker.getResolvedDependencies()).isNotEmpty();
                  assertThat(marker.getResolvedDependency("requests")).isNotNull();
                  assertThat(marker.getResolvedDependency("requests").getVersion()).isNotEmpty();

                  // The declared dependency should be linked to its resolved version
                  assertThat(marker.getDependencies()).hasSize(1);
                  assertThat(marker.getDependencies().get(0).getResolved()).isNotNull();
                  assertThat(marker.getDependencies().get(0).getResolved().getVersion()).isNotEmpty();
              })
            )
          )
        );
    }
}
