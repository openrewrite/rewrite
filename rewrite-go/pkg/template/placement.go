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
	"reflect"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parenthesize"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// placeAt fits result into the site it replaces. site is the replaced node.
// Held strictly to it, the result is nil where Go's grammar admits no such node
// there, which leaves the site as it was rather than print what does not parse.
// A caller placing the result itself may name a site only near the one it
// fills, and is not held to it.
func placeAt(result java.J, site *visitor.Cursor, strict bool) java.J {
	replaced, ok := site.Value().(java.J)
	if !ok {
		return result
	}
	slot := slotOf(site)
	var out java.J
	_, isStatement := result.(java.Statement)
	if expr, ok := result.(java.Expression); ok && !isStatement && slot == statementType {
		out = &golang.ExpressionStatement{
			ID:         uuid.New(),
			Prefix:     withCommentsOf(getLeadingPrefix(replaced), getLeadingPrefix(expr)),
			Expression: setLeadingPrefix(expr, java.EmptySpace).(java.Expression),
		}
	} else {
		out = parenthesized(setLeadingPrefix(result, withCommentsOf(getLeadingPrefix(replaced), getLeadingPrefix(result))), site)
	}
	if strict && slot != nil && !reflect.TypeOf(out).AssignableTo(slot) {
		return nil
	}
	out = asElseBody(out, site)
	if strict && !admitted(out, site) {
		return nil
	}
	out = regroupedWithin(out, site.Parent())
	if formatted, ok := format.AutoFormat(out, nil, nil, site.Parent()).(java.J); ok {
		return formatted
	}
	return out
}

// parenthesized applies parenthesize.Maybe to anything that is an expression.
// A template result can be a statement or a declaration, which never regroups.
func parenthesized(j java.J, site *visitor.Cursor) java.J {
	expr, ok := j.(java.Expression)
	if !ok {
		return j
	}
	return parenthesize.Maybe(expr, site)
}

// regroupedWithin parenthesizes what the substitution spliced into result where
// the site, rather than the template, makes it regroup: a composite literal
// that reaches a control clause's header, for one.
func regroupedWithin(result java.J, parent *visitor.Cursor) java.J {
	v := parenthesize.NewVisitor()
	v.SetCursor(parent)
	if regrouped, ok := v.Visit(result, nil).(java.J); ok && regrouped != nil {
		return regrouped
	}
	return result
}

var statementType = reflect.TypeOf((*java.Statement)(nil)).Elem()

// slotOf returns the type the parent declares the field holding the node at
// site with, which is what a replacement has to be, or nil where it is unknown.
func slotOf(site *visitor.Cursor) reflect.Type {
	parent := site.Parent()
	if parent == nil {
		return nil
	}
	node, holder := reflect.ValueOf(site.Value()), reflect.ValueOf(parent.Value())
	if node.Kind() != reflect.Pointer || holder.Kind() != reflect.Pointer || holder.Elem().Kind() != reflect.Struct {
		return nil
	}
	return heldAs(holder.Elem(), node.Pointer())
}

// inStatementSlot reports whether the parent declares the field holding the
// node at site as a java.Statement, where an expression alone cannot go.
func inStatementSlot(site *visitor.Cursor) bool {
	return slotOf(site) == statementType
}

// heldAs looks through the padding and lists of v, but not into other nodes,
// for a field pointing at node, and returns the type it is declared with.
func heldAs(v reflect.Value, node uintptr) reflect.Type {
	if t := v.Type(); t == spaceType || t == markersType || t.Implements(javaTypeType) {
		return nil
	}
	switch v.Kind() {
	case reflect.Interface:
		e := v.Elem()
		if e.Kind() == reflect.Pointer && e.Pointer() == node {
			return v.Type()
		}
	case reflect.Pointer:
		if v.IsNil() {
			return nil
		}
		if v.Type().Implements(jType) {
			if v.Pointer() == node {
				return v.Type()
			}
			return nil
		}
		return heldAs(v.Elem(), node)
	case reflect.Struct:
		for i := range v.NumField() {
			if t := heldAs(v.Field(i), node); t != nil {
				return t
			}
		}
	case reflect.Slice:
		for i := range v.Len() {
			if t := heldAs(v.Index(i), node); t != nil {
				return t
			}
		}
	}
	return nil
}

// asElseBody puts a statement replacing an else clause's body in a block, the
// clause admitting only a block or another if.
func asElseBody(out java.J, site *visitor.Cursor) java.J {
	parent := site.Parent()
	if parent == nil {
		return out
	}
	if _, isElse := parent.Value().(*java.Else); !isElse {
		return out
	}
	stmt, ok := out.(java.Statement)
	if !ok {
		return out
	}
	switch stmt.(type) {
	case *java.If, *java.Block:
		return out
	}
	body, ok := setLeadingPrefix(stmt, java.MakeSpace(nil, "\n")).(java.Statement)
	if !ok {
		return out
	}
	return &java.Block{
		ID:         uuid.New(),
		Prefix:     getLeadingPrefix(stmt),
		Statements: []java.RightPadded[java.Statement]{{Element: body}},
		End:        java.MakeSpace(nil, "\n"),
	}
}

// admitted reports whether Go's grammar admits out where site is, past the
// type the field declares: a header clause takes a simple statement only,
// defer and go a call, and a short variable declaration declares names.
func admitted(out java.J, site *visitor.Cursor) bool {
	parent := site.Parent()
	if parent == nil {
		return true
	}
	node := site.Value()
	if spread, ok := out.(*golang.Variadic); ok && spread.Postfix {
		call, ok := parent.Value().(*java.MethodInvocation)
		if !ok {
			return false
		}
		args := call.Arguments.Elements
		return len(args) > 0 && any(args[len(args)-1].Element) == node
	}
	switch p := parent.Value().(type) {
	case *golang.Composite:
		// A composite literal's type leads it, so an operator there applies
		// to the literal instead: `*S{}` dereferences `S{}`.
		if any(p.TypeExpr) == node {
			switch out.(type) {
			case *golang.PointerType, *golang.Unary, *java.Unary, *golang.FuncType, *golang.Channel:
				return false
			}
		}
	case *golang.CommClause:
		if any(p.Comm) == node {
			return isCommunication(out)
		}
	case *golang.ExpressionStatement:
		if grandparent := parent.Parent(); grandparent != nil {
			if clause, ok := grandparent.Value().(*golang.CommClause); ok && any(clause.Comm) == any(p) {
				return isReceive(out)
			}
		}
	case *golang.StatementWithInit:
		if any(p.Init.Element) == node {
			return isSimpleStatement(out)
		}
		switch out.(type) {
		case *java.If, *java.Switch:
			return true
		}
		return false
	case *java.ForControl:
		if (p.Init != nil && any(p.Init.Element) == node) || (p.Update != nil && any(p.Update.Element) == node) {
			return isSimpleStatement(out)
		}
	case *golang.Defer, *golang.GoStmt:
		switch out.(type) {
		case *java.MethodInvocation, *java.TypeCast:
			return true
		}
		return false
	case *java.If:
		if any(p.ThenPart.Element) == node {
			_, ok := out.(*java.Block)
			return ok
		}
	case *java.ForLoop, *java.ForEachLoop:
		if _, isBody := node.(*java.Block); isBody {
			_, ok := out.(*java.Block)
			return ok
		}
	case *java.Assignment:
		if any(p.Variable) == node && isShortVarDecl(p.Markers) {
			_, ok := out.(*java.Identifier)
			return ok
		}
	case *golang.MultiAssignment:
		if isShortVarDecl(p.Markers) {
			for _, v := range p.Variables {
				if any(v.Element) == node {
					_, ok := out.(*java.Identifier)
					return ok
				}
			}
		}
	}
	return true
}

// isSimpleStatement reports a statement Go's grammar admits in a header
// clause: an expression, a send, an increment or decrement, an assignment or a
// short variable declaration.
func isSimpleStatement(j java.J) bool {
	switch s := j.(type) {
	case *golang.ExpressionStatement, *java.MethodInvocation, *golang.Send, *java.Empty,
		*java.Assignment, *java.AssignmentOperation, *golang.AssignmentOperation, *golang.MultiAssignment:
		return true
	case *java.Unary:
		return s.Operator.Element == java.PostIncrement || s.Operator.Element == java.PostDecrement
	}
	_, isExpression := j.(java.Expression)
	return isExpression
}

// isCommunication reports a statement a select case admits: a send, or a
// receive whose value is dropped, assigned or declared.
func isCommunication(j java.J) bool {
	switch s := j.(type) {
	case *golang.Send:
		return true
	case *golang.ExpressionStatement:
		return isReceive(s.Expression)
	case *golang.Unary:
		return isReceive(s)
	case *java.Assignment:
		return isReceive(s.Value.Element)
	case *golang.MultiAssignment:
		return len(s.Values) == 1 && isReceive(s.Values[0].Element)
	}
	return false
}

func isReceive(j java.J) bool {
	u, ok := unparenthesize(j).(*golang.Unary)
	return ok && u.Operator.Element == golang.Receive
}

// wellFormed reports whether Go's grammar admits every node of tree where it
// sits in tree, which a substitution can break as a placement can: by binding
// a spread to a capture that lands outside a call, or a selector to one that
// lands left of :=.
func wellFormed(tree java.J) bool {
	g := visitor.Init(&grammarCheck{ok: true})
	g.Visit(tree, nil)
	return g.ok
}

type grammarCheck struct {
	visitor.GoVisitor
	ok bool
}

func (g *grammarCheck) Visit(t java.Tree, p any) java.Tree {
	if !g.ok {
		return t
	}
	if j, isJ := t.(java.J); isJ && g.Cursor() != nil && !admitted(j, visitor.NewCursor(g.Cursor(), t)) {
		g.ok = false
		return t
	}
	return g.GoVisitor.Visit(t, p)
}

func isShortVarDecl(markers java.Markers) bool {
	return java.FindMarker[golang.ShortVarDecl](markers) != nil
}
