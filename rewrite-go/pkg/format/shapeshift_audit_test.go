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
	"fmt"
	"os"
	"path/filepath"
	"reflect"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

// TestShapeshiftAudit enumerates every node type that maps differently with
// and without type attribution, over the Go standard library.
func TestShapeshiftAudit(t *testing.T) {
	root := filepath.Join(runtime.GOROOT(), "src")
	var files []string
	filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err == nil && !info.IsDir() && strings.HasSuffix(path, ".go") && !strings.Contains(path, "/testdata/") {
			files = append(files, path)
		}
		return nil
	})

	pairs := map[string]int{}
	filesWithDivergence := 0
	for _, f := range files {
		content, err := os.ReadFile(f)
		if err != nil {
			continue
		}
		typed, err := parser.NewGoParser().Parse(filepath.Base(f), string(content))
		if err != nil {
			continue
		}
		p := parser.NewGoParser()
		p.ParseOnly = true
		untyped, err := p.Parse(filepath.Base(f), string(content))
		if err != nil {
			continue
		}
		found := map[string]int{}
		collectDivergences(reflect.ValueOf(typed), reflect.ValueOf(untyped), found)
		if len(found) > 0 {
			filesWithDivergence++
		}
		for k, v := range found {
			pairs[k] += v
		}
	}

	keys := make([]string, 0, len(pairs))
	for k := range pairs {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return pairs[keys[i]] > pairs[keys[j]] })
	t.Logf("files=%d filesWithDivergence=%d distinctPairs=%d", len(files), filesWithDivergence, len(pairs))
	for _, k := range keys {
		t.Logf("  %6d  %s", pairs[k], k)
	}
}

// collectDivergences records each place the two trees disagree on node type,
// then stops descending that branch.
func collectDivergences(a, b reflect.Value, out map[string]int) {
	if !a.IsValid() || !b.IsValid() {
		return
	}
	t := a.Type()
	if t != b.Type() {
		if shapeOf(t).isTree || shapeOf(b.Type()).isTree {
			out[fmt.Sprintf("%s vs %s", t, b.Type())]++
		}
		return
	}
	d := shapeOf(t)
	if !d.carriesSpace || d.isSpace || d.isMarkers {
		return
	}
	switch d.kind {
	case reflect.Interface, reflect.Pointer:
		if a.IsNil() || b.IsNil() {
			if a.IsNil() != b.IsNil() {
				out[fmt.Sprintf("nil mismatch at %s", t)]++
			}
			return
		}
		collectDivergences(a.Elem(), b.Elem(), out)
	case reflect.Struct:
		for _, i := range d.spaceFields {
			collectDivergences(a.Field(i), b.Field(i), out)
		}
	case reflect.Slice:
		if a.Len() != b.Len() {
			out[fmt.Sprintf("length mismatch at %s", t)]++
			return
		}
		for i := 0; i < a.Len(); i++ {
			collectDivergences(a.Index(i), b.Index(i), out)
		}
	}
}
