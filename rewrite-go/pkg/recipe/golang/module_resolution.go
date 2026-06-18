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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser/modgraph"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// modSourceKey is the ExecutionContext key under which the RPC server installs
// the dependency-resolution ModSource (cache + GOPROXY). The proxy tier routes
// HTTP through the CLI's HttpSender, so recipe-time resolution may hit the
// network exactly as parse-time resolution does.
const modSourceKey = "org.openrewrite.golang.modgraph.source"

// SetModSource installs the ModSource used by recipe-time module-graph
// resolution. Called by the RPC server when it builds the recipe
// ExecutionContext.
func SetModSource(ctx *recipe.ExecutionContext, src modgraph.ModSource) {
	ctx.PutMessage(modSourceKey, src)
}

// ModSourceFrom returns the installed ModSource, or nil when none is present
// (e.g. running outside the CLI, with no network access configured).
func ModSourceFrom(ctx *recipe.ExecutionContext) modgraph.ModSource {
	if v, ok := ctx.GetMessage(modSourceKey); ok {
		if s, ok := v.(modgraph.ModSource); ok {
			return s
		}
	}
	return nil
}

// RefreshModel recomputes the GoResolutionResult for an (already-edited) go.mod
// and returns the new marker. It re-parses the declared model from the current
// go.mod text (module path, requires, replaces, excludes, retracts, go directive)
// and, when a ModSource is installed in ctx, re-resolves the module graph + build
// list — fetching dependency metadata via that source, which may reach the
// network through the CLI HttpSender.
//
// This is the mechanism by which a recipe that mutates dependencies refreshes the
// model so later recipes in the same run see an accurate view. Mirrors
// rewrite-maven's UpdateMavenModel. The declared `Requires` stay faithful to the
// file; the resolved view rides in BuildList/Graph.
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
	if src := ModSourceFrom(ctx); src != nil {
		if res, e := modgraph.Resolve([]byte(content), src); e == nil {
			modgraph.ApplyTo(res, mrr)
		}
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
