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
	"reflect"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type expressionTypeCollector struct {
	visitor.GoVisitor
	nodes []java.Expression
}

func (v *expressionTypeCollector) VisitGoBinary(b *golang.Binary, p any) java.J {
	v.nodes = append(v.nodes, b)
	return v.GoVisitor.VisitGoBinary(b, p)
}

func (v *expressionTypeCollector) VisitKeyValue(kv *golang.KeyValue, p any) java.J {
	v.nodes = append(v.nodes, kv)
	return v.GoVisitor.VisitKeyValue(kv, p)
}

func (v *expressionTypeCollector) VisitSlice(s *golang.Slice, p any) java.J {
	v.nodes = append(v.nodes, s)
	return v.GoVisitor.VisitSlice(s, p)
}

func TestExpressionTypesSurviveRoundTrip(t *testing.T) {
	// given
	src := `package main

type Mask uint64
type Values []int

func f(m Mask, values Values, a *[5]int) {
    _ = m &^ 1
    _ = map[string]int{"one": 1}
    _ = struct{ N int }{N: 2}
    _ = [3]int{1: 2}
    _ = values[1:]
    _ = values[:2:3]
    _ = a[1:3]
    _ = "abc"[1:]
}
`
	cu, err := parser.NewGoParser().Parse("types.go", src)
	require.NoError(t, err)
	before := &expressionTypeCollector{}
	visitor.Init(before).Visit(cu, nil)
	require.Len(t, before.nodes, 8)
	for _, node := range before.nodes {
		field := reflect.ValueOf(node).Elem().FieldByName("Type")
		require.True(t, field.IsValid(), "%T must carry a type", node)
		require.False(t, field.IsNil(), "%T must be attributed", node)
	}

	// when
	rt := roundTripNode(t, cu, &golang.CompilationUnit{ID: cu.ID}).(java.Tree)
	after := &expressionTypeCollector{}
	visitor.Init(after).Visit(rt, nil)

	// then
	require.Len(t, after.nodes, len(before.nodes))
	for i, node := range after.nodes {
		require.Equal(t, reflect.TypeOf(before.nodes[i]), reflect.TypeOf(node))
		require.Equal(t, reflect.ValueOf(before.nodes[i]).Elem().FieldByName("Type").Interface(),
			reflect.ValueOf(node).Elem().FieldByName("Type").Interface())
	}
	require.Equal(t, src, printer.Print(rt))
}
