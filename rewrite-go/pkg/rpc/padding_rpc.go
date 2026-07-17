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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
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
	q.GetAndSend(c, func(v any) any { return containerMarkers(v) },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

func receiveRightPadded(r Receiver, q *ReceiveQueue, before any) any {
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

// receiveLeftPaddedParts deserializes the three wire fields of a JLeftPadded —
// before-space, element, markers — shared by receiveLeftPadded (type-inferred) and
// receiveLeftPaddedEnum (type-directed).
func receiveLeftPaddedParts(r Receiver, q *ReceiveQueue, before any) (java.Space, any, java.Markers) {
	beforeSpace := q.Receive(leftPaddedBefore(before), func(v any) any {
		return receiveSpace(v.(java.Space), q)
	})
	elem := q.Receive(leftPaddedElement(before), func(v any) any {
		if _, ok := v.(java.Space); ok {
			return receiveSpace(v.(java.Space), q)
		}
		if _, ok := v.(java.J); ok {
			return r.Visit(v.(java.Tree), q)
		}
		return v
	})
	markers := q.Receive(leftPaddedMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	})
	return beforeSpace.(java.Space), elem, markers.(java.Markers)
}

// receiveLeftPadded deserializes a LeftPadded element, inferring its type from the
// payload. Operator fields must use receiveLeftPaddedEnum instead (see its doc).
func receiveLeftPadded(r Receiver, q *ReceiveQueue, before any) any {
	beforeSpace, elem, markers := receiveLeftPaddedParts(r, q, before)
	return leftPaddedFromElement(beforeSpace, elem, markers)
}

// receiveLeftPaddedEnum receives an enum-valued JLeftPadded field, returning the typed
// LeftPadded[T] directly — it wraps the q.Receive call, the deserialization closure, and
// the result assertion so call sites are a single typed assignment.
//
// It uses the caller-supplied parser to interpret the wire payload rather than inferring
// the type from it (leftPaddedFromElement). Inference is unsafe for enums: they travel
// the wire as their Java enum-constant name, and those names can be AMBIGUOUS across Go
// enum types — e.g. BinaryOperator.Add and AssignmentOperator.AddAssign both serialize
// to "Addition". leftPaddedFromElement tries ParseBinaryOperator first, so every
// compound-assignment operator (+=, |=, …) was mis-typed as LeftPadded[BinaryOperator]
// and the AssignmentOperation call site's raw assertion panicked. The field's type T is
// inferred from `before` (and the parser), so the call site just passes the matching
// parser (ParseBinaryOperator, ParseAssignmentOperator, …). Any enum wrapped in a
// LeftPadded — current or future — should be received this way. Counterpart to
// receiveContainerTyped.
func receiveLeftPaddedEnum[T any](r Receiver, q *ReceiveQueue, before java.LeftPadded[T], parse func(string) T) java.LeftPadded[T] {
	result := q.Receive(before, func(v any) any {
		beforeSpace, elem, markers := receiveLeftPaddedParts(r, q, v)
		return coerceLeftPaddedEnum(beforeSpace, elem, markers, parse)
	})
	if result == nil {
		return before
	}
	return result.(java.LeftPadded[T])
}

// coerceLeftPaddedEnum builds a LeftPadded[T] for an enum slot. The element is either
// already a T (NO_CHANGE pass-through / pre-typed enum) or the enum's Java
// enum-constant name as a string, which `parse` resolves to the T constant.
func coerceLeftPaddedEnum[T any](before java.Space, elem any, m java.Markers, parse func(string) T) java.LeftPadded[T] {
	if e, ok := elem.(T); ok {
		return java.LeftPadded[T]{Before: before, Element: e, Markers: m}
	}
	if s, ok := elem.(string); ok {
		return java.LeftPadded[T]{Before: before, Element: parse(s), Markers: m}
	}
	return java.LeftPadded[T]{Before: before, Markers: m}
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

// coerceAnnotation narrows a received leadingAnnotations element to *java.Annotation,
// mapping a nil element to nil so receiveTypedListNonNil can drop it.
func coerceAnnotation(v any) *java.Annotation {
	if v == nil {
		return nil
	}
	return v.(*java.Annotation)
}

func annotationIsNil(a *java.Annotation) bool { return a == nil }

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
	case java.LeftPadded[golang.BinaryOperator]:
		return v.Before
	case java.LeftPadded[golang.AssignmentOperator]:
		return v.Before
	case java.LeftPadded[golang.UnaryOperator]:
		return v.Before
	case java.LeftPadded[java.Space]:
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
	case java.LeftPadded[golang.BinaryOperator]:
		return v.Element
	case java.LeftPadded[golang.AssignmentOperator]:
		return v.Element
	case java.LeftPadded[golang.UnaryOperator]:
		return v.Element
	case java.LeftPadded[java.Space]:
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
	case java.LeftPadded[golang.BinaryOperator]:
		return v.Markers
	case java.LeftPadded[golang.AssignmentOperator]:
		return v.Markers
	case java.LeftPadded[golang.UnaryOperator]:
		return v.Markers
	case java.LeftPadded[java.Space]:
		return v.Markers
	case java.LeftPadded[string]:
		return v.Markers
	case java.LeftPadded[bool]:
		return v.Markers
	default:
		return java.Markers{}
	}
}

// leftPaddedFromElement creates a LeftPadded with the correct generic type
// based on the element's concrete type.
// leftPaddedFromElement infers a LeftPadded's generic type from the element's concrete
// type. Enum-valued slots (operators) do NOT come through here — they use
// receiveLeftPaddedEnum, which resolves the ambiguous wire name via a caller-supplied
// parser — so this only handles unambiguous element kinds.
func leftPaddedFromElement(before java.Space, elem any, markers java.Markers) any {
	if b, ok := elem.(bool); ok {
		return java.LeftPadded[bool]{Before: before, Element: b, Markers: markers}
	}
	if sp, ok := elem.(java.Space); ok {
		return java.LeftPadded[java.Space]{Before: before, Element: sp, Markers: markers}
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

// receiveContainer receives a value-typed Container[T] field. NO_CHANGE returns the
// before value (a Container[T], which the cast matches); ADD/CHANGE returns the typed
// container from receiveContainerTyped; DELETE leaves the field unchanged (value-typed
// container fields are never deleted).
//
// Unlike receiveValue, the onChange closure forwards `before` to receiveContainerTyped
// as a bare `any` — it must NOT cast to Container[T] first. On the ADD path the wire's
// erased "JContainer" type makes the queue materialize a Container[java.J] baseline
// (newObj can't know the element type T); receiveContainerTyped tolerates that via
// containerElements, but a `v.(Container[T])` cast would panic on it. This is the same
// reason receivePointerContainer keeps the closure untyped.
func receiveContainer[T any](r Receiver, q *ReceiveQueue, before java.Container[T]) java.Container[T] {
	if result := q.Receive(before, func(v any) any { return receiveContainerTyped[T](r, q, v) }); result != nil {
		return result.(java.Container[T])
	}
	return before
}

// receivePointerContainer receives a nullable *Container[T] field. The before-pointer is
// dereferenced to a value baseline before q.Receive so the NO_CHANGE path returns an
// assertable value Container[T] rather than the *Container[T] pointer (a raw value-cast
// of which panics — the #7831 / pointer-vs-value bug class). DELETE (nil result) clears
// the field; ADD/CHANGE rewraps the typed container as a pointer.
func receivePointerContainer[T any](r Receiver, q *ReceiveQueue, before *java.Container[T]) *java.Container[T] {
	var beforeVal any
	if before != nil {
		beforeVal = *before
	}
	if result := q.Receive(beforeVal, func(v any) any { return receiveContainerTyped[T](r, q, v) }); result != nil {
		c := result.(java.Container[T])
		return &c
	}
	return nil
}
