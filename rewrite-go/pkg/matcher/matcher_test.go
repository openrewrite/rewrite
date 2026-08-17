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
	assert.True(t, IsOfClassType(cls, "time.Time"), "expected true for exact match")
	assert.False(t, IsOfClassType(cls, "time.Duration"), "expected false for different type")
}

func TestIsAssignableTo(t *testing.T) {
	stringer := &java.JavaTypeClass{FullyQualifiedName: "fmt.Stringer"}
	myType := &java.JavaTypeClass{
		FullyQualifiedName: "main.MyType",
		Interfaces:         []java.FullyQualified{stringer},
	}

	assert.True(t, IsAssignableTo(myType, "main.MyType"), "type should be assignable to itself")
	assert.True(t, IsAssignableTo(myType, "fmt.Stringer"), "type should be assignable to its interface")
	assert.False(t, IsAssignableTo(myType, "io.Reader"), "type should not be assignable to unrelated interface")
}

func TestIsError(t *testing.T) {
	errType := &java.JavaTypeClass{FullyQualifiedName: "error"}
	assert.True(t, IsError(errType), "expected true for error type")
	assert.False(t, IsError(&java.JavaTypeClass{FullyQualifiedName: "string"}), "expected false for non-error type")
}

// goType names a Go basic type the way the type mapper attributes it.
func goType(name string) java.JavaType {
	return &java.JavaTypeClass{FullyQualifiedName: name, Kind: "Class"}
}

func TestIsString(t *testing.T) {
	assert.True(t, IsString(goType("string")), "expected true for string")
	assert.True(t, IsString(goType("untyped string")), "expected true for an untyped string constant")
	assert.False(t, IsString(goType("int")), "expected false for int")
	assert.False(t, IsString(nil), "expected false for an unattributed type")
}

func TestIsInt(t *testing.T) {
	for _, name := range []string{
		"int", "int8", "int16", "int32", "int64",
		"uint", "uint8", "uint16", "uint32", "uint64", "uintptr",
		"byte", "rune", "untyped int", "untyped rune",
	} {
		assert.Truef(t, IsInt(goType(name)), "IsInt(%q) = false, want true", name)
	}
	for _, name := range []string{"float64", "complex128", "string", "bool", "untyped nil", "main.Point"} {
		assert.Falsef(t, IsInt(goType(name)), "IsInt(%q) = true, want false", name)
	}
	assert.False(t, IsInt(nil), "IsInt(nil) = true, want false")
}

func TestIsNumeric(t *testing.T) {
	for _, name := range []string{
		"int", "int64", "uint64", "byte", "rune",
		"float32", "float64", "complex64", "complex128",
		"untyped int", "untyped float", "untyped complex",
	} {
		assert.Truef(t, IsNumeric(goType(name)), "IsNumeric(%q) = false, want true", name)
	}
	for _, name := range []string{"string", "bool", "untyped nil", "main.Point"} {
		assert.Falsef(t, IsNumeric(goType(name)), "IsNumeric(%q) = true, want false", name)
	}
	assert.False(t, IsNumeric(nil), "IsNumeric(nil) = true, want false")
}

func TestIsBool(t *testing.T) {
	assert.True(t, IsBool(goType("bool")), "expected true for bool")
	assert.True(t, IsBool(goType("untyped bool")), "expected true for an untyped bool constant")
	assert.False(t, IsBool(goType("int")), "expected false for int")
	assert.False(t, IsBool(nil), "expected false for an unattributed type")
}

func TestIsSameGoType(t *testing.T) {
	assert.True(t, IsSameGoType(goType("byte"), goType("uint8")), "byte and uint8 are one type")
	assert.True(t, IsSameGoType(goType("rune"), goType("int32")), "rune and int32 are one type")
	assert.True(t, IsSameGoType(goType("int"), goType("int")))
	assert.False(t, IsSameGoType(goType("int"), goType("int32")), "int is its own type, distinct from int32")
	assert.False(t, IsSameGoType(goType("byte"), goType("int8")), "byte is unsigned, int8 signed")
	assert.False(t, IsSameGoType(goType("int"), goType("untyped int")), "an untyped constant is not yet an int")
	assert.False(t, IsSameGoType(goType("int"), nil), "an unattributed type is not known to match")
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
	assert.Nil(t, AsClass(&java.JavaTypePrimitive{Keyword: "int"}), "AsClass should return nil for non-class types")
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
		assert.Equalf(t, tt.want, got, "globToRegexp(%q).MatchString", tt.pattern)
	}
}

func TestMethodMatcherParsing(t *testing.T) {
	mm := NewMethodMatcher("fmt Sprintf(string, ..)")
	require.NotNil(t, mm.declaringTypePattern, "expected declaringTypePattern")
	require.NotNil(t, mm.methodNamePattern, "expected methodNamePattern")
	assert.True(t, mm.matchesAnyArgs, "expected matchesAnyArgs for .. pattern")
}

func TestMethodMatcherMatchesAnyArgs(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "fmt"},
		},
		Name: &java.Identifier{Name: "Println"},
	}
	assert.True(t, mm.Matches(mi), "expected match for fmt.Println with any args")
}

func TestMethodMatcherNoMatchWrongName(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "fmt"},
		},
		Name: &java.Identifier{Name: "Sprintf"},
	}
	assert.False(t, mm.Matches(mi), "expected no match for wrong method name")
}

func TestMethodMatcherNoMatchWrongPackage(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{
			Element: &java.Identifier{Name: "log"},
		},
		Name: &java.Identifier{Name: "Println"},
	}
	assert.False(t, mm.Matches(mi), "expected no match for wrong package")
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
	assert.True(t, mm.Matches(mi), "expected match for wildcard declaring type")
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
	assert.True(t, mm.Matches(mi), "expected match with type info")
}

func TestMethodMatcherMatchesMethod(t *testing.T) {
	mm := NewMethodMatcher("time.Time Sub(..)")
	mt := &java.JavaTypeMethod{
		DeclaringType: &java.JavaTypeClass{FullyQualifiedName: "time.Time"},
		Name:          "Sub",
	}
	assert.True(t, mm.MatchesMethod(mt), "expected match for time.Time Sub(..)")
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

	assert.Equal(t, 1, printlnCount, "expected 1 Println match")
	assert.Equal(t, 1, sprintfCount, "expected 1 Sprintf match")
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

	assert.Equal(t, 0, count, "expected 0 matches for fmt.Println in log code")
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
