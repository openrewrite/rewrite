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

func TestParseMethodOnStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Counter struct {
				count int
			}

			func (c *Counter) Increment() {
				c.count++
			}
		`))
}

func TestParseMethodOnValue(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Point struct {
				X int
				Y int
			}

			func (p Point) String() string {
				return "point"
			}
		`))
}

func TestParseMultipleMethodsOnStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Stack struct {
				items []int
			}

			func (s *Stack) Push(x int) {
				s.items = append(s.items, x)
			}

			func (s *Stack) Pop() int {
				n := len(s.items) - 1
				x := s.items[n]
				s.items = s.items[:n]
				return x
			}
		`))
}

func TestParseElseBlock(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func abs(x int) int {
				if x < 0 {
					return -x
				} else {
					return x
				}
			}
		`))
}

func TestParseNestedIf(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x, y int) {
				if x > 0 {
					if y > 0 {
						use(x, y)
					}
				}
			}
		`))
}

func TestParseForThreeClause(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i := 0; i < 10; i++ {
					use(i)
				}
			}
		`))
}

func TestParseForConditionOnly(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0
				for x < 10 {
					x++
				}
			}
		`))
}

func TestParseForInfinite(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for {
					break
				}
			}
		`))
}

func TestParseMultipleImports(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				"os"
				"strings"
			)

			func f() {
				fmt.Println(os.Args)
				strings.Join(nil, "")
			}
		`))
}

func TestParsePackageLevelFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func init() {
			}

			func main() {
			}
		`))
}

func TestParseGoRoutineWithClosure(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 42
				go func() {
					use(x)
				}()
			}
		`))
}

func TestParseDeferWithClosure(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				defer func() {
					recover()
				}()
			}
		`))
}

func TestParseAppendBuiltin(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := []int{1, 2}
				s = append(s, 3, 4)
				use(s)
			}
		`))
}

func TestParseLenCap(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := make([]int, 5, 10)
				l := len(s)
				c := cap(s)
				use(l, c)
			}
		`))
}

func TestParseDeleteFromMap(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]int{"a": 1}
				delete(m, "a")
			}
		`))
}

func TestParseMapLookupWithOk(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]int{"a": 1}
				v, ok := m["a"]
				use(v, ok)
			}
		`))
}

func TestParseSwitchWithBlock(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) {
				switch x {
				case 1:
					a := 1
					use(a)
				case 2:
					b := 2
					use(b)
				default:
					use(x)
				}
			}
		`))
}

func TestParseNewBuiltin(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				p := new(int)
				use(p)
			}
		`))
}

func TestParsePanicRecover(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				defer func() {
					if r := recover(); r != nil {
						use(r)
					}
				}()
				panic("oops")
			}
		`))
}

func TestParseMultiLineComposite(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []struct {
					Name  string
					Value int
				}{
					{Name: "a", Value: 1},
					{Name: "b", Value: 2},
				}
				use(items)
			}
		`))
}

func TestParseFuncTypeParam(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func apply(fn func(int) int, x int) int {
				return fn(x)
			}
		`))
}

func TestParseFuncTypeReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func adder(x int) func(int) int {
				return func(y int) int {
					return x + y
				}
			}
		`))
}

func TestParseConstBlock(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				A = 1
				B = 2
				C = 3
			)
		`))
}

func TestParseMultiVarDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				var x, y int
				use(x, y)
			}
		`))
}

func TestParseBlankIdentifier(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				_, err := doSomething()
				use(err)
			}
		`))
}
