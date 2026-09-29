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
	"go/build"
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestSelfParseGoParser(t *testing.T) {
	data, err := os.ReadFile("../pkg/parser/go_parser.go")
	require.NoError(t, err, "failed to read file")
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseGoPrinter(t *testing.T) {
	data, err := os.ReadFile("../pkg/printer/go_printer.go")
	require.NoError(t, err, "failed to read file")
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseGoVisitor(t *testing.T) {
	data, err := os.ReadFile("../pkg/visitor/go_visitor.go")
	require.NoError(t, err, "failed to read file")
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseJTree(t *testing.T) {
	data, err := os.ReadFile("../pkg/tree/java/j.go")
	require.NoError(t, err, "failed to read file")
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseGoTree(t *testing.T) {
	data, err := os.ReadFile("../pkg/tree/golang/go.go")
	require.NoError(t, err, "failed to read file")
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

func TestSelfParseSpec(t *testing.T) {
	data, err := os.ReadFile("../pkg/test/spec.go")
	require.NoError(t, err, "failed to read file")
	NewRecipeSpec().RewriteRun(t,
		GolangRaw(string(data)),
	)
}

// readStdlibFile returns the source of a $GOROOT/src file, skipping when the
// host toolchain lacks it or excludes it from the build — the corpus is the
// running toolchain's stdlib, and an excluded file yields no compilation unit.
func readStdlibFile(t *testing.T, path string) string {
	t.Helper()
	data, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", path))
	if err != nil {
		t.Skipf("skipping: %v", err)
	}
	src := string(data)
	if !parser.MatchBuildContext(build.Default, filepath.Base(path), src) {
		t.Skipf("skipping: %s is excluded from the build under this toolchain", path)
	}
	return src
}

func TestParseStdlibSort(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "sort/sort.go")))
}

func TestParseStdlibStrings(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "strings/strings.go")))
}

func TestParseStdlibFmt(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "fmt/print.go")))
}

func TestParseStdlibSync(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "sync/mutex.go")))
}

func TestParseStdlibHTTP(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "net/http/server.go")))
}

func TestParseStdlibJSON(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "encoding/json/encode.go")))
}

func TestParseStdlibReflect(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "reflect/type.go")))
}

func TestParseStdlibGoParser(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "go/parser/parser.go")))
}

func TestParseStdlibGoAST(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "go/ast/ast.go")))
}

func TestParseStdlibIO(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "io/io.go")))
}

func TestParseStdlibContext(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "context/context.go")))
}

func TestParseStdlibBytesBuffer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "bytes/buffer.go")))
}

func TestParseStdlibRegexp(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "regexp/regexp.go")))
}

func TestParseStdlibOsFile(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "os/file.go")))
}

func TestParseStdlibTLS(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "crypto/tls/tls.go")))
}

func TestParseStdlibSQL(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "database/sql/sql.go")))
}

func TestParseStdlibTesting(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "testing/testing.go")))
}

func TestParseStdlibSlices(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "slices/slices.go")))
}

func TestParseStdlibMaps(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "maps/maps.go")))
}

func TestParseStdlibSlog(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "log/slog/handler.go")))
}

func TestParseStdlibAtomic(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "sync/atomic/value.go")))
}

func TestParseStdlibBufio(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "bufio/bufio.go")))
}

func TestParseStdlibStrconv(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "strconv/quote.go")))
}

func TestParseStdlibPath(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "path/filepath/path.go")))
}

func TestParseStdlibExec(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "os/exec/exec.go")))
}

func TestParseStdlibTemplate(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "text/template/exec.go")))
}

func TestParseStdlibScanner(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "go/scanner/scanner.go")))
}

func TestParseStdlibToken(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "go/token/token.go")))
}

func TestParseStdlibErrors(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "errors/wrap.go")))
}

func TestParseStdlibUnicode(t *testing.T) {
	NewRecipeSpec().RewriteRun(t, GolangRaw(readStdlibFile(t, "unicode/utf8/utf8.go")))
}
