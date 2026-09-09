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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestParseAddressOf(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 42
				p := &x
				use(p)
			}
		`))
}

func TestParseDereference(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(p *int) int {
				return *p
			}
		`))
}

func TestParseCompoundAssignPlus(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 1
				x += 2
				use(x)
			}
		`))
}

func TestParseCompoundAssignBitwise(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 0xFF
				x &= 0x0F
				x |= 0x10
				x ^= 0x01
				use(x)
			}
		`))
}

func TestParseUnaryNot(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(b bool) bool {
				return !b
			}
		`))
}

func TestParseUnaryNegate(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) int {
				return -x
			}
		`))
}

func TestParseMultipleAssignOps(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 10
				x -= 3
				x *= 2
				x /= 4
				x %= 3
				use(x)
			}
		`))
}

func TestParseTypeConversion(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 42
				y := float64(x)
				use(y)
			}
		`))
}

func TestParseMultiValueTypeAssertion(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x interface{}) {
				v, ok := x.(string)
				use(v, ok)
			}
		`))
}

func TestParseNilComparison(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(err error) bool {
				return err != nil
			}
		`))
}

func TestParseStructLiteralInReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Point struct {
				X int
				Y int
			}

			func origin() Point {
				return Point{X: 0, Y: 0}
			}
		`))
}

func TestParseElseIfChain(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) string {
				if x > 0 {
					return "positive"
				} else if x < 0 {
					return "negative"
				} else {
					return "zero"
				}
			}
		`))
}

func TestParseForRangeSlice(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []int{1, 2, 3}
				for i, v := range items {
					use(i, v)
				}
			}
		`))
}

func TestParseForRangeKeyOnly(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for i := range items {
					use(i)
				}
			}
		`))
}

func TestParseForRangeBlankKey(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				for _, v := range items {
					use(v)
				}
			}
		`))
}

func TestParseSelectWithChannelOps(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				select {
				case v := <-ch:
					use(v)
				case ch <- 42:
				default:
				}
			}
		`))
}

func TestParseCompositeStructPointer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				p := &Point{X: 1, Y: 2}
				use(p)
			}
		`))
}

func TestParseNestedFieldAccess(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := a.b.c
				use(x)
			}
		`))
}

func TestParseShiftOperators(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() int {
				return 1 << 10
			}
		`))
}

func TestParseRightShift(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) int {
				return x >> 2
			}
		`))
}

func TestParseLogicalOperators(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(a, b bool) bool {
				return a && b || !a
			}
		`))
}

func TestParseReceiveExpression(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				v := <-ch
				use(v)
			}
		`))
}

func TestParseShiftAssign(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x := 1
				x <<= 3
				x >>= 1
				use(x)
			}
		`))
}

func TestParseMakeCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				s := make([]int, 0, 10)
				m := make(map[string]int)
				ch := make(chan int, 5)
				use(s, m, ch)
			}
		`))
}

func TestParseMultipleReturnTypes(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() (int, error) {
				return 0, nil
			}
		`))
}

func TestParseClosureAssigned(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				add := func(a, b int) int {
					return a + b
				}
				use(add)
			}
		`))
}

func TestParseRawStringLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(""+
			"package main\n"+
			"\n"+
			"func f() string {\n"+
			"\treturn `hello\n"+
			"world`\n"+
			"}\n",
		))
}

func TestParseEmptyInterface(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x interface{}) {
				use(x)
			}
		`))
}

func TestParseBitwiseComplement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x int) int {
				return ^x
			}
		`))
}

func TestParseByteOrderMark(t *testing.T) {
	src := "\ufeffpackage main\n\nfunc f() {\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if !cu.CharsetBomMarked {
		t.Error("CharsetBomMarked should be set")
	}
	if cu.Prefix.Whitespace != "" {
		t.Errorf("Prefix should not hold the BOM, got %q", cu.Prefix.Whitespace)
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

func TestParseWithoutByteOrderMark(t *testing.T) {
	src := "package main\n\nfunc f() {\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if cu.CharsetBomMarked {
		t.Error("CharsetBomMarked should be unset")
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

// gofmt writes no space inside an empty argument list or composite
// literal, so hand-written spacing there is the only source of it.

func TestParseSpaceInsideEmptyCallArguments(t *testing.T) {
	src := "package main\n\nfunc g() int { return 0 }\n\nfunc f() {\n\tx := g( )\n\t_ = x\n}\n"
	assertRoundtrip(t, src)
}

func TestParseSpaceInsideEmptyMethodCallArguments(t *testing.T) {
	src := "package main\n\ntype T struct{}\n\nfunc (T) M() {}\n\nfunc f() {\n\tvar t T\n\tt.M(\n\t)\n}\n"
	assertRoundtrip(t, src)
}

func TestParseSpaceInsideEmptyCompositeLiteral(t *testing.T) {
	src := "package main\n\nfunc f() {\n\t_ = []int{ }\n\t_ = map[string]int{\n\t}\n\t_ = struct{}{ }\n}\n"
	assertRoundtrip(t, src)
}

func assertRoundtrip(t *testing.T, src string) {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

func TestParseParenthesizedCallStatement(t *testing.T) {
	src := "package main\n\nfunc h() {}\n\nfunc f() {\n\t(h())\n}\n"
	assertRoundtrip(t, src)
}

func TestParseFieldAccessStatement(t *testing.T) {
	src := "package main\n\nvar x struct{ Y int }\n\nfunc f() {\n\tx.Y\n}\n"
	assertRoundtrip(t, src)
}
