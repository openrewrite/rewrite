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
	depth := 0
	for i := len(chain) - 1; i > 0; i-- {
		depth += indentDelta(chain[i], chain[i-1])
	}
	return depth + indentDelta(chain[0], node)
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
// moves the list in a level, and the elements after it stay there.
func listDelta[T java.Tree](elements []java.RightPadded[T], child java.Tree) int {
	for _, rp := range elements {
		if breaksLine(getPrefix(rp.Element)) {
			return 1
		}
		if any(rp.Element) == any(child) {
			return 0
		}
	}
	return 0
}

// VisitCompilationUnit indents the specs of a parenthesized import
// declaration.
func (v *TabsAndIndentsVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	if cu.Imports != nil {
		grouped := java.HasMarker[golang.GroupedImport](cu.Imports.Markers)
		elements := make([]java.RightPadded[*java.Import], len(cu.Imports.Elements))
		for i, rp := range cu.Imports.Elements {
			if block := java.FindMarker[golang.ImportBlock](rp.Element.Markers); block != nil {
				grouped = block.Grouped
				block.Before = v.reindentSpace(block.Before)
			}
			if grouped {
				v.depth++
			}
			rp.Element = rp.Element.WithPrefix(v.reindentSpace(rp.Element.Prefix))
			if grouped {
				v.depth--
			}
			rp.After = v.reindentSpace(rp.After)
			elements[i] = rp
		}
		imports := *cu.Imports
		imports.Elements = elements
		cu = cu.WithImports(&imports)
	}
	return v.GoVisitor.VisitCompilationUnit(cu, p)
}

// VisitDeclarationBlock indents the specs of a `var (…)` or `const (…)` group.
func (v *TabsAndIndentsVisitor) VisitDeclarationBlock(db *golang.DeclarationBlock, p any) java.J {
	if db.Specs == nil {
		return v.GoVisitor.VisitDeclarationBlock(db, p)
	}
	out := *db
	out.Specs = v.indentSpecs(db.Specs, p)
	return &out
}

// VisitTypeDecl indents the specs of a `type (…)` group. An ungrouped
// declaration has no Specs and is indented by its enclosing block.
func (v *TabsAndIndentsVisitor) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	if td.Specs == nil {
		return v.GoVisitor.VisitTypeDecl(td, p)
	}
	out := *td
	out.Specs = v.indentSpecs(td.Specs, p)
	return &out
}

// VisitComposite indents the elements of a composite literal.
func (v *TabsAndIndentsVisitor) VisitComposite(c *golang.Composite, p any) java.J {
	out := *c
	// The type expression sits at the literal's own level — the fields of an
	// inline struct type line up with the elements, not one level further in.
	if c.TypeExpr != nil {
		if typeExpr, ok := v.Visit(c.TypeExpr, p).(java.Expression); ok {
			out.TypeExpr = typeExpr
		}
	}
	out.Elements.Elements = indentSubtrees(v, c.Elements.Elements, p)
	out.Markers = v.reindentTrailingComma(c.Markers)
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
	entries := append([]java.Marker(nil), m.Entries...)
	for i, e := range entries {
		if _, isComma := e.(golang.TrailingComma); isComma {
			entries[i] = updated
		}
	}
	return java.Markers{ID: m.ID, Entries: entries}
}

// indentSpecs indents the body of a parenthesized declaration group.
func (v *TabsAndIndentsVisitor) indentSpecs(c *java.Container[java.Statement], p any) *java.Container[java.Statement] {
	out := *c
	out.Elements = indentSubtrees(v, c.Elements, p)
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
	out := make([]java.RightPadded[T], len(elements))
	for i, rp := range elements {
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
		out[i] = rp
	}
	v.depth = outer
	return out
}

// VisitLabel sits a label one level out from the statements around it, and
// leaves the statement it labels — and any comment ahead of the label — at
// their level.
func (v *TabsAndIndentsVisitor) VisitLabel(l *java.Label, p any) java.J {
	out := l.WithPrefix(v.reindentClauseLeadIn(l.Prefix))

	copied := *out
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
	copied.Statement = statement
	return &copied
}

// VisitBlock dispatches the body at depth+1 and re-indents each
// statement's Prefix and the closing-brace `End`.
func (v *TabsAndIndentsVisitor) VisitBlock(block *java.Block, p any) java.J {
	v.depth++
	stmts := make([]java.RightPadded[java.Statement], len(block.Statements))
	for i, rp := range block.Statements {
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
		stmts[i] = rp
	}
	v.depth--

	block = block.WithStatements(stmts)
	block = block.WithEnd(v.reindentClosing(block.End))
	return block
}

// VisitCase indents a switch clause: the lead-in aligns the keyword with the
// enclosing switch, and the body one level in. It visits Body itself, which
// GoVisitor.VisitCase does not recurse into.
func (v *TabsAndIndentsVisitor) VisitCase(c *java.Case, p any) java.J {
	c = c.WithPrefix(v.reindentClauseLeadIn(c.Prefix))

	// Expressions that wrap sit one level in from the `case` keyword, which
	// is the depth the body already runs at.
	exprs := make([]java.RightPadded[java.Expression], len(c.Expressions.Elements))
	for i, rp := range c.Expressions.Elements {
		if fixed, ok := transformPrefix(rp.Element, v.reindentSpace).(java.Expression); ok {
			rp.Element = fixed
		}
		exprs[i] = rp
	}

	out := *c
	out.Expressions.Elements = exprs
	out.Body = indentBody(v, c.Body, p)
	return &out
}

// VisitMethodInvocation indents arguments that wrap onto their own lines.
func (v *TabsAndIndentsVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	out := *mi
	out.Arguments.Elements = indentSubtrees(v, mi.Arguments.Elements, p)
	out.Markers = v.reindentTrailingComma(mi.Markers)
	if mi.Select != nil {
		selected := *mi.Select
		if next, ok := v.Visit(selected.Element, p).(java.Expression); ok {
			selected.Element = next
			out.Select = &selected
		}
	}
	return &out
}

// VisitBinary indents a right operand that continues on its own line. Both
// operands sit at the same level however deeply the expression nests, so the
// depth is taken from the enclosing statement rather than from the operand.
func (v *TabsAndIndentsVisitor) VisitBinary(b *java.Binary, p any) java.J {
	out := *b
	out.Left = v.visitOperand(b.Left, false, p)
	out.Right = v.visitOperand(b.Right, true, p)
	return &out
}

// VisitGoBinary applies VisitBinary's rule to Go's own binary expressions.
func (v *TabsAndIndentsVisitor) VisitGoBinary(b *golang.Binary, p any) java.J {
	out := *b
	out.Left = v.visitOperand(b.Left, false, p)
	out.Right = v.visitOperand(b.Right, true, p)
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
// level in.
func (v *TabsAndIndentsVisitor) VisitCommClause(cc *golang.CommClause, p any) java.J {
	cc = cc.WithPrefix(v.reindentClauseLeadIn(cc.Prefix))

	out := *cc
	out.Body = indentBody(v, cc.Body, p)
	return v.GoVisitor.VisitCommClause(&out, p)
}

func indentBody(v *TabsAndIndentsVisitor, body []java.RightPadded[java.Statement], p any) []java.RightPadded[java.Statement] {
	out := make([]java.RightPadded[java.Statement], len(body))
	for i, rp := range body {
		if rp.Element != nil {
			if fixed, ok := transformPrefix(rp.Element, v.reindentSpace).(java.Statement); ok {
				rp.Element = fixed
			}
			if next, ok := v.Visit(rp.Element, p).(java.Statement); ok {
				rp.Element = next
			}
		}
		out[i] = rp
	}
	return out
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

	segments := make([]string, 0, len(s.Comments)+1)
	segments = append(segments, s.Whitespace)
	for _, c := range s.Comments {
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

	out := s
	out.Whitespace = segments[0]
	if len(s.Comments) > 0 {
		comments := append([]java.Comment(nil), s.Comments...)
		for i := range comments {
			comments[i].Suffix = segments[i+1]
		}
		out.Comments = comments
	}
	return out
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
	if len(s.Comments) == 0 {
		saved := v.depth
		v.depth = depth
		out := v.reindentSpace(s)
		v.depth = saved
		return out
	}
	inner := strings.Repeat("\t", depth+1)
	outer := strings.Repeat("\t", depth)

	s.Whitespace = reindentTail(s.Whitespace, inner)
	comments := append([]java.Comment(nil), s.Comments...)
	for i := range comments {
		indent := inner
		if i == len(comments)-1 {
			indent = outer
		}
		comments[i].Suffix = reindentTail(comments[i].Suffix, indent)
	}
	s.Comments = comments
	return s
}

// reindentSpace rewrites to `\t × v.depth` the indent of everything s
// introduces: its comments, and the syntax that follows them. A Space prints
// as Whitespace, then each comment followed by its Suffix, so every one of
// those segments ends in the indent of whatever comes next.
func (v *TabsAndIndentsVisitor) reindentSpace(s java.Space) java.Space {
	want := strings.Repeat("\t", v.depth)
	s.Whitespace = reindentTail(s.Whitespace, want)

	var comments []java.Comment
	for i, c := range s.Comments {
		suffix := reindentTail(c.Suffix, want)
		if suffix == c.Suffix {
			continue
		}
		if comments == nil {
			comments = append([]java.Comment(nil), s.Comments...)
		}
		comments[i].Suffix = suffix
	}
	if comments != nil {
		s.Comments = comments
	}
	return s
}

// breaksLine reports whether s puts what follows it on a new line. The break
// can sit in the whitespace or in the suffix of a comment s carries, as it does
// when the previous line ends in a trailing comment.
func breaksLine(s java.Space) bool {
	if strings.Contains(s.Whitespace, "\n") {
		return true
	}
	for _, c := range s.Comments {
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
