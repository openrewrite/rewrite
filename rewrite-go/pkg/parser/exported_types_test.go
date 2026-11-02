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

package parser

import (
	"go/ast"
	"go/constant"
	"go/importer"
	goparser "go/parser"
	"go/token"
	"go/types"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestGoBuildContextIsHostIndependent(t *testing.T) {
	ctx := goBuildContext()
	for _, f := range []struct{ field, got, want string }{
		{"GOOS", ctx.GOOS, "linux"},
		{"GOARCH", ctx.GOARCH, "amd64"},
		{"Compiler", ctx.Compiler, "gc"},
	} {
		if f.got != f.want {
			t.Errorf("%s = %q, want %q", f.field, f.got, f.want)
		}
	}
	if ctx.CgoEnabled {
		t.Error("CgoEnabled = true, want false")
	}
}

func TestEnumeratePackageFlagsPackageLevelVariablesPublic(t *testing.T) {
	pkg := types.NewPackage("example.com/p", "p")
	pkg.Scope().Insert(types.NewVar(token.NoPos, pkg, "V", types.Typ[types.Int]))
	pkg.Scope().Insert(types.NewConst(token.NoPos, pkg, "C", types.Typ[types.Int], constant.MakeInt64(1)))

	var pkgClass *java.JavaTypeClass
	enumeratePackage(pkg, "example.com/p", newTypeMapper(), func(c *java.JavaTypeClass) { pkgClass = c })

	require.NotNil(t, pkgClass)
	flags := map[string]int64{}
	for _, member := range pkgClass.Members {
		flags[member.Name] = member.FlagsBitMap
	}
	assert.Equal(t, map[string]int64{"V": 1, "C": 1}, flags)
}

func TestImporterEnumeratesTheAPIOfAFullCheck(t *testing.T) {
	api := `package api

import (
	"io"
	"strings"
)

type Option func(*Config)

type Config struct {
	Name  string
	Out   io.Writer
	Items []Item[string]
}

type Item[T any] struct {
	Value T
}

func (i Item[T]) Get() T { return i.Value }

// Typed by the literal's signature, whose body is the part left unchecked.
var Default = func() *Config { return &Config{Name: strings.ToUpper("default")} }()

const (
	First = iota
	Second
)

func New(opts ...Option) *Config {
	c := &Config{}
	for _, opt := range opts {
		opt(c)
	}
	return c
}

func Map[T, U any](in []T, f func(T) U) []U {
	out := make([]U, 0, len(in))
	for _, v := range in {
		out = append(out, f(v))
	}
	return out
}
`
	fset := token.NewFileSet()
	file, err := goparser.ParseFile(fset, "api.go", api, 0)
	require.NoError(t, err)
	checked, err := (&types.Config{Importer: importer.Default()}).Check("example.com/api", fset, []*ast.File{file}, nil)
	require.NoError(t, err)
	full := enumeratedAPI(checked)
	require.NotEmpty(t, full)

	pi := NewProjectImporter("example.com/api", nil)
	pi.AddSource("api.go", api)
	imported, err := pi.Import("example.com/api")
	require.NoError(t, err)
	assert.Equal(t, full, enumeratedAPI(imported))
}

func enumeratedAPI(pkg *types.Package) []string {
	var described []string
	enumeratePackage(pkg, "example.com/api", newTypeMapper(), func(c *java.JavaTypeClass) {
		if c == nil {
			return
		}
		described = append(described, c.Kind+" "+c.FullyQualifiedName)
		for _, m := range c.Members {
			described = append(described, "  "+m.Name+" "+java.TypeSignature(m.Type))
		}
		for _, m := range c.Methods {
			params := make([]string, len(m.ParameterTypes))
			for i, p := range m.ParameterTypes {
				params[i] = java.TypeSignature(p)
			}
			described = append(described, "  "+m.Name+"("+strings.Join(params, ", ")+") "+java.TypeSignature(m.ReturnType))
		}
	})
	return described
}
