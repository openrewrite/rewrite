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

package rpc

import (
	"reflect"
	"regexp"
	"slices"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

var javaVisitorSources = []string{
	"../../../rewrite-java/src/main/java/org/openrewrite/java/JavaVisitor.java",
	"../../src/main/java/org/openrewrite/golang/GolangVisitor.java",
}

var (
	javaMethodStart = regexp.MustCompile(`(?m)^    (?:public|protected|private) [^\n]*\(`)
	javaVisitMethod = regexp.MustCompile(`^    public (?:<[^>]+> )?[\w.<>?]+ visit\w+\((J|Go)\.([\w.]+)(?:<[^>]*>)? \w+, P p\)`)
	javaTreeOuter   = map[string]string{"J": "org.openrewrite.java.tree.J", "Go": "org.openrewrite.golang.tree.Go"}
)

// javaHooks maps each class a Java visit method takes, by its binary name, to
// the hooks among visitStatement and visitExpression that the method calls.
func javaHooks(t *testing.T) map[string]map[string]bool {
	hooks := map[string]map[string]bool{}
	for _, path := range javaVisitorSources {
		source := readJavaSource(t, path)
		starts := javaMethodStart.FindAllStringIndex(source, -1)
		for i, start := range starts {
			end := len(source)
			if i+1 < len(starts) {
				end = starts[i+1][0]
			}
			body := source[start[0]:end]
			m := javaVisitMethod.FindStringSubmatch(body)
			if m == nil {
				continue
			}
			class := javaTreeOuter[m[1]] + "$" + strings.ReplaceAll(m[2], ".", "$")
			if hooks[class] == nil {
				hooks[class] = map[string]bool{}
			}
			for _, hook := range []string{"Statement", "Expression"} {
				if strings.Contains(body, "visit"+hook+"(") {
					hooks[class][hook] = true
				}
			}
		}
	}
	return hooks
}

type hookRecorder struct {
	visitor.GoVisitor
	present map[reflect.Type]bool
	called  map[reflect.Type]map[string]bool
}

func (r *hookRecorder) PreVisit(t java.Tree, p any) java.Tree {
	r.present[reflect.TypeOf(t)] = true
	return t
}

func (r *hookRecorder) record(t java.J, hook string) {
	if r.called[reflect.TypeOf(t)] == nil {
		r.called[reflect.TypeOf(t)] = map[string]bool{}
	}
	r.called[reflect.TypeOf(t)][hook] = true
}

func (r *hookRecorder) VisitStatement(s java.Statement, p any) java.J {
	r.record(s, "Statement")
	return s
}

func (r *hookRecorder) VisitExpression(e java.Expression, p any) java.J {
	r.record(e, "Expression")
	return e
}

const dispatchSource = `package main

import "fmt"

type Number interface {
	~int | ~float64
}

type Pair[K comparable, V any] struct {
	Key K ` + "`json:\"key\"`" + `
	Val V
}

type S struct{}

var (
	arr  [3]int
	jarr []int
	ptrs map[string]*int
	ch   chan int
	fn   func(int) (int, error)
)

func (s *S) N() int { return 0 }

func Map2[A, B any]() {}

func (s *S) M(xs ...int) (int, error) {
	p := Pair[string, int]{Key: "k", Val: 1}
	_ = p
	g := Map2[int, string]
	_ = g
	_ = [...]int{1, 2}
	x, y := 1, 2
	x, y = y, x
	x += 1
	x &^= y
	_ = x + y
	_ = x &^ y
	_ = -x
	_ = &x
	_ = (x)
	_ = xs[1:2]
	_ = xs[0]
	_ = s.N
	_ = float64(x)
	var t *(int)
	_ = t
	var i any = x
	_ = i.(int)
	(s.N())
	fmt.Println(x)
	f := func() {}
	go f()
	defer f()
	ch <- 1
	select {
	case v := <-ch:
		_ = v
	}
	if z := x; z > 0 {
	} else {
	}
	for i := 0; i < 3; i++ {
		continue
	}
	for range xs {
		break
	}
	switch x {
	case 1:
		fallthrough
	default:
	}
	{
	}
	;
	goto End
End:
	if x > 0 {
		return x, nil
	}
	return 0, nil
}

func one() int {
	return 1
}
`

func TestGoVisitorCallsTheHooksJavaVisitorCalls(t *testing.T) {
	// given
	expected := javaHooks(t)
	cu, err := parser.NewGoParser().Parse("dispatch.go", dispatchSource)
	require.NoError(t, err)
	r := &hookRecorder{present: map[reflect.Type]bool{}, called: map[reflect.Type]map[string]bool{}}
	r.Self = r

	// when
	r.Visit(cu, nil)

	// then
	markers := map[string]reflect.Type{
		"Statement":  reflect.TypeOf((*java.Statement)(nil)).Elem(),
		"Expression": reflect.TypeOf((*java.Expression)(nil)).Elem(),
	}
	var mismatches []string
	for goType, javaClassName := range valueTypeMap {
		if !goType.Implements(markers["Statement"]) && !goType.Implements(markers["Expression"]) {
			continue
		}
		if !r.present[goType] {
			mismatches = append(mismatches, goType.String()+": not in the source")
			continue
		}
		hooks, ok := expected[javaClassName]
		if !ok {
			mismatches = append(mismatches, javaClassName+": no Java visit method")
			continue
		}
		for _, hook := range []string{"Statement", "Expression"} {
			if r.called[goType][hook] != hooks[hook] {
				mismatches = append(mismatches, goType.String()+": Visit"+hook+
					map[bool]string{true: " called", false: " not called"}[r.called[goType][hook]]+
					", Java "+map[bool]string{true: "calls", false: "does not call"}[hooks[hook]]+" visit"+hook)
			}
		}
	}
	slices.Sort(mismatches)
	assert.Empty(t, mismatches)
}
