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
	"fmt"
	"os"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

// TestReduce shrinks a failing corpus file to a minimal reproducer by
// line-level delta debugging: it drops chunks of lines and keeps any
// candidate that go/parser still accepts and that still exhibits the
// same failure class as the original.
//
//	GO_REDUCE=/tmp/go-corpus/uber-go_zap/config_test.go \
//	  go test -tags parityaudit ./test/ -run TestReduce -v
func TestReduce(t *testing.T) {
	path := os.Getenv("GO_REDUCE")
	if path == "" {
		t.Skip("GO_REDUCE not set")
	}
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read: %v", err)
	}
	src := string(raw)

	want := failureClass(src)
	if want == "" {
		t.Fatalf("%s does not fail; nothing to reduce", path)
	}
	t.Logf("original failure: %s", want)

	lines := strings.Split(src, "\n")
	// ddmin over line granularity: halve the chunk size each time a full
	// pass removes nothing, down to single lines.
	for n := len(lines); n > 0; {
		chunk := max(1, len(lines)/n)
		removed := false
		for i := 0; i+chunk <= len(lines); {
			cand := append(append([]string{}, lines[:i]...), lines[i+chunk:]...)
			text := strings.Join(cand, "\n")
			if goParses(text) && failureClass(text) == want {
				lines = cand
				removed = true
				continue // retry the same index against the shortened slice
			}
			i++
		}
		if !removed {
			if chunk == 1 {
				break
			}
			n *= 2
		}
	}

	// Second pass: shrink each surviving line from the right, which
	// strips long argument lists and struct literals the line pass
	// cannot touch.
	for i := range lines {
		orig := lines[i]
		for cut := len(orig); cut > 0; cut-- {
			cand := append(append([]string{}, lines[:i]...), append([]string{orig[:cut-1]}, lines[i+1:]...)...)
			text := strings.Join(cand, "\n")
			if goParses(text) && failureClass(text) == want {
				lines = cand
				orig = orig[:cut-1]
			}
		}
	}

	out := strings.Join(lines, "\n")
	t.Logf("reduced to %d bytes (from %d):\n%s\n--- end ---", len(out), len(src), out)
	if dst := os.Getenv("GO_REDUCE_OUT"); dst != "" {
		if err := os.WriteFile(dst, []byte(out), 0o644); err != nil {
			t.Fatalf("write reduced: %v", err)
		}
	}
}

// failureClass returns a stable label for how src fails, or "" when it
// round-trips soundly. Reduction must preserve the label, not merely
// "still fails", or ddmin happily converges on an unrelated bug.
func failureClass(src string) (class string) {
	defer func() {
		if r := recover(); r != nil {
			class = "panic: " + normalizeErr(fmt.Sprint(r))
		}
	}()
	cu, err := parser.NewGoParser().Parse("reduce.go", src)
	if err != nil {
		return "error: " + normalizeErr(err.Error())
	}
	if printed := printer.Print(cu); printed != src {
		b, _ := diffSignature(src, printed)
		return "roundtrip: " + b
	}
	if sites := locateHidden(cu); len(sites) > 0 {
		return "hidden in " + sites[0].owner + ": " + normalizeHidden(sites[0].msg)
	}
	return ""
}
