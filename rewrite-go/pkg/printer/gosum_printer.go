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
)

func PrintGoSum(gs *golang.GoSum) string {
	return printGoSum(gs, NewPrintOutputCapture())
}

func PrintGoSumWithMarkers(gs *golang.GoSum, mp MarkerPrinter) string {
	return printGoSum(gs, NewPrintOutputCaptureWithMarkers(mp))
}

func printGoSum(gs *golang.GoSum, out *PrintOutputCapture) string {
	out.BeforePrefix(gs.Markers)
	printGoModSpace(gs.Prefix, out)
	out.BeforeSyntax(gs.Markers)
	for _, rp := range gs.Lines {
		printGoSumLine(rp.Element, out)
		printGoModSpace(rp.After, out)
	}
	out.AfterSyntax(gs.Markers)
	printGoModSpace(gs.Eof, out)
	return out.String()
}

func printGoSumLine(l *golang.GoSumLine, out *PrintOutputCapture) {
	out.BeforePrefix(l.Markers)
	printGoModSpace(l.Prefix, out)
	out.BeforeSyntax(l.Markers)
	out.Append(l.ModulePath)
	out.Append(" ")
	out.Append(l.Version)
	if l.GoMod {
		out.Append("/go.mod")
	}
	out.Append(" ")
	out.Append(l.Hash)
	out.AfterSyntax(l.Markers)
}
