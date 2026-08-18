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

package parser_test

import (
	"go/build"
	goparser "go/parser"
	gotoken "go/token"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

// TestParserCoverageAudit buckets why standard library files fail to parse,
// separating build-context exclusion from real gaps.
func TestParserCoverageAudit(t *testing.T) {
	root := filepath.Join(runtime.GOROOT(), "src")
	var files []string
	filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err == nil && !info.IsDir() && strings.HasSuffix(path, ".go") && !strings.Contains(path, "/testdata/") {
			files = append(files, path)
		}
		return nil
	})

	contexts := []build.Context{}
	for _, pair := range [][2]string{
		{"darwin", "arm64"}, {"linux", "amd64"}, {"windows", "amd64"},
		{"js", "wasm"}, {"plan9", "386"}, {"linux", "riscv64"}, {"aix", "ppc64"},
	} {
		c := build.Default
		c.GOOS, c.GOARCH = pair[0], pair[1]
		c.CgoEnabled = true
		contexts = append(contexts, c)
	}

	digits := regexp.MustCompile(`[0-9]+`)
	buckets := map[string]int{}
	examples := map[string]string{}
	var ok, excluded, goParserRejects, rewriteRejects, roundTripFails int
	var roundTripExamples []string

	for _, f := range files {
		content, err := os.ReadFile(f)
		if err != nil {
			continue
		}
		src := string(content)

		// Try every build context, so files constrained to another platform
		// are still exercised.
		var selected *build.Context
		for i, c := range contexts {
			if parser.MatchBuildContext(c, filepath.Base(f), src) {
				selected = &contexts[i]
				break
			}
		}
		if selected == nil {
			excluded++
			continue
		}
		// Does Go's own parser accept it?
		fset := gotoken.NewFileSet()
		if _, err := goparser.ParseFile(fset, f, src, goparser.ParseComments); err != nil {
			goParserRejects++
			continue
		}

		p := parser.NewGoParserWithBuildContext(*selected)
		p.ParseOnly = true
		cu, err := p.Parse(filepath.Base(f), src)
		if err != nil {
			rewriteRejects++
			key := digits.ReplaceAllString(err.Error(), "N")
			if i := strings.Index(key, ": "); i > 0 && strings.HasSuffix(key[:i], ".go") {
				key = key[i+2:]
			}
			buckets[key]++
			if _, seen := examples[key]; !seen {
				examples[key] = f
			}
			continue
		}
		if printed := printer.Print(cu); printed != src {
			roundTripFails++
			if len(roundTripExamples) < 10 {
				roundTripExamples = append(roundTripExamples, f)
			}
			continue
		}
		ok++
	}

	keys := make([]string, 0, len(buckets))
	for k := range buckets {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return buckets[keys[i]] > buckets[keys[j]] })

	t.Logf("files=%d parsedAndRoundTripped=%d buildExcluded=%d goParserRejects=%d rewriteRejects=%d roundTripFails=%d distinctCauses=%d",
		len(files), ok, excluded, goParserRejects, rewriteRejects, roundTripFails, len(buckets))
	for _, e := range roundTripExamples {
		t.Logf("  roundTrip: %s", e)
	}
	for _, k := range keys {
		t.Logf("  %5d  %s\n         e.g. %s", buckets[k], k, examples[k])
	}
}
