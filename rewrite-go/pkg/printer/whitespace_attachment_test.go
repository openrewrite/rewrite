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

package printer_test

import (
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

// Leading whitespace must be attached to the outermost LST element rather than
// leaking onto the prefix of a node's first child. The enforcement logic lives
// in test.WhitespaceAttachmentViolations and is also asserted against the
// whole native Go test corpus from the RewriteRun harness.
func TestWhitespaceAttachedToOutermostElement(t *testing.T) {
	sources := []string{
		"package main\n\nfunc f() int { return 1 + 2 }\n",
		"package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println(\"hi\") }\n",
		"package main\n\ntype Point struct {\n\tX int\n\tY int\n}\n",
		"package main\n\ntype Stringer interface {\n\tString() string\n}\n",
		"package main\n\nfunc m() {\n\tfor i := 0; i < 10; i++ {\n\t}\n}\n",
		"package main\n\nfunc s(x int) string {\n\tswitch x {\n\tcase 1:\n\t\treturn \"one\"\n\tdefault:\n\t\treturn \"other\"\n\t}\n}\n",
		"package main\n\nfunc g() {\n\tmm := map[string]int{\"a\": 1}\n\t_ = mm\n}\n",
		"package main\n\nfunc Max[T int | float64](a, b T) T {\n\tif a > b {\n\t\treturn a\n\t}\n\treturn b\n}\n",
		"package main\n\nfunc h() {\n\tch := make(chan int)\n\tgo func() { ch <- 1 }()\n\tdefer close(ch)\n}\n",
		"package main\n\nfunc sl() {\n\txs := []int{1, 2, 3}\n\t_ = xs[1:2]\n}\n",
		"package main\n\nfunc p() {\n\tvar x int = 5\n\ty := &x\n\t_ = *y\n}\n",
		"package main\n\ntype T struct{}\n\nfunc (t *T) Do() {}\n",
	}

	for _, source := range sources {
		t.Run(source, func(t *testing.T) {
			// given
			cu, err := parser.NewGoParser().Parse("test.go", source)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}

			// when
			violations := test.WhitespaceAttachmentViolations(cu)

			// then
			if len(violations) > 0 {
				t.Errorf("expected no whitespace-attachment violations, got %d:\n%s",
					len(violations), strings.Join(violations, "\n"))
			}
		})
	}
}
