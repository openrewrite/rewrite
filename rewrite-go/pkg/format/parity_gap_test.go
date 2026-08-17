//go:build gofmtaudit

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
	"go/build"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// parityFloor is the number of standard library files the hand-rolled
// visitors render exactly as gofmt does. Raise it whenever a change earns
// more; it never goes down.
const parityFloor = 2099

// TestParityGap measures how far the hand-rolled visitors are from gofmt,
// using gofmtSource as the oracle, and holds the result at parityFloor. The
// per-file counts are the reliable figure; the per-kind line counts are
// indicative only, since one line of drift misaligns every comparison after it.
func TestParityGap(t *testing.T) {
	var contexts []build.Context
	for _, pair := range [][2]string{
		{"darwin", "arm64"}, {"linux", "amd64"}, {"windows", "amd64"},
		{"js", "wasm"}, {"plan9", "386"}, {"linux", "riscv64"}, {"aix", "ppc64"},
	} {
		c := build.Default
		c.GOOS, c.GOARCH = pair[0], pair[1]
		c.CgoEnabled = true
		contexts = append(contexts, c)
	}

	root := filepath.Join(runtime.GOROOT(), "src")
	var files []string
	filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err == nil && !info.IsDir() && strings.HasSuffix(path, ".go") && !strings.Contains(path, "/testdata/") {
			files = append(files, path)
		}
		return nil
	})

	strip := regexp.MustCompile(`(?m)^[ \t]+`)
	buckets := map[string]int{}
	examples := map[string]string{}
	var considered, exact, exactIgnoringIndent int
	var indentOnly []string

	for _, f := range files {
		content, err := os.ReadFile(f)
		if err != nil {
			continue
		}
		var ctx *build.Context
		for i := range contexts {
			if parser.MatchBuildContext(contexts[i], filepath.Base(f), string(content)) {
				ctx = &contexts[i]
				break
			}
		}
		if ctx == nil {
			continue
		}
		mangled := strip.ReplaceAllString(string(content), "")

		p := parser.NewGoParserWithBuildContext(*ctx)
		p.ParseOnly = true
		cu, err := p.Parse(filepath.Base(f), mangled)
		if err != nil || printer.Print(cu) != mangled {
			continue
		}
		want, err := gofmtSource(filepath.Base(f), mangled)
		if err != nil {
			continue
		}

		v := NewAutoFormatVisitor(nil)
		out := v.Visit(cu, nil)
		formatted := visitor.DrainAfterVisits(v, out.(java.Tree), nil)
		got := printer.Print(formatted)

		considered++
		if got == want {
			exact++
			continue
		}
		if sameIgnoringIndent(got, want) {
			exactIgnoringIndent++
			if len(indentOnly) < 8 {
				g, w := strings.Split(got, "\n"), strings.Split(want, "\n")
				for i := range g {
					if g[i] != w[i] {
						ctx := ""
						if i > 0 {
							ctx = "\n         prev |" + w[i-1] + "|"
						}
						indentOnly = append(indentOnly, filepath.Base(f)+ctx+
							"\n         want |"+w[i]+"|\n         got  |"+g[i]+"|")
						break
					}
				}
			}
		}
		g, w := strings.Split(got, "\n"), strings.Split(want, "\n")
		for i := 0; i < len(g) && i < len(w); i++ {
			if g[i] == w[i] {
				continue
			}
			kind, detail := classifyLine(g[i], w[i])
			buckets[kind]++
			if _, seen := examples[kind]; !seen {
				examples[kind] = f + "\n         " + detail
			}
		}
	}

	keys := make([]string, 0, len(buckets))
	for k := range buckets {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return buckets[keys[i]] > buckets[keys[j]] })

	t.Logf("considered=%d exactlyGofmt=%d wouldMatchIfIndentWereFixed=%d differ=%d",
		considered, exact, exact+exactIgnoringIndent, considered-exact)
	t.Logf("differing lines by kind:")
	for _, k := range keys {
		t.Logf("  %5d  %s\n         e.g. %s", buckets[k], k, examples[k])
	}

	t.Logf("indent-only mismatches:")
	for _, e := range indentOnly {
		t.Logf("  %s", e)
	}

	if exact < parityFloor {
		t.Errorf("parity regressed: %d files render exactly as gofmt, floor is %d", exact, parityFloor)
	}
}

var (
	spaceRun    = regexp.MustCompile(`[ \t]+`)
	commentLine = regexp.MustCompile(`^\s*(//|/\*|\*)`)
)

// classifyLine names one differing line.
func classifyLine(g, w string) (string, string) {
	detail := "want |" + w + "|\n         got  |" + g + "|"
	switch {
	case strings.TrimSpace(g) == "" || strings.TrimSpace(w) == "":
		return "blank lines", detail
	case commentLine.MatchString(w) || commentLine.MatchString(g):
		if collapse(g) == collapse(w) && indentOf(g) != indentOf(w) {
			return "comment indentation", detail
		}
		return "comment layout", detail
	case collapse(g) == collapse(w):
		if indentOf(g) != indentOf(w) {
			return "indentation", detail
		}
		return "intra-line alignment", detail
	default:
		return "token spacing", detail
	}
}

// sameIgnoringIndent reports whether the two renderings agree once every
// line's leading whitespace is discarded.
func sameIgnoringIndent(got, want string) bool {
	g, w := strings.Split(got, "\n"), strings.Split(want, "\n")
	if len(g) != len(w) {
		return false
	}
	for i := range g {
		if strings.TrimLeft(g[i], " \t") != strings.TrimLeft(w[i], " \t") {
			return false
		}
	}
	return true
}

func collapse(s string) string { return spaceRun.ReplaceAllString(strings.TrimSpace(s), " ") }

func indentOf(s string) string { return s[:len(s)-len(strings.TrimLeft(s, " \t"))] }
