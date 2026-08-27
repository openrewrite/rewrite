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

package diff

import (
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

func lines(from, to int) string {
	var sb strings.Builder
	for i := from; i <= to; i++ {
		fmt.Fprintf(&sb, "l%d\n", i)
	}
	return sb.String()
}

func TestUnifiedIdentical(t *testing.T) {
	assert.Equal(t, "", Unified(lines(1, 10), lines(1, 10), "foo.go"))
	assert.Equal(t, "", Unified("", "", "foo.go"))
}

func TestUnifiedSingleChangedLine(t *testing.T) {
	before := lines(1, 10)
	after := strings.Replace(before, "l5\n", "L5\n", 1)

	assert.Equal(t, `--- a/foo.go
+++ b/foo.go
@@ -2,7 +2,7 @@
 l2
 l3
 l4
-l5
+L5
 l6
 l7
 l8
`, Unified(before, after, "foo.go"))
}

func TestUnifiedDistantChangesProduceTwoHunks(t *testing.T) {
	before := lines(1, 30)
	after := strings.Replace(before, "l2\n", "L2\n", 1)
	after = strings.Replace(after, "l25\n", "L25\n", 1)

	out := Unified(before, after, "foo.go")
	assert.Equal(t, 2, strings.Count(out, "@@ -"), "expected two hunks in:\n"+out)
	assert.Contains(t, out, "@@ -1,5 +1,5 @@")
	assert.Contains(t, out, "@@ -22,7 +22,7 @@")
}

func TestUnifiedNearbyChangesMergeIntoOneHunk(t *testing.T) {
	before := lines(1, 30)
	after := strings.Replace(before, "l10\n", "L10\n", 1)
	after = strings.Replace(after, "l13\n", "L13\n", 1)

	out := Unified(before, after, "foo.go")
	assert.Equal(t, 1, strings.Count(out, "@@ -"), "expected one merged hunk in:\n"+out)
}

func TestUnifiedPureInsertion(t *testing.T) {
	assert.Equal(t, `--- a/foo.go
+++ b/foo.go
@@ -1,2 +1,3 @@
 l1
+inserted
 l2
`, Unified("l1\nl2\n", "l1\ninserted\nl2\n", "foo.go"))
}

func TestUnifiedTrailingNewlineOnlyDifference(t *testing.T) {
	out := Unified("package main\n", "package main", "foo.go")
	// Splitting on "\n" alone would render this as an empty diff.
	assert.NotEmpty(t, out)
	assert.Contains(t, out, noNewlineMarker)
}

func TestUnifiedNoNewlineMarkerIsAnnotationNotLine(t *testing.T) {
	// git emits the marker unprefixed and outside the @@ line counts.
	assert.Equal(t, `--- a/foo.go
+++ b/foo.go
@@ -1,1 +1,1 @@
 package main
`+noNewlineMarker+`
`, Unified("package main\n", "package main", "foo.go"))
}

func TestUnifiedAgainstEmptyBeforeUsesGitsZeroRange(t *testing.T) {
	assert.Contains(t, Unified("", "l1\nl2\n", "foo.go"), "@@ -0,0 +1,2 @@")
}

func TestUnifiedTruncatesRunawayDiff(t *testing.T) {
	var before, after strings.Builder
	for i := 0; i < maxDiffLines*2; i++ {
		fmt.Fprintf(&before, "a%d\n", i)
		fmt.Fprintf(&after, "b%d\n", i)
	}

	out := Unified(before.String(), after.String(), "foo.go")
	assert.Contains(t, out, "diff truncated")
	assert.LessOrEqual(t, strings.Count(out, "\n"), maxDiffLines+5)
}

func TestUnifiedGiantInputsDoNotBuildFullTable(t *testing.T) {
	var before, after strings.Builder
	for i := 0; i < 40_000; i++ {
		fmt.Fprintf(&before, "a%d\n", i)
		fmt.Fprintf(&after, "b%d\n", i)
	}

	out := Unified(before.String(), after.String(), "foo.go")
	assert.Contains(t, out, "--- a/foo.go")
}

func TestUnifiedLineNumbersAreAbsoluteInLongFiles(t *testing.T) {
	before := lines(1, 5000)
	after := strings.Replace(before, "l4000\n", "L4000\n", 1)

	out := Unified(before, after, "foo.go")
	// Common context is trimmed before the LCS runs; the numbers stay absolute.
	assert.Contains(t, out, "@@ -3997,7 +3997,7 @@")
	assert.Contains(t, out, "-l4000")
	assert.Contains(t, out, "+L4000")
}
