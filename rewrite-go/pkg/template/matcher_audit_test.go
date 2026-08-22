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

package template_test

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// fixture is one snippet of Go source, paired with the scaffold kind that
// parses it.
type fixture struct {
	name string
	kind template.ScaffoldKind
	code string
}

// auditFixtures spans the node kinds visitor.GoVisitor.Visit dispatches, in
// pairs that differ in one place each, so a comparison that skips a field
// shows up as two fixtures matching each other.
func auditFixtures() []fixture {
	expr := template.ScaffoldExpression
	stmt := template.ScaffoldStatement
	top := template.ScaffoldTopLevel
	return []fixture{
		{"ident", expr, `x`},
		{"identOther", expr, `y`},
		{"literalInt", expr, `1`},
		{"literalIntOther", expr, `2`},
		{"literalString", expr, `"a"`},
		{"binary", expr, `a + b`},
		{"binaryOtherOp", expr, `a - b`},
		{"binaryOtherLeft", expr, `c + b`},
		{"binaryOtherRight", expr, `a + c`},
		{"unary", expr, `-a`},
		{"fieldAccess", expr, `a.b`},
		{"fieldAccessOtherTarget", expr, `c.b`},
		{"fieldAccessOtherName", expr, `a.c`},
		{"call", expr, `f(1)`},
		{"callOtherArg", expr, `f(2)`},
		{"callOtherName", expr, `g(1)`},
		{"callNoArgs", expr, `f()`},
		{"callTwoArgs", expr, `f(1, 2)`},
		{"callSelect", expr, `a.f(1)`},
		{"callOtherSelect", expr, `b.f(1)`},
		{"callTypeArgs", expr, `f[int](1)`},
		{"callTypeArgsOther", expr, `f[string](1)`},
		{"index", expr, `a[0]`},
		{"slice", expr, `a[1:2]`},
		{"sliceMax", expr, `a[1:2:3]`},
		{"composite", expr, `T{1}`},
		{"keyValue", expr, `T{K: 1}`},
		{"typeAssert", expr, `a.(T)`},
		{"conversion", expr, `T(a)`},
		{"parens", expr, `(a)`},
		{"funcLit", expr, `func() { g() }`},
		{"funcLitParam", expr, `func(v int) { g() }`},
		{"mapLit", expr, `map[string]int{}`},
		{"mapLitOtherKey", expr, `map[int]int{}`},
		{"parameterizedType", expr, `Foo[string]{}`},
		{"parameterizedTypeOther", expr, `Foo[int]{}`},
		{"indexList", expr, `Store[string, any]{}`},

		{"assign", stmt, `x = 1`},
		{"shortVarDecl", stmt, `x := 1`},
		{"multiAssign", stmt, `x, y = 1, 2`},
		{"multiShortDecl", stmt, `x, y := 1, 2`},
		{"assignOp", stmt, `x += 1`},
		{"assignOpOther", stmt, `x -= 1`},
		{"ifStmt", stmt, `if c { g() }`},
		{"ifTwoStmts", stmt, "if c {\n\tg()\n\th()\n}"},
		{"ifEmptyBlock", stmt, `if c { }`},
		{"ifElse", stmt, `if c { g() } else { h() }`},
		{"ifInit", stmt, `if v := f(); c { g() }`},
		{"forLoop", stmt, `for i := 0; i < 3; i++ { g() }`},
		{"forRange", stmt, `for k := range m { g() }`},
		{"forever", stmt, `for { g() }`},
		{"switchStmt", stmt, `switch v { case 1: g() }`},
		{"switchOtherCase", stmt, `switch v { case 2: g() }`},
		{"selectStmt", stmt, `select { case <-ch: g() }`},
		{"goStmt", stmt, `go g()`},
		{"deferStmt", stmt, `defer g()`},
		{"sendStmt", stmt, `ch <- 1`},
		{"returnStmt", stmt, `return 1`},
		{"returnOther", stmt, `return 2`},
		{"labeled", stmt, `L: g()`},
		{"exprStmtParen", stmt, `(g())`},
		{"varStmt", stmt, `var x = 1`},
		{"constStmt", stmt, `const x = 1`},

		{"funcDecl", top, `func F(x int) { g() }`},
		{"funcDeclOtherParam", top, `func F(y bool) { g() }`},
		{"funcDeclReturn", top, `func F() int { g() }`},
		{"funcDeclReturnOther", top, `func F() string { g() }`},
		{"methodDecl", top, `func (a *A) F() { g() }`},
		{"methodDeclOtherRecv", top, `func (b *B) F() { g() }`},
		{"genericDecl", top, `func G[T any]() { g() }`},
		{"genericDeclOther", top, `func G[U comparable]() { g() }`},
		{"varDecl", top, `var x = 1`},
		{"constDecl", top, `const x = 1`},
		{"constBlock", top, "const (\n\tA = 1\n)"},
		{"varBlock", top, "var (\n\tA = 1\n)"},
		{"typeDecl", top, `type S struct{ A int }`},
		{"typeDeclOtherField", top, `type S struct{ B string }`},
		{"structTagA", top, "type S struct{ F int `json:\"a\"` }"},
		{"structTagB", top, "type S struct{ F int `json:\"b\"` }"},
		{"ifaceDecl", top, `type I interface{ M() }`},
		{"ifaceDeclOther", top, `type I interface{ N() }`},
		{"ifaceUnion", top, `type I interface{ int | string }`},
		{"aliasDecl", top, `type S = Foo[string]`},
		{"aliasDeclOther", top, `type S = Foo[int]`},
		{"chanDecl", top, `type C chan int`},
		{"chanSendDecl", top, `type C chan<- int`},
		{"pointerDecl", top, `type P *int`},
		{"arrayDecl", top, `type A [3]int`},
		{"sliceDecl", top, `type A []int`},
		{"funcTypeDecl", top, `type F func(int) error`},
		{"funcTypeDeclOther", top, `type F func(string) error`},
	}
}

// matches runs the match under a recover: a comparison that panics has
// failed the same way one that answers wrongly has, and letting it abort the
// process would hide every fixture after it.
func matches(t *testing.T, pat *template.GoPattern, candidate java.J, label string) (result bool) {
	t.Helper()
	defer func() {
		if r := recover(); r != nil {
			t.Errorf("%s: match panicked: %v", label, r)
			result = false
		}
	}()
	return pat.Matches(candidate, nil)
}

func patternFor(f fixture) *template.GoPattern {
	return builderFor(f).Build()
}

func builderFor(f fixture) *template.PatternBuilder {
	switch f.kind {
	case template.ScaffoldStatement:
		return template.StatementPattern(f.code)
	case template.ScaffoldTopLevel:
		return template.TopLevel(f.code)
	default:
		return template.Expression(f.code)
	}
}

func fixtureSource(f fixture) string {
	switch f.kind {
	case template.ScaffoldStatement:
		return fmt.Sprintf("package a\n\nfunc __c__() {\n%s\n}\n", f.code)
	case template.ScaffoldTopLevel:
		return fmt.Sprintf("package a\n\n%s\n", f.code)
	default:
		return fmt.Sprintf("package a\n\nvar __c__ = %s\n", f.code)
	}
}

// candidateFor parses a fixture as ordinary source and returns the node a
// pattern of the same kind is built to match.
func candidateFor(t *testing.T, f fixture) java.J {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("audit.go", fixtureSource(f))
	require.NoError(t, err, "fixture %q", f.name)

	switch f.kind {
	case template.ScaffoldStatement:
		for _, s := range cu.Statements {
			if md, ok := s.Element.(*java.MethodDeclaration); ok && md.Body != nil {
				return md.Body.Statements[0].Element
			}
			if gmd, ok := s.Element.(*golang.MethodDeclaration); ok && gmd.Declaration.Body != nil {
				return gmd.Declaration.Body.Statements[0].Element
			}
		}
		t.Fatalf("fixture %q: no function body", f.name)
		return nil
	case template.ScaffoldTopLevel:
		return cu.Statements[0].Element
	default:
		vd, ok := cu.Statements[0].Element.(*java.VariableDeclarations)
		require.True(t, ok, "fixture %q: expected VariableDeclarations, got %T", f.name, cu.Statements[0].Element)
		return vd.Variables[0].Element.Initializer.Element
	}
}

func TestMatcherMatchesItself(t *testing.T) {
	for _, f := range auditFixtures() {
		t.Run(f.name, func(t *testing.T) {
			if !matches(t, patternFor(f), candidateFor(t, f), f.name) {
				t.Errorf("fixture %q does not match itself", f.name)
			}
		})
	}
}

func TestMatcherDistinctness(t *testing.T) {
	fixtures := auditFixtures()
	candidates := make([]java.J, len(fixtures))
	for i, f := range fixtures {
		candidates[i] = candidateFor(t, f)
	}
	for i, f := range fixtures {
		pat := patternFor(f)
		for j, other := range fixtures {
			if i == j || f.kind != other.kind {
				continue
			}
			label := fmt.Sprintf("%s vs %s", f.name, other.name)
			if matches(t, pat, candidates[j], label) {
				t.Errorf("pattern %q matches unrelated fixture %q", f.name, other.name)
			}
		}
	}
}

// unreachableKinds are LST nodes for go.mod and go.sum. A pattern is parsed
// from Go source, which yields neither, so the matcher owes them nothing.
type unreachableCollector struct {
	visitor.GoVisitor
	found map[string]bool
}

func (u *unreachableCollector) PreVisit(t java.Tree, p any) java.Tree {
	switch t.(type) {
	case *golang.GoMod, *golang.GoModBlock, *golang.GoModDirective,
		*golang.GoModValue, *golang.GoSum, *golang.GoSumLine:
		u.found[fmt.Sprintf("%T", t)] = true
	}
	return t
}

func TestMatcherUnreachableKinds(t *testing.T) {
	u := &unreachableCollector{found: map[string]bool{}}
	u.Self = u
	for _, f := range auditFixtures() {
		cu, err := parser.NewGoParser().Parse("audit.go", fixtureSource(f))
		require.NoError(t, err, "fixture %q", f.name)
		u.Visit(cu, nil)
	}
	require.Empty(t, u.found, "go.mod and go.sum nodes are not reachable from Go source")
}

// TestFastPathAgreesWithWalk is what makes the hand-written comparisons in
// fast_path.go safe: the walk reaches every field by construction, so it is
// the answer they are held to.
func TestFastPathAgreesWithWalk(t *testing.T) {
	for _, mode := range []template.TypeMatchingMode{
		template.TypeMatchingOff, template.TypeMatchingLenient, template.TypeMatchingStrict,
	} {
		t.Run(fmt.Sprintf("mode%d", mode), func(t *testing.T) { fastPathAgrees(t, mode) })
	}
}

func fastPathAgrees(t *testing.T, mode template.TypeMatchingMode) {
	fixtures := auditFixtures()
	candidates := make([]java.J, len(fixtures))
	for i, f := range fixtures {
		candidates[i] = candidateFor(t, f)
	}
	for i, f := range fixtures {
		pat := builderFor(f).TypeMatching(mode).Build()
		for j, other := range fixtures {
			if f.kind != other.kind {
				continue
			}
			label := fmt.Sprintf("%s vs %s", f.name, other.name)
			viaWalk := matchesVia(t, pat.MatchesViaWalk, candidates[j], label)
			viaFast := matches(t, pat, candidates[j], label)
			if viaWalk != viaFast {
				t.Errorf("%s: fast path says %v, walk says %v", label, viaFast, viaWalk)
			}
			_ = i
		}
	}
}

func matchesVia(t *testing.T, match func(java.J, *visitor.Cursor) bool, candidate java.J, label string) (result bool) {
	t.Helper()
	defer func() {
		if r := recover(); r != nil {
			t.Errorf("%s: walk panicked: %v", label, r)
			result = false
		}
	}()
	return match(candidate, nil)
}

type callFinder struct {
	visitor.GoVisitor
	call *java.MethodInvocation
}

func (c *callFinder) PreVisit(t java.Tree, p any) java.Tree {
	if mi, ok := t.(*java.MethodInvocation); ok && c.call == nil {
		c.call = mi
	}
	return t
}

// patternCall returns the sole call in a pattern's own tree, so a test can
// assert what the pattern was attributed with.
func patternCall(t *testing.T, pat *template.GoPattern) *java.MethodInvocation {
	t.Helper()
	tree := pat.Tree(t)
	found := &callFinder{}
	found.Self = found
	found.Visit(tree, nil)
	require.NotNil(t, found.call, "no call in the pattern")
	return found.call
}

func patternParamFQNs(mi *java.MethodInvocation) []string {
	var names []string
	for _, p := range mi.MethodType.ParameterTypes {
		names = append(names, matcher.GetFullyQualifiedName(p))
	}
	return names
}
