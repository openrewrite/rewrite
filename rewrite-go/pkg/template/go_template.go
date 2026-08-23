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
	"sync"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoTemplate represents a parsed code template that can be applied to
// produce new AST nodes with captured values substituted in.
type GoTemplate struct {
	code     string
	captures map[string]*Capture
	imports  []string
	context  []string
	kind     ScaffoldKind
	importerCache

	once     sync.Once
	cached   java.J
	parseErr error
}

// Apply produces a new AST node by parsing the template and substituting
// captured values from the MatchResult. cursor is the node the result replaces,
// and is what lets Apply place the result there: leading whitespace,
// parentheses where the expression around it binds tighter, indentation. Pass
// nil for the substituted tree alone, carrying the template's own whitespace.
func (t *GoTemplate) Apply(cursor *visitor.Cursor, values *MatchResult) java.J {
	templateTree, err := t.getTree()
	if err != nil || templateTree == nil {
		return nil
	}

	// A placeholder left unsubstituted prints as the identifier standing in
	// for it, which would reach the source file. A capture the template never
	// names has no placeholder to leave, so it need not be bound: a recipe
	// declares its captures once and shares them across its alternatives.
	for name := range placeholdersIn(templateTree) {
		capture, declared := t.captures[name]
		if !declared {
			continue
		}
		if values == nil || !values.satisfies(capture) {
			return nil
		}
	}

	// Giving every node a new ID copies the whole tree, so the result shares
	// no node with the cached one and two applications share none with each
	// other. The cache is what the scaffold parse produced, once.
	fresh := withFreshIDs(templateTree)

	if values != nil {
		fresh = substitute(fresh, values)
	}
	if cursor == nil {
		return fresh
	}
	return placeAt(fresh, cursor)
}

// Instantiate produces the template as a detached node for a recipe that
// inserts the result somewhere new, where Apply replaces a matched node. Bound
// values are copied, so a spliced subtree may also stay where it came from.
// The result carries no leading whitespace. Like Apply it is nil unless every
// capture is bound, through Bind for a single subtree or BindList for a run of
// them within the capture's declared bounds.
func (t *GoTemplate) Instantiate(values *MatchResult) java.J {
	instantiated := t.Apply(nil, values)
	if instantiated == nil {
		return nil
	}
	// The prefix is the gap the scaffold leaves before the template code,
	// not whitespace the template itself declares.
	return setLeadingPrefix(instantiated, java.EmptySpace)
}

// withFreshIDs returns a copy of j in which every node has a new ID, keeping
// IDs unique within whatever tree the result is inserted into.
func withFreshIDs(j java.J) java.J {
	v := &idRefreshVisitor{}
	v.Self = v
	return v.Visit(j, nil).(java.J)
}

type idRefreshVisitor struct {
	visitor.GoVisitor
}

func (v *idRefreshVisitor) Visit(t java.Tree, p any) java.Tree {
	visited := v.GoVisitor.Visit(t, p)
	if j, ok := visited.(java.J); ok && j != nil {
		return j.WithID(uuid.New())
	}
	return visited
}

// getTree lazily parses the template and caches the result.
func (t *GoTemplate) getTree() (java.J, error) {
	t.once.Do(func() {
		t.cached, t.parseErr = parseScaffold(t.code, t.captures, t.imports, t.context, t.kind, t.shared())
	})
	return t.cached, t.parseErr
}

type TemplateBuilder struct {
	code       string
	captures   []*Capture
	imports    []string
	context    []string
	kind       ScaffoldKind
	exportData []fs.FS
}

func ExpressionTemplate(code string) *TemplateBuilder {
	return &TemplateBuilder{code: code, kind: ScaffoldExpression}
}

func StatementTemplate(code string) *TemplateBuilder {
	return &TemplateBuilder{code: code, kind: ScaffoldStatement}
}

func TopLevelTemplate(code string) *TemplateBuilder {
	return &TemplateBuilder{code: code, kind: ScaffoldTopLevel}
}

func (b *TemplateBuilder) Captures(caps ...*Capture) *TemplateBuilder {
	b.captures = append(b.captures, caps...)
	return b
}

// It does not edit imports in the source file being rewritten.
func (b *TemplateBuilder) Imports(pkgs ...string) *TemplateBuilder {
	b.imports = append(b.imports, pkgs...)
	return b
}

// Context adds declarations the template is parsed against. See
// PatternBuilder.Context.
func (b *TemplateBuilder) Context(decls ...string) *TemplateBuilder {
	b.context = append(b.context, decls...)
	return b
}

// ExportData attributes the template against compiler export data the recipe
// module carries, reaching packages the running toolchain cannot load. Sets
// accumulate, as Imports does, so a module can draw on several generated
// packages; see exportdata.Importer for what none of them covers.
func (b *TemplateBuilder) ExportData(sets ...fs.FS) *TemplateBuilder {
	b.exportData = append(b.exportData, sets...)
	return b
}

func (b *TemplateBuilder) Build() *GoTemplate {
	return &GoTemplate{
		code:          b.code,
		captures:      captureMap(b.captures),
		imports:       b.imports,
		context:       b.context,
		kind:          b.kind,
		importerCache: importerCache{exportData: b.exportData},
	}
}

// Rewrite creates a visitor that matches the "before" pattern and replaces
// with the "after" template. This is a convenience for simple 1:1 rewrites.
func Rewrite(before *GoPattern, after *GoTemplate) *RewriteVisitor {
	v := &RewriteVisitor{before: before, after: after}
	v.Self = v
	return v
}

type RewriteVisitor struct {
	visitor.GoVisitor
	before *GoPattern
	after  *GoTemplate
}

// Visit overrides the default Visit to attempt pattern matching on every node.
func (v *RewriteVisitor) Visit(t java.Tree, p any) java.Tree {
	result := v.GoVisitor.Visit(t, p)
	if result == nil {
		return nil
	}

	j, ok := result.(java.J)
	if !ok {
		return result
	}

	match := v.before.Match(j, nil)
	if match == nil {
		return result
	}

	// The cursor names t, not the visited j: rewriting a descendant leaves j a
	// new node, while the parent Apply consults still holds t.
	replaced := v.after.Apply(visitor.NewCursor(v.Cursor(), t), match)
	if replaced == nil {
		return result
	}
	return replaced
}

// setLeadingPrefix sets the node's own leading whitespace. The parser
// attaches inter-element whitespace to the outermost element, so the
// leading prefix lives directly on the node.
func setLeadingPrefix(j java.J, prefix java.Space) java.J {
	return setPrefix(j, prefix)
}

// getLeadingPrefix returns the node's own leading whitespace. The parser
// attaches inter-element whitespace to the outermost element, so the
// leading prefix lives directly on the node.
func getLeadingPrefix(j java.J) java.Space {
	return j.GetPrefix()
}

// placeholdersIn names the captures a template tree actually stands in for.
func placeholdersIn(tree java.J) map[string]bool {
	v := &placeholderCollector{names: map[string]bool{}}
	v.Self = v
	v.Visit(tree, nil)
	return v.names
}

type placeholderCollector struct {
	visitor.GoVisitor
	names map[string]bool
}

func (p *placeholderCollector) PreVisit(t java.Tree, _ any) java.Tree {
	if ident, ok := t.(*java.Identifier); ok {
		if name, isPlaceholder := FromPlaceholder(ident.Name); isPlaceholder {
			p.names[name] = true
		}
	}
	return t
}
