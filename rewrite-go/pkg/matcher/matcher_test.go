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

package matcher

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestGetFullyQualifiedName(t *testing.T) {
	tests := []struct {
		name string
		typ  java.JavaType
		want string
	}{
		{"nil", nil, ""},
		{"class", &java.JavaTypeClass{FullyQualifiedName: "fmt.Stringer"}, "fmt.Stringer"},
		{"primitive", &java.JavaTypePrimitive{Keyword: "int"}, "int"},
		{"parameterized", &java.JavaTypeParameterized{Type: &java.JavaTypeClass{FullyQualifiedName: "map"}}, "map"},
		{"unknown", &java.JavaTypeUnknown{}, ""},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetFullyQualifiedName(tt.typ); got != tt.want {
				t.Errorf("GetFullyQualifiedName() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestIsOfClassType(t *testing.T) {
	cls := &java.JavaTypeClass{FullyQualifiedName: "time.Time"}
	assert.True(t, IsOfClassType(cls, "time.Time"))
	assert.False(t, IsOfClassType(cls, "time.Duration"))
}

func TestIsAssignableTo(t *testing.T) {
	stringer := &java.JavaTypeClass{FullyQualifiedName: "fmt.Stringer"}
	myType := &java.JavaTypeClass{
		FullyQualifiedName: "main.MyType",
		Interfaces:         []java.FullyQualified{stringer},
	}

	assert.True(t, IsAssignableTo(myType, "main.MyType"))
	assert.True(t, IsAssignableTo(myType, "fmt.Stringer"))
	assert.False(t, IsAssignableTo(myType, "io.Reader"))
}

func TestIsError(t *testing.T) {
	errType := &java.JavaTypeClass{FullyQualifiedName: "error"}
	assert.True(t, IsError(errType))
	assert.False(t, IsError(&java.JavaTypeClass{FullyQualifiedName: "string"}))
}

func TestIsString(t *testing.T) {
	assert.True(t, IsString(&java.JavaTypePrimitive{Keyword: "String"}))
	assert.False(t, IsString(&java.JavaTypePrimitive{Keyword: "int"}))
}

func TestIsInt(t *testing.T) {
	// Signed widths map to primitive keywords; unsigned widths to named types.
	intLike := []java.JavaType{
		&java.JavaTypePrimitive{Keyword: "int"},   // int, int32
		&java.JavaTypePrimitive{Keyword: "long"},  // int64
		&java.JavaTypePrimitive{Keyword: "short"}, // int16
		&java.JavaTypePrimitive{Keyword: "byte"},  // int8, byte
		&java.JavaTypeClass{FullyQualifiedName: "uint"},
		&java.JavaTypeClass{FullyQualifiedName: "uint8"},
		&java.JavaTypeClass{FullyQualifiedName: "uint16"},
		&java.JavaTypeClass{FullyQualifiedName: "uint32"},
		&java.JavaTypeClass{FullyQualifiedName: "uint64"},
		&java.JavaTypeClass{FullyQualifiedName: "uintptr"},
	}
	for _, typ := range intLike {
		assert.True(t, IsInt(typ))
	}
	notInt := []java.JavaType{
		&java.JavaTypePrimitive{Keyword: "double"},
		&java.JavaTypePrimitive{Keyword: "String"},
		&java.JavaTypePrimitive{Keyword: "boolean"},
		nil,
	}
	for _, typ := range notInt {
		assert.False(t, IsInt(typ))
	}
}

func TestIsNumeric(t *testing.T) {
	numeric := []java.JavaType{
		// Signed widths, floats, and rune map to primitive keywords.
		&java.JavaTypePrimitive{Keyword: "int"},    // int, int32
		&java.JavaTypePrimitive{Keyword: "long"},   // int64
		&java.JavaTypePrimitive{Keyword: "short"},  // int16
		&java.JavaTypePrimitive{Keyword: "byte"},   // int8, byte
		&java.JavaTypePrimitive{Keyword: "float"},  // float32
		&java.JavaTypePrimitive{Keyword: "double"}, // float64
		&java.JavaTypePrimitive{Keyword: "char"},   // rune
		// Unsigned widths have no Java primitive, so they are named types.
		&java.JavaTypeClass{FullyQualifiedName: "uint"},
		&java.JavaTypeClass{FullyQualifiedName: "uint8"},
		&java.JavaTypeClass{FullyQualifiedName: "uint16"},
		&java.JavaTypeClass{FullyQualifiedName: "uint32"},
		&java.JavaTypeClass{FullyQualifiedName: "uint64"},
		&java.JavaTypeClass{FullyQualifiedName: "uintptr"},
	}
	for _, typ := range numeric {
		assert.True(t, IsNumeric(typ))
	}
	assert.False(t, IsNumeric(&java.JavaTypePrimitive{Keyword: "String"}))
	assert.False(t, IsNumeric(nil))
}

// Pattern type names must resolve to the same signature the type mapper
// produces, so int64 patterns match "long" and uint stays "uint".
func TestResolveGoType(t *testing.T) {
	tests := map[string]string{
		"int": "int", "int32": "int", "int16": "short", "int8": "byte", "int64": "long",
		"uint": "uint", "uint64": "uint64", "uintptr": "uintptr",
		"float32": "float", "float64": "double",
		"string": "String", "bool": "boolean", "byte": "byte", "rune": "char", "error": "error",
	}
	for in, want := range tests {
		if got := resolveGoType(in); got != want {
			t.Errorf("resolveGoType(%q) = %q, want %q", in, got, want)
		}
	}
}

func TestAsClass(t *testing.T) {
	cls := &java.JavaTypeClass{FullyQualifiedName: "foo.Bar"}
	if AsClass(cls) != cls {
		t.Error("AsClass should return the class directly")
	}
	param := &java.JavaTypeParameterized{Type: cls}
	if AsClass(param) != cls {
		t.Error("AsClass should unwrap parameterized types")
	}
	assert.Nil(t, AsClass(&java.JavaTypePrimitive{Keyword: "int"}))
}

func TestGlobToRegexp(t *testing.T) {
	tests := []struct {
		pattern string
		input   string
		want    bool
	}{
		{"fmt", "fmt", true},
		{"fmt", "log", false},
		{"*", "fmt", true},
		{"*", "time.Time", false}, // * doesn't match dots
		{"*..*", "time.Time", true},
		{"*..*", "io", true},
		{"time.*", "time.Time", true},
		{"time.*", "time.Duration", true},
		{"time.*", "fmt.Stringer", false},
		{"Sprintf", "Sprintf", true},
		{"Sprint*", "Sprintf", true},
		{"Sprint*", "Sprint", true},
		{"Sprint*", "Println", false},
	}
	for _, tt := range tests {
		re := globToRegexp(tt.pattern)
		got := re.MatchString(tt.input)
		assert.Equal(t, tt.want, got)
	}
}

func TestMethodMatcherParsing(t *testing.T) {
	mm := NewMethodMatcher("fmt Sprintf(string, ..)")
	require.NotNil(t, mm.declaringTypePattern)
	require.NotNil(t, mm.methodNamePattern)
	assert.True(t, mm.matchesAnyArgs)
}

func TestMethodMatcherMatchesAnyArgs(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "fmt"},
		},
		Name: &java.Identifier{Name: "Println"},
	}
	assert.True(t, mm.Matches(mi))
}

func TestMethodMatcherNoMatchWrongName(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "fmt"},
		},
		Name: &java.Identifier{Name: "Sprintf"},
	}
	assert.False(t, mm.Matches(mi))
}

func TestMethodMatcherNoMatchWrongPackage(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "log"},
		},
		Name: &java.Identifier{Name: "Println"},
	}
	assert.False(t, mm.Matches(mi))
}

func TestMethodMatcherWildcardType(t *testing.T) {
	mm := NewMethodMatcher("* Sub(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "t"},
		},
		Name: &java.Identifier{Name: "Sub"},
	}
	// With just an identifier "t" as select, DeclaringTypeFQN returns "t"
	// which matches "*" pattern
	assert.True(t, mm.Matches(mi))
}

func TestMethodMatcherWithTypeInfo(t *testing.T) {
	mm := NewMethodMatcher("fmt Sprintf(..)")
	fmtType := &java.JavaTypeClass{FullyQualifiedName: "fmt"}
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "fmt"},
		},
		Name: &java.Identifier{Name: "Sprintf"},
		MethodType: &java.JavaTypeMethod{
			DeclaringType: fmtType,
			Name:          "Sprintf",
		},
	}
	assert.True(t, mm.Matches(mi))
}

func TestMethodMatcherMatchesMethod(t *testing.T) {
	mm := NewMethodMatcher("time.Time Sub(..)")
	mt := &java.JavaTypeMethod{
		DeclaringType: &java.JavaTypeClass{FullyQualifiedName: "time.Time"},
		Name:          "Sub",
	}
	assert.True(t, mm.MatchesMethod(mt))
}

func TestMethodMatcherOnParsedCode(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "fmt"

func main() {
	fmt.Println("hello")
	fmt.Sprintf("%d", 42)
}
`)
	require.NoError(t, err)

	printlnMatcher := NewMethodMatcher("fmt Println(..)")
	sprintfMatcher := NewMethodMatcher("fmt Sprintf(..)")

	var printlnCount, sprintfCount int
	v := visitor.Init(&methodMatcherVisitor{
		matchers: map[string]*MethodMatcher{
			"println": printlnMatcher,
			"sprintf": sprintfMatcher,
		},
		counts: map[string]*int{
			"println": &printlnCount,
			"sprintf": &sprintfCount,
		},
	})
	v.Visit(cu, nil)

	assert.Equal(t, 1, printlnCount)
	assert.Equal(t, 1, sprintfCount)
}

func TestMethodMatcherNoMatchOnParsedCode(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "log"

func main() {
	log.Println("hello")
}
`)
	require.NoError(t, err)

	fmtPrintln := NewMethodMatcher("fmt Println(..)")

	var count int
	v := visitor.Init(&methodMatcherVisitor{
		matchers: map[string]*MethodMatcher{"match": fmtPrintln},
		counts:   map[string]*int{"match": &count},
	})
	v.Visit(cu, nil)

	assert.Equal(t, 0, count)
}

// Test helper visitor that counts method matcher hits.
type methodMatcherVisitor struct {
	visitor.GoVisitor
	matchers map[string]*MethodMatcher
	counts   map[string]*int
}

func (v *methodMatcherVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	mi = v.GoVisitor.VisitMethodInvocation(mi, p).(*java.MethodInvocation)
	for name, matcher := range v.matchers {
		if matcher.Matches(mi) {
			*v.counts[name]++
		}
	}
	return mi
}

// TypeOfExpression for Parentheses and TypeCast must DERIVE the type from the
// inner expression (mirroring Java's J.Parentheses.getType / J.TypeCast.getType),
// since neither struct stores a Type field.
func TestTypeOfExpressionDerivesThroughWrappers(t *testing.T) {
	// given
	strType := &java.JavaTypeClass{FullyQualifiedName: "string"}
	inner := &java.Identifier{Name: "s", Type: strType}

	parens := &java.Parentheses{
		Tree: java.RightPadded[java.Expression]{Element: inner},
	}
	cast := &java.TypeCast{
		Clazz: &java.ControlParentheses{
			Tree: java.RightPadded[java.Expression]{Element: inner},
		},
		Expr: &java.Identifier{Name: "x"},
	}

	// when
	parensType := TypeOfExpression(parens)
	castType := TypeOfExpression(cast)

	// then
	if parensType != strType {
		t.Errorf("Parentheses: got %v, want derived inner type %v", parensType, strType)
	}
	if castType != strType {
		t.Errorf("TypeCast: got %v, want derived Clazz type %v", castType, strType)
	}
}
