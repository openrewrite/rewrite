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
	"reflect"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// expectUnparsedSourcesHandled runs the recipe without a source of each kind it
// changed, once per process, as a Go recipe meets a file the parser failed on,
// left unparsed or read as plain text, failing t if the recipe panics there.
func (spec *RecipeSpec) expectUnparsedSourcesHandled(t testing.TB, run *recipeRun) {
	t.Helper()
	if spec.Recipe == nil {
		return
	}
	for i, ps := range run.parsed {
		if ps.tree == nil || ps.result == ps.tree {
			continue
		}
		failure := probes.once(spec.Recipe, reflect.TypeOf(ps.tree), "missing", func() string {
			var others []java.Tree
			for j, other := range run.parsed {
				if j != i && other.tree != nil {
					others = append(others, other.tree)
				}
			}
			if p := probe(spec.Recipe, others, run.freshParseContext, spec.cycles()); p != nil {
				return unparsedSourceFailure(spec, sourcePathOf(ps.tree), p)
			}
			return ""
		})
		if failure != "" {
			t.Error(failure)
		}
	}
}

func unparsedSourceFailure(spec *RecipeSpec, sourcePath string, p *probePanic) string {
	return "Check that the scanner found the files the recipe needs before using what it keeps of them, so that " +
		"the recipe changes nothing instead of panicking when one is missing.\n" +
		spec.Recipe.Name() + " panicked when " + sourcePath + " was missing, which is how a Go recipe sees a file " +
		"its parser failed on, left unparsed or read as plain text:\n" +
		p.describe(spec.Recipe, "    ") + "\n" +
		"Besides the files the test gives it, a repository holds files their parser failed on, files left " +
		"unparsed and files read as plain text. The host sends a Go recipe only the files it parsed as Go " +
		"source, go.mod or go.sum, so to the recipe such a file is not there at all.\n" +
		"  - when the scanner found no file of the kind the recipe needs, generate and change nothing, instead " +
		"of reading an empty accumulator\n" +
		"  - check for nil before dereferencing what the scanner keeps of a file it may not find\n" +
		"  - don't recover from the panic to make this pass"
}
