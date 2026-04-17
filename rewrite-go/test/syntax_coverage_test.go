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

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

// === GENERICS (Go 1.18+) ===

func TestParseGenericMultiTypeParamInstantiation(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				p := Pair[int, string]{First: 1, Second: "a"}
				use(p)
			}
		`))
}

func TestParseGenericFuncMultiTypeParamCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				result := Map[int, string](items, convert)
				use(result)
			}
		`))
}

func TestParseGenericComparableConstraint(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func Contains[T comparable](s []T, v T) bool {
				for _, x := range s {
					if x == v {
						return true
					}
				}
				return false
			}
		`))
}

func TestParseGenericStructWithMultipleTypeParams(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Result[T any, E error] struct {
				Value T
				Err   E
			}
		`))
}

func TestParseGenericInterfaceConstraint(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Ordered interface {
				~int | ~int8 | ~int16 | ~int32 | ~int64 |
					~uint | ~uint8 | ~uint16 | ~uint32 | ~uint64 |
					~float32 | ~float64 |
					~string
			}
		`))
}

func TestParseGenericTypeParamInMethod(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Set[T comparable] struct {
				items map[T]struct{}
			}

			func (s *Set[T]) Add(item T) {
				s.items[item] = struct{}{}
			}

			func (s *Set[T]) Contains(item T) bool {
				_, ok := s.items[item]
				return ok
			}
		`))
}

// === LITERALS ===

func TestParseRuneLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				c := 'a'
				use(c)
			}
		`))
}

func TestParseRuneEscapes(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				tab := '\t'
				nl := '\n'
				bs := '\\'
				sq := '\''
				use(tab, nl, bs, sq)
			}
		`))
}

func TestParseHexLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0xFF
				y := 0xDEAD_BEEF
				use(x, y)
			}
		`))
}

func TestParseOctalLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0o755
				y := 0644
				use(x, y)
			}
		`))
}

func TestParseBinaryLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0b1010_1100
				use(x)
			}
		`))
}

func TestParseFloatLiterals(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				a := 3.14
				b := .5
				c := 1.
				d := 1e10
				e := 1.5e-3
				use(a, b, c, d, e)
			}
		`))
}

func TestParseImaginaryLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				c := 1 + 2i
				use(c)
			}
		`))
}

func TestParseStringEscapes(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := "hello\tworld\n"
				q := "say \"hi\""
				h := "\x41\x42"
				u := "\u0041"
				use(s, q, h, u)
			}
		`))
}

func TestParseUnderscoredNumber(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 1_000_000
				y := 3.141_592_653
				use(x, y)
			}
		`))
}

// === TYPE DECLARATIONS ===

func TestParseFuncTypeDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Handler func(string) error
		`))
}

func TestParseFuncTypeDeclMultiParam(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Reducer func(acc int, val int) int
		`))
}

func TestParseFuncTypeDeclMultiReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Parser func(string) (int, error)
		`))
}

func TestParseEmptyStructType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]struct{}{}
				use(m)
			}
		`))
}

func TestParseNestedStructDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Outer struct {
				Inner struct {
					Value int
				}
			}
		`))
}

func TestParseEmbeddedPointerType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Child struct {
				*Parent
				Name string
			}
		`))
}

func TestParseStructMultipleEmbedded(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type MyType struct {
				Reader
				Writer
				Name string
			}
		`))
}

func TestParseTypeConstraintMultiLine(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Number interface {
				int | int8 | int16 | int32 | int64
				float32 | float64
			}
		`))
}

// === EXPRESSIONS ===

func TestParsePrecedenceComplex(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() int {
				return a + b*c - d/e%f
			}
		`))
}

func TestParseNestedParentheses(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() int {
				return ((a + b) * (c - d))
			}
		`))
}

func TestParseComparisonChain(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(a, b, c int) bool {
				return a < b && b < c
			}
		`))
}

func TestParseTernaryLikePattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func max(a, b int) int {
				if a > b {
					return a
				}
				return b
			}
		`))
}

func TestParsePointerToPointer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(pp **int) int {
				return **pp
			}
		`))
}

func TestParseArrayOfArrays(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				var matrix [3][4]int
				use(matrix)
			}
		`))
}

func TestParseMapWithStructKey(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Point struct {
				X, Y int
			}

			func f() {
				m := map[Point]string{
					{X: 1, Y: 2}: "a",
				}
				use(m)
			}
		`))
}

func TestParseMultilineCallArgs(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				result := longFunctionName(
					arg1,
					arg2,
					arg3,
				)
				use(result)
			}
		`))
}

func TestParseChainedMethodCalls(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				result := builder.
					SetName("test").
					SetValue(42).
					Build()
				use(result)
			}
		`))
}

// === STATEMENTS ===

func TestParseNestedSwitch(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x, y int) {
				switch x {
				case 1:
					switch y {
					case 2:
						use(x, y)
					}
				}
			}
		`))
}

func TestParseNestedFor(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i := 0; i < 3; i++ {
					for j := 0; j < 3; j++ {
						use(i, j)
					}
				}
			}
		`))
}

func TestParseContinueWithLabel(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
			loop:
				for i := 0; i < 10; i++ {
					if i%2 == 0 {
						continue loop
					}
					use(i)
				}
			}
		`))
}

func TestParseNestedSelect(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for {
					select {
					case v := <-ch1:
						use(v)
					case v := <-ch2:
						use(v)
					}
				}
			}
		`))
}

func TestParseEmptyFor(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for {
				}
			}
		`))
}

func TestParseLabeledFor(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
			outer:
				for i := 0; i < 10; i++ {
					for j := 0; j < 10; j++ {
						if i*j > 50 {
							break outer
						}
					}
				}
			}
		`))
}

// === DECLARATIONS ===

func TestParseMultipleConstBlocks(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				A = iota
				B
				C
			)

			const (
				X = "x"
				Y = "y"
			)
		`))
}

func TestParseConstIotaWithExpr(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				_  = iota
				KB = 1 << (10 * iota)
				MB
				GB
				TB
			)
		`))
}

func TestParseVarTopLevelSlice(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var defaults = []string{
				"alpha",
				"beta",
				"gamma",
			}
		`))
}

func TestParseVarTopLevelMap(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var registry = map[string]func(){
				"init": func() {},
				"run":  func() {},
			}
		`))
}

func TestParseTypeDeclInsideFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				type local struct {
					x int
				}
				v := local{x: 1}
				use(v)
			}
		`))
}

// === COMMENTS ===

func TestParseCommentInsideFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				// step 1
				a := 1
				// step 2
				b := 2
				use(a, b)
			}
		`))
}

func TestParseCommentAfterImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "fmt" // for printing

			func f() {
				fmt.Println("hi")
			}
		`))
}

func TestParseCommentOnStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			// Point represents a 2D point.
			type Point struct {
				X int // x coordinate
				Y int // y coordinate
			}
		`))
}

func TestParseCommentOnConst(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				// MaxRetries is the maximum number of retries.
				MaxRetries = 3
				// DefaultTimeout in seconds.
				DefaultTimeout = 30
			)
		`))
}

func TestParseCommentInSwitch(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch x {
				// positive cases
				case 1:
					one()
				case 2:
					two()
				// negative
				default:
					other()
				}
			}
		`))
}

func TestParseCommentInIf(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				if x > 0 {
					// positive
					pos()
				} else {
					// non-positive
					neg()
				}
			}
		`))
}

// === WHITESPACE AND FORMATTING ===

func TestParseExtraBlankLines(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				a := 1

				b := 2

				use(a, b)
			}
		`))
}

func TestParseTabIndentation(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		GolangRaw("package main\n\nfunc f() {\n\tx := 1\n\tuse(x)\n}\n"))
}

func TestParseNoTrailingNewline(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		GolangRaw("package main\n\nfunc f() {\n}"))
}

func TestParseMultipleBlankLinesBetweenFuncs(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func a() {
			}


			func b() {
			}
		`))
}

// === COMPLEX PATTERNS ===

func TestParseBuilderPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Builder struct {
				name  string
				value int
			}

			func NewBuilder() *Builder {
				return &Builder{}
			}

			func (b *Builder) SetName(name string) *Builder {
				b.name = name
				return b
			}

			func (b *Builder) SetValue(value int) *Builder {
				b.value = value
				return b
			}
		`))
}

func TestParseErrorWrapping(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func f() error {
				err := doWork()
				if err != nil {
					return fmt.Errorf("failed to do work: %w", err)
				}
				return nil
			}
		`))
}

func TestParseTableDrivenTestPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "testing"

			func TestAdd(t *testing.T) {
				tests := []struct {
					name string
					a, b int
					want int
				}{
					{"positive", 1, 2, 3},
					{"negative", -1, -2, -3},
					{"zero", 0, 0, 0},
				}
				for _, tt := range tests {
					t.Run(tt.name, func(t *testing.T) {
						got := add(tt.a, tt.b)
						if got != tt.want {
							t.Errorf("add(%d, %d) = %d, want %d", tt.a, tt.b, got, tt.want)
						}
					})
				}
			}
		`))
}

func TestParseFunctionalOptions(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Server struct {
				host    string
				port    int
				timeout int
			}

			type Option func(*Server)

			func WithHost(host string) Option {
				return func(s *Server) {
					s.host = host
				}
			}

			func WithPort(port int) Option {
				return func(s *Server) {
					s.port = port
				}
			}

			func NewServer(opts ...Option) *Server {
				s := &Server{host: "localhost", port: 8080}
				for _, opt := range opts {
					opt(s)
				}
				return s
			}
		`))
}

func TestParseConcurrentMapAccess(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "sync"

			type SafeMap struct {
				mu sync.RWMutex
				m  map[string]int
			}

			func (sm *SafeMap) Get(key string) (int, bool) {
				sm.mu.RLock()
				defer sm.mu.RUnlock()
				v, ok := sm.m[key]
				return v, ok
			}

			func (sm *SafeMap) Set(key string, value int) {
				sm.mu.Lock()
				defer sm.mu.Unlock()
				sm.m[key] = value
			}
		`))
}

func TestParseContextPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "context"

			func doWork(ctx context.Context) error {
				select {
				case <-ctx.Done():
					return ctx.Err()
				case result := <-work():
					use(result)
					return nil
				}
			}
		`))
}

func TestParseIteratorPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Iterator struct {
				items []int
				pos   int
			}

			func (it *Iterator) Next() (int, bool) {
				if it.pos >= len(it.items) {
					return 0, false
				}
				v := it.items[it.pos]
				it.pos++
				return v, true
			}

			func (it *Iterator) Reset() {
				it.pos = 0
			}
		`))
}

// === EDGE CASES ===

func TestParseEmptyFile(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main
		`))
}

func TestParseOnlyImports(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "fmt"
		`))
}

func TestParseUnicodeIdentifier(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func 日本語() string {
				return "hello"
			}
		`))
}

func TestParseReturnFuncLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() func() {
				return func() {
				}
			}
		`))
}

func TestParseAssignToIndex(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := make(map[string]int)
				m["key"] = 42
			}
		`))
}

func TestParseAssignToField(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s.Field = 42
			}
		`))
}

func TestParseSliceOfInterface(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(items []interface{}) {
				for _, item := range items {
					use(item)
				}
			}
		`))
}

func TestParseNestedFuncTypes(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(fn func(func(int) int) func(int) int) {
			}
		`))
}

func TestParseMultipleReceiverMethods(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type T struct{}

			func (t T) A() {}
			func (t T) B() {}
			func (t *T) C() {}
		`))
}

func TestParseEmptyCase(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch x {
				case 1:
				case 2:
				default:
				}
			}
		`))
}

func TestParseEmptyReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				return
			}
		`))
}

func TestParseNilMapAccess(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				var m map[string]int
				_ = m["key"]
			}
		`))
}

func TestParseChannelOfStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Event struct {
				Name string
			}

			func f(ch chan Event) {
				ch <- Event{Name: "test"}
			}
		`))
}

func TestParseForPostIncrement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i := 0; i < 10; i += 2 {
					use(i)
				}
			}
		`))
}

func TestParseSingleLineIf(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) int {
				if x > 0 { return x }
				return -x
			}
		`))
}

func TestParseVarInsideIf(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				if v, ok := m["key"]; ok {
					use(v)
				}
			}
		`))
}

func TestParseComplexMapType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() map[string]map[string][]int {
				return nil
			}
		`))
}

func TestParseFuncReturningError(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func open(path string) (*File, error) {
				f, err := os.Open(path)
				if err != nil {
					return nil, err
				}
				return f, nil
			}
		`))
}

func TestParseCopyBuiltin(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				src := []int{1, 2, 3}
				dst := make([]int, len(src))
				copy(dst, src)
				use(dst)
			}
		`))
}

func TestParseCloseBuiltin(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				ch := make(chan int)
				close(ch)
			}
		`))
}

func TestParseSprintfPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func f() string {
				return fmt.Sprintf("name=%s, age=%d", name, age)
			}
		`))
}

func TestParseMethodOnImportedType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "strings"

			func f() string {
				return strings.NewReplacer("a", "b", "c", "d").Replace("abcd")
			}
		`))
}

func TestParseComplexCompositeLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				config := &Config{
					Server: ServerConfig{
						Host: "localhost",
						Port: 8080,
					},
					Database: DatabaseConfig{
						Host: "db.local",
						Port: 5432,
					},
				}
				use(config)
			}
		`))
}

func TestParseInterfaceWithUnionConstraint(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Stringable interface {
				~string | ~[]byte
				String() string
			}
		`))
}

func TestParseSwitchNoBody(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch x {
				}
			}
		`))
}

func TestParseForRangeNoVars(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for range items {
					doSomething()
				}
			}
		`))
}

func TestParseForNoInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 100
				for ; x > 0; x >>= 1 {
					use(x)
				}
			}
		`))
}

func TestParseForNoInitNoPost(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0
				for ; x < 10; {
					x++
				}
			}
		`))
}

func TestParseMultiLineBinaryExpr(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() bool {
				return a > 0 &&
					b > 0 &&
					c > 0
			}
		`))
}
