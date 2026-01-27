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
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;

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
}
