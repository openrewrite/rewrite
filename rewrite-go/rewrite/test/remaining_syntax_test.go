/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test

import (
	"testing"

	. "github.com/openrewrite/rewrite/pkg/test"
)

// === FOR RANGE INTEGER (Go 1.22+) ===

func TestParseForRangeInteger(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i := range 10 {
					use(i)
				}
			}
		`))
}

func TestParseForRangeIntegerNoVar(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for range 10 {
					doSomething()
				}
			}
		`))
}

// === NESTED TYPE ASSERTIONS ===

func TestParseChainedTypeAssertion(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x interface{}) string {
				return x.(interface{}).(string)
			}
		`))
}

// === MULTIPLE BLANK IMPORTS ===

func TestParseMultipleBlankImports(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import (
				_ "net/http/pprof"
				_ "image/png"
				_ "image/jpeg"
			)
		`))
}

// === IMPORT WITH COMMENTS BETWEEN GROUPS ===

func TestParseImportWithGroupComments(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import (
				// standard library
				"fmt"
				"os"

				// third party
				"github.com/pkg/errors"
			)

			func f() {
				fmt.Println(os.Args)
				errors.New("x")
			}
		`))
}

// === NESTED CLOSURES 3+ LEVELS ===

func TestParseTripleNestedClosure(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				outer := func() {
					middle := func() {
						inner := func() {
							doWork()
						}
						inner()
					}
					middle()
				}
				outer()
			}
		`))
}

func TestParseQuadNestedClosure(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() func() func() func() int {
				return func() func() func() int {
					return func() func() int {
						return func() int {
							return 42
						}
					}
				}
			}
		`))
}

// === UNSAFE OPERATIONS ===

func TestParseUnsafeSizeof(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "unsafe"

			func f() {
				size := unsafe.Sizeof(int(0))
				use(size)
			}
		`))
}

func TestParseUnsafePointer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "unsafe"

			func f(p *int) {
				ptr := unsafe.Pointer(p)
				use(ptr)
			}
		`))
}

// === PACKAGE LEVEL COMPLEX INIT ===

func TestParsePackageLevelComplexVar(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var (
				handlers = map[string]func() error{
					"start": func() error { return nil },
					"stop":  func() error { return nil },
				}
				defaults = struct {
					Host string
					Port int
				}{
					Host: "localhost",
					Port: 8080,
				}
			)
		`))
}

// === METHOD EXPRESSIONS vs METHOD VALUES ===

func TestParseMethodExpression(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type T struct{}

			func (T) Hello() string { return "hi" }

			func f() {
				fn := T.Hello
				use(fn)
			}
		`))
}

func TestParseMethodExpressionPointer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type T struct{ val int }

			func (*T) Get() int { return 0 }

			func f() {
				fn := (*T).Get
				use(fn)
			}
		`))
}

// === ANY KEYWORD ===

func TestParseAnyType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x any) any {
				return x
			}
		`))
}

func TestParseSliceOfAny(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(items []any) {
				for _, item := range items {
					use(item)
				}
			}
		`))
}

func TestParseMapOfAny(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() map[string]any {
				return map[string]any{
					"name": "test",
					"age":  42,
				}
			}
		`))
}

// === COMPLEX CONSTANT EXPRESSIONS ===

func TestParseConstBitShiftIota(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Flags uint

			const (
				FlagRead Flags = 1 << iota
				FlagWrite
				FlagExec
				FlagAll = FlagRead | FlagWrite | FlagExec
			)
		`))
}

func TestParseConstIotaArithmetic(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				Sunday    = iota
				Monday
				Tuesday
				Wednesday
				Thursday
				Friday
				Saturday
				NumDays = iota
			)
		`))
}

// === EDGE CASE EXPRESSIONS ===

func TestParseNegativeLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const minInt = -1 << 63
		`))
}

func TestParseAssignToDeref(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(p *int) {
				*p = 42
			}
		`))
}

func TestParseAssignToSliceIndex(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(s []int) {
				s[0] = 1
				s[len(s)-1] = 2
			}
		`))
}

func TestParseMultiReturnIgnoreSecond(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() int {
				v, _ := twoReturns()
				return v
			}
		`))
}

func TestParseMultiReturnIgnoreFirst(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() error {
				_, err := twoReturns()
				return err
			}
		`))
}

// === COMPLEX STRUCT PATTERNS ===

func TestParseRecursiveStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Node struct {
				Value    int
				Children []*Node
			}
		`))
}

func TestParseLinkedListStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type ListNode struct {
				Val  int
				Next *ListNode
			}
		`))
}

func TestParseStructWithFuncField(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Config struct {
				OnConnect    func(addr string) error
				OnDisconnect func(addr string)
				Transform    func([]byte) ([]byte, error)
			}
		`))
}

func TestParseStructWithChanField(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Worker struct {
				jobs    <-chan Job
				results chan<- Result
				done    chan struct{}
			}
		`))
}

// === COMPLEX INTERFACE PATTERNS ===

func TestParseInterfaceWithGenericMethod(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Stringer interface {
				String() string
			}

			type Container[T Stringer] interface {
				Get(key string) T
				Set(key string, value T)
				Delete(key string)
			}
		`))
}

func TestParseInterfaceEmbedMultiple(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Reader interface {
				Read(p []byte) (n int, err error)
			}

			type Writer interface {
				Write(p []byte) (n int, err error)
			}

			type Closer interface {
				Close() error
			}

			type ReadWriteCloser interface {
				Reader
				Writer
				Closer
			}
		`))
}

// === COMPLEX CONTROL FLOW ===

func TestParseSwitchFallthrough(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) string {
				switch {
				case x < 0:
					return "negative"
				case x == 0:
					fallthrough
				case x == 1:
					return "small"
				default:
					return "large"
				}
			}
		`))
}

func TestParseSelectNonBlocking(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func tryReceive(ch chan int) (int, bool) {
				select {
				case v := <-ch:
					return v, true
				default:
					return 0, false
				}
			}
		`))
}

func TestParseIfInitChained(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(m map[string]int) {
				if v, ok := m["a"]; ok {
					use(v)
				} else if v, ok := m["b"]; ok {
					use(v)
				} else {
					use(0)
				}
			}
		`))
}

func TestParseForRangeNestedMapSlice(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(m map[string][]int) {
				for key, values := range m {
					for i, v := range values {
						use(key, i, v)
					}
				}
			}
		`))
}

// === COMPLEX FUNCTION SIGNATURES ===

func TestParseFuncMultiReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func parse(input string) (result int, rest string, err error) {
				return 0, "", nil
			}
		`))
}

func TestParseFuncNoReturnNoParams(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func doWork() {
			}
		`))
}

func TestParseFuncSingleParam(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func identity(x int) int {
				return x
			}
		`))
}

func TestParseFuncParamNoNames(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Handler func(string, int) error
		`))
}

func TestParseFuncVariadicMiddle(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []int{1, 2, 3}
				all := append([]int{0}, items...)
				use(all)
			}
		`))
}

// === MULTILINE CONSTRUCTS ===

func TestParseMultilineReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() (int, string, error) {
				return 42,
					"hello",
					nil
			}
		`))
}

func TestParseMultilineCondition(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(a, b, c bool) {
				if a &&
					b &&
					c {
					doWork()
				}
			}
		`))
}

func TestParseMultilineFuncParams(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func longFunc(
				name string,
				age int,
				address string,
			) error {
				return nil
			}
		`))
}

func TestParseMultilineStructLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				cfg := Config{
					Host:     "localhost",
					Port:     8080,
					LogLevel: "debug",
					Timeout:  30,
					Retries:  3,
				}
				use(cfg)
			}
		`))
}

func TestParseMultilineMapLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]string{
					"name":    "test",
					"version": "1.0",
					"author":  "me",
				}
				use(m)
			}
		`))
}

// === ADDITIONAL EDGE CASES ===

func TestParseBlankAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				_ = compute()
			}
		`))
}

func TestParseMultiBlankAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				_, _, _ = a, b, c
			}
		`))
}

func TestParseTypeConversionComplex(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := string([]byte("hello"))
				b := []byte("world")
				r := []rune("日本語")
				use(s, b, r)
			}
		`))
}

func TestParseSendInGoroutine(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				ch := make(chan int, 10)
				for i := 0; i < 10; i++ {
					go func(n int) {
						ch <- n * n
					}(i)
				}
			}
		`))
}

func TestParseRecoverInDefer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func safeCall(fn func()) (err error) {
				defer func() {
					if r := recover(); r != nil {
						switch v := r.(type) {
						case error:
							err = v
						case string:
							err = errors.New(v)
						default:
							err = errors.New("unknown panic")
						}
					}
				}()
				fn()
				return nil
			}
		`))
}

func TestParseConstUntyped(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				pi    = 3.14159
				e     = 2.71828
				sqrt2 = 1.41421
			)
		`))
}

func TestParseVarWithoutInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var (
				globalCount int
				globalName  string
				globalDone  bool
			)
		`))
}

func TestParseEmbeddedAnonymousStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Container struct {
				sync struct {
					mu    int
					count int
				}
				items []int
			}
		`))
}

func TestParseInterfaceAny(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Cache interface {
				Get(key string) (any, bool)
				Set(key string, value any)
				Delete(key string)
				Clear()
			}
		`))
}

func TestParseEmptyMapLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]int{}
				use(m)
			}
		`))
}

func TestParseEmptyStructLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type S struct{}

			func f() {
				s := S{}
				use(s)
			}
		`))
}

func TestParseAssignmentToComplex(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := make(map[string]*Config)
				m["a"] = &Config{Name: "test"}
				m["a"].Name = "updated"
			}
		`))
}

func TestParseSwitchOnBool(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch {
				case x > 100:
					big()
				case x > 10:
					medium()
				case x > 0:
					small()
				default:
					zero()
				}
			}
		`))
}

func TestParseForRangeWithAssign(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				var i int
				var v string
				for i, v = range items {
					use(i, v)
				}
			}
		`))
}

func TestParseMultilineChainedCalls(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() error {
				return NewClient().
					WithTimeout(30).
					WithRetries(3).
					Connect("localhost:8080")
			}
		`))
}

func TestParseComplexSliceExpr(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := make([]int, 100)
				a := s[:10]
				b := s[10:20]
				c := s[20:]
				d := s[:]
				use(a, b, c, d)
			}
		`))
}

func TestParseChannelBufferedAndUnbuffered(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				unbuf := make(chan int)
				buf := make(chan int, 100)
				done := make(chan struct{})
				use(unbuf, buf, done)
			}
		`))
}

func TestParseMultipleInitFunctions(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func init() {
				setup1()
			}

			func init() {
				setup2()
			}

			func init() {
				setup3()
			}
		`))
}

func TestParseMultiLineRawString(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang("package main\n\nfunc f() string {\n\treturn `line1\nline2\nline3\n\ttabbed\n  spaced`\n}\n"))
}

func TestParseRawStringWithSpecialChars(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang("package main\n\nfunc f() string {\n\treturn `contains \"quotes\" and 'apostrophes' and \\backslashes\\`\n}\n"))
}

func TestParseNestedFuncLiterals(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				apply := func(fn func(int) int, x int) int {
					return fn(x)
				}
				result := apply(func(n int) int {
					return n * 2
				}, 5)
				use(result)
			}
		`))
}

func TestParseSelectWithAssign(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				ch1 := make(chan int)
				ch2 := make(chan string)
				select {
				case v, ok := <-ch1:
					use(v, ok)
				case msg := <-ch2:
					use(msg)
				}
			}
		`))
}

func TestParseMapWithFuncValues(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				handlers := map[string]func(int) error{
					"add": func(n int) error {
						return nil
					},
					"sub": func(n int) error {
						return nil
					},
				}
				use(handlers)
			}
		`))
}

func TestParseComplexConstExpressions(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				_  = iota
				KB = 1 << (10 * (iota + 1))
				MB
				GB
				TB
			)
		`))
}

func TestParseEmbeddedInterfaceConstraint(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Ordered interface {
				~int | ~int8 | ~int16 | ~int32 | ~int64 |
					~uint | ~uint8 | ~uint16 | ~uint32 | ~uint64 |
					~float32 | ~float64 |
					~string
			}

			func Max[T Ordered](a, b T) T {
				if a > b {
					return a
				}
				return b
			}
		`))
}

func TestParseMethodSetReceiver(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Counter struct {
				n int
			}

			func (c Counter) Value() int {
				return c.n
			}

			func (c *Counter) Increment() {
				c.n++
			}

			func (c *Counter) Add(delta int) {
				c.n += delta
			}
		`))
}

func TestParseComplexSliceExpressions(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := []int{1, 2, 3, 4, 5}
				a := s[1:3]
				b := s[:2]
				c := s[2:]
				d := s[:]
				e := s[1:3:4]
				use(a, b, c, d, e)
			}
		`))
}

func TestParseNestedTypeAssertions(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x interface{}) {
				if m, ok := x.(map[string]interface{}); ok {
					if v, ok := m["key"].([]interface{}); ok {
						use(v)
					}
				}
			}
		`))
}

func TestParseGoRoutineWithClosureLoop(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i := 0; i < 10; i++ {
					go func(n int) {
						process(n)
					}(i)
				}
			}
		`))
}

func TestParseDeferWithClosureCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				mu.Lock()
				defer func() {
					mu.Unlock()
					cleanup()
				}()
			}
		`))
}

func TestParseSwitchWithInitAndNoTag(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch n := compute(x); {
				case n < 0:
					handleNeg(n)
				case n == 0:
					handleZero()
				default:
					handlePos(n)
				}
			}
		`))
}

func TestParseVariadicWithSpread(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func sum(nums ...int) int {
				total := 0
				for _, n := range nums {
					total += n
				}
				return total
			}

			func f() {
				nums := []int{1, 2, 3}
				result := sum(nums...)
				use(result)
			}
		`))
}

func TestParseLabeledBreakContinue(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
			outer:
				for i := 0; i < 10; i++ {
					for j := 0; j < 10; j++ {
						if i+j > 15 {
							break outer
						}
						if j%2 == 0 {
							continue outer
						}
					}
				}
			}
		`))
}

func TestParseMultipleAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x, y := 1, 2
				x, y = y, x
				a, b, c := compute()
				use(a, b, c, x, y)
			}
		`))
}

func TestParseEmptySwitchTagless(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch {
				case x > 0:
					positive()
				case x < 0:
					negative()
				default:
					zero()
				}
			}
		`))
}
