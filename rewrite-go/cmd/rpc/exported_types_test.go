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

package main

import (
	"encoding/json"
	"go/build"
	"os"
	"path/filepath"
	"reflect"
	"testing"

	"golang.org/x/mod/module"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A small module: an exported struct with a method + field (one referencing a
// foreign type), plus a package-level function and const, exercising the
// named-type, shallow-reference, and synthetic-package-class paths.
const greeterModule = `package foo

import "io"

type Greeter struct {
	Name string
	Out  io.Writer
}

func (g *Greeter) Greet(prefix string) string {
	return prefix + g.Name
}

func NewGreeter(name string) *Greeter {
	return &Greeter{Name: name}
}

const Version = "1.0"
`

func TestHandleExportedTypes(t *testing.T) {
	dir := modCacheDir(t, "example.com/foo", "v1.0.0")
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/foo\n\ngo 1.21\n")
	writeFile(t, filepath.Join(dir, "greeter.go"), greeterModule)

	s, _ := newTestServer(t)
	params, err := json.Marshal(dependencyRequest{ModulePath: "example.com/foo", Version: "v1.0.0"})
	require.NoError(t, err)
	result, rpcErr := s.handleDependencyTypes(params)
	require.Nil(t, rpcErr)
	data := result.([]rpc.RpcObjectData)
	require.False(t, len(data) == 0 || data[len(data)-1].State != rpc.EndOfObject)

	// The own-FQN list is sent first and must name every enumerated type, so the
	// caller can tell defined types from references before the first type arrives.
	fqnSet := map[string]bool{}
	for _, f := range receiveFqnList(t, data) {
		fqnSet[f] = true
	}
	if !fqnSet["example.com/foo.Greeter"] || !fqnSet["example.com/foo"] {
		t.Errorf("own-FQN list missing enumerated FQNs; got %v", fqnSet)
	}

	types := receiveTypeList(t, data)
	if len(types) == 0 {
		t.Fatal("no types enumerated")
	}

	byFqn := map[string]*java.JavaTypeClass{}
	for _, ty := range types {
		if cls, ok := ty.(*java.JavaTypeClass); ok {
			byFqn[cls.FullyQualifiedName] = cls
		}
	}

	greeter := byFqn["example.com/foo.Greeter"]
	if greeter == nil {
		t.Fatalf("Greeter type not enumerated; got %v", keys(byFqn))
	}
	if len(greeter.Methods) == 0 {
		t.Error("Greeter.Methods is empty; expected Greet")
	}
	if len(greeter.Members) == 0 {
		t.Error("Greeter.Members is empty; expected Name")
	}

	pkg := byFqn["example.com/foo"]
	if pkg == nil {
		t.Fatalf("synthetic package class not enumerated; got %v", keys(byFqn))
	}
	if len(pkg.Methods) == 0 {
		t.Error("package class Methods is empty; expected NewGreeter")
	}
	if len(pkg.Members) == 0 {
		t.Error("package class Members is empty; expected Version")
	}

	// A type the module references but doesn't define arrives as an FQN-only ShallowClass.
	var out java.JavaType
	for _, m := range greeter.Members {
		if m.Name == "Out" {
			out = m.Type
		}
	}
	sc, ok := out.(*java.JavaTypeShallowClass)
	if !ok {
		t.Fatalf("io.Writer reference is %T, want *java.JavaTypeShallowClass", out)
	}
	assert.False(t, sc.FullyQualifiedName != "io.Writer" || len(sc.Members) != 0 || len(sc.Methods) != 0)
	sawShallowTag := false
	for _, d := range data {
		if d.ValueType != nil && *d.ValueType == java.JavaTypeShallowClassKind {
			sawShallowTag = true
			break
		}
	}
	assert.True(t, sawShallowTag)
}

// Covers the enumerator's correctness fixes: an alias to an external type is not
// owned by this module, own non-alias types keep their members/methods, internal/
// packages are skipped, a second package is enumerated, and the fixed linux/amd64
// build context selects the _linux variant of a per-OS type rather than _windows.
func TestExportedTypes_Correctness(t *testing.T) {
	dir := modCacheDir(t, "example.com/foo", "v1.0.0")
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/foo\n\ngo 1.21\n")
	writeFile(t, filepath.Join(dir, "foo.go"), `package foo

import "io"

type MyReader = io.Reader

type Greeter struct {
	Name string
}

func (g *Greeter) Greet() string { return g.Name }
`)
	// Same type declared per-OS; only the linux variant must be read.
	writeFile(t, filepath.Join(dir, "plat_linux.go"), "package foo\n\ntype Config struct {\n\tLinux bool\n}\n")
	writeFile(t, filepath.Join(dir, "plat_windows.go"), "package foo\n\ntype Config struct {\n\tWindows bool\n}\n")
	// A non-importable internal package — must not be enumerated.
	writeFile(t, filepath.Join(dir, "internal", "secret", "secret.go"), "package secret\n\ntype Hidden struct {\n\tX int\n}\n")
	// A second package in the same module — must be enumerated.
	writeFile(t, filepath.Join(dir, "bar", "bar.go"), "package bar\n\ntype Bar struct {\n\tY int\n}\n\nfunc NewBar() *Bar { return &Bar{} }\n")

	byFqn := exportedTypesByFqn(t, "example.com/foo", "v1.0.0")

	greeter := byFqn["example.com/foo.Greeter"]
	if greeter == nil {
		t.Fatalf("Greeter not enumerated; got %v", keys(byFqn))
	}
	assert.True(t, hasMethod(greeter, "Greet"))
	assert.True(t, hasMember(greeter, "Name"))
	if _, ok := byFqn["io.Reader"]; ok {
		t.Errorf("external alias target io.Reader was collected as an owned type")
	}
	if _, ok := byFqn["example.com/foo/internal/secret.Hidden"]; ok {
		t.Errorf("internal/ package was enumerated")
	}
	if _, ok := byFqn["example.com/foo/bar.Bar"]; !ok {
		t.Errorf("bar.Bar not enumerated; got %v", keys(byFqn))
	}
	if barPkg := byFqn["example.com/foo/bar"]; barPkg == nil || !hasMethod(barPkg, "NewBar") {
		t.Errorf("bar package class missing NewBar; got %v", barPkg)
	}
	config := byFqn["example.com/foo.Config"]
	if config == nil {
		t.Fatalf("Config not enumerated; got %v", keys(byFqn))
	}
	assert.True(t, hasMember(config, "Linux"))
	assert.False(t, hasMember(config, "Windows"))
}

// A subdirectory with its own go.mod is a separate module; its files must not be
// attributed to the parent module's import path.
func TestExportedTypes_SkipsNestedModule(t *testing.T) {
	dir := modCacheDir(t, "example.com/outer", "v1.0.0")
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/outer\n\ngo 1.21\n")
	writeFile(t, filepath.Join(dir, "outer.go"), "package outer\n\ntype Outer struct{}\n")
	writeFile(t, filepath.Join(dir, "nested", "go.mod"), "module example.com/nested\n\ngo 1.21\n")
	writeFile(t, filepath.Join(dir, "nested", "nested.go"), "package nested\n\ntype Nested struct{}\n")

	byFqn := exportedTypesByFqn(t, "example.com/outer", "v1.0.0")

	if _, ok := byFqn["example.com/outer.Outer"]; !ok {
		t.Errorf("Outer not enumerated; got %v", keys(byFqn))
	}
	if _, ok := byFqn["example.com/outer/nested.Nested"]; ok {
		t.Errorf("nested module file mis-attributed to parent import path")
	}
}

// A stdlib package dir has no go.mod; the enumerator must still resolve its import path
// and enumerate from source. Checked directly, not through the wire: fmt's API reaches
// types the round-trip receiver mirror doesn't model (the wire path is covered above).
func TestExportedTypes_StdlibPackage(t *testing.T) {
	dir := filepath.Join(build.Default.GOROOT, "src", "fmt")
	if fi, err := os.Stat(dir); err != nil || !fi.IsDir() {
		t.Skipf("no stdlib source at %s", dir)
	}

	byFqn := map[string]*java.JavaTypeClass{}
	for _, ty := range goparser.ExportedTypes([]string{dir}, nil) {
		if cls, ok := ty.(*java.JavaTypeClass); ok {
			byFqn[cls.FullyQualifiedName] = cls
		}
	}

	stringer := byFqn["fmt.Stringer"]
	if stringer == nil {
		t.Fatalf("fmt.Stringer not enumerated; got %v", keys(byFqn))
	}
	assert.True(t, hasMethod(stringer, "String"))
	pkg := byFqn["fmt"]
	if pkg == nil {
		t.Fatalf("synthetic fmt package class not enumerated; got %v", keys(byFqn))
	}
	assert.True(t, hasMethod(pkg, "Sprintf"))
	// A stdlib package's cross-package references are shallow too.
	for _, m := range pkg.Methods {
		if m.Name != "Fprintf" {
			continue
		}
		if len(m.ParameterTypes) == 0 {
			t.Fatal("Fprintf has no parameter types")
		}
		if sc, ok := m.ParameterTypes[0].(*java.JavaTypeShallowClass); !ok || sc.FullyQualifiedName != "io.Writer" {
			t.Errorf("Fprintf writer param = %T (%v); want ShallowClass io.Writer", m.ParameterTypes[0], m.ParameterTypes[0])
		}
	}
}

// A versionless coordinate names a stdlib import path resolved under $GOROOT/src. Only the
// resolution + framing are checked over the wire; fmt's full API reaches types the round-trip
// receiver mirror doesn't model (covered by the direct test above).
func TestHandleDependencyTypes_StdlibCoordinate(t *testing.T) {
	dir := filepath.Join(build.Default.GOROOT, "src", "fmt")
	if fi, err := os.Stat(dir); err != nil || !fi.IsDir() {
		t.Skipf("no stdlib source at %s", dir)
	}

	s, _ := newTestServer(t)
	s.batchSize = 1 << 20
	params, err := json.Marshal(dependencyRequest{ModulePath: "fmt"})
	require.NoError(t, err)
	result, rpcErr := s.handleDependencyTypes(params)
	require.Nil(t, rpcErr)
	data := result.([]rpc.RpcObjectData)
	require.False(t, len(data) == 0 || data[len(data)-1].State != rpc.EndOfObject)
	fqns := map[string]bool{}
	for _, f := range receiveFqnList(t, data) {
		fqns[f] = true
	}
	if !fqns["fmt"] || !fqns["fmt.Stringer"] {
		t.Errorf("own-FQN list missing fmt package FQNs; got %d FQNs", len(fqns))
	}
}

// A versioned coordinate resolves against the parsed project's vendor/ tree before the module
// cache; ParseProject records the project root that anchors the vendor lookup.
func TestHandleDependencyTypes_VendorTree(t *testing.T) {
	proj := t.TempDir()
	vdir := filepath.Join(proj, "vendor", "example.com", "foo")
	writeFile(t, filepath.Join(vdir, "go.mod"), "module example.com/foo\n\ngo 1.21\n")
	writeFile(t, filepath.Join(vdir, "greeter.go"), greeterModule)
	t.Setenv("GOMODCACHE", t.TempDir()) // empty cache: the vendor tree must win

	s, _ := newTestServer(t)
	pp, err := json.Marshal(parseProjectRequest{ProjectPath: proj})
	require.NoError(t, err)
	if _, rpcErr := s.handleParseProject(pp); rpcErr != nil {
		t.Fatalf("ParseProject failed: %+v", rpcErr)
	}

	params, err := json.Marshal(dependencyRequest{ModulePath: "example.com/foo", Version: "v1.0.0"})
	require.NoError(t, err)
	result, rpcErr := s.handleDependencyTypes(params)
	require.Nil(t, rpcErr)
	byFqn := map[string]*java.JavaTypeClass{}
	for _, ty := range receiveTypeList(t, result.([]rpc.RpcObjectData)) {
		if cls, ok := ty.(*java.JavaTypeClass); ok {
			byFqn[cls.FullyQualifiedName] = cls
		}
	}
	if byFqn["example.com/foo.Greeter"] == nil {
		t.Errorf("vendored Greeter not enumerated; got %v", keys(byFqn))
	}
}

// A coordinate found in neither the vendor tree nor the module cache is a per-dependency
// error, not an empty table.
func TestHandleDependencyTypes_UnresolvableCoordinate(t *testing.T) {
	t.Setenv("GOMODCACHE", t.TempDir())
	s, _ := newTestServer(t)
	params, err := json.Marshal(dependencyRequest{ModulePath: "example.com/missing", Version: "v9.9.9"})
	require.NoError(t, err)
	if _, rpcErr := s.handleDependencyTypes(params); rpcErr == nil {
		t.Fatal("expected an error for an unresolvable coordinate")
	}
}

// A single ExportedTypes table is paginated across the client's repeated
// identical requests: each call returns at most batchSize items, the
// concatenation reproduces a single-shot run exactly (same items, same order,
// END_OF_OBJECT last), and the cache entry is freed once drained.
func TestExportedTypes_Paginates(t *testing.T) {
	dir := modCacheDir(t, "example.com/foo", "v1.0.0")
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/foo\n\ngo 1.21\n")
	writeFile(t, filepath.Join(dir, "greeter.go"), greeterModule)

	params, err := json.Marshal(dependencyRequest{ModulePath: "example.com/foo", Version: "v1.0.0"})
	require.NoError(t, err)

	// Single-shot reference run with a batch large enough to hold the whole table.
	single, _ := newTestServer(t)
	single.batchSize = 1 << 20
	oneShot, rpcErr := single.handleDependencyTypes(params)
	require.Nil(t, rpcErr)
	want := oneShot.([]rpc.RpcObjectData)
	require.False(t, len(want) == 0 || want[len(want)-1].State != rpc.EndOfObject)

	// Paginated run: same params, tiny batch, drained across repeated calls.
	s, _ := newTestServer(t)
	s.batchSize = 2
	var got []rpc.RpcObjectData
	for calls := 0; ; calls++ {
		if calls > len(want)+2 {
			t.Fatalf("pagination did not terminate after %d calls", calls)
		}
		result, rpcErr := s.handleDependencyTypes(params)
		require.Nil(t, rpcErr)
		batch := result.([]rpc.RpcObjectData)
		if len(batch) > s.batchSize {
			t.Fatalf("batch of %d exceeds batchSize %d", len(batch), s.batchSize)
		}
		got = append(got, batch...)
		if len(batch) > 0 && batch[len(batch)-1].State == rpc.EndOfObject {
			break
		}
	}

	assert.Len(t, s.pendingDependencyTypes, 0)
	if len(got) != len(want) {
		t.Fatalf("paginated total %d != single-shot %d", len(got), len(want))
	}
	for i := range want {
		if !reflect.DeepEqual(got[i], want[i]) {
			t.Fatalf("item %d differs between paginated and single-shot runs:\n paginated=%+v\n single=%+v", i, got[i], want[i])
		}
	}
}

// modCacheDir lays a fixture module out as a module cache entry, points $GOMODCACHE at it,
// and returns the module dir to populate.
func modCacheDir(t *testing.T, modulePath, version string) string {
	t.Helper()
	cache := t.TempDir()
	t.Setenv("GOMODCACHE", cache)
	esc, err := module.EscapePath(modulePath)
	if err != nil {
		t.Fatalf("escape %q: %v", modulePath, err)
	}
	escVer, err := module.EscapeVersion(version)
	if err != nil {
		t.Fatalf("escape %q: %v", version, err)
	}
	return filepath.Join(cache, filepath.FromSlash(esc)+"@"+escVer)
}

// exportedTypesByFqn runs the handler for one coordinate and replays the wire batch into a
// map of enumerated top-level classes keyed by FQN.
func exportedTypesByFqn(t *testing.T, modulePath, version string) map[string]*java.JavaTypeClass {
	t.Helper()
	s, _ := newTestServer(t)
	params, err := json.Marshal(dependencyRequest{ModulePath: modulePath, Version: version})
	require.NoError(t, err)
	result, rpcErr := s.handleDependencyTypes(params)
	require.Nil(t, rpcErr)
	byFqn := map[string]*java.JavaTypeClass{}
	for _, ty := range receiveTypeList(t, result.([]rpc.RpcObjectData)) {
		if cls, ok := ty.(*java.JavaTypeClass); ok {
			byFqn[cls.FullyQualifiedName] = cls
		}
	}
	return byFqn
}

func hasMember(cls *java.JavaTypeClass, name string) bool {
	for _, m := range cls.Members {
		if m.Name == name {
			return true
		}
	}
	return false
}

func hasMethod(cls *java.JavaTypeClass, name string) bool {
	for _, m := range cls.Methods {
		if m.Name == name {
			return true
		}
	}
	return false
}

// receiveTypeList replays the wire batch through the receive queue + java type
// receiver, mirroring GoRewriteRpc.exportedTypes on the Java side: the own-FQN
// string list precedes the types, then END_OF_OBJECT closes the batch.
func receiveTypeList(t *testing.T, data []rpc.RpcObjectData) []java.JavaType {
	t.Helper()
	q := rpc.NewReceiveQueue(make(map[int]any), func() []rpc.RpcObjectData { return data })
	q.ReceiveList(nil, nil) // own-FQN string list, sent before the types
	receiver := rpc.NewJavaTypeReceiver()
	raw := q.ReceiveList(nil, func(v any) any {
		if jt, ok := v.(java.JavaType); ok {
			return receiver.Visit(jt, q)
		}
		return v
	})
	if end := q.Take(); end.State != rpc.EndOfObject {
		t.Fatalf("end marker = %s, want END_OF_OBJECT", end.State)
	}
	out := make([]java.JavaType, 0, len(raw))
	for _, v := range raw {
		if jt, ok := v.(java.JavaType); ok {
			out = append(out, jt)
		}
	}
	return out
}

// receiveFqnList decodes the leading own-FQN string list — the first list on the
// wire, delivered before the types so the caller can tell defined types from
// references up front.
func receiveFqnList(t *testing.T, data []rpc.RpcObjectData) []string {
	t.Helper()
	q := rpc.NewReceiveQueue(make(map[int]any), func() []rpc.RpcObjectData { return data })
	raw := q.ReceiveList(nil, nil)
	out := make([]string, 0, len(raw))
	for _, v := range raw {
		if s, ok := v.(string); ok {
			out = append(out, s)
		}
	}
	return out
}

func keys(m map[string]*java.JavaTypeClass) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	return out
}
