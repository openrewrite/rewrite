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

package visitor

import (
	"reflect"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// GoVisitor traverses and optionally transforms an OpenRewrite LST.
// Embed GoVisitor in a struct and override visit methods to customize behavior.
// Set Self to the outer struct to enable virtual dispatch.
//
// Cursor: GoVisitor maintains the current cursor as state. Recipes that
// need ancestor context call v.Cursor() inside any Visit* override (matches
// the JavaVisitor.getCursor() pattern). The RPC layer seeds an initial
// cursor via SetCursor before traversal begins.
//
// After-visits: a visit method can queue follow-up visitors via
// DoAfterVisit. The recipe runner drains the queue once the main visit
// returns and re-applies each queued visitor (transitively — afters can
// queue afters). This mirrors JavaVisitor.doAfterVisit and is the canonical
// way to compose side-effects like "add an import" after the main edit.
type GoVisitor struct {
	// Self must point to the outermost embedding struct for virtual dispatch.
	// If nil, dispatches to the default implementations on GoVisitor itself.
	Self interface{}

	cursor      *Cursor
	afterVisits []AfterVisitor
}

// AfterVisitor is the interface a follow-up visitor must satisfy to
// participate in DoAfterVisit. It's a structural alias for the
// recipe.TreeVisitor interface — duplicated here to avoid an import
// cycle between pkg/visitor and pkg/recipe.
type AfterVisitor interface {
	Visit(t java.Tree, p any) java.Tree
}

// Cursor returns the current cursor (the path from root to the node
// currently being visited). Mirrors JavaVisitor.getCursor().
func (v *GoVisitor) Cursor() *Cursor { return v.cursor }

// SetCursor seeds the visitor with an initial cursor chain. The RPC layer
// calls this with the chain reconstructed from a Visit request's cursor
// IDs before invoking Visit. Recipes typically don't call this directly.
func (v *GoVisitor) SetCursor(c *Cursor) { v.cursor = c }

// DoAfterVisit queues a follow-up visitor to run after the main visit
// completes. Mirrors JavaVisitor.doAfterVisit. Use this from inside any
// Visit* override to compose side-effects like adding an import:
//
//	svc := service.ImportServiceFor(cu)
//	v.DoAfterVisit(svc.AddImportVisitor("fmt", nil, false))
//
// The recipe runner drains the queue after Visit returns; queued
// visitors can themselves queue more after-visitors (transitive).
func (v *GoVisitor) DoAfterVisit(other AfterVisitor) {
	v.afterVisits = append(v.afterVisits, other)
}

// AfterVisits returns the queued follow-up visitors, then clears the
// queue. The recipe runner calls this once the main Visit returns and
// applies each visitor to the modified tree, looping until empty.
func (v *GoVisitor) AfterVisits() []AfterVisitor {
	out := v.afterVisits
	v.afterVisits = nil
	return out
}

// PendingAfterVisits returns the currently queued follow-up visitors
// without clearing the queue. Helper APIs use this to keep convenience
// methods like MaybeAddImport idempotent before the drain runs.
func (v *GoVisitor) PendingAfterVisits() []AfterVisitor {
	out := make([]AfterVisitor, len(v.afterVisits))
	copy(out, v.afterVisits)
	return out
}

// Visit dispatches to the appropriate visit method based on the node's concrete type.
//
// Lifecycle:
//  1. cursor is pushed for `t`.
//  2. PreVisit(t, p) is called via virtual dispatch — subclasses
//     (e.g. RPC sender/receiver) override it to handle cross-cutting
//     fields (id, prefix, markers) once per node.
//  3. The type-specific Visit* method is dispatched via virtual
//     dispatch on the (possibly modified) tree returned by PreVisit.
//  4. cursor pops on return.
//
// PreVisit returning nil short-circuits the visit — useful for
// receivers that get DELETE state from the wire.
func (v *GoVisitor) Visit(t java.Tree, p any) java.Tree {
	if t == nil {
		return nil
	}

	v.cursor = NewCursor(v.cursor, t)
	defer func() { v.cursor = v.cursor.parent }()

	t = v.self().PreVisit(t, p)
	if t == nil {
		return nil
	}

	switch n := t.(type) {
	case *golang.CompilationUnit:
		return v.self().VisitCompilationUnit(n, p)
	case *golang.GoMod:
		return v.self().VisitGoMod(n, p)
	case *golang.GoModDirective:
		return v.self().VisitGoModDirective(n, p)
	case *golang.GoModBlock:
		return v.self().VisitGoModBlock(n, p)
	case *golang.GoModValue:
		return v.self().VisitGoModValue(n, p)
	case *golang.GoSum:
		return v.self().VisitGoSum(n, p)
	case *golang.GoSumLine:
		return v.self().VisitGoSumLine(n, p)
	case *java.Identifier:
		return v.self().VisitIdentifier(n, p)
	case *java.Literal:
		return v.self().VisitLiteral(n, p)
	case *java.Binary:
		return v.self().VisitBinary(n, p)
	case *java.Block:
		return v.self().VisitBlock(n, p)
	case *java.Return:
		return v.self().VisitReturn(n, p)
	case *java.If:
		return v.self().VisitIf(n, p)
	case *java.Else:
		return v.self().VisitElse(n, p)
	case *java.Assignment:
		return v.self().VisitAssignment(n, p)
	case *java.MethodDeclaration:
		return v.self().VisitMethodDeclaration(n, p)
	case *java.FieldAccess:
		return v.self().VisitFieldAccess(n, p)
	case *java.MethodInvocation:
		return v.self().VisitMethodInvocation(n, p)
	case *java.VariableDeclarations:
		return v.self().VisitVariableDeclarations(n, p)
	case *golang.DeclarationBlock:
		return v.self().VisitDeclarationBlock(n, p)
	case *java.VariableDeclarator:
		return v.self().VisitVariableDeclarator(n, p)
	case *java.Import:
		return v.self().VisitImport(n, p)
	case *java.Unary:
		return v.self().VisitUnary(n, p)
	case *java.AssignmentOperation:
		return v.self().VisitAssignmentOperation(n, p)
	case *java.Switch:
		return v.self().VisitSwitch(n, p)
	case *java.Case:
		return v.self().VisitCase(n, p)
	case *java.ForLoop:
		return v.self().VisitForLoop(n, p)
	case *java.ForControl:
		return v.self().VisitForControl(n, p)
	case *java.ForEachLoop:
		return v.self().VisitForEachLoop(n, p)
	case *java.ForEachControl:
		return v.self().VisitForEachControl(n, p)
	case *java.Break:
		return v.self().VisitBreak(n, p)
	case *java.Continue:
		return v.self().VisitContinue(n, p)
	case *java.Label:
		return v.self().VisitLabel(n, p)
	case *golang.GoStmt:
		return v.self().VisitGoStmt(n, p)
	case *golang.Defer:
		return v.self().VisitDefer(n, p)
	case *golang.Send:
		return v.self().VisitSend(n, p)
	case *golang.Goto:
		return v.self().VisitGoto(n, p)
	case *golang.Fallthrough:
		return v.self().VisitFallthrough(n, p)
	case *java.Empty:
		return v.self().VisitEmpty(n, p)
	case *java.Annotation:
		return v.self().VisitAnnotation(n, p)
	case *java.ArrayType:
		return v.self().VisitArrayType(n, p)
	case *java.Parentheses:
		return v.self().VisitParentheses(n, p)
	case *java.TypeCast:
		return v.self().VisitTypeCast(n, p)
	case *java.ControlParentheses:
		return v.self().VisitControlParentheses(n, p)
	case *java.ArrayAccess:
		return v.self().VisitArrayAccess(n, p)
	case *java.ParameterizedType:
		return v.self().VisitParameterizedType(n, p)
	case *java.TypeParameters:
		return v.self().VisitTypeParameters(n, p)
	case *java.TypeParameter:
		return v.self().VisitTypeParameter(n, p)
	case *golang.IndexList:
		return v.self().VisitIndexList(n, p)
	case *java.ArrayDimension:
		return v.self().VisitArrayDimension(n, p)
	case *golang.Composite:
		return v.self().VisitComposite(n, p)
	case *golang.KeyValue:
		return v.self().VisitKeyValue(n, p)
	case *golang.Slice:
		return v.self().VisitSlice(n, p)
	case *golang.MapType:
		return v.self().VisitMapType(n, p)
	case *golang.StatementExpression:
		return v.self().VisitStatementExpression(n, p)
	case *golang.PointerType:
		return v.self().VisitPointerType(n, p)
	case *golang.ArrayType:
		return v.self().VisitGoArrayType(n, p)
	case *golang.Channel:
		return v.self().VisitChannel(n, p)
	case *golang.FuncType:
		return v.self().VisitFuncType(n, p)
	case *golang.TypeList:
		return v.self().VisitTypeList(n, p)
	case *golang.Union:
		return v.self().VisitUnion(n, p)
	case *golang.UnderlyingType:
		return v.self().VisitUnderlyingType(n, p)
	case *golang.TypeDecl:
		return v.self().VisitTypeDecl(n, p)
	case *golang.StructType:
		return v.self().VisitStructType(n, p)
	case *golang.InterfaceType:
		return v.self().VisitInterfaceType(n, p)
	case *golang.MultiAssignment:
		return v.self().VisitMultiAssignment(n, p)
	case *golang.Return:
		return v.self().VisitGoReturn(n, p)
	case *golang.MethodDeclaration:
		return v.self().VisitGoMethodDeclaration(n, p)
	case *golang.StatementWithInit:
		return v.self().VisitStatementWithInit(n, p)
	case *golang.CommClause:
		return v.self().VisitCommClause(n, p)
	case *golang.Unary:
		return v.self().VisitGoUnary(n, p)
	case *golang.Binary:
		return v.self().VisitGoBinary(n, p)
	case *golang.AssignmentOperation:
		return v.self().VisitGoAssignmentOperation(n, p)
	case *golang.Variadic:
		return v.self().VisitGoVariadic(n, p)
	default:
		return t
	}
}

func (v *GoVisitor) self() VisitorI {
	if v.Self != nil {
		if vi, ok := v.Self.(VisitorI); ok {
			return vi
		}
	}
	return v
}

// VisitorI defines all overridable visit methods.
type VisitorI interface {
	Visit(t java.Tree, p any) java.Tree
	PreVisit(t java.Tree, p any) java.Tree
	VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J
	// go.mod nodes are Tree, not J (their tokens are not Java
	// expressions), so these return java.Tree rather than java.J.
	VisitGoMod(gm *golang.GoMod, p any) java.Tree
	VisitGoModDirective(d *golang.GoModDirective, p any) java.Tree
	VisitGoModBlock(b *golang.GoModBlock, p any) java.Tree
	VisitGoModValue(val *golang.GoModValue, p any) java.Tree
	VisitGoSum(gs *golang.GoSum, p any) java.Tree
	VisitGoSumLine(l *golang.GoSumLine, p any) java.Tree
	VisitIdentifier(ident *java.Identifier, p any) java.J
	VisitLiteral(lit *java.Literal, p any) java.J
	VisitBinary(bin *java.Binary, p any) java.J
	VisitBlock(block *java.Block, p any) java.J
	VisitReturn(ret *java.Return, p any) java.J
	VisitIf(ifStmt *java.If, p any) java.J
	VisitElse(el *java.Else, p any) java.J
	VisitAssignment(assign *java.Assignment, p any) java.J
	VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J
	VisitFieldAccess(fa *java.FieldAccess, p any) java.J
	VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J
	VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J
	VisitDeclarationBlock(db *golang.DeclarationBlock, p any) java.J
	VisitVariableDeclarator(vd *java.VariableDeclarator, p any) java.J
	VisitImport(imp *java.Import, p any) java.J
	VisitUnary(unary *java.Unary, p any) java.J
	VisitAssignmentOperation(ao *java.AssignmentOperation, p any) java.J
	VisitSwitch(sw *java.Switch, p any) java.J
	VisitCase(c *java.Case, p any) java.J
	VisitForLoop(forLoop *java.ForLoop, p any) java.J
	VisitForControl(control *java.ForControl, p any) java.J
	VisitForEachLoop(forEach *java.ForEachLoop, p any) java.J
	VisitForEachControl(control *java.ForEachControl, p any) java.J
	VisitBreak(b *java.Break, p any) java.J
	VisitContinue(c *java.Continue, p any) java.J
	VisitLabel(l *java.Label, p any) java.J
	VisitGoStmt(g *golang.GoStmt, p any) java.J
	VisitDefer(d *golang.Defer, p any) java.J
	VisitSend(s *golang.Send, p any) java.J
	VisitGoto(g *golang.Goto, p any) java.J
	VisitFallthrough(f *golang.Fallthrough, p any) java.J
	VisitEmpty(empty *java.Empty, p any) java.J
	VisitAnnotation(ann *java.Annotation, p any) java.J
	VisitArrayType(at *java.ArrayType, p any) java.J
	VisitGoArrayType(at *golang.ArrayType, p any) java.J
	VisitParentheses(paren *java.Parentheses, p any) java.J
	VisitTypeCast(tc *java.TypeCast, p any) java.J
	VisitControlParentheses(cp *java.ControlParentheses, p any) java.J
	VisitArrayAccess(aa *java.ArrayAccess, p any) java.J
	VisitParameterizedType(pt *java.ParameterizedType, p any) java.J
	VisitTypeParameters(tps *java.TypeParameters, p any) java.J
	VisitTypeParameter(tp *java.TypeParameter, p any) java.J
	VisitIndexList(il *golang.IndexList, p any) java.J
	VisitArrayDimension(ad *java.ArrayDimension, p any) java.J
	VisitComposite(c *golang.Composite, p any) java.J
	VisitKeyValue(kv *golang.KeyValue, p any) java.J
	VisitSlice(s *golang.Slice, p any) java.J
	VisitMapType(mt *golang.MapType, p any) java.J
	VisitStatementExpression(se *golang.StatementExpression, p any) java.J
	VisitPointerType(pt *golang.PointerType, p any) java.J
	VisitChannel(ch *golang.Channel, p any) java.J
	VisitFuncType(ft *golang.FuncType, p any) java.J
	VisitTypeList(tl *golang.TypeList, p any) java.J
	VisitUnion(u *golang.Union, p any) java.J
	VisitUnderlyingType(ut *golang.UnderlyingType, p any) java.J
	VisitTypeDecl(td *golang.TypeDecl, p any) java.J
	VisitStructType(st *golang.StructType, p any) java.J
	VisitInterfaceType(it *golang.InterfaceType, p any) java.J
	VisitMultiAssignment(ma *golang.MultiAssignment, p any) java.J
	VisitGoReturn(ret *golang.Return, p any) java.J
	VisitGoMethodDeclaration(md *golang.MethodDeclaration, p any) java.J
	VisitStatementWithInit(s *golang.StatementWithInit, p any) java.J
	VisitCommClause(cc *golang.CommClause, p any) java.J
	VisitGoUnary(u *golang.Unary, p any) java.J
	VisitGoBinary(b *golang.Binary, p any) java.J
	VisitGoAssignmentOperation(a *golang.AssignmentOperation, p any) java.J
	VisitGoVariadic(v *golang.Variadic, p any) java.J
	VisitSpace(space java.Space, p any) java.Space
	VisitType(javaType java.JavaType, p any) java.JavaType
	VisitMarker(marker java.Marker, p any) java.Marker
}

// Ensure GoVisitor satisfies VisitorI.
var _ VisitorI = (*GoVisitor)(nil)

// PreVisit is the per-node hook called by Visit() before dispatching
// to the type-specific Visit* method. The default implementation is
// the identity function. RPC senders/receivers override it to
// serialize/deserialize the cross-cutting `id`, `prefix`, and
// `markers` fields once per node, mirroring Java's
// JavaVisitor.preVisit pattern.
func (v *GoVisitor) PreVisit(t java.Tree, p any) java.Tree { return t }

func (v *GoVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	prefix := v.self().VisitSpace(cu.Prefix, p)
	markers := v.visitMarkers(cu.Markers, p)
	pkg := cu.PackageDecl
	if pkg != nil {
		elem := visitAndCast[*java.Identifier](v, pkg.Element, p)
		after := v.self().VisitSpace(pkg.After, p)
		if elem != pkg.Element || !java.SpaceEqual(after, pkg.After) {
			c := *pkg
			c.Element = elem
			c.After = after
			pkg = &c
		}
	}
	imports := cu.Imports
	if imports != nil {
		before := v.self().VisitSpace(imports.Before, p)
		impMarkers := v.visitMarkers(imports.Markers, p)
		elems := visitRightPaddedList(v, imports.Elements, p)
		if !java.SpaceEqual(before, imports.Before) || !java.MarkersEqual(impMarkers, imports.Markers) || !java.SameSlice(elems, imports.Elements) {
			c := *imports
			c.Before = before
			c.Markers = impMarkers
			c.Elements = elems
			imports = &c
		}
	}
	statements := visitRightPaddedList(v, cu.Statements, p)
	eof := v.self().VisitSpace(cu.EOF, p)
	if java.SpaceEqual(prefix, cu.Prefix) && java.MarkersEqual(markers, cu.Markers) &&
		pkg == cu.PackageDecl && imports == cu.Imports &&
		java.SameSlice(statements, cu.Statements) && java.SpaceEqual(eof, cu.EOF) {
		return cu
	}
	c := *cu
	c.Prefix = prefix
	c.Markers = markers
	c.PackageDecl = pkg
	c.Imports = imports
	c.Statements = statements
	c.EOF = eof
	return &c
}

func (v *GoVisitor) VisitGoMod(gm *golang.GoMod, p any) java.Tree {
	gm = gm.WithPrefix(v.self().VisitSpace(gm.Prefix, p))
	gm = gm.WithMarkers(v.visitMarkers(gm.Markers, p))
	gm = gm.WithStatements(visitGoModStatementList(v, gm.Statements, p))
	gm = gm.WithEof(v.self().VisitSpace(gm.Eof, p))
	return gm
}

func (v *GoVisitor) VisitGoModDirective(d *golang.GoModDirective, p any) java.Tree {
	d = d.WithPrefix(v.self().VisitSpace(d.Prefix, p))
	d = d.WithMarkers(v.visitMarkers(d.Markers, p))
	d = d.WithValues(visitGoModValueList(v, d.Values, p))
	return d
}

func (v *GoVisitor) VisitGoModBlock(b *golang.GoModBlock, p any) java.Tree {
	prefix := v.self().VisitSpace(b.Prefix, p)
	markers := v.visitMarkers(b.Markers, p)
	beforeLParen := v.self().VisitSpace(b.BeforeLParen, p)
	entries := visitGoModStatementList(v, b.Entries, p)
	beforeRParen := v.self().VisitSpace(b.BeforeRParen, p)
	if java.SpaceEqual(prefix, b.Prefix) && java.MarkersEqual(markers, b.Markers) &&
		java.SpaceEqual(beforeLParen, b.BeforeLParen) && java.SameSlice(entries, b.Entries) &&
		java.SpaceEqual(beforeRParen, b.BeforeRParen) {
		return b
	}
	c := *b
	c.Prefix = prefix
	c.Markers = markers
	c.BeforeLParen = beforeLParen
	c.Entries = entries
	c.BeforeRParen = beforeRParen
	return &c
}

func (v *GoVisitor) VisitGoModValue(val *golang.GoModValue, p any) java.Tree {
	val = val.WithPrefix(v.self().VisitSpace(val.Prefix, p))
	val = val.WithMarkers(v.visitMarkers(val.Markers, p))
	return val
}

func (v *GoVisitor) VisitGoSum(gs *golang.GoSum, p any) java.Tree {
	gs = gs.WithPrefix(v.self().VisitSpace(gs.Prefix, p))
	gs = gs.WithMarkers(v.visitMarkers(gs.Markers, p))
	gs = gs.WithLines(visitGoSumLineList(v, gs.Lines, p))
	gs = gs.WithEof(v.self().VisitSpace(gs.Eof, p))
	return gs
}

func (v *GoVisitor) VisitGoSumLine(l *golang.GoSumLine, p any) java.Tree {
	l = l.WithPrefix(v.self().VisitSpace(l.Prefix, p))
	l = l.WithMarkers(v.visitMarkers(l.Markers, p))
	return l
}

func (v *GoVisitor) VisitIdentifier(ident *java.Identifier, p any) java.J {
	ident = ident.WithPrefix(v.self().VisitSpace(ident.Prefix, p))
	ident = ident.WithMarkers(v.visitMarkers(ident.Markers, p))
	return ident
}

func (v *GoVisitor) VisitLiteral(lit *java.Literal, p any) java.J {
	lit = lit.WithPrefix(v.self().VisitSpace(lit.Prefix, p))
	lit = lit.WithMarkers(v.visitMarkers(lit.Markers, p))
	return lit
}

func (v *GoVisitor) VisitBinary(bin *java.Binary, p any) java.J {
	bin = bin.WithPrefix(v.self().VisitSpace(bin.Prefix, p))
	bin = bin.WithMarkers(v.visitMarkers(bin.Markers, p))
	bin = bin.WithLeft(visitExpression(v, bin.Left, p))
	bin = bin.WithRight(visitExpression(v, bin.Right, p))
	return bin
}

func (v *GoVisitor) VisitBlock(block *java.Block, p any) java.J {
	block = block.WithPrefix(v.self().VisitSpace(block.Prefix, p))
	block = block.WithMarkers(v.visitMarkers(block.Markers, p))
	block = block.WithStatements(visitRightPaddedList(v, block.Statements, p))
	block = block.WithEnd(v.self().VisitSpace(block.End, p))
	return block
}

func (v *GoVisitor) VisitReturn(ret *java.Return, p any) java.J {
	prefix := v.self().VisitSpace(ret.Prefix, p)
	markers := v.visitMarkers(ret.Markers, p)
	expr := ret.Expression
	if expr != nil {
		expr = visitAndCast[java.Expression](v, expr, p)
	}
	if java.SpaceEqual(prefix, ret.Prefix) && java.MarkersEqual(markers, ret.Markers) && expr == ret.Expression {
		return ret
	}
	c := *ret
	c.Prefix = prefix
	c.Markers = markers
	c.Expression = expr
	return &c
}

func (v *GoVisitor) VisitGoReturn(ret *golang.Return, p any) java.J {
	prefix := v.self().VisitSpace(ret.Prefix, p)
	markers := v.visitMarkers(ret.Markers, p)
	exprs := visitRightPaddedExpressionList(v, ret.Expressions, p)
	if java.SpaceEqual(prefix, ret.Prefix) && java.MarkersEqual(markers, ret.Markers) && java.SameSlice(exprs, ret.Expressions) {
		return ret
	}
	c := *ret
	c.Prefix = prefix
	c.Markers = markers
	c.Expressions = exprs
	return &c
}

func (v *GoVisitor) VisitIf(ifStmt *java.If, p any) java.J {
	prefix := v.self().VisitSpace(ifStmt.Prefix, p)
	markers := v.visitMarkers(ifStmt.Markers, p)
	condition := visitAndCast[*java.ControlParentheses](v, ifStmt.Condition, p)
	thenPart := ifStmt.ThenPart
	thenPart.Element = v.self().Visit(ifStmt.ThenPart.Element, p).(java.Statement)
	thenPart.After = v.self().VisitSpace(ifStmt.ThenPart.After, p)
	elsePart := ifStmt.ElsePart
	if elsePart != nil {
		elsePart = visitAndCast[*java.Else](v, ifStmt.ElsePart, p)
	}
	if java.SpaceEqual(prefix, ifStmt.Prefix) && java.MarkersEqual(markers, ifStmt.Markers) &&
		condition == ifStmt.Condition && java.RightPaddedEqual(thenPart, ifStmt.ThenPart) && elsePart == ifStmt.ElsePart {
		return ifStmt
	}
	c := *ifStmt
	c.Prefix = prefix
	c.Markers = markers
	c.Condition = condition
	c.ThenPart = thenPart
	c.ElsePart = elsePart
	return &c
}

func (v *GoVisitor) VisitElse(el *java.Else, p any) java.J {
	prefix := v.self().VisitSpace(el.Prefix, p)
	markers := v.visitMarkers(el.Markers, p)
	bodyElem := v.self().Visit(el.Body.Element, p).(java.Statement)
	bodyAfter := v.self().VisitSpace(el.Body.After, p)
	if java.SpaceEqual(prefix, el.Prefix) && java.MarkersEqual(markers, el.Markers) &&
		bodyElem == el.Body.Element && java.SpaceEqual(bodyAfter, el.Body.After) {
		return el
	}
	c := *el
	c.Prefix = prefix
	c.Markers = markers
	c.Body.Element = bodyElem
	c.Body.After = bodyAfter
	return &c
}

func (v *GoVisitor) VisitAssignment(assign *java.Assignment, p any) java.J {
	prefix := v.self().VisitSpace(assign.Prefix, p)
	markers := v.visitMarkers(assign.Markers, p)
	variable := visitExpression(v, assign.Variable, p)
	valueBefore := v.self().VisitSpace(assign.Value.Before, p)
	valueElem := visitExpression(v, assign.Value.Element, p)
	if java.SpaceEqual(prefix, assign.Prefix) && java.MarkersEqual(markers, assign.Markers) &&
		variable == assign.Variable &&
		java.SpaceEqual(valueBefore, assign.Value.Before) && valueElem == assign.Value.Element {
		return assign
	}
	c := *assign
	c.Prefix = prefix
	c.Markers = markers
	c.Variable = variable
	c.Value.Before = valueBefore
	c.Value.Element = valueElem
	return &c
}

func (v *GoVisitor) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	prefix := v.self().VisitSpace(md.Prefix, p)
	markers := v.visitMarkers(md.Markers, p)
	anns := visitAnnotationList(v, md.LeadingAnnotations, p)
	name := visitAndCast[*java.Identifier](v, md.Name, p)
	tps := md.TypeParameters
	if tps != nil {
		tps = visitAndCast[*java.TypeParameters](v, tps, p)
	}
	paramsBefore := v.self().VisitSpace(md.Parameters.Before, p)
	paramsElems := visitRightPaddedList(v, md.Parameters.Elements, p)
	returnType := md.ReturnType
	if returnType != nil {
		returnType = visitExpression(v, returnType, p)
	}
	body := md.Body
	if body != nil {
		body = visitAndCast[*java.Block](v, body, p)
	}
	if java.SpaceEqual(prefix, md.Prefix) && java.MarkersEqual(markers, md.Markers) &&
		java.SameSlice(anns, md.LeadingAnnotations) && name == md.Name && tps == md.TypeParameters &&
		java.SpaceEqual(paramsBefore, md.Parameters.Before) && java.SameSlice(paramsElems, md.Parameters.Elements) &&
		returnType == md.ReturnType && body == md.Body {
		return md
	}
	c := *md
	c.Prefix = prefix
	c.Markers = markers
	c.LeadingAnnotations = anns
	c.Name = name
	c.TypeParameters = tps
	c.Parameters.Before = paramsBefore
	c.Parameters.Elements = paramsElems
	c.ReturnType = returnType
	c.Body = body
	return &c
}

func (v *GoVisitor) VisitGoMethodDeclaration(md *golang.MethodDeclaration, p any) java.J {
	prefix := v.self().VisitSpace(md.Prefix, p)
	markers := v.visitMarkers(md.Markers, p)
	recvBefore := v.self().VisitSpace(md.Receiver.Before, p)
	recvElems := visitRightPaddedList(v, md.Receiver.Elements, p)
	decl := visitAndCast[*java.MethodDeclaration](v, md.Declaration, p)
	if java.SpaceEqual(prefix, md.Prefix) && java.MarkersEqual(markers, md.Markers) &&
		java.SpaceEqual(recvBefore, md.Receiver.Before) && java.SameSlice(recvElems, md.Receiver.Elements) &&
		decl == md.Declaration {
		return md
	}
	c := *md
	c.Prefix = prefix
	c.Markers = markers
	c.Receiver.Before = recvBefore
	c.Receiver.Elements = recvElems
	c.Declaration = decl
	return &c
}

func (v *GoVisitor) VisitStatementWithInit(s *golang.StatementWithInit, p any) java.J {
	prefix := v.self().VisitSpace(s.Prefix, p)
	markers := v.visitMarkers(s.Markers, p)
	initElem := v.self().Visit(s.Init.Element, p).(java.Statement)
	initAfter := v.self().VisitSpace(s.Init.After, p)
	stmt := v.self().Visit(s.Statement, p).(java.Statement)
	if java.SpaceEqual(prefix, s.Prefix) && java.MarkersEqual(markers, s.Markers) &&
		initElem == s.Init.Element && java.SpaceEqual(initAfter, s.Init.After) && stmt == s.Statement {
		return s
	}
	c := *s
	c.Prefix = prefix
	c.Markers = markers
	c.Init.Element = initElem
	c.Init.After = initAfter
	c.Statement = stmt
	return &c
}

func (v *GoVisitor) VisitTypeParameters(tps *java.TypeParameters, p any) java.J {
	prefix := v.self().VisitSpace(tps.Prefix, p)
	markers := v.visitMarkers(tps.Markers, p)
	params := visitRightPaddedList(v, tps.TypeParameters, p)
	if java.SpaceEqual(prefix, tps.Prefix) && java.MarkersEqual(markers, tps.Markers) &&
		java.SameSlice(params, tps.TypeParameters) {
		return tps
	}
	c := *tps
	c.Prefix = prefix
	c.Markers = markers
	c.TypeParameters = params
	return &c
}

func (v *GoVisitor) VisitTypeParameter(tp *java.TypeParameter, p any) java.J {
	prefix := v.self().VisitSpace(tp.Prefix, p)
	markers := v.visitMarkers(tp.Markers, p)
	name := tp.Name
	if name != nil {
		name = visitExpression(v, name, p)
	}
	bounds := tp.Bounds
	if bounds != nil {
		before := v.self().VisitSpace(bounds.Before, p)
		elems := visitRightPaddedList(v, bounds.Elements, p)
		if !java.SpaceEqual(before, bounds.Before) || !java.SameSlice(elems, bounds.Elements) {
			c := *bounds
			c.Before = before
			c.Elements = elems
			bounds = &c
		}
	}
	if java.SpaceEqual(prefix, tp.Prefix) && java.MarkersEqual(markers, tp.Markers) &&
		name == tp.Name && bounds == tp.Bounds {
		return tp
	}
	c := *tp
	c.Prefix = prefix
	c.Markers = markers
	c.Name = name
	c.Bounds = bounds
	return &c
}

func (v *GoVisitor) VisitFieldAccess(fa *java.FieldAccess, p any) java.J {
	prefix := v.self().VisitSpace(fa.Prefix, p)
	markers := v.visitMarkers(fa.Markers, p)
	target := visitExpression(v, fa.Target, p)
	// Visit the selector identifier so recipes that traverse identifiers
	// see the right-hand side of `target.Name` (e.g. the `Box` in
	// `a.Box[int]{...}`). Mirrors JavaIsoVisitor.visitFieldAccess.
	nameBefore := v.self().VisitSpace(fa.Name.Before, p)
	nameElem := visitAndCast[*java.Identifier](v, fa.Name.Element, p)
	if java.SpaceEqual(prefix, fa.Prefix) && java.MarkersEqual(markers, fa.Markers) &&
		target == fa.Target &&
		java.SpaceEqual(nameBefore, fa.Name.Before) && nameElem == fa.Name.Element {
		return fa
	}
	c := *fa
	c.Prefix = prefix
	c.Markers = markers
	c.Target = target
	c.Name.Before = nameBefore
	c.Name.Element = nameElem
	return &c
}

func (v *GoVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	prefix := v.self().VisitSpace(mi.Prefix, p)
	markers := v.visitMarkers(mi.Markers, p)
	sel := mi.Select
	if sel != nil {
		e := visitExpression(v, sel.Element, p)
		a := v.self().VisitSpace(sel.After, p)
		if e != sel.Element || !java.SpaceEqual(a, sel.After) {
			c := *sel
			c.Element = e
			c.After = a
			sel = &c
		}
	}
	name := visitAndCast[*java.Identifier](v, mi.Name, p)
	tps := mi.TypeParameters
	if tps != nil {
		before := v.self().VisitSpace(tps.Before, p)
		elems := visitRightPaddedList(v, tps.Elements, p)
		if !java.SpaceEqual(before, tps.Before) || !java.SameSlice(elems, tps.Elements) {
			c := *tps
			c.Before = before
			c.Elements = elems
			tps = &c
		}
	}
	argsBefore := v.self().VisitSpace(mi.Arguments.Before, p)
	argsElems := visitRightPaddedList(v, mi.Arguments.Elements, p)
	if java.SpaceEqual(prefix, mi.Prefix) && java.MarkersEqual(markers, mi.Markers) &&
		sel == mi.Select && name == mi.Name && tps == mi.TypeParameters &&
		java.SpaceEqual(argsBefore, mi.Arguments.Before) && java.SameSlice(argsElems, mi.Arguments.Elements) {
		return mi
	}
	c := *mi
	c.Prefix = prefix
	c.Markers = markers
	c.Select = sel
	c.Name = name
	c.TypeParameters = tps
	c.Arguments.Before = argsBefore
	c.Arguments.Elements = argsElems
	return &c
}

func (v *GoVisitor) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	prefix := v.self().VisitSpace(vd.Prefix, p)
	markers := v.visitMarkers(vd.Markers, p)
	anns := visitAnnotationList(v, vd.LeadingAnnotations, p)
	typeExpr := vd.TypeExpr
	if typeExpr != nil {
		typeExpr = visitExpression(v, typeExpr, p)
	}
	variables := visitRightPaddedList(v, vd.Variables, p)
	if java.SpaceEqual(prefix, vd.Prefix) && java.MarkersEqual(markers, vd.Markers) &&
		java.SameSlice(anns, vd.LeadingAnnotations) && typeExpr == vd.TypeExpr &&
		java.SameSlice(variables, vd.Variables) {
		return vd
	}
	c := *vd
	c.Prefix = prefix
	c.Markers = markers
	c.LeadingAnnotations = anns
	c.TypeExpr = typeExpr
	c.Variables = variables
	return &c
}

func (v *GoVisitor) VisitDeclarationBlock(db *golang.DeclarationBlock, p any) java.J {
	prefix := v.self().VisitSpace(db.Prefix, p)
	markers := v.visitMarkers(db.Markers, p)
	anns := visitAnnotationList(v, db.LeadingAnnotations, p)
	specs := db.Specs
	if specs != nil {
		before := v.self().VisitSpace(specs.Before, p)
		elems := visitRightPaddedList(v, specs.Elements, p)
		if !java.SpaceEqual(before, specs.Before) || !java.SameSlice(elems, specs.Elements) {
			c := *specs
			c.Before = before
			c.Elements = elems
			specs = &c
		}
	}
	if java.SpaceEqual(prefix, db.Prefix) && java.MarkersEqual(markers, db.Markers) &&
		java.SameSlice(anns, db.LeadingAnnotations) && specs == db.Specs {
		return db
	}
	c := *db
	c.Prefix = prefix
	c.Markers = markers
	c.LeadingAnnotations = anns
	c.Specs = specs
	return &c
}

func (v *GoVisitor) VisitVariableDeclarator(vd *java.VariableDeclarator, p any) java.J {
	prefix := v.self().VisitSpace(vd.Prefix, p)
	markers := v.visitMarkers(vd.Markers, p)
	name := visitAndCast[*java.Identifier](v, vd.Name, p)
	init := vd.Initializer
	if init != nil {
		before := v.self().VisitSpace(init.Before, p)
		elem := visitExpression(v, init.Element, p)
		if !java.SpaceEqual(before, init.Before) || elem != init.Element {
			c := *init
			c.Before = before
			c.Element = elem
			init = &c
		}
	}
	if java.SpaceEqual(prefix, vd.Prefix) && java.MarkersEqual(markers, vd.Markers) &&
		name == vd.Name && init == vd.Initializer {
		return vd
	}
	c := *vd
	c.Prefix = prefix
	c.Markers = markers
	c.Name = name
	c.Initializer = init
	return &c
}

func (v *GoVisitor) VisitImport(imp *java.Import, p any) java.J {
	prefix := v.self().VisitSpace(imp.Prefix, p)
	markers := v.visitMarkers(imp.Markers, p)
	alias := imp.Alias
	if alias != nil {
		before := v.self().VisitSpace(alias.Before, p)
		elem := visitAndCast[*java.Identifier](v, alias.Element, p)
		if !java.SpaceEqual(before, alias.Before) || elem != alias.Element {
			c := *alias
			c.Before = before
			c.Element = elem
			alias = &c
		}
	}
	qualid := visitExpression(v, imp.Qualid, p)
	if java.SpaceEqual(prefix, imp.Prefix) && java.MarkersEqual(markers, imp.Markers) &&
		alias == imp.Alias && qualid == imp.Qualid {
		return imp
	}
	c := *imp
	c.Prefix = prefix
	c.Markers = markers
	c.Alias = alias
	c.Qualid = qualid
	return &c
}

func (v *GoVisitor) VisitUnary(unary *java.Unary, p any) java.J {
	unary = unary.WithPrefix(v.self().VisitSpace(unary.Prefix, p))
	unary = unary.WithMarkers(v.visitMarkers(unary.Markers, p))
	unary = unary.WithOperand(visitExpression(v, unary.Operand, p))
	return unary
}

func (v *GoVisitor) VisitAssignmentOperation(ao *java.AssignmentOperation, p any) java.J {
	prefix := v.self().VisitSpace(ao.Prefix, p)
	markers := v.visitMarkers(ao.Markers, p)
	variable := visitExpression(v, ao.Variable, p)
	assignment := visitExpression(v, ao.Assignment, p)
	if java.SpaceEqual(prefix, ao.Prefix) && java.MarkersEqual(markers, ao.Markers) &&
		variable == ao.Variable && assignment == ao.Assignment {
		return ao
	}
	c := *ao
	c.Prefix = prefix
	c.Markers = markers
	c.Variable = variable
	c.Assignment = assignment
	return &c
}

func (v *GoVisitor) VisitSwitch(sw *java.Switch, p any) java.J {
	sw = sw.WithPrefix(v.self().VisitSpace(sw.Prefix, p))
	sw = sw.WithMarkers(v.visitMarkers(sw.Markers, p))
	sw = sw.WithBody(visitAndCast[*java.Block](v, sw.Body, p))
	return sw
}

func (v *GoVisitor) VisitCase(cse *java.Case, p any) java.J {
	prefix := v.self().VisitSpace(cse.Prefix, p)
	markers := v.visitMarkers(cse.Markers, p)
	exprsBefore := v.self().VisitSpace(cse.Expressions.Before, p)
	exprsElems := visitRightPaddedExpressionList(v, cse.Expressions.Elements, p)
	body := visitRightPaddedList(v, cse.Body, p)
	if java.SpaceEqual(prefix, cse.Prefix) && java.MarkersEqual(markers, cse.Markers) &&
		java.SpaceEqual(exprsBefore, cse.Expressions.Before) && java.SameSlice(exprsElems, cse.Expressions.Elements) &&
		java.SameSlice(body, cse.Body) {
		return cse
	}
	c := *cse
	c.Prefix = prefix
	c.Markers = markers
	c.Expressions.Before = exprsBefore
	c.Expressions.Elements = exprsElems
	c.Body = body
	return &c
}

func (v *GoVisitor) VisitForLoop(forLoop *java.ForLoop, p any) java.J {
	prefix := v.self().VisitSpace(forLoop.Prefix, p)
	markers := v.visitMarkers(forLoop.Markers, p)
	ctrl := visitAndCast[*java.ForControl](v, &forLoop.Control, p)
	body := visitAndCast[*java.Block](v, forLoop.Body, p)
	if java.SpaceEqual(prefix, forLoop.Prefix) && java.MarkersEqual(markers, forLoop.Markers) &&
		ctrl == &forLoop.Control && body == forLoop.Body {
		return forLoop
	}
	c := *forLoop
	c.Prefix = prefix
	c.Markers = markers
	c.Control = *ctrl
	c.Body = body
	return &c
}

func (v *GoVisitor) VisitForControl(control *java.ForControl, p any) java.J {
	prefix := v.self().VisitSpace(control.Prefix, p)
	markers := v.visitMarkers(control.Markers, p)
	init := control.Init
	if init != nil {
		elem := visitAndCast[java.Statement](v, init.Element, p)
		after := v.self().VisitSpace(init.After, p)
		if elem != init.Element || !java.SpaceEqual(after, init.After) {
			c := *init
			c.Element = elem
			c.After = after
			init = &c
		}
	}
	cond := control.Condition
	if cond != nil {
		elem := visitExpression(v, cond.Element, p)
		after := v.self().VisitSpace(cond.After, p)
		if elem != cond.Element || !java.SpaceEqual(after, cond.After) {
			c := *cond
			c.Element = elem
			c.After = after
			cond = &c
		}
	}
	update := control.Update
	if update != nil {
		elem := visitAndCast[java.Statement](v, update.Element, p)
		after := v.self().VisitSpace(update.After, p)
		if elem != update.Element || !java.SpaceEqual(after, update.After) {
			c := *update
			c.Element = elem
			c.After = after
			update = &c
		}
	}
	if java.SpaceEqual(prefix, control.Prefix) && java.MarkersEqual(markers, control.Markers) &&
		init == control.Init && cond == control.Condition && update == control.Update {
		return control
	}
	c := *control
	c.Prefix = prefix
	c.Markers = markers
	c.Init = init
	c.Condition = cond
	c.Update = update
	return &c
}

func (v *GoVisitor) VisitForEachLoop(forEach *java.ForEachLoop, p any) java.J {
	forEach = forEach.WithPrefix(v.self().VisitSpace(forEach.Prefix, p))
	forEach = forEach.WithMarkers(v.visitMarkers(forEach.Markers, p))
	forEach = forEach.WithBody(visitAndCast[*java.Block](v, forEach.Body, p))
	return forEach
}

func (v *GoVisitor) VisitForEachControl(control *java.ForEachControl, p any) java.J {
	prefix := v.self().VisitSpace(control.Prefix, p)
	markers := v.visitMarkers(control.Markers, p)
	varElem := visitAndCast[java.Statement](v, control.Variable.Element, p)
	varAfter := v.self().VisitSpace(control.Variable.After, p)
	iterElem := visitExpression(v, control.Iterable.Element, p)
	iterAfter := v.self().VisitSpace(control.Iterable.After, p)
	if java.SpaceEqual(prefix, control.Prefix) && java.MarkersEqual(markers, control.Markers) &&
		varElem == control.Variable.Element && java.SpaceEqual(varAfter, control.Variable.After) &&
		iterElem == control.Iterable.Element && java.SpaceEqual(iterAfter, control.Iterable.After) {
		return control
	}
	c := *control
	c.Prefix = prefix
	c.Markers = markers
	c.Variable.Element = varElem
	c.Variable.After = varAfter
	c.Iterable.Element = iterElem
	c.Iterable.After = iterAfter
	return &c
}

func (v *GoVisitor) VisitBreak(b *java.Break, p any) java.J {
	prefix := v.self().VisitSpace(b.Prefix, p)
	markers := v.visitMarkers(b.Markers, p)
	label := b.Label
	if label != nil {
		label = visitAndCast[*java.Identifier](v, label, p)
	}
	if java.SpaceEqual(prefix, b.Prefix) && java.MarkersEqual(markers, b.Markers) && label == b.Label {
		return b
	}
	c := *b
	c.Prefix = prefix
	c.Markers = markers
	c.Label = label
	return &c
}

func (v *GoVisitor) VisitContinue(cont *java.Continue, p any) java.J {
	prefix := v.self().VisitSpace(cont.Prefix, p)
	markers := v.visitMarkers(cont.Markers, p)
	label := cont.Label
	if label != nil {
		label = visitAndCast[*java.Identifier](v, label, p)
	}
	if java.SpaceEqual(prefix, cont.Prefix) && java.MarkersEqual(markers, cont.Markers) && label == cont.Label {
		return cont
	}
	c := *cont
	c.Prefix = prefix
	c.Markers = markers
	c.Label = label
	return &c
}

func (v *GoVisitor) VisitLabel(l *java.Label, p any) java.J {
	prefix := v.self().VisitSpace(l.Prefix, p)
	markers := v.visitMarkers(l.Markers, p)
	nameElem := visitAndCast[*java.Identifier](v, l.Name.Element, p)
	nameAfter := v.self().VisitSpace(l.Name.After, p)
	stmt := visitAndCast[java.Statement](v, l.Statement, p)
	if java.SpaceEqual(prefix, l.Prefix) && java.MarkersEqual(markers, l.Markers) &&
		nameElem == l.Name.Element && java.SpaceEqual(nameAfter, l.Name.After) && stmt == l.Statement {
		return l
	}
	c := *l
	c.Prefix = prefix
	c.Markers = markers
	c.Name.Element = nameElem
	c.Name.After = nameAfter
	c.Statement = stmt
	return &c
}

func (v *GoVisitor) VisitGoStmt(g *golang.GoStmt, p any) java.J {
	prefix := v.self().VisitSpace(g.Prefix, p)
	markers := v.visitMarkers(g.Markers, p)
	expr := visitExpression(v, g.Expr, p)
	if java.SpaceEqual(prefix, g.Prefix) && java.MarkersEqual(markers, g.Markers) && expr == g.Expr {
		return g
	}
	c := *g
	c.Prefix = prefix
	c.Markers = markers
	c.Expr = expr
	return &c
}

func (v *GoVisitor) VisitDefer(d *golang.Defer, p any) java.J {
	prefix := v.self().VisitSpace(d.Prefix, p)
	markers := v.visitMarkers(d.Markers, p)
	expr := visitExpression(v, d.Expr, p)
	if java.SpaceEqual(prefix, d.Prefix) && java.MarkersEqual(markers, d.Markers) && expr == d.Expr {
		return d
	}
	c := *d
	c.Prefix = prefix
	c.Markers = markers
	c.Expr = expr
	return &c
}

func (v *GoVisitor) VisitSend(s *golang.Send, p any) java.J {
	prefix := v.self().VisitSpace(s.Prefix, p)
	markers := v.visitMarkers(s.Markers, p)
	channel := visitExpression(v, s.Channel, p)
	arrowBefore := v.self().VisitSpace(s.Arrow.Before, p)
	arrowElem := visitExpression(v, s.Arrow.Element, p)
	if java.SpaceEqual(prefix, s.Prefix) && java.MarkersEqual(markers, s.Markers) &&
		channel == s.Channel &&
		java.SpaceEqual(arrowBefore, s.Arrow.Before) && arrowElem == s.Arrow.Element {
		return s
	}
	c := *s
	c.Prefix = prefix
	c.Markers = markers
	c.Channel = channel
	c.Arrow.Before = arrowBefore
	c.Arrow.Element = arrowElem
	return &c
}

func (v *GoVisitor) VisitGoto(g *golang.Goto, p any) java.J {
	prefix := v.self().VisitSpace(g.Prefix, p)
	markers := v.visitMarkers(g.Markers, p)
	label := g.Label
	if label != nil {
		label = visitAndCast[*java.Identifier](v, label, p)
	}
	if java.SpaceEqual(prefix, g.Prefix) && java.MarkersEqual(markers, g.Markers) && label == g.Label {
		return g
	}
	c := *g
	c.Prefix = prefix
	c.Markers = markers
	c.Label = label
	return &c
}

func (v *GoVisitor) VisitGoUnary(u *golang.Unary, p any) java.J {
	prefix := v.self().VisitSpace(u.Prefix, p)
	markers := v.visitMarkers(u.Markers, p)
	opBefore := v.self().VisitSpace(u.Operator.Before, p)
	expr := visitAndCast[java.Expression](v, u.Expression, p)
	if java.SpaceEqual(prefix, u.Prefix) && java.MarkersEqual(markers, u.Markers) &&
		java.SpaceEqual(opBefore, u.Operator.Before) && expr == u.Expression {
		return u
	}
	c := *u
	c.Prefix = prefix
	c.Markers = markers
	c.Operator.Before = opBefore
	c.Expression = expr
	return &c
}

func (v *GoVisitor) VisitGoBinary(b *golang.Binary, p any) java.J {
	prefix := v.self().VisitSpace(b.Prefix, p)
	markers := v.visitMarkers(b.Markers, p)
	left := visitAndCast[java.Expression](v, b.Left, p)
	opBefore := v.self().VisitSpace(b.Operator.Before, p)
	right := visitAndCast[java.Expression](v, b.Right, p)
	if java.SpaceEqual(prefix, b.Prefix) && java.MarkersEqual(markers, b.Markers) &&
		left == b.Left && java.SpaceEqual(opBefore, b.Operator.Before) && right == b.Right {
		return b
	}
	c := *b
	c.Prefix = prefix
	c.Markers = markers
	c.Left = left
	c.Operator.Before = opBefore
	c.Right = right
	return &c
}

func (v *GoVisitor) VisitGoAssignmentOperation(a *golang.AssignmentOperation, p any) java.J {
	prefix := v.self().VisitSpace(a.Prefix, p)
	markers := v.visitMarkers(a.Markers, p)
	variable := visitAndCast[java.Expression](v, a.Variable, p)
	opBefore := v.self().VisitSpace(a.Operator.Before, p)
	assignment := visitAndCast[java.Expression](v, a.Assignment, p)
	if java.SpaceEqual(prefix, a.Prefix) && java.MarkersEqual(markers, a.Markers) &&
		variable == a.Variable && java.SpaceEqual(opBefore, a.Operator.Before) && assignment == a.Assignment {
		return a
	}
	c := *a
	c.Prefix = prefix
	c.Markers = markers
	c.Variable = variable
	c.Operator.Before = opBefore
	c.Assignment = assignment
	return &c
}

func (v *GoVisitor) VisitGoVariadic(vr *golang.Variadic, p any) java.J {
	prefix := v.self().VisitSpace(vr.Prefix, p)
	markers := v.visitMarkers(vr.Markers, p)
	elem := visitAndCast[java.Expression](v, vr.Element, p)
	dots := v.self().VisitSpace(vr.Dots, p)
	if java.SpaceEqual(prefix, vr.Prefix) && java.MarkersEqual(markers, vr.Markers) &&
		elem == vr.Element && java.SpaceEqual(dots, vr.Dots) {
		return vr
	}
	c := *vr
	c.Prefix = prefix
	c.Markers = markers
	c.Element = elem
	c.Dots = dots
	return &c
}

func (v *GoVisitor) VisitFallthrough(f *golang.Fallthrough, p any) java.J {
	f = f.WithPrefix(v.self().VisitSpace(f.Prefix, p))
	f = f.WithMarkers(v.visitMarkers(f.Markers, p))
	return f
}

func (v *GoVisitor) VisitEmpty(empty *java.Empty, p any) java.J {
	empty = empty.WithPrefix(v.self().VisitSpace(empty.Prefix, p))
	return empty
}

func (v *GoVisitor) VisitAnnotation(ann *java.Annotation, p any) java.J {
	prefix := v.self().VisitSpace(ann.Prefix, p)
	markers := v.visitMarkers(ann.Markers, p)
	annType := ann.AnnotationType
	if annType != nil {
		annType = visitExpression(v, annType, p)
	}
	args := ann.Arguments
	if args != nil {
		before := v.self().VisitSpace(args.Before, p)
		argMarkers := v.visitMarkers(args.Markers, p)
		elems := visitRightPaddedExpressionList(v, args.Elements, p)
		if !java.SpaceEqual(before, args.Before) || !java.MarkersEqual(argMarkers, args.Markers) || !java.SameSlice(elems, args.Elements) {
			c := *args
			c.Before = before
			c.Markers = argMarkers
			c.Elements = elems
			args = &c
		}
	}
	if java.SpaceEqual(prefix, ann.Prefix) && java.MarkersEqual(markers, ann.Markers) &&
		annType == ann.AnnotationType && args == ann.Arguments {
		return ann
	}
	c := *ann
	c.Prefix = prefix
	c.Markers = markers
	c.AnnotationType = annType
	c.Arguments = args
	return &c
}

func (v *GoVisitor) VisitArrayType(at *java.ArrayType, p any) java.J {
	prefix := v.self().VisitSpace(at.Prefix, p)
	markers := v.visitMarkers(at.Markers, p)
	dimBefore := v.self().VisitSpace(at.Dimension.Before, p)
	dimElem := v.self().VisitSpace(at.Dimension.Element, p)
	elemType := visitExpression(v, at.ElementType, p)
	if java.SpaceEqual(prefix, at.Prefix) && java.MarkersEqual(markers, at.Markers) &&
		java.SpaceEqual(dimBefore, at.Dimension.Before) && java.SpaceEqual(dimElem, at.Dimension.Element) &&
		elemType == at.ElementType {
		return at
	}
	c := *at
	c.Prefix = prefix
	c.Markers = markers
	c.Dimension.Before = dimBefore
	c.Dimension.Element = dimElem
	c.ElementType = elemType
	return &c
}

func (v *GoVisitor) VisitGoArrayType(at *golang.ArrayType, p any) java.J {
	prefix := v.self().VisitSpace(at.Prefix, p)
	markers := v.visitMarkers(at.Markers, p)
	lenElem := visitExpression(v, at.Length.Element, p)
	lenAfter := v.self().VisitSpace(at.Length.After, p)
	elemType := visitExpression(v, at.ElementType, p)
	if java.SpaceEqual(prefix, at.Prefix) && java.MarkersEqual(markers, at.Markers) &&
		lenElem == at.Length.Element && java.SpaceEqual(lenAfter, at.Length.After) &&
		elemType == at.ElementType {
		return at
	}
	c := *at
	c.Prefix = prefix
	c.Markers = markers
	c.Length.Element = lenElem
	c.Length.After = lenAfter
	c.ElementType = elemType
	return &c
}

func (v *GoVisitor) VisitParentheses(paren *java.Parentheses, p any) java.J {
	prefix := v.self().VisitSpace(paren.Prefix, p)
	markers := v.visitMarkers(paren.Markers, p)
	treeElem := visitExpression(v, paren.Tree.Element, p)
	treeAfter := v.self().VisitSpace(paren.Tree.After, p)
	if java.SpaceEqual(prefix, paren.Prefix) && java.MarkersEqual(markers, paren.Markers) &&
		treeElem == paren.Tree.Element && java.SpaceEqual(treeAfter, paren.Tree.After) {
		return paren
	}
	c := *paren
	c.Prefix = prefix
	c.Markers = markers
	c.Tree.Element = treeElem
	c.Tree.After = treeAfter
	return &c
}

func (v *GoVisitor) VisitTypeCast(tc *java.TypeCast, p any) java.J {
	prefix := v.self().VisitSpace(tc.Prefix, p)
	markers := v.visitMarkers(tc.Markers, p)
	expr := visitExpression(v, tc.Expr, p)
	clazz := tc.Clazz
	if clazz != nil {
		clazz = visitAndCast[*java.ControlParentheses](v, clazz, p)
	}
	if java.SpaceEqual(prefix, tc.Prefix) && java.MarkersEqual(markers, tc.Markers) &&
		expr == tc.Expr && clazz == tc.Clazz {
		return tc
	}
	c := *tc
	c.Prefix = prefix
	c.Markers = markers
	c.Expr = expr
	c.Clazz = clazz
	return &c
}

func (v *GoVisitor) VisitControlParentheses(cp *java.ControlParentheses, p any) java.J {
	prefix := v.self().VisitSpace(cp.Prefix, p)
	markers := v.visitMarkers(cp.Markers, p)
	treeElem := visitExpression(v, cp.Tree.Element, p)
	treeAfter := v.self().VisitSpace(cp.Tree.After, p)
	if java.SpaceEqual(prefix, cp.Prefix) && java.MarkersEqual(markers, cp.Markers) &&
		treeElem == cp.Tree.Element && java.SpaceEqual(treeAfter, cp.Tree.After) {
		return cp
	}
	c := *cp
	c.Prefix = prefix
	c.Markers = markers
	c.Tree.Element = treeElem
	c.Tree.After = treeAfter
	return &c
}

func (v *GoVisitor) VisitArrayAccess(aa *java.ArrayAccess, p any) java.J {
	prefix := v.self().VisitSpace(aa.Prefix, p)
	markers := v.visitMarkers(aa.Markers, p)
	indexed := visitExpression(v, aa.Indexed, p)
	dim := aa.Dimension
	if dim != nil {
		dim = visitAndCast[*java.ArrayDimension](v, dim, p)
	}
	if java.SpaceEqual(prefix, aa.Prefix) && java.MarkersEqual(markers, aa.Markers) &&
		indexed == aa.Indexed && dim == aa.Dimension {
		return aa
	}
	c := *aa
	c.Prefix = prefix
	c.Markers = markers
	c.Indexed = indexed
	c.Dimension = dim
	return &c
}

func (v *GoVisitor) VisitParameterizedType(pt *java.ParameterizedType, p any) java.J {
	prefix := v.self().VisitSpace(pt.Prefix, p)
	markers := v.visitMarkers(pt.Markers, p)
	clazz := pt.Clazz
	if clazz != nil {
		clazz = visitExpression(v, clazz, p)
	}
	tps := pt.TypeParameters
	if tps != nil {
		before := v.self().VisitSpace(tps.Before, p)
		elems := visitRightPaddedList(v, tps.Elements, p)
		if !java.SpaceEqual(before, tps.Before) || !java.SameSlice(elems, tps.Elements) {
			c := *tps
			c.Before = before
			c.Elements = elems
			tps = &c
		}
	}
	if java.SpaceEqual(prefix, pt.Prefix) && java.MarkersEqual(markers, pt.Markers) &&
		clazz == pt.Clazz && tps == pt.TypeParameters {
		return pt
	}
	c := *pt
	c.Prefix = prefix
	c.Markers = markers
	c.Clazz = clazz
	c.TypeParameters = tps
	return &c
}

func (v *GoVisitor) VisitIndexList(il *golang.IndexList, p any) java.J {
	prefix := v.self().VisitSpace(il.Prefix, p)
	markers := v.visitMarkers(il.Markers, p)
	target := il.Target
	if target != nil {
		target = visitExpression(v, target, p)
	}
	indicesBefore := v.self().VisitSpace(il.Indices.Before, p)
	indicesElems := visitRightPaddedList(v, il.Indices.Elements, p)
	if java.SpaceEqual(prefix, il.Prefix) && java.MarkersEqual(markers, il.Markers) &&
		target == il.Target &&
		java.SpaceEqual(indicesBefore, il.Indices.Before) && java.SameSlice(indicesElems, il.Indices.Elements) {
		return il
	}
	c := *il
	c.Prefix = prefix
	c.Markers = markers
	c.Target = target
	c.Indices.Before = indicesBefore
	c.Indices.Elements = indicesElems
	return &c
}

func (v *GoVisitor) VisitArrayDimension(ad *java.ArrayDimension, p any) java.J {
	prefix := v.self().VisitSpace(ad.Prefix, p)
	markers := v.visitMarkers(ad.Markers, p)
	idxElem := visitExpression(v, ad.Index.Element, p)
	idxAfter := v.self().VisitSpace(ad.Index.After, p)
	if java.SpaceEqual(prefix, ad.Prefix) && java.MarkersEqual(markers, ad.Markers) &&
		idxElem == ad.Index.Element && java.SpaceEqual(idxAfter, ad.Index.After) {
		return ad
	}
	c := *ad
	c.Prefix = prefix
	c.Markers = markers
	c.Index.Element = idxElem
	c.Index.After = idxAfter
	return &c
}

func (v *GoVisitor) VisitComposite(comp *golang.Composite, p any) java.J {
	prefix := v.self().VisitSpace(comp.Prefix, p)
	markers := v.visitMarkers(comp.Markers, p)
	typeExpr := comp.TypeExpr
	if typeExpr != nil {
		typeExpr = visitExpression(v, typeExpr, p)
	}
	elemsBefore := v.self().VisitSpace(comp.Elements.Before, p)
	elemsElems := visitRightPaddedList(v, comp.Elements.Elements, p)
	if java.SpaceEqual(prefix, comp.Prefix) && java.MarkersEqual(markers, comp.Markers) &&
		typeExpr == comp.TypeExpr &&
		java.SpaceEqual(elemsBefore, comp.Elements.Before) && java.SameSlice(elemsElems, comp.Elements.Elements) {
		return comp
	}
	c := *comp
	c.Prefix = prefix
	c.Markers = markers
	c.TypeExpr = typeExpr
	c.Elements.Before = elemsBefore
	c.Elements.Elements = elemsElems
	return &c
}

func (v *GoVisitor) VisitKeyValue(kv *golang.KeyValue, p any) java.J {
	prefix := v.self().VisitSpace(kv.Prefix, p)
	markers := v.visitMarkers(kv.Markers, p)
	key := visitExpression(v, kv.Key, p)
	valueBefore := v.self().VisitSpace(kv.Value.Before, p)
	valueElem := visitExpression(v, kv.Value.Element, p)
	if java.SpaceEqual(prefix, kv.Prefix) && java.MarkersEqual(markers, kv.Markers) &&
		key == kv.Key &&
		java.SpaceEqual(valueBefore, kv.Value.Before) && valueElem == kv.Value.Element {
		return kv
	}
	c := *kv
	c.Prefix = prefix
	c.Markers = markers
	c.Key = key
	c.Value.Before = valueBefore
	c.Value.Element = valueElem
	return &c
}

func (v *GoVisitor) VisitSlice(s *golang.Slice, p any) java.J {
	prefix := v.self().VisitSpace(s.Prefix, p)
	markers := v.visitMarkers(s.Markers, p)
	indexed := visitExpression(v, s.Indexed, p)
	openBracket := v.self().VisitSpace(s.OpenBracket, p)
	lowElem := visitExpression(v, s.Low.Element, p)
	lowAfter := v.self().VisitSpace(s.Low.After, p)
	highElem := visitExpression(v, s.High.Element, p)
	highAfter := v.self().VisitSpace(s.High.After, p)
	max := s.Max
	if max != nil {
		max = visitExpression(v, max, p)
	}
	closeBracket := v.self().VisitSpace(s.CloseBracket, p)
	if java.SpaceEqual(prefix, s.Prefix) && java.MarkersEqual(markers, s.Markers) &&
		indexed == s.Indexed && java.SpaceEqual(openBracket, s.OpenBracket) &&
		lowElem == s.Low.Element && java.SpaceEqual(lowAfter, s.Low.After) &&
		highElem == s.High.Element && java.SpaceEqual(highAfter, s.High.After) &&
		max == s.Max && java.SpaceEqual(closeBracket, s.CloseBracket) {
		return s
	}
	c := *s
	c.Prefix = prefix
	c.Markers = markers
	c.Indexed = indexed
	c.OpenBracket = openBracket
	c.Low.Element = lowElem
	c.Low.After = lowAfter
	c.High.Element = highElem
	c.High.After = highAfter
	c.Max = max
	c.CloseBracket = closeBracket
	return &c
}

func (v *GoVisitor) VisitMapType(mt *golang.MapType, p any) java.J {
	prefix := v.self().VisitSpace(mt.Prefix, p)
	markers := v.visitMarkers(mt.Markers, p)
	openBracket := v.self().VisitSpace(mt.OpenBracket, p)
	keyElem := visitExpression(v, mt.Key.Element, p)
	keyAfter := v.self().VisitSpace(mt.Key.After, p)
	value := visitExpression(v, mt.Value, p)
	if java.SpaceEqual(prefix, mt.Prefix) && java.MarkersEqual(markers, mt.Markers) &&
		java.SpaceEqual(openBracket, mt.OpenBracket) &&
		keyElem == mt.Key.Element && java.SpaceEqual(keyAfter, mt.Key.After) &&
		value == mt.Value {
		return mt
	}
	c := *mt
	c.Prefix = prefix
	c.Markers = markers
	c.OpenBracket = openBracket
	c.Key.Element = keyElem
	c.Key.After = keyAfter
	c.Value = value
	return &c
}

func (v *GoVisitor) VisitStatementExpression(se *golang.StatementExpression, p any) java.J {
	prefix := v.self().VisitSpace(se.Prefix, p)
	markers := v.visitMarkers(se.Markers, p)
	stmt := se.Statement
	if result, ok := v.self().Visit(se.Statement, p).(java.Statement); ok {
		stmt = result
	}
	if java.SpaceEqual(prefix, se.Prefix) && java.MarkersEqual(markers, se.Markers) && stmt == se.Statement {
		return se
	}
	c := *se
	c.Prefix = prefix
	c.Markers = markers
	c.Statement = stmt
	return &c
}

func (v *GoVisitor) VisitPointerType(pt *golang.PointerType, p any) java.J {
	prefix := v.self().VisitSpace(pt.Prefix, p)
	markers := v.visitMarkers(pt.Markers, p)
	elem := visitExpression(v, pt.Elem, p)
	if java.SpaceEqual(prefix, pt.Prefix) && java.MarkersEqual(markers, pt.Markers) && elem == pt.Elem {
		return pt
	}
	c := *pt
	c.Prefix = prefix
	c.Markers = markers
	c.Elem = elem
	return &c
}

func (v *GoVisitor) VisitChannel(ch *golang.Channel, p any) java.J {
	prefix := v.self().VisitSpace(ch.Prefix, p)
	markers := v.visitMarkers(ch.Markers, p)
	value := visitExpression(v, ch.Value, p)
	if java.SpaceEqual(prefix, ch.Prefix) && java.MarkersEqual(markers, ch.Markers) && value == ch.Value {
		return ch
	}
	c := *ch
	c.Prefix = prefix
	c.Markers = markers
	c.Value = value
	return &c
}

func (v *GoVisitor) VisitFuncType(ft *golang.FuncType, p any) java.J {
	prefix := v.self().VisitSpace(ft.Prefix, p)
	markers := v.visitMarkers(ft.Markers, p)
	paramsBefore := v.self().VisitSpace(ft.Parameters.Before, p)
	paramsElems := visitRightPaddedList(v, ft.Parameters.Elements, p)
	returnType := ft.ReturnType
	if returnType != nil {
		returnType = visitExpression(v, returnType, p)
	}
	if java.SpaceEqual(prefix, ft.Prefix) && java.MarkersEqual(markers, ft.Markers) &&
		java.SpaceEqual(paramsBefore, ft.Parameters.Before) && java.SameSlice(paramsElems, ft.Parameters.Elements) &&
		returnType == ft.ReturnType {
		return ft
	}
	c := *ft
	c.Prefix = prefix
	c.Markers = markers
	c.Parameters.Before = paramsBefore
	c.Parameters.Elements = paramsElems
	c.ReturnType = returnType
	return &c
}

func (v *GoVisitor) VisitTypeList(tl *golang.TypeList, p any) java.J {
	prefix := v.self().VisitSpace(tl.Prefix, p)
	markers := v.visitMarkers(tl.Markers, p)
	typesBefore := v.self().VisitSpace(tl.Types.Before, p)
	typesElems := visitRightPaddedList(v, tl.Types.Elements, p)
	if java.SpaceEqual(prefix, tl.Prefix) && java.MarkersEqual(markers, tl.Markers) &&
		java.SpaceEqual(typesBefore, tl.Types.Before) && java.SameSlice(typesElems, tl.Types.Elements) {
		return tl
	}
	c := *tl
	c.Prefix = prefix
	c.Markers = markers
	c.Types.Before = typesBefore
	c.Types.Elements = typesElems
	return &c
}

func (v *GoVisitor) VisitUnion(u *golang.Union, p any) java.J {
	prefix := v.self().VisitSpace(u.Prefix, p)
	markers := v.visitMarkers(u.Markers, p)
	types := visitRightPaddedExpressionList(v, u.Types, p)
	if java.SpaceEqual(prefix, u.Prefix) && java.MarkersEqual(markers, u.Markers) && java.SameSlice(types, u.Types) {
		return u
	}
	c := *u
	c.Prefix = prefix
	c.Markers = markers
	c.Types = types
	return &c
}

func (v *GoVisitor) VisitUnderlyingType(ut *golang.UnderlyingType, p any) java.J {
	prefix := v.self().VisitSpace(ut.Prefix, p)
	markers := v.visitMarkers(ut.Markers, p)
	elem := visitExpression(v, ut.Element, p)
	if java.SpaceEqual(prefix, ut.Prefix) && java.MarkersEqual(markers, ut.Markers) && elem == ut.Element {
		return ut
	}
	c := *ut
	c.Prefix = prefix
	c.Markers = markers
	c.Element = elem
	return &c
}

func (v *GoVisitor) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	prefix := v.self().VisitSpace(td.Prefix, p)
	markers := v.visitMarkers(td.Markers, p)
	anns := visitAnnotationList(v, td.LeadingAnnotations, p)
	name := td.Name
	if name != nil {
		name = visitAndCast[*java.Identifier](v, name, p)
	}
	tps := td.TypeParameters
	if tps != nil {
		tps = visitAndCast[*java.TypeParameters](v, tps, p)
	}
	assign := td.Assign
	if assign != nil {
		before := v.self().VisitSpace(assign.Before, p)
		if !java.SpaceEqual(before, assign.Before) {
			c := *assign
			c.Before = before
			assign = &c
		}
	}
	definition := td.Definition
	if definition != nil {
		definition = visitExpression(v, definition, p)
	}
	specs := td.Specs
	if specs != nil {
		before := v.self().VisitSpace(specs.Before, p)
		elems := visitRightPaddedList(v, specs.Elements, p)
		if !java.SpaceEqual(before, specs.Before) || !java.SameSlice(elems, specs.Elements) {
			c := *specs
			c.Before = before
			c.Elements = elems
			specs = &c
		}
	}
	if java.SpaceEqual(prefix, td.Prefix) && java.MarkersEqual(markers, td.Markers) &&
		java.SameSlice(anns, td.LeadingAnnotations) && name == td.Name && tps == td.TypeParameters &&
		assign == td.Assign && definition == td.Definition && specs == td.Specs {
		return td
	}
	c := *td
	c.Prefix = prefix
	c.Markers = markers
	c.LeadingAnnotations = anns
	c.Name = name
	c.TypeParameters = tps
	c.Assign = assign
	c.Definition = definition
	c.Specs = specs
	return &c
}

func (v *GoVisitor) VisitStructType(st *golang.StructType, p any) java.J {
	prefix := v.self().VisitSpace(st.Prefix, p)
	markers := v.visitMarkers(st.Markers, p)
	body := st.Body
	if body != nil {
		body = visitAndCast[*java.Block](v, body, p)
	}
	if java.SpaceEqual(prefix, st.Prefix) && java.MarkersEqual(markers, st.Markers) && body == st.Body {
		return st
	}
	c := *st
	c.Prefix = prefix
	c.Markers = markers
	c.Body = body
	return &c
}

func (v *GoVisitor) VisitInterfaceType(it *golang.InterfaceType, p any) java.J {
	prefix := v.self().VisitSpace(it.Prefix, p)
	markers := v.visitMarkers(it.Markers, p)
	body := it.Body
	if body != nil {
		body = visitAndCast[*java.Block](v, body, p)
	}
	if java.SpaceEqual(prefix, it.Prefix) && java.MarkersEqual(markers, it.Markers) && body == it.Body {
		return it
	}
	c := *it
	c.Prefix = prefix
	c.Markers = markers
	c.Body = body
	return &c
}

func (v *GoVisitor) VisitMultiAssignment(ma *golang.MultiAssignment, p any) java.J {
	prefix := v.self().VisitSpace(ma.Prefix, p)
	markers := v.visitMarkers(ma.Markers, p)
	variables := visitRightPaddedExpressionList(v, ma.Variables, p)
	opBefore := v.self().VisitSpace(ma.Operator.Before, p)
	values := visitRightPaddedExpressionList(v, ma.Values, p)
	if java.SpaceEqual(prefix, ma.Prefix) && java.MarkersEqual(markers, ma.Markers) &&
		java.SameSlice(variables, ma.Variables) && java.SpaceEqual(opBefore, ma.Operator.Before) &&
		java.SameSlice(values, ma.Values) {
		return ma
	}
	c := *ma
	c.Prefix = prefix
	c.Markers = markers
	c.Variables = variables
	c.Operator.Before = opBefore
	c.Values = values
	return &c
}

func (v *GoVisitor) VisitCommClause(cc *golang.CommClause, p any) java.J {
	prefix := v.self().VisitSpace(cc.Prefix, p)
	markers := v.visitMarkers(cc.Markers, p)
	comm := cc.Comm
	if comm != nil {
		comm = visitAndCast[java.Statement](v, comm, p)
	}
	colon := v.self().VisitSpace(cc.Colon, p)
	body := visitRightPaddedList(v, cc.Body, p)
	if java.SpaceEqual(prefix, cc.Prefix) && java.MarkersEqual(markers, cc.Markers) &&
		comm == cc.Comm && java.SpaceEqual(colon, cc.Colon) && java.SameSlice(body, cc.Body) {
		return cc
	}
	c := *cc
	c.Prefix = prefix
	c.Markers = markers
	c.Comm = comm
	c.Colon = colon
	c.Body = body
	return &c
}

func (v *GoVisitor) VisitSpace(space java.Space, p any) java.Space {
	return space
}

func (v *GoVisitor) VisitType(javaType java.JavaType, p any) java.JavaType {
	return javaType
}

// VisitMarker is the per-marker dispatch seam, mirroring
// TreeVisitor.visitMarker in rewrite-java (and visit_marker / visitMarker in
// the Python, JS, and C# ports). Every Visit* method routes its node's
// markers through visitMarkers, which invokes VisitMarker on each entry via
// virtual dispatch, so subclasses collect or rewrite markers without
// touching individual node types. The default returns the marker unchanged.
func (v *GoVisitor) VisitMarker(marker java.Marker, p any) java.Marker {
	return marker
}

// visitMarkers maps VisitMarker over every entry. Nodes carrying no markers
// (the vast majority) short-circuit with no allocation; marker-bearing nodes
// rebuild the slice. We can't skip the rebuild via an == comparison because
// marker types may be uncomparable (they hold slices/maps), which would
// panic — and it would buy nothing anyway, since WithMarkers already
// reallocates the node regardless of marker identity.
func (v *GoVisitor) visitMarkers(markers java.Markers, p any) java.Markers {
	if len(markers.Entries) == 0 {
		return markers
	}
	// Preserve identity: return the same Markers (same entries slice) when no
	// entry changed, so the enclosing withX guard (java.MarkersEqual) reports
	// no change. Only allocate once VisitMarker actually rewrites an entry.
	var entries []java.Marker
	for i, m := range markers.Entries {
		visited := v.self().VisitMarker(m, p)
		if entries == nil && !sameMarker(visited, m) {
			entries = make([]java.Marker, len(markers.Entries))
			copy(entries, markers.Entries[:i])
		}
		if entries != nil {
			entries[i] = visited
		}
	}
	if entries == nil {
		return markers
	}
	return java.Markers{ID: markers.ID, Entries: entries}
}

// sameMarker reports whether VisitMarker left an entry unchanged, without an
// interface == (which panics on value-typed markers holding slices/maps, e.g.
// golang.GroupedImport). Pointer markers compare by identity; value markers
// fall back to a deep compare.
func sameMarker(a, b java.Marker) bool {
	if a == nil || b == nil {
		return a == nil && b == nil
	}
	ra, rb := reflect.ValueOf(a), reflect.ValueOf(b)
	if ra.Kind() == reflect.Ptr && rb.Kind() == reflect.Ptr {
		return ra.Pointer() == rb.Pointer()
	}
	return reflect.DeepEqual(a, b)
}

func visitAndCast[T java.Tree](v *GoVisitor, t java.Tree, p any) T {
	result := v.self().Visit(t, p)
	if result == nil {
		var zero T
		return zero
	}
	return result.(T)
}

// visitAnnotationList visits a slice of annotations, preserving slice identity:
// it returns the original slice when no annotation changed (and none deleted),
// allocating only on the first change. Mirrors visitRightPaddedList's idiom.
func visitAnnotationList(v *GoVisitor, list []*java.Annotation, p any) []*java.Annotation {
	var result []*java.Annotation
	materialize := func(i int) {
		if result == nil {
			result = make([]*java.Annotation, 0, len(list))
			result = append(result, list[:i]...)
		}
	}
	for i := range list {
		a := list[i]
		visited := v.self().Visit(a, p)
		if visited == nil {
			materialize(i)
			continue
		}
		na := visited.(*java.Annotation)
		if na == a {
			if result != nil {
				result = append(result, na)
			}
			continue
		}
		materialize(i)
		result = append(result, na)
	}
	if result == nil {
		return list
	}
	return result
}

// visitGoModValueList visits a slice of go.mod values, preserving slice identity:
// it returns the original slice when nothing changed, allocating only on the
// first change/deletion.
func visitGoModValueList(v *GoVisitor, list []*golang.GoModValue, p any) []*golang.GoModValue {
	var result []*golang.GoModValue
	materialize := func(i int) {
		if result == nil {
			result = make([]*golang.GoModValue, 0, len(list))
			result = append(result, list[:i]...)
		}
	}
	for i := range list {
		val := list[i]
		visited := v.self().Visit(val, p)
		if visited == nil {
			materialize(i)
			continue
		}
		nv := visited.(*golang.GoModValue)
		if nv == val {
			if result != nil {
				result = append(result, nv)
			}
			continue
		}
		materialize(i)
		result = append(result, nv)
	}
	if result == nil {
		return list
	}
	return result
}

// visitGoModStatementList visits a right-padded list of go.mod statements.
// GoModStatement is a java.Tree (not java.J), so the J-constrained
// visitRightPaddedList helper can't be reused here.
func visitGoModStatementList(v *GoVisitor, list []java.RightPadded[golang.GoModStatement], p any) []java.RightPadded[golang.GoModStatement] {
	var result []java.RightPadded[golang.GoModStatement]
	materialize := func(i int) {
		if result == nil {
			result = make([]java.RightPadded[golang.GoModStatement], 0, len(list))
			result = append(result, list[:i]...)
		}
	}
	for i := range list {
		rp := list[i]
		visited := v.self().Visit(rp.Element, p)
		newAfter := v.self().VisitSpace(rp.After, p)
		if visited == nil {
			materialize(i)
			continue
		}
		ne := visited.(golang.GoModStatement)
		if ne == rp.Element && java.SpaceEqual(newAfter, rp.After) {
			if result != nil {
				result = append(result, rp)
			}
			continue
		}
		materialize(i)
		rp.Element = ne
		rp.After = newAfter
		result = append(result, rp)
	}
	if result == nil {
		return list
	}
	return result
}

func visitGoSumLineList(v *GoVisitor, list []java.RightPadded[*golang.GoSumLine], p any) []java.RightPadded[*golang.GoSumLine] {
	result := make([]java.RightPadded[*golang.GoSumLine], 0, len(list))
	for _, rp := range list {
		visited := v.self().Visit(rp.Element, p)
		if visited == nil {
			continue
		}
		rp.Element = visited.(*golang.GoSumLine)
		rp.After = v.self().VisitSpace(rp.After, p)
		result = append(result, rp)
	}
	return result
}

func visitExpression(v *GoVisitor, expr java.Expression, p any) java.Expression {
	result := v.self().Visit(expr, p)
	if result == nil {
		return nil
	}
	return result.(java.Expression)
}

func visitRightPaddedExpressionList(v *GoVisitor, list []java.RightPadded[java.Expression], p any) []java.RightPadded[java.Expression] {
	var result []java.RightPadded[java.Expression]
	materialize := func(i int) {
		if result == nil {
			result = make([]java.RightPadded[java.Expression], 0, len(list))
			result = append(result, list[:i]...)
		}
	}
	for i := range list {
		rp := list[i]
		visited := v.self().Visit(rp.Element, p)
		newAfter := v.self().VisitSpace(rp.After, p)
		if visited == nil {
			materialize(i)
			continue
		}
		ne := visited.(java.Expression)
		if ne == rp.Element && java.SpaceEqual(newAfter, rp.After) {
			if result != nil {
				result = append(result, rp)
			}
			continue
		}
		materialize(i)
		rp.Element = ne
		rp.After = newAfter
		result = append(result, rp)
	}
	if result == nil {
		return list
	}
	return result
}

func visitRightPaddedList[T java.J](v *GoVisitor, list []java.RightPadded[T], p any) []java.RightPadded[T] {
	// Preserve slice identity: return the original `list` when no element
	// changed, so the enclosing withX guard (java.SameSlice) returns the same
	// node. Only allocate once the first change (mutation/deletion) is seen.
	var result []java.RightPadded[T]
	materialize := func(i int) {
		if result == nil {
			result = make([]java.RightPadded[T], 0, len(list))
			result = append(result, list[:i]...)
		}
	}
	for i := range list {
		rp := list[i]
		visited := v.self().Visit(rp.Element, p)
		newAfter := v.self().VisitSpace(rp.After, p)
		if visited == nil {
			materialize(i)
			continue
		}
		ne := visited.(T)
		if any(ne) == any(rp.Element) && java.SpaceEqual(newAfter, rp.After) {
			if result != nil {
				result = append(result, rp)
			}
			continue
		}
		materialize(i)
		rp.Element = ne
		rp.After = newAfter
		result = append(result, rp)
	}
	if result == nil {
		return list
	}
	return result
}
