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

package template_test

import (
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// firstCall returns the sole call expression in a source file, so a test can
// state the source it means rather than the tree it produces.
func firstCall(t *testing.T, src string) *java.MethodInvocation {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("a.go", src)
	require.NoError(t, err)
	found := &callFinder{}
	found.Self = found
	found.Visit(cu, nil)
	require.NotNil(t, found.call, "no call in %q", src)
	return found.call
}

const (
	attributed = `package a

import "fmt"

func f() { fmt.Println("x") }
`
	aliased = `package a

import f "fmt"

func g() { f.Println("x") }
`
	unattributed = `package a

import "example.invalid/nowhere"

func f() { nowhere.Println("x") }
`
)

func printlnPattern(mode template.TypeMatchingMode) *template.GoPattern {
	return template.Expression(`fmt.Println("x")`).Imports("fmt").TypeMatching(mode).Build()
}

func TestTypeMatchingOffIgnoresAttribution(t *testing.T) {
	call := firstCall(t, unattributed)
	require.False(t, matcher.IsResolved(call), "candidate should be unattributed")
	// The pattern is attributed and the candidate is not; without type
	// matching neither side's attribution is read.
	pat := template.Expression(`nowhere.Println("x")`).TypeMatching(template.TypeMatchingOff).Build()
	require.True(t, pat.Matches(call, nil))
}

func TestTypeMatchingLenientAcceptsAMissingAttribution(t *testing.T) {
	call := firstCall(t, unattributed)
	pat := template.Expression(`nowhere.Println("x")`).TypeMatching(template.TypeMatchingLenient).Build()
	require.True(t, pat.Matches(call, nil))
}

func TestTypeMatchingStrictRefusesAMissingAttribution(t *testing.T) {
	call := firstCall(t, unattributed)
	pat := template.Expression(`nowhere.Println("x")`).TypeMatching(template.TypeMatchingStrict).Build()
	require.False(t, pat.Matches(call, nil))
}

func TestTypeMatchingRefusesADifferentDeclaringType(t *testing.T) {
	call := firstCall(t, `package a

import "log"

func f() { log.Println("x") }
`)
	require.True(t, matcher.IsResolved(call))
	for _, mode := range []template.TypeMatchingMode{template.TypeMatchingLenient, template.TypeMatchingStrict} {
		require.False(t, printlnPattern(mode).Matches(call, nil), "mode %v", mode)
	}
}

func TestTypeMatchingMatchesThroughAnImportAlias(t *testing.T) {
	call := firstCall(t, aliased)
	require.True(t, matcher.IsResolved(call))
	// Structurally the receiver reads `f`, not `fmt`.
	require.False(t, printlnPattern(template.TypeMatchingOff).Matches(call, nil))
	require.True(t, printlnPattern(template.TypeMatchingLenient).Matches(call, nil))
}

func TestTypeMatchingMatchesTheSameCall(t *testing.T) {
	call := firstCall(t, attributed)
	for _, mode := range []template.TypeMatchingMode{
		template.TypeMatchingOff, template.TypeMatchingLenient, template.TypeMatchingStrict,
	} {
		require.True(t, printlnPattern(mode).Matches(call, nil), "mode %v", mode)
	}
}
