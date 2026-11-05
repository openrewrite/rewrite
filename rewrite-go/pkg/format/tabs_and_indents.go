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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TabsAndIndentsVisitor re-indents to gofmt's `\t × depth` convention.
//
// Indentation is driven from the constructs that introduce a level — blocks,
// clause bodies, and delimited lists that wrap across lines — rather than from
// every Space in the tree, which would also rewrite the continuation alignment
// inside multi-line expressions.
type TabsAndIndentsVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
	depth  int
	seeded bool
}

func NewTabsAndIndentsVisitor(stopAfter java.Tree) *TabsAndIndentsVisitor {
	return visitor.Init(&TabsAndIndentsVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *TabsAndIndentsVisitor) Visit(t java.Tree, p any) java.Tree {
	if v.shouldHalt() {
		return t
	}
	// The level a subtree sits at is set by the ancestors above it, which a
	// visit starting below the file only knows through a seeded cursor.
	if !v.seeded {
		v.seeded = true
		v.depth = depthAt(v.Cursor(), t)
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

// depthAt returns the level a node sits at, given the cursor of its parent.
func depthAt(parent *visitor.Cursor, node java.Tree) int {
	if parent == nil {
		return 0
	}
	var chain []java.Tree
	for c := parent; c != nil; c = c.Parent() {
		chain = append(chain, c.Value())
	}
	// A clause's expressions step out a level, to its `case` keyword, but never
	// past the margin, where reduceIndentDepth stops the keyword too.
	depth := 0
	for i := len(chain) - 1; i > 0; i-- {
		depth = max(0, depth+indentDelta(chain[i], chain[i-1]))
	}
	return max(0, depth+indentDelta(chain[0], node))
}

// indentDelta reports how many levels child sits in from parent, mirroring what
// the traversal below accumulates in v.depth on its way down. The two stay in
// step by test: TestSubtreeIndentMatchesWholeFile checks every node of a corpus.
func indentDelta(parent, child java.Tree) int {
	switch p := parent.(type) {
	case *java.Block:
		return 1
	case *java.Binary:
		return operandDelta(p.Right, child)
	case *golang.Binary:
		return operandDelta(p.Right, child)
	case *golang.DeclarationBlock:
		return specsDelta(p.Specs, child)
	case *golang.TypeDecl:
		return specsDelta(p.Specs, child)
	case *golang.Composite:
		return listDelta(p.Elements.Elements, child)
	case *java.MethodInvocation:
		return listDelta(p.Arguments.Elements, child)
	case *java.TypeCast:
		if p.Clazz == nil {
			return 0
		}
		return listDelta([]java.RightPadded[java.Expression]{
			{Element: p.Expr, After: p.Clazz.Tree.After},
		}, child)
	case *java.Case:
		// A clause's expressions start at its keyword's level, one out from
		// its body.
		for _, rp := range p.Expressions.Elements {
			if java.Tree(rp.Element) == child {
				return listDelta(p.Expressions.Elements, child) - 1
			}
		}
	case *golang.CommClause:
		if p.Comm != nil && java.Tree(p.Comm) == child {
			return -1
		}
	}
	return 0
}

// operandDelta mirrors visitOperand: only a right operand continuing on its own
// line moves in a level.
func operandDelta(right java.Expression, child java.Tree) int {
	if right != nil && java.Tree(right) == child && breaksLine(getPrefix(child)) {
		return 1
	}
	return 0
}

func specsDelta(specs *java.Container[java.Statement], child java.Tree) int {
	if specs == nil {
		return 0
	}
	return listDelta(specs.Elements, child)
}

// listDelta mirrors eachElement: the first element to start a line of its own
// moves the list in a level, and the elements after it stay there. A child that
// is not in the list at all sits outside it, as a composite literal's type
// expression and a method invocation's select do.
func listDelta[T java.Tree](elements []java.RightPadded[T], child java.Tree) int {
	delta := 0
	for _, rp := range elements {
		if breaksLine(getPrefix(rp.Element)) {
			delta = 1
		}
		if any(rp.Element) == any(child) {
			return delta
		}
	}
	return 0
}

// VisitCompilationUnit sets the package clause, the import declarations and
// the other declarations of the file at the left margin, and indents the specs
// of a parenthesized import declaration.
func (v *TabsAndIndentsVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = cu.WithPrefix(v.reindentFileStart(cu.Prefix))
	if cu.Imports != nil {
		leadIn := v.reindentSpace(cu.Imports.Before)
		grouped := java.HasMarker[golang.GroupedImport](cu.Imports.Markers)
		elements := eachPadded(cu.Imports.Elements, func(rp java.RightPadded[*java.Import]) java.RightPadded[*java.Import] {
			if block := java.FindMarker[golang.ImportBlock](rp.Element.Markers); block != nil {
				grouped = block.Grouped
				if before := v.reindentSpace(block.Before); !java.SpaceEqual(before, block.Before) {
					reindented := *block
					reindented.Before = before
					rp.Element = rp.Element.WithMarkers(withMarker(rp.Element.Markers, reindented))
				}
			}
			if grouped {
				v.depth++
			}
			rp.Element = rp.Element.WithPrefix(v.reindentSpace(rp.Element.Prefix))
			if grouped {
				v.depth--
			}
			rp.After = v.reindentSpace(rp.After)
			return rp
		})
		if !java.SpaceEqual(leadIn, cu.Imports.Before) || !java.SameSlice(elements, cu.Imports.Elements) {
			imports := *cu.Imports
			imports.Before = leadIn
			imports.Elements = elements
			cu = cu.WithImports(&imports)
		}
	}
	cu = cu.WithStatements(eachPadded(cu.Statements, func(rp java.RightPadded[java.Statement]) java.RightPadded[java.Statement] {
		if fixed, ok := transformPrefix(rp.Element, v.reindentSpace).(java.Statement); ok {
			rp.Element = fixed
		}
		return rp
	}))
	// A comment closing the file is left where it is: gofmt keeps one that
	// follows a wrapped expression at the expression's indent.
	return v.GoVisitor.VisitCompilationUnit(cu, p)
}

// reindentFileStart re-indents the space a file opens with, whose whitespace
// begins the first line rather than following a line break.
func (v *TabsAndIndentsVisitor) reindentFileStart(s java.Space) java.Space {
	out := v.reindentSpace(s)
	if !strings.Contains(out.Whitespace(), "\n") {
		out = java.MakeSpace(out.Comments(), strings.Repeat("\t", v.depth))
	}
	return unchangedOr(s, out)
}

// reindentDirectives re-indents the `//go:` directives the parser lifts out of
// a declaration's prefix into its leading annotations, which print ahead of it.
func (v *TabsAndIndentsVisitor) reindentDirectives(anns []*java.Annotation) []*java.Annotation {
	var out []*java.Annotation
	for i, a := range anns {
		prefix := v.reindentSpace(a.Prefix)
		if spaceContentEqual(prefix, a.Prefix) {
			continue
		}
		if out == nil {
			out = append([]*java.Annotation(nil), anns...)
		}
		moved := *a
		moved.Prefix = prefix
		out[i] = &moved
	}
	if out == nil {
		return anns
	}
	return out
}

// VisitMethodDeclaration sets a func's directives at its level.
func (v *TabsAndIndentsVisitor) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	md = md.WithLeadingAnnotations(v.reindentDirectives(md.LeadingAnnotations))
	return v.GoVisitor.VisitMethodDeclaration(md, p)
}

// VisitVariableDeclarations sets a var or const declaration's directives at its
// level. A struct field's annotations are its tag, which is left as written.
func (v *TabsAndIndentsVisitor) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	if java.HasMarker[golang.VarKeyword](vd.Markers) || java.HasMarker[golang.ConstDecl](vd.Markers) {
		vd = vd.WithLeadingAnnotations(v.reindentDirectives(vd.LeadingAnnotations))
	}
	return v.GoVisitor.VisitVariableDeclarations(vd, p)
}

// VisitDeclarationBlock sets a `var (…)` or `const (…)` group's directives at
// its level, and indents its specs.
func (v *TabsAndIndentsVisitor) VisitDeclarationBlock(db *golang.DeclarationBlock, p any) java.J {
	db = db.WithLeadingAnnotations(v.reindentDirectives(db.LeadingAnnotations))
	if db.Specs == nil {
		return v.GoVisitor.VisitDeclarationBlock(db, p)
	}
	specs := v.indentSpecs(db.Specs, p)
	if specs == db.Specs {
		return db
	}
	out := *db
	out.Specs = specs
	return &out
}

// VisitTypeDecl sets a type declaration's directives at its level, and indents
// the specs of a `type (…)` group. An ungrouped declaration has no Specs and is
// indented by its enclosing block.
func (v *TabsAndIndentsVisitor) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	td = td.WithLeadingAnnotations(v.reindentDirectives(td.LeadingAnnotations))
	if td.Specs == nil {
		return v.GoVisitor.VisitTypeDecl(td, p)
	}
	specs := v.indentSpecs(td.Specs, p)
	if specs == td.Specs {
		return td
	}
	out := *td
	out.Specs = specs
	return &out
}

// VisitComposite indents the elements of a composite literal.
func (v *TabsAndIndentsVisitor) VisitComposite(c *golang.Composite, p any) java.J {
	typeExpr := c.TypeExpr
	// The type expression sits at the literal's own level — the fields of an
	// inline struct type line up with the elements, not one level further in.
	if typeExpr != nil {
		if next, ok := v.Visit(typeExpr, p).(java.Expression); ok {
			typeExpr = next
		}
	}
	elements := indentSubtrees(v, c.Elements.Elements, p)
	markers := v.reindentTrailingComma(c.Markers)
	if typeExpr == c.TypeExpr && java.SameSlice(elements, c.Elements.Elements) && java.MarkersEqual(markers, c.Markers) {
		return c
	}
	out := *c
	out.TypeExpr = typeExpr
	out.Elements.Elements = elements
	out.Markers = markers
	return &out
}

// reindentTrailingComma re-indents the closing delimiter of a list that ends
// in a comma, where the space between the two rides on the marker rather than
// on any element.
func (v *TabsAndIndentsVisitor) reindentTrailingComma(m java.Markers) java.Markers {
	comma := java.FindMarker[golang.TrailingComma](m)
	if comma == nil {
		return m
	}
	after := v.reindentClosing(comma.After)
	if java.SpaceEqual(after, comma.After) {
		return m
	}
	updated := *comma
	updated.After = after
	entries := append([]java.Marker(nil), m.Entries()...)
	for i, e := range entries {
		if _, isComma := e.(golang.TrailingComma); isComma {
			entries[i] = updated
		}
	}
	return java.MakeMarkers(m.GetID(), entries)
}

// indentSpecs indents the body of a parenthesized declaration group.
func (v *TabsAndIndentsVisitor) indentSpecs(c *java.Container[java.Statement], p any) *java.Container[java.Statement] {
	elements := indentSubtrees(v, c.Elements, p)
	if java.SameSlice(elements, c.Elements) {
		return c
	}
	out := *c
	out.Elements = elements
	return &out
}

// indentElements indents the elements of a delimited list.
func indentElements[T java.Tree](v *TabsAndIndentsVisitor, elements []java.RightPadded[T]) []java.RightPadded[T] {
	return eachElement(v, elements, nil, false)
}

// indentSubtrees indents a delimited list and descends into each element at the
// element's own level, so a list nested inside one lands a level further in.
func indentSubtrees[T java.Tree](v *TabsAndIndentsVisitor, elements []java.RightPadded[T], p any) []java.RightPadded[T] {
	return eachElement(v, elements, p, true)
}

// eachElement moves each element of a delimited list one level in and the
// closing delimiter back out. An element's After holds a line break only in
// that closing position, so treating every After as a closing one leaves the
// ones between elements untouched.
func eachElement[T java.Tree](v *TabsAndIndentsVisitor, elements []java.RightPadded[T], p any, descend bool) []java.RightPadded[T] {
	outer := v.depth
	out := eachPadded(elements, func(rp java.RightPadded[T]) java.RightPadded[T] {
		// Elements written alongside the delimiter that opens the list stay on
		// that line's level; from the first element to start a line of its own,
		// the rest share the level it set.
		if breaksLine(getPrefix(rp.Element)) {
			v.depth = outer + 1
			if fixed, ok := any(transformPrefix(rp.Element, v.reindentSpace)).(T); ok {
				rp.Element = fixed
			}
		}
		if descend {
			if next, ok := any(v.Visit(rp.Element, p)).(T); ok {
				rp.Element = next
			}
		}
		rp.After = v.reindentClosingAt(rp.After, outer)
		return rp
	})
	v.depth = outer
	return out
}

// VisitLabel sits a label one level out from the statements around it, and
// leaves the statement it labels — and any comment ahead of the label — at
// their level.
func (v *TabsAndIndentsVisitor) VisitLabel(l *java.Label, p any) java.J {
	out := l.WithPrefix(v.reindentClauseLeadIn(l.Prefix))

	statement := l.Statement
	// An empty labelled statement prints nothing, so the whitespace ahead of it
	// closes the block rather than opening a line of its own.
	if _, empty := statement.(*java.Empty); !empty {
		if fixed, ok := transformPrefix(statement, v.reindentSpace).(java.Statement); ok {
			statement = fixed
		}
	}
	if next, ok := v.Visit(statement, p).(java.Statement); ok {
		statement = next
	}
	statement = unlessLabelUnchanged(l.Statement, statement)
	if statement == out.Statement {
		return out
	}
	copied := *out
	copied.Statement = statement
	return &copied
}

// VisitBlock dispatches the body at depth+1 and re-indents each
// statement's Prefix and the closing-brace `End`.
func (v *TabsAndIndentsVisitor) VisitBlock(block *java.Block, p any) java.J {
	v.depth++
	stmts := eachPadded(block.Statements, func(rp java.RightPadded[java.Statement]) java.RightPadded[java.Statement] {
		if rp.Element != nil {
			// A clause aligns with the switch or select that encloses it and
			// decides its own lead-in, so its prefix is left to it.
			if !ownsItsLeadIn(rp.Element) {
				if fixed, ok := transformPrefix(rp.Element, v.reindentSpace).(java.Statement); ok {
					rp.Element = fixed
				}
			}
			if next, ok := v.Visit(rp.Element, p).(java.Statement); ok {
				rp.Element = next
			}
		}
		return rp
	})
	v.depth--

	block = block.WithStatements(stmts)
	block = block.WithEnd(v.reindentClosing(block.End))
	return block
}

// VisitCase indents a switch clause: the lead-in aligns the keyword with the
// enclosing switch, and the body one level in. It visits Body itself rather
// than through GoVisitor.VisitCase, so as to re-indent each statement before
// descending into it.
func (v *TabsAndIndentsVisitor) VisitCase(c *java.Case, p any) java.J {
	c = c.WithPrefix(v.reindentClauseLeadIn(c.Prefix))

	// The expressions nest from the `case` keyword's level, and those that
	// wrap sit one level in from it, which is the depth the body runs at.
	exprs := atKeywordLevel(v, func() []java.RightPadded[java.Expression] {
		return indentSubtrees(v, c.Expressions.Elements, p)
	})
	body := indentBody(v, c.Body.Elements, p)
	if java.SameSlice(exprs, c.Expressions.Elements) && java.SameSlice(body, c.Body.Elements) {
		return c
	}
	out := *c
	out.Expressions.Elements = exprs
	out.Body.Elements = body
	return &out
}

// VisitMethodInvocation indents arguments that wrap onto their own lines.
func (v *TabsAndIndentsVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	arguments := indentSubtrees(v, mi.Arguments.Elements, p)
	markers := v.reindentTrailingComma(mi.Markers)
	selected := mi.Select
	if selected != nil {
		if next, ok := v.Visit(selected.Element, p).(java.Expression); ok && next != selected.Element {
			copied := *selected
			copied.Element = next
			selected = &copied
		}
	}
	if java.SameSlice(arguments, mi.Arguments.Elements) && java.MarkersEqual(markers, mi.Markers) && selected == mi.Select {
		return mi
	}
	out := *mi
	out.Arguments.Elements = arguments
	out.Markers = markers
	out.Select = selected
	return &out
}

// VisitTypeCast indents a conversion's operand that wraps onto its own line,
// the way a wrapped argument indents.
func (v *TabsAndIndentsVisitor) VisitTypeCast(tc *java.TypeCast, p any) java.J {
	if tc.Clazz == nil {
		return v.GoVisitor.VisitTypeCast(tc, p)
	}
	clazz := tc.Clazz
	if next, ok := v.Visit(tc.Clazz, p).(*java.ControlParentheses); ok {
		clazz = next
	}
	operand := indentSubtrees(v, []java.RightPadded[java.Expression]{
		{Element: tc.Expr, After: clazz.Tree.After},
	}, p)
	if !java.SpaceEqual(operand[0].After, clazz.Tree.After) {
		copied := *clazz
		copied.Tree.After = operand[0].After
		clazz = &copied
	}
	markers := v.reindentTrailingComma(tc.Markers)
	if clazz == tc.Clazz && operand[0].Element == tc.Expr && java.MarkersEqual(markers, tc.Markers) {
		return tc
	}
	out := *tc
	out.Clazz = clazz
	out.Expr = operand[0].Element
	out.Markers = markers
	return &out
}

// VisitBinary indents a right operand that continues on its own line. Both
// operands sit at the same level however deeply the expression nests, so the
// depth is taken from the enclosing statement rather than from the operand.
func (v *TabsAndIndentsVisitor) VisitBinary(b *java.Binary, p any) java.J {
	left := v.visitOperand(b.Left, false, p)
	right := v.visitOperand(b.Right, true, p)
	return b.WithLeft(left).WithRight(right)
}

// VisitGoBinary applies VisitBinary's rule to Go's own binary expressions.
func (v *TabsAndIndentsVisitor) VisitGoBinary(b *golang.Binary, p any) java.J {
	left := v.visitOperand(b.Left, false, p)
	right := v.visitOperand(b.Right, true, p)
	if left == b.Left && right == b.Right {
		return b
	}
	out := *b
	out.Left = left
	out.Right = right
	return &out
}

// visitOperand indents an operand that continues on its own line, and descends
// into it at that level so an operand wrapping inside one lands a level
// further in. Only the right-hand operand can start a continuation line; the
// left one opens the expression.
func (v *TabsAndIndentsVisitor) visitOperand(e java.Expression, continues bool, p any) java.Expression {
	if e == nil {
		return nil
	}
	wraps := continues && breaksLine(getPrefix(e))
	if wraps {
		v.depth++
		if fixed, ok := transformPrefix(e, v.reindentSpace).(java.Expression); ok {
			e = fixed
		}
	}
	if next, ok := v.Visit(e, p).(java.Expression); ok {
		e = next
	}
	if wraps {
		v.depth--
	}
	return e
}

// VisitCommClause aligns a select clause the way VisitCase aligns a switch
// clause: the `case` keyword sits with the enclosing `select`, its body one
// level in. Like VisitCase, it visits the clause's parts itself, in the order
// they are written.
func (v *TabsAndIndentsVisitor) VisitCommClause(cc *golang.CommClause, p any) java.J {
	cc = cc.WithPrefix(v.reindentClauseLeadIn(cc.Prefix))

	comm := cc.Comm
	if comm != nil {
		comm = atKeywordLevel(v, func() java.Statement {
			if next, ok := v.Visit(comm, p).(java.Statement); ok {
				return next
			}
			return comm
		})
	}
	body := indentBody(v, cc.Body, p)
	if comm == cc.Comm && java.SameSlice(body, cc.Body) {
		return cc
	}
	out := *cc
	out.Comm = comm
	out.Body = body
	return &out
}

// atKeywordLevel runs visit at the level of a clause's `case` keyword, one out
// from the body the clause's depth is set for.
func atKeywordLevel[T any](v *TabsAndIndentsVisitor, visit func() T) T {
	body := v.depth
	v.depth = reduceIndentDepth(body)
	defer func() { v.depth = body }()
	return visit()
}

func indentBody(v *TabsAndIndentsVisitor, body []java.RightPadded[java.Statement], p any) []java.RightPadded[java.Statement] {
	return eachPadded(body, func(rp java.RightPadded[java.Statement]) java.RightPadded[java.Statement] {
		if rp.Element != nil {
			original := rp.Element
			if fixed, ok := transformPrefix(rp.Element, v.reindentSpace).(java.Statement); ok {
				rp.Element = fixed
			}
			if next, ok := v.Visit(rp.Element, p).(java.Statement); ok {
				rp.Element = next
			}
			rp.Element = unlessLabelUnchanged(original, rp.Element)
		}
		return rp
	})
}

// unlessLabelUnchanged returns original when it is a label whose lead-in,
// indented as a statement and then back out by VisitLabel, ends as it began.
func unlessLabelUnchanged(original, formatted java.Statement) java.Statement {
	before, ok := original.(*java.Label)
	if !ok {
		return formatted
	}
	after, ok := formatted.(*java.Label)
	if ok && after.Statement == before.Statement && spaceContentEqual(after.Prefix, before.Prefix) {
		return original
	}
	return formatted
}

// ownsItsLeadIn reports whether a statement re-indents the whitespace ahead of
// itself rather than leaving that to the block holding it.
func ownsItsLeadIn(t java.Tree) bool {
	switch t.(type) {
	case *java.Case, *golang.CommClause, *java.Label:
		return true
	}
	return false
}

// reindentClauseLeadIn re-indents what runs up to a keyword that sits one level
// out from the statements around it — `case`, `default`, a label. A comment
// ahead of the keyword belongs to those statements and is indented with them,
// unless it was written at the keyword's own level, where it reads as
// introducing what follows and stays.
func (v *TabsAndIndentsVisitor) reindentClauseLeadIn(s java.Space) java.Space {
	body := strings.Repeat("\t", v.depth)
	keyword := strings.Repeat("\t", reduceIndentDepth(v.depth))

	segments := make([]string, 0, len(s.Comments())+1)
	segments = append(segments, s.Whitespace())
	for _, c := range s.Comments() {
		segments = append(segments, c.Suffix)
	}
	for i, segment := range segments {
		want := body
		if i == len(segments)-1 {
			want = keyword
		} else if indentOf(segment) == keyword {
			continue
		}
		segments[i] = reindentTail(segment, want)
	}

	comments := s.Comments()
	if len(comments) > 0 {
		comments = append([]java.Comment(nil), comments...)
		for i := range comments {
			comments[i].Suffix = segments[i+1]
		}
	}
	return unchangedOr(s, java.MakeSpace(comments, segments[0]))
}

// indentOf reports the whitespace after the last line break in ws, which is the
// indent of whatever follows it.
func indentOf(ws string) string {
	if i := strings.LastIndex(ws, "\n"); i >= 0 {
		return ws[i+1:]
	}
	return ws
}

func reduceIndentDepth(depth int) int {
	if depth <= 1 {
		return 0
	}
	return depth - 1
}

// reindentClosing re-indents the whitespace before a closing delimiter. Any
// comments in it trail the body and so sit one level in, while the delimiter
// itself returns to the outer depth.
func (v *TabsAndIndentsVisitor) reindentClosing(s java.Space) java.Space {
	return v.reindentClosingAt(s, v.depth)
}

// reindentClosingAt re-indents a closing delimiter that sits at the given
// depth, whatever level the elements ahead of it reached.
func (v *TabsAndIndentsVisitor) reindentClosingAt(s java.Space, depth int) java.Space {
	if len(s.Comments()) == 0 {
		saved := v.depth
		v.depth = depth
		out := v.reindentSpace(s)
		v.depth = saved
		return out
	}
	inner := strings.Repeat("\t", depth+1)
	outer := strings.Repeat("\t", depth)

	whitespace := reindentTail(s.Whitespace(), inner)
	comments := append([]java.Comment(nil), s.Comments()...)
	for i := range comments {
		indent := inner
		if i == len(comments)-1 {
			indent = outer
		}
		comments[i].Suffix = reindentTail(comments[i].Suffix, indent)
	}
	return unchangedOr(s, java.MakeSpace(comments, whitespace))
}

// reindentSpace rewrites to `\t × v.depth` the indent of everything s
// introduces: its comments, and the syntax that follows them. A Space prints
// as Whitespace, then each comment followed by its Suffix, so every one of
// those segments ends in the indent of whatever comes next.
func (v *TabsAndIndentsVisitor) reindentSpace(s java.Space) java.Space {
	want := strings.Repeat("\t", v.depth)
	whitespace := reindentTail(s.Whitespace(), want)

	comments := s.Comments()
	var newComments []java.Comment
	for i, c := range comments {
		suffix := reindentTail(c.Suffix, want)
		if suffix == c.Suffix {
			continue
		}
		if newComments == nil {
			newComments = append([]java.Comment(nil), comments...)
		}
		newComments[i].Suffix = suffix
	}
	if newComments != nil {
		comments = newComments
	}
	return java.MakeSpace(comments, whitespace)
}

// breaksLine reports whether s puts what follows it on a new line. The break
// can sit in the whitespace or in the suffix of a comment s carries, as it does
// when the previous line ends in a trailing comment.
func breaksLine(s java.Space) bool {
	if strings.Contains(s.Whitespace(), "\n") {
		return true
	}
	for _, c := range s.Comments() {
		if strings.Contains(c.Suffix, "\n") {
			return true
		}
	}
	return false
}

// reindentTail rewrites the run of whitespace following the last newline in
// ws, which is the indent of whatever comes next. Whitespace holding no
// newline sits mid-line and is returned unchanged, which is what keeps a
// trailing comment beside the code it follows. The portion before the last
// newline can hold blank lines and is preserved — BlankLinesVisitor owns that.
func reindentTail(ws, indent string) string {
	last := strings.LastIndex(ws, "\n")
	if last < 0 || ws[last+1:] == indent {
		return ws
	}
	return ws[:last+1] + indent
}
