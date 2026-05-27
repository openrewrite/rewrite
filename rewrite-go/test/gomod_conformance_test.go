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
	"encoding/json"
	"os"
	"path/filepath"
	"reflect"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// conformanceShape is the canonical JSON form used by both Java and Go
// conformance tests. Field names and ordering MUST stay in sync with the
// Java GoModConformanceTest's matching record / class.
type conformanceShape struct {
	ModulePath           string                       `json:"modulePath"`
	GoVersion            string                       `json:"goVersion"`
	Toolchain            string                       `json:"toolchain"`
	Requires             []conformanceRequire         `json:"requires"`
	Replaces             []conformanceReplace         `json:"replaces"`
	Excludes             []conformanceExclude         `json:"excludes"`
	Retracts             []conformanceRetract         `json:"retracts"`
	ResolvedDependencies []conformanceResolvedDep     `json:"resolvedDependencies"`
}

type conformanceRequire struct {
	ModulePath string `json:"modulePath"`
	Version    string `json:"version"`
	Indirect   bool   `json:"indirect"`
}

type conformanceReplace struct {
	OldPath    string  `json:"oldPath"`
	OldVersion *string `json:"oldVersion"`
	NewPath    string  `json:"newPath"`
	NewVersion *string `json:"newVersion"`
}

type conformanceExclude struct {
	ModulePath string `json:"modulePath"`
	Version    string `json:"version"`
}

type conformanceRetract struct {
	VersionRange string  `json:"versionRange"`
	Rationale    *string `json:"rationale"`
}

type conformanceResolvedDep struct {
	ModulePath string  `json:"modulePath"`
	Version    string  `json:"version"`
	ModuleHash *string `json:"moduleHash"`
	GoModHash  *string `json:"goModHash"`
}

func toConformance(mrr *tree.GoResolutionResult) conformanceShape {
	out := conformanceShape{
		ModulePath:           mrr.ModulePath,
		GoVersion:            mrr.GoVersion,
		Toolchain:            mrr.Toolchain,
		Requires:             []conformanceRequire{},
		Replaces:             []conformanceReplace{},
		Excludes:             []conformanceExclude{},
		Retracts:             []conformanceRetract{},
		ResolvedDependencies: []conformanceResolvedDep{},
	}
	for _, r := range mrr.Requires {
		out.Requires = append(out.Requires, conformanceRequire{
			ModulePath: r.ModulePath,
			Version:    r.Version,
			Indirect:   r.Indirect,
		})
	}
	for _, r := range mrr.Replaces {
		out.Replaces = append(out.Replaces, conformanceReplace{
			OldPath:    r.OldPath,
			OldVersion: nilIfEmpty(r.OldVersion),
			NewPath:    r.NewPath,
			NewVersion: nilIfEmpty(r.NewVersion),
		})
	}
	for _, e := range mrr.Excludes {
		out.Excludes = append(out.Excludes, conformanceExclude{
			ModulePath: e.ModulePath,
			Version:    e.Version,
		})
	}
	for _, r := range mrr.Retracts {
		out.Retracts = append(out.Retracts, conformanceRetract{
			VersionRange: r.VersionRange,
			Rationale:    nilIfEmpty(r.Rationale),
		})
	}
	for _, d := range mrr.ResolvedDependencies {
		out.ResolvedDependencies = append(out.ResolvedDependencies, conformanceResolvedDep{
			ModulePath: d.ModulePath,
			Version:    d.Version,
			ModuleHash: nilIfEmpty(d.ModuleHash),
			GoModHash:  nilIfEmpty(d.GoModHash),
		})
	}
	return out
}

func nilIfEmpty(s string) *string {
	if s == "" {
		return nil
	}
	return &s
}

// TestGoModConformanceCorpus iterates every .gomod under
// src/test/resources/gomod-conformance/, parses it (with sibling .gosum if
// present), and compares the result to the corresponding .gomod.json
// golden. The Java GoModConformanceTest runs the same corpus.
func TestGoModConformanceCorpus(t *testing.T) {
	corpusDir := filepath.Join("..", "src", "test", "resources", "gomod-conformance")
	entries, err := os.ReadDir(corpusDir)
	if err != nil {
		t.Fatalf("read corpus dir: %v", err)
	}
	cases := 0
	for _, ent := range entries {
		if ent.IsDir() || !strings.HasSuffix(ent.Name(), ".gomod") {
			continue
		}
		cases++
		caseName := strings.TrimSuffix(ent.Name(), ".gomod")
		t.Run(caseName, func(t *testing.T) {
			modContent := mustRead(t, filepath.Join(corpusDir, ent.Name()))
			mrr, err := parser.ParseGoMod("go.mod", modContent)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			if sumPath := filepath.Join(corpusDir, caseName+".gosum"); fileExists(sumPath) {
				mrr.ResolvedDependencies = parser.ParseGoSum(mustRead(t, sumPath))
			}
			actual := toConformance(mrr)

			goldenContent := mustRead(t, filepath.Join(corpusDir, caseName+".gomod.json"))
			var expected conformanceShape
			if err := json.Unmarshal([]byte(goldenContent), &expected); err != nil {
				t.Fatalf("unmarshal golden: %v", err)
			}
			normalize(&expected)

			if !reflect.DeepEqual(actual, expected) {
				actualJSON, _ := json.MarshalIndent(actual, "", "  ")
				expectedJSON, _ := json.MarshalIndent(expected, "", "  ")
				t.Errorf("conformance mismatch\nactual:\n%s\n\nexpected:\n%s",
					string(actualJSON), string(expectedJSON))
			}
		})
	}
	if cases == 0 {
		t.Fatal("no .gomod cases found in corpus")
	}
}

// normalize replaces nil slices with empty slices so reflect.DeepEqual
// matches the conformance shape produced by toConformance.
func normalize(c *conformanceShape) {
	if c.Requires == nil {
		c.Requires = []conformanceRequire{}
	}
	if c.Replaces == nil {
		c.Replaces = []conformanceReplace{}
	}
	if c.Excludes == nil {
		c.Excludes = []conformanceExclude{}
	}
	if c.Retracts == nil {
		c.Retracts = []conformanceRetract{}
	}
	if c.ResolvedDependencies == nil {
		c.ResolvedDependencies = []conformanceResolvedDep{}
	}
}

func mustRead(t *testing.T, path string) string {
	t.Helper()
	b, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read %s: %v", path, err)
	}
	return string(b)
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}
