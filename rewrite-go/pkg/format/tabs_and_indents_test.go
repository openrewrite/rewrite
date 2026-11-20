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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestTabsAndIndentsVisitorSetsTheFilesDeclarationsAtTheMargin(t *testing.T) {
	// given a package clause, an import and declarations written indented, some
	// under directives
	cu, err := parser.NewGoParser().Parse("t.go", "  // Package p is a package.\n  package p\n\n\timport \"fmt\"\n\n  var V = fmt.Sprint()\n\n    // F does nothing.\n    func F() {\n  }\n\n  //go:noinline\n  func G() {}\n\n\t//go:generate stringer -type K\n\ttype K int\n")
	require.NoError(t, err)

	// when
	out := NewTabsAndIndentsVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, "// Package p is a package.\npackage p\n\nimport \"fmt\"\n\nvar V = fmt.Sprint()\n\n// F does nothing.\nfunc F() {\n}\n\n//go:noinline\nfunc G() {}\n\n//go:generate stringer -type K\ntype K int\n", printer.Print(out))
}

func TestTabsAndIndentsVisitorNestsAClausesExpressionsFromItsKeyword(t *testing.T) {
	// given a switch clause's expressions and a select clause's communication
	// that open a call and a function literal on the keyword's line
	src := "package p\n\nfunc f(x int, c chan int) {\n\tswitch {\n\tcase g(\n\tx,\n\t1):\n\tcase func() bool {\n\treturn true\n\t}():\n\t}\n\tselect {\n\tcase v := <-h(\n\tc):\n\t\t_ = v\n\tcase <-func() chan int {\n\treturn c\n\t}():\n\t}\n}\n"
	cu, err := parser.NewGoParser().Parse("t.go", src)
	require.NoError(t, err)

	// when
	out := NewTabsAndIndentsVisitor(nil).Visit(cu, nil)

	// then they nest from the keyword's level, as gofmt nests them
	want, err := gofmtSource("t.go", src)
	require.NoError(t, err)
	assert.Equal(t, want, printer.Print(out))
}

func TestTabsAndIndentsVisitorStopsAfterASelectClausesCommunication(t *testing.T) {
	// given a select clause whose body, written after its communication, holds
	// a statement indented too deep
	src := "package p\n\nfunc f(c chan int) {\n\tselect {\n\tcase v := <-c:\n\t\tif v > 0 {\n\t\t\t\t\tv++\n\t\t}\n\t}\n}\n"
	cu, err := parser.NewGoParser().Parse("t.go", src)
	require.NoError(t, err)
	var comm java.Tree
	for _, n := range walkNodes(cu).nodes {
		if cc, ok := n.(*golang.CommClause); ok {
			comm = cc.Comm
			break
		}
	}
	require.NotNil(t, comm)

	// when indenting stops after the communication
	out := NewTabsAndIndentsVisitor(comm).Visit(cu, nil)

	// then the body is left as it was
	assert.Equal(t, src, printer.Print(out))
}

func TestTabsAndIndentsVisitorIndentsALaterImportDeclaration(t *testing.T) {
	// given a second import declaration written indented
	cu, err := parser.NewGoParser().Parse("t.go", "package p\n\nimport \"fmt\"\n\n  import \"os\"\n\nvar _ = fmt.Sprint\nvar _ = os.Args\n")
	require.NoError(t, err)

	// when
	out := NewTabsAndIndentsVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, "package p\n\nimport \"fmt\"\n\nimport \"os\"\n\nvar _ = fmt.Sprint\nvar _ = os.Args\n", printer.Print(out))
}
