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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// --- TypeUtils tests ---

func TestGetFullyQualifiedName(t *testing.T) {
	tests := []struct {
		name string
		typ  tree.JavaType
		want string
	}{
		{"nil", nil, ""},
		{"class", &tree.JavaTypeClass{FullyQualifiedName: "fmt.Stringer"}, "fmt.Stringer"},
		{"primitive", &tree.JavaTypePrimitive{Keyword: "int"}, "int"},
		{"parameterized", &tree.JavaTypeParameterized{Type: &tree.JavaTypeClass{FullyQualifiedName: "map"}}, "map"},
		{"unknown", &tree.JavaTypeUnknown{}, ""},
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
	cls := &tree.JavaTypeClass{FullyQualifiedName: "time.Time"}
	if !IsOfClassType(cls, "time.Time") {
		t.Error("expected true for exact match")
	}
	if IsOfClassType(cls, "time.Duration") {
		t.Error("expected false for different type")
	}
}

func TestIsAssignableTo(t *testing.T) {
	stringer := &tree.JavaTypeClass{FullyQualifiedName: "fmt.Stringer"}
	myType := &tree.JavaTypeClass{
		FullyQualifiedName: "main.MyType",
		Interfaces:         []*tree.JavaTypeClass{stringer},
	}

	if !IsAssignableTo(myType, "main.MyType") {
		t.Error("type should be assignable to itself")
	}
	if !IsAssignableTo(myType, "fmt.Stringer") {
		t.Error("type should be assignable to its interface")
	}
	if IsAssignableTo(myType, "io.Reader") {
		t.Error("type should not be assignable to unrelated interface")
	}
}

func TestIsError(t *testing.T) {
	errType := &tree.JavaTypeClass{FullyQualifiedName: "error"}
	if !IsError(errType) {
		t.Error("expected true for error type")
	}
	if IsError(&tree.JavaTypeClass{FullyQualifiedName: "string"}) {
		t.Error("expected false for non-error type")
	}
}

func TestIsString(t *testing.T) {
	if !IsString(&tree.JavaTypePrimitive{Keyword: "String"}) {
		t.Error("expected true for String primitive")
	}
	if IsString(&tree.JavaTypePrimitive{Keyword: "int"}) {
		t.Error("expected false for int")
	}
}

func TestAsClass(t *testing.T) {
	cls := &tree.JavaTypeClass{FullyQualifiedName: "foo.Bar"}
	if AsClass(cls) != cls {
		t.Error("AsClass should return the class directly")
	}
	param := &tree.JavaTypeParameterized{Type: cls}
	if AsClass(param) != cls {
		t.Error("AsClass should unwrap parameterized types")
	}
	if AsClass(&tree.JavaTypePrimitive{Keyword: "int"}) != nil {
		t.Error("AsClass should return nil for non-class types")
	}
}

// --- MethodMatcher tests ---

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
		if got != tt.want {
			t.Errorf("globToRegexp(%q).MatchString(%q) = %v, want %v", tt.pattern, tt.input, got, tt.want)
		}
	}
}

func TestMethodMatcherParsing(t *testing.T) {
	mm := NewMethodMatcher("fmt Sprintf(string, ..)")
	if mm.declaringTypePattern == nil {
		t.Fatal("expected declaringTypePattern")
	}
	if mm.methodNamePattern == nil {
		t.Fatal("expected methodNamePattern")
	}
	if !mm.matchesAnyArgs {
		t.Error("expected matchesAnyArgs for .. pattern")
	}
}

func TestMethodMatcherMatchesAnyArgs(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &tree.MethodInvocation{
		Select: &tree.RightPadded[tree.Expression]{
			Element: &tree.Identifier{Name: "fmt"},
		},
		Name: &tree.Identifier{Name: "Println"},
	}
	if !mm.Matches(mi) {
		t.Error("expected match for fmt.Println with any args")
	}
}

func TestMethodMatcherNoMatchWrongName(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &tree.MethodInvocation{
		Select: &tree.RightPadded[tree.Expression]{
			Element: &tree.Identifier{Name: "fmt"},
		},
		Name: &tree.Identifier{Name: "Sprintf"},
	}
	if mm.Matches(mi) {
		t.Error("expected no match for wrong method name")
	}
}

func TestMethodMatcherNoMatchWrongPackage(t *testing.T) {
	mm := NewMethodMatcher("fmt Println(..)")
	mi := &tree.MethodInvocation{
		Select: &tree.RightPadded[tree.Expression]{
			Element: &tree.Identifier{Name: "log"},
		},
		Name: &tree.Identifier{Name: "Println"},
	}
	if mm.Matches(mi) {
		t.Error("expected no match for wrong package")
	}
}

func TestMethodMatcherWildcardType(t *testing.T) {
	mm := NewMethodMatcher("* Sub(..)")
	mi := &tree.MethodInvocation{
		Select: &tree.RightPadded[tree.Expression]{
			Element: &tree.Identifier{Name: "t"},
		},
		Name: &tree.Identifier{Name: "Sub"},
	}
	// With just an identifier "t" as select, DeclaringTypeFQN returns "t"
	// which matches "*" pattern
	if !mm.Matches(mi) {
		t.Error("expected match for wildcard declaring type")
	}
}

func TestMethodMatcherWithTypeInfo(t *testing.T) {
	mm := NewMethodMatcher("fmt Sprintf(..)")
	fmtType := &tree.JavaTypeClass{FullyQualifiedName: "fmt"}
	mi := &tree.MethodInvocation{
		Select: &tree.RightPadded[tree.Expression]{
			Element: &tree.Identifier{Name: "fmt"},
		},
		Name: &tree.Identifier{Name: "Sprintf"},
		MethodType: &tree.JavaTypeMethod{
			DeclaringType: fmtType,
			Name:          "Sprintf",
		},
	}
	if !mm.Matches(mi) {
		t.Error("expected match with type info")
	}
}

func TestMethodMatcherMatchesMethod(t *testing.T) {
	mm := NewMethodMatcher("time.Time Sub(..)")
	mt := &tree.JavaTypeMethod{
		DeclaringType: &tree.JavaTypeClass{FullyQualifiedName: "time.Time"},
		Name:          "Sub",
	}
	if !mm.MatchesMethod(mt) {
		t.Error("expected match for time.Time Sub(..)")
	}
}

// --- Integration test: MethodMatcher against parsed Go code ---

func TestMethodMatcherOnParsedCode(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "fmt"

func main() {
	fmt.Println("hello")
	fmt.Sprintf("%d", 42)
}
`)
	if err != nil {
		t.Fatal(err)
	}

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

	if printlnCount != 1 {
		t.Errorf("expected 1 Println match, got %d", printlnCount)
	}
	if sprintfCount != 1 {
		t.Errorf("expected 1 Sprintf match, got %d", sprintfCount)
	}
}

func TestMethodMatcherNoMatchOnParsedCode(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "log"

func main() {
	log.Println("hello")
}
`)
	if err != nil {
		t.Fatal(err)
	}

	fmtPrintln := NewMethodMatcher("fmt Println(..)")

	var count int
	v := visitor.Init(&methodMatcherVisitor{
		matchers: map[string]*MethodMatcher{"match": fmtPrintln},
		counts:   map[string]*int{"match": &count},
	})
	v.Visit(cu, nil)

	if count != 0 {
		t.Errorf("expected 0 matches for fmt.Println in log code, got %d", count)
	}
}

// Test helper visitor that counts method matcher hits.
type methodMatcherVisitor struct {
	visitor.GoVisitor
	matchers map[string]*MethodMatcher
	counts   map[string]*int
}

func (v *methodMatcherVisitor) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	mi = v.GoVisitor.VisitMethodInvocation(mi, p).(*tree.MethodInvocation)
	for name, matcher := range v.matchers {
		if matcher.Matches(mi) {
			*v.counts[name]++
		}
	}
	return mi
}
