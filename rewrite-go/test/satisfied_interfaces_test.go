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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

const store = `package shop

import "io"

type Store interface {
	Get(key string) string
}

type ReadStore interface {
	Store
	io.Reader
}

type memory struct{}

func (m *memory) Get(key string) string { return key }

func (m *memory) Write(p []byte) (int, error) { return len(p), nil }

type label string

func (l label) Get(key string) string { return string(l) }

type cached struct {
	*memory
}

type keys []string

type notFound struct{ key string }

func (e notFound) Error() string { return e.key }

type Box[T any] struct{ v T }

func (b Box[T]) Get(key string) string { return key }
`

func TestATypeNamesTheInterfacesItsMethodsSatisfy(t *testing.T) {
	declared := declaredClasses(t, store)

	assert.Equal(t, []string{"io.Writer", "shop.Store"}, interfaceNames(declared["memory"]),
		"methods with pointer receivers satisfy an interface for the type")
	assert.Equal(t, []string{"shop.Store"}, interfaceNames(declared["label"]))
	assert.Equal(t, []string{"io.Writer", "shop.Store"}, interfaceNames(declared["cached"]),
		"methods promoted from an embedded field count")
	assert.Empty(t, interfaceNames(declared["keys"]))
	assert.Equal(t, []string{"error"}, interfaceNames(declared["notFound"]), "the predeclared error is visible everywhere")
}

func TestAnInterfaceNamesTheInterfacesItEmbedsButNotItself(t *testing.T) {
	declared := declaredClasses(t, store)

	assert.Equal(t, []string{"io.Reader", "shop.Store"}, interfaceNames(declared["ReadStore"]))
	assert.Empty(t, interfaceNames(declared["Store"]))
}

func TestAGenericTypeNamesNoInterfacesSinceItsMethodSetVariesWithItsArguments(t *testing.T) {
	assert.Empty(t, interfaceNames(declaredClasses(t, store)["Box"]))
}

// io reaches the package only through net/http's export data, which names a few of io's interfaces until a package
// that imports io is parsed. What a type satisfies is the same either way.
func TestWhatATypeSatisfiesDoesNotDependOnWhatWasParsedBefore(t *testing.T) {
	page := `package shop

import "net/http"

type page struct{}

func (p page) WriteString(s string) (int, error) { return len(s), nil }

var _ http.Handler
`
	alone := interfaceNames(declaredClasses(t, page)["page"])

	p := parser.NewGoParser()
	_, err := p.Parse("writer.go", "package other\n\nimport \"io\"\n\nvar _ io.StringWriter\n")
	require.NoError(t, err)
	cu, err := p.Parse("page.go", page)
	require.NoError(t, err)
	v := visitor.Init(&declaredClassCollector{classes: make(map[string]java.JavaType)})
	v.Visit(cu, nil)

	assert.Equal(t, alone, interfaceNames(v.classes["page"]))
}

// A package of the module can type-assert to its unexported interface a value of a type it does not declare.
func TestATypeNamesTheUnexportedInterfacesOfItsModulesPackagesItImports(t *testing.T) {
	files := map[string]string{
		"store/store.go": `package store

type namer interface {
	Name() string
}

func Label(v any) string {
	if n, ok := v.(namer); ok {
		return n.Name()
	}
	return ""
}
`,
		"store/disk/disk.go": `package disk

import "example.com/shop/store"

type File struct{ path string }

func (f File) Name() string { return f.path }

var _ = store.Label
`,
	}
	pi := parser.NewProjectImporter("example.com/shop", nil)
	pi.SetProjectRoot(t.TempDir())
	for rel, content := range files {
		pi.AddSource(rel, content)
	}
	p := parser.NewGoParser()
	p.Importer = pi
	cu, err := p.Parse("store/disk/disk.go", files["store/disk/disk.go"])
	require.NoError(t, err)
	v := visitor.Init(&declaredClassCollector{classes: make(map[string]java.JavaType)})
	v.Visit(cu, nil)

	assert.Equal(t, []string{"example.com/shop/store.namer"}, interfaceNames(v.classes["File"]))
}

// declaredClassCollector collects the class each type declaration declares, by the declared name.
type declaredClassCollector struct {
	visitor.GoVisitor
	classes map[string]java.JavaType
}

func (v *declaredClassCollector) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	if td.Name != nil && td.Name.Type != nil {
		v.classes[td.Name.Name] = td.Name.Type
	}
	return v.GoVisitor.VisitTypeDecl(td, p)
}

func declaredClasses(t *testing.T, source string) map[string]java.JavaType {
	cu, err := parser.NewGoParser().Parse("store.go", source)
	require.NoError(t, err)
	v := visitor.Init(&declaredClassCollector{classes: make(map[string]java.JavaType)})
	v.Visit(cu, nil)
	return v.classes
}

func interfaceNames(t java.JavaType) []string {
	cls, ok := t.(*java.JavaTypeClass)
	if !ok {
		return nil
	}
	var names []string
	for _, iface := range cls.Interfaces {
		names = append(names, iface.GetFullyQualifiedName())
	}
	return names
}
