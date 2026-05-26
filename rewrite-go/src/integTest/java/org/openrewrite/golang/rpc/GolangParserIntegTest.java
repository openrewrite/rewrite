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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.test.RewriteTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.golang.Assertions.expectMethodType;
import static org.openrewrite.golang.Assertions.expectType;
import static org.openrewrite.golang.Assertions.go;

/**
 * Integration tests that parse Go source via the RPC subprocess and verify
 * the resulting LST round-trips through Java correctly.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangParserIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        // The goBuild task places the binary at build/rewrite-go-rpc
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
                .allowNonWhitespaceInWhitespace(true)
                .build());
    }

    @Test
    void verifyPackageDeclSurvivesRecipe() {
        rewriteRun(
                go(
                        """
                                package main

                                func hello() {
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Go.CompilationUnit goCu = (Go.CompilationUnit) cu;

                            // Verify the tree is valid before visiting
                            assertThat(goCu.getPadding().getPackageDecl())
                                    .as("packageDecl should exist before recipe")
                                    .isNotNull();

                            // Run a no-op visitor — should preserve the tree unchanged
                            var noopVisitor = new org.openrewrite.java.JavaIsoVisitor<org.openrewrite.ExecutionContext>() {};
                            var noopResult = (Go.CompilationUnit) noopVisitor.visit(goCu, new org.openrewrite.InMemoryExecutionContext());
                            assertThat(noopResult).as("no-op visitor should not return null").isNotNull();
                            assertThat(noopResult.getPadding().getPackageDecl())
                                    .as("packageDecl should survive no-op visitor")
                                    .isNotNull();
                        })
                )
        );
    }

    @Test
    void verifyMethodTypeAttribution() {
        rewriteRun(
                go(
                        """
                                package main

                                import "fmt"

                                func main() {
                                \tfmt.Println("Hello")
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> expectMethodType(cu, "Println", "fmt"))
                )
        );
    }

    @Test
    void verifyStructTypeAttribution() {
        rewriteRun(
                go(
                        """
                                package main

                                type Point struct {
                                \tX int
                                \tY int
                                }

                                func main() {
                                \tp := Point{X: 1, Y: 2}
                                \t_ = p
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> expectType(cu, "p", "main.Point"))
                )
        );
    }

    @Test
    void helloWorld() {
        rewriteRun(
                go(
                        """
                                package main

                                import "fmt"

                                func main() {
                                \tfmt.Println("Hello, World!")
                                }
                                """
                )
        );
    }

    @Test
    void multipleImports() {
        rewriteRun(
                go(
                        """
                                package main

                                import (
                                \t"fmt"
                                \t"strings"
                                )

                                func main() {
                                \tfmt.Println(strings.ToUpper("hello"))
                                }
                                """
                )
        );
    }

    @Test
    void structDeclaration() {
        rewriteRun(
                go(
                        """
                                package main

                                type Point struct {
                                \tX int
                                \tY int
                                }
                                """
                )
        );
    }

    @Test
    void ifElseStatement() {
        rewriteRun(
                go(
                        """
                                package main

                                func abs(x int) int {
                                \tif x < 0 {
                                \t\treturn -x
                                \t} else {
                                \t\treturn x
                                \t}
                                }
                                """
                )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
                go(
                        """
                                package main

                                func sum(n int) int {
                                \ts := 0
                                \tfor i := 0; i < n; i++ {
                                \t\ts += i
                                \t}
                                \treturn s
                                }
                                """
                )
        );
    }

    @Test
    void rangeLoop() {
        rewriteRun(
                go(
                        """
                                package main

                                func total(items []int) int {
                                \ts := 0
                                \tfor _, v := range items {
                                \t\ts += v
                                \t}
                                \treturn s
                                }
                                """
                )
        );
    }

    @Test
    void switchStatement() {
        rewriteRun(
                go(
                        """
                                package main

                                func describe(x int) string {
                                \tswitch {
                                \tcase x < 0:
                                \t\treturn "negative"
                                \tcase x == 0:
                                \t\treturn "zero"
                                \tdefault:
                                \t\treturn "positive"
                                \t}
                                }
                                """
                )
        );
    }

    @Test
    void interfaceDeclaration() {
        rewriteRun(
                go(
                        """
                                package main

                                type Stringer interface {
                                \tString() string
                                }
                                """
                )
        );
    }

    @Test
    void mapType() {
        rewriteRun(
                go(
                        """
                                package main

                                func count(words []string) map[string]int {
                                \tm := map[string]int{}
                                \tfor _, w := range words {
                                \t\tm[w]++
                                \t}
                                \treturn m
                                }
                                """
                )
        );
    }

    @Test
    void goroutineAndDefer() {
        rewriteRun(
                go(
                        """
                                package main

                                import "fmt"

                                func main() {
                                \tdefer fmt.Println("done")
                                \tgo func() {
                                \t\tfmt.Println("hello from goroutine")
                                \t}()
                                }
                                """
                )
        );
    }

    @Test
    void channelSendReceive() {
        rewriteRun(
                go(
                        """
                                package main

                                func producer(ch chan<- int) {
                                \tch <- 42
                                }
                                """
                )
        );
    }

    @Test
    void sliceExpression() {
        rewriteRun(
                go(
                        """
                                package main

                                func first(s []int) []int {
                                \treturn s[0:3]
                                }
                                """
                )
        );
    }

    @Test
    void multipleAssignment() {
        rewriteRun(
                go(
                        """
                                package main

                                func swap(a, b int) (int, int) {
                                \treturn b, a
                                }
                                """
                )
        );
    }

    @Test
    void typeDeclaration() {
        rewriteRun(
                go(
                        """
                                package main

                                type Duration int64

                                type Handler func(string) error
                                """
                )
        );
    }

    @Test
    void lineComment() {
        rewriteRun(
                go(
                        """
                                package main

                                // hello is a function
                                func hello() {
                                }
                                """
                )
        );
    }

    @Test
    void pointerTypeVar() {
        rewriteRun(
                go(
                        """
                                package main

                                func f() {
                                \tvar x *int
                                \t_ = x
                                }
                                """
                )
        );
    }

    @Test
    void structTag() {
        rewriteRun(
                go(
                        """
                                package main

                                type Finding struct {
                                \tLine string `json:"-"`
                                }
                                """
                )
        );
    }

    @Test
    void compositeAndKeyValue() {
        rewriteRun(
                go(
                        """
                                package main

                                type Config struct {
                                \tName    string
                                \tTimeout int
                                }

                                func defaults() Config {
                                \treturn Config{
                                \t\tName:    "default",
                                \t\tTimeout: 30,
                                \t}
                                }
                                """
                )
        );
    }

    @Test
    void literalWithNonPrimitiveType() {
        rewriteRun(
                go(
                        """
                                package main

                                import (
                                \t"time"

                                \t"github.com/example/pkg"
                                )

                                var c = pkg.Config{
                                \tTTL: 10 * time.Minute,
                                }
                                """
                )
        );
    }

    @Test
    void variadicParameter() {
        rewriteRun(
                go(
                        """
                                package main

                                func foo(args ...string) {
                                }
                                """
                )
        );
    }

    @Test
    void genericTypeInStructField() {
        rewriteRun(
                go(
                        """
                                package core

                                type baseCollection struct {
                                \tIndexes JSONArray[string]
                                \tData    Store[string, any]
                                }
                                """
                )
        );
    }
}
