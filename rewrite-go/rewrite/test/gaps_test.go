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

func TestParseThreeIndexSlice(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := make([]int, 10)
				t := s[1:3:5]
				use(t)
			}
		`))
}

func TestParseTypeAliasDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type MyInt = int
		`))
}

func TestParseEllipsisArrayLen(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				a := [...]int{1, 2, 3}
				use(a)
			}
		`))
}

func TestParseMultiValueShortVarWithFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				a, b, c := 1, "two", 3.0
				use(a, b, c)
			}
		`))
}

func TestParseNestedMapAccess(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := make(map[string]map[string]int)
				m["a"]["b"] = 1
			}
		`))
}

func TestParseSliceOfPointers(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(items []*Item) {
				for _, item := range items {
					use(item)
				}
			}
		`))
}

func TestParseFuncLiteralAsMapValue(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				handlers := map[string]func(){
					"a": func() {},
					"b": func() {},
				}
				use(handlers)
			}
		`))
}

func TestParseCompositeLiteralNoFieldNames(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				p := Point{1, 2}
				use(p)
			}
		`))
}

func TestParseChannelDirection(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func producer() <-chan int {
				ch := make(chan int)
				go func() {
					ch <- 42
					close(ch)
				}()
				return ch
			}
		`))
}

func TestParseTypeSwitchMultipleCases(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func describe(x interface{}) string {
				switch v := x.(type) {
				case int:
					return "int"
				case string:
					return v
				case bool:
					return "bool"
				default:
					return "unknown"
				}
			}
		`))
}

func TestParseEmptySwitch(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				switch {
				}
			}
		`))
}

func TestParseForRangeString(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i, ch := range "hello" {
					use(i, ch)
				}
			}
		`))
}

func TestParseChanOfChan(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(ch chan chan int) {
			}
		`))
}

func TestParseComplexConst(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				KB = 1024
				MB = 1024 * KB
				GB = 1024 * MB
			)
		`))
}

func TestParseVarBlock(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var (
				x int    = 1
				y string = "hello"
				z bool
			)
		`))
}

func TestParseMethodExprAsValue(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				fn := obj.Method
				fn()
			}
		`))
}

func TestParseIndexExprOnCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := getItems()[0]
				use(x)
			}
		`))
}

func TestParseSliceExprOnCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := getItems()[1:3]
				use(x)
			}
		`))
}

func TestParseBitClear(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) int {
				return x &^ 0xFF
			}
		`))
}

func TestParseBitClearAssign(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0xFF
				x &^= 0x0F
				use(x)
			}
		`))
}

func TestParseMultipleResults(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func swap(a, b int) (int, int) {
				return b, a
			}
		`))
}

func TestParseNestedFuncLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				outer := func() func() int {
					return func() int {
						return 42
					}
				}
				use(outer)
			}
		`))
}

func TestParseConstWithType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const maxSize int = 100
		`))
}

func TestParsePointerMethodReceiver(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type List struct {
				items []int
			}

			func (l *List) Add(item int) {
				l.items = append(l.items, item)
			}

			func (l *List) Len() int {
				return len(l.items)
			}

			func (l *List) Get(i int) int {
				return l.items[i]
			}
		`))
}
