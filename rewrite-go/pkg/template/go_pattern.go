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
	"fmt"
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
	declared map[string]java.JavaType
	// rootIsPlaceholder marks a pattern that is a placeholder alone: it has
	// no kind to reject a candidate on.
	rootIsPlaceholder bool
	parseErr          error
}

// Match attempts to match this pattern against the given candidate node.
// Returns a MatchResult containing captured bindings on success, or nil on failure.
func (p *GoPattern) Match(candidate java.J, cursor *visitor.Cursor) *MatchResult {
	patternTree, err := p.getTree()
	if err != nil || patternTree == nil {
		return nil
	}

	// The same rule matchNode applies to every node, narrowed by the pattern
	// root already being unwrapped, and worth its own place: rejecting here
	// costs no comparator. A placeholder root reaches the comparator instead,
	// to be bound and held to the type it declared.
	if !p.rootIsPlaceholder &&
		reflect.TypeOf(patternTree) != reflect.TypeOf(candidate) &&
		reflect.TypeOf(patternTree) != reflect.TypeOf(unparenthesize(candidate)) {
		return nil
	}
	return p.comparator(cursor).match(patternTree, candidate)
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
			_, p.rootIsPlaceholder = placeholderName(p.cached)
			p.declared, p.parseErr = declaredTypes(p.cached, p.captures)
		}
	})
	return p.cached, p.parseErr
}

// comparator reads the types getTree resolved, so a match runs through it only
// after the parse.
func (p *GoPattern) comparator(cursor *visitor.Cursor) *patternComparator {
	return &patternComparator{
		captures: p.captures,
		declared: p.declared,
		cursor:   cursor,
		mode:     p.mode,
		variadic: p.variadic,
	}
}

// declaredTypes resolves each typed capture against the placeholder the
// scaffold preamble declared for it. A name that did not resolve would
// constrain nothing, which reads as a pattern matching everything, so it fails
// the parse.
func declaredTypes(tree java.J, captures map[string]*Capture) (map[string]java.JavaType, error) {
	var declared map[string]java.JavaType
	var err error
	visitor.Walk(tree, func(t java.Tree) bool {
		ident, ok := t.(*java.Identifier)
		if !ok {
			return true
		}
		name, isPlaceholder := FromPlaceholder(ident.Name)
		if !isPlaceholder {
			return true
		}
		capture, ok := captures[name]
		if !ok || capture.TypeName() == "" {
			return true
		}
		if ident.Type == nil || java.IsUnknown(ident.Type) {
			err = fmt.Errorf("capture %q declares type %q, which the pattern could not resolve; "+
				"name it through Imports, Context or ExportData", name, capture.TypeName())
			return false
		}
		if declared == nil {
			declared = make(map[string]java.JavaType)
		}
		declared[name] = ident.Type
		return true
	})
	return declared, err
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
