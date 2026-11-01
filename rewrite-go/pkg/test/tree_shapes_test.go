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
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// replaceOne replaces each literal `1` with what replace builds.
type replaceOne struct {
	recipe.Base
	replace func(lit *java.Literal) java.J
}

func (r *replaceOne) Name() string        { return "org.openrewrite.golang.test.ReplaceOne" }
func (r *replaceOne) DisplayName() string { return "Replace one" }
func (r *replaceOne) Description() string { return "Replaces each literal `1`." }

func (r *replaceOne) Editor() recipe.TreeVisitor {
	return visitor.Init(&oneReplacer{replace: r.replace})
}

type oneReplacer struct {
	visitor.GoVisitor
	replace func(lit *java.Literal) java.J
}

func (v *oneReplacer) VisitLiteral(lit *java.Literal, _ any) java.J {
	if lit.Source != "1" {
		return lit
	}
	return v.replace(lit)
}

const assignsOne = `
	package main

	func main() {
		x := 1
		_ = x
	}
`

const assignsTwo = `
	package main

	func main() {
		x := 2
		_ = x
	}
`

func TestNodeTheParserBuildsDifferentlyFails(t *testing.T) {
	// given a recipe that writes a number as an identifier
	spec := NewRecipeSpec().WithRecipe(&replaceOne{replace: func(lit *java.Literal) java.J {
		return &java.Identifier{ID: uuid.New(), Prefix: lit.Prefix, Markers: lit.Markers, Name: "2"}
	}})

	// when it runs
	got := failures(t, spec, Golang(assignsOne, assignsTwo))

	// then the run fails, naming the node and what the parser makes of its text
	assert.Equal(t, []string{
		"Write code with a template from pkg/template or with the parser, or build the nodes the way the parser " +
			"builds the text they print. The recipe's output holds nodes that the parser makes differently of " +
			"the same text:\n" +
			"  test.go:\n" +
			"    `2` is a java.Identifier where the parser makes a java.Literal\n" +
			"A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that " +
			"parsed code never holds. If the output is meant to differ from parsed code, set " +
			"spec.CheckParserShapedTrees = false.",
	}, got)
}

func TestLiteralWhoseValueDisagreesWithItsSourceFails(t *testing.T) {
	// given a recipe that changes the source of a literal but not its value
	spec := NewRecipeSpec().WithRecipe(&replaceOne{replace: func(lit *java.Literal) java.J {
		c := *lit
		c.Source = "2"
		return &c
	}})

	// when it runs
	got := failures(t, spec, Golang(assignsOne, assignsTwo))

	// then the value is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    `2` has the int64 value 1 where the parser makes the int64 value 2\n")
}

func TestOutputThatDoesNotParseFails(t *testing.T) {
	// given a recipe that writes a name with a space in it
	spec := NewRecipeSpec().WithRecipe(&replaceOne{replace: func(lit *java.Literal) java.J {
		return &java.Identifier{ID: uuid.New(), Prefix: lit.Prefix, Markers: lit.Markers, Name: "a b"}
	}})

	// when it runs
	got := failures(t, spec, Golang(assignsOne, `
		package main

		func main() {
			x := a b
			_ = x
		}
	`))

	// then the parse error is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  test.go:\n    the text it prints doesn't parse: ")
}

func TestNodeTheParserBuildsTheSamePasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&replaceOne{replace: func(lit *java.Literal) java.J {
			c := *lit
			c.Source = "2"
			c.Value = int64(2)
			return &c
		}}).
		RewriteRun(t, Golang(assignsOne, assignsTwo))
}

func TestTreeShapesCheckCanBeTurnedOff(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&replaceOne{replace: func(lit *java.Literal) java.J {
		return &java.Identifier{ID: uuid.New(), Prefix: lit.Prefix, Markers: lit.Markers, Name: "2"}
	}})
	spec.CheckParserShapedTrees = false
	spec.RewriteRun(t, Golang(assignsOne, assignsTwo))
}
