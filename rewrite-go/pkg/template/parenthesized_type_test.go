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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type conversionCollector struct {
	visitor.GoVisitor
	found []java.J
}

func (c *conversionCollector) PreVisit(t java.Tree, p any) java.Tree {
	if tc, ok := t.(*java.TypeCast); ok {
		c.found = append(c.found, tc)
	}
	return t
}

// A repeated capture `#{a}` … `#{a}` holds only where two source-identical
// nodes compare equal.
func TestStructurallyEqualAcceptsAParenthesizedConversionType(t *testing.T) {
	for _, tc := range []struct{ name, conversion string }{
		{"pointer", "(*T)(p)"},
		{"func", "(func())(fn)"},
		{"channel", "(<-chan int)(rc)"},
		{"map", "(map[string]int)(m)"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			cu, err := parser.NewGoParser().Parse("test.go",
				"package main\n\ntype T struct{}\n\nfunc g(a, b any) {}\n\n"+
					"func f(p *T, fn func(), rc <-chan int, m map[string]int) {\n"+
					"\tg("+tc.conversion+", "+tc.conversion+")\n}\n")
			if err != nil {
				t.Fatal(err)
			}
			c := visitor.Init(&conversionCollector{})
			c.Visit(cu, nil)
			if len(c.found) != 2 {
				t.Fatalf("expected 2 conversions, got %d", len(c.found))
			}
			if !structurallyEqual(c.found[0], c.found[1]) {
				t.Errorf("two %s conversions compare unequal", tc.conversion)
			}
		})
	}
}
