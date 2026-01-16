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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.openrewrite.python.Assertions.python;

/**
 * Integration test for running Python recipes via RPC.
 * <p>
 * This test verifies that Python recipes (like RemovePass) can be:
 * 1. Discovered via GetMarketplace RPC
 * 2. Prepared for execution via PrepareRecipe RPC
 * 3. Applied to Python source code via Visit RPC
 * 4. Printed back via Print RPC
 */
class RemovePassIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log"))
                .traceRpcMessages()
        );
    }

    @AfterEach
    void after() throws IOException {
        PythonRewriteRpc.shutdownCurrent();
        if (Files.exists(tempDir.resolve("python-rpc.log"))) {
            System.out.println("=== Python RPC Log ===");
            System.out.println(Files.readString(tempDir.resolve("python-rpc.log")));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @Test
    void removesPassFromFunctionWithOtherStatements() {
        Recipe removePass = client().prepareRecipe("org.openrewrite.python.RemovePass", Map.of());
        rewriteRun(
                spec -> spec.recipe(removePass),
                python(
                        """
                        def foo():
                            pass
                            x = 1
                        """,
                        """
                        def foo():
                            x = 1
                        """
                )
        );
    }

    @Test
    void keepsPassWhenOnlyStatementInFunction() {
        Recipe removePass = client().prepareRecipe("org.openrewrite.python.RemovePass", Map.of());
        rewriteRun(
                spec -> spec.recipe(removePass),
                python(
                        """
                        def foo():
                            pass
                        """
                )
        );
    }

    @Test
    void removesPassFromClassMethod() {
        Recipe removePass = client().prepareRecipe("org.openrewrite.python.RemovePass", Map.of());
        rewriteRun(
                spec -> spec.recipe(removePass),
                python(
                        """
                        class Foo:
                            def bar(self):
                                pass
                                x = 1
                        """,
                        """
                        class Foo:
                            def bar(self):
                                x = 1
                        """
                )
        );
    }

    @Test
    void removesPassFromIfBlock() {
        Recipe removePass = client().prepareRecipe("org.openrewrite.python.RemovePass", Map.of());
        rewriteRun(
                spec -> spec.recipe(removePass),
                python(
                        """
                        if True:
                            pass
                            x = 1
                        """,
                        """
                        if True:
                            x = 1
                        """
                )
        );
    }

    @Test
    void keepsPassWhenOnlyDocstringInFunction() {
        Recipe removePass = client().prepareRecipe("org.openrewrite.python.RemovePass", Map.of());
        rewriteRun(
                spec -> spec.recipe(removePass),
                python(
                        """
                        def foo():
                            \"\"\"This is a docstring.\"\"\"
                            pass
                        """
                )
        );
    }

    private PythonRewriteRpc client() {
        return PythonRewriteRpc.getOrStart();
    }
}
