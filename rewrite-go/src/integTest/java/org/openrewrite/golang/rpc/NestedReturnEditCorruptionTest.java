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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Editing a statement inside an {@code if} (or {@code for}) must round-trip cleanly over RPC. The
 * {@code receiveBlockBody} path in {@code rewrite-go/pkg/rpc/java_receiver.go} has to pass the correct
 * baseline when reconstructing {@code J.If.thenPart} and {@code ForLoop/ForEachLoop.body}; otherwise on
 * a CHANGE every NO_CHANGE sibling field resolves to its zero value — the then-block's whitespace
 * collapses (printing {@code if ...; a != 0{returnb}}) and, when wrapped in a {@code for}, the inner
 * {@code if}'s {@code Condition} is dropped entirely.
 * <p>
 * A one-line {@code toRecipe} visitor that renames {@code a} to {@code b} is enough to exercise this —
 * the corruption is structural, not recipe-specific.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class NestedReturnEditCorruptionTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
                .allowNonWhitespaceInWhitespace(true)
                .identifiers(false)
                .methodInvocations(false)
                .build());
    }

    /**
     * Renames the identifier in any {@code return a} to {@code b} — the simplest
     * possible edit to a statement. Used by every case below.
     */
    private static org.openrewrite.Recipe renameReturned() {
        return toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Return visitReturn(J.Return aReturn, ExecutionContext ctx) {
                if (aReturn.getExpression() instanceof J.Identifier id && "a".equals(id.getSimpleName())) {
                    return aReturn.withExpression(id.withSimpleName("b"));
                }
                return aReturn;
            }
        });
    }

    /** if-with-init, inside a for. */
    @Test
    void ifWithInitInsideFor() {
        rewriteRun(
                spec -> spec.recipe(renameReturned()),
                go(
                        """
                        package main

                        func f(items []int) int {
                        \tfor _, item := range items {
                        \t\tif a := g(item); a != 0 {
                        \t\t\treturn a
                        \t\t}
                        \t}
                        \treturn 0
                        }
                        """,
                        """
                        package main

                        func f(items []int) int {
                        \tfor _, item := range items {
                        \t\tif a := g(item); a != 0 {
                        \t\t\treturn b
                        \t\t}
                        \t}
                        \treturn 0
                        }
                        """
                )
        );
    }

    /** plain if (no init), inside a for. */
    @Test
    void plainIfInsideFor() {
        rewriteRun(
                spec -> spec.recipe(renameReturned()),
                go(
                        """
                        package main

                        func f(items []int, a int) int {
                        \tfor _, item := range items {
                        \t\tif a != 0 {
                        \t\t\treturn a
                        \t\t}
                        \t}
                        \treturn 0
                        }
                        """,
                        """
                        package main

                        func f(items []int, a int) int {
                        \tfor _, item := range items {
                        \t\tif a != 0 {
                        \t\t\treturn b
                        \t\t}
                        \t}
                        \treturn 0
                        }
                        """
                )
        );
    }

    /** if-with-init at top level (no enclosing for). */
    @Test
    void ifWithInitNoFor() {
        rewriteRun(
                spec -> spec.recipe(renameReturned()),
                go(
                        """
                        package main

                        func f(item int) int {
                        \tif a := g(item); a != 0 {
                        \t\treturn a
                        \t}
                        \treturn 0
                        }
                        """,
                        """
                        package main

                        func f(item int) int {
                        \tif a := g(item); a != 0 {
                        \t\treturn b
                        \t}
                        \treturn 0
                        }
                        """
                )
        );
    }

    private static org.openrewrite.Recipe lowercaseErrorfFormat() {
        return toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if ("Errorf".equals(m.getSimpleName()) && m.getArguments().size() == 3 &&
                    m.getArguments().get(0) instanceof J.Literal lit && lit.getValue() instanceof String s &&
                    s.startsWith("Expected error")) {
                    String replaced = "expected error" + s.substring("Expected error".length());
                    J.Literal newLit = lit.withValue(replaced).withValueSource("\"" + replaced + "\"");
                    return m.withArguments(List.of(newLit, m.getArguments().get(1), m.getArguments().get(2)));
                }
                return m;
            }
        });
    }

    @Test
    void editCaseBodyStatementKeepsSiblingFields() {
        rewriteRun(
                spec -> spec.recipe(lowercaseErrorfFormat()),
                go(
                        """
                        package main

                        func TestRun(t *T) {
                        \tfor _, tc := range testcases {
                        \t\terr := c.Execute()
                        \t\tswitch {
                        \t\tcase err == nil && len(tc.expectErr) > 0:
                        \t\t\tt.Errorf("Expected error %q but got nil", tc.expectErr)
                        \t\tcase err != nil && err.Error() != tc.expectErr:
                        \t\t\tt.Errorf("Expected error %q but got %q", tc.expectErr, err)
                        \t\t}
                        \t}
                        }
                        """,
                        """
                        package main

                        func TestRun(t *T) {
                        \tfor _, tc := range testcases {
                        \t\terr := c.Execute()
                        \t\tswitch {
                        \t\tcase err == nil && len(tc.expectErr) > 0:
                        \t\t\tt.Errorf("Expected error %q but got nil", tc.expectErr)
                        \t\tcase err != nil && err.Error() != tc.expectErr:
                        \t\t\tt.Errorf("expected error %q but got %q", tc.expectErr, err)
                        \t\t}
                        \t}
                        }
                        """
                )
        );
    }
}
