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

package test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// rpcRoundTrip serializes the CompilationUnit through the Go RPC sender and
// reconstructs it through the Go RPC receiver, mirroring a Go<->Java session.
func rpcRoundTrip(t *testing.T, cu *golang.CompilationUnit) *golang.CompilationUnit {
	t.Helper()
	var messages []rpc.RpcObjectData
	sendQ := rpc.NewSendQueue(1000, func(batch []rpc.RpcObjectData) {
		messages = append(messages, batch...)
	}, make(map[uintptr]int))
	rpc.NewGoSender().Visit(cu, sendQ)
	sendQ.Flush()

	delivered := false
	recvQ := rpc.NewReceiveQueue(make(map[int]any), func() []rpc.RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	return rpc.NewGoReceiver().Visit(&golang.CompilationUnit{ID: cu.ID}, recvQ).(*golang.CompilationUnit)
}

func TestGenericFuncTypeParametersSurviveRpc(t *testing.T) {
	for _, src := range []string{
		"package main\n\nfunc Min[T int | float64](a, b T) T {\n\treturn a\n}\n",
		"package main\n\nfunc Map[T any, U any](s []T) []U {\n\treturn nil\n}\n",
		"package main\n\nfunc Pair[T, U any](a T, b U) {\n}\n",
	} {
		// given
		cu, err := parser.NewGoParser().Parse("x.go", src)
		if err != nil {
			t.Fatalf("parse error: %v", err)
		}

		// when
		got := rpcRoundTrip(t, cu)

		// then
		md := got.Statements[0].Element.(*java.MethodDeclaration)
		if md.TypeParameters == nil {
			t.Fatalf("type parameters lost over RPC for %q", src)
		}
		if printed := printer.Print(got); printed != src {
			t.Errorf("RPC round-trip mismatch\nexpected:\n%s\nactual:\n%s", src, printed)
		}
	}
}

func TestGenericTypeDeclTypeParametersSurviveRpc(t *testing.T) {
	for _, src := range []string{
		"package main\n\ntype Pair[T any] struct {\n\tFirst  T\n\tSecond T\n}\n",
		"package main\n\ntype Container[T any] interface {\n\tGet() T\n}\n",
		"package main\n\ntype MyList[T any] []T\n",
		"package main\n\ntype V[T any] = []T\n",
		"package main\n\ntype Map[K comparable, V any] map[K]V\n",
	} {
		// given
		cu, err := parser.NewGoParser().Parse("x.go", src)
		if err != nil {
			t.Fatalf("parse error: %v", err)
		}

		// when
		got := rpcRoundTrip(t, cu)

		// then
		td := got.Statements[0].Element.(*golang.TypeDecl)
		if td.TypeParameters == nil {
			t.Fatalf("type parameters lost over RPC for %q", src)
		}
		if printed := printer.Print(got); printed != src {
			t.Errorf("RPC round-trip mismatch\nexpected:\n%s\nactual:\n%s", src, printed)
		}
	}
}
