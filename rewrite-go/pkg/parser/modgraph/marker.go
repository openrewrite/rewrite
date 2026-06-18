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

package modgraph

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"

// ToResolutionResult folds a resolved Result into a GoResolutionResult marker,
// populating the module-graph fields (BuildList/Graph/GraphComplete). The
// caller supplies the module identity fields parsed from go.mod. This is the
// single mapping used by both the parser wiring and the parity harness.
func ToResolutionResult(res Result, modulePath, goVersion, toolchain, path string) golang.GoResolutionResult {
	m := golang.NewGoResolutionResult(modulePath, goVersion, toolchain, path)
	ApplyTo(res, &m)
	return m
}

// FromMarker reconstructs a resolver Result from the module-graph fields of a
// GoResolutionResult marker, so recipes can run NeededModules (the package-import
// graph) at recipe time against the parse-time-resolved graph without re-fetching
// dependency go.mod files.
func FromMarker(m golang.GoResolutionResult) Result {
	res := Result{Complete: m.GraphComplete}
	for _, b := range m.BuildList {
		res.BuildList = append(res.BuildList, Module{
			Path:       b.ModulePath,
			Version:    b.Version,
			GoVersion:  b.GoVersion,
			Main:       b.Main,
			ModuleHash: b.ModuleHash,
			GoModHash:  b.GoModHash,
		})
	}
	for _, e := range m.Graph {
		res.Graph = append(res.Graph, Edge{
			FromPath:    e.FromPath,
			FromVersion: e.FromVersion,
			ToPath:      e.ToPath,
			ToVersion:   e.ToVersion,
			Indirect:    e.Indirect,
		})
	}
	return res
}

// ApplyTo populates the module-graph fields (BuildList/Graph/GraphComplete) of
// an existing GoResolutionResult marker from a resolved Result. Used by the RPC
// parser to enrich the marker it already built from the go.mod text.
func ApplyTo(res Result, m *golang.GoResolutionResult) {
	m.BuildList = make([]golang.GoModule, 0, len(res.BuildList))
	for _, b := range res.BuildList {
		m.BuildList = append(m.BuildList, golang.GoModule{
			ModulePath: b.Path,
			Version:    b.Version,
			GoVersion:  b.GoVersion,
			Main:       b.Main,
			ModuleHash: b.ModuleHash,
			GoModHash:  b.GoModHash,
		})
	}
	m.Graph = make([]golang.GoModuleEdge, 0, len(res.Graph))
	for _, e := range res.Graph {
		m.Graph = append(m.Graph, golang.GoModuleEdge{
			FromPath:    e.FromPath,
			FromVersion: e.FromVersion,
			ToPath:      e.ToPath,
			ToVersion:   e.ToVersion,
			Indirect:    e.Indirect,
		})
	}
	m.GraphComplete = res.Complete
}
