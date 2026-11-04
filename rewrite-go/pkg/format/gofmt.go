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
	"bytes"
	goparser "go/parser"
	goprinter "go/printer"
	gotoken "go/token"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// gofmt's own printer settings (see cmd/gofmt and go/format).
const (
	gofmtMode     = goprinter.UseSpaces | goprinter.TabIndent
	gofmtTabWidth = 8
)

// Gofmt lays out cu the way gofmt would, by printing it, running the result
// through go/printer, and splicing that layout back in with SpliceWhitespace.
// When target is non-nil only that subtree is re-laid-out, so a recipe's diff
// stays limited to what the recipe touched.
//
// cu comes back unchanged when it is already formatted or when the printed
// source doesn't parse.
func Gofmt(cu *golang.CompilationUnit, target java.Tree) (*golang.CompilationUnit, error) {
	src := printer.Print(cu)
	formattedSrc, err := gofmtSource(cu.SourcePath, src)
	if err != nil {
		return cu, err
	}
	if formattedSrc == src {
		return cu, nil
	}
	// The layout-only parse is the cheaper one but shapes a few expressions
	// differently (see GoParser.ParseOnly), so a tree that diverges from cu is
	// re-parsed with attribution before settling for the partial splice.
	best := cu
	p := parser.NewGoParser()
	for _, parseOnly := range [...]bool{true, false} {
		p.ParseOnly = parseOnly
		formatted, err := p.Parse(cu.SourcePath, formattedSrc)
		if err != nil {
			return best, err
		}
		spliced, complete := SpliceWhitespace(cu, formatted, target)
		best = spliced.(*golang.CompilationUnit)
		if complete {
			break
		}
	}
	return best, nil
}

// GofmtVisitor applies Gofmt to every compilation unit it visits.
type GofmtVisitor struct {
	visitor.GoVisitor
	target java.Tree
}

// NewGofmtVisitor returns a visitor bounded to the given target subtree. Pass
// nil to lay out the whole file.
func NewGofmtVisitor(target java.Tree) *GofmtVisitor {
	return visitor.Init(&GofmtVisitor{target: target})
}

func (v *GofmtVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	out, err := Gofmt(cu, v.target)
	if err != nil {
		return cu
	}
	return out
}

// gofmtSource applies gofmt's layout rules and nothing else. Plain gofmt also
// sorts imports and normalizes number literals, which rewrite tokens: a
// whitespace splice cannot carry those, and they belong to recipes anyway.
func gofmtSource(sourcePath, src string) (string, error) {
	fset := gotoken.NewFileSet()
	file, err := goparser.ParseFile(fset, sourcePath, src, goparser.ParseComments|goparser.SkipObjectResolution)
	if err != nil {
		return "", err
	}
	var buf bytes.Buffer
	if err := (&goprinter.Config{Mode: gofmtMode, Tabwidth: gofmtTabWidth}).Fprint(&buf, fset, file); err != nil {
		return "", err
	}
	return buf.String(), nil
}
