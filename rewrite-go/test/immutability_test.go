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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// renamer changes every identifier named "x" to "y" — a minimal change-visit.
type renamer struct{ visitor.GoVisitor }

func (r *renamer) VisitIdentifier(id *java.Identifier, p any) java.J {
	if id.Name == "x" {
		return id.WithName("y")
	}
	return r.GoVisitor.VisitIdentifier(id, p)
}

// immutabilitySamples exercises a broad spread of node types.
var immutabilitySamples = map[string]string{
	"assign-if-unary": "package p\n\nfunc f() {\n\tx := 1\n\tif x == 1 {\n\t\tx = 2\n\t}\n\t_ = &x\n}\n",
	"imports":         "package p\n\nimport \"fmt\"\n\nfunc f() { fmt.Println(\"hi\") }\n",
	"struct-method":   "package p\n\ntype T struct{ A int }\n\nfunc (t T) M() int { return t.A }\n",
	"for-range-chan":  "package p\n\nfunc f(ch <-chan int) {\n\tfor i := 0; i < 3; i++ {\n\t\t_ = <-ch\n\t}\n}\n",
	"composite":       "package p\n\ntype T struct{ A int }\n\nfunc f() { _ = T{A: 1} }\n",
}

// TestVisitDoesNotMutateOriginal is the immutability invariant: a change-visit
// must not mutate the shared input tree in place. With identity-preserving
// withX, any VisitX that assigns to a receiver field directly would corrupt
// the original — this guards against that.
func TestVisitDoesNotMutateOriginal(t *testing.T) {
	for name, src := range immutabilitySamples {
		cu, err := parser.NewGoParser().Parse("p.go", src)
		if err != nil {
			t.Fatalf("%s: parse: %v", name, err)
		}
		before := printer.Print(cu)
		r := &renamer{}
		visitor.Init(r)
		r.Visit(cu, nil)
		if after := printer.Print(cu); after != before {
			t.Errorf("%s: ORIGINAL tree mutated in place\n--- was ---\n%s\n--- now ---\n%s", name, before, after)
		}
	}
}

// TestNoOpVisitPreservesIdentity is the identity invariant: a visit that
// changes nothing must return the SAME pointer (so the engine's change
// detection reports "unchanged"). This is what keeps untouched files out of a
// recipe's results/patch.
func TestNoOpVisitPreservesIdentity(t *testing.T) {
	for name, src := range immutabilitySamples {
		cu, err := parser.NewGoParser().Parse("p.go", src)
		if err != nil {
			t.Fatalf("%s: parse: %v", name, err)
		}
		v := &visitor.GoVisitor{}
		visitor.Init(v)
		got := v.Visit(cu, nil)
		if got != java.Tree(cu) {
			t.Errorf("%s: no-op visit did not preserve identity (returned a new pointer)", name)
		}
	}
}
