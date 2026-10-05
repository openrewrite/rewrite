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
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

// TestWhitespaceAttachmentAudit reports where leading whitespace sits on a
// child that shares its parent's leading edge. It belongs to the outermost
// element: anything reading a node's own prefix otherwise finds it empty while
// the text in front of the node is not.
func TestWhitespaceAttachmentAudit(t *testing.T) {
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

	kinds := map[string]int{}
	examples := map[string]string{}
	var checked, withViolations int

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
		p := parser.NewGoParserWithBuildContext(*ctx)
		p.ParseOnly = true
		cu, err := p.Parse(filepath.Base(f), src)
		if err != nil {
			continue
		}
		checked++

		violations := test.WhitespaceAttachmentViolations(cu)
		if len(violations) == 0 {
			continue
		}
		withViolations++
		for _, v := range violations {
			kind := kindOf(v)
			kinds[kind]++
			if _, seen := examples[kind]; !seen {
				examples[kind] = filepath.Base(f) + ": " + v
			}
		}
	}

	keys := make([]string, 0, len(kinds))
	for k := range kinds {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return kinds[keys[i]] > kinds[keys[j]] })

	t.Logf("filesChecked=%d filesWithViolations=%d distinctKinds=%d", checked, withViolations, len(kinds))
	for _, k := range keys {
		t.Logf("  %6d  %s\n         e.g. %s", kinds[k], k, examples[k])
	}
}

// kindOf reduces a violation message to the parent-and-child pair it reports,
// so the same modelling gap counts once however often it occurs.
func kindOf(violation string) string {
	parts := strings.SplitN(violation, " has child ", 2)
	if len(parts) != 2 {
		return violation
	}
	child, _, _ := strings.Cut(parts[1], " starting with whitespace")
	return parts[0] + " -> " + child
}
