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
// GoMod prints to exactly the bytes it was parsed from.
func PrintGoMod(gm *golang.GoMod) string {
	out := NewPrintOutputCapture()
	printGoModSpace(gm.Prefix, out)
	for _, rp := range gm.Statements {
		printGoModStatement(rp.Element, out)
		printGoModSpace(rp.After, out)
	}
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
	printGoModSpace(d.Prefix, out)
	if d.Keyword != "" {
		out.Append(d.Keyword)
	}
	for _, v := range d.Values {
		printGoModSpace(v.Prefix, out)
		out.Append(v.Text)
	}
}

func printGoModBlock(b *golang.GoModBlock, out *PrintOutputCapture) {
	printGoModSpace(b.Prefix, out)
	out.Append(b.Keyword)
	printGoModSpace(b.BeforeLParen, out)
	out.Append("(")
	for _, rp := range b.Entries {
		printGoModStatement(rp.Element, out)
		printGoModSpace(rp.After, out)
	}
	printGoModSpace(b.BeforeRParen, out)
	out.Append(")")
}

func printGoModSpace(space java.Space, out *PrintOutputCapture) {
	out.Append(space.Whitespace)
	for _, comment := range space.Comments {
		out.Append(comment.Text)
		out.Append(comment.Suffix)
	}
}
