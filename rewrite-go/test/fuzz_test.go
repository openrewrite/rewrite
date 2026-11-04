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
	goparser "go/parser"
	"go/scanner"
	"go/token"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"testing"
)

// TestFuzzTokenGaps widens the gap between two adjacent tokens of a
// corpus file — with a block comment, extra spaces, a tab, a newline —
// and checks the result still round-trips soundly.
//
// Real Go is overwhelmingly gofmt'd, which leaves exactly one canonical
// amount of space between any two tokens. Bugs that mishandle a gap only
// surface where an author wrote something else, so the corpus is thin
// exactly where the parser is weakest.
//
//	GO_CORPUS=/tmp/go-corpus go test -tags parityaudit ./test/ -run TestFuzzTokenGaps -timeout 60m
func TestFuzzTokenGaps(t *testing.T) {
	root := os.Getenv("GO_CORPUS")
	if root == "" {
		t.Skip("GO_CORPUS not set")
	}
	// Cost is (seeds x gaps x fillers) full reparses, so both the seed
	// sample and the gaps within a seed are strided rather than taken as
	// a prefix — a prefix would be one directory of one repository, and
	// the first few hundred bytes of each file.
	seeds := collectGoFiles(t, root)
	seeds = stride(seeds, envInt("GO_FUZZ_SEEDS", 300))

	fillers := []string{"/*x*/", "  ", "\t", "\n", " /*x*/ ", "\n\t"}
	failures := map[string]string{}
	checked := 0

	for _, seed := range seeds {
		src, err := os.ReadFile(seed)
		if err != nil {
			continue
		}
		if failureClass(string(src)) != "" {
			continue // already failing: not this test's business
		}
		for _, gap := range strideInts(tokenGaps(string(src)), envInt("GO_FUZZ_GAPS", 60)) {
			for _, filler := range fillers {
				cand := string(src[:gap]) + filler + string(src[gap:])
				if !goParses(cand) {
					continue
				}
				checked++
				if class := failureClass(cand); class != "" {
					if _, seen := failures[class]; !seen {
						rel, _ := filepath.Rel(root, seed)
						failures[class] = fmt.Sprintf("%s at %d with %q", rel, gap, filler)
					}
				}
			}
		}
	}

	t.Logf("checked %d mutations of %d seeds", checked, len(seeds))
	if len(failures) == 0 {
		return
	}
	keys := make([]string, 0, len(failures))
	for k := range failures {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	var sb strings.Builder
	for _, k := range keys {
		fmt.Fprintf(&sb, "\n%s\n    e.g. %s", k, failures[k])
	}
	t.Errorf("%d distinct failures:%s", len(keys), sb.String())
}

// tokenGaps returns the offsets where a token ends, which is where a
// gap can be widened without changing the token stream. Positions
// inside a comment or a string literal are not gaps.
func tokenGaps(src string) []int {
	fset := token.NewFileSet()
	f := fset.AddFile("f.go", -1, len(src))
	var s scanner.Scanner
	s.Init(f, []byte(src), func(token.Position, string) {}, scanner.ScanComments)
	var gaps []int
	for {
		pos, tok, lit := s.Scan()
		if tok == token.EOF {
			break
		}
		end := f.Offset(pos) + len(lit)
		if lit == "" {
			end = f.Offset(pos) + len(tok.String())
		}
		if end <= len(src) {
			gaps = append(gaps, end)
		}
	}
	return gaps
}

func stride[T any](xs []T, n int) []T {
	if n <= 0 || n >= len(xs) {
		return xs
	}
	step := len(xs) / n
	var out []T
	for i := 0; i < len(xs); i += step {
		out = append(out, xs[i])
	}
	return out
}

func strideInts(xs []int, n int) []int { return stride(xs, n) }

func envInt(name string, def int) int {
	if v := os.Getenv(name); v != "" {
		var n int
		if _, err := fmt.Sscanf(v, "%d", &n); err == nil {
			return n
		}
	}
	return def
}

// goParses gates candidates on Go's own parser so only valid programs
// are checked.
func goParses(src string) bool {
	_, err := goparser.ParseFile(token.NewFileSet(), "f.go", src, goparser.ParseComments)
	return err == nil
}
