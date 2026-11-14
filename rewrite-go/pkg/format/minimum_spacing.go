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

package format

import (
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// MinimumViableSpacingVisitor supplies the whitespace a tree needs to print as
// the Go it represents. A node a recipe splices in carries no prefix, which
// would run a keyword into the identifier after it and put two statements on one
// line; this pass gives each the least separation that keeps the token stream
// intact, and leaves layout to the passes after it.
//
// Mirrors org.openrewrite.java.format.MinimumViableSpacingVisitor.
type MinimumViableSpacingVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
}

// NewMinimumViableSpacingVisitor returns a visitor configured with the given
// stopAfter bound. Pass nil to format the entire visited tree.
func NewMinimumViableSpacingVisitor(stopAfter java.Tree) *MinimumViableSpacingVisitor {
	return visitor.Init(&MinimumViableSpacingVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *MinimumViableSpacingVisitor) Visit(t java.Tree, p any) java.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

func (v *MinimumViableSpacingVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	out := cu
	if pkg := cu.PackageDecl; pkg != nil {
		if elem := separateFrom("package", pkg.Element); elem != pkg.Element {
			decl := *pkg
			decl.Element = elem
			out = out.WithPackageDecl(&decl)
		}
	}
	// Everything after the package clause is a declaration of its own, so the
	// first one needs the same line break as the rest.
	if cu.Imports != nil && cu.Imports.Before.IsEmpty() {
		imports := *cu.Imports
		imports.Before = java.MakeSpace(nil, "\n")
		out = out.WithImports(&imports)
	}
	out = out.WithStatements(separateStatements(cu.Statements, true))
	return v.GoVisitor.VisitCompilationUnit(out, p)
}

func (v *MinimumViableSpacingVisitor) VisitBlock(block *java.Block, p any) java.J {
	out := v.GoVisitor.VisitBlock(block, p).(*java.Block)
	// The first statement follows `{`, which separates it already.
	return out.WithStatements(separateStatements(out.Statements, false))
}

func (v *MinimumViableSpacingVisitor) VisitCase(c *java.Case, p any) java.J {
	out := v.GoVisitor.VisitCase(c, p).(*java.Case)
	expressions := out.Expressions.Elements
	if len(expressions) > 0 && !isDefaultClause(out) {
		expressions = separateFirst("case", expressions)
	}
	body := separateStatements(out.Body.Elements, false)
	if java.SameSlice(expressions, out.Expressions.Elements) && java.SameSlice(body, out.Body.Elements) {
		return out
	}
	copied := *out
	copied.Expressions.Elements = expressions
	copied.Body.Elements = body
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitCommClause(cc *golang.CommClause, p any) java.J {
	out := v.GoVisitor.VisitCommClause(cc, p).(*golang.CommClause)
	comm := out.Comm
	if comm != nil {
		comm = separateFrom("case", comm)
	}
	body := separateStatements(out.Body, false)
	if comm == out.Comm && java.SameSlice(body, out.Body) {
		return out
	}
	copied := *out
	copied.Comm = comm
	copied.Body = body
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitReturn(ret *java.Return, p any) java.J {
	out := v.GoVisitor.VisitReturn(ret, p).(*java.Return)
	if out.Expression == nil {
		return out
	}
	expr := separateFrom("return", out.Expression)
	if expr == out.Expression {
		return out
	}
	copied := *out
	copied.Expression = expr
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitGoReturn(ret *golang.Return, p any) java.J {
	out := v.GoVisitor.VisitGoReturn(ret, p).(*golang.Return)
	if len(out.Expressions) == 0 {
		return out
	}
	expressions := separateFirst("return", out.Expressions)
	if java.SameSlice(expressions, out.Expressions) {
		return out
	}
	copied := *out
	copied.Expressions = expressions
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	out := v.GoVisitor.VisitVariableDeclarations(vd, p).(*java.VariableDeclarations)
	variables, typeExpr := out.Variables, out.TypeExpr

	if keyword := declarationKeyword(out.Markers); keyword != "" && len(variables) > 0 {
		variables = separateFirst(keyword, variables)
	}
	// Go writes a declared type straight after the name, so their separation is
	// the grammar's rather than the layout's. An unnamed parameter or result
	// prints no name to separate from.
	if typeExpr != nil && out.Varargs == nil && len(variables) > 0 &&
		getPrefix(typeExpr).IsEmpty() && printer.Print(variables[0].Element) != "" {
		typeExpr = withLeadingSpace(typeExpr).(java.Expression)
	}
	if java.SameSlice(variables, out.Variables) && typeExpr == out.TypeExpr {
		return out
	}
	copied := *out
	copied.Variables = variables
	copied.TypeExpr = typeExpr
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	out := v.GoVisitor.VisitTypeDecl(td, p).(*golang.TypeDecl)
	name, definition := out.Name, out.Definition
	// A spec inside `type (…)` prints no keyword of its own.
	if name != nil && out.Specs == nil && !java.HasMarker[golang.GroupedSpec](out.Markers) {
		name = separateFrom("type", name)
	}
	if definition != nil && name != nil {
		if fusesWith(printer.Print(name), printer.Print(definition)) {
			definition = withLeadingSpace(definition).(java.Expression)
		}
	}
	if name == out.Name && definition == out.Definition {
		return out
	}
	copied := *out
	copied.Name = name
	copied.Definition = definition
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	out := v.GoVisitor.VisitMethodDeclaration(md, p).(*java.MethodDeclaration)
	if out.Name != nil && !java.HasMarker[golang.InterfaceMethod](out.Markers) {
		return out.WithName(separateFrom("func", out.Name))
	}
	return out
}

// isDefaultClause reports whether c is Go's `default:`, which the printer spells
// as a lone identifier named default with no `case` ahead of it.
func isDefaultClause(c *java.Case) bool {
	if len(c.Expressions.Elements) != 1 {
		return false
	}
	ident, ok := c.Expressions.Elements[0].Element.(*java.Identifier)
	return ok && ident.Name == "default"
}

// followsAnInit reports whether the statement being visited is wrapped in an
// init clause, in which case its condition or tag follows the init's semicolon
// rather than the keyword.
func (v *MinimumViableSpacingVisitor) followsAnInit() bool {
	parent := v.Cursor().Parent()
	if parent == nil {
		return false
	}
	_, wrapped := parent.Value().(*golang.StatementWithInit)
	return wrapped
}

// separateStatements gives each statement the line break Go requires between
// two statements. Only a line break or a semicolon will do here, so this is not
// spacing the later passes could supply.
func separateStatements[T java.Tree](statements []java.RightPadded[T], separateFirst bool) []java.RightPadded[T] {
	var out []java.RightPadded[T]
	for i, rp := range statements {
		if (i == 0 && !separateFirst) || !getPrefix(rp.Element).IsEmpty() {
			continue
		}
		if out == nil {
			out = append([]java.RightPadded[T](nil), statements...)
		}
		if fixed, ok := any(withPrefix(rp.Element, java.MakeSpace(nil, "\n"))).(T); ok {
			out[i].Element = fixed
		}
	}
	if out == nil {
		return statements
	}
	return out
}

// separateFirst separates the first of a list of elements from the keyword
// written ahead of the list.
func separateFirst[T java.Tree](keyword string, elements []java.RightPadded[T]) []java.RightPadded[T] {
	first := separateFrom(keyword, elements[0].Element)
	if any(first) == any(elements[0].Element) {
		return elements
	}
	out := append([]java.RightPadded[T](nil), elements...)
	out[0].Element = first
	return out
}

// separateFrom gives t a leading space when writing it straight after keyword
// would lex as a single token. Whether whitespace already separates them is
// read off t's printed form, so it does not matter which of the Spaces within t
// holds it; leading names any Space printed ahead of t by its parent.
func separateFrom[T java.Tree](keyword string, t T, leading ...java.Space) T {
	for _, s := range leading {
		if !s.IsEmpty() {
			return t
		}
	}
	printed := printer.Print(t)
	if printed == "" || printed[0] == ' ' || printed[0] == '\t' || printed[0] == '\n' {
		return t
	}
	if !fusesWith(keyword, printed) {
		return t
	}
	return withLeadingSpace(t).(T)
}

func withLeadingSpace(t java.Tree) java.Tree {
	return withPrefix(t, java.MakeSpace(nil, " "))
}

// fusesWith reports whether the first token of after would join the last token
// of before, which happens when the characters either side of the join can both
// appear inside one identifier or number, or together spell one operator.
func fusesWith(before, after string) bool {
	before = strings.TrimRight(before, " \t\n")
	if before == "" || after == "" {
		return false
	}
	return tokenChar(before[len(before)-1]) && tokenChar(after[0]) ||
		spellsOperator(before, after[0])
}

// spellsOperator reports whether writing a straight after before lexes as one
// operator rather than two, keeping `+ +x` from reading as `++x`. before is the
// whole operator, so a `<-` followed by `-` stays two tokens.
func spellsOperator(before string, a byte) bool {
	switch before + string(a) {
	case "++", "--", "&&", "&^":
		return true
	}
	return false
}

func tokenChar(b byte) bool {
	return b == '_' || b == '.' ||
		'a' <= b && b <= 'z' ||
		'A' <= b && b <= 'Z' ||
		'0' <= b && b <= '9'
}

// declarationKeyword reports the keyword a declaration prints ahead of its
// first name, or "" for the forms that print none — a grouped spec, and the
// parameters and struct fields that share this node type.
func declarationKeyword(markers java.Markers) string {
	if java.HasMarker[golang.GroupedSpec](markers) {
		return ""
	}
	if java.HasMarker[golang.ConstDecl](markers) {
		return "const"
	}
	if java.HasMarker[golang.VarKeyword](markers) {
		return "var"
	}
	return ""
}

// VisitTypeParameter separates a type parameter from its constraint, which Go
// writes straight after the name.
func (v *MinimumViableSpacingVisitor) VisitTypeParameter(tp *java.TypeParameter, p any) java.J {
	out := v.GoVisitor.VisitTypeParameter(tp, p).(*java.TypeParameter)
	if out.Bounds == nil || len(out.Bounds.Elements) == 0 {
		return out
	}
	if !out.Bounds.Before.IsEmpty() || !getPrefix(out.Bounds.Elements[0].Element).IsEmpty() {
		return out
	}
	copied := *out
	bounds := *out.Bounds
	bounds.Before = java.MakeSpace(nil, " ")
	copied.Bounds = &bounds
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitIf(ifStmt *java.If, p any) java.J {
	out := v.GoVisitor.VisitIf(ifStmt, p).(*java.If)
	if !v.followsAnInit() {
		out = out.WithCondition(separateControl("if", out.Condition))
	}
	if out.ElsePart != nil && out.ElsePart.Body.Element != nil {
		if body := separateFrom("else", out.ElsePart.Body.Element); body != out.ElsePart.Body.Element {
			elsePart := *out.ElsePart
			elsePart.Body.Element = body
			out = out.WithElsePart(&elsePart)
		}
	}
	return out
}

func (v *MinimumViableSpacingVisitor) VisitSwitch(sw *java.Switch, p any) java.J {
	out := v.GoVisitor.VisitSwitch(sw, p).(*java.Switch)
	if out.Selector == nil {
		return out
	}
	if _, isEmpty := out.Selector.Tree.Element.(*java.Empty); isEmpty {
		return out
	}
	if v.followsAnInit() {
		return out
	}
	selector := separateControl("switch", out.Selector)
	if selector == out.Selector {
		return out
	}
	copied := *out
	copied.Selector = selector
	return &copied
}

// separateControl separates the expression a keyword controls, which Go writes
// without the parentheses the tree models around it.
func separateControl(keyword string, control *java.ControlParentheses) *java.ControlParentheses {
	element := separateFrom(keyword, control.Tree.Element, control.Prefix)
	if element == control.Tree.Element {
		return control
	}
	copied := *control
	copied.Tree.Element = element
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitForEachControl(control *java.ForEachControl, p any) java.J {
	out := v.GoVisitor.VisitForEachControl(control, p).(*java.ForEachControl)
	variable := separateFrom("for", out.Variable.Element, out.Prefix)
	iterable := separateFrom("range", out.Iterable.Element)
	if variable == out.Variable.Element && iterable == out.Iterable.Element {
		return out
	}
	copied := *out
	copied.Variable.Element = variable
	copied.Iterable.Element = iterable
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitGoStmt(g *golang.GoStmt, p any) java.J {
	out := v.GoVisitor.VisitGoStmt(g, p).(*golang.GoStmt)
	expr := separateFrom("go", out.Expr)
	if expr == out.Expr {
		return out
	}
	copied := *out
	copied.Expr = expr
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitDefer(d *golang.Defer, p any) java.J {
	out := v.GoVisitor.VisitDefer(d, p).(*golang.Defer)
	expr := separateFrom("defer", out.Expr)
	if expr == out.Expr {
		return out
	}
	copied := *out
	copied.Expr = expr
	return &copied
}

func (v *MinimumViableSpacingVisitor) VisitChannel(ch *golang.Channel, p any) java.J {
	out := v.GoVisitor.VisitChannel(ch, p).(*golang.Channel)
	value := separateFrom("chan", out.Value)
	if value == out.Value {
		return out
	}
	copied := *out
	copied.Value = value
	return &copied
}
