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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
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

// Imports adds required imports for the template code.
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

// setLeadingPrefix sets the prefix on the leftmost leaf of the node,
// mirroring getLeadingPrefix. This ensures compound nodes like
// MethodInvocation (where the prefix lives on Select, not the root)
// get the right whitespace.
func setLeadingPrefix(j java.J, prefix java.Space) java.J {
	switch n := j.(type) {
	case *java.Identifier:
		return n.WithPrefix(prefix)
	case *java.Literal:
		return n.WithPrefix(prefix)
	case *java.Empty:
		return n.WithPrefix(prefix)
	case *java.Binary:
		return n.WithLeft(setLeadingPrefix(n.Left, prefix).(java.Expression))
	case *java.Unary:
		return n.WithPrefix(prefix)
	case *java.FieldAccess:
		return n.WithTarget(setLeadingPrefix(n.Target, prefix).(java.Expression))
	case *java.MethodInvocation:
		if n.Select != nil {
			sel := *n.Select
			sel.Element = setLeadingPrefix(sel.Element, prefix).(java.Expression)
			return &java.MethodInvocation{
				ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
				Select: &sel, Name: n.Name, Arguments: n.Arguments, MethodType: n.MethodType,
			}
		}
		return n.WithName(n.Name.WithPrefix(prefix))
	case *java.Assignment:
		return n.WithVariable(setLeadingPrefix(n.Variable, prefix).(java.Expression))
	case *java.AssignmentOperation:
		return n.WithVariable(setLeadingPrefix(n.Variable, prefix).(java.Expression))
	case *java.Parentheses:
		return n.WithPrefix(prefix)
	case *java.TypeCast:
		return n.WithPrefix(prefix)
	case *java.ArrayAccess:
		return &java.ArrayAccess{
			ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
			Indexed:   setLeadingPrefix(n.Indexed, prefix).(java.Expression),
			Dimension: n.Dimension, Type: n.Type,
		}
	case *golang.Composite:
		if n.TypeExpr != nil {
			return &golang.Composite{
				ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
				TypeExpr: setLeadingPrefix(n.TypeExpr, prefix).(java.Expression),
				Elements: n.Elements,
			}
		}
		return n.WithPrefix(prefix)
	case *golang.Slice:
		return &golang.Slice{
			ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
			Indexed:     setLeadingPrefix(n.Indexed, prefix).(java.Expression),
			OpenBracket: n.OpenBracket, Low: n.Low, High: n.High, Max: n.Max, CloseBracket: n.CloseBracket,
		}
	case *golang.IndexList:
		return &golang.IndexList{
			ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
			Target:  setLeadingPrefix(n.Target, prefix).(java.Expression),
			Indices: n.Indices,
		}
	case *golang.Send:
		return &golang.Send{
			ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
			Channel: setLeadingPrefix(n.Channel, prefix).(java.Expression),
			Arrow:   n.Arrow,
		}
	case *golang.KeyValue:
		return &golang.KeyValue{
			ID: n.ID, Prefix: n.Prefix, Markers: n.Markers,
			Key:   setLeadingPrefix(n.Key, prefix).(java.Expression),
			Value: n.Value,
		}
	default:
		return setPrefix(j, prefix)
	}
}

// getLeadingPrefix extracts the effective leading whitespace from a node
// by walking to its leftmost leaf token. This is needed because in the
// Go LST, compound nodes (Binary, Unary, etc.) may have empty prefixes
// with the actual leading whitespace on the first child.
func getLeadingPrefix(j java.J) java.Space {
	switch n := j.(type) {
	case *java.Identifier:
		return n.Prefix
	case *java.Literal:
		return n.Prefix
	case *java.Empty:
		return n.Prefix
	case *java.Binary:
		// The leading prefix is on the left operand.
		return getLeadingPrefix(n.Left)
	case *java.Unary:
		return n.Prefix
	case *java.FieldAccess:
		return getLeadingPrefix(n.Target)
	case *java.MethodInvocation:
		if n.Select != nil {
			return getLeadingPrefix(n.Select.Element)
		}
		return getLeadingPrefix(n.Name)
	case *java.Assignment:
		return getLeadingPrefix(n.Variable)
	case *java.AssignmentOperation:
		return getLeadingPrefix(n.Variable)
	case *java.Block:
		return n.Prefix
	case *java.Return:
		return n.Prefix
	case *java.If:
		return n.Prefix
	case *java.MethodDeclaration:
		return n.Prefix
	case *java.VariableDeclarations:
		return n.Prefix
	case *java.Parentheses:
		return n.Prefix
	case *java.TypeCast:
		return getLeadingPrefix(n.Expr)
	case *java.ControlParentheses:
		return n.Prefix
	case *java.ArrayAccess:
		return getLeadingPrefix(n.Indexed)
	case *java.ArrayType:
		return n.Prefix
	case *java.ForLoop:
		return n.Prefix
	case *golang.RangeLoop:
		return n.Prefix
	case *java.Switch:
		return n.Prefix
	case *java.Case:
		return n.Prefix
	case *java.Break:
		return n.Prefix
	case *java.Continue:
		return n.Prefix
	case *java.Label:
		return n.Prefix
	case *golang.GoStmt:
		return n.Prefix
	case *golang.Defer:
		return n.Prefix
	case *golang.Send:
		return getLeadingPrefix(n.Channel)
	case *golang.Goto:
		return n.Prefix
	case *golang.Fallthrough:
		return n.Prefix
	case *golang.Composite:
		if n.TypeExpr != nil {
			return getLeadingPrefix(n.TypeExpr)
		}
		return n.Prefix
	case *golang.KeyValue:
		return getLeadingPrefix(n.Key)
	case *golang.Slice:
		return getLeadingPrefix(n.Indexed)
	case *golang.MapType:
		return n.Prefix
	case *golang.Channel:
		return n.Prefix
	case *golang.FuncType:
		return n.Prefix
	case *golang.StructType:
		return n.Prefix
	case *golang.InterfaceType:
		return n.Prefix
	case *golang.TypeList:
		return n.Prefix
	case *golang.Union:
		if len(n.Types) > 0 {
			return getLeadingPrefix(n.Types[0].Element)
		}
		return n.Prefix
	case *golang.UnderlyingType:
		return n.Prefix
	case *golang.TypeDecl:
		return n.Prefix
	case *golang.MultiAssignment:
		if len(n.Variables) > 0 {
			return getLeadingPrefix(n.Variables[0].Element)
		}
		return n.Prefix
	case *golang.CommClause:
		return n.Prefix
	case *golang.IndexList:
		return getLeadingPrefix(n.Target)
	case *java.Import:
		return n.Prefix
	case *golang.CompilationUnit:
		return n.Prefix
	default:
		return java.Space{}
	}
}
