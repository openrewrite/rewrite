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
	VisitCommClause(cc *golang.CommClause, p any) java.J
	VisitGoUnary(u *golang.Unary, p any) java.J
	VisitGoBinary(b *golang.Binary, p any) java.J
	VisitGoAssignmentOperation(a *golang.AssignmentOperation, p any) java.J
	VisitGoVariadic(v *golang.Variadic, p any) java.J
	VisitSpace(space java.Space, p any) java.Space
	VisitType(javaType java.JavaType, p any) java.JavaType
}

// Ensure GoVisitor satisfies VisitorI.
var _ VisitorI = (*GoVisitor)(nil)

// --- Default visit implementations ---

// PreVisit is the per-node hook called by Visit() before dispatching
// to the type-specific Visit* method. The default implementation is
// the identity function. RPC senders/receivers override it to
// serialize/deserialize the cross-cutting `id`, `prefix`, and
// `markers` fields once per node, mirroring Java's
// JavaVisitor.preVisit pattern.
func (v *GoVisitor) PreVisit(t java.Tree, p any) java.Tree { return t }

func (v *GoVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = cu.WithPrefix(v.self().VisitSpace(cu.Prefix, p))
	cu = cu.WithMarkers(v.visitMarkers(cu.Markers, p))
	if cu.PackageDecl != nil {
		pkg := *cu.PackageDecl
		pkg.Element = visitAndCast[*java.Identifier](v, pkg.Element, p)
		pkg.After = v.self().VisitSpace(pkg.After, p)
		cu = cu.WithPackageDecl(&pkg)
	}
	if cu.Imports != nil {
		imports := *cu.Imports
		imports.Before = v.self().VisitSpace(imports.Before, p)
		imports.Markers = v.visitMarkers(imports.Markers, p)
		imports.Elements = visitRightPaddedList(v, imports.Elements, p)
		cu = cu.WithImports(&imports)
	}
	cu = cu.WithStatements(visitRightPaddedList(v, cu.Statements, p))
	cu = cu.WithEOF(v.self().VisitSpace(cu.EOF, p))
	return cu
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
	ret = ret.WithPrefix(v.self().VisitSpace(ret.Prefix, p))
	ret = ret.WithMarkers(v.visitMarkers(ret.Markers, p))
	ret.Expressions = visitRightPaddedExpressionList(v, ret.Expressions, p)
	return ret
}

func (v *GoVisitor) VisitIf(ifStmt *java.If, p any) java.J {
	ifStmt = ifStmt.WithPrefix(v.self().VisitSpace(ifStmt.Prefix, p))
	ifStmt = ifStmt.WithMarkers(v.visitMarkers(ifStmt.Markers, p))
	if ifStmt.Init != nil {
		init := *ifStmt.Init
		init.Element = v.self().Visit(init.Element, p).(java.Statement)
		init.After = v.self().VisitSpace(init.After, p)
		ifStmt.Init = &init
	}
	ifStmt = ifStmt.WithCondition(visitExpression(v, ifStmt.Condition, p))
	ifStmt = ifStmt.WithThen(visitAndCast[*java.Block](v, ifStmt.Then, p))
	if ifStmt.ElsePart != nil {
		ep := *ifStmt.ElsePart
		ep.Element = v.self().Visit(ep.Element, p).(java.J)
		ep.After = v.self().VisitSpace(ep.After, p)
		ifStmt.ElsePart = &ep
	}
	return ifStmt
}

// VisitElse handles the synthetic *java.Else wrapper that JavaSender produces
// for RPC parity with Java's J.If.Else. The node never appears in a parsed
// Go AST — language-level recipes go through VisitIf instead — so the default
// implementation simply visits the body so traversal terminates correctly.
func (v *GoVisitor) VisitElse(el *java.Else, p any) java.J {
	el = el.WithPrefix(v.self().VisitSpace(el.Prefix, p))
	el = el.WithMarkers(v.visitMarkers(el.Markers, p))
	body := el.Body
	body.Element = v.self().Visit(body.Element, p).(java.Statement)
	body.After = v.self().VisitSpace(body.After, p)
	el.Body = body
	return el
}

func (v *GoVisitor) VisitAssignment(assign *java.Assignment, p any) java.J {
	assign = assign.WithPrefix(v.self().VisitSpace(assign.Prefix, p))
	assign = assign.WithMarkers(v.visitMarkers(assign.Markers, p))
	assign = assign.WithVariable(visitExpression(v, assign.Variable, p))
	assign.Value.Before = v.self().VisitSpace(assign.Value.Before, p)
	assign.Value.Element = visitExpression(v, assign.Value.Element, p)
	return assign
}

func (v *GoVisitor) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	md = md.WithPrefix(v.self().VisitSpace(md.Prefix, p))
	md = md.WithMarkers(v.visitMarkers(md.Markers, p))
	if len(md.LeadingAnnotations) > 0 {
		anns := make([]*java.Annotation, 0, len(md.LeadingAnnotations))
		for _, a := range md.LeadingAnnotations {
			visited := v.self().Visit(a, p)
			if visited == nil {
				continue
			}
			anns = append(anns, visited.(*java.Annotation))
		}
		md = md.WithLeadingAnnotations(anns)
	}
	md = md.WithName(visitAndCast[*java.Identifier](v, md.Name, p))
	if md.TypeParameters != nil {
		md = md.WithTypeParameters(visitAndCast[*java.TypeParameters](v, md.TypeParameters, p))
	}
	if md.Body != nil {
		md = md.WithBody(visitAndCast[*java.Block](v, md.Body, p))
	}
	return md
}

func (v *GoVisitor) VisitTypeParameters(tps *java.TypeParameters, p any) java.J {
	tps = tps.WithPrefix(v.self().VisitSpace(tps.Prefix, p))
	tps = tps.WithMarkers(v.visitMarkers(tps.Markers, p))
	tps.TypeParameters = visitRightPaddedList(v, tps.TypeParameters, p)
	return tps
}

func (v *GoVisitor) VisitTypeParameter(tp *java.TypeParameter, p any) java.J {
	tp = tp.WithPrefix(v.self().VisitSpace(tp.Prefix, p))
	tp = tp.WithMarkers(v.visitMarkers(tp.Markers, p))
	if tp.Name != nil {
		tp.Name = visitExpression(v, tp.Name, p)
	}
	if tp.Bounds != nil {
		tp.Bounds.Before = v.self().VisitSpace(tp.Bounds.Before, p)
		tp.Bounds.Elements = visitRightPaddedList(v, tp.Bounds.Elements, p)
	}
	return tp
}

func (v *GoVisitor) VisitFieldAccess(fa *java.FieldAccess, p any) java.J {
	fa = fa.WithPrefix(v.self().VisitSpace(fa.Prefix, p))
	fa = fa.WithMarkers(v.visitMarkers(fa.Markers, p))
	fa = fa.WithTarget(visitExpression(v, fa.Target, p))
	// Visit the selector identifier so recipes that traverse identifiers
	// see the right-hand side of `target.Name` (e.g. the `Box` in
	// `a.Box[int]{...}`). Mirrors JavaIsoVisitor.visitFieldAccess.
	name := fa.Name
	name.Before = v.self().VisitSpace(name.Before, p)
	name.Element = visitAndCast[*java.Identifier](v, name.Element, p)
	fa.Name = name
	return fa
}

func (v *GoVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	mi = mi.WithPrefix(v.self().VisitSpace(mi.Prefix, p))
	mi = mi.WithMarkers(v.visitMarkers(mi.Markers, p))
	if mi.Select != nil {
		sel := *mi.Select
		sel.Element = visitExpression(v, sel.Element, p)
		sel.After = v.self().VisitSpace(sel.After, p)
		mi.Select = &sel
	}
	mi = mi.WithName(visitAndCast[*java.Identifier](v, mi.Name, p))
	mi.Arguments.Before = v.self().VisitSpace(mi.Arguments.Before, p)
	mi.Arguments.Elements = visitRightPaddedList(v, mi.Arguments.Elements, p)
	return mi
}

func (v *GoVisitor) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	vd = vd.WithPrefix(v.self().VisitSpace(vd.Prefix, p))
	vd = vd.WithMarkers(v.visitMarkers(vd.Markers, p))
	if len(vd.LeadingAnnotations) > 0 {
		anns := make([]*java.Annotation, 0, len(vd.LeadingAnnotations))
		for _, a := range vd.LeadingAnnotations {
			visited := v.self().Visit(a, p)
			if visited == nil {
				continue
			}
			anns = append(anns, visited.(*java.Annotation))
		}
		vd = vd.WithLeadingAnnotations(anns)
	}
	if vd.TypeExpr != nil {
		vd.TypeExpr = visitExpression(v, vd.TypeExpr, p)
	}
	vd.Variables = visitRightPaddedList(v, vd.Variables, p)
	if vd.Specs != nil {
		specs := *vd.Specs
		specs.Before = v.self().VisitSpace(specs.Before, p)
		specs.Elements = visitRightPaddedList(v, specs.Elements, p)
		vd.Specs = &specs
	}
	return vd
}

func (v *GoVisitor) VisitVariableDeclarator(vd *java.VariableDeclarator, p any) java.J {
	vd = vd.WithPrefix(v.self().VisitSpace(vd.Prefix, p))
	vd = vd.WithMarkers(v.visitMarkers(vd.Markers, p))
	vd = vd.WithName(visitAndCast[*java.Identifier](v, vd.Name, p))
	if vd.Initializer != nil {
		init := *vd.Initializer
		init.Before = v.self().VisitSpace(init.Before, p)
		init.Element = visitExpression(v, init.Element, p)
		vd.Initializer = &init
	}
	return vd
}

func (v *GoVisitor) VisitImport(imp *java.Import, p any) java.J {
	imp = imp.WithPrefix(v.self().VisitSpace(imp.Prefix, p))
	imp = imp.WithMarkers(v.visitMarkers(imp.Markers, p))
	return imp
}

func (v *GoVisitor) VisitUnary(unary *java.Unary, p any) java.J {
	unary = unary.WithPrefix(v.self().VisitSpace(unary.Prefix, p))
	unary = unary.WithMarkers(v.visitMarkers(unary.Markers, p))
	unary = unary.WithOperand(visitExpression(v, unary.Operand, p))
	return unary
}

func (v *GoVisitor) VisitAssignmentOperation(ao *java.AssignmentOperation, p any) java.J {
	ao = ao.WithPrefix(v.self().VisitSpace(ao.Prefix, p))
	ao = ao.WithMarkers(v.visitMarkers(ao.Markers, p))
	ao = ao.WithVariable(visitExpression(v, ao.Variable, p))
	ao.Assignment = visitExpression(v, ao.Assignment, p)
	return ao
}

func (v *GoVisitor) VisitSwitch(sw *java.Switch, p any) java.J {
	sw = sw.WithPrefix(v.self().VisitSpace(sw.Prefix, p))
	sw = sw.WithMarkers(v.visitMarkers(sw.Markers, p))
	sw = sw.WithBody(visitAndCast[*java.Block](v, sw.Body, p))
	return sw
}

func (v *GoVisitor) VisitCase(c *java.Case, p any) java.J {
	c = c.WithPrefix(v.self().VisitSpace(c.Prefix, p))
	c = c.WithMarkers(v.visitMarkers(c.Markers, p))
	return c
}

func (v *GoVisitor) VisitForLoop(forLoop *java.ForLoop, p any) java.J {
	forLoop = forLoop.WithPrefix(v.self().VisitSpace(forLoop.Prefix, p))
	forLoop = forLoop.WithMarkers(v.visitMarkers(forLoop.Markers, p))
	forLoop.Control = *visitAndCast[*java.ForControl](v, &forLoop.Control, p)
	forLoop = forLoop.WithBody(visitAndCast[*java.Block](v, forLoop.Body, p))
	return forLoop
}

func (v *GoVisitor) VisitForControl(control *java.ForControl, p any) java.J {
	control = control.WithPrefix(v.self().VisitSpace(control.Prefix, p))
	control = control.WithMarkers(v.visitMarkers(control.Markers, p))
	return control
}

func (v *GoVisitor) VisitForEachLoop(forEach *java.ForEachLoop, p any) java.J {
	forEach = forEach.WithPrefix(v.self().VisitSpace(forEach.Prefix, p))
	forEach = forEach.WithMarkers(v.visitMarkers(forEach.Markers, p))
	forEach = forEach.WithBody(visitAndCast[*java.Block](v, forEach.Body, p))
	return forEach
}

func (v *GoVisitor) VisitForEachControl(control *java.ForEachControl, p any) java.J {
	control = control.WithPrefix(v.self().VisitSpace(control.Prefix, p))
	control = control.WithMarkers(v.visitMarkers(control.Markers, p))
	return control
}

func (v *GoVisitor) VisitBreak(b *java.Break, p any) java.J {
	b = b.WithPrefix(v.self().VisitSpace(b.Prefix, p))
	b = b.WithMarkers(v.visitMarkers(b.Markers, p))
	return b
}

func (v *GoVisitor) VisitContinue(c *java.Continue, p any) java.J {
	c = c.WithPrefix(v.self().VisitSpace(c.Prefix, p))
	c = c.WithMarkers(v.visitMarkers(c.Markers, p))
	return c
}

func (v *GoVisitor) VisitLabel(l *java.Label, p any) java.J {
	l = l.WithPrefix(v.self().VisitSpace(l.Prefix, p))
	l = l.WithMarkers(v.visitMarkers(l.Markers, p))
	return l
}

func (v *GoVisitor) VisitGoStmt(g *golang.GoStmt, p any) java.J {
	g = g.WithPrefix(v.self().VisitSpace(g.Prefix, p))
	g = g.WithMarkers(v.visitMarkers(g.Markers, p))
	return g
}

func (v *GoVisitor) VisitDefer(d *golang.Defer, p any) java.J {
	d = d.WithPrefix(v.self().VisitSpace(d.Prefix, p))
	d = d.WithMarkers(v.visitMarkers(d.Markers, p))
	return d
}

func (v *GoVisitor) VisitSend(s *golang.Send, p any) java.J {
	s = s.WithPrefix(v.self().VisitSpace(s.Prefix, p))
	s = s.WithMarkers(v.visitMarkers(s.Markers, p))
	return s
}

func (v *GoVisitor) VisitGoto(g *golang.Goto, p any) java.J {
	g = g.WithPrefix(v.self().VisitSpace(g.Prefix, p))
	g = g.WithMarkers(v.visitMarkers(g.Markers, p))
	return g
}

func (v *GoVisitor) VisitGoUnary(u *golang.Unary, p any) java.J {
	u = u.WithPrefix(v.self().VisitSpace(u.Prefix, p))
	u = u.WithMarkers(v.visitMarkers(u.Markers, p))
	op := u.Operator
	op.Before = v.self().VisitSpace(op.Before, p)
	u.Operator = op
	u.Expression = visitAndCast[java.Expression](v, u.Expression, p)
	return u
}

func (v *GoVisitor) VisitGoBinary(b *golang.Binary, p any) java.J {
	b = b.WithPrefix(v.self().VisitSpace(b.Prefix, p))
	b = b.WithMarkers(v.visitMarkers(b.Markers, p))
	b.Left = visitAndCast[java.Expression](v, b.Left, p)
	op := b.Operator
	op.Before = v.self().VisitSpace(op.Before, p)
	b.Operator = op
	b.Right = visitAndCast[java.Expression](v, b.Right, p)
	return b
}

func (v *GoVisitor) VisitGoAssignmentOperation(a *golang.AssignmentOperation, p any) java.J {
	a = a.WithPrefix(v.self().VisitSpace(a.Prefix, p))
	a = a.WithMarkers(v.visitMarkers(a.Markers, p))
	a.Variable = visitAndCast[java.Expression](v, a.Variable, p)
	op := a.Operator
	op.Before = v.self().VisitSpace(op.Before, p)
	a.Operator = op
	a.Assignment = visitAndCast[java.Expression](v, a.Assignment, p)
	return a
}

func (v *GoVisitor) VisitGoVariadic(vr *golang.Variadic, p any) java.J {
	vr = vr.WithPrefix(v.self().VisitSpace(vr.Prefix, p))
	vr = vr.WithMarkers(v.visitMarkers(vr.Markers, p))
	vr.Element = visitAndCast[java.Expression](v, vr.Element, p)
	vr.Dots = v.self().VisitSpace(vr.Dots, p)
	return vr
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
	ann = ann.WithPrefix(v.self().VisitSpace(ann.Prefix, p))
	ann = ann.WithMarkers(v.visitMarkers(ann.Markers, p))
	if ann.AnnotationType != nil {
		ann = ann.WithAnnotationType(visitExpression(v, ann.AnnotationType, p))
	}
	if ann.Arguments != nil {
		args := *ann.Arguments
		args.Before = v.self().VisitSpace(args.Before, p)
		args.Markers = v.visitMarkers(args.Markers, p)
		args.Elements = visitRightPaddedExpressionList(v, args.Elements, p)
		ann = ann.WithArguments(&args)
	}
	return ann
}

func (v *GoVisitor) VisitArrayType(at *java.ArrayType, p any) java.J {
	at = at.WithPrefix(v.self().VisitSpace(at.Prefix, p))
	at = at.WithMarkers(v.visitMarkers(at.Markers, p))
	return at
}

func (v *GoVisitor) VisitParentheses(paren *java.Parentheses, p any) java.J {
	paren = paren.WithPrefix(v.self().VisitSpace(paren.Prefix, p))
	paren = paren.WithMarkers(v.visitMarkers(paren.Markers, p))
	return paren
}

func (v *GoVisitor) VisitTypeCast(tc *java.TypeCast, p any) java.J {
	tc = tc.WithPrefix(v.self().VisitSpace(tc.Prefix, p))
	tc = tc.WithMarkers(v.visitMarkers(tc.Markers, p))
	return tc
}

func (v *GoVisitor) VisitControlParentheses(cp *java.ControlParentheses, p any) java.J {
	cp = cp.WithPrefix(v.self().VisitSpace(cp.Prefix, p))
	cp = cp.WithMarkers(v.visitMarkers(cp.Markers, p))
	return cp
}

func (v *GoVisitor) VisitArrayAccess(aa *java.ArrayAccess, p any) java.J {
	aa = aa.WithPrefix(v.self().VisitSpace(aa.Prefix, p))
	aa = aa.WithMarkers(v.visitMarkers(aa.Markers, p))
	return aa
}

func (v *GoVisitor) VisitParameterizedType(pt *java.ParameterizedType, p any) java.J {
	pt = pt.WithPrefix(v.self().VisitSpace(pt.Prefix, p))
	pt = pt.WithMarkers(v.visitMarkers(pt.Markers, p))
	if pt.Clazz != nil {
		pt.Clazz = visitExpression(v, pt.Clazz, p)
	}
	if pt.TypeParameters != nil {
		pt.TypeParameters.Before = v.self().VisitSpace(pt.TypeParameters.Before, p)
		pt.TypeParameters.Elements = visitRightPaddedList(v, pt.TypeParameters.Elements, p)
	}
	return pt
}

func (v *GoVisitor) VisitIndexList(il *golang.IndexList, p any) java.J {
	il = il.WithPrefix(v.self().VisitSpace(il.Prefix, p))
	il = il.WithMarkers(v.visitMarkers(il.Markers, p))
	if il.Target != nil {
		il.Target = visitExpression(v, il.Target, p)
	}
	il.Indices.Before = v.self().VisitSpace(il.Indices.Before, p)
	il.Indices.Elements = visitRightPaddedList(v, il.Indices.Elements, p)
	return il
}

func (v *GoVisitor) VisitArrayDimension(ad *java.ArrayDimension, p any) java.J {
	ad = ad.WithPrefix(v.self().VisitSpace(ad.Prefix, p))
	ad = ad.WithMarkers(v.visitMarkers(ad.Markers, p))
	return ad
}

func (v *GoVisitor) VisitComposite(c *golang.Composite, p any) java.J {
	c = c.WithPrefix(v.self().VisitSpace(c.Prefix, p))
	c = c.WithMarkers(v.visitMarkers(c.Markers, p))
	if c.TypeExpr != nil {
		c.TypeExpr = visitExpression(v, c.TypeExpr, p)
	}
	c.Elements.Before = v.self().VisitSpace(c.Elements.Before, p)
	c.Elements.Elements = visitRightPaddedList(v, c.Elements.Elements, p)
	return c
}

func (v *GoVisitor) VisitKeyValue(kv *golang.KeyValue, p any) java.J {
	kv = kv.WithPrefix(v.self().VisitSpace(kv.Prefix, p))
	kv = kv.WithMarkers(v.visitMarkers(kv.Markers, p))
	return kv
}

func (v *GoVisitor) VisitSlice(s *golang.Slice, p any) java.J {
	s = s.WithPrefix(v.self().VisitSpace(s.Prefix, p))
	s = s.WithMarkers(v.visitMarkers(s.Markers, p))
	return s
}

func (v *GoVisitor) VisitMapType(mt *golang.MapType, p any) java.J {
	mt = mt.WithPrefix(v.self().VisitSpace(mt.Prefix, p))
	mt = mt.WithMarkers(v.visitMarkers(mt.Markers, p))
	return mt
}

func (v *GoVisitor) VisitStatementExpression(se *golang.StatementExpression, p any) java.J {
	se = se.WithPrefix(v.self().VisitSpace(se.Prefix, p))
	se = se.WithMarkers(v.visitMarkers(se.Markers, p))
	result := v.self().Visit(se.Statement, p)
	if stmt, ok := result.(java.Statement); ok {
		se.Statement = stmt
	}
	return se
}

func (v *GoVisitor) VisitPointerType(pt *golang.PointerType, p any) java.J {
	pt = pt.WithPrefix(v.self().VisitSpace(pt.Prefix, p))
	pt = pt.WithMarkers(v.visitMarkers(pt.Markers, p))
	return pt
}

func (v *GoVisitor) VisitChannel(ch *golang.Channel, p any) java.J {
	ch = ch.WithPrefix(v.self().VisitSpace(ch.Prefix, p))
	ch = ch.WithMarkers(v.visitMarkers(ch.Markers, p))
	return ch
}

func (v *GoVisitor) VisitFuncType(ft *golang.FuncType, p any) java.J {
	ft = ft.WithPrefix(v.self().VisitSpace(ft.Prefix, p))
	ft = ft.WithMarkers(v.visitMarkers(ft.Markers, p))
	return ft
}

func (v *GoVisitor) VisitTypeList(tl *golang.TypeList, p any) java.J {
	tl = tl.WithPrefix(v.self().VisitSpace(tl.Prefix, p))
	tl = tl.WithMarkers(v.visitMarkers(tl.Markers, p))
	return tl
}

func (v *GoVisitor) VisitUnion(u *golang.Union, p any) java.J {
	u = u.WithPrefix(v.self().VisitSpace(u.Prefix, p))
	u = u.WithMarkers(v.visitMarkers(u.Markers, p))
	u.Types = visitRightPaddedExpressionList(v, u.Types, p)
	return u
}

func (v *GoVisitor) VisitUnderlyingType(ut *golang.UnderlyingType, p any) java.J {
	ut = ut.WithPrefix(v.self().VisitSpace(ut.Prefix, p))
	ut = ut.WithMarkers(v.visitMarkers(ut.Markers, p))
	ut.Element = visitExpression(v, ut.Element, p)
	return ut
}

func (v *GoVisitor) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	td = td.WithPrefix(v.self().VisitSpace(td.Prefix, p))
	td = td.WithMarkers(v.visitMarkers(td.Markers, p))
	if len(td.LeadingAnnotations) > 0 {
		anns := make([]*java.Annotation, 0, len(td.LeadingAnnotations))
		for _, a := range td.LeadingAnnotations {
			visited := v.self().Visit(a, p)
			if visited == nil {
				continue
			}
			anns = append(anns, visited.(*java.Annotation))
		}
		td = td.WithLeadingAnnotations(anns)
	}
	if td.Name != nil {
		td.Name = visitAndCast[*java.Identifier](v, td.Name, p)
	}
	if td.TypeParameters != nil {
		td = td.WithTypeParameters(visitAndCast[*java.TypeParameters](v, td.TypeParameters, p))
	}
	if td.Assign != nil {
		assign := *td.Assign
		assign.Before = v.self().VisitSpace(assign.Before, p)
		td.Assign = &assign
	}
	if td.Definition != nil {
		td.Definition = visitExpression(v, td.Definition, p)
	}
	if td.Specs != nil {
		specs := *td.Specs
		specs.Before = v.self().VisitSpace(specs.Before, p)
		specs.Elements = visitRightPaddedList(v, specs.Elements, p)
		td.Specs = &specs
	}
	return td
}

func (v *GoVisitor) VisitStructType(st *golang.StructType, p any) java.J {
	st = st.WithPrefix(v.self().VisitSpace(st.Prefix, p))
	st = st.WithMarkers(v.visitMarkers(st.Markers, p))
	return st
}

func (v *GoVisitor) VisitInterfaceType(it *golang.InterfaceType, p any) java.J {
	it = it.WithPrefix(v.self().VisitSpace(it.Prefix, p))
	it = it.WithMarkers(v.visitMarkers(it.Markers, p))
	return it
}

func (v *GoVisitor) VisitMultiAssignment(ma *golang.MultiAssignment, p any) java.J {
	ma = ma.WithPrefix(v.self().VisitSpace(ma.Prefix, p))
	ma = ma.WithMarkers(v.visitMarkers(ma.Markers, p))
	ma.Variables = visitRightPaddedExpressionList(v, ma.Variables, p)
	ma.Operator.Before = v.self().VisitSpace(ma.Operator.Before, p)
	ma.Values = visitRightPaddedExpressionList(v, ma.Values, p)
	return ma
}

func (v *GoVisitor) VisitCommClause(cc *golang.CommClause, p any) java.J {
	cc = cc.WithPrefix(v.self().VisitSpace(cc.Prefix, p))
	cc = cc.WithMarkers(v.visitMarkers(cc.Markers, p))
	return cc
}

func (v *GoVisitor) VisitSpace(space java.Space, p any) java.Space {
	return space
}

func (v *GoVisitor) VisitType(javaType java.JavaType, p any) java.JavaType {
	return javaType
}

func (v *GoVisitor) visitMarkers(markers java.Markers, p any) java.Markers {
	return markers
}

// --- Helper functions ---

func visitAndCast[T java.Tree](v *GoVisitor, t java.Tree, p any) T {
	result := v.self().Visit(t, p)
	if result == nil {
		var zero T
		return zero
	}
	return result.(T)
}

func visitExpression(v *GoVisitor, expr java.Expression, p any) java.Expression {
	result := v.self().Visit(expr, p)
	if result == nil {
		return nil
	}
	return result.(java.Expression)
}

func visitRightPaddedExpressionList(v *GoVisitor, list []java.RightPadded[java.Expression], p any) []java.RightPadded[java.Expression] {
	result := make([]java.RightPadded[java.Expression], 0, len(list))
	for _, rp := range list {
		visited := v.self().Visit(rp.Element, p)
		if visited == nil {
			continue
		}
		rp.Element = visited.(java.Expression)
		rp.After = v.self().VisitSpace(rp.After, p)
		result = append(result, rp)
	}
	return result
}

func visitRightPaddedList[T java.J](v *GoVisitor, list []java.RightPadded[T], p any) []java.RightPadded[T] {
	result := make([]java.RightPadded[T], 0, len(list))
	for _, rp := range list {
		visited := v.self().Visit(rp.Element, p)
		if visited == nil {
			continue
		}
		rp.Element = visited.(T)
		rp.After = v.self().VisitSpace(rp.After, p)
		result = append(result, rp)
	}
	return result
}
