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

package format

import (
	goparser "go/parser"
	gotoken "go/token"
	"reflect"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TestFormatsTreeWithNoWhitespace strips every Space from a parsed tree and
// requires the formatter to rebuild valid Go from it. A recipe splicing in a
// synthesized node leaves exactly this: elements whose prefix nothing has set.
func TestFormatsTreeWithNoWhitespace(t *testing.T) {
	sources := map[string]string{
		"declarations":     "package p\n\nimport \"fmt\"\n\nvar x int = 1\n\nconst y = 2\n\ntype T struct {\n\tName string\n\tN    int\n}\n\nfunc F(a int, b string) (int, error) {\n\treturn a, nil\n}\n",
		"statements":       "package p\n\nfunc F(xs []int) int {\n\tvar total int\n\tfor _, x := range xs {\n\t\ttotal += x\n\t}\n\tif total > 0 {\n\t\treturn total\n\t} else if total == 0 {\n\t\treturn 0\n\t}\n\tgo F(xs)\n\tdefer F(xs)\n\treturn -total\n}\n",
		"switch and types": "package p\n\ntype I interface {\n\tM() int\n}\n\nfunc F(i int, c chan int) {\n\tswitch i {\n\tcase 1:\n\t\treturn\n\tdefault:\n\t}\n\tvar ch chan int = c\n\tvar m map[string]int\n\t_, _ = ch, m\n}\n",
		"generics":         "package p\n\nfunc Map[T any, U any](in []T, f func(T) U) []U {\n\tvar out []U\n\tfor _, v := range in {\n\t\tout = append(out, f(v))\n\t}\n\treturn out\n}\n",
	}

	for name, src := range sources {
		t.Run(name, func(t *testing.T) {
			p := parser.NewGoParser()
			p.ParseOnly = true
			cu, err := p.Parse("t.go", src)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			stripped := stripAllSpaces(reflect.ValueOf(cu)).Interface().(java.Tree)

			got := runAutoFormat(stripped)
			if _, err := goparser.ParseFile(gotoken.NewFileSet(), "t.go", got, goparser.ParseComments); err != nil {
				t.Fatalf("output does not parse: %v\n%s", err, got)
			}
			if before, after := codeTokens(src), codeTokens(got); before != after {
				t.Errorf("token stream changed\n  before %s\n  after  %s", before, after)
			}
		})
	}
}

// stripAllSpaces returns v with every Space replaced by the empty one.
func stripAllSpaces(v reflect.Value) reflect.Value {
	d := shapeOf(v.Type())
	if !d.carriesSpace {
		return v
	}
	if d.isSpace {
		return reflect.ValueOf(java.Space{})
	}
	if d.isMarkers {
		return v
	}
	switch d.kind {
	case reflect.Interface:
		if v.IsNil() {
			return v
		}
		boxed := reflect.New(v.Type()).Elem()
		boxed.Set(stripAllSpaces(v.Elem()))
		return boxed
	case reflect.Pointer:
		if v.IsNil() {
			return v
		}
		elem := stripAllSpaces(v.Elem())
		p := reflect.New(elem.Type())
		p.Elem().Set(elem)
		return p
	case reflect.Struct:
		out := reflect.New(v.Type()).Elem()
		out.Set(v)
		for _, i := range d.spaceFields {
			out.Field(i).Set(stripAllSpaces(v.Field(i)))
		}
		return out
	case reflect.Slice:
		out := reflect.MakeSlice(v.Type(), v.Len(), v.Len())
		reflect.Copy(out, v)
		for i := 0; i < v.Len(); i++ {
			out.Index(i).Set(stripAllSpaces(v.Index(i)))
		}
		return out
	default:
		return v
	}
}
