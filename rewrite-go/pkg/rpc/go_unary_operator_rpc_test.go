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

func TestGoUnaryRoundTrip_OperatorSpaceChangePreservesAddressOf(t *testing.T) {
	// given
	id := uuid.New()
	expr := makeIdent("x")
	after := &golang.Unary{
		ID:         id,
		Operator:   java.LeftPadded[golang.UnaryOperator]{Before: java.EmptySpace, Element: golang.AddressOf},
		Expression: expr,
	}
	before := &golang.Unary{
		ID:         id,
		Operator:   java.LeftPadded[golang.UnaryOperator]{Before: java.SingleSpace, Element: golang.AddressOf},
		Expression: expr,
	}
	seed := &golang.Unary{
		ID:       id,
		Operator: java.LeftPadded[golang.UnaryOperator]{Element: golang.AddressOf},
	}

	// when
	got := roundTripNodeWithBefore(t, after, before, seed).(*golang.Unary)

	// then
	if got.Operator.Element != golang.AddressOf {
		t.Errorf("Operator: want AddressOf=%d (&), got %d (0 makes the printer emit '?')",
			int(golang.AddressOf), int(got.Operator.Element))
	}
}
