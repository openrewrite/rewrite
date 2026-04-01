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

func TestParseMultipleTopLevelFunctions(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func add(a, b int) int {
				return a + b
			}

			func sub(a, b int) int {
				return a - b
			}
		`),
	)
}

func TestParseNestedBlocks(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				if true {
					if false {
						return
					}
				}
			}
		`),
	)
}

func TestParseIfWithInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				if x := compute(); x > 0 {
					use(x)
				}
			}
		`),
	)
}

func TestParseForRangeMap(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for k, v := range m {
					use(k, v)
				}
			}
		`),
	)
}

func TestParseMethodChaining(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x.Method1().Method2().Method3()
			}
		`),
	)
}

func TestParseFuncAsArgument(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				apply(func(x int) int {
					return x * 2
				})
			}
		`),
	)
}

func TestParseComplexStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Config struct {
				Host    string
				Port    int
				Options map[string]string
				Handler func(string) error
			}
		`),
	)
}

func TestParseSliceOfStructLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []Item{
					{Name: "a", Value: 1},
					{Name: "b", Value: 2},
				}
				use(items)
			}
		`),
	)
}

func TestParseMapLiteralInFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]int{
					"one": 1,
					"two": 2,
				}
				use(m)
			}
		`),
	)
}

func TestParseDeferClose(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				defer file.Close()
			}
		`),
	)
}

func TestParseGoRoutine(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				go func() {
					work()
				}()
			}
		`),
	)
}

func TestParseErrorHandlingPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() error {
				val, err := doSomething()
				if err != nil {
					return err
				}
				use(val)
				return nil
			}
		`),
	)
}

func TestParseVariadicFunction(t *testing.T) {
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
		`),
	)
}

func TestParseVariadicCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				nums := []int{1, 2, 3}
				sum(nums...)
			}
		`),
	)
}

func TestParseChannelOperations(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				ch := make(chan int)
				ch <- 42
				v := <-ch
				use(v)
			}
		`),
	)
}

func TestParseSwitchWithFallthrough(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch x {
				case 1:
					first()
					fallthrough
				case 2:
					second()
				}
			}
		`),
	)
}

func TestParseBreakContinueWithLabel(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
			outer:
				for i := 0; i < 10; i++ {
					for j := 0; j < 10; j++ {
						if j == 5 {
							continue outer
						}
						if i == 5 {
							break outer
						}
					}
				}
			}
		`),
	)
}

func TestParseBlankImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import _ "net/http/pprof"
		`),
	)
}

func TestParseDotImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import . "fmt"
		`),
	)
}

func TestParseAliasedImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import f "fmt"
		`),
	)
}

func TestParseMultipleReturnWithComma(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				return 1, nil
			}
		`),
	)
}

func TestParseNestedFuncType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func apply(f func(int) int, x int) int {
				return f(x)
			}
		`),
	)
}

func TestParseTypeSwitchWithAssign(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x interface{}) {
				switch v := x.(type) {
				case int:
					use(v)
				}
			}
		`),
	)
}

func TestParseCompleteHTTPHandler(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "net/http"

			type Handler struct {
				Name string
			}

			func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
				if r.Method != "GET" {
					w.WriteHeader(405)
					return
				}
				w.Write([]byte(h.Name))
			}
		`),
	)
}

func TestParseBareReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() (err error) {
				return
			}
		`),
	)
}

func TestParseNestedComposite(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string][]int{
					"a": {1, 2, 3},
					"b": {4, 5},
				}
				use(m)
			}
		`),
	)
}

func TestParseImmediatelyInvokedClosure(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				result := func(x int) int {
					return x * 2
				}(5)
				use(result)
			}
		`),
	)
}

func TestParseSelectEmptyDefault(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				select {
				default:
				}
			}
		`),
	)
}

func TestParseConstIota(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				A = iota
				B
				C
			)
		`),
	)
}
