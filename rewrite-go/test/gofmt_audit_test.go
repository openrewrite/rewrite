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

package test

import (
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

// TestGofmtAudit strips the indentation off every Go standard library file and
// checks that the splice restores exactly what gofmt produces. It reports
// coverage rather than asserting, so parser gaps don't read as splice failures.
//
// Gated behind the `gofmtaudit` build tag; run with
// `go test -tags gofmtaudit ./test/ -run TestGofmtAudit -v`.
func TestGofmtAudit(t *testing.T) {
	root := filepath.Join(runtime.GOROOT(), "src")

	var files []string
	filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || !strings.HasSuffix(path, ".go") {
			return nil
		}
		if strings.Contains(path, "/testdata/") {
			return nil
		}
		files = append(files, path)
		return nil
	})

	var parsed, roundTripped, exact, mismatch, bailed int
	var examples []string
	for _, f := range files {
		content, err := os.ReadFile(f)
		if err != nil {
			continue
		}
		mangled := stripIndent(string(content))
		want, err := gofmtBinary(t, mangled)
		if err != nil {
			continue
		}

		cu, err := parser.NewGoParser().Parse(filepath.Base(f), mangled)
		if err != nil {
			continue
		}
		parsed++
		if printer.Print(cu) != mangled {
			continue // printer round-trip gap, not a splice gap
		}
		roundTripped++

		out, err := format.Gofmt(cu, nil)
		if err != nil {
			bailed++
			continue
		}
		got := printer.Print(out)
		switch {
		case got == want:
			exact++
		case got == mangled:
			bailed++
		default:
			mismatch++
			if len(examples) < 10 {
				examples = append(examples, f)
			}
		}
	}

	t.Logf("files=%d parsed=%d roundTripped=%d exactGofmt=%d bailed=%d mismatch=%d",
		len(files), parsed, roundTripped, exact, bailed, mismatch)
	for _, e := range examples {
		t.Logf("  mismatch: %s", e)
	}
}

func gofmtBinary(t *testing.T, src string) (string, error) {
	t.Helper()
	cmd := exec.Command("gofmt")
	cmd.Stdin = strings.NewReader(src)
	out, err := cmd.Output()
	return string(out), err
}
