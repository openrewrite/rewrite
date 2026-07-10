/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
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
)

// PrintGoMod renders a GoMod LST back to source. Because every byte of
// whitespace and every comment is preserved on the tree, an unmodified
func PrintGoMod(gm *golang.GoMod) string {
	return printGoMod(gm, NewPrintOutputCapture())
}

// PrintGoModWithMarkers renders a GoMod LST to source, printing cross-cutting
// markers (SearchResult, Markup) via the given MarkerPrinter. With a nil-free
// marker printer that emits nothing for a node, the output is byte-identical
// to PrintGoMod — so search recipes can be tested with the same /*~~>*/
// convention used for .go sources.
func PrintGoModWithMarkers(gm *golang.GoMod, mp MarkerPrinter) string {
	return printGoMod(gm, NewPrintOutputCaptureWithMarkers(mp))
}

func printGoMod(gm *golang.GoMod, out *PrintOutputCapture) string {
	out.BeforePrefix(gm.Markers)
	printGoModSpace(gm.Prefix, out)
	out.BeforeSyntax(gm.Markers)
	for _, rp := range gm.Statements {
		printGoModStatement(rp.Element, out)
		printGoModSpace(rp.After, out)
	}
	out.AfterSyntax(gm.Markers)
	printGoModSpace(gm.Eof, out)
	return out.String()
}

func printGoModStatement(s golang.GoModStatement, out *PrintOutputCapture) {
	switch n := s.(type) {
	case *golang.GoModDirective:
		printGoModDirective(n, out)
	case *golang.GoModBlock:
		printGoModBlock(n, out)
	}
}

func printGoModDirective(d *golang.GoModDirective, out *PrintOutputCapture) {
	out.BeforePrefix(d.Markers)
	printGoModSpace(d.Prefix, out)
	out.BeforeSyntax(d.Markers)
	if d.Keyword != "" {
		out.Append(d.Keyword)
	}
	for _, v := range d.Values {
		printGoModValue(v, out)
	}
	out.AfterSyntax(d.Markers)
}

func printGoModValue(v *golang.GoModValue, out *PrintOutputCapture) {
	out.BeforePrefix(v.Markers)
	printGoModSpace(v.Prefix, out)
	out.BeforeSyntax(v.Markers)
	out.Append(v.Text)
	out.AfterSyntax(v.Markers)
}

func printGoModBlock(b *golang.GoModBlock, out *PrintOutputCapture) {
	out.BeforePrefix(b.Markers)
	printGoModSpace(b.Prefix, out)
	out.BeforeSyntax(b.Markers)
	out.Append(b.Keyword)
	printGoModSpace(b.BeforeLParen, out)
	out.Append("(")
	for _, rp := range b.Entries {
		printGoModStatement(rp.Element, out)
		printGoModSpace(rp.After, out)
	}
	printGoModSpace(b.BeforeRParen, out)
	out.Append(")")
	out.AfterSyntax(b.Markers)
}

func printGoModSpace(space java.Space, out *PrintOutputCapture) {
	out.Append(space.Whitespace)
	for _, comment := range space.Comments {
		out.Append(comment.Text)
		out.Append(comment.Suffix)
	}
}
