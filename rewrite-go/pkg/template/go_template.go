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
	"sync"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoTemplate represents a parsed code template that can be applied to
// produce new AST nodes with captured values substituted in.
type GoTemplate struct {
	code     string
	captures map[string]*Capture
	imports  []string
	kind     ScaffoldKind

	once     sync.Once
	cached   java.J
	parseErr error
}

// Apply produces a new AST node by parsing the template and substituting
// captured values from the MatchResult.
func (t *GoTemplate) Apply(cursor *visitor.Cursor, values *MatchResult) java.J {
	templateTree, err := t.getTree()
	if err != nil || templateTree == nil {
		return nil
	}

	// Deep-copy the template tree by re-parsing (safe because parseScaffold is cached
	// and we need a fresh tree to substitute into).
	// For now, re-parse each time. Optimization: clone the cached tree.
	fresh, err := parseScaffold(t.code, t.captures, t.imports, t.kind)
	if err != nil {
		return nil
	}

	if values != nil {
		fresh = substitute(fresh, values)
	}

	return fresh
}

// getTree lazily parses the template and caches the result.
func (t *GoTemplate) getTree() (java.J, error) {
	t.once.Do(func() {
		t.cached, t.parseErr = parseScaffold(t.code, t.captures, t.imports, t.kind)
	})
	return t.cached, t.parseErr
}

// --- Template builder ---

// TemplateBuilder constructs a GoTemplate with fluent configuration.
type TemplateBuilder struct {
	code     string
	captures []*Capture
	imports  []string
	kind     ScaffoldKind
}

// ExpressionTemplate creates a TemplateBuilder for producing an expression.
func ExpressionTemplate(code string) *TemplateBuilder {
	return &TemplateBuilder{code: code, kind: ScaffoldExpression}
}

// StatementTemplate creates a TemplateBuilder for producing a statement.
func StatementTemplate(code string) *TemplateBuilder {
	return &TemplateBuilder{code: code, kind: ScaffoldStatement}
}

// TopLevelTemplate creates a TemplateBuilder for producing a top-level declaration.
func TopLevelTemplate(code string) *TemplateBuilder {
	return &TemplateBuilder{code: code, kind: ScaffoldTopLevel}
}

// Captures adds captures to the builder.
func (b *TemplateBuilder) Captures(caps ...*Capture) *TemplateBuilder {
	b.captures = append(b.captures, caps...)
	return b
}

// Imports adds imports to the synthetic source used to parse the template.
// It does not edit imports in the source file being rewritten.
func (b *TemplateBuilder) Imports(pkgs ...string) *TemplateBuilder {
	b.imports = append(b.imports, pkgs...)
	return b
}

// Build creates the GoTemplate.
func (b *TemplateBuilder) Build() *GoTemplate {
	return &GoTemplate{
		code:     b.code,
		captures: captureMap(b.captures),
		imports:  b.imports,
		kind:     b.kind,
	}
}

// Rewrite creates a visitor that matches the "before" pattern and replaces
// with the "after" template. This is a convenience for simple 1:1 rewrites.
func Rewrite(before *GoPattern, after *GoTemplate) *RewriteVisitor {
	v := &RewriteVisitor{before: before, after: after}
	v.Self = v
	return v
}

// RewriteVisitor applies a pattern match + template replacement on every node.
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

	replaced := v.after.Apply(nil, match)
	if replaced == nil {
		return result
	}

	// Preserve the original node's leading prefix on the replacement.
	return setLeadingPrefix(replaced, getLeadingPrefix(j))
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
