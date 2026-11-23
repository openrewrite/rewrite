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
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// formattedCorpus is gofmt-clean source reaching each construct a format pass
// lays out, with comments where a pass rewrites the space around them.
const formattedCorpus = `// Package p is documented.
package p

import (
	"fmt" // why fmt
	"strings"
)

import "os"

// Doc comment for a var block.
var (
	a = 1
	b = "x"
)

const c, d = 1, 2

type (
	// T is documented.
	T struct {
		Name string ` + "`json:\"name\"`" + `
		N    int
	}
	I interface {
		M() int
	}
)

type G[K comparable, V any] map[K]V

type Ch chan<- int

func (t *T) Method(xs []int, fs ...func(int) int) (total int, err error) {
	for _, x := range xs {
		total += x &^ 1
	}
	if n := len(xs); n > 0 {
		return total, nil
	} else if n == 0 {
		return 0, fmt.Errorf("empty %s", os.Args[0])
	} else {
		total = -n
	}
	return
}

func F(x int, ch chan int, s []int, m map[string]int) int {
	switch y := x * 2; {
	// ahead of a case
	case y > 10,
		y < -10:
		// ahead of a statement
		return y
	default:
	}
	switch x {
	case 1:
		fmt.Println(x)
	next:
		x++
		if x < 3 {
			goto next
		}
		// before the closing brace
	}
	select {
	case v := <-ch:
		_ = v
	case ch <- 1:
	default:
	}
	go func() {
		defer close(ch)
	}()
	defer fmt.Println(
		"wrapped",
		x+1,
	)
	_ = s[1 : x+1]
	_ = s[x : x+1 : x+2]
	_ = s[x*2]
	p := &x
	_ = *p + (x - 1)
	_, _ = m["a"], strings.Repeat("a", x)
	total := x +
		1
	_ = []T{
		{Name: "a", N: 1},
		{Name: "b", N: 2}, // after the trailing comma
	}
	_ = float64(
		x,
	)
	var i interface{} = x
	switch v := i.(type) {
	case int:
		_ = v
	}
retry:
outer:
	for j := 0; j < 3; j++ {
		for {
			continue outer
		}
	}
	if total < 0 {
		goto retry
	}
	return total
}
`

func TestFormattingFormattedSourceReturnsTheTreeItWasGiven(t *testing.T) {
	formatted, err := gofmtSource("t.go", formattedCorpus)
	require.NoError(t, err)
	require.Equal(t, formatted, formattedCorpus, "the corpus is gofmt-clean")

	for _, pass := range []struct {
		name string
		run  func(java.Tree) java.Tree
	}{
		{"MinimumViableSpacingVisitor", func(t java.Tree) java.Tree { return NewMinimumViableSpacingVisitor(nil).Visit(t, nil) }},
		{"DocCommentVisitor", func(t java.Tree) java.Tree { return NewDocCommentVisitor(nil).Visit(t, nil) }},
		{"BinarySpacingVisitor", func(t java.Tree) java.Tree { return NewBinarySpacingVisitor(nil).Visit(t, nil) }},
		{"SpacesVisitor", func(t java.Tree) java.Tree { return NewSpacesVisitor(nil).Visit(t, nil) }},
		{"TabsAndIndentsVisitor", func(t java.Tree) java.Tree { return NewTabsAndIndentsVisitor(nil).Visit(t, nil) }},
		{"BlankLinesVisitor", func(t java.Tree) java.Tree { return NewBlankLinesVisitor(nil).Visit(t, nil) }},
		{"RemoveTrailingWhitespaceVisitor", func(t java.Tree) java.Tree {
			return NewRemoveTrailingWhitespaceVisitor(nil).Visit(t, nil)
		}},
		{"AutoFormat", func(t java.Tree) java.Tree { return AutoFormat(t, nil, nil, nil) }},
	} {
		t.Run(pass.name, func(t *testing.T) {
			// given
			cu, err := parser.NewGoParser().Parse("t.go", formattedCorpus)
			require.NoError(t, err)

			// when
			out := pass.run(cu)

			// then
			assert.Equal(t, formattedCorpus, printer.Print(out))
			if out != java.Tree(cu) {
				t.Errorf("returned a new tree, rebuilding a %s", innermostRebuilt(cu, out))
			}
		})
	}
}

// innermostRebuilt names the last node, in visiting order, that two trees of
// the same shape hold different copies of, which is where the copying began.
func innermostRebuilt(before, after java.Tree) string {
	b, a := walkNodes(before), walkNodes(after)
	for i := len(b.nodes) - 1; i >= 0; i-- {
		if i < len(a.nodes) && b.nodes[i] != a.nodes[i] {
			text := printer.Print(b.nodes[i])
			if len(text) > 60 {
				text = text[:60] + "..."
			}
			return fmt.Sprintf("%T %q", b.nodes[i], text)
		}
	}
	return "node"
}
