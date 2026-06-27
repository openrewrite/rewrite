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

package golang

import (
	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TidyRequireSet is the result of the `go mod tidy` require-set computation,
// which is performed by the pure-Java module resolver on the host side. The Go
// recipe sends the go.mod and scanned imports over RPC and receives this back.
type TidyRequireSet struct {
	// Direct maps module path -> version for modules imported by the main module.
	Direct map[string]string
	// Indirect maps module path -> version for transitively-needed modules.
	Indirect map[string]string
	// Complete is false if resolution was best-effort (e.g. a package directory
	// could not be read); the recipe then falls back to its LST-only path.
	Complete bool
}

// TidyResolver computes the exact tidy require set for an edited go.mod. The RPC
// server installs an implementation that delegates to the Java resolver (which
// performs all GOPROXY HTTP through the CLI HttpSender). ok is false when no
// resolver is installed (e.g. running outside the CLI).
type TidyResolver func(content string, mainImports []string, modulePath string, separateIndirect bool) (TidyRequireSet, bool)

// tidyResolverKey is the ExecutionContext key under which the RPC server installs
// the TidyResolver.
const tidyResolverKey = "org.openrewrite.golang.modgraph.tidyResolver"

// SetTidyResolver installs the recipe-time tidy resolver. Called by the RPC
// server when it builds the recipe ExecutionContext.
func SetTidyResolver(ctx *recipe.ExecutionContext, fn TidyResolver) {
	ctx.PutMessage(tidyResolverKey, fn)
}

// TidyResolverFrom returns the installed TidyResolver, or nil when none is
// present (e.g. running outside the CLI, with no network access configured).
func TidyResolverFrom(ctx *recipe.ExecutionContext) TidyResolver {
	if v, ok := ctx.GetMessage(tidyResolverKey); ok {
		if fn, ok := v.(TidyResolver); ok {
			return fn
		}
	}
	return nil
}

// RefreshModel recomputes the GoResolutionResult for an (already-edited) go.mod
// and returns the new marker. It re-parses the declared model from the current
// go.mod text (module path, requires, replaces, excludes, retracts, go directive)
// so later recipes in the same run see an accurate declared view. The resolved
// build list/graph is no longer carried in the marker — recipes that need the
// tidy require set obtain it from the Java resolver via the TidyResolver.
//
// Returns ok=false only if the go.mod cannot be parsed.
func RefreshModel(gm *golang.GoMod, ctx *recipe.ExecutionContext) (golang.GoResolutionResult, bool) {
	content := printer.PrintGoMod(gm)
	mrr, err := goparser.ParseGoMod("go.mod", content)
	if err != nil || mrr == nil {
		return golang.GoResolutionResult{}, false
	}
	// Carry over the go.sum-derived resolutions from the prior marker — a
	// dependency edit changes go.mod, not go.sum.
	if prev := GetResolutionResult(gm); prev != nil {
		mrr.ResolvedDependencies = prev.ResolvedDependencies
	}
	return *mrr, true
}

// GetResolutionResult returns the current GoResolutionResult marker on a go.mod,
// or nil. Recipes use it to read the resolved model (build list, graph).
func GetResolutionResult(gm *golang.GoMod) *golang.GoResolutionResult {
	for i := range gm.Markers.Entries {
		if r, ok := gm.Markers.Entries[i].(golang.GoResolutionResult); ok {
			return &r
		}
	}
	return nil
}

// replaceResolution returns markers with any existing GoResolutionResult swapped
// for the freshly-resolved one.
func replaceResolution(m java.Markers, mrr golang.GoResolutionResult) java.Markers {
	out := make([]java.Marker, 0, len(m.Entries)+1)
	for _, e := range m.Entries {
		if _, ok := e.(golang.GoResolutionResult); ok {
			continue
		}
		out = append(out, e)
	}
	out = append(out, mrr)
	return java.Markers{ID: m.ID, Entries: out}
}
