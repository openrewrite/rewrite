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
	"go/constant"
	"go/token"
	"go/types"
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
