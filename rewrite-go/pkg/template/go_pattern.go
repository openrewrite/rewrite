/*
 * Copyright 2025 the original author or authors.
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
	"reflect"
	"sync"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoPattern represents a parsed code pattern that can be matched against
// AST nodes. Patterns use placeholder identifiers (from Captures) that
// bind to matched subtrees.
type GoPattern struct {
	code     string
	captures map[string]*Capture
	imports  []string
	kind     ScaffoldKind

	once     sync.Once
	cached   java.J
	parseErr error
}

// Match attempts to match this pattern against the given candidate node.
// Returns a MatchResult containing captured bindings on success, or nil on failure.
func (p *GoPattern) Match(candidate java.J, cursor *visitor.Cursor) *MatchResult {
	patternTree, err := p.getTree()
	if err != nil || patternTree == nil {
		return nil
	}

	// Fast reject: if the pattern root (when not a placeholder) has a different
	// concrete type than the candidate, the match cannot succeed.
	if ident, ok := patternTree.(*java.Identifier); ok {
		if _, isPlaceholder := FromPlaceholder(ident.Name); isPlaceholder {
			// Pattern is a bare placeholder — it matches anything.
			result := NewMatchResult()
			result.bind(ident.Name[len(placeholderPrefix):len(ident.Name)-len(placeholderSuffix)], candidate)
			return result
		}
	}
	if reflect.TypeOf(patternTree) != reflect.TypeOf(candidate) {
		return nil
	}

	cmp := newPatternComparator(p.captures, cursor)
	return cmp.match(patternTree, candidate)
}

func (p *GoPattern) Matches(candidate java.J, cursor *visitor.Cursor) bool {
	return p.Match(candidate, cursor) != nil
}

// getTree lazily parses the pattern and caches the result.
func (p *GoPattern) getTree() (java.J, error) {
	p.once.Do(func() {
		p.cached, p.parseErr = parseScaffold(p.code, p.captures, p.imports, p.kind)
	})
	return p.cached, p.parseErr
}

type PatternBuilder struct {
	code     string
	captures []*Capture
	imports  []string
	kind     ScaffoldKind
}

func Expression(code string) *PatternBuilder {
	return &PatternBuilder{code: code, kind: ScaffoldExpression}
}

func StatementPattern(code string) *PatternBuilder {
	return &PatternBuilder{code: code, kind: ScaffoldStatement}
}

func TopLevel(code string) *PatternBuilder {
	return &PatternBuilder{code: code, kind: ScaffoldTopLevel}
}

func (b *PatternBuilder) Captures(caps ...*Capture) *PatternBuilder {
	b.captures = append(b.captures, caps...)
	return b
}

func (b *PatternBuilder) Imports(pkgs ...string) *PatternBuilder {
	b.imports = append(b.imports, pkgs...)
	return b
}

func (b *PatternBuilder) Build() *GoPattern {
	return &GoPattern{
		code:     b.code,
		captures: captureMap(b.captures),
		imports:  b.imports,
		kind:     b.kind,
	}
}
