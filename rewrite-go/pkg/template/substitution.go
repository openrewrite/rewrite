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
	"strings"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parenthesize"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// substitutionVisitor walks a template tree and replaces placeholder
// identifiers with the corresponding captured AST nodes from a MatchResult.
type substitutionVisitor struct {
	visitor.GoVisitor
	values *MatchResult
}

func newSubstitutionVisitor(values *MatchResult) *substitutionVisitor {
	v := &substitutionVisitor{values: values}
	v.Self = v
	return v
}

func (v *substitutionVisitor) VisitIdentifier(ident *java.Identifier, p any) java.J {
	name, ok := FromPlaceholder(ident.Name)
	if !ok {
		return v.GoVisitor.VisitIdentifier(ident, p)
	}

	val := v.values.Get(name)
	if val == nil {
		return v.GoVisitor.VisitIdentifier(ident, p)
	}
	// A bound subtree came from the source and stays there, so what is
	// spliced is a copy of it.
	val = withFreshIDs(val)

	// Preserve the placeholder's prefix whitespace on the substituted node.
	// Use setLeadingPrefix to handle compound nodes where the prefix
	// lives on the first child (e.g., MethodInvocation's Select).
	// The cursor here walks the template, so the position a capture lands in is
	// the placeholder's own.
	out := parenthesized(setLeadingPrefix(val, withCommentsOf(ident.Prefix, val.GetPrefix())), v.Cursor())
	// A slot that holds a name only, such as a selector's member or what a
	// declaration declares, keeps the placeholder, which fails the
	// substitution rather than the visit.
	if slot := slotOf(v.Cursor()); slot != nil && !reflect.TypeOf(out).AssignableTo(slot) {
		return ident
	}
	return out
}

// A placeholder written in statement position is an identifier the parser
// wraps in an expression statement. A statement bound to it takes the wrapper's
// place rather than land in it, an expression statement holding an expression.
func (v *substitutionVisitor) VisitExpressionStatement(es *golang.ExpressionStatement, p any) java.J {
	name, ok := placeholderName(es)
	if !ok {
		return v.GoVisitor.VisitExpressionStatement(es, p)
	}
	val := v.values.Get(name)
	if _, isStatement := val.(java.Statement); !isStatement {
		return v.GoVisitor.VisitExpressionStatement(es, p)
	}
	return setLeadingPrefix(withFreshIDs(val), withCommentsOf(es.Prefix, val.GetPrefix()))
}

// A list binding expands within the list its placeholder sits in, so the
// placeholder's own visit leaves it in place for the enclosing call or block to
// replace. Get holds only single bindings, and returns nil for a list.
func (v *substitutionVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	visited := v.GoVisitor.VisitMethodInvocation(v.withCalleeSubstituted(mi), p)
	call, ok := visited.(*java.MethodInvocation)
	if !ok {
		return visited
	}
	expanded, changed := expandList(v, call.Arguments.Elements, expressionSeparator)
	if !changed {
		return call
	}
	c := *call
	c.Arguments.Elements = expanded
	return &c
}

// withCalleeSubstituted splices what a placeholder callee is bound to into the
// call the way the parser shapes one: an identifier is the call's name, a
// selector splits into the select and the name, and anything else is the
// select of a call with an empty name. The name slot holds an identifier only,
// so the identifier visit cannot do this itself.
func (v *substitutionVisitor) withCalleeSubstituted(mi *java.MethodInvocation) *java.MethodInvocation {
	if mi.Select != nil || mi.Name == nil {
		return mi
	}
	name, ok := FromPlaceholder(mi.Name.Name)
	if !ok {
		return mi
	}
	val := v.values.Get(name)
	if val == nil {
		return mi
	}
	callee, ok := withFreshIDs(val).(java.Expression)
	if !ok {
		return mi
	}
	callee = setLeadingPrefix(callee, withCommentsOf(mi.Name.Prefix, callee.GetPrefix())).(java.Expression)
	c := *mi
	switch f := callee.(type) {
	case *java.Identifier:
		c.Name = f
	case *java.FieldAccess:
		c.Select = &java.RightPadded[java.Expression]{Element: f.Target, After: f.Name.Before}
		c.Name = f.Name.Element
	default:
		if needsDelimiting(callee) {
			callee = parenthesize.Wrap(callee)
		}
		c.Select = &java.RightPadded[java.Expression]{Element: callee}
		c.Name = &java.Identifier{ID: uuid.New()}
	}
	return &c
}

// needsDelimiting reports a callee Go's grammar does not admit unparenthesized
// before a call's argument list.
func needsDelimiting(e java.Expression) bool {
	switch e.(type) {
	case *java.Binary, *golang.Binary, *java.Unary, *golang.Unary:
		return true
	}
	return false
}

func (v *substitutionVisitor) VisitBlock(block *java.Block, p any) java.J {
	visited := v.GoVisitor.VisitBlock(block, p)
	b, ok := visited.(*java.Block)
	if !ok {
		return visited
	}
	expanded, changed := expandList(v, b.Statements, statementSeparator)
	if !changed {
		return b
	}
	return b.WithStatements(expanded)
}

// separator supplies the leading whitespace of the nth element a placeholder
// expands to, given the whitespace the placeholder itself carried.
type separator func(n int, placeholder java.Space) java.Space

// Arguments are separated by the comma the printer emits plus a space, which is
// what the parser leaves on every argument after the first.
func expressionSeparator(n int, placeholder java.Space) java.Space {
	if n == 0 {
		return placeholder
	}
	return java.SingleSpace
}

// Statements are separated by the line break and indent the placeholder already
// stands at, so every one of them starts where it did.
func statementSeparator(_ int, placeholder java.Space) java.Space {
	return placeholder
}

// expandList replaces each list-bound placeholder with the subtrees bound to
// it. The run takes over the placeholder's padding: the whitespace before it
// goes to the run's first element, the whitespace after it to the last. An
// empty run hands the whitespace before it to the element taking its place.
func expandList[T java.J](v *substitutionVisitor, elements []java.RightPadded[T], sep separator) ([]java.RightPadded[T], bool) {
	changed := false
	expanded := make([]java.RightPadded[T], 0, len(elements))
	var vacated *java.Space

	for _, rp := range elements {
		prefix := rp.Element.GetPrefix()
		if vacated != nil {
			prefix, vacated = *vacated, nil
		}

		var values []java.J
		isList := false
		if name, isPlaceholder := placeholderName(java.J(rp.Element)); isPlaceholder {
			values, isList = v.values.listBinding(name)
		}

		if !isList {
			if !java.SpaceEqual(prefix, rp.Element.GetPrefix()) {
				element, ok := setPrefix(rp.Element, prefix).(T)
				if !ok {
					return elements, false
				}
				rp.Element = element
			}
			expanded = append(expanded, rp)
			continue
		}

		changed = true
		if len(values) == 0 {
			vacated = &prefix
			continue
		}
		for i, value := range values {
			// Returning the list untouched leaves the placeholder
			// standing, which substitute reads as the failure it is.
			element, ok := setPrefix(withFreshIDs(value), withCommentsOf(sep(i, prefix), value.GetPrefix())).(T)
			if !ok {
				return elements, false
			}
			padded := java.RightPadded[T]{Element: element}
			if i == len(values)-1 {
				padded.After, padded.Markers = rp.After, rp.Markers
			}
			expanded = append(expanded, padded)
		}
	}
	return expanded, changed
}

// substitute applies the substitution visitor to the template tree, replacing
// all placeholder identifiers with captured values. It is nil unless every
// placeholder was replaced, a placeholder in the output being no valid Go.
func substitute(templateTree java.J, values *MatchResult) java.J {
	v := newSubstitutionVisitor(values)
	result := v.Visit(templateTree, nil)
	if result == nil {
		return nil
	}
	substituted, ok := result.(java.J)
	if !ok || holdsPlaceholder(substituted) {
		return nil
	}
	return substituted
}

// holdsPlaceholder reports a placeholder the substitution left standing,
// which is the failure its caller reads. A list binding leaves its
// placeholder for the enclosing list to expand, so whether one was resolved
// is known only once the whole tree has been rewritten.
func holdsPlaceholder(j java.J) bool {
	found := false
	visitor.Walk(j, func(t java.Tree) bool {
		ident, ok := t.(*java.Identifier)
		found = ok && IsPlaceholder(ident.Name)
		return !found
	})
	return found
}

// withCommentsOf is the whitespace a substituted subtree takes from where it
// lands, keeping the comments it led with where it came from, indented as
// what they precede now is. A comment that ends a line stays behind unless the
// subtree lands at the start of one, as the line break would end the statement
// before it.
func withCommentsOf(at, from java.Space) java.Space {
	comments := from.Comments()
	if len(comments) == 0 {
		return at
	}
	lead := trailingWhitespace(at)
	if !strings.Contains(lead, "\n") {
		for _, c := range comments {
			if !c.Multiline || strings.Contains(c.Suffix, "\n") {
				return at
			}
		}
	}
	indent := lead[strings.LastIndex(lead, "\n")+1:]
	merged := append([]java.Comment(nil), at.Comments()...)
	for _, c := range comments {
		if i := strings.LastIndex(c.Suffix, "\n"); i >= 0 {
			c.Suffix = c.Suffix[:i+1] + indent
		}
		merged = append(merged, c)
	}
	return java.MakeSpace(merged, at.Whitespace())
}

// trailingWhitespace is what a space ends with after its last comment.
func trailingWhitespace(s java.Space) string {
	if comments := s.Comments(); len(comments) > 0 {
		return comments[len(comments)-1].Suffix
	}
	return s.Whitespace()
}

func setPrefix(j java.J, prefix java.Space) java.J {
	switch n := j.(type) {
	case *java.Identifier:
		return n.WithPrefix(prefix)
	case *java.Literal:
		return n.WithPrefix(prefix)
	case *java.Binary:
		return n.WithPrefix(prefix)
	case *java.Unary:
		return n.WithPrefix(prefix)
	case *golang.Binary:
		return n.WithPrefix(prefix)
	case *golang.Unary:
		return n.WithPrefix(prefix)
	case *golang.AssignmentOperation:
		return n.WithPrefix(prefix)
	case *golang.PointerType:
		return n.WithPrefix(prefix)
	case *golang.Variadic:
		return n.WithPrefix(prefix)
	case *golang.StatementExpression:
		return n.WithPrefix(prefix)
	case *golang.ExpressionStatement:
		return n.WithPrefix(prefix)
	case *golang.DeclarationBlock:
		return n.WithPrefix(prefix)
	case *java.ParameterizedType:
		return n.WithPrefix(prefix)
	case *java.FieldAccess:
		return n.WithPrefix(prefix)
	case *java.MethodInvocation:
		return n.WithPrefix(prefix)
	case *java.Assignment:
		return n.WithPrefix(prefix)
	case *java.AssignmentOperation:
		return n.WithPrefix(prefix)
	case *java.Block:
		return n.WithPrefix(prefix)
	case *java.Return:
		return n.WithPrefix(prefix)
	case *golang.Return:
		return n.WithPrefix(prefix)
	case *java.If:
		return n.WithPrefix(prefix)
	case *golang.MethodDeclaration:
		return n.WithPrefix(prefix)
	case *golang.StatementWithInit:
		return n.WithPrefix(prefix)
	case *java.MethodDeclaration:
		return n.WithPrefix(prefix)
	case *java.VariableDeclarations:
		return n.WithPrefix(prefix)
	case *java.VariableDeclarator:
		return n.WithPrefix(prefix)
	case *java.Parentheses:
		return n.WithPrefix(prefix)
	case *java.ParenthesizedTypeTree:
		return n.WithPrefix(prefix)
	case *golang.TypeAssertion:
		return n.WithPrefix(prefix)
	case *java.TypeCast:
		return n.WithPrefix(prefix)
	case *java.ControlParentheses:
		return n.WithPrefix(prefix)
	case *java.ArrayAccess:
		return n.WithPrefix(prefix)
	case *java.ArrayType:
		return n.WithPrefix(prefix)
	case *golang.ArrayType:
		return n.WithPrefix(prefix)
	case *java.ForLoop:
		return n.WithPrefix(prefix)
	case *java.ForEachLoop:
		return n.WithPrefix(prefix)
	case *java.Switch:
		return n.WithPrefix(prefix)
	case *java.Case:
		return n.WithPrefix(prefix)
	case *java.Break:
		return n.WithPrefix(prefix)
	case *java.Continue:
		return n.WithPrefix(prefix)
	case *java.Label:
		return n.WithPrefix(prefix)
	case *java.Empty:
		return n.WithPrefix(prefix)
	case *java.Import:
		return n.WithPrefix(prefix)
	case *golang.GoStmt:
		return n.WithPrefix(prefix)
	case *golang.Defer:
		return n.WithPrefix(prefix)
	case *golang.Send:
		return n.WithPrefix(prefix)
	case *golang.Goto:
		return n.WithPrefix(prefix)
	case *golang.Fallthrough:
		return n.WithPrefix(prefix)
	case *golang.Composite:
		return n.WithPrefix(prefix)
	case *golang.KeyValue:
		return n.WithPrefix(prefix)
	case *golang.Slice:
		return n.WithPrefix(prefix)
	case *golang.MapType:
		return n.WithPrefix(prefix)
	case *golang.Channel:
		return n.WithPrefix(prefix)
	case *golang.FuncType:
		return n.WithPrefix(prefix)
	case *golang.StructType:
		return n.WithPrefix(prefix)
	case *golang.InterfaceType:
		return n.WithPrefix(prefix)
	case *golang.TypeList:
		return n.WithPrefix(prefix)
	case *golang.Union:
		return n.WithPrefix(prefix)
	case *golang.UnderlyingType:
		return n.WithPrefix(prefix)
	case *golang.TypeDecl:
		return n.WithPrefix(prefix)
	case *golang.MultiAssignment:
		return n.WithPrefix(prefix)
	case *golang.CommClause:
		return n.WithPrefix(prefix)
	case *golang.Select:
		return n.WithPrefix(prefix)
	case *golang.IndexList:
		return n.WithPrefix(prefix)
	case *golang.CompilationUnit:
		return n.WithPrefix(prefix)
	default:
		return j
	}
}
