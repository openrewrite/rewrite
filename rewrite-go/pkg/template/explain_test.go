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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
)

func TestExplainCountsWhatAMissingAttributionDecided(t *testing.T) {
	pat := template.Expression(`nowhere.Println("x")`).
		TypeMatching(template.TypeMatchingStrict).Build()

	why := pat.Explain(firstCall(t, unattributed), nil)
	require.False(t, why.Matched)
	require.Positive(t, why.InconclusiveTypes)
	require.NotEmpty(t, why.FirstInconclusivePath)
}

func TestExplainReportsNothingInconclusiveForARealMismatch(t *testing.T) {
	pat := template.Expression(`fmt.Println("x")`).Imports("fmt").
		TypeMatching(template.TypeMatchingStrict).Build()

	why := pat.Explain(firstCall(t, `package a

import "log"

func f() { log.Println("x") }
`), nil)
	require.False(t, why.Matched)
	require.Zero(t, why.InconclusiveTypes)
}

func TestExplainReportsAMatch(t *testing.T) {
	pat := template.Expression(`fmt.Println("x")`).Imports("fmt").
		TypeMatching(template.TypeMatchingStrict).Build()

	why := pat.Explain(firstCall(t, attributed), nil)
	require.True(t, why.Matched)
	require.Zero(t, why.InconclusiveTypes)
}

func TestExplainCountsNothingWhenTypeMatchingIsOff(t *testing.T) {
	pat := template.Expression(`nowhere.Println("x")`).Build()
	why := pat.Explain(firstCall(t, unattributed), nil)
	require.True(t, why.Matched)
	require.Zero(t, why.InconclusiveTypes)
}
