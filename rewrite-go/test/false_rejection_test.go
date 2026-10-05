//go:build parityaudit

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

import (
	"go/build"
	"os"
	"path/filepath"
	"testing"
)

// A file excluded under every audit context that go/build would take is
// a false rejection. See printer-corpus/README.md.
func TestFalseRejections(t *testing.T) {
	root := os.Getenv("GO_CORPUS")
	if root == "" {
		t.Skip("GO_CORPUS not set")
	}
	var falseRejects []string
	var skipped int
	for _, path := range collectGoFiles(t, root) {
		src, err := os.ReadFile(path)
		if err != nil {
			continue
		}
		name := filepath.Base(path)
		if parserSelecting(name, string(src)) != nil {
			continue
		}
		skipped++
		for _, c := range append([]struct{ goos, goarch string }{{build.Default.GOOS, build.Default.GOARCH}}, auditContexts...) {
			bc := build.Default
			bc.GOOS, bc.GOARCH = c.goos, c.goarch
			if ok, err := bc.MatchFile(filepath.Dir(path), name); err == nil && ok {
				rel, _ := filepath.Rel(root, path)
				falseRejects = append(falseRejects, rel)
				break
			}
		}
	}
	t.Logf("skipped %d, of which go/build accepts %d under some audit context", skipped, len(falseRejects))
	for _, f := range falseRejects {
		t.Logf("  %s", f)
	}
}
