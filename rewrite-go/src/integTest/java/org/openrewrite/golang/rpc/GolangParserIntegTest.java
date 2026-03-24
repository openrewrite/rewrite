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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.golang.Assertions.goSource;

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
    void helloWorld() {
        rewriteRun(
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
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
                goSource(
                        """
                                package main

                                type Duration int64

                                type Handler func(string) error
                                """
                )
        );
    }

    @Test
    void compositeAndKeyValue() {
        rewriteRun(
                goSource(
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
}
