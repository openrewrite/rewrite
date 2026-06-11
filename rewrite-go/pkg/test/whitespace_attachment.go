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

// Whitespace-attachment enforcement: leading whitespace must be attached to
// the outermost LST element rather than leaking onto the prefix of a node's
// first child. If a node emits nothing of its own before its first child, and
// that child carries a non-empty whitespace prefix, the whitespace should have
// been attached to the parent instead. This mirrors rewrite-javascript's
// whitespace-attachment.test.ts and is enforced for every source flowing
// through RewriteRun. The capturing printer here is test-only.

import (
	"fmt"
	"reflect"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// captureNode is one node in the reconstructed print tree.
type captureNode struct {
	kind             string
	startOffset      int    // output length when this node started printing
	prefixWhitespace string // the node's own prefix whitespace, emitted first
	hasPrefix        bool   // whether the node is a J carrying a prefix
	children         []*captureNode
}

// treeCapturingPrinter embeds the real GoPrinter and overrides PreVisit to
// record the print tree without altering any printing behavior. PreVisit is
// dispatched via the Self field and fires for every node right before it emits
// its prefix; recording the output-buffer length at each PreVisit (the node's
// start offset) lets us reconstruct, for every parent, whether it emitted
// anything before its first child.
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
			// children[0].
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

// WhitespaceAttachmentViolations prints the given tree through a capturing
// printer and returns a violation message for every node whose first child
// carries leading whitespace that should have been attached to the node
// itself. Returns nil when the tree satisfies the whitespace-attachment
// objective.
func WhitespaceAttachmentViolations(root java.Tree) []string {
	capturing := visitor.Init(&treeCapturingPrinter{
		byTree: make(map[java.Tree]*captureNode),
	})
	out := printer.NewPrintOutputCaptureWithMarkers(printer.SanitizedMarkerPrinter)
	capturing.Visit(root, out)
	return findWhitespaceViolations(capturing.roots)
}
