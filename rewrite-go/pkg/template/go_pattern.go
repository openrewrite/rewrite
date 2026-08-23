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
	"io/fs"
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
	context  []string
	kind     ScaffoldKind
	mode     TypeMatchingMode
	variadic bool
	importerCache

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
	// The same rule matchNode applies to every node, narrowed by the pattern
	// root already being unwrapped, and worth its own place: rejecting here
	// costs no comparator.
	if reflect.TypeOf(patternTree) != reflect.TypeOf(candidate) &&
		reflect.TypeOf(patternTree) != reflect.TypeOf(unparenthesize(candidate)) {
		return nil
	}

	cmp := newPatternComparator(p.captures, cursor, p.mode, p.variadic)
	return cmp.match(patternTree, candidate)
}

func (p *GoPattern) Matches(candidate java.J, cursor *visitor.Cursor) bool {
	return p.Match(candidate, cursor) != nil
}

// getTree lazily parses the pattern and caches the result.
func (p *GoPattern) getTree() (java.J, error) {
	p.once.Do(func() {
		p.cached, p.parseErr = parseScaffold(p.code, p.captures, p.imports, p.context, p.kind, p.shared())
		if p.cached != nil {
			// A pattern is never printed, so it keeps only what it matches by.
			p.cached = unparenthesize(p.cached)
		}
	})
	return p.cached, p.parseErr
}

type PatternBuilder struct {
	code       string
	captures   []*Capture
	imports    []string
	context    []string
	kind       ScaffoldKind
	mode       TypeMatchingMode
	exportData []fs.FS
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

// ExportData attributes the pattern against export data the recipe module
// carries. See TemplateBuilder.ExportData.
func (b *PatternBuilder) ExportData(sets ...fs.FS) *PatternBuilder {
	b.exportData = append(b.exportData, sets...)
	return b
}

// Context adds declarations the pattern is parsed against, so it can be
// attributed against types and functions no package exports. Imports and
// ExportData cover everything importable. Mirrors PatternOptions.context in
// the JavaScript matcher.
func (b *PatternBuilder) Context(decls ...string) *PatternBuilder {
	b.context = append(b.context, decls...)
	return b
}

// TypeMatching says whether the match reads the attribution the pattern and
// the candidate carry. Structural comparison is the default.
func (b *PatternBuilder) TypeMatching(mode TypeMatchingMode) *PatternBuilder {
	b.mode = mode
	return b
}

func (b *PatternBuilder) Build() *GoPattern {
	return &GoPattern{
		code:          b.code,
		captures:      captureMap(b.captures),
		imports:       b.imports,
		context:       b.context,
		kind:          b.kind,
		mode:          b.mode,
		variadic:      anyVariadic(captureMap(b.captures)),
		importerCache: importerCache{exportData: b.exportData},
	}
}
