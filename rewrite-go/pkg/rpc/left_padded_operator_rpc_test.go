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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Operator enums travel the wire as their Java enum-constant NAME, and those
// names are ambiguous: AssignmentOperator.AddAssign ("+=") and BinaryOperator.Add
// ("+") both serialize to "Addition". The pre-fix receiver inferred the LeftPadded
// element type from that string (trying ParseBinaryOperator first), so every
// compound-assignment operator was mis-typed as LeftPadded[BinaryOperator] and the
// AssignmentOperation call site's raw assertion to LeftPadded[AssignmentOperator]
// panicked — breaking Print on essentially any Go file containing +=, |=, etc.
//
// receiveLeftPaddedTyped[T] disambiguates by parsing the wire string with the
// parser matching the field's statically-known element type T.

func TestCoerceLeftPaddedTyped_AmbiguousNameResolvesByTargetType(t *testing.T) {
	// "Addition" is valid for BOTH operator enums; T decides which.
	asAssign := coerceLeftPaddedTyped[java.AssignmentOperator](java.EmptySpace, "Addition", java.Markers{})
	if asAssign.Element != java.AddAssign {
		t.Errorf("T=AssignmentOperator: want AddAssign, got %v", asAssign.Element)
	}
	asBinary := coerceLeftPaddedTyped[java.BinaryOperator](java.EmptySpace, "Addition", java.Markers{})
	if asBinary.Element != java.Add {
		t.Errorf("T=BinaryOperator: want Add, got %v", asBinary.Element)
	}
}

func TestCoerceLeftPaddedTyped_PreTypedEnumPassThrough(t *testing.T) {
	// NO_CHANGE hands back the already-typed enum; it must survive.
	got := coerceLeftPaddedTyped[java.AssignmentOperator](java.EmptySpace, java.OrAssign, java.Markers{})
	if got.Element != java.OrAssign {
		t.Errorf("pre-typed pass-through: want OrAssign, got %v", got.Element)
	}
}

func TestAssignmentOperationRoundTrip_OperatorPreserved(t *testing.T) {
	// Full send->receive (Print path) of `x += y`. Pre-fix this panicked at
	// VisitAssignmentOperation: result.(LeftPadded[AssignmentOperator]) on a
	// value that was actually LeftPadded[BinaryOperator].
	id := uuid.New()
	before := &java.AssignmentOperation{
		ID:         id,
		Variable:   makeIdent("x"),
		Operator:   java.LeftPadded[java.AssignmentOperator]{Element: java.AddAssign, Markers: java.Markers{}},
		Assignment: makeIdent("y"),
	}
	seed := &java.AssignmentOperation{ID: id}

	got := roundTripNode(t, before, seed).(*java.AssignmentOperation)

	if got.Operator.Element != java.AddAssign {
		t.Errorf("Operator: want AddAssign (+=), got %v", got.Operator.Element)
	}
}

func TestBinaryRoundTrip_OperatorPreserved(t *testing.T) {
	// Sibling sanity check: a real BinaryOperator must still resolve to Binary,
	// not be poached by the assignment parser.
	id := uuid.New()
	before := &java.Binary{
		ID:       id,
		Left:     makeIdent("a"),
		Operator: java.LeftPadded[java.BinaryOperator]{Element: java.Add, Markers: java.Markers{}},
		Right:    makeIdent("b"),
	}
	seed := &java.Binary{ID: id}

	got := roundTripNode(t, before, seed).(*java.Binary)

	if got.Operator.Element != java.Add {
		t.Errorf("Operator: want Add (+), got %v", got.Operator.Element)
	}
}
