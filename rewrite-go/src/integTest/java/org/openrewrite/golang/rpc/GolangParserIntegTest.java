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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Regression test for the {@code dimensionsAfterName} field added to
     * {@code J.MethodDeclaration} in #6992. The Go RPC sender/receiver originally
     * omitted this left-padded list (positioned between {@code parameters} and
     * {@code throws}), desyncing the RPC stream by one field for every method
     * declaration. The symptom was a {@code ClassCastException: J$Block cannot be
     * cast to JContainer} in {@code JavaReceiver.visitMethodDeclaration}, which
     * broke parsing of <em>any</em> Go source containing a function — not just
     * C-style arrays (which don't exist in Go). This asserts a plain method
     * declaration round-trips with its parameters and body intact.
     */
    @Test
    void methodDeclarationRoundTrips() {
        rewriteRun(
                go(
                        """
                                package main

                                func add(a int, b int) int {
                                \treturn a + b
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            J.MethodDeclaration method = new JavaIsoVisitor<AtomicReference<J.MethodDeclaration>>() {
                                @Override
                                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, AtomicReference<J.MethodDeclaration> found) {
                                    if ("add".equals(m.getSimpleName())) {
                                        found.set(m);
                                    }
                                    return super.visitMethodDeclaration(m, found);
                                }
                            }.reduce(cu, new AtomicReference<J.MethodDeclaration>()).get();

                            assertThat(method).as("method declaration 'add' should be present").isNotNull();
                            assertThat(method.getParameters()).as("parameters should survive the round-trip").hasSize(2);
                            assertThat(method.getBody()).as("body should be a J.Block, not a desynced field").isNotNull();
                            assertThat(method.getReturnTypeExpression()).as("return type should survive the round-trip").isNotNull();
                        })
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
    void typeSetUnionConstraintWithApproximation() {
        rewriteRun(
                go(
                        """
                                package main

                                type Signed interface {
                                \t~int | ~int8 | ~int16 | ~int32 | ~int64
                                }
                                """
                )
        );
    }

    @Test
    void standaloneApproximationConstraint() {
        rewriteRun(
                go(
                        """
                                package main

                                type MyInt interface {
                                \t~int
                                }
                                """
                )
        );
    }

    @Test
    void unionOfNamedConstraints() {
        rewriteRun(
                go(
                        """
                                package main

                                type Signed interface {
                                \t~int | ~int64
                                }

                                type Unsigned interface {
                                \t~uint | ~uint64
                                }

                                type Integer interface {
                                \tSigned | Unsigned
                                }
                                """
                )
        );
    }

    @Test
    void plainUnionWithoutApproximation() {
        // Mirrors klauspost/compress .../le.go — a plain union of
        // primitive type names with no `~`.
        rewriteRun(
                go(
                        """
                                package main

                                type Indexer interface {
                                \tint | int8 | int16 | int32 | int64 | uint | uint8 | uint16 | uint32 | uint64
                                }
                                """
                )
        );
    }

    @Test
    void unionOfQualifiedNameAndSlice() {
        // Mirrors golang-jwt/jwt .../token.go — `crypto.PublicKey | []uint8`.
        rewriteRun(
                go(
                        """
                                package main

                                import "crypto"

                                type VerificationKey interface {
                                \tcrypto.PublicKey | []uint8
                                }
                                """
                )
        );
    }

    @Test
    void typeSetUnionWithCompositeOperands() {
        rewriteRun(
                go(
                        """
                                package main

                                type ByteSeq interface {
                                \t~[]byte | ~string
                                }

                                type IntPtr interface {
                                \t*int | *int64
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
