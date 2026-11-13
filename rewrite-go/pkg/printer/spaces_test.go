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
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// numberedSpaces adds a comment to every space a visitor reaches, recording
// the node that holds each one.
type numberedSpaces struct {
	visitor.GoVisitor
	holders []string
}

var numberedComment = regexp.MustCompile(`/\*s(\d+)s\*/`)

func (v *numberedSpaces) VisitSpace(space java.Space, p any) java.Space {
	holder := "?"
	if c := v.Cursor(); c != nil {
		holder = fmt.Sprintf("%T", c.Value())
	}
	n := len(v.holders)
	v.holders = append(v.holders, holder)
	comments := append(append([]java.Comment{}, space.Comments()...),
		java.Comment{Multiline: true, Text: "s" + strconv.Itoa(n) + "s"})
	return java.MakeSpace(comments, space.Whitespace())
}

const everySpace = `package main

import (
	"fmt"
	str "strings"
)

var x, y int = 1, 2

type T struct {
	A, B int ` + "`json:\"a\"`" + `
}

func (t *T) M(a, b int, rest ...string) (n int, err error) {
	var arr [3]int
	var s []string
	a, b = b, a
	if v := 1; v > 0 {
		return v, nil
	} else if a > b {
		a++
	} else {
		b--
	}
	for i := 0; i < 3; i++ {
		arr[i] = i
	}
	for a < b {
		a++
	}
	for {
		break
	}
	for k, v := range s {
		fmt.Println(k, v, str.ToUpper(v))
	}
	switch z := a; z {
	case 1, 2:
		b = 1
	default:
		b = 2
	}
	return 0, nil
}
`

// A recipe may put whitespace where the parser never does, and it is printed
// wherever a visitor can put it.
func TestEverySpaceAVisitorReachesIsPrinted(t *testing.T) {
	// the corpus, and this module's own sources for the breadth of syntax in them
	sources := map[string]string{"spaces.go": everySpace}
	for _, dir := range []string{
		filepath.Join("..", "..", "test", "testdata", "printer-corpus"),
		filepath.Join("..", "..", "pkg"),
		filepath.Join("..", "..", "cmd"),
	} {
		require.NoError(t, filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
			if err == nil && strings.HasSuffix(path, ".go") {
				content, readErr := os.ReadFile(path)
				sources[path], err = string(content), readErr
			}
			return err
		}))
	}
	require.Greater(t, len(sources), 100)

	for name, source := range sources {
		t.Run(name, func(t *testing.T) {
			cu, err := parser.NewGoParser().Parse(filepath.Base(name), source)
			require.NoError(t, err)
			assert.Empty(t, spacesNotPrintedOnce(cu))
		})
	}
}

func TestEverySpaceOfGoModAndGoSumIsPrinted(t *testing.T) {
	gm, err := parser.ParseGoModFile("go.mod", "// header\nmodule example.com/m\n\nrequire (\n"+
		"\texample.com/a v1.0.0 // indirect\n)\n\nreplace example.com/a => ../a\n")
	require.NoError(t, err)
	gs, err := parser.ParseGoSumFile("go.sum", "example.com/a v1.0.0 h1:abc=\nexample.com/a v1.0.0/go.mod h1:def=\n")
	require.NoError(t, err)

	assert.Empty(t, spacesNotPrintedOnce(gm))
	assert.Empty(t, spacesNotPrintedOnce(gs))
}

func spacesNotPrintedOnce(tree java.Tree) []string {
	v := visitor.Init(&numberedSpaces{})
	printed := printer.Print(v.Visit(tree, nil))

	counts := make([]int, len(v.holders))
	for _, numbered := range numberedComment.FindAllStringSubmatch(printed, -1) {
		n, _ := strconv.Atoi(numbered[1])
		counts[n]++
	}
	var notOnce []string
	if len(counts) == 0 {
		notOnce = append(notOnce, "the visitor reached no space")
	}
	for n, count := range counts {
		if count != 1 {
			notOnce = append(notOnce, fmt.Sprintf("%d in %s printed %d times", n, v.holders[n], count))
		}
	}
	return notOnce
}
