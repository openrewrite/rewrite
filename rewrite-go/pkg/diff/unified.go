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

// Package diff renders unified diffs for parse-to-print idempotency
// diagnostics. The output mirrors the C# engine's Core/DiffUtils so the
// two report the same shape for the same class of failure.
package diff

import (
	"fmt"
	"math"
	"strings"
)

const (
	contextLines = 3

	// Diffs travel to the JVM inside a ParseError marker, so a file the
	// printer mangles wholesale must not produce a multi-megabyte message.
	maxDiffLines = 200

	// The LCS table is one cell per line pair. Beyond this the diff
	// degrades to a plain delete-all/insert-all listing, which truncation
	// then trims to the leading mismatch.
	maxLCSCells = 4_000_000

	noNewlineMarker = `\ No newline at end of file`
)

// Option adjusts how Unified renders a diff.
type Option func(*config)

type config struct{ maxLines int }

// Unlimited lifts the maxDiffLines cap, for consumers that feed the output to
// a patch tool rather than to a reader.
func Unlimited() Option {
	return func(c *config) { c.maxLines = math.MaxInt }
}

// Unified renders a git-style unified diff of before against after,
// or "" when they are identical.
func Unified(before, after, path string, opts ...Option) string {
	if before == after {
		return ""
	}

	cfg := config{maxLines: maxDiffLines}
	for _, opt := range opts {
		opt(&cfg)
	}

	a := splitLines(before)
	b := splitLines(after)

	// Diff only the region around the mismatch: an idempotency failure is
	// usually a handful of lines in an otherwise identical file.
	offset := commonPrefix(a, b)
	if offset > contextLines {
		offset -= contextLines
	} else {
		offset = 0
	}
	trimBack := commonSuffix(a[offset:], b[offset:])
	if trimBack > contextLines {
		trimBack -= contextLines
	} else {
		trimBack = 0
	}
	a = a[offset : len(a)-trimBack]
	b = b[offset : len(b)-trimBack]

	edits := computeEdits(a, b)

	out := []string{"--- a/" + path, "+++ b/" + path}
	body := make([]string, 0, min(cfg.maxLines, maxDiffLines))
	truncated := false
	for _, hunk := range groupIntoHunks(edits) {
		body, truncated = formatHunk(body, cfg.maxLines, edits, hunk, a, b, offset)
		if truncated {
			break
		}
	}
	out = append(out, body...)
	if truncated {
		out = append(out, fmt.Sprintf("... diff truncated after %d lines", cfg.maxLines))
	}
	return strings.Join(out, "\n") + "\n"
}

// splitLines drops the empty element a trailing newline leaves behind, and
// marks its absence the way git does so that a printer which gains or loses
// the final newline still renders as a visible difference.
func splitLines(text string) []string {
	if text == "" {
		return nil
	}
	lines := strings.Split(text, "\n")
	if lines[len(lines)-1] == "" {
		return lines[:len(lines)-1]
	}
	return append(lines, noNewlineMarker)
}

func commonPrefix(a, b []string) int {
	n := 0
	for n < len(a) && n < len(b) && a[n] == b[n] {
		n++
	}
	return n
}

func commonSuffix(a, b []string) int {
	n := 0
	for n < len(a) && n < len(b) && a[len(a)-1-n] == b[len(b)-1-n] {
		n++
	}
	return n
}

type editKind int

const (
	editEqual editKind = iota
	editDelete
	editInsert
)

type edit struct {
	kind   editKind
	aIndex int
	bIndex int
}

func computeEdits(a, b []string) []edit {
	if len(a)*len(b) > maxLCSCells {
		edits := make([]edit, 0, len(a)+len(b))
		for i := range a {
			edits = append(edits, edit{editDelete, i, -1})
		}
		for j := range b {
			edits = append(edits, edit{editInsert, -1, j})
		}
		return edits
	}

	lcs := computeLCSTable(a, b)
	stride := len(b) + 1
	edits := make([]edit, 0, len(a)+len(b))
	i, j := 0, 0
	for i < len(a) && j < len(b) {
		switch {
		case a[i] == b[j]:
			edits = append(edits, edit{editEqual, i, j})
			i++
			j++
		case lcs[(i+1)*stride+j] >= lcs[i*stride+j+1]:
			edits = append(edits, edit{editDelete, i, -1})
			i++
		default:
			edits = append(edits, edit{editInsert, -1, j})
			j++
		}
	}
	for ; i < len(a); i++ {
		edits = append(edits, edit{editDelete, i, -1})
	}
	for ; j < len(b); j++ {
		edits = append(edits, edit{editInsert, -1, j})
	}
	return edits
}

func computeLCSTable(a, b []string) []int {
	m, n := len(a), len(b)
	stride := n + 1
	dp := make([]int, (m+1)*stride)
	for i := m - 1; i >= 0; i-- {
		for j := n - 1; j >= 0; j-- {
			if a[i] == b[j] {
				dp[i*stride+j] = dp[(i+1)*stride+j+1] + 1
			} else if dp[(i+1)*stride+j] >= dp[i*stride+j+1] {
				dp[i*stride+j] = dp[(i+1)*stride+j]
			} else {
				dp[i*stride+j] = dp[i*stride+j+1]
			}
		}
	}
	return dp
}

type hunkRange struct{ startEdit, endEdit int }

func groupIntoHunks(edits []edit) []hunkRange {
	type span struct{ start, end int }
	var changes []span
	rangeStart := -1
	for idx, e := range edits {
		if e.kind != editEqual {
			if rangeStart < 0 {
				rangeStart = idx
			}
		} else if rangeStart >= 0 {
			changes = append(changes, span{rangeStart, idx - 1})
			rangeStart = -1
		}
	}
	if rangeStart >= 0 {
		changes = append(changes, span{rangeStart, len(edits) - 1})
	}
	if len(changes) == 0 {
		return nil
	}

	var hunks []hunkRange
	curStart := max(0, changes[0].start-contextLines)
	curEnd := min(len(edits)-1, changes[0].end+contextLines)
	for _, c := range changes[1:] {
		nextStart := max(0, c.start-contextLines)
		nextEnd := min(len(edits)-1, c.end+contextLines)
		if nextStart <= curEnd+1 {
			curEnd = nextEnd
			continue
		}
		hunks = append(hunks, hunkRange{curStart, curEnd})
		curStart, curEnd = nextStart, nextEnd
	}
	return append(hunks, hunkRange{curStart, curEnd})
}

// formatHunk appends one hunk to out, stopping and reporting truncation once
// out reaches limit. offset restores the absolute line numbers of the region
// Unified trimmed.
func formatHunk(out []string, limit int, edits []edit, hunk hunkRange, a, b []string, offset int) ([]string, bool) {
	hunkEdits := edits[hunk.startEdit : hunk.endEdit+1]

	// The hunk's own header occupies a line of the budget.
	budget := limit - len(out) - 1
	if budget <= 0 {
		return out, true
	}

	n, truncated := len(hunkEdits), len(hunkEdits) > budget
	if truncated {
		// git refuses a hunk that ends on a +/- line before the end of the
		// file, so the cut lands on the last context line that still leaves a
		// change above it. A change run longer than the budget offers no such
		// line and keeps the raw cut: it has no applicable prefix, and the cut
		// still shows the leading mismatch.
		n = budget
		leading := firstChange(hunkEdits)
		for k := budget; k > leading; k-- {
			if e := hunkEdits[k-1]; e.kind == editEqual && a[e.aIndex] != noNewlineMarker {
				n = k
				break
			}
		}
	}

	// Counting the body first keeps the header's extents equal to what follows.
	aCount, bCount := 0, 0
	aStart, bStart := -1, -1
	body := make([]string, 0, n)
	for _, e := range hunkEdits[:n] {
		if e.kind != editInsert {
			if aStart < 0 {
				aStart = e.aIndex
			}
			if a[e.aIndex] != noNewlineMarker {
				aCount++
			}
		}
		if e.kind != editDelete {
			if bStart < 0 {
				bStart = e.bIndex
			}
			if b[e.bIndex] != noNewlineMarker {
				bCount++
			}
		}
		switch {
		case e.kind == editEqual && a[e.aIndex] == noNewlineMarker:
			body = append(body, noNewlineMarker)
		case e.kind == editEqual:
			body = append(body, " "+a[e.aIndex])
		case e.kind == editDelete && a[e.aIndex] == noNewlineMarker,
			e.kind == editInsert && b[e.bIndex] == noNewlineMarker:
			body = append(body, noNewlineMarker)
		case e.kind == editDelete:
			body = append(body, "-"+a[e.aIndex])
		default:
			body = append(body, "+"+b[e.bIndex])
		}
	}

	out = append(out, fmt.Sprintf("@@ -%d,%d +%d,%d @@",
		rangeStart(aStart, aCount, offset), aCount,
		rangeStart(bStart, bCount, offset), bCount))
	return append(out, body...), truncated
}

// firstChange reports where a hunk's leading context ends: a cut reaching
// further back would leave the hunk with no changes in it.
func firstChange(hunkEdits []edit) int {
	for i, e := range hunkEdits {
		if e.kind != editEqual {
			return i
		}
	}
	return len(hunkEdits)
}

// rangeStart renders a hunk header's line number, using git's 0 for a side the
// hunk contributes no lines to.
func rangeStart(start, count, offset int) int {
	if count == 0 {
		return max(start, 0) + offset
	}
	return start + offset + 1
}
