//go:build gofmtaudit

package format

import (
	"go/build"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TestCanonicalInputUnchanged runs the formatter over gofmt-clean sources,
// where the layout is already canonical and every pass must leave it alone.
// Anything rewritten here is a disagreement with gofmt, as distinct from the
// parity gap, which also counts layout the formatter does not reach yet.
// canonicalDamageCeiling is the number of gofmt-clean standard library files
// the whole pipeline still rewrites, all of it layout the formatter renders
// differently from gofmt. Lower it as passes converge; it never goes up.
const canonicalDamageCeiling = 2098

func TestCanonicalInputUnchanged(t *testing.T) {
	t.Run("doc comments", func(t *testing.T) { canonicalInputUnchanged(t, "doc") })
	t.Run("minimum spacing", func(t *testing.T) { canonicalInputUnchanged(t, "mvs") })
	t.Run("full pipeline", func(t *testing.T) { canonicalInputUnchanged(t, "all") })
}

func canonicalInputUnchanged(t *testing.T, pass string) {
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

	var checked, changed int
	var examples []string
	kinds := map[string]int{}
	kindExamples := map[string]string{}
	for _, f := range files {
		content, err := os.ReadFile(f)
		if err != nil {
			continue
		}
		src := string(content)
		var ctx *build.Context
		for i := range contexts {
			if parser.MatchBuildContext(contexts[i], filepath.Base(f), src) {
				ctx = &contexts[i]
				break
			}
		}
		if ctx == nil {
			continue
		}
		// Only gofmt-clean files carry the invariant.
		if formatted, err := gofmtSource(filepath.Base(f), src); err != nil || formatted != src {
			continue
		}
		p := parser.NewGoParserWithBuildContext(*ctx)
		p.ParseOnly = true
		cu, err := p.Parse(filepath.Base(f), src)
		if err != nil || printer.Print(cu) != src {
			continue
		}
		checked++

		var got string
		switch pass {
		case "all":
			v := NewAutoFormatVisitor(nil)
			out := v.Visit(cu, nil)
			got = printer.Print(visitor.DrainAfterVisits(v, out.(java.Tree), nil))
		case "mvs":
			got = printer.Print(NewMinimumViableSpacingVisitor(nil).Visit(cu, nil))
		default:
			got = printer.Print(NewDocCommentVisitor(nil).Visit(cu, nil))
		}
		if got == src {
			continue
		}
		changed++
		{
			g, w := strings.Split(got, "\n"), strings.Split(src, "\n")
			for i := range g {
				if i < len(w) && g[i] != w[i] {
					kind, _ := classifyLine(g[i], w[i])
					kinds[kind]++
					if _, seen := kindExamples[kind]; !seen {
						kindExamples[kind] = filepath.Base(f) +
							"\n         want |" + w[i] + "|\n         got  |" + g[i] + "|"
					}
					break
				}
			}
		}
		if len(examples) < 6 {
			for i, line := range strings.Split(got, "\n") {
				want := strings.Split(src, "\n")
				if i < len(want) && line != want[i] {
					examples = append(examples, filepath.Base(f)+
						"\n         want |"+want[i]+"|\n         got  |"+line+"|")
					break
				}
			}
		}
	}
	t.Logf("gofmtCleanFilesChecked=%d changed=%d", checked, changed)
	keys := make([]string, 0, len(kinds))
	for k := range kinds {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return kinds[keys[i]] > kinds[keys[j]] })
	for _, k := range keys {
		t.Logf("  %5d files  %s\n         e.g. %s", kinds[k], k, kindExamples[k])
	}
	for _, e := range examples {
		t.Logf("  %s", e)
	}
	ceiling := 0
	if pass == "all" {
		ceiling = canonicalDamageCeiling
	}
	if changed > ceiling {
		t.Errorf("rewrote %d already-canonical files, ceiling is %d", changed, ceiling)
	}
}
