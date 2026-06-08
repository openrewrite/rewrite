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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
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
func Print(t java.Tree) string {
	p := NewGoPrinter()
	out := NewPrintOutputCapture()
	p.Visit(t, out)
	return out.String()
}

// PrintWithMarkers renders the given tree to a string, printing cross-cutting markers
// (SearchResult, Markup) using the given MarkerPrinter.
func PrintWithMarkers(t java.Tree, mp MarkerPrinter) string {
	p := NewGoPrinter()
	out := NewPrintOutputCaptureWithMarkers(mp)
	p.Visit(t, out)
	return out.String()
}

// beforeSyntax handles the common pattern of emitting marker hooks and prefix space
// before a node's syntax. It replaces direct visitSpace calls for node prefixes.
func (p *GoPrinter) beforeSyntax(prefix java.Space, markers java.Markers, out *PrintOutputCapture) {
	out.BeforePrefix(markers)
	p.visitSpace(prefix, out)
	out.BeforeSyntax(markers)
}

// afterSyntax emits marker hooks after a node's syntax.
func (p *GoPrinter) afterSyntax(markers java.Markers, out *PrintOutputCapture) {
	out.AfterSyntax(markers)
}

func (p *GoPrinter) VisitCompilationUnit(cu *golang.CompilationUnit, param any) java.J {
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

		grouped := java.FindMarker[golang.GroupedImport](cu.Imports.Markers)
		isGrouped := grouped != nil
		if isGrouped {
			p.visitSpace(grouped.Before, out)
			out.Append("(")
		}
		for _, rp := range cu.Imports.Elements {
			block := java.FindMarker[golang.ImportBlock](rp.Element.Markers)
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

	p.afterSyntax(cu.Markers, out)
	p.visitSpace(cu.EOF, out)
	return cu
}

func (p *GoPrinter) VisitIdentifier(ident *java.Identifier, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ident.Prefix, ident.Markers, out)
	out.Append(ident.Name)
	p.afterSyntax(ident.Markers, out)
	return ident
}

func (p *GoPrinter) VisitLiteral(lit *java.Literal, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(lit.Prefix, lit.Markers, out)
	out.Append(lit.Source)
	p.afterSyntax(lit.Markers, out)
	return lit
}

func (p *GoPrinter) VisitBinary(bin *java.Binary, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(bin.Prefix, bin.Markers, out)
	p.Visit(bin.Left, out)
	p.visitSpace(bin.Operator.Before, out)
	out.Append(binaryOperatorString(bin.Operator.Element))
	p.Visit(bin.Right, out)
	p.afterSyntax(bin.Markers, out)
	return bin
}

func (p *GoPrinter) VisitBlock(block *java.Block, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(block.Prefix, block.Markers, out)
	out.Append("{")
	for _, rp := range block.Statements {
		p.Visit(rp.Element, out)
		// If the source had `;` separating this statement from the
		// next, the parser captured the leading space as After and
		// stamped a Semicolon marker on the RightPadded.
		p.visitSpace(rp.After, out)
		if java.FindMarker[golang.Semicolon](rp.Markers) != nil {
			out.Append(";")
		}
	}
	p.visitSpace(block.End, out)
	out.Append("}")
	p.afterSyntax(block.Markers, out)
	return block
}

func (p *GoPrinter) VisitReturn(ret *java.Return, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ret.Prefix, ret.Markers, out)
	out.Append("return")
	if ret.Expression != nil {
		p.Visit(ret.Expression, out)
	}
	p.afterSyntax(ret.Markers, out)
	return ret
}

func (p *GoPrinter) VisitGoReturn(ret *golang.Return, param any) java.J {
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

func (p *GoPrinter) VisitIf(ifStmt *java.If, param any) java.J {
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

func (p *GoPrinter) VisitAssignment(assign *java.Assignment, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(assign.Prefix, assign.Markers, out)
	p.Visit(assign.Variable, out)
	p.visitSpace(assign.Value.Before, out)
	if java.FindMarker[golang.ShortVarDecl](assign.Markers) != nil {
		out.Append(":=")
	} else {
		out.Append("=")
	}
	p.Visit(assign.Value.Element, out)
	p.afterSyntax(assign.Markers, out)
	return assign
}

func (p *GoPrinter) VisitMethodDeclaration(md *java.MethodDeclaration, param any) java.J {
	out := param.(*PrintOutputCapture)
	// A method with a receiver is wrapped in a golang.MethodDeclaration, which
	// owns the prefix (J.MethodDeclaration has no receiver slot). When wrapped,
	// the prefix and the receiver `(s *Service)` come from that parent; the
	// inner declaration's own prefix is empty.
	wrapper, wrapped := p.methodDeclarationWrapper()
	prefix := md.Prefix
	if wrapped {
		prefix = wrapper.Prefix
	}
	// Each leading annotation emits as a `//<name>[ <args>]` line. The
	// annotation's Prefix supplies the whitespace before `//` (newline
	// + indent for non-first directives). The newline that follows the
	// last directive lives on the prefix below.
	for _, ann := range md.LeadingAnnotations {
		p.visitSpace(ann.Prefix, out)
		out.Append("//")
		p.printDirectiveBody(ann, out)
	}
	p.beforeSyntax(prefix, md.Markers, out)
	isInterfaceMethod := java.FindMarker[golang.InterfaceMethod](md.Markers) != nil
	if !isInterfaceMethod {
		out.Append("func")
	}
	if wrapped {
		// receiver printed between `func` and the name
		p.printParamList(wrapper.Receiver, out)
	}
	if md.Name.Name != "" {
		p.Visit(md.Name, out)
	}
	if md.TypeParameters != nil {
		p.Visit(md.TypeParameters, out)
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

// VisitGoMethodDeclaration prints a method declaration with a receiver. The
// wrapper owns the prefix and the receiver, but both are emitted by the inner
// declaration's VisitMethodDeclaration (which sources them via the cursor),
// keeping the receiver correctly positioned between `func` and the name and the
// prefix after any leading `//go:` directives. So this just visits the inner.
func (p *GoPrinter) VisitGoMethodDeclaration(md *golang.MethodDeclaration, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.Visit(md.Declaration, out)
	return md
}

// methodDeclarationWrapper returns the golang.MethodDeclaration directly
// wrapping the J.MethodDeclaration currently being printed, if any.
func (p *GoPrinter) methodDeclarationWrapper() (*golang.MethodDeclaration, bool) {
	c := p.Cursor()
	if c == nil || c.Parent() == nil {
		return nil, false
	}
	wrapper, ok := c.Parent().Value().(*golang.MethodDeclaration)
	return wrapper, ok
}

func (p *GoPrinter) VisitTypeParameters(tps *java.TypeParameters, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(tps.Prefix, tps.Markers, out)
	out.Append("[")
	for i, rp := range tps.TypeParameters {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
		if i < len(tps.TypeParameters)-1 {
			out.Append(",")
		}
	}
	out.Append("]")
	p.afterSyntax(tps.Markers, out)
	return tps
}

func (p *GoPrinter) VisitTypeParameter(tp *java.TypeParameter, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(tp.Prefix, tp.Markers, out)
	p.Visit(tp.Name, out)
	if tp.Bounds != nil {
		p.visitSpace(tp.Bounds.Before, out)
		for i, rp := range tp.Bounds.Elements {
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
			if i < len(tp.Bounds.Elements)-1 {
				out.Append(",")
			}
		}
	}
	p.afterSyntax(tp.Markers, out)
	return tp
}

func (p *GoPrinter) printParamList(params java.Container[java.Statement], out *PrintOutputCapture) {
	p.visitSpace(params.Before, out)
	out.Append("(")
	tc := java.FindMarker[golang.TrailingComma](params.Markers)
	for i, rp := range params.Elements {
		p.Visit(rp.Element, out)
		if i < len(params.Elements)-1 {
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
}

func (p *GoPrinter) VisitFieldAccess(fa *java.FieldAccess, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(fa.Prefix, fa.Markers, out)
	p.Visit(fa.Target, out)
	p.visitSpace(fa.Name.Before, out)
	out.Append(".")
	p.Visit(fa.Name.Element, out)
	p.afterSyntax(fa.Markers, out)
	return fa
}

func (p *GoPrinter) VisitMethodInvocation(mi *java.MethodInvocation, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(mi.Prefix, mi.Markers, out)
	if mi.Select != nil {
		p.Visit(mi.Select.Element, out)
		if mi.Name.Name != "" {
			p.visitSpace(mi.Select.After, out)
			out.Append(".")
		}
	}
	if mi.Name.Name != "" {
		p.Visit(mi.Name, out)
	}
	p.visitSpace(mi.Arguments.Before, out)
	out.Append("(")
	tc := java.FindMarker[golang.TrailingComma](mi.Markers)
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

func (p *GoPrinter) VisitVariableDeclarations(vd *java.VariableDeclarations, param any) java.J {
	out := param.(*PrintOutputCapture)
	// Non-struct context: any leading annotations are `//go:`-style
	// directives, emitted before the var/const keyword. Struct-field
	// context handles annotations later (after the type) as a
	// backtick-wrapped tag.
	if !p.insideStructType() {
		for _, ann := range vd.LeadingAnnotations {
			p.visitSpace(ann.Prefix, out)
			out.Append("//")
			p.printDirectiveBody(ann, out)
		}
	}
	p.beforeSyntax(vd.Prefix, vd.Markers, out)
	isGroupedSpec := java.FindMarker[golang.GroupedSpec](vd.Markers) != nil
	if !isGroupedSpec {
		if java.FindMarker[golang.ConstDecl](vd.Markers) != nil {
			out.Append("const")
		} else if java.FindMarker[golang.VarKeyword](vd.Markers) != nil {
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
	// Then varargs + type expression
	if vd.Varargs != nil {
		p.visitSpace(*vd.Varargs, out)
		out.Append("...")
	}
	if vd.TypeExpr != nil {
		p.Visit(vd.TypeExpr, out)
	}
	// Then struct tag, reconstructed from LeadingAnnotations (one
	// Annotation per `key:"value"` pair). Only emitted when this
	// VariableDeclarations is a struct field — non-struct positions
	// don't allow tags syntactically. Inner-leading / inner-trailing
	// whitespace is normalized to gofmt's canonical zero-padding (we
	// chose Option 1 in the design discussion: lossy on non-canonical
	// input, exact on gofmt'd input).
	if len(vd.LeadingAnnotations) > 0 && p.insideStructType() {
		first := vd.LeadingAnnotations[0]
		p.visitSpace(first.Prefix, out)
		out.Append("`")
		p.printAnnotationBody(first, out)
		for _, ann := range vd.LeadingAnnotations[1:] {
			p.visitSpace(ann.Prefix, out)
			p.printAnnotationBody(ann, out)
		}
		out.Append("`")
	}
	// Then initializers
	firstInit := true
	for _, v := range vd.Variables {
		if v.Element.Initializer != nil {
			p.visitSpace(v.Element.Initializer.Before, out)
			if firstInit {
				out.Append("=")
				firstInit = false
			} else {
				out.Append(",")
			}
			p.Visit(v.Element.Initializer.Element, out)
		}
	}
	p.afterSyntax(vd.Markers, out)
	return vd
}

func (p *GoPrinter) VisitVariableDeclarator(vd *java.VariableDeclarator, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(vd.Prefix, vd.Markers, out)
	p.Visit(vd.Name, out)
	p.afterSyntax(vd.Markers, out)
	return vd
}

func (p *GoPrinter) VisitImport(imp *java.Import, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(imp.Prefix, imp.Markers, out)
	if imp.Alias != nil {
		p.Visit(imp.Alias.Element, out)
	}
	p.Visit(imp.Qualid, out)
	p.afterSyntax(imp.Markers, out)
	return imp
}

func (p *GoPrinter) VisitSwitch(sw *java.Switch, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(sw.Prefix, sw.Markers, out)
	if java.FindMarker[golang.SelectStmt](sw.Markers) != nil {
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

func (p *GoPrinter) VisitCase(c *java.Case, param any) java.J {
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

func (p *GoPrinter) VisitAssignmentOperation(ao *java.AssignmentOperation, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ao.Prefix, ao.Markers, out)
	p.Visit(ao.Variable, out)
	p.visitSpace(ao.Operator.Before, out)
	out.Append(assignmentOperatorString(ao.Operator.Element))
	p.Visit(ao.Assignment, out)
	p.afterSyntax(ao.Markers, out)
	return ao
}

func (p *GoPrinter) VisitForLoop(forLoop *java.ForLoop, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(forLoop.Prefix, forLoop.Markers, out)
	out.Append("for")
	p.Visit(&forLoop.Control, out)
	p.Visit(forLoop.Body, out)
	p.afterSyntax(forLoop.Markers, out)
	return forLoop
}

func (p *GoPrinter) VisitForControl(control *java.ForControl, param any) java.J {
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

func (p *GoPrinter) VisitForEachLoop(forEach *java.ForEachLoop, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(forEach.Prefix, forEach.Markers, out)
	out.Append("for")
	p.Visit(&forEach.Control, out)
	p.Visit(forEach.Body, out)
	p.afterSyntax(forEach.Markers, out)
	return forEach
}

func (p *GoPrinter) VisitForEachControl(control *java.ForEachControl, param any) java.J {
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
		if control.Operator.Element == java.AssignOpDefine {
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

// VisitAnnotation prints an Annotation in struct-tag form
// (`key:"value"`) — including its leading whitespace via Prefix.
// Backtick wrapping is the VariableDeclarations printer's job for
// struct-field context; this method only emits the annotation's own
// substring.
func (p *GoPrinter) VisitAnnotation(ann *java.Annotation, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ann.Prefix, ann.Markers, out)
	p.printAnnotationBody(ann, out)
	p.afterSyntax(ann.Markers, out)
	return ann
}

// printAnnotationBody emits an annotation's body content in struct-tag
// form (type, colon, arguments) without the leading Prefix. Used by
// VariableDeclarations to lay out a backtick-wrapped struct tag where
// the first annotation's Prefix lives outside the backticks.
func (p *GoPrinter) printAnnotationBody(ann *java.Annotation, out *PrintOutputCapture) {
	if ann.AnnotationType != nil {
		p.Visit(ann.AnnotationType, out)
	}
	if ann.Arguments != nil {
		p.visitSpace(ann.Arguments.Before, out)
		out.Append(":")
		for _, rp := range ann.Arguments.Elements {
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
		}
	}
}

// printDirectiveBody emits an annotation's body in source-directive
// form: `<name>[ <args>]`. Used to render `//go:noinline`,
// `//go:linkname x runtime.x`, `//lint:ignore`, etc., on
// MethodDeclaration / TypeDecl / top-level VariableDeclarations. The
// preceding `//` and the annotation's leading Prefix are emitted by
// the caller; this helper only produces the substring after `//`.
//
// Arguments are emitted as their raw source (single Literal whose
// Source field carries the rest-of-line text). The space between the
// directive name and its arguments lives on the Arguments.Before slot
// — typically a single space.
func (p *GoPrinter) printDirectiveBody(ann *java.Annotation, out *PrintOutputCapture) {
	if ann.AnnotationType != nil {
		p.Visit(ann.AnnotationType, out)
	}
	if ann.Arguments != nil {
		p.visitSpace(ann.Arguments.Before, out)
		for _, rp := range ann.Arguments.Elements {
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
		}
	}
}

// insideStructType reports whether the cursor's value sits inside a
// StructType ancestor — i.e., it's a struct field rather than a
// top-level / local / parameter declaration. Drives the struct-tag
// rendering decision in VisitVariableDeclarations.
func (p *GoPrinter) insideStructType() bool {
	c := p.Cursor()
	if c == nil {
		return false
	}
	for cur := c.Parent(); cur != nil; cur = cur.Parent() {
		if _, ok := cur.Value().(*golang.StructType); ok {
			return true
		}
	}
	return false
}

func (p *GoPrinter) VisitUnary(unary *java.Unary, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(unary.Prefix, unary.Markers, out)
	if unary.Operator.Element == java.PostIncrement || unary.Operator.Element == java.PostDecrement || unary.Operator.Element == java.SpreadPostfix {
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

func goUnaryOperatorString(op golang.UnaryOperator) string {
	switch op {
	case golang.AddressOf:
		return "&"
	case golang.Indirection:
		return "*"
	case golang.Receive:
		return "<-"
	default:
		return "?"
	}
}

func (p *GoPrinter) VisitGoUnary(unary *golang.Unary, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(unary.Prefix, unary.Markers, out)
	p.visitSpace(unary.Operator.Before, out)
	out.Append(goUnaryOperatorString(unary.Operator.Element))
	p.Visit(unary.Expression, out)
	p.afterSyntax(unary.Markers, out)
	return unary
}

func (p *GoPrinter) VisitGoBinary(binary *golang.Binary, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(binary.Prefix, binary.Markers, out)
	p.Visit(binary.Left, out)
	p.visitSpace(binary.Operator.Before, out)
	if binary.Operator.Element == golang.BinAndNot {
		out.Append("&^")
	} else {
		out.Append("?")
	}
	p.Visit(binary.Right, out)
	p.afterSyntax(binary.Markers, out)
	return binary
}

func (p *GoPrinter) VisitGoAssignmentOperation(ao *golang.AssignmentOperation, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ao.Prefix, ao.Markers, out)
	p.Visit(ao.Variable, out)
	p.visitSpace(ao.Operator.Before, out)
	if ao.Operator.Element == golang.AssignAndNot {
		out.Append("&^=")
	} else {
		out.Append("?=")
	}
	p.Visit(ao.Assignment, out)
	p.afterSyntax(ao.Markers, out)
	return ao
}

func (p *GoPrinter) VisitGoVariadic(v *golang.Variadic, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(v.Prefix, v.Markers, out)
	if v.Postfix {
		// `args...` — element, then the ellipsis.
		p.Visit(v.Element, out)
		p.visitSpace(v.Dots, out)
		out.Append("...")
	} else {
		// `...T` — the ellipsis, then the element (its prefix is the gap).
		out.Append("...")
		p.Visit(v.Element, out)
	}
	p.afterSyntax(v.Markers, out)
	return v
}

func (p *GoPrinter) VisitBreak(b *java.Break, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(b.Prefix, b.Markers, out)
	out.Append("break")
	if b.Label != nil {
		p.Visit(b.Label, out)
	}
	p.afterSyntax(b.Markers, out)
	return b
}

func (p *GoPrinter) VisitContinue(c *java.Continue, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(c.Prefix, c.Markers, out)
	out.Append("continue")
	if c.Label != nil {
		p.Visit(c.Label, out)
	}
	p.afterSyntax(c.Markers, out)
	return c
}

func (p *GoPrinter) VisitLabel(l *java.Label, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(l.Prefix, l.Markers, out)
	p.Visit(l.Name.Element, out)
	p.visitSpace(l.Name.After, out)
	out.Append(":")
	p.Visit(l.Statement, out)
	p.afterSyntax(l.Markers, out)
	return l
}

func (p *GoPrinter) VisitGoStmt(g *golang.GoStmt, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(g.Prefix, g.Markers, out)
	out.Append("go")
	p.Visit(g.Expr, out)
	p.afterSyntax(g.Markers, out)
	return g
}

func (p *GoPrinter) VisitDefer(d *golang.Defer, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(d.Prefix, d.Markers, out)
	out.Append("defer")
	p.Visit(d.Expr, out)
	p.afterSyntax(d.Markers, out)
	return d
}

func (p *GoPrinter) VisitSend(s *golang.Send, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(s.Prefix, s.Markers, out)
	p.Visit(s.Channel, out)
	p.visitSpace(s.Arrow.Before, out)
	out.Append("<-")
	p.Visit(s.Arrow.Element, out)
	p.afterSyntax(s.Markers, out)
	return s
}

func (p *GoPrinter) VisitGoto(g *golang.Goto, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(g.Prefix, g.Markers, out)
	out.Append("goto")
	p.Visit(g.Label, out)
	p.afterSyntax(g.Markers, out)
	return g
}

func (p *GoPrinter) VisitFallthrough(f *golang.Fallthrough, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(f.Prefix, f.Markers, out)
	out.Append("fallthrough")
	p.afterSyntax(f.Markers, out)
	return f
}

func (p *GoPrinter) VisitArrayType(at *java.ArrayType, param any) java.J {
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

func (p *GoPrinter) VisitParentheses(paren *java.Parentheses, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(paren.Prefix, paren.Markers, out)
	out.Append("(")
	p.Visit(paren.Tree.Element, out)
	p.visitSpace(paren.Tree.After, out)
	out.Append(")")
	p.afterSyntax(paren.Markers, out)
	return paren
}

func (p *GoPrinter) VisitTypeCast(tc *java.TypeCast, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(tc.Prefix, tc.Markers, out)
	// Go type assertion: expr.(Type)
	p.Visit(tc.Expr, out)
	out.Append(".")
	p.Visit(tc.Clazz, out)
	p.afterSyntax(tc.Markers, out)
	return tc
}

func (p *GoPrinter) VisitControlParentheses(cp *java.ControlParentheses, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(cp.Prefix, cp.Markers, out)
	out.Append("(")
	p.Visit(cp.Tree.Element, out)
	p.visitSpace(cp.Tree.After, out)
	out.Append(")")
	p.afterSyntax(cp.Markers, out)
	return cp
}

func (p *GoPrinter) VisitArrayAccess(aa *java.ArrayAccess, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(aa.Prefix, aa.Markers, out)
	p.Visit(aa.Indexed, out)
	p.Visit(aa.Dimension, out)
	p.afterSyntax(aa.Markers, out)
	return aa
}

func (p *GoPrinter) VisitArrayDimension(ad *java.ArrayDimension, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ad.Prefix, ad.Markers, out)
	out.Append("[")
	p.Visit(ad.Index.Element, out)
	p.visitSpace(ad.Index.After, out)
	out.Append("]")
	p.afterSyntax(ad.Markers, out)
	return ad
}

func (p *GoPrinter) VisitIndexList(il *golang.IndexList, param any) java.J {
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

func (p *GoPrinter) VisitParameterizedType(pt *java.ParameterizedType, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(pt.Prefix, pt.Markers, out)
	p.Visit(pt.Clazz, out)
	if pt.TypeParameters != nil {
		p.visitSpace(pt.TypeParameters.Before, out)
		out.Append("[")
		for i, rp := range pt.TypeParameters.Elements {
			p.Visit(rp.Element, out)
			p.visitSpace(rp.After, out)
			if i < len(pt.TypeParameters.Elements)-1 {
				out.Append(",")
			}
		}
		out.Append("]")
	}
	p.afterSyntax(pt.Markers, out)
	return pt
}

func (p *GoPrinter) VisitComposite(c *golang.Composite, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(c.Prefix, c.Markers, out)
	if c.TypeExpr != nil {
		p.Visit(c.TypeExpr, out)
	}
	p.visitSpace(c.Elements.Before, out)
	out.Append("{")
	ctc := java.FindMarker[golang.TrailingComma](c.Markers)
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

func (p *GoPrinter) VisitKeyValue(kv *golang.KeyValue, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(kv.Prefix, kv.Markers, out)
	p.Visit(kv.Key, out)
	p.visitSpace(kv.Value.Before, out)
	out.Append(":")
	p.Visit(kv.Value.Element, out)
	p.afterSyntax(kv.Markers, out)
	return kv
}

func (p *GoPrinter) VisitSlice(s *golang.Slice, param any) java.J {
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

func (p *GoPrinter) VisitMapType(mt *golang.MapType, param any) java.J {
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

func (p *GoPrinter) VisitStatementExpression(se *golang.StatementExpression, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(se.Prefix, se.Markers, out)
	p.Visit(se.Statement, out)
	p.afterSyntax(se.Markers, out)
	return se
}

func (p *GoPrinter) VisitPointerType(pt *golang.PointerType, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(pt.Prefix, pt.Markers, out)
	out.Append("*")
	p.Visit(pt.Elem, out)
	p.afterSyntax(pt.Markers, out)
	return pt
}

func (p *GoPrinter) VisitChannel(ch *golang.Channel, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ch.Prefix, ch.Markers, out)

	var dirMarker java.Space
	if marker := java.FindMarker[golang.ChanDirMarker](ch.Markers); marker != nil {
		dirMarker = marker.Before
	}

	switch ch.Dir {
	case golang.ChanBidi:
		out.Append("chan")
	case golang.ChanSendOnly:
		out.Append("chan")
		p.visitSpace(dirMarker, out)
		out.Append("<-")
	case golang.ChanRecvOnly:
		out.Append("<-")
		p.visitSpace(dirMarker, out)
		out.Append("chan")
	}
	p.Visit(ch.Value, out)
	p.afterSyntax(ch.Markers, out)
	return ch
}

func (p *GoPrinter) VisitFuncType(ft *golang.FuncType, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ft.Prefix, ft.Markers, out)
	out.Append("func")
	p.printParamList(ft.Parameters, out)
	if ft.ReturnType != nil {
		p.Visit(ft.ReturnType, out)
	}
	p.afterSyntax(ft.Markers, out)
	return ft
}

func (p *GoPrinter) VisitUnion(u *golang.Union, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(u.Prefix, u.Markers, out)
	for i, rp := range u.Types {
		p.Visit(rp.Element, out)
		p.visitSpace(rp.After, out)
		if i < len(u.Types)-1 {
			out.Append("|")
		}
	}
	p.afterSyntax(u.Markers, out)
	return u
}

func (p *GoPrinter) VisitUnderlyingType(ut *golang.UnderlyingType, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(ut.Prefix, ut.Markers, out)
	out.Append("~")
	p.Visit(ut.Element, out)
	p.afterSyntax(ut.Markers, out)
	return ut
}

func (p *GoPrinter) VisitTypeList(tl *golang.TypeList, param any) java.J {
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

func (p *GoPrinter) VisitCommClause(cc *golang.CommClause, param any) java.J {
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

func (p *GoPrinter) VisitMultiAssignment(ma *golang.MultiAssignment, param any) java.J {
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
	if java.FindMarker[golang.ShortVarDecl](ma.Markers) != nil {
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

func (p *GoPrinter) VisitStructType(st *golang.StructType, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(st.Prefix, st.Markers, out)
	out.Append("struct")
	p.Visit(st.Body, out)
	p.afterSyntax(st.Markers, out)
	return st
}

func (p *GoPrinter) VisitInterfaceType(it *golang.InterfaceType, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(it.Prefix, it.Markers, out)
	out.Append("interface")
	p.Visit(it.Body, out)
	p.afterSyntax(it.Markers, out)
	return it
}

func (p *GoPrinter) VisitTypeDecl(td *golang.TypeDecl, param any) java.J {
	out := param.(*PrintOutputCapture)
	for _, ann := range td.LeadingAnnotations {
		p.visitSpace(ann.Prefix, out)
		out.Append("//")
		p.printDirectiveBody(ann, out)
	}
	p.beforeSyntax(td.Prefix, td.Markers, out)
	if java.FindMarker[golang.GroupedSpec](td.Markers) == nil {
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
		// Single: type Name[TypeParams] Type
		p.Visit(td.Name, out)
		if td.TypeParameters != nil {
			p.Visit(td.TypeParameters, out)
		}
		if td.Assign != nil {
			p.visitSpace(td.Assign.Before, out)
			out.Append("=")
		}
		p.Visit(td.Definition, out)
	}
	p.afterSyntax(td.Markers, out)
	return td
}

func (p *GoPrinter) VisitEmpty(empty *java.Empty, param any) java.J {
	out := param.(*PrintOutputCapture)
	p.beforeSyntax(empty.Prefix, empty.Markers, out)
	p.afterSyntax(empty.Markers, out)
	return empty
}

// visitSpace emits whitespace and comments to the output.
// Convention: Whitespace (before comments) is emitted first, then each comment
// with its suffix. This matches Java OpenRewrite's Space model.
func (p *GoPrinter) visitSpace(space java.Space, out *PrintOutputCapture) {
	out.Append(space.Whitespace)
	for _, comment := range space.Comments {
		out.Append(comment.Text)
		out.Append(comment.Suffix)
	}
}

func binaryOperatorString(op java.BinaryOperator) string {
	switch op {
	case java.Add:
		return "+"
	case java.Subtract:
		return "-"
	case java.Multiply:
		return "*"
	case java.Divide:
		return "/"
	case java.Modulo:
		return "%"
	case java.Equal:
		return "=="
	case java.NotEqual:
		return "!="
	case java.LessThan:
		return "<"
	case java.LessThanOrEqual:
		return "<="
	case java.GreaterThan:
		return ">"
	case java.GreaterThanOrEqual:
		return ">="
	case java.And, java.LogicalAnd:
		// Go's `&&`. The wire round-trip canonicalizes to the Java enum name
		// "And" (BinaryOperator.String()), which ParseBinaryOperator maps back
		// to `And` rather than `LogicalAnd`; handle both so a parsed tree and a
		// round-tripped tree print identically. Bitwise `&` is BitwiseAnd.
		return "&&"
	case java.Or, java.LogicalOr:
		// Go's `||`; see the `&&` note above. Bitwise `|` is BitwiseOr.
		return "||"
	case java.BitwiseAnd:
		return "&"
	case java.BitwiseOr:
		return "|"
	case java.BitwiseXor:
		return "^"
	case java.LeftShift:
		return "<<"
	case java.RightShift:
		return ">>"
	case java.AndNot:
		return "&^"
	default:
		return "?"
	}
}

func assignmentOperatorString(op java.AssignmentOperator) string {
	switch op {
	case java.AddAssign:
		return "+="
	case java.SubAssign:
		return "-="
	case java.MulAssign:
		return "*="
	case java.DivAssign:
		return "/="
	case java.ModAssign:
		return "%="
	case java.AndAssign:
		return "&="
	case java.OrAssign:
		return "|="
	case java.XorAssign:
		return "^="
	case java.ShlAssign:
		return "<<="
	case java.ShrAssign:
		return ">>="
	case java.AndNotAssign:
		return "&^="
	default:
		return "?="
	}
}

func unaryOperatorString(op java.UnaryOperator) string {
	switch op {
	case java.Negate:
		return "-"
	case java.Not:
		return "!"
	case java.BitwiseNot:
		return "^"
	case java.Deref:
		return "*"
	case java.AddressOf:
		return "&"
	case java.Receive:
		return "<-"
	case java.Positive:
		return "+"
	case java.PostIncrement:
		return "++"
	case java.PostDecrement:
		return "--"
	case java.Spread:
		return "..."
	case java.SpreadPostfix:
		return "..."
	case java.Tilde:
		return "~"
	default:
		return "?"
	}
}
