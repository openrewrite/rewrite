/*
 * Copyright 2026 the original author or authors.
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

package template

import (
	"fmt"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

const parenPrelude = `package main

var a, b, c = 1, 2, 3

func f() int { return 0 }
func g(i int) int { return i }
func h(i int) int { return i }

`

// replacingCall runs a recipe rewriting the given call to the given expression
// over one `var z = ...` line, and checks the line comes back as wantExpr.
func replacingCall(t *testing.T, name, before, after, srcExpr, wantExpr string) {
	t.Helper()
	r := NewRecipe(
		RecipeName("test."+name),
		WithDisplayName(name),
		WithBefore(before),
		WithAfter(after),
		WithCaptures(parenCaptures(before, after)...),
	)
	test.NewRecipeSpec().WithRecipe(r).RewriteRun(t,
		test.GolangRaw(
			parenPrelude+"var z = "+srcExpr+"\n",
			parenPrelude+"var z = "+wantExpr+"\n"))
}

// parenCaptures declares the one capture the capture-carrying cases use.
func parenCaptures(before, after string) []*Capture {
	if strings.Contains(before, "__plh_") || strings.Contains(after, "__plh_") {
		return []*Capture{parenX}
	}
	return nil
}

var parenX = Expr("x")

func TestTemplateParenthesizesLooserResultInTighterContext(t *testing.T) {
	replacingCall(t, "TighterContext", `f()`, `a + b`, `c * f()`, `c * (a + b)`)
}

// `-` is not associative, so an equally-binding right operand still needs them.
func TestTemplateParenthesizesRightOperandOfEqualPrecedence(t *testing.T) {
	replacingCall(t, "EqualPrecRight", `f()`, `a + b`, `c - f()`, `c - (a + b)`)
}

// The left operand of an equally-binding operator already groups that way.
func TestTemplateLeavesLeftOperandOfEqualPrecedenceAlone(t *testing.T) {
	replacingCall(t, "EqualPrecLeft", `f()`, `a + b`, `f() + c`, `a + b + c`)
}

func TestTemplateParenthesizesOperandOfUnary(t *testing.T) {
	replacingCall(t, "UnaryOperand", `f()`, `a + b`, `-f()`, `-(a + b)`)
}

// A call argument already delimits its operand.
func TestTemplateLeavesCallArgumentAlone(t *testing.T) {
	replacingCall(t, "CallArgument", `f()`, `a + b`, `h(f())`, `h(a + b)`)
}

// The position a capture lands in is inside the template, not the source file.
func TestTemplateParenthesizesCapturedOperand(t *testing.T) {
	replacingCall(t, "CapturedOperand",
		fmt.Sprintf(`g(%s)`, parenX), fmt.Sprintf(`%s * 2`, parenX),
		`g(a + b)`, `(a + b) * 2`)
}

// A capture that binds tighter than the template needs nothing added.
func TestTemplateLeavesTighterCapturedOperandAlone(t *testing.T) {
	replacingCall(t, "TighterCapture",
		fmt.Sprintf(`g(%s)`, parenX), fmt.Sprintf(`%s + 2`, parenX),
		`g(a * b)`, `a*b + 2`)
}
