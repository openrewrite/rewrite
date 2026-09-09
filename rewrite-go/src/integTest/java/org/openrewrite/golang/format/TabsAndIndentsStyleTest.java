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
package org.openrewrite.golang.format;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * The Go formatter emulates gofmt, whose canonical indentation is tabs. But when a recipe touches a
 * source that was written with spaces, re-indenting the changed region to tabs produces a mixed-style
 * file and a noisy diff. {@link TabsAndIndentsVisitor} therefore detects the source's own indentation
 * unit and reuses it, keeping space-indented sources space-indented and tab-indented sources tab-indented.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class TabsAndIndentsStyleTest implements RewriteTest {

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
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TabsAndIndentsVisitor<>(null)))
                .typeValidationOptions(TypeValidation.builder()
                        .allowNonWhitespaceInWhitespace(true)
                        .identifiers(false)
                        .methodInvocations(false)
                        .build());
    }

    @Test
    void reindentsUsingDetectedSpaceUnit() {
        // given a space-indented source with an over-indented body,
        // when the formatter fixes the indentation,
        // then it uses the source's four-space unit rather than a tab
        rewriteRun(
                go(
                        """
                        package main

                        func a() {
                            return
                        }

                        func b() {
                                return
                        }
                        """,
                        """
                        package main

                        func a() {
                            return
                        }

                        func b() {
                            return
                        }
                        """
                )
        );
    }

    @Test
    void reindentsUsingDetectedTwoSpaceUnit() {
        // given a two-space-indented source with an over-indented body,
        // when the formatter fixes the indentation,
        // then it uses the source's two-space unit
        rewriteRun(
                go(
                        """
                        package main

                        func a() {
                          if x {
                            return
                          }
                        }

                        func b() {
                              return
                        }
                        """,
                        """
                        package main

                        func a() {
                          if x {
                            return
                          }
                        }

                        func b() {
                          return
                        }
                        """
                )
        );
    }

    @Test
    void keepsTabIndentedSourceOnTabs() {
        // given a canonical tab-indented (gofmt) source,
        // when the formatter runs,
        // then it stays on tabs
        rewriteRun(
                go(
                        """
                        package main

                        func a() {
                        \treturn
                        }
                        """
                )
        );
    }
}
