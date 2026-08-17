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
	depth int
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
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

// VisitCompilationUnit indents the specs of a parenthesized import
// declaration one level in, and the whitespace before its closing paren back
// out to file level.
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
	if db.Specs != nil {
		c := *db
		c.Specs = v.indentSpecs(db.Specs)
		db = &c
	}
	return v.GoVisitor.VisitDeclarationBlock(db, p)
}

// VisitTypeDecl indents the specs of a `type (…)` group. An ungrouped
// declaration has no Specs and is indented by its enclosing block.
func (v *TabsAndIndentsVisitor) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	if td.Specs != nil {
		c := *td
		c.Specs = v.indentSpecs(td.Specs)
		td = &c
	}
	return v.GoVisitor.VisitTypeDecl(td, p)
}

// VisitComposite indents the elements of a composite literal that spans
// lines, and the whitespace before its closing brace back out.
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
	after := v.reindentSpace(comma.After)
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
func (v *TabsAndIndentsVisitor) indentSpecs(c *java.Container[java.Statement]) *java.Container[java.Statement] {
	out := *c
	out.Elements = indentElements(v, c.Elements)
	return &out
}

// indentElements moves each element of a delimited list one level in, and the
// whitespace before the closing delimiter back out. An element's After holds a
// newline only in that closing position, so re-indenting every After at the
// outer depth leaves the ones between elements untouched.
func indentElements[T java.Tree](v *TabsAndIndentsVisitor, elements []java.RightPadded[T]) []java.RightPadded[T] {
	return eachElement(v, elements, nil, false)
}

// indentSubtrees indents a delimited list and descends into each element at the
// element's own level, so a list nested inside one lands a level further in.
func indentSubtrees[T java.Tree](v *TabsAndIndentsVisitor, elements []java.RightPadded[T], p any) []java.RightPadded[T] {
	return eachElement(v, elements, p, true)
}

func eachElement[T java.Tree](v *TabsAndIndentsVisitor, elements []java.RightPadded[T], p any, descend bool) []java.RightPadded[T] {
	out := make([]java.RightPadded[T], len(elements))
	for i, rp := range elements {
		// A list contributes a level only where it breaks the line. An element
		// written alongside the delimiter that opens the list stays on that
		// line's level, and whatever nests inside it counts from there.
		if breaksLine(getPrefix(rp.Element)) {
			v.depth++
			if fixed, ok := any(transformPrefix(rp.Element, v.reindentSpace)).(T); ok {
				rp.Element = fixed
			}
			if descend {
				if next, ok := any(v.Visit(rp.Element, p)).(T); ok {
					rp.Element = next
				}
			}
			v.depth--
		} else if descend {
			if next, ok := any(v.Visit(rp.Element, p)).(T); ok {
				rp.Element = next
			}
		}
		rp.After = v.reindentSpace(rp.After)
		out[i] = rp
	}
	return out
}

// VisitBlock dispatches the body at depth+1 and re-indents each
// statement's Prefix and the closing-brace `End`.
func (v *TabsAndIndentsVisitor) VisitBlock(block *java.Block, p any) java.J {
	v.depth++
	stmts := make([]java.RightPadded[java.Statement], len(block.Statements))
	for i, rp := range block.Statements {
		if rp.Element != nil {
			fixed, _ := transformPrefix(rp.Element, v.reindentSpace).(java.Statement)
			if fixed != nil {
				rp.Element = fixed
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

// VisitCase aligns the case keyword with the enclosing switch (one
// tab less than the switch body's depth) while keeping the case body
// statements at body depth. Also explicitly visits Body so nested
// blocks inside a case get their own indent fixes (the default
// GoVisitor.VisitCase doesn't recurse into Body).
func (v *TabsAndIndentsVisitor) VisitCase(c *java.Case, p any) java.J {
	v.depth--
	c = c.WithPrefix(v.reindentSpace(c.Prefix))
	v.depth++

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

// VisitMethodInvocation indents arguments that wrap onto their own lines, and
// the whitespace before the closing paren back out to the call's own level.
func (v *TabsAndIndentsVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	out := *mi
	out.Arguments.Elements = indentElements(v, mi.Arguments.Elements)
	out.Markers = v.reindentTrailingComma(mi.Markers)
	return v.GoVisitor.VisitMethodInvocation(&out, p)
}

// VisitBinary indents a right operand that continues on its own line. Both
// operands sit at the same level however deeply the expression nests, so the
// depth is taken from the enclosing statement rather than from the operand.
func (v *TabsAndIndentsVisitor) VisitBinary(b *java.Binary, p any) java.J {
	out := *b
	v.depth++
	if right, ok := transformPrefix(b.Right, v.reindentSpace).(java.Expression); ok {
		out.Right = right
	}
	v.depth--
	return v.GoVisitor.VisitBinary(&out, p)
}

// VisitGoBinary applies VisitBinary's rule to Go's own binary expressions.
func (v *TabsAndIndentsVisitor) VisitGoBinary(b *golang.Binary, p any) java.J {
	out := *b
	v.depth++
	if right, ok := transformPrefix(b.Right, v.reindentSpace).(java.Expression); ok {
		out.Right = right
	}
	v.depth--
	return v.GoVisitor.VisitGoBinary(&out, p)
}

// VisitCommClause aligns a select clause the way VisitCase aligns a switch
// clause: the `case` keyword sits with the enclosing `select`, its body one
// level in.
func (v *TabsAndIndentsVisitor) VisitCommClause(cc *golang.CommClause, p any) java.J {
	v.depth--
	cc = cc.WithPrefix(v.reindentSpace(cc.Prefix))
	v.depth++

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

// reindentClosing re-indents the whitespace before a closing delimiter. Any
// comments in it trail the body and so sit one level in, while the delimiter
// itself returns to the outer depth.
func (v *TabsAndIndentsVisitor) reindentClosing(s java.Space) java.Space {
	if len(s.Comments) == 0 {
		return v.reindentSpace(s)
	}
	inner := strings.Repeat("\t", v.depth+1)
	outer := strings.Repeat("\t", v.depth)

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
