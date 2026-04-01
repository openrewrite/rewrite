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

package printer

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoPrinter prints an OpenRewrite LST back to Go source code.
type GoPrinter struct {
	visitor.GoVisitor
}

func NewGoPrinter() *GoPrinter {
	return visitor.Init(&GoPrinter{})
}

// Print renders the given tree to a string.
func Print(t tree.Tree) string {
	p := NewGoPrinter()
	out := NewPrintOutputCapture()
	p.Visit(t, out)
	return out.String()
}

// PrintWithMarkers renders the given tree to a string, printing cross-cutting markers
// (SearchResult, Markup) using the given MarkerPrinter.
func PrintWithMarkers(t tree.Tree, mp MarkerPrinter) string {
	p := NewGoPrinter()
	out := NewPrintOutputCaptureWithMarkers(mp)
	p.Visit(t, out)
	return out.String()
}

// beforeSyntax handles the common pattern of emitting marker hooks and prefix space
// before a node's syntax. It replaces direct visitSpace calls for node prefixes.
func (p *GoPrinter) beforeSyntax(prefix tree.Space, markers tree.Markers, out *PrintOutputCapture) {
	out.BeforePrefix(markers)
	p.visitSpace(prefix, out)
	out.BeforeSyntax(markers)
}

// afterSyntax emits marker hooks after a node's syntax.
func (p *GoPrinter) afterSyntax(markers tree.Markers, out *PrintOutputCapture) {
	out.AfterSyntax(markers)
}

func (p *GoPrinter) VisitCompilationUnit(cu *tree.CompilationUnit, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(cu.Prefix, cu.Markers, out)
	out.Append("package")

	if cu.PackageDecl != nil {
		p.Visit(cu.PackageDecl.Element, out)
		p.visitSpace(cu.PackageDecl.After, out)
	}

	if cu.Imports != nil {
		p.visitSpace(cu.Imports.Before, out)
		out.Append("import")

		grouped := tree.FindMarker[tree.GroupedImport](cu.Imports.Markers)
		isGrouped := grouped != nil
		if isGrouped {
			p.visitSpace(grouped.Before, out)
			out.Append("(")
		}
		for _, rp := range cu.Imports.Elements {
			block := tree.FindMarker[tree.ImportBlock](rp.Element.Markers)
			if block != nil {
				if block.ClosePrevious {
					out.Append(")")
				}
				p.visitSpace(block.Before, out)
				out.Append("import")
				if block.Grouped {
					p.visitSpace(block.GroupedBefore, out)
					out.Append("(")
				}
				isGrouped = block.Grouped
			}
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
		}
		if isGrouped {
			out.Append(")")
		}
	}

	for _, rp := range cu.Statements {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
	}

	p.visitSpace(cu.EOF, out)
	p.afterSyntax(cu.Markers, out)
	return cu
}

func (p *GoPrinter) VisitIdentifier(ident *tree.Identifier, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ident.Prefix, ident.Markers, out)
	out.Append(ident.Name)
	p.afterSyntax(ident.Markers, out)
	return ident
}

func (p *GoPrinter) VisitLiteral(lit *tree.Literal, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(lit.Prefix, lit.Markers, out)
	out.Append(lit.Source)
	p.afterSyntax(lit.Markers, out)
	return lit
}

func (p *GoPrinter) VisitBinary(bin *tree.Binary, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(bin.Prefix, bin.Markers, out)
	p.Visit(bin.Left, out)
	p.visitSpace(bin.Operator.Before, out)
	out.Append(binaryOperatorString(bin.Operator.Element))
	p.Visit(bin.Right, out)
	p.afterSyntax(bin.Markers, out)
	return bin
}

func (p *GoPrinter) VisitBlock(block *tree.Block, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(block.Prefix, block.Markers, out)
	out.Append("{")
	for _, rp := range block.Statements {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
	}
	p.visitSpace(block.End, out)
	out.Append("}")
	p.afterSyntax(block.Markers, out)
	return block
}

func (p *GoPrinter) VisitReturn(ret *tree.Return, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ret.Prefix, ret.Markers, out)
	out.Append("return")
	for i, rp := range ret.Expressions {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
		if i < len(ret.Expressions)-1 {
			out.Append(",")
		}
	}
	p.afterSyntax(ret.Markers, out)
	return ret
}

func (p *GoPrinter) VisitIf(ifStmt *tree.If, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ifStmt.Prefix, ifStmt.Markers, out)
	out.Append("if")
	if ifStmt.Init != nil {
		p.Visit(ifStmt.Init.Element, out)
		p.visitSpace(ifStmt.Init.After, out)
		out.Append(";")
	}
	p.Visit(ifStmt.Condition, out)
	p.Visit(ifStmt.Then, out)
	if ifStmt.ElsePart != nil {
		p.visitSpace(ifStmt.ElsePart.After, out)
		out.Append("else")
		p.Visit(ifStmt.ElsePart.Element, out)
	}
	p.afterSyntax(ifStmt.Markers, out)
	return ifStmt
}

func (p *GoPrinter) VisitAssignment(assign *tree.Assignment, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(assign.Prefix, assign.Markers, out)
	p.Visit(assign.Variable, out)
	p.visitSpace(assign.Value.Before, out)
	if tree.FindMarker[tree.ShortVarDecl](assign.Markers) != nil {
		out.Append(":=")
	} else {
		out.Append("=")
	}
	p.Visit(assign.Value.Element, out)
	p.afterSyntax(assign.Markers, out)
	return assign
}

func (p *GoPrinter) VisitMethodDeclaration(md *tree.MethodDeclaration, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(md.Prefix, md.Markers, out)
	isInterfaceMethod := tree.FindMarker[tree.InterfaceMethod](md.Markers) != nil
	if !isInterfaceMethod {
		out.Append("func")
	}
	if md.Receiver != nil {
		p.printParamList(*md.Receiver, out)
	}
	if md.Name.Name != "" {
		p.Visit(md.Name, out)
	}
	p.printParamList(md.Parameters, out)
	if md.ReturnType != nil {
		p.Visit(md.ReturnType, out)
	}
	if md.Body != nil {
		p.Visit(md.Body, out)
	}
	p.afterSyntax(md.Markers, out)
	return md
}

func (p *GoPrinter) printParamList(params tree.Container[tree.Statement], out *PrintOutputCapture) {
	p.visitSpace(params.Before, out)
	out.Append("(")
	for i, rp := range params.Elements {
		p.Visit(rp.Element, out)
		if i < len(params.Elements)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		} else {
			p.visitSpace(rp.After, out)
		}
	}
	out.Append(")")
}

func (p *GoPrinter) VisitFieldAccess(fa *tree.FieldAccess, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(fa.Prefix, fa.Markers, out)
	p.Visit(fa.Target, out)
	p.visitSpace(fa.Name.Before, out)
	out.Append(".")
	p.Visit(fa.Name.Element, out)
	p.afterSyntax(fa.Markers, out)
	return fa
}

func (p *GoPrinter) VisitMethodInvocation(mi *tree.MethodInvocation, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(mi.Prefix, mi.Markers, out)
	if mi.Select != nil {
		p.Visit(mi.Select.Element, out)
		if mi.Name.Name != "" {
			out.Append(".")
		}
	}
	if mi.Name.Name != "" {
		p.Visit(mi.Name, out)
	}
	p.visitSpace(mi.Arguments.Before, out)
	out.Append("(")
	tc := tree.FindMarker[tree.TrailingComma](mi.Markers)
	for i, rp := range mi.Arguments.Elements {
		p.Visit(rp.Element, out)
		if i < len(mi.Arguments.Elements)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		} else if tc != nil {
			p.visitSpace(tc.Before, out)
			out.Append(",")
			p.visitSpace(tc.After, out)
		} else {
			p.visitSpace(rp.After, out)
		}
	}
	out.Append(")")
	p.afterSyntax(mi.Markers, out)
	return mi
}

func (p *GoPrinter) VisitVariableDeclarations(vd *tree.VariableDeclarations, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(vd.Prefix, vd.Markers, out)
	isGroupedSpec := tree.FindMarker[tree.GroupedSpec](vd.Markers) != nil
	if !isGroupedSpec {
		if tree.FindMarker[tree.ConstDecl](vd.Markers) != nil {
			out.Append("const")
		} else if tree.FindMarker[tree.VarKeyword](vd.Markers) != nil {
			out.Append("var")
		}
	}

	if vd.Specs != nil {
		// Grouped: var ( ... ) or const ( ... )
		p.visitSpace(vd.Specs.Before, out)
		out.Append("(")
		for _, rp := range vd.Specs.Elements {
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
		}
		out.Append(")")
		p.afterSyntax(vd.Markers, out)
		return vd
	}

	// Go order: keyword name[, name]... type [= value]
	// Print variable names first (without initializers)
	for i, v := range vd.Variables {
		p.visitSpace(v.Element.Prefix, out)
		if v.Element.Name.Name != "" {
			p.Visit(v.Element.Name, out)
		}
		if i < len(vd.Variables)-1 {
			p.visitSpace(v.After, out)
			out.Append(",")
		}
	}
	// Then type expression
	if vd.TypeExpr != nil {
		p.Visit(vd.TypeExpr, out)
	}
	// Then struct tag if present
	if tag := tree.FindMarker[tree.StructTag](vd.Markers); tag != nil {
		p.Visit(tag.Tag, out)
	}
	// Then initializers
	for _, v := range vd.Variables {
		if v.Element.Initializer != nil {
			p.visitSpace(v.Element.Initializer.Before, out)
			out.Append("=")
			p.Visit(v.Element.Initializer.Element, out)
		}
	}
	p.afterSyntax(vd.Markers, out)
	return vd
}

func (p *GoPrinter) VisitVariableDeclarator(vd *tree.VariableDeclarator, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(vd.Prefix, vd.Markers, out)
	p.Visit(vd.Name, out)
	p.afterSyntax(vd.Markers, out)
	return vd
}

func (p *GoPrinter) VisitImport(imp *tree.Import, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(imp.Prefix, imp.Markers, out)
	if imp.Alias != nil {
		p.Visit(imp.Alias.Element, out)
	}
	p.Visit(imp.Qualid, out)
	p.afterSyntax(imp.Markers, out)
	return imp
}

func (p *GoPrinter) VisitSwitch(sw *tree.Switch, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(sw.Prefix, sw.Markers, out)
	if tree.FindMarker[tree.SelectStmt](sw.Markers) != nil {
		out.Append("select")
	} else {
		out.Append("switch")
	}
	if sw.Init != nil {
		p.Visit(sw.Init.Element, out)
		p.visitSpace(sw.Init.After, out)
		out.Append(";")
	}
	if sw.Tag != nil {
		p.Visit(sw.Tag.Element, out)
		p.visitSpace(sw.Tag.After, out)
	}
	p.Visit(sw.Body, out)
	p.afterSyntax(sw.Markers, out)
	return sw
}

func (p *GoPrinter) VisitCase(c *tree.Case, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(c.Prefix, c.Markers, out)
	if len(c.Expressions.Elements) > 0 {
		out.Append("case")
		for i, rp := range c.Expressions.Elements {
			p.Visit(rp.Element, out)
			if i < len(c.Expressions.Elements)-1 {
				p.visitSpace(rp.After, out)
				out.Append(",")
			} else {
				p.visitSpace(rp.After, out)
			}
		}
	} else {
		out.Append("default")
		p.visitSpace(c.Expressions.Before, out)
	}
	out.Append(":")
	for _, rp := range c.Body {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
	}
	p.afterSyntax(c.Markers, out)
	return c
}

func (p *GoPrinter) VisitAssignmentOperation(ao *tree.AssignmentOperation, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ao.Prefix, ao.Markers, out)
	p.Visit(ao.Variable, out)
	p.visitSpace(ao.Operator.Before, out)
	out.Append(assignmentOperatorString(ao.Operator.Element))
	p.Visit(ao.Assignment, out)
	p.afterSyntax(ao.Markers, out)
	return ao
}

func (p *GoPrinter) VisitForLoop(forLoop *tree.ForLoop, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(forLoop.Prefix, forLoop.Markers, out)
	out.Append("for")
	p.Visit(&forLoop.Control, out)
	p.Visit(forLoop.Body, out)
	p.afterSyntax(forLoop.Markers, out)
	return forLoop
}

func (p *GoPrinter) VisitForControl(control *tree.ForControl, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(control.Prefix, control.Markers, out)
	if control.Init != nil {
		// 3-clause form: init; cond; update
		p.Visit(control.Init.Element, out)
		p.visitSpace(control.Init.After, out)
		out.Append(";")
		if control.Condition != nil {
			p.Visit(control.Condition.Element, out)
			p.visitSpace(control.Condition.After, out)
		}
		out.Append(";")
		if control.Update != nil {
			p.Visit(control.Update.Element, out)
			p.visitSpace(control.Update.After, out)
		}
	} else if control.Condition != nil {
		// Condition-only form: for cond {}
		p.Visit(control.Condition.Element, out)
		p.visitSpace(control.Condition.After, out)
	}
	// else: infinite loop, nothing to print
	p.afterSyntax(control.Markers, out)
	return control
}

func (p *GoPrinter) VisitForEachLoop(forEach *tree.ForEachLoop, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(forEach.Prefix, forEach.Markers, out)
	out.Append("for")
	p.Visit(&forEach.Control, out)
	p.Visit(forEach.Body, out)
	p.afterSyntax(forEach.Markers, out)
	return forEach
}

func (p *GoPrinter) VisitForEachControl(control *tree.ForEachControl, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(control.Prefix, control.Markers, out)
	if control.Key != nil {
		p.Visit(control.Key.Element, out)
		p.visitSpace(control.Key.After, out)
		if control.Value != nil {
			out.Append(",")
			p.Visit(control.Value.Element, out)
			p.visitSpace(control.Value.After, out)
		}
		if control.Operator.Element == tree.AssignOpDefine {
			out.Append(":=")
		} else {
			out.Append("=")
		}
		p.visitSpace(control.Operator.Before, out)
	}
	out.Append("range")
	p.Visit(control.Iterable, out)
	p.afterSyntax(control.Markers, out)
	return control
}

func (p *GoPrinter) VisitUnary(unary *tree.Unary, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(unary.Prefix, unary.Markers, out)
	if unary.Operator.Element == tree.PostIncrement || unary.Operator.Element == tree.PostDecrement || unary.Operator.Element == tree.SpreadPostfix {
		p.Visit(unary.Operand, out)
		p.visitSpace(unary.Operator.Before, out)
		out.Append(unaryOperatorString(unary.Operator.Element))
	} else {
		out.Append(unaryOperatorString(unary.Operator.Element))
		p.Visit(unary.Operand, out)
	}
	p.afterSyntax(unary.Markers, out)
	return unary
}

func (p *GoPrinter) VisitBreak(b *tree.Break, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(b.Prefix, b.Markers, out)
	out.Append("break")
	if b.Label != nil {
		p.Visit(b.Label, out)
	}
	p.afterSyntax(b.Markers, out)
	return b
}

func (p *GoPrinter) VisitContinue(c *tree.Continue, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(c.Prefix, c.Markers, out)
	out.Append("continue")
	if c.Label != nil {
		p.Visit(c.Label, out)
	}
	p.afterSyntax(c.Markers, out)
	return c
}

func (p *GoPrinter) VisitLabel(l *tree.Label, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(l.Prefix, l.Markers, out)
	p.Visit(l.Name.Element, out)
	p.visitSpace(l.Name.After, out)
	out.Append(":")
	p.Visit(l.Statement, out)
	p.afterSyntax(l.Markers, out)
	return l
}

func (p *GoPrinter) VisitGoStmt(g *tree.GoStmt, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(g.Prefix, g.Markers, out)
	out.Append("go")
	p.Visit(g.Expr, out)
	p.afterSyntax(g.Markers, out)
	return g
}

func (p *GoPrinter) VisitDefer(d *tree.Defer, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(d.Prefix, d.Markers, out)
	out.Append("defer")
	p.Visit(d.Expr, out)
	p.afterSyntax(d.Markers, out)
	return d
}

func (p *GoPrinter) VisitSend(s *tree.Send, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(s.Prefix, s.Markers, out)
	p.Visit(s.Channel, out)
	p.visitSpace(s.Arrow.Before, out)
	out.Append("<-")
	p.Visit(s.Arrow.Element, out)
	p.afterSyntax(s.Markers, out)
	return s
}

func (p *GoPrinter) VisitGoto(g *tree.Goto, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(g.Prefix, g.Markers, out)
	out.Append("goto")
	p.Visit(g.Label, out)
	p.afterSyntax(g.Markers, out)
	return g
}

func (p *GoPrinter) VisitFallthrough(f *tree.Fallthrough, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(f.Prefix, f.Markers, out)
	out.Append("fallthrough")
	p.afterSyntax(f.Markers, out)
	return f
}

func (p *GoPrinter) VisitArrayType(at *tree.ArrayType, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(at.Prefix, at.Markers, out)
	out.Append("[")
	if at.Length != nil {
		p.Visit(at.Length, out)
	}
	p.visitSpace(at.Dimension.Element, out)
	out.Append("]")
	p.Visit(at.ElementType, out)
	p.afterSyntax(at.Markers, out)
	return at
}

func (p *GoPrinter) VisitParentheses(paren *tree.Parentheses, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(paren.Prefix, paren.Markers, out)
	out.Append("(")
	p.Visit(paren.Tree.Element, out)
	p.visitSpace(paren.Tree.After, out)
	out.Append(")")
	p.afterSyntax(paren.Markers, out)
	return paren
}

func (p *GoPrinter) VisitTypeCast(tc *tree.TypeCast, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(tc.Prefix, tc.Markers, out)
	// Go type assertion: expr.(Type)
	p.Visit(tc.Expr, out)
	out.Append(".")
	p.Visit(tc.Clazz, out)
	p.afterSyntax(tc.Markers, out)
	return tc
}

func (p *GoPrinter) VisitControlParentheses(cp *tree.ControlParentheses, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(cp.Prefix, cp.Markers, out)
	out.Append("(")
	p.Visit(cp.Tree.Element, out)
	p.visitSpace(cp.Tree.After, out)
	out.Append(")")
	p.afterSyntax(cp.Markers, out)
	return cp
}

func (p *GoPrinter) VisitArrayAccess(aa *tree.ArrayAccess, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(aa.Prefix, aa.Markers, out)
	p.Visit(aa.Indexed, out)
	p.Visit(aa.Dimension, out)
	p.afterSyntax(aa.Markers, out)
	return aa
}

func (p *GoPrinter) VisitArrayDimension(ad *tree.ArrayDimension, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ad.Prefix, ad.Markers, out)
	out.Append("[")
	p.Visit(ad.Index.Element, out)
	p.visitSpace(ad.Index.After, out)
	out.Append("]")
	p.afterSyntax(ad.Markers, out)
	return ad
}

func (p *GoPrinter) VisitIndexList(il *tree.IndexList, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(il.Prefix, il.Markers, out)
	p.Visit(il.Target, out)
	p.visitSpace(il.Indices.Before, out)
	out.Append("[")
	for i, rp := range il.Indices.Elements {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
		if i < len(il.Indices.Elements)-1 {
			out.Append(",")
		}
	}
	out.Append("]")
	p.afterSyntax(il.Markers, out)
	return il
}

func (p *GoPrinter) VisitComposite(c *tree.Composite, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(c.Prefix, c.Markers, out)
	if c.TypeExpr != nil {
		p.Visit(c.TypeExpr, out)
	}
	p.visitSpace(c.Elements.Before, out)
	out.Append("{")
	ctc := tree.FindMarker[tree.TrailingComma](c.Markers)
	for i, rp := range c.Elements.Elements {
		p.Visit(rp.Element, out)
		if i < len(c.Elements.Elements)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		} else if ctc != nil {
			p.visitSpace(ctc.Before, out)
			out.Append(",")
			p.visitSpace(ctc.After, out)
		} else {
			p.visitSpace(rp.After, out)
		}
	}
	out.Append("}")
	p.afterSyntax(c.Markers, out)
	return c
}

func (p *GoPrinter) VisitKeyValue(kv *tree.KeyValue, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(kv.Prefix, kv.Markers, out)
	p.Visit(kv.Key, out)
	p.visitSpace(kv.Value.Before, out)
	out.Append(":")
	p.Visit(kv.Value.Element, out)
	p.afterSyntax(kv.Markers, out)
	return kv
}

func (p *GoPrinter) VisitSlice(s *tree.Slice, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(s.Prefix, s.Markers, out)
	p.Visit(s.Indexed, out)
	p.visitSpace(s.OpenBracket, out)
	out.Append("[")
	p.Visit(s.Low.Element, out)
	p.visitSpace(s.Low.After, out)
	out.Append(":")
	p.Visit(s.High.Element, out)
	if s.Max != nil {
		p.visitSpace(s.High.After, out)
		out.Append(":")
		p.Visit(s.Max, out)
	}
	p.visitSpace(s.CloseBracket, out)
	out.Append("]")
	p.afterSyntax(s.Markers, out)
	return s
}

func (p *GoPrinter) VisitMapType(mt *tree.MapType, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(mt.Prefix, mt.Markers, out)
	out.Append("map")
	p.visitSpace(mt.OpenBracket, out)
	out.Append("[")
	p.Visit(mt.Key.Element, out)
	p.visitSpace(mt.Key.After, out)
	out.Append("]")
	p.Visit(mt.Value, out)
	p.afterSyntax(mt.Markers, out)
	return mt
}

func (p *GoPrinter) VisitChannel(ch *tree.Channel, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ch.Prefix, ch.Markers, out)
	switch ch.Dir {
	case tree.ChanBidi:
		out.Append("chan")
	case tree.ChanSendOnly:
		out.Append("chan<-")
	case tree.ChanRecvOnly:
		out.Append("<-chan")
	}
	p.Visit(ch.Value, out)
	p.afterSyntax(ch.Markers, out)
	return ch
}

func (p *GoPrinter) VisitFuncType(ft *tree.FuncType, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ft.Prefix, ft.Markers, out)
	out.Append("func")
	p.visitSpace(ft.Parameters.Before, out)
	out.Append("(")
	for i, rp := range ft.Parameters.Elements {
		p.Visit(rp.Element, out)
		if i < len(ft.Parameters.Elements)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		}
	}
	out.Append(")")
	if ft.ReturnType != nil {
		p.Visit(ft.ReturnType, out)
	}
	p.afterSyntax(ft.Markers, out)
	return ft
}

func (p *GoPrinter) VisitTypeList(tl *tree.TypeList, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(tl.Prefix, tl.Markers, out)
	p.visitSpace(tl.Types.Before, out)
	out.Append("(")
	for i, rp := range tl.Types.Elements {
		p.Visit(rp.Element, out)
		if i < len(tl.Types.Elements)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		} else {
			p.visitSpace(rp.After, out)
		}
	}
	out.Append(")")
	p.afterSyntax(tl.Markers, out)
	return tl
}

func (p *GoPrinter) VisitCommClause(cc *tree.CommClause, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(cc.Prefix, cc.Markers, out)
	if cc.Comm != nil {
		out.Append("case")
		p.Visit(cc.Comm, out)
	} else {
		out.Append("default")
	}
	p.visitSpace(cc.Colon, out)
	out.Append(":")
	for _, rp := range cc.Body {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
	}
	p.afterSyntax(cc.Markers, out)
	return cc
}

func (p *GoPrinter) VisitMultiAssignment(ma *tree.MultiAssignment, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ma.Prefix, ma.Markers, out)
	for i, rp := range ma.Variables {
		p.Visit(rp.Element, out)
		if i < len(ma.Variables)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		}
	}
	p.visitSpace(ma.Operator.Before, out)
	if tree.FindMarker[tree.ShortVarDecl](ma.Markers) != nil {
		out.Append(":=")
	} else {
		out.Append("=")
	}
	for i, rp := range ma.Values {
		p.Visit(rp.Element, out)
		if i < len(ma.Values)-1 {
			p.visitSpace(rp.After, out)
			out.Append(",")
		}
	}
	p.afterSyntax(ma.Markers, out)
	return ma
}

func (p *GoPrinter) VisitStructType(st *tree.StructType, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(st.Prefix, st.Markers, out)
	out.Append("struct")
	p.Visit(st.Body, out)
	p.afterSyntax(st.Markers, out)
	return st
}

func (p *GoPrinter) VisitInterfaceType(it *tree.InterfaceType, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(it.Prefix, it.Markers, out)
	out.Append("interface")
	p.Visit(it.Body, out)
	p.afterSyntax(it.Markers, out)
	return it
}

func (p *GoPrinter) VisitTypeDecl(td *tree.TypeDecl, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(td.Prefix, td.Markers, out)
	if tree.FindMarker[tree.GroupedSpec](td.Markers) == nil {
		out.Append("type")
	}

	if td.Specs != nil {
		// Grouped: type ( ... )
		p.visitSpace(td.Specs.Before, out)
		out.Append("(")
		for _, rp := range td.Specs.Elements {
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
		}
		out.Append(")")
	} else {
		// Single: type Name Type
		p.Visit(td.Name, out)
		if td.Assign != nil {
			p.visitSpace(td.Assign.Before, out)
			out.Append("=")
		}
		p.Visit(td.Definition, out)
	}
	p.afterSyntax(td.Markers, out)
	return td
}

func (p *GoPrinter) VisitEmpty(empty *tree.Empty, param any) tree.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(empty.Prefix, empty.Markers, out)
	p.afterSyntax(empty.Markers, out)
	return empty
}

// visitSpace emits whitespace and comments to the output.
// Convention: Whitespace (before comments) is emitted first, then each comment
// with its suffix. This matches Java OpenRewrite's Space model.
func (p *GoPrinter) visitSpace(space tree.Space, out *PrintOutputCapture) {
	out.Append(space.Whitespace)
	for _, comment := range space.Comments {
		out.Append(comment.Text)
		out.Append(comment.Suffix)
	}
}

func binaryOperatorString(op tree.BinaryOperator) string {
	switch op {
	case tree.Add:
		return "+"
	case tree.Subtract:
		return "-"
	case tree.Multiply:
		return "*"
	case tree.Divide:
		return "/"
	case tree.Modulo:
		return "%"
	case tree.Equal:
		return "=="
	case tree.NotEqual:
		return "!="
	case tree.LessThan:
		return "<"
	case tree.LessThanOrEqual:
		return "<="
	case tree.GreaterThan:
		return ">"
	case tree.GreaterThanOrEqual:
		return ">="
	case tree.LogicalAnd:
		return "&&"
	case tree.LogicalOr:
		return "||"
	case tree.BitwiseAnd:
		return "&"
	case tree.BitwiseOr:
		return "|"
	case tree.BitwiseXor:
		return "^"
	case tree.LeftShift:
		return "<<"
	case tree.RightShift:
		return ">>"
	case tree.AndNot:
		return "&^"
	default:
		return "?"
	}
}

func assignmentOperatorString(op tree.AssignmentOperator) string {
	switch op {
	case tree.AddAssign:
		return "+="
	case tree.SubAssign:
		return "-="
	case tree.MulAssign:
		return "*="
	case tree.DivAssign:
		return "/="
	case tree.ModAssign:
		return "%="
	case tree.AndAssign:
		return "&="
	case tree.OrAssign:
		return "|="
	case tree.XorAssign:
		return "^="
	case tree.ShlAssign:
		return "<<="
	case tree.ShrAssign:
		return ">>="
	case tree.AndNotAssign:
		return "&^="
	default:
		return "?="
	}
}

func unaryOperatorString(op tree.UnaryOperator) string {
	switch op {
	case tree.Negate:
		return "-"
	case tree.Not:
		return "!"
	case tree.BitwiseNot:
		return "^"
	case tree.Deref:
		return "*"
	case tree.AddressOf:
		return "&"
	case tree.Receive:
		return "<-"
	case tree.Positive:
		return "+"
	case tree.PostIncrement:
		return "++"
	case tree.PostDecrement:
		return "--"
	case tree.Spread:
		return "..."
	case tree.SpreadPostfix:
		return "..."
	case tree.Tilde:
		return "~"
	default:
		return "?"
	}
}
