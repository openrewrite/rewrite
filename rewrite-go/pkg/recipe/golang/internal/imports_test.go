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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestPackageNameForPath(t *testing.T) {
	for path, want := range map[string]string{
		"strings":                   "strings",
		"encoding/json":             "json",
		"github.com/pkg/errors":     "errors",
		"encoding/json/v2":          "json",
		"encoding/json/jsontext":    "jsontext",
		"github.com/x/y/v2":         "y",
		"github.com/x/y/v10":        "y",
		"gopkg.in/yaml.v3":          "yaml",
		"gopkg.in/check.v1":         "check",
		"example.com/shipped/mathx": "mathx",
		"v2":                        "v2",
		"github.com/x/version":      "version",
		"github.com/x/v2x":          "v2x",
	} {
		assert.Equal(t, want, packageNameForPath(path), "path %q", path)
	}
}

func parseImports(t *testing.T, paths ...string) []*java.Import {
	t.Helper()
	src := "package p\n\nimport (\n"
	for _, path := range paths {
		src += "\t\"" + path + "\"\n"
	}
	src += ")\n"
	cu, err := parser.NewGoParser().Parse("p.go", src)
	require.NoError(t, err)
	var imps []*java.Import
	for _, rp := range cu.Imports.Elements {
		imps = append(imps, rp.Element)
	}
	return imps
}

func TestResolvedQualifiersNamesWhatAttributionAccountedFor(t *testing.T) {
	imps := parseImports(t, "encoding/json", "encoding/json/v2")
	resolved := ResolvedQualifiers(imps, map[string]bool{"encoding/json/v2": true})
	assert.Equal(t, map[string]bool{"json": true}, resolved)

	assert.Empty(t, ResolvedQualifiers(imps, map[string]bool{}),
		"nothing attributed leaves every qualifier unresolved")
}

func TestResolvedQualifierDoesNotRescueTheSupersededImport(t *testing.T) {
	// Both bind `json`, the shape every major-version package move takes.
	imps := parseImports(t, "encoding/json", "encoding/json/v2")
	refs := map[string]bool{"encoding/json/v2": true}
	quals := map[string]bool{"json": true}
	resolved := ResolvedQualifiers(imps, refs)

	assert.False(t, IsReferenced(imps[0], refs, quals, resolved), "the superseded import must go")
	assert.True(t, IsReferenced(imps[1], refs, quals, resolved), "the one refs names must stay")
}

func TestQualifierFallbackStillKeepsUnattributedImports(t *testing.T) {
	imps := parseImports(t, "encoding/json", "gopkg.in/yaml.v3")
	refs := map[string]bool{}
	quals := map[string]bool{"json": true, "yaml": true}
	resolved := ResolvedQualifiers(imps, refs)

	for _, imp := range imps {
		assert.True(t, IsReferenced(imp, refs, quals, resolved), ImportPath(imp))
	}
}

func TestResolvedQualifierLeavesTheSoleImportAlone(t *testing.T) {
	imps := parseImports(t, "encoding/json/v2")
	refs := map[string]bool{"encoding/json/v2": true}
	resolved := ResolvedQualifiers(imps, refs)

	assert.True(t, IsReferenced(imps[0], refs, map[string]bool{}, resolved))
}
