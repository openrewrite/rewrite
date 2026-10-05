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
package org.openrewrite.python.cleanup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.python.Assertions.python;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Timeout(60)
class SimplifyBooleanExpressionVisitorTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log")));
    }

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(SimplifyBooleanExpressionVisitor::new))
                .validateRecipeSerialization(false);
    }

    @Test
    void doNotInvertChainedComparison() {
        rewriteRun(
                python(
                        """
                        def test(value):
                            if not (0 <= value < 6):
                                return value
                        """
                )
        );
    }

    @Test
    void invertSingleComparison() {
        rewriteRun(
                python(
                        """
                        def test(a, b):
                            if not (a < b):
                                return a
                        """,
                        """
                        def test(a, b):
                            if a >= b:
                                return a
                        """
                )
        );
    }
}
