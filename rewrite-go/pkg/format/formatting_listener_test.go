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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestFormattingListenerHearsEachAutoFormatUntilStopped(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("t.go", "package p\n\nfunc F() {}\n")
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	var heard []java.Tree
	stop := ListenToFormatting(func(tree, stopAfter java.Tree, _ *visitor.Cursor) {
		if stopAfter != nil {
			t.Errorf("stopAfter = %v, want nil", stopAfter)
		}
		heard = append(heard, tree)
	})

	AutoFormat(cu, nil, nil, nil)
	stop()
	AutoFormat(cu, nil, nil, nil)

	if len(heard) != 1 || heard[0] != java.Tree(cu) {
		t.Errorf("heard %v, want the compilation unit once", heard)
	}
}
