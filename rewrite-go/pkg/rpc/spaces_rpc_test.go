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
package rpc

import (
	"strconv"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type commentedSpaces struct {
	visitor.GoVisitor
	n int
}

func (v *commentedSpaces) VisitSpace(space java.Space, p any) java.Space {
	v.n++
	comments := append(append([]java.Comment{}, space.Comments()...),
		java.Comment{Multiline: true, Text: "s" + strconv.Itoa(v.n) + "s"})
	return java.MakeSpace(comments, space.Whitespace())
}

// A recipe may put whitespace where the parser never does, and it reaches the
// peer from wherever a visitor can put it.
func TestEverySpaceAVisitorReachesCrossesRpc(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("spaces.go", `package main

import (
	"fmt"
	str "strings"
)

func (t *T) M(xs []string) {
	if v := 1; v > 0 {
		return
	} else {
		v--
	}
	for i := 0; i < 3; i++ {
	}
	for {
	}
	for k, v := range xs {
		fmt.Println(k, str.ToUpper(v))
	}
	switch z := len(xs); z {
	case 1, 2:
		z = 1
	default:
	}
}
`)
	require.NoError(t, err)
	v := visitor.Init(&commentedSpaces{})
	commented := v.Visit(cu, nil)

	received, ok := roundTripNode(t, commented, &golang.CompilationUnit{ID: cu.ID}).(java.Tree)

	require.True(t, ok)
	printed := printer.Print(received)
	assert.Equal(t, printer.Print(commented), printed)
	assert.Equal(t, v.n, strings.Count(printed, "s*/"))
}
