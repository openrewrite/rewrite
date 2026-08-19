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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const modulesTxt = `# github.com/google/uuid v1.6.0
## explicit; go 1.16
github.com/google/uuid
# golang.org/x/mod v0.35.0
## explicit; go 1.23
golang.org/x/mod/modfile
golang.org/x/mod/semver
# golang.org/x/tools v0.43.0
## go 1.23
golang.org/x/tools/go/ast/astutil
# rsc.io/quote v1.5.2 => rsc.io/quote/v3 v3.1.0
## explicit
rsc.io/quote
`

func TestParseVendorModulesBuildList(t *testing.T) {
	mods, _ := ParseVendorModules(modulesTxt)

	require.Len(t, mods, 4)
	assert.Equal(t, "github.com/google/uuid", mods[0].ModulePath)
	assert.Equal(t, "v1.6.0", mods[0].Version)
	for _, m := range mods {
		assert.Truef(t, m.Selected, "%s is in the vendored build", m.ModulePath)
	}
}

func TestParseVendorModulesMarksNonExplicitIndirect(t *testing.T) {
	mods, _ := ParseVendorModules(modulesTxt)

	byPath := map[string]bool{}
	for _, m := range mods {
		byPath[m.ModulePath] = m.Indirect
	}
	assert.False(t, byPath["github.com/google/uuid"])
	assert.True(t, byPath["golang.org/x/tools"], "no ## explicit marker means it is required transitively")
}

func TestParseVendorModulesCapturesReplacement(t *testing.T) {
	mods, _ := ParseVendorModules(modulesTxt)

	var quote *struct{ path, version string }
	for _, m := range mods {
		if m.ModulePath == "rsc.io/quote" {
			quote = &struct{ path, version string }{m.ReplacePath, m.ReplaceVersion}
		}
	}
	require.NotNil(t, quote)
	assert.Equal(t, "rsc.io/quote/v3", quote.path)
	assert.Equal(t, "v3.1.0", quote.version)
}

func TestParseVendorModulesCapturesModuleGoVersion(t *testing.T) {
	mods, _ := ParseVendorModules(modulesTxt)

	for _, m := range mods {
		if m.ModulePath == "golang.org/x/mod" {
			assert.Equal(t, "1.23", m.ModuleGoVersion)
			return
		}
	}
	t.Fatal("golang.org/x/mod missing from build list")
}

func TestParseVendorModulesPackageMap(t *testing.T) {
	_, pkgs := ParseVendorModules(modulesTxt)

	byImport := map[string]string{}
	for _, p := range pkgs {
		byImport[p.ImportPath] = p.ModulePath
		assert.Falsef(t, p.Standard, "%s is vendored, not stdlib", p.ImportPath)
	}
	assert.Equal(t, "golang.org/x/mod", byImport["golang.org/x/mod/modfile"])
	assert.Equal(t, "golang.org/x/mod", byImport["golang.org/x/mod/semver"])
	assert.Equal(t, "golang.org/x/tools", byImport["golang.org/x/tools/go/ast/astutil"])
	assert.Len(t, pkgs, 5)
}

func TestParseVendorModulesEmpty(t *testing.T) {
	mods, pkgs := ParseVendorModules("")

	assert.NotNil(t, mods)
	assert.Empty(t, mods)
	assert.NotNil(t, pkgs)
	assert.Empty(t, pkgs)
}

func TestParseVendorModulesIgnoresMalformedLines(t *testing.T) {
	mods, pkgs := ParseVendorModules("# nonsense\n## explicit\nsome/pkg\n#\n\n")

	assert.Empty(t, mods, "a `# module` line without a version names no module version")
	assert.Empty(t, pkgs, "a package line outside any module belongs to nothing")
}
