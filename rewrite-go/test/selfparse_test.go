/*
 * Copyright 2025 the original author or authors.
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
	"path/filepath"
	"runtime"
	"testing"

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestSelfParseGoParser(t *testing.T) {
	data, err := os.ReadFile("../pkg/parser/go_parser.go")
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseGoPrinter(t *testing.T) {
	data, err := os.ReadFile("../pkg/printer/go_printer.go")
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseGoVisitor(t *testing.T) {
	data, err := os.ReadFile("../pkg/visitor/go_visitor.go")
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseJTree(t *testing.T) {
	data, err := os.ReadFile("../pkg/tree/j.go")
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseGoTree(t *testing.T) {
	data, err := os.ReadFile("../pkg/tree/go.go")
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseSpec(t *testing.T) {
	data, err := os.ReadFile("../pkg/test/spec.go")
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func stdlibFile(path string) string {
	return filepath.Join(runtime.GOROOT(), "src", path)
}

func TestParseStdlibSort(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("sort/sort.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibStrings(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("strings/strings.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibFmt(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("fmt/print.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibSync(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("sync/mutex.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibHTTP(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("net/http/server.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibJSON(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("encoding/json/encode.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibReflect(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("reflect/type.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibGoParser(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("go/parser/parser.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibGoAST(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("go/ast/ast.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibIO(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("io/io.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibContext(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("context/context.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibBytesBuffer(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("bytes/buffer.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibRegexp(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("regexp/regexp.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibOsFile(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("os/file.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibTLS(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("crypto/tls/tls.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibSQL(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("database/sql/sql.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibTesting(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("testing/testing.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibSlices(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("slices/slices.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibMaps(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("maps/maps.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibSlog(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("log/slog/handler.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibAtomic(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("sync/atomic/value.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibBufio(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("bufio/bufio.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibStrconv(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("strconv/atoi.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibPath(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("path/filepath/path.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibExec(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("os/exec/exec.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibTemplate(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("text/template/exec.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibScanner(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("go/scanner/scanner.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibToken(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("go/token/token.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibErrors(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("errors/wrap.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}

func TestParseStdlibUnicode(t *testing.T) {
	data, err := os.ReadFile(stdlibFile("unicode/utf8/utf8.go"))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	NewRecipeSpec().RewriteRun(t, GolangRaw(string(data)))
}
