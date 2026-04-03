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

import static org.openrewrite.golang.Assertions.go;

/**
 * Comprehensive coverage test for Go parser → Java receiver round-trip.
 * Each test exercises a specific Go syntax pattern to identify which
 * patterns the Java receiver can't handle.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangParserCoverageTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
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
                .identifiers(false)
                .methodInvocations(false)
                .build());
    }

    // === BASICS ===

    @Test
    void shortVarDecl() {
        rewriteRun(go("""
                package main

                func f() {
                \tx := 42
                \t_ = x
                }
                """));
    }

    @Test
    void multipleReturnValues() {
        rewriteRun(go("""
                package main

                func divide(a, b int) (int, int) {
                \treturn a / b, a % b
                }
                """));
    }

    @Test
    void namedReturnValues() {
        rewriteRun(go("""
                package main

                func divide(a, b int) (quotient int, remainder int) {
                \tquotient = a / b
                \tremainder = a % b
                \treturn
                }
                """));
    }

    @Test
    void blankIdentifier() {
        rewriteRun(go("""
                package main

                func f() {
                \t_, err := divide(10, 3)
                \t_ = err
                }

                func divide(a, b int) (int, error) {
                \treturn a / b, nil
                }
                """));
    }

    // === CONTROL FLOW ===

    @Test
    void ifWithInit() {
        rewriteRun(go("""
                package main

                func f() string {
                \tif x := 10; x > 5 {
                \t\treturn "big"
                \t}
                \treturn "small"
                }
                """));
    }

    @Test
    void switchWithValue() {
        rewriteRun(go("""
                package main

                func f(x int) string {
                \tswitch x {
                \tcase 1:
                \t\treturn "one"
                \tcase 2:
                \t\treturn "two"
                \tdefault:
                \t\treturn "other"
                \t}
                }
                """));
    }

    @Test
    void forRangeMap() {
        rewriteRun(go("""
                package main

                func f() {
                \tm := map[string]int{"a": 1}
                \tfor k, v := range m {
                \t\t_ = k
                \t\t_ = v
                \t}
                }
                """));
    }

    @Test
    void forRangeString() {
        rewriteRun(go("""
                package main

                func f() {
                \tfor i, ch := range "hello" {
                \t\t_ = i
                \t\t_ = ch
                \t}
                }
                """));
    }

    @Test
    void infiniteForLoop() {
        rewriteRun(go("""
                package main

                func f() {
                \tfor {
                \t\tbreak
                \t}
                }
                """));
    }

    @Test
    void labeledBreakContinue() {
        rewriteRun(go("""
                package main

                func f() {
                outer:
                \tfor i := 0; i < 10; i++ {
                \t\tfor j := 0; j < 10; j++ {
                \t\t\tif j == 5 {
                \t\t\t\tcontinue outer
                \t\t\t}
                \t\t\tif i == 5 {
                \t\t\t\tbreak outer
                \t\t\t}
                \t\t}
                \t}
                }
                """));
    }

    // === FUNCTIONS ===

    @Test
    void variadicFunction() {
        rewriteRun(go("""
                package main

                func sum(nums ...int) int {
                \ts := 0
                \tfor _, n := range nums {
                \t\ts += n
                \t}
                \treturn s
                }
                """));
    }

    @Test
    void closureCapture() {
        rewriteRun(go("""
                package main

                func counter() func() int {
                \tn := 0
                \treturn func() int {
                \t\tn++
                \t\treturn n
                \t}
                }
                """));
    }

    @Test
    void methodWithPointerReceiver() {
        rewriteRun(go("""
                package main

                type Counter struct {
                \tn int
                }

                func (c *Counter) Inc() {
                \tc.n++
                }
                """));
    }

    @Test
    void methodWithValueReceiver() {
        rewriteRun(go("""
                package main

                type Point struct {
                \tX int
                \tY int
                }

                func (p Point) Sum() int {
                \treturn p.X + p.Y
                }
                """));
    }

    // === TYPES ===

    @Test
    void typeAssertion() {
        rewriteRun(go("""
                package main

                func f(x any) string {
                \ts, ok := x.(string)
                \tif ok {
                \t\treturn s
                \t}
                \treturn ""
                }
                """));
    }

    @Test
    void typeSwitch() {
        rewriteRun(go("""
                package main

                func f(x any) string {
                \tswitch v := x.(type) {
                \tcase string:
                \t\treturn v
                \tcase int:
                \t\treturn "int"
                \tdefault:
                \t\treturn "unknown"
                \t}
                }
                """));
    }

    @Test
    void embeddedStruct() {
        rewriteRun(go("""
                package main

                type Base struct {
                \tID int
                }

                type Extended struct {
                \tBase
                \tName string
                }
                """));
    }

    @Test
    void interfaceWithEmbedding() {
        rewriteRun(go("""
                package main

                type Reader interface {
                \tRead(p []byte) (n int, err error)
                }

                type ReadWriter interface {
                \tReader
                \tWrite(p []byte) (n int, err error)
                }
                """));
    }

    @Test
    void typeAlias() {
        rewriteRun(go("""
                package main

                type MyInt = int
                type MyString string
                """));
    }

    // === CHANNELS AND GOROUTINES ===

    @Test
    void channelSendReceive() {
        rewriteRun(go("""
                package main

                func f() {
                \tch := make(chan int)
                \tch <- 42
                \tx := <-ch
                \t_ = x
                }
                """));
    }

    @Test
    void selectStatement() {
        rewriteRun(go("""
                package main

                func f(ch1 chan int, ch2 chan string) {
                \tselect {
                \tcase v := <-ch1:
                \t\t_ = v
                \tcase s := <-ch2:
                \t\t_ = s
                \tdefault:
                \t}
                }
                """));
    }

    @Test
    void deferSimple() {
        rewriteRun(go("""
                package main

                import "fmt"

                func f() {
                \tdefer fmt.Println("done")
                }
                """));
    }

    @Test
    void goRoutineSimple() {
        rewriteRun(go("""
                package main

                import "fmt"

                func f() {
                \tgo fmt.Println("async")
                }
                """));
    }

    @Test
    void goRoutineFuncLiteral() {
        rewriteRun(go("""
                package main

                import "fmt"

                func f() {
                \tgo func() {
                \t\tfmt.Println("hello from goroutine")
                \t}()
                }
                """));
    }

    // === COMPOSITE LITERALS ===

    @Test
    void sliceLiteral() {
        rewriteRun(go("""
                package main

                func f() {
                \ts := []int{1, 2, 3}
                \t_ = s
                }
                """));
    }

    @Test
    void mapLiteral() {
        rewriteRun(go("""
                package main

                func f() {
                \tm := map[string]int{
                \t\t"a": 1,
                \t\t"b": 2,
                \t}
                \t_ = m
                }
                """));
    }

    @Test
    void structLiteral() {
        rewriteRun(go("""
                package main

                type Point struct {
                \tX int
                \tY int
                }

                func f() {
                \tp := Point{X: 1, Y: 2}
                \t_ = p
                }
                """));
    }

    @Test
    void nestedCompositeLiteral() {
        rewriteRun(go("""
                package main

                type Point struct {
                \tX int
                \tY int
                }

                func f() {
                \tpoints := []Point{
                \t\t{X: 1, Y: 2},
                \t\t{X: 3, Y: 4},
                \t}
                \t_ = points
                }
                """));
    }

    // === EXPRESSIONS ===

    @Test
    void sliceExpression() {
        rewriteRun(go("""
                package main

                func f() {
                \ts := []int{1, 2, 3, 4, 5}
                \t_ = s[1:3]
                }
                """));
    }

    @Test
    void indexExpression() {
        rewriteRun(go("""
                package main

                func f() {
                \ts := []int{1, 2, 3}
                \t_ = s[0]
                }
                """));
    }

    @Test
    void addressAndDeref() {
        rewriteRun(go("""
                package main

                func f() {
                \tx := 42
                \tp := &x
                \t_ = *p
                }
                """));
    }

    @Test
    void multipleAssignment() {
        rewriteRun(go("""
                package main

                func f() {
                \tx, y := 1, 2
                \tx, y = y, x
                \t_ = x
                \t_ = y
                }
                """));
    }

    @Test
    void compoundAssignment() {
        rewriteRun(go("""
                package main

                func f() {
                \tx := 10
                \tx += 5
                \tx -= 3
                \tx *= 2
                \t_ = x
                }
                """));
    }

    @Test
    void methodChain() {
        rewriteRun(go("""
                package main

                import "strings"

                func f() string {
                \treturn strings.ToUpper(strings.TrimSpace(" hello "))
                }
                """));
    }

    // === ERROR HANDLING ===

    @Test
    void errorReturn() {
        rewriteRun(go("""
                package main

                import "errors"

                func f() error {
                \treturn errors.New("something failed")
                }
                """));
    }

    @Test
    void errorCheck() {
        rewriteRun(go("""
                package main

                import "os"

                func f() error {
                \t_, err := os.Open("file.txt")
                \tif err != nil {
                \t\treturn err
                \t}
                \treturn nil
                }
                """));
    }

    // === CONSTANTS AND IOTA ===

    @Test
    void constBlock() {
        rewriteRun(go("""
                package main

                const (
                \tA = 1
                \tB = 2
                \tC = 3
                )
                """));
    }

    @Test
    void constIota() {
        rewriteRun(go("""
                package main

                type Color int

                const (
                \tRed Color = iota
                \tGreen
                \tBlue
                )
                """));
    }

    // === ADVANCED PATTERNS ===

    @Test
    void structTag() {
        rewriteRun(go("""
                package main

                type User struct {
                \tName  string `json:"name"`
                \tEmail string `json:"email,omitempty"`
                }
                """));
    }

    @Test
    void gotoStatement() {
        rewriteRun(go("""
                package main

                func f() {
                \tgoto done
                done:
                }
                """));
    }

    @Test
    void deferWithClosure() {
        rewriteRun(go("""
                package main

                import "fmt"

                func f() {
                \tfor i := 0; i < 3; i++ {
                \t\tdefer func(n int) {
                \t\t\tfmt.Println(n)
                \t\t}(i)
                \t}
                }
                """));
    }

    @Test
    void panicRecover() {
        rewriteRun(go("""
                package main

                import "fmt"

                func f() {
                \tdefer func() {
                \t\tif r := recover(); r != nil {
                \t\t\tfmt.Println("recovered:", r)
                \t\t}
                \t}()
                \tpanic("oops")
                }
                """));
    }

    @Test
    void multiValueMapLookup() {
        rewriteRun(go("""
                package main

                func f() {
                \tm := map[string]int{"a": 1}
                \tv, ok := m["a"]
                \t_ = v
                \t_ = ok
                }
                """));
    }

    @Test
    void stringConcatenation() {
        rewriteRun(go("""
                package main

                func f() string {
                \ta := "hello"
                \tb := "world"
                \treturn a + " " + b
                }
                """));
    }

    @Test
    void arrayType() {
        rewriteRun(go("""
                package main

                func f() {
                \tvar a [3]int
                \ta[0] = 1
                \t_ = a
                }
                """));
    }

    @Test
    void pointerType() {
        rewriteRun(go("""
                package main

                func f() {
                \tvar p *int
                \tx := 42
                \tp = &x
                \t_ = p
                }
                """));
    }

    @Test
    void channelType() {
        rewriteRun(go("""
                package main

                func producer() <-chan int {
                \tch := make(chan int)
                \tgo func() {
                \t\tch <- 42
                \t}()
                \treturn ch
                }
                """));
    }

    @Test
    void funcType() {
        rewriteRun(go("""
                package main

                type Handler func(string) error

                func f(h Handler) error {
                \treturn h("test")
                }
                """));
    }

    @Test
    void emptyInterface() {
        rewriteRun(go("""
                package main

                func f(x interface{}) string {
                \tif s, ok := x.(string); ok {
                \t\treturn s
                \t}
                \treturn ""
                }
                """));
    }

    @Test
    void rawStringLiteral() {
        rewriteRun(go("""
                package main

                func f() string {
                \treturn `hello
                world`
                }
                """));
    }

    @Test
    void multiReturnAssignment() {
        rewriteRun(go("""
                package main

                import "fmt"

                func f() {
                \tn, err := fmt.Println("hello")
                \t_ = n
                \t_ = err
                }
                """));
    }

    @Test
    void varBlock() {
        rewriteRun(go("""
                package main

                var (
                \tx int
                \ty string = "hello"
                )
                """));
    }

    @Test
    void initFunction() {
        rewriteRun(go("""
                package main

                var ready bool

                func init() {
                \tready = true
                }
                """));
    }
}
