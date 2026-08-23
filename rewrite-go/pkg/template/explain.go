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

package template

import (
	"errors"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// MatchExplanation reports why a match came out as it did. A pattern reading
// attribution answers false against a package go/types could not resolve and
// against source that differs; InconclusiveTypes tells the two apart. See
// PARITY-AUDIT.md for the marker that would let the first be refused outright.
type MatchExplanation struct {
	Matched bool

	// ParseError is set when the pattern itself did not parse, which is not
	// the same answer as source that does not match.
	ParseError error

	// InconclusiveTypes counts the comparisons the mode decided because one
	// side carried no attribution, rather than because the types differed.
	InconclusiveTypes int

	// FirstInconclusivePath names the field path of the first of them, empty
	// at the root.
	FirstInconclusivePath string
}

// Explain matches and reports what the match turned on.
func (p *GoPattern) Explain(candidate java.J, cursor *visitor.Cursor) *MatchExplanation {
	tree, err := p.getTree()
	if err != nil {
		return &MatchExplanation{ParseError: err}
	}
	if tree == nil {
		return &MatchExplanation{ParseError: errNoTree}
	}
	// Matched is what Match answers, so the explanation is of the behaviour
	// the caller sees rather than of another path to the same question.
	cmp := newPatternComparator(p.captures, cursor, p.mode)
	cmp.tracking = true
	why := &MatchExplanation{
		Matched:               cmp.match(tree, candidate) != nil,
		InconclusiveTypes:     cmp.inconclusive,
		FirstInconclusivePath: strings.Join(cmp.firstInconclusive, "."),
	}

	// Only the walk names the field it is at, so it supplies the path where
	// the hand-written comparisons decided.
	if why.InconclusiveTypes > 0 && why.FirstInconclusivePath == "" {
		walk := newPatternComparator(p.captures, cursor, p.mode)
		walk.tracking, walk.skipFastPath = true, true
		walk.match(tree, candidate)
		why.FirstInconclusivePath = strings.Join(walk.firstInconclusive, ".")
	}
	return why
}

// noteInconclusive records a type comparison the mode decided rather than the
// types themselves.
func (c *patternComparator) noteInconclusive() {
	c.inconclusive++
	if !c.noted {
		c.noted = true
		c.firstInconclusive = append([]string(nil), c.path...)
	}
}

var errNoTree = errors.New("pattern produced no tree")
