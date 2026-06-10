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

package printer_test

import (
	"fmt"
	"reflect"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// This mirrors rewrite-javascript's whitespace-attachment.test.ts: leading
// whitespace must be attached to the outermost LST element rather than leaking
// onto the prefix of a node's first child. If a node emits nothing of its own
// before its first child, and that child carries a non-empty whitespace prefix,
// the whitespace should have been attached to the parent instead.
//
// The JS test subclasses the printer to override the virtual beforeSyntax /
// afterSyntax hooks and build a node tree. The Go printer routes prefixes
// through per-method beforeSyntax calls that are not virtually dispatched, but
// PreVisit *is* dispatched via the Self field and fires for every node right
// before it emits its prefix. Recording the output-buffer length at each
// PreVisit (the node's start offset) lets us reconstruct, for every parent,
// whether it emitted anything before its first child — which is exactly what
// the JS test checks by looking at children[0].

// captureNode is one node in the reconstructed print tree.
type captureNode struct {
	kind             string
	startOffset      int    // output length when this node started printing
	prefixWhitespace string // the node's own prefix whitespace, emitted first
	hasPrefix        bool   // whether the node is a J carrying a prefix
	children         []*captureNode
}

// treeCapturingPrinter embeds the real GoPrinter and overrides PreVisit to
// record the print tree without altering any printing behavior.
type treeCapturingPrinter struct {
	printer.GoPrinter
	byTree map[java.Tree]*captureNode
	roots  []*captureNode
}

func (p *treeCapturingPrinter) PreVisit(t java.Tree, param any) java.Tree {
	out := param.(*printer.PrintOutputCapture)

	node := &captureNode{
		kind:        prettifyKind(t),
		startOffset: len(out.String()),
	}
	if j, ok := t.(java.J); ok {
		node.hasPrefix = true
		node.prefixWhitespace = j.GetPrefix().Whitespace
	}
	p.byTree[t] = node

	if parent := p.Cursor().Parent(); parent != nil && parent.Value() != nil {
		if pn, ok := p.byTree[parent.Value()]; ok {
			pn.children = append(pn.children, node)
		} else {
			p.roots = append(p.roots, node)
		}
	} else {
		p.roots = append(p.roots, node)
	}

	return t
}

// prettifyKind renders a node's type as "package.TypeName" (e.g.
// "java.MethodInvocation", "golang.Composite").
func prettifyKind(t java.Tree) string {
	name := reflect.TypeOf(t).String()
	return strings.TrimPrefix(name, "*")
}

func findWhitespaceViolations(roots []*captureNode) []string {
	var violations []string

	var check func(n *captureNode)
	check = func(n *captureNode) {
		if len(n.children) > 0 {
			first := n.children[0]
			// first.startOffset == n.startOffset means the parent emitted
			// nothing before its first child — i.e. the child is the parent's
			// children[0] (equivalent to firstChild being a node in the JS test).
			if first.startOffset == n.startOffset && first.hasPrefix {
				ws := first.prefixWhitespace
				if ws != "" && strings.TrimSpace(ws) == "" {
					violations = append(violations, fmt.Sprintf(
						"%s has child %s starting with whitespace |%s|. "+
							"The whitespace should rather be attached to %s.",
						n.kind, first.kind, ws, n.kind))
				}
			}
		}
		for _, c := range n.children {
			check(c)
		}
	}

	for _, r := range roots {
		check(r)
	}
	return violations
}

func TestWhitespaceAttachedToOutermostElement(t *testing.T) {
	sources := []string{
		"package main\n\nfunc f() int { return 1 + 2 }\n",
		"package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println(\"hi\") }\n",
		"package main\n\ntype Point struct {\n\tX int\n\tY int\n}\n",
		"package main\n\ntype Stringer interface {\n\tString() string\n}\n",
		"package main\n\nfunc m() {\n\tfor i := 0; i < 10; i++ {\n\t}\n}\n",
		"package main\n\nfunc s(x int) string {\n\tswitch x {\n\tcase 1:\n\t\treturn \"one\"\n\tdefault:\n\t\treturn \"other\"\n\t}\n}\n",
		"package main\n\nfunc g() {\n\tmm := map[string]int{\"a\": 1}\n\t_ = mm\n}\n",
		"package main\n\nfunc Max[T int | float64](a, b T) T {\n\tif a > b {\n\t\treturn a\n\t}\n\treturn b\n}\n",
		"package main\n\nfunc h() {\n\tch := make(chan int)\n\tgo func() { ch <- 1 }()\n\tdefer close(ch)\n}\n",
		"package main\n\nfunc sl() {\n\txs := []int{1, 2, 3}\n\t_ = xs[1:2]\n}\n",
		"package main\n\nfunc p() {\n\tvar x int = 5\n\ty := &x\n\t_ = *y\n}\n",
		"package main\n\ntype T struct{}\n\nfunc (t *T) Do() {}\n",
	}

	for _, source := range sources {
		t.Run(source, func(t *testing.T) {
			// given
			cu, err := parser.NewGoParser().Parse("test.go", source)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			capturing := visitor.Init(&treeCapturingPrinter{
				byTree: make(map[java.Tree]*captureNode),
			})
			out := printer.NewPrintOutputCaptureWithMarkers(printer.SanitizedMarkerPrinter)

			// when
			capturing.Visit(cu, out)

			// then
			violations := findWhitespaceViolations(capturing.roots)
			if len(violations) > 0 {
				t.Errorf("expected no whitespace-attachment violations, got %d:\n%s",
					len(violations), strings.Join(violations, "\n"))
			}
		})
	}
}
