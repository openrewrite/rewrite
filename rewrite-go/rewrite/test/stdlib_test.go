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

func TestParseStringConcat(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() string {
				return "hello" + " " + "world"
			}
		`))
}

func TestParseMultiLineString(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() string {
				return "line1\n" +
					"line2\n" +
					"line3"
			}
		`))
}

func TestParseSliceAppendChain(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() []int {
				var s []int
				s = append(s, 1)
				s = append(s, 2, 3)
				return s
			}
		`))
}

func TestParseMapIteration(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]int{
					"a": 1,
					"b": 2,
					"c": 3,
				}
				for k, v := range m {
					use(k, v)
				}
			}
		`))
}

func TestParseEmbeddedStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Base struct {
				ID int
			}

			type Derived struct {
				Base
				Name string
			}
		`))
}

func TestParseMultipleInterfaceMethods(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Handler interface {
				Handle(req Request) Response
				Close() error
			}
		`))
}

func TestParseMethodChainOnReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() string {
				return builder.WriteString("hello").String()
			}
		`))
}

func TestParseNestedSliceIndex(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				matrix := [][]int{{1, 2}, {3, 4}}
				v := matrix[0][1]
				use(v)
			}
		`))
}

func TestParseIfElseIfElse(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func classify(x int) string {
				if x < 0 {
					return "negative"
				} else if x == 0 {
					return "zero"
				} else if x < 10 {
					return "small"
				} else if x < 100 {
					return "medium"
				} else {
					return "large"
				}
			}
		`))
}

func TestParseSwitchWithReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func dayType(d int) string {
				switch d {
				case 0, 6:
					return "weekend"
				case 1, 2, 3, 4, 5:
					return "weekday"
				default:
					return "invalid"
				}
			}
		`))
}

func TestParsePanicWithFormat(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func mustPositive(x int) int {
				if x <= 0 {
					panic(fmt.Sprintf("expected positive, got %d", x))
				}
				return x
			}
		`))
}

func TestParseClosureCapture(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func counter() func() int {
				n := 0
				return func() int {
					n++
					return n
				}
			}
		`))
}

func TestParseComplexForLoop(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func findFirst(items []int, target int) int {
				for i := 0; i < len(items); i++ {
					if items[i] == target {
						return i
					}
				}
				return -1
			}
		`))
}

func TestParseMultipleDefers(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				defer cleanup1()
				defer cleanup2()
				defer cleanup3()
				doWork()
			}
		`))
}

func TestParseForRangeChannel(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func drain(ch <-chan int) {
				for v := range ch {
					use(v)
				}
			}
		`))
}
