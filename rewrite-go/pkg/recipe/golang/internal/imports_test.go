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

package internal

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

func TestPackageNameForPath(t *testing.T) {
	for path, want := range map[string]string{
		"strings":                             "strings",
		"encoding/json":                       "json",
		"github.com/pkg/errors":               "errors",
		"encoding/json/v2":                    "json",
		"encoding/json/jsontext":              "jsontext",
		"github.com/x/y/v2":                   "y",
		"github.com/x/y/v10":                  "y",
		"gopkg.in/yaml.v3":                    "yaml",
		"gopkg.in/check.v1":                   "check",
		"example.com/shipped/mathx":           "mathx",
		"v2":                                  "v2",
		"github.com/x/version":                "version",
		"github.com/x/v2x":                    "v2x",
		"github.com/pelletier/go-toml/v2":     "toml",
		"github.com/goccy/go-yaml":            "yaml",
		"github.com/dustin/go-humanize":       "humanize",
		"github.com/mattn/go-isatty":          "isatty",
		"github.com/nats-io/nats.go":          "nats",
		"k8s.io/client-go":                    "client",
		"github.com/go-viper/mapstructure/v2": "mapstructure",
		"go.uber.org/zap":                     "zap",
		"github.com/x/go":                     "go",
	} {
		assert.Equal(t, want, packageNameForPath(path), "path %q", path)
	}
}

func TestPkgPathOf(t *testing.T) {
	for fqn, want := range map[string]string{
		"gopkg.in/yaml.v3":         "gopkg.in/yaml.v3",
		"gopkg.in/check.v1":        "gopkg.in/check.v1",
		"gopkg.in/yaml.v3.Node":    "gopkg.in/yaml.v3",
		"example.com/pkg.TypeName": "example.com/pkg",
		"example.com/pkg.V2":       "example.com/pkg",
		"github.com/x/y/v2":        "github.com/x/y/v2",
		"encoding/json/v2":         "encoding/json/v2",
		"fmt.Println":              "fmt",
		"fmt":                      "fmt",
		"":                         "",
	} {
		assert.Equal(t, want, pkgPathOf(fqn), "fqn %q", fqn)
	}
}

func TestIsIdentifier(t *testing.T) {
	for s, want := range map[string]bool{
		"toml": true, "x9": true, "_x": true, "": false,
		"go-toml": false, "nats.go": false, "9x": false,
	} {
		assert.Equal(t, want, isIdentifier(s), "%q", s)
	}
}

func TestImportUsesReferenced(t *testing.T) {
	blank := "_"
	rand1 := NewImport("math/rand", nil)
	rand2 := NewImport("math/rand/v2", nil)
	fmtImp := NewImport("fmt", nil)
	pprof := NewImport("net/http/pprof", &blank)

	attributed := ImportUses{
		refs:          map[string]bool{"math/rand/v2": true},
		quals:         map[string]bool{"rand": true},
		resolvedQuals: map[string]bool{"rand": true},
	}
	assert.True(t, attributed.Referenced(rand2), "attribution names the path")
	assert.False(t, attributed.Referenced(rand1),
		"the qualifier is spoken for, so refs passing over this import is an answer")

	lexical := ImportUses{
		refs:          map[string]bool{},
		quals:         map[string]bool{"rand": true},
		resolvedQuals: map[string]bool{},
	}
	assert.True(t, lexical.Referenced(rand1), "the qualifier stands in for attribution")
	assert.False(t, lexical.Referenced(fmtImp))
	assert.False(t, lexical.Referenced(pprof), "a blank import binds no qualifier")
	assert.True(t, lexical.Referenced(NewImport("github.com/x/foo-bar", nil)),
		"no qualifier can spell this name, so its absence says nothing")
}

func TestUsesOfReadsAttributionAndQualifiers(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("f.go", `package main

import (
	"fmt"
	"strings"

	"github.com/x/y"
)

func f() {
	fmt.Println(y.Hello())
}
`)
	require.NoError(t, err)

	uses := UsesOf(cu)
	assert.Equal(t, map[string]bool{"fmt": true, "y": true}, uses.quals)
	assert.Equal(t, map[string]bool{"fmt": true, "y": true}, uses.resolvedQuals,
		"a qualifier binding under the name its path spells resolves to that path")
	assert.False(t, uses.refs["strings"], "an import the body never names")

	imports := ImportsOf(cu)
	require.Len(t, imports, 3)
	assert.True(t, uses.Referenced(imports[0]), "fmt")
	assert.False(t, uses.Referenced(imports[1]), "strings")
	assert.True(t, uses.Referenced(imports[2]), "github.com/x/y")
}
