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
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A Go unary `&x` / `*x` / `<-x` operator must survive the RPC round-trip. If
// the operator enum is lost it decodes to 0 (invalid) and the printer emits the
// unknown-operator placeholder "?", corrupting emitted patches (e.g. `&T{}` →
// `?T{}`).
func TestGoUnaryRoundTrip_OperatorPreserved(t *testing.T) {
	for _, tc := range []struct {
		name string
		op   golang.UnaryOperator
	}{
		{"AddressOf", golang.AddressOf},
		{"Indirection", golang.Indirection},
		{"Receive", golang.Receive},
	} {
		t.Run(tc.name, func(t *testing.T) {
			id := uuid.New()
			before := &golang.Unary{
				ID:         id,
				Operator:   java.LeftPadded[golang.UnaryOperator]{Element: tc.op, Markers: java.Markers{}},
				Expression: makeIdent("x"),
			}
			seed := &golang.Unary{ID: id}
			got := roundTripNode(t, before, seed).(*golang.Unary)
			if got.Operator.Element != tc.op {
				t.Errorf("Operator: want %v (%d), got %v (%d)", tc.op, tc.op, got.Operator.Element, got.Operator.Element)
			}
		})
	}
}
