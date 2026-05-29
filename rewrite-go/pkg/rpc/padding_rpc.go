/*
 * Copyright 2025 the original author or authors.
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
	"fmt"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// sendRightPadded serializes a RightPadded element.
// Matches JavaSender.visitRightPadded: element, after, markers
func sendRightPadded(s Sender, rp any, q *SendQueue) {
	// Element: dispatch based on whether it's a J node, Space, or primitive
	elem := rightPaddedElement(rp)
	if _, ok := elem.(java.J); ok {
		q.GetAndSend(rp, func(v any) any { return rightPaddedElement(v) },
			func(v any) { s.Visit(v.(java.Tree), q) })
	} else {
		// Non-J elements (primitives, etc.) are sent as raw values
		q.GetAndSend(rp, func(v any) any { return rightPaddedElement(v) }, nil)
	}
	// After space
	q.GetAndSend(rp, func(v any) any { return rightPaddedAfter(v) },
		func(v any) { sendSpace(v.(java.Space), q) })
	// Markers
	q.GetAndSend(rp, func(v any) any { return rightPaddedMarkers(v) },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

// sendRightPaddedBool sends a RightPadded<Boolean> manually for fields like Block.static.
// This avoids the generic padding type system since Go doesn't have RightPadded[bool].
func sendRightPaddedBool(elem bool, after java.Space, markers java.Markers, q *SendQueue) {
	rpVT := "org.openrewrite.java.tree.JRightPadded"
	q.Put(RpcObjectData{State: Add, ValueType: &rpVT})
	// Element: boolean (raw value)
	q.Put(RpcObjectData{State: Add, Value: elem})
	// After: Space
	q.Send(after, nil, func(v any) { sendSpace(v.(java.Space), q) })
	// Markers
	q.Send(markers, nil, func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

// sendLeftPadded serializes a LeftPadded element.
// Matches JavaSender.visitLeftPadded: before, element, markers
func sendLeftPadded(s Sender, lp any, q *SendQueue) {
	// Before space
	q.GetAndSend(lp, func(v any) any { return leftPaddedBefore(v) },
		func(v any) { sendSpace(v.(java.Space), q) })
	// Element: dispatch based on type
	elem := leftPaddedElement(lp)
	switch elem.(type) {
	case java.Space:
		q.GetAndSend(lp, func(v any) any { return leftPaddedElement(v) },
			func(v any) { sendSpace(v.(java.Space), q) })
	case java.J:
		q.GetAndSend(lp, func(v any) any { return leftPaddedElement(v) },
			func(v any) { s.Visit(v.(java.Tree), q) })
	default:
		// Primitives (strings, enums, bools) are sent as raw values with nil onChange
		q.GetAndSend(lp, func(v any) any { return leftPaddedElement(v) }, nil)
	}
	// Markers
	q.GetAndSend(lp, func(v any) any { return leftPaddedMarkers(v) },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

// sendContainer serializes a Container.
// Matches JavaSender.visitContainer: before, elements (list of right-padded), markers
func sendContainer(s Sender, c any, q *SendQueue) {
	// Before space
	q.GetAndSend(c, func(v any) any { return containerBefore(v) },
		func(v any) { sendSpace(v.(java.Space), q) })
	// Elements (list of RightPadded)
	q.GetAndSendList(c,
		func(v any) []any { return containerElements(v) },
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// Markers
	q.GetAndSend(c, func(v any) any { return containerMarkers(v) },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

// receiveRightPadded deserializes a RightPadded element.
func receiveRightPadded(r Receiver, q *ReceiveQueue, before any) any {
	// Element
	elem := q.Receive(rightPaddedElement(before), func(v any) any {
		if _, ok := v.(java.J); ok {
			return r.Visit(v.(java.Tree), q)
		}
		return v
	})
	// After space
	afterSpace := q.Receive(rightPaddedAfter(before), func(v any) any {
		return receiveSpace(v.(java.Space), q)
	})
	// Markers
	markers := q.Receive(rightPaddedMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	})

	var after java.Space
	if afterSpace != nil {
		after = afterSpace.(java.Space)
	}
	var m java.Markers
	if markers != nil {
		m = markers.(java.Markers)
	}

	// Always use element-type detection since Go lacks generic covariance.
	return rightPaddedFromElement(elem, after, m)
}

// receiveLeftPadded deserializes a LeftPadded element.
func receiveLeftPadded(r Receiver, q *ReceiveQueue, before any) any {
	// Before space
	beforeSpace := q.Receive(leftPaddedBefore(before), func(v any) any {
		return receiveSpace(v.(java.Space), q)
	})
	// Element
	elem := q.Receive(leftPaddedElement(before), func(v any) any {
		if _, ok := v.(java.Space); ok {
			return receiveSpace(v.(java.Space), q)
		}
		if _, ok := v.(java.J); ok {
			return r.Visit(v.(java.Tree), q)
		}
		return v
	})
	// Markers
	markers := q.Receive(leftPaddedMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	})

	return updateLeftPadded(before, beforeSpace.(java.Space), elem, markers.(java.Markers))
}

// Accessor functions for generic padding types (using type switches for Go's type-parameterized structs)

func rightPaddedElement(rp any) any {
	switch v := rp.(type) {
	case java.RightPadded[java.Statement]:
		return v.Element
	case java.RightPadded[java.Expression]:
		return v.Element
	case java.RightPadded[java.J]:
		return v.Element
	case java.RightPadded[*java.Identifier]:
		return v.Element
	case java.RightPadded[*java.VariableDeclarator]:
		return v.Element
	case java.RightPadded[*java.Import]:
		return v.Element
	default:
		return nil
	}
}

func rightPaddedAfter(rp any) any {
	switch v := rp.(type) {
	case java.RightPadded[java.Statement]:
		return v.After
	case java.RightPadded[java.Expression]:
		return v.After
	case java.RightPadded[java.J]:
		return v.After
	case java.RightPadded[*java.Identifier]:
		return v.After
	case java.RightPadded[*java.VariableDeclarator]:
		return v.After
	case java.RightPadded[*java.Import]:
		return v.After
	default:
		return java.EmptySpace
	}
}

func rightPaddedMarkers(rp any) any {
	switch v := rp.(type) {
	case java.RightPadded[java.Statement]:
		return v.Markers
	case java.RightPadded[java.Expression]:
		return v.Markers
	case java.RightPadded[java.J]:
		return v.Markers
	case java.RightPadded[*java.Identifier]:
		return v.Markers
	case java.RightPadded[*java.VariableDeclarator]:
		return v.Markers
	case java.RightPadded[*java.Import]:
		return v.Markers
	default:
		return java.Markers{}
	}
}

// updateRightPadded creates a correctly-typed RightPadded from the element.
// Always uses element-type detection since Go lacks generic covariance.
func updateRightPadded(rp any, elem any, after java.Space, markers java.Markers) any {
	return rightPaddedFromElement(elem, after, markers)
}

// rightPaddedFromElement creates the most specific RightPadded variant based on
// the element's concrete type. This is needed for factory-created fallback instances
// (RightPadded[java.J]) where we don't know the desired variant until we see the element.
func rightPaddedFromElement(elem any, after java.Space, markers java.Markers) any {
	// Types that are only J (not Statement or Expression) need explicit handling
	switch e := elem.(type) {
	case *java.VariableDeclarator:
		return java.RightPadded[*java.VariableDeclarator]{Element: e, After: after, Markers: markers}
	case *java.Import:
		return java.RightPadded[*java.Import]{Element: e, After: after, Markers: markers}
	case bool:
		// Primitive wrappers like Block.static (JRightPadded<Boolean> on the Java side).
		// The caller (VisitBlock) discards the result, so the wire shape is preserved
		// even though Go can't represent the typed element.
		_ = e
		return java.RightPadded[java.J]{After: after, Markers: markers}
	}
	// For types that implement both Statement and Expression (like MethodInvocation),
	// we create BOTH variants and let the caller decide. In practice, the parent container
	// determines which variant is needed. Since we can't know the parent's expectation here,
	// we prefer Expression for most call-like expressions and Statement for block-level items.
	//
	// The key insight: Statement includes Block, Return, If, ForLoop, Switch, etc.
	// Expression includes Identifier, Literal, Binary, MethodInvocation, FieldAccess, etc.
	// Types implementing both: MethodInvocation, Assignment, Unary, MethodDeclaration, etc.
	//
	// For the bidirectional RPC, the container type determines which variant is needed.
	// We prefer Expression over Statement because most RPC containers (arguments,
	// conditions, return expressions) expect Expression, and types like MethodInvocation
	// implement both interfaces. Statement-only containers (block bodies) use
	// coerceToStatementRP to convert as needed.
	if expr, ok := elem.(java.Expression); ok {
		return java.RightPadded[java.Expression]{Element: expr, After: after, Markers: markers}
	}
	if stmt, ok := elem.(java.Statement); ok {
		return java.RightPadded[java.Statement]{Element: stmt, After: after, Markers: markers}
	}
	if j, ok := elem.(java.J); ok {
		return java.RightPadded[java.J]{Element: j, After: after, Markers: markers}
	}
	return java.RightPadded[java.J]{After: after, Markers: markers}
}

// coerceToExpressionRP converts a RightPadded of any variant to RightPadded[Expression].
// Panics if the underlying element does not implement Expression — silently dropping it
// (the previous behavior) corrupts the LST. Callers must only use this for fields whose
// every element is known to satisfy Expression.
func coerceToExpressionRP(rp any) java.RightPadded[java.Expression] {
	if rp, ok := rp.(java.RightPadded[java.Expression]); ok {
		return rp
	}
	elem := rightPaddedElement(rp)
	after := rightPaddedAfter(rp).(java.Space)
	m := rightPaddedMarkers(rp).(java.Markers)
	if expr, ok := elem.(java.Expression); ok {
		return java.RightPadded[java.Expression]{Element: expr, After: after, Markers: m}
	}
	panic(fmt.Sprintf("coerceToExpressionRP: element does not implement java.Expression (rp=%T elem=%T nil=%v)", rp, elem, elem == nil))
}

// coerceToStatementRP converts a RightPadded of any variant to RightPadded[Statement].
// Panics if the underlying element does not implement Statement (see coerceToExpressionRP).
func coerceToStatementRP(rp any) java.RightPadded[java.Statement] {
	if rp, ok := rp.(java.RightPadded[java.Statement]); ok {
		return rp
	}
	elem := rightPaddedElement(rp)
	after := rightPaddedAfter(rp).(java.Space)
	m := rightPaddedMarkers(rp).(java.Markers)
	if stmt, ok := elem.(java.Statement); ok {
		return java.RightPadded[java.Statement]{Element: stmt, After: after, Markers: m}
	}
	panic(fmt.Sprintf("coerceToStatementRP: element does not implement java.Statement (rp=%T elem=%T nil=%v)", rp, elem, elem == nil))
}

// coerceLeftPaddedIdent converts a LeftPadded of any variant to LeftPadded[*Identifier].
// Java may send the value generic-parameterized on Expression even though the element
// is an *Identifier; this helper bridges that asymmetry.
func coerceLeftPaddedIdent(lp any) java.LeftPadded[*java.Identifier] {
	if lp, ok := lp.(java.LeftPadded[*java.Identifier]); ok {
		return lp
	}
	elem := leftPaddedElement(lp)
	before := leftPaddedBefore(lp).(java.Space)
	m := leftPaddedMarkers(lp).(java.Markers)
	if id, ok := elem.(*java.Identifier); ok {
		return java.LeftPadded[*java.Identifier]{Element: id, Before: before, Markers: m}
	}
	return java.LeftPadded[*java.Identifier]{Before: before, Markers: m}
}

// coerceLeftPaddedAssignOp converts a LeftPadded of any variant to LeftPadded[AssignOp].
// Java ships the operator as a literal source symbol ("=", ":=") so leftPaddedFromElement
// produces a LeftPadded[string] when ParseAssignOp can't resolve it; this helper
// re-parses and falls back to AssignOpEquals defensively.
func coerceLeftPaddedAssignOp(lp any) java.LeftPadded[java.AssignOp] {
	if lp, ok := lp.(java.LeftPadded[java.AssignOp]); ok {
		return lp
	}
	elem := leftPaddedElement(lp)
	before := leftPaddedBefore(lp).(java.Space)
	m := leftPaddedMarkers(lp).(java.Markers)
	if op, ok := elem.(java.AssignOp); ok {
		return java.LeftPadded[java.AssignOp]{Element: op, Before: before, Markers: m}
	}
	if s, ok := elem.(string); ok {
		if op := java.ParseAssignOp(s); op != 0 {
			return java.LeftPadded[java.AssignOp]{Element: op, Before: before, Markers: m}
		}
		return java.LeftPadded[java.AssignOp]{Element: java.AssignOpEquals, Before: before, Markers: m}
	}
	return java.LeftPadded[java.AssignOp]{Element: java.AssignOpEquals, Before: before, Markers: m}
}

func leftPaddedBefore(lp any) any {
	switch v := lp.(type) {
	case java.LeftPadded[java.J]:
		return v.Before
	case java.LeftPadded[java.Expression]:
		return v.Before
	case java.LeftPadded[*java.Identifier]:
		return v.Before
	case java.LeftPadded[java.BinaryOperator]:
		return v.Before
	case java.LeftPadded[java.AssignmentOperator]:
		return v.Before
	case java.LeftPadded[java.UnaryOperator]:
		return v.Before
	case java.LeftPadded[java.Space]:
		return v.Before
	case java.LeftPadded[java.AssignOp]:
		return v.Before
	case java.LeftPadded[string]:
		return v.Before
	case java.LeftPadded[bool]:
		return v.Before
	default:
		return java.EmptySpace
	}
}

func leftPaddedElement(lp any) any {
	switch v := lp.(type) {
	case java.LeftPadded[java.J]:
		return v.Element
	case java.LeftPadded[java.Expression]:
		return v.Element
	case java.LeftPadded[*java.Identifier]:
		return v.Element
	case java.LeftPadded[java.BinaryOperator]:
		return v.Element
	case java.LeftPadded[java.AssignmentOperator]:
		return v.Element
	case java.LeftPadded[java.UnaryOperator]:
		return v.Element
	case java.LeftPadded[java.Space]:
		return v.Element
	case java.LeftPadded[java.AssignOp]:
		return v.Element
	case java.LeftPadded[string]:
		return v.Element
	case java.LeftPadded[bool]:
		return v.Element
	default:
		return nil
	}
}

func leftPaddedMarkers(lp any) any {
	switch v := lp.(type) {
	case java.LeftPadded[java.J]:
		return v.Markers
	case java.LeftPadded[java.Expression]:
		return v.Markers
	case java.LeftPadded[*java.Identifier]:
		return v.Markers
	case java.LeftPadded[java.BinaryOperator]:
		return v.Markers
	case java.LeftPadded[java.AssignmentOperator]:
		return v.Markers
	case java.LeftPadded[java.UnaryOperator]:
		return v.Markers
	case java.LeftPadded[java.Space]:
		return v.Markers
	case java.LeftPadded[java.AssignOp]:
		return v.Markers
	case java.LeftPadded[string]:
		return v.Markers
	case java.LeftPadded[bool]:
		return v.Markers
	default:
		return java.Markers{}
	}
}

// updateLeftPadded creates a correctly-typed LeftPadded from the element.
// Uses the element's concrete type to determine the generic type parameter,
// since Go lacks generic covariance (LeftPadded[J] != LeftPadded[Expression]).
func updateLeftPadded(lp any, before java.Space, elem any, markers java.Markers) any {
	return leftPaddedFromElement(before, elem, markers)
}

// leftPaddedFromElement creates a LeftPadded with the correct generic type
// based on the element's concrete type.
func leftPaddedFromElement(before java.Space, elem any, markers java.Markers) any {
	// String values may encode operator enums
	if s, ok := elem.(string); ok {
		if op := java.ParseBinaryOperator(s); op != 0 {
			return java.LeftPadded[java.BinaryOperator]{Before: before, Element: op, Markers: markers}
		}
		if op := java.ParseAssignmentOperator(s); op != 0 {
			return java.LeftPadded[java.AssignmentOperator]{Before: before, Element: op, Markers: markers}
		}
		if op := java.ParseUnaryOperator(s); op != 0 {
			return java.LeftPadded[java.UnaryOperator]{Before: before, Element: op, Markers: markers}
		}
		if op := java.ParseAssignOp(s); op != 0 {
			return java.LeftPadded[java.AssignOp]{Before: before, Element: op, Markers: markers}
		}
		return java.LeftPadded[string]{Before: before, Element: s, Markers: markers}
	}
	if b, ok := elem.(bool); ok {
		return java.LeftPadded[bool]{Before: before, Element: b, Markers: markers}
	}
	if sp, ok := elem.(java.Space); ok {
		return java.LeftPadded[java.Space]{Before: before, Element: sp, Markers: markers}
	}
	// Pre-typed operator enums (NO_CHANGE path passes the existing typed value through)
	if op, ok := elem.(java.BinaryOperator); ok {
		return java.LeftPadded[java.BinaryOperator]{Before: before, Element: op, Markers: markers}
	}
	if op, ok := elem.(java.UnaryOperator); ok {
		return java.LeftPadded[java.UnaryOperator]{Before: before, Element: op, Markers: markers}
	}
	if op, ok := elem.(java.AssignmentOperator); ok {
		return java.LeftPadded[java.AssignmentOperator]{Before: before, Element: op, Markers: markers}
	}
	if op, ok := elem.(java.AssignOp); ok {
		return java.LeftPadded[java.AssignOp]{Before: before, Element: op, Markers: markers}
	}
	// Interface types — prefer Expression over Statement
	if expr, ok := elem.(java.Expression); ok {
		return java.LeftPadded[java.Expression]{Before: before, Element: expr, Markers: markers}
	}
	if stmt, ok := elem.(java.Statement); ok {
		return java.LeftPadded[java.Statement]{Before: before, Element: stmt, Markers: markers}
	}
	if j, ok := elem.(java.J); ok {
		return java.LeftPadded[java.J]{Before: before, Element: j, Markers: markers}
	}
	return java.LeftPadded[java.J]{Before: before, Markers: markers}
}

func containerBefore(c any) any {
	switch v := c.(type) {
	case java.Container[java.J]:
		return v.Before
	case java.Container[java.Statement]:
		return v.Before
	case java.Container[java.Expression]:
		return v.Before
	case java.Container[*java.Import]:
		return v.Before
	default:
		return java.EmptySpace
	}
}

func containerElements(c any) []any {
	switch v := c.(type) {
	case java.Container[java.J]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	case java.Container[java.Statement]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	case java.Container[java.Expression]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	case java.Container[*java.Import]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	default:
		return nil
	}
}

func containerElementID(rp any) any {
	elem := rightPaddedElement(rp)
	if elem == nil {
		return nil
	}
	// Extract ID from the inner element
	switch v := elem.(type) {
	case *java.Identifier:
		return v.ID
	case *java.VariableDeclarator:
		return v.ID
	case *java.Import:
		return v.ID
	default:
		// For interface types, try to get ID via type switch on common types
		return extractID(v)
	}
}

func containerMarkers(c any) any {
	switch v := c.(type) {
	case java.Container[java.J]:
		return v.Markers
	case java.Container[java.Statement]:
		return v.Markers
	case java.Container[java.Expression]:
		return v.Markers
	case java.Container[*java.Import]:
		return v.Markers
	default:
		return java.Markers{}
	}
}

// coerceRightPaddedTyped converts a RightPadded of any variant to RightPadded[T].
// Java erases the element type parameter of JRightPadded on the wire, so the value
// receiveRightPadded hands back may be parameterized on a wider interface (e.g.
// RightPadded[Expression]) than the field's declared RightPadded[T]. Go generics
// are invariant and distinct instantiations are unrelated at runtime, so this
// re-wraps the element under T.
//
// Unlike coerceToExpressionRP/coerceToStatementRP — which panic when the element
// doesn't satisfy the target type — this falls back to an element-less padding.
// Those coerce single-slot fields where a non-conforming element is an unambiguous
// bug worth failing loudly on; this coerces container elements, where a stray
// element should not abort the whole container's (and thus the node's) receive.
func coerceRightPaddedTyped[T any](rp any) java.RightPadded[T] {
	if rp, ok := rp.(java.RightPadded[T]); ok {
		return rp
	}
	elem := rightPaddedElement(rp)
	after := rightPaddedAfter(rp).(java.Space)
	m := rightPaddedMarkers(rp).(java.Markers)
	if e, ok := elem.(T); ok {
		return java.RightPadded[T]{Element: e, After: after, Markers: m}
	}
	return java.RightPadded[T]{After: after, Markers: m}
}

// receiveContainerTyped deserializes a JContainer into a Go Container[T], building
// it directly from the field's statically-known element type T rather than inferring
// the type from the payload. Inference has to guess for empty containers — and since
// Go generics are invariant (see coerceRightPaddedTyped), a guessed Container[Expression]
// for an empty imports field is not assertable to Container[*Import] and would panic.
// Supplying T up front sidesteps that. The type-safe counterpart to sendContainer.
func receiveContainerTyped[T any](r Receiver, q *ReceiveQueue, before any) java.Container[T] {
	beforeSpace := q.Receive(containerBefore(before), func(v any) any {
		return receiveSpace(v.(java.Space), q)
	})
	elemsBefore := containerElements(before)
	elemsAfter := q.ReceiveList(elemsBefore, func(v any) any {
		return receiveRightPadded(r, q, v)
	})
	markers := q.Receive(containerMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	})
	elems := make([]java.RightPadded[T], len(elemsAfter))
	for i, rp := range elemsAfter {
		elems[i] = coerceRightPaddedTyped[T](rp)
	}
	return java.Container[T]{Before: beforeSpace.(java.Space), Elements: elems, Markers: markers.(java.Markers)}
}
