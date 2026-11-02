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
)

func TestUnaryOperandSpacing(t *testing.T) {
	sources := []string{
		"package p\n\nvar A = + +0\n",
		"package p\n\nvar A = - -0\n",
		"package p\n\nvar A = ^ ^0\n",
		"package p\n\nvar A = !  !true\n",
		"package p\n\nvar A = -  0\n",
		"package p\n\nvar A = 1 - -0\n",
	}
	for _, src := range sources {
		t.Run(src, func(t *testing.T) {
			p := parser.NewGoParser()
			p.ParseOnly = true
			cu, err := p.Parse("t.go", src)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			want, err := gofmtSource("t.go", src)
			if err != nil {
				t.Fatalf("gofmt: %v", err)
			}
			if got := runAutoFormat(cu); got != want {
				t.Errorf("got  %q\nwant %q", got, want)
			}
		})
	}
}

func TestSpacesVisitorLeavesTheVisitedTreeUntouched(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tx := 0\n\tx=1\n\tx+=2\n\t_ = ! true\n}\n"
	cu, err := parser.NewGoParser().Parse("t.go", src)
	require.NoError(t, err)

	// when
	out := NewSpacesVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, src, printer.Print(cu))
	assert.Equal(t, "package main\n\nfunc f() {\n\tx := 0\n\tx = 1\n\tx += 2\n\t_ = !true\n}\n", printer.Print(out))
}
