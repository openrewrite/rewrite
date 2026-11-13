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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.python.Assertions.python;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Timeout(60)
class AwaitPrinterTest implements RewriteTest {

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
        spec.recipe(toRecipe(() -> new PythonIsoVisitor<ExecutionContext>() {
            @Override
            public J.Assert visitAssert(J.Assert assert_, ExecutionContext ctx) {
                J.Assert a = super.visitAssert(assert_, ctx);
                Expression condition = a.getCondition();
                if (condition instanceof Py.Await) {
                    return a;
                }
                return a.withCondition(new Py.Await(randomId(), condition.getPrefix(), Markers.EMPTY,
                        condition.withPrefix(Space.EMPTY), condition.getType()));
            }
        })).validateRecipeSerialization(false);
    }

    @Test
    void separateSynthesizedAwaitFromMethodCall() {
        rewriteRun(
                python(
                        """
                        async def test(session):
                            assert session.ready()
                        """,
                        """
                        async def test(session):
                            assert await session.ready()
                        """
                )
        );
    }

    @Test
    void keepSynthesizedAwaitGluedToParenthesizedOperand() {
        rewriteRun(
                python(
                        """
                        async def test(session):
                            assert (session.ready())
                        """,
                        """
                        async def test(session):
                            assert await(session.ready())
                        """
                )
        );
    }
}
