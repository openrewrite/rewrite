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
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// nestingCorpus exercises each construct that moves the indent level: blocks,
// switch and select clauses, wrapped list elements and wrapped operands.
const nestingCorpus = `package main

import (
	"fmt"
	"strings"
)

var (
	alpha = 1
	beta  = 2
)

type shape struct {
	sides int
}

func nested(n int) int {
	total := 0
	for i := 0; i < n; i++ {
		if i%2 == 0 {
			switch i {
			case 0:
				total += 1
			default:
				total += 2
			}
		} else {
			func() {
				total += 3
			}()
		}
	}
	return total
}

func wrapped(n int) string {
	parts := []string{
		"a",
		"b",
	}
	joined := strings.Join(parts, ",") +
		fmt.Sprint(n) +
		"tail"
	s := shape{
		sides: n,
	}
	ch := make(chan int, 1)
	ch <- s.sides
	select {
	case v := <-ch:
		joined += fmt.Sprint(v)
	}
	return fmt.Sprintf(
		"%s-%d",
		joined,
		alpha+beta,
	)
}
`

// nodeWalk records every node together with the cursor of its parent, which is
// the cursor a caller formatting that node on its own would have.
type nodeWalk struct {
	visitor.GoVisitor
	nodes   []java.Tree
	parents []*visitor.Cursor
}

func (w *nodeWalk) Visit(t java.Tree, p any) java.Tree {
	if t != nil {
		w.nodes = append(w.nodes, t)
		w.parents = append(w.parents, w.Cursor())
	}
	return w.GoVisitor.Visit(t, p)
}

func walkNodes(root java.Tree) *nodeWalk {
	w := visitor.Init(&nodeWalk{})
	w.Visit(root, nil)
	return w
}

// printBody renders a node with its own prefix cleared. A node's leading
// whitespace is set by whoever places it, not by formatting it, so it is not
// part of what the two paths have to agree on.
func printBody(t java.Tree) string {
	return printer.Print(transformPrefix(t, func(java.Space) java.Space { return java.Space{} }))
}

func TestSubtreeIndentMatchesWholeFile(t *testing.T) {
	assertSubtreeIndentMatches(t, "nesting.go", nestingCorpus)
}

func TestSubtreeIndentMatchesWholeFileOutsideLists(t *testing.T) {
	assertSubtreeIndentMatches(t, "outside.go", outsideListCorpus)
}

const outsideListCorpus = `package main

import "strings"

func c(y string) {
	_ = strings.NewReplacer(
		"a",
		"b",
	).Replace(
		y,
	)
	_ = struct {
		a int
		b int
	}{
		a: 1,
		b: 2,
	}
}
`

func TestSubtreeIndentMatchesWholeFileOnStdlib(t *testing.T) {
	for _, rel := range []string{
		"net/http/cookie.go",
		"strings/replace.go",
		"encoding/json/encode.go",
		"go/printer/nodes.go",
	} {
		t.Run(rel, func(t *testing.T) {
			content, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", rel))
			if err != nil {
				t.Skip(err)
			}
			assertSubtreeIndentMatches(t, filepath.Base(rel), string(content))
		})
	}
}

// assertSubtreeIndentMatches checks the invariant the seeded cursor exists for:
// formatting a node on its own, given the cursor of its parent, indents it
// exactly as formatting the whole file does.
func assertSubtreeIndentMatches(t *testing.T, path, src string) {
	t.Helper()
	cu, err := parser.NewGoParser().Parse(path, src)
	if err != nil {
		t.Fatal(err)
	}

	whole := NewTabsAndIndentsVisitor(nil).Visit(cu, nil)

	original := walkNodes(cu)
	formatted := walkNodes(whole)
	if len(original.nodes) != len(formatted.nodes) {
		t.Fatalf("indenting changed the tree shape: %d nodes before, %d after",
			len(original.nodes), len(formatted.nodes))
	}

	for i, node := range original.nodes {
		v := NewTabsAndIndentsVisitor(nil)
		v.SetCursor(original.parents[i])
		alone := v.Visit(node, nil)
		if alone == nil {
			continue
		}
		if got, want := printBody(alone), printBody(formatted.nodes[i]); got != want {
			t.Errorf("node %d (%T) indents differently on its own\n  alone: %q\n  whole: %q",
				i, node, got, want)
		}
	}
}
