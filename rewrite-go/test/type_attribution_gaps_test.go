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

package test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	rwtest "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func collectAttribution(t *testing.T, src string) *rwtest.TypedNodes {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("attribution.go", src)
	require.NoError(t, err)
	return rwtest.CollectTypedNodes(cu)
}

func parseAttribution(t *testing.T, src string) *golang.CompilationUnit {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("attribution.go", src)
	require.NoError(t, err)
	return cu
}

// identType is the attributed type of the first identifier named `name`.
func identType(t *testing.T, cu *golang.CompilationUnit, name string) java.JavaType {
	t.Helper()
	var found java.JavaType
	visitor.Walk(cu, func(n java.Tree) bool {
		if id, ok := n.(*java.Identifier); ok && id.Name == name && found == nil {
			found = id.Type
		}
		return found == nil
	})
	require.NotNilf(t, found, "no typed identifier named %q", name)
	return found
}

// invocationNamed returns the single invocation of the given name, failing
// when the source has none or more than one.
func invocationNamed(t *testing.T, c *rwtest.TypedNodes, name string) *java.MethodInvocation {
	t.Helper()
	var found *java.MethodInvocation
	for _, mi := range c.Invocations {
		if mi.Name != nil && mi.Name.Name == name {
			require.Nilf(t, found, "more than one call named %q", name)
			found = mi
		}
	}
	require.NotNilf(t, found, "no call named %q", name)
	return found
}

// A receiver's own spelling is never a declaring type: a local variable named
// `os` must not read back as the `os` package.

func TestUnresolvedReceiverHasNoDeclaringTypeFQN(t *testing.T) {
	c := collectAttribution(t, `package main

import "github.com/unresolvable/thing"

func f() {
	os := thing.New()
	_ = os.WriteFile("f", nil, 0777)
	http := thing.New()
	_, _ = http.Get("http://x")
}
`)

	for _, name := range []string{"WriteFile", "Get"} {
		mi := invocationNamed(t, c, name)
		assert.Emptyf(t, matcher.DeclaringTypeFQN(mi), "%s: local variable impersonating a package", name)
		assert.Falsef(t, matcher.IsResolved(mi), "%s: call did not resolve", name)
	}
}

func TestUnresolvedPackageReportsImportPath(t *testing.T) {
	c := collectAttribution(t, `package main

import "github.com/unresolvable/thing"

func f() {
	_ = thing.New()
}
`)

	mi := invocationNamed(t, c, "New")
	assert.Equal(t, "github.com/unresolvable/thing", matcher.DeclaringTypeFQN(mi))
	assert.False(t, matcher.IsResolved(mi), "the package is named but its symbols are not loaded")
}

func TestResolvedPackageFunctionIsResolved(t *testing.T) {
	c := collectAttribution(t, `package main

import "fmt"

func f() {
	fmt.Println("x")
}
`)

	mi := invocationNamed(t, c, "Println")
	assert.Equal(t, "fmt", matcher.DeclaringTypeFQN(mi))
	assert.True(t, matcher.IsResolved(mi))
}

// A structural (inline) interface receiver and a func-typed field call both
// resolve to a declaring type.

func TestStructuralInterfaceReceiverHasDeclaringType(t *testing.T) {
	c := collectAttribution(t, `package main

func g(anon interface{ Query(string) (interface{ Close() }, error) }) {
	_, _ = anon.Query("SELECT 1")
}
`)

	mi := invocationNamed(t, c, "Query")
	require.NotNil(t, mi.MethodType)
	require.NotNil(t, mi.MethodType.DeclaringType, "structural interface not attributed")
	declaring := matcher.AsClass(mi.MethodType.DeclaringType)
	require.NotNil(t, declaring)
	assert.Equal(t, "Interface", declaring.Kind)
	assert.Empty(t, declaring.FullyQualifiedName, "an inline interface names nothing")
	require.Len(t, declaring.Methods, 1)
	assert.Equal(t, "Query", declaring.Methods[0].Name)
	assert.True(t, matcher.IsResolved(mi))
	assert.Empty(t, matcher.DeclaringTypeFQN(mi))
}

func TestFuncTypedFieldCallDeclaresOwningStruct(t *testing.T) {
	c := collectAttribution(t, `package main

type Handlers struct {
	fn func() error
}

func g(h Handlers) {
	_ = h.fn()
}
`)

	mi := invocationNamed(t, c, "fn")
	require.NotNil(t, mi.MethodType)
	require.NotNil(t, mi.MethodType.DeclaringType, "func-typed field not attributed to its owner")
	assert.Equal(t, "main.Handlers", matcher.DeclaringTypeFQN(mi))
	assert.True(t, matcher.IsResolved(mi))
}

func TestFuncTypedFieldOnAnonymousStruct(t *testing.T) {
	c := collectAttribution(t, `package main

func g(b struct{ fn func() error }) {
	_ = b.fn()
}
`)

	mi := invocationNamed(t, c, "fn")
	require.NotNil(t, mi.MethodType)
	require.NotNil(t, mi.MethodType.DeclaringType)
	assert.Empty(t, mi.MethodType.DeclaringType.GetFullyQualifiedName(), "an inline struct names nothing")
}

func TestNamedFuncTypeFieldKeepsItsOwnType(t *testing.T) {
	c := collectAttribution(t, `package main

type Callback func() error

type Handlers struct {
	fn Callback
}

func g(h Handlers) {
	_ = h.fn()
}
`)

	mi := invocationNamed(t, c, "fn")
	require.NotNil(t, mi.MethodType)
	assert.Equal(t, "main.Callback", matcher.DeclaringTypeFQN(mi),
		"a named func type is what the call goes through")
}

// A composite literal carries its own type, not only a type expression.

func TestCompositeCarriesItsType(t *testing.T) {
	c := collectAttribution(t, `package main

import "crypto/tls"

func h() {
	_ = &tls.Config{InsecureSkipVerify: true}
	_ = []tls.Config{{InsecureSkipVerify: true}}
	_ = map[string]tls.Config{"a": {InsecureSkipVerify: true}}
}
`)

	var fqns []string
	for _, comp := range c.Composites {
		fqns = append(fqns, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(comp)))
	}
	assert.Equal(t, []string{
		"crypto/tls.Config",
		"crypto/tls.Config[]",
		"crypto/tls.Config",
		"map",
		"crypto/tls.Config",
	}, fqns)
}

func TestUntypedInnerCompositeCarriesItsType(t *testing.T) {
	c := collectAttribution(t, `package main

import "crypto/tls"

func h() {
	_ = [][]tls.Config{{{InsecureSkipVerify: true}}}
}
`)

	require.Len(t, c.Composites, 3)
	inner := c.Composites[2]
	assert.Nil(t, inner.TypeExpr, "the innermost composite has no type expression to read")
	assert.Equal(t, "crypto/tls.Config", matcher.GetFullyQualifiedName(matcher.TypeOfExpression(inner)))
}

// A conversion is its own node; a marker separates a builtin from an ordinary call.

func TestConversionIsATypeCast(t *testing.T) {
	c := collectAttribution(t, `package main

import "time"

type MyInt int

func declared(b []byte) []byte { return b }

func k(src []byte, n int64) {
	_ = []byte("a")
	_ = string(src)
	_ = MyInt(3)
	_ = time.Duration(n)
	_ = declared(src)
}
`)

	var converted []string
	for _, tc := range c.Conversions {
		converted = append(converted, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(tc)))
	}
	// A package-qualified conversion is the type it names, not its package.
	assert.Equal(t, []string{"byte[]", "string", "main.MyInt", "time.Duration"}, converted)
}

func TestBuiltinIsMarked(t *testing.T) {
	c := collectAttribution(t, `package main

func k(dst, src []byte) {
	_ = copy(dst, src)
	_ = len(src)
	_ = append(dst, src...)
}
`)

	var marked []string
	for _, mi := range c.Invocations {
		if java.FindMarker[golang.Builtin](mi.Markers) != nil {
			marked = append(marked, mi.Name.Name)
		}
	}
	assert.Equal(t, []string{"copy", "len", "append"}, marked)
}

// Go resolves a predeclared function to a *types.Builtin, whose own type is
// invalid; the call-site signature lives on the callee expression instead.
// `builtin` is the package Go's own documentation files them under.
func TestBuiltinCallCarriesItsCallSiteSignature(t *testing.T) {
	c := collectAttribution(t, `package main

func k(dst, src []byte, m map[string]int, ch chan int) {
	_ = append(dst, src...)
	_ = len(src)
	delete(m, "k")
	close(ch)
}
`)

	for _, name := range []string{"append", "len", "delete", "close"} {
		mi := invocationNamed(t, c, name)
		require.NotNilf(t, mi.MethodType, "%s has no MethodType", name)
		assert.Equalf(t, name, mi.MethodType.Name, "%s name", name)
		assert.Equalf(t, "builtin", matcher.GetFullyQualifiedName(mi.MethodType.DeclaringType),
			"%s declaring type", name)
		assert.Truef(t, matcher.IsResolved(mi), "%s is not resolved", name)
	}

	assert.Equal(t, "byte[]", matcher.GetFullyQualifiedName(invocationNamed(t, c, "append").MethodType.ReturnType))
	assert.Equal(t, "int", matcher.GetFullyQualifiedName(invocationNamed(t, c, "len").MethodType.ReturnType))
	// A builtin returning nothing is void, as any other such call is.
	assert.Equal(t, "void", matcher.GetFullyQualifiedName(invocationNamed(t, c, "close").MethodType.ReturnType))
}

// The identifier naming the callee carries the same method type a declared
// function's would.
func TestBuiltinCalleeIdentifierIsTyped(t *testing.T) {
	c := collectAttribution(t, `package main

func k(src []byte) {
	_ = len(src)
}
`)

	mi := invocationNamed(t, c, "len")
	assert.Equal(t, mi.MethodType, mi.Name.Type)
}

// unsafe's builtins fold to a constant, so go/types records `invalid type` for
// the callee rather than the signature it recorded for every other builtin.
// Attribution reports what the checker knows, so the call stays untyped.
func TestUnsafeBuiltinCallIsUntyped(t *testing.T) {
	c := collectAttribution(t, `package main

import "unsafe"

func k(x int) uintptr {
	return unsafe.Sizeof(x)
}
`)

	assert.Nil(t, invocationNamed(t, c, "Sizeof").MethodType)
}

// In the comma-ok form go/types records the assertion as the tuple `(T, bool)`,
// which is the idiomatic spelling; the assertion evaluates to T either way.
func TestTypeAssertionCarriesTheTypeItAsserts(t *testing.T) {
	c := collectAttribution(t, `package main

import "bytes"

func k(a any) {
	s := a.(string)
	n, ok := a.(int)
	b, ok2 := a.(*bytes.Buffer)
	_, _, _, _, _ = s, n, ok, b, ok2
}
`)

	var asserted []string
	for _, ta := range c.TypeAssertions {
		asserted = append(asserted, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(ta)))
	}
	assert.Equal(t, []string{"string", "int", "bytes.Buffer"}, asserted)
}

// `any` is an alias for the empty interface, which the checker resolves to a
// *types.Interface naming nothing. It takes its Go name, as map and chan do,
// rather than a class with no name to match on.
func TestEmptyInterfaceIsNamedAny(t *testing.T) {
	cu := parseAttribution(t, `package main

func k(a any, e interface{}, m map[string]any) {
	_, _, _ = a, e, m
}
`)

	assert.Equal(t, "any", matcher.GetFullyQualifiedName(identType(t, cu, "a")))
	assert.Equal(t, "any", matcher.GetFullyQualifiedName(identType(t, cu, "e")))
	mapType, ok := identType(t, cu, "m").(*java.JavaTypeParameterized)
	require.Truef(t, ok, "m is %T, want a parameterized map", identType(t, cu, "m"))
	assert.Equal(t, "map", matcher.GetFullyQualifiedName(mapType))
	assert.Equal(t, "any", matcher.GetFullyQualifiedName(mapType.TypeParameters[1]))
}

// An anonymous interface that declares methods has a method set but no name, so
// its members are what a recipe can reach; there is no name to invent.
func TestAnonymousInterfaceHasMembersButNoName(t *testing.T) {
	cu := parseAttribution(t, `package main

func k(r interface{ Read() int }) {
	_ = r
}
`)

	cls := matcher.AsClass(identType(t, cu, "r"))
	require.NotNil(t, cls)
	assert.Equal(t, "", cls.FullyQualifiedName)
	require.Len(t, cls.Methods, 1)
	assert.Equal(t, "Read", cls.Methods[0].Name)
}

// A composite type spelling — `map[K]V`, `chan T`, `[N]T` — is a node with no
// type slot of its own, so its type comes from the parts it is spelled out of.
// A recipe reading a conversion's type has to see the type either way.
func TestCompositeTypeSpellingCarriesAType(t *testing.T) {
	c := collectAttribution(t, `package main

func f(ch chan int, m map[string]int, a [3]int, s []int) {
	_ = (chan int)(ch)
	_ = (map[string]int)(m)
	_ = ([3]int)(a)
	_ = ([]int)(s)
}
`)

	var converted []string
	for _, tc := range c.Conversions {
		converted = append(converted, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(tc)))
	}
	assert.Equal(t, []string{"chan", "map", "int[]", "int[]"}, converted)
}

// A directional channel is its own type, as the type mapper names it.
func TestChannelDirectionIsPartOfTheType(t *testing.T) {
	c := collectAttribution(t, `package main

func f(send chan<- int, recv <-chan int) {
	_ = (chan<- int)(send)
	_ = (<-chan int)(recv)
}
`)

	var converted []string
	for _, tc := range c.Conversions {
		converted = append(converted, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(tc)))
	}
	assert.Equal(t, []string{"chan<-", "<-chan"}, converted)
}

// The element and key types are what make a composite spelling matchable.
func TestCompositeTypeSpellingCarriesItsParts(t *testing.T) {
	c := collectAttribution(t, `package main

import "bytes"

func f(m map[string]*bytes.Buffer) {
	_ = (map[string]*bytes.Buffer)(m)
}
`)

	require.Len(t, c.Conversions, 1)
	mapType, ok := matcher.TypeOfExpression(c.Conversions[0]).(*java.JavaTypeParameterized)
	require.Truef(t, ok, "conversion type is %T, want a parameterized map",
		matcher.TypeOfExpression(c.Conversions[0]))
	require.Len(t, mapType.TypeParameters, 2)
	assert.Equal(t, "string", matcher.GetFullyQualifiedName(mapType.TypeParameters[0]))
	assert.Equal(t, "bytes.Buffer", matcher.GetFullyQualifiedName(mapType.TypeParameters[1]))
}

func TestUserDefinedCopyIsNotABuiltin(t *testing.T) {
	c := collectAttribution(t, `package main

func copy(dst, src []byte) int { return 0 }

func k(dst, src []byte) {
	_ = copy(dst, src)
}
`)

	mi := invocationNamed(t, c, "copy")
	assert.Nil(t, java.FindMarker[golang.Builtin](mi.Markers))
	assert.Equal(t, "main", matcher.DeclaringTypeFQN(mi))
}

// Receiver shapes that resolve to a declaring type.

func TestReceiverShapesResolveToDeclaringType(t *testing.T) {
	for _, tc := range []struct {
		shape    string
		receiver string
		decls    string
	}{
		{"function result", "newDB()", ""},
		{"struct field", "holder.db", "var holder struct{ db *sql.DB }"},
		{"map index", "byName[\"x\"]", "var byName map[string]*sql.DB"},
		{"slice index", "dbs[0]", "var dbs []*sql.DB"},
		{"channel receive", "(<-ch)", "var ch chan *sql.DB"},
		{"type alias", "aliased", "type Alias = sql.DB\n\nvar aliased *Alias"},
		{"type assertion", "boxed.(*sql.DB)", "var boxed interface{}"},
		{"interface variable", "pinger", "type Pinger interface{ Ping() error }\n\nvar pinger Pinger"},
		{"embedded promotion", "embedder", "type Embedder struct{ *sql.DB }\n\nvar embedder Embedder"},
	} {
		t.Run(tc.shape, func(t *testing.T) {
			c := collectAttribution(t, "package main\n\nimport \"database/sql\"\n\n"+
				tc.decls+"\n\nfunc newDB() *sql.DB { return nil }\n\nfunc f() {\n\t_ = "+tc.receiver+".Ping()\n}\n")
			mi := invocationNamed(t, c, "Ping")
			want := "database/sql.DB"
			if tc.shape == "interface variable" {
				want = "main.Pinger"
			}
			assert.Equal(t, want, matcher.DeclaringTypeFQN(mi))
			assert.True(t, matcher.IsResolved(mi))
		})
	}
}

func TestGenericReceiverResolvesToItsConstraint(t *testing.T) {
	c := collectAttribution(t, `package main

type Pinger interface{ Ping() error }

func ping[T Pinger](t T) error {
	return t.Ping()
}
`)

	mi := invocationNamed(t, c, "Ping")
	assert.Equal(t, "main.Pinger", matcher.DeclaringTypeFQN(mi))
}

func TestShortVarDeclGetsTypes(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("attribution.go", `package main

import "net/http"

func f() {
	resp, err := http.Get("http://x")
	_, _ = resp, err
}
`)
	require.NoError(t, err)

	types := map[string]string{}
	v := visitor.Init(&identTypeCollector{types: types})
	v.Visit(cu, nil)

	assert.Equal(t, "net/http.Response", types["resp"])
	assert.Equal(t, "error", types["err"])
}

type identTypeCollector struct {
	visitor.GoVisitor
	types map[string]string
}

func (v *identTypeCollector) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if fqn := matcher.GetFullyQualifiedName(ident.Type); fqn != "" {
		if _, seen := v.types[ident.Name]; !seen {
			v.types[ident.Name] = fqn
		}
	}
	return ident
}

func TestOneUnresolvableImportDoesNotDegradeOtherCalls(t *testing.T) {
	c := collectAttribution(t, `package main

import (
	"fmt"
	"github.com/unresolvable/thing"
)

func f() {
	_ = thing.New()
	fmt.Println("x")
}
`)

	assert.Equal(t, "fmt", matcher.DeclaringTypeFQN(invocationNamed(t, c, "Println")))
	assert.True(t, matcher.IsResolved(invocationNamed(t, c, "Println")))
}

// An index, a unary operator and a generic instantiation each carry the type
// they evaluate to.

func TestArrayAccessCarriesItsResultType(t *testing.T) {
	c := collectAttribution(t, `package main

func f(xs []int, m map[string]bool, arr [3]byte, s string) {
	_ = xs[0]
	_ = m["k"]
	v, ok := m["k"]
	_, _ = v, ok
	_ = arr[1]
	_ = s[0]
}
`)

	var types []string
	for _, aa := range c.ArrayAccesses {
		types = append(types, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(aa)))
	}
	// The comma-ok form evaluates to V.
	assert.Equal(t, []string{"int", "bool", "bool", "byte", "byte"}, types)
}

func TestUnaryCarriesItsResultType(t *testing.T) {
	c := collectAttribution(t, `package main

func f(n int, b bool) {
	_ = -n
	_ = !b
	_ = ^n
	n++
}
`)

	var types []string
	for _, u := range c.Unaries {
		types = append(types, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(u)))
	}
	assert.Equal(t, []string{"int", "bool", "int", "int"}, types)
}

func TestGoUnaryCarriesItsResultType(t *testing.T) {
	c := collectAttribution(t, `package main

import "crypto/tls"

func f(p *int, ch chan string) {
	_ = &tls.Config{}
	_ = *p
	_ = <-ch
}
`)

	var types []string
	for _, u := range c.GoUnaries {
		types = append(types, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(u)))
	}
	// Go pointers are transparent for refactoring, so `&T{}` is typed T.
	assert.Equal(t, []string{"crypto/tls.Config", "int", "string"}, types)
}

func TestParameterizedTypeCarriesItsType(t *testing.T) {
	c := collectAttribution(t, `package main

type Box[T any] struct{ v T }

type Pair[K comparable, V any] struct {
	k K
	v V
}

func f(b Box[int], p Pair[string, int]) {
}
`)

	var types []string
	for _, pt := range c.ParameterizedTypes {
		types = append(types, matcher.GetFullyQualifiedName(matcher.TypeOfExpression(pt)))
	}
	assert.Equal(t, []string{"main.Box", "main.Pair"}, types)
}
