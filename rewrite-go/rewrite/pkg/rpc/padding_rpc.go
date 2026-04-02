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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// sendRightPadded serializes a RightPadded element.
// Matches JavaSender.visitRightPadded: element, after, markers
func sendRightPadded(s Sender, rp any, q *SendQueue) {
	// Element: dispatch based on whether it's a J node, Space, or primitive
	elem := rightPaddedElement(rp)
	if _, ok := elem.(tree.J); ok {
		q.GetAndSend(rp, func(v any) any { return rightPaddedElement(v) },
			func(v any) { s.Visit(v, q) })
	} else {
		// Non-J elements (primitives, etc.) are sent as raw values
		q.GetAndSend(rp, func(v any) any { return rightPaddedElement(v) }, nil)
	}
	// After space
	q.GetAndSend(rp, func(v any) any { return rightPaddedAfter(v) },
		func(v any) { sendSpace(v.(tree.Space), q) })
	// Markers
	q.GetAndSend(rp, func(v any) any { return rightPaddedMarkers(v) },
		func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
}

// sendRightPaddedBool sends a RightPadded<Boolean> manually for fields like Block.static.
// This avoids the generic padding type system since Go doesn't have RightPadded[bool].
func sendRightPaddedBool(elem bool, after tree.Space, markers tree.Markers, q *SendQueue) {
	rpVT := "org.openrewrite.java.tree.JRightPadded"
	q.Put(RpcObjectData{State: Add, ValueType: &rpVT})
	// Element: boolean (raw value)
	q.Put(RpcObjectData{State: Add, Value: elem})
	// After: Space
	q.Send(after, nil, func(v any) { sendSpace(v.(tree.Space), q) })
	// Markers
	q.Send(markers, nil, func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
}

// sendLeftPadded serializes a LeftPadded element.
// Matches JavaSender.visitLeftPadded: before, element, markers
func sendLeftPadded(s Sender, lp any, q *SendQueue) {
	// Before space
	q.GetAndSend(lp, func(v any) any { return leftPaddedBefore(v) },
		func(v any) { sendSpace(v.(tree.Space), q) })
	// Element: dispatch based on type
	elem := leftPaddedElement(lp)
	switch elem.(type) {
	case tree.Space:
		q.GetAndSend(lp, func(v any) any { return leftPaddedElement(v) },
			func(v any) { sendSpace(v.(tree.Space), q) })
	case tree.J:
		q.GetAndSend(lp, func(v any) any { return leftPaddedElement(v) },
			func(v any) { s.Visit(v, q) })
	default:
		// Primitives (strings, enums, bools) are sent as raw values with nil onChange
		q.GetAndSend(lp, func(v any) any { return leftPaddedElement(v) }, nil)
	}
	// Markers
	q.GetAndSend(lp, func(v any) any { return leftPaddedMarkers(v) },
		func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
}

// sendContainer serializes a Container.
// Matches JavaSender.visitContainer: before, elements (list of right-padded), markers
func sendContainer(s Sender, c any, q *SendQueue) {
	// Before space
	q.GetAndSend(c, func(v any) any { return containerBefore(v) },
		func(v any) { sendSpace(v.(tree.Space), q) })
	// Elements (list of RightPadded)
	q.GetAndSendList(c,
		func(v any) []any { return containerElements(v) },
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// Markers
	q.GetAndSend(c, func(v any) any { return containerMarkers(v) },
		func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
}

// receiveRightPadded deserializes a RightPadded element.
func receiveRightPadded(r Receiver, q *ReceiveQueue, before any) any {
	// Element
	elem := q.Receive(rightPaddedElement(before), func(v any) any {
		if _, ok := v.(tree.J); ok {
			return r.Visit(v, q)
		}
		return v
	})
	// After space
	afterSpace := q.Receive(rightPaddedAfter(before), func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	})
	// Markers
	markers := q.Receive(rightPaddedMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(tree.Markers))
	})

	var after tree.Space
	if afterSpace != nil {
		after = afterSpace.(tree.Space)
	}
	var m tree.Markers
	if markers != nil {
		m = markers.(tree.Markers)
	}

	// For factory-created fallback (RightPadded[tree.J]), create the result
	// based on the element's type since we don't know the parent's expectation.
	if _, ok := before.(tree.RightPadded[tree.J]); ok {
		return rightPaddedFromElement(elem, after, m)
	}
	return updateRightPadded(before, elem, after, m)
}

// receiveLeftPadded deserializes a LeftPadded element.
func receiveLeftPadded(r Receiver, q *ReceiveQueue, before any) any {
	// Before space
	beforeSpace := q.Receive(leftPaddedBefore(before), func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	})
	// Element
	elem := q.Receive(leftPaddedElement(before), func(v any) any {
		if _, ok := v.(tree.Space); ok {
			return receiveSpace(v.(tree.Space), q)
		}
		if _, ok := v.(tree.J); ok {
			return r.Visit(v, q)
		}
		return v
	})
	// Markers
	markers := q.Receive(leftPaddedMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(tree.Markers))
	})

	return updateLeftPadded(before, beforeSpace.(tree.Space), elem, markers.(tree.Markers))
}

// receiveContainer deserializes a Container.
func receiveContainer(r Receiver, q *ReceiveQueue, before any) any {
	// Before space
	beforeSpace := q.Receive(containerBefore(before), func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	})
	// Elements
	elemsBefore := containerElements(before)
	elemsAfter := q.ReceiveList(elemsBefore, func(v any) any {
		return receiveRightPadded(r, q, v)
	})
	// Markers
	markers := q.Receive(containerMarkers(before), func(v any) any {
		return receiveMarkersCodec(q, v.(tree.Markers))
	})

	return updateContainer(before, beforeSpace.(tree.Space), elemsAfter, markers.(tree.Markers))
}

// Accessor functions for generic padding types (using type switches for Go's type-parameterized structs)

func rightPaddedElement(rp any) any {
	switch v := rp.(type) {
	case tree.RightPadded[tree.Statement]:
		return v.Element
	case tree.RightPadded[tree.Expression]:
		return v.Element
	case tree.RightPadded[tree.J]:
		return v.Element
	case tree.RightPadded[*tree.Identifier]:
		return v.Element
	case tree.RightPadded[*tree.VariableDeclarator]:
		return v.Element
	case tree.RightPadded[*tree.Import]:
		return v.Element
	default:
		return nil
	}
}

func rightPaddedAfter(rp any) any {
	switch v := rp.(type) {
	case tree.RightPadded[tree.Statement]:
		return v.After
	case tree.RightPadded[tree.Expression]:
		return v.After
	case tree.RightPadded[tree.J]:
		return v.After
	case tree.RightPadded[*tree.Identifier]:
		return v.After
	case tree.RightPadded[*tree.VariableDeclarator]:
		return v.After
	case tree.RightPadded[*tree.Import]:
		return v.After
	default:
		return tree.EmptySpace
	}
}

func rightPaddedMarkers(rp any) any {
	switch v := rp.(type) {
	case tree.RightPadded[tree.Statement]:
		return v.Markers
	case tree.RightPadded[tree.Expression]:
		return v.Markers
	case tree.RightPadded[tree.J]:
		return v.Markers
	case tree.RightPadded[*tree.Identifier]:
		return v.Markers
	case tree.RightPadded[*tree.VariableDeclarator]:
		return v.Markers
	case tree.RightPadded[*tree.Import]:
		return v.Markers
	default:
		return tree.Markers{}
	}
}

func updateRightPadded(rp any, elem any, after tree.Space, markers tree.Markers) any {
	switch rp.(type) {
	case tree.RightPadded[tree.Statement]:
		return tree.RightPadded[tree.Statement]{Element: elem.(tree.Statement), After: after, Markers: markers}
	case tree.RightPadded[tree.Expression]:
		return tree.RightPadded[tree.Expression]{Element: elem.(tree.Expression), After: after, Markers: markers}
	case tree.RightPadded[tree.J]:
		// Factory-created fallback: determine the correct RightPadded variant.
		// This is called when Java sends ADD for a new RightPadded in the
		// bidirectional direction. We need to create the exact generic variant
		// that the parent container expects.
		return rightPaddedFromElement(elem, after, markers)
	case tree.RightPadded[*tree.Identifier]:
		return tree.RightPadded[*tree.Identifier]{Element: elem.(*tree.Identifier), After: after, Markers: markers}
	case tree.RightPadded[*tree.VariableDeclarator]:
		return tree.RightPadded[*tree.VariableDeclarator]{Element: elem.(*tree.VariableDeclarator), After: after, Markers: markers}
	case tree.RightPadded[*tree.Import]:
		return tree.RightPadded[*tree.Import]{Element: elem.(*tree.Import), After: after, Markers: markers}
	default:
		return rp
	}
}

// rightPaddedFromElement creates the most specific RightPadded variant based on
// the element's concrete type. This is needed for factory-created fallback instances
// (RightPadded[tree.J]) where we don't know the desired variant until we see the element.
func rightPaddedFromElement(elem any, after tree.Space, markers tree.Markers) any {
	// Try concrete pointer types first (most specific)
	switch e := elem.(type) {
	case *tree.Identifier:
		return tree.RightPadded[*tree.Identifier]{Element: e, After: after, Markers: markers}
	case *tree.VariableDeclarator:
		return tree.RightPadded[*tree.VariableDeclarator]{Element: e, After: after, Markers: markers}
	case *tree.Import:
		return tree.RightPadded[*tree.Import]{Element: e, After: after, Markers: markers}
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
	// Since most containers in the RPC protocol use Expression (arguments, conditions),
	// and Statement-only containers are mostly for block bodies, we check Statement FIRST
	// and fall back to Expression. The caller (container update) will handle the type assertion.
	if stmt, ok := elem.(tree.Statement); ok {
		return tree.RightPadded[tree.Statement]{Element: stmt, After: after, Markers: markers}
	}
	if expr, ok := elem.(tree.Expression); ok {
		return tree.RightPadded[tree.Expression]{Element: expr, After: after, Markers: markers}
	}
	if j, ok := elem.(tree.J); ok {
		return tree.RightPadded[tree.J]{Element: j, After: after, Markers: markers}
	}
	return tree.RightPadded[tree.J]{After: after, Markers: markers}
}

// coerceToExpressionRP converts a RightPadded of any variant to RightPadded[Expression]
// by extracting the element and verifying it implements Expression.
func coerceToExpressionRP(rp any) tree.RightPadded[tree.Expression] {
	if rp, ok := rp.(tree.RightPadded[tree.Expression]); ok {
		return rp
	}
	elem := rightPaddedElement(rp)
	after := rightPaddedAfter(rp).(tree.Space)
	m := rightPaddedMarkers(rp).(tree.Markers)
	if expr, ok := elem.(tree.Expression); ok {
		return tree.RightPadded[tree.Expression]{Element: expr, After: after, Markers: m}
	}
	return tree.RightPadded[tree.Expression]{After: after, Markers: m}
}

// coerceToStatementRP converts a RightPadded of any variant to RightPadded[Statement].
func coerceToStatementRP(rp any) tree.RightPadded[tree.Statement] {
	if rp, ok := rp.(tree.RightPadded[tree.Statement]); ok {
		return rp
	}
	elem := rightPaddedElement(rp)
	after := rightPaddedAfter(rp).(tree.Space)
	m := rightPaddedMarkers(rp).(tree.Markers)
	if stmt, ok := elem.(tree.Statement); ok {
		return tree.RightPadded[tree.Statement]{Element: stmt, After: after, Markers: m}
	}
	return tree.RightPadded[tree.Statement]{After: after, Markers: m}
}

// coerceRightPaddedIdent converts a RightPadded of any variant to RightPadded[*Identifier].
func coerceRightPaddedIdent(rp any) tree.RightPadded[*tree.Identifier] {
	if rp, ok := rp.(tree.RightPadded[*tree.Identifier]); ok {
		return rp
	}
	elem := rightPaddedElement(rp)
	after := rightPaddedAfter(rp).(tree.Space)
	m := rightPaddedMarkers(rp).(tree.Markers)
	if id, ok := elem.(*tree.Identifier); ok {
		return tree.RightPadded[*tree.Identifier]{Element: id, After: after, Markers: m}
	}
	return tree.RightPadded[*tree.Identifier]{After: after, Markers: m}
}

func leftPaddedBefore(lp any) any {
	switch v := lp.(type) {
	case tree.LeftPadded[tree.J]:
		return v.Before
	case tree.LeftPadded[tree.Expression]:
		return v.Before
	case tree.LeftPadded[*tree.Identifier]:
		return v.Before
	case tree.LeftPadded[tree.BinaryOperator]:
		return v.Before
	case tree.LeftPadded[tree.AssignmentOperator]:
		return v.Before
	case tree.LeftPadded[tree.UnaryOperator]:
		return v.Before
	case tree.LeftPadded[tree.Space]:
		return v.Before
	case tree.LeftPadded[tree.AssignOp]:
		return v.Before
	case tree.LeftPadded[string]:
		return v.Before
	case tree.LeftPadded[bool]:
		return v.Before
	default:
		return tree.EmptySpace
	}
}

func leftPaddedElement(lp any) any {
	switch v := lp.(type) {
	case tree.LeftPadded[tree.J]:
		return v.Element
	case tree.LeftPadded[tree.Expression]:
		return v.Element
	case tree.LeftPadded[*tree.Identifier]:
		return v.Element
	case tree.LeftPadded[tree.BinaryOperator]:
		return v.Element
	case tree.LeftPadded[tree.AssignmentOperator]:
		return v.Element
	case tree.LeftPadded[tree.UnaryOperator]:
		return v.Element
	case tree.LeftPadded[tree.Space]:
		return v.Element
	case tree.LeftPadded[tree.AssignOp]:
		return v.Element
	case tree.LeftPadded[string]:
		return v.Element
	case tree.LeftPadded[bool]:
		return v.Element
	default:
		return nil
	}
}

func leftPaddedMarkers(lp any) any {
	switch v := lp.(type) {
	case tree.LeftPadded[tree.J]:
		return v.Markers
	case tree.LeftPadded[tree.Expression]:
		return v.Markers
	case tree.LeftPadded[*tree.Identifier]:
		return v.Markers
	case tree.LeftPadded[tree.BinaryOperator]:
		return v.Markers
	case tree.LeftPadded[tree.AssignmentOperator]:
		return v.Markers
	case tree.LeftPadded[tree.UnaryOperator]:
		return v.Markers
	case tree.LeftPadded[tree.Space]:
		return v.Markers
	case tree.LeftPadded[tree.AssignOp]:
		return v.Markers
	case tree.LeftPadded[string]:
		return v.Markers
	case tree.LeftPadded[bool]:
		return v.Markers
	default:
		return tree.Markers{}
	}
}

func updateLeftPadded(lp any, before tree.Space, elem any, markers tree.Markers) any {
	switch lp.(type) {
	case tree.LeftPadded[tree.J]:
		// Factory-created fallback: determine the most specific type from the element
		if expr, ok := elem.(tree.Expression); ok {
			return tree.LeftPadded[tree.Expression]{Before: before, Element: expr, Markers: markers}
		}
		if j, ok := elem.(tree.J); ok {
			return tree.LeftPadded[tree.J]{Before: before, Element: j, Markers: markers}
		}
		return tree.LeftPadded[tree.J]{Before: before, Markers: markers}
	case tree.LeftPadded[tree.Expression]:
		return tree.LeftPadded[tree.Expression]{Before: before, Element: elem.(tree.Expression), Markers: markers}
	case tree.LeftPadded[*tree.Identifier]:
		return tree.LeftPadded[*tree.Identifier]{Before: before, Element: elem.(*tree.Identifier), Markers: markers}
	case tree.LeftPadded[tree.BinaryOperator]:
		// Java sends enum as string name; convert back to Go enum
		if s, ok := elem.(string); ok {
			return tree.LeftPadded[tree.BinaryOperator]{Before: before, Element: tree.ParseBinaryOperator(s), Markers: markers}
		}
		return tree.LeftPadded[tree.BinaryOperator]{Before: before, Element: elem.(tree.BinaryOperator), Markers: markers}
	case tree.LeftPadded[tree.AssignmentOperator]:
		if s, ok := elem.(string); ok {
			return tree.LeftPadded[tree.AssignmentOperator]{Before: before, Element: tree.ParseAssignmentOperator(s), Markers: markers}
		}
		return tree.LeftPadded[tree.AssignmentOperator]{Before: before, Element: elem.(tree.AssignmentOperator), Markers: markers}
	case tree.LeftPadded[tree.UnaryOperator]:
		if s, ok := elem.(string); ok {
			return tree.LeftPadded[tree.UnaryOperator]{Before: before, Element: tree.ParseUnaryOperator(s), Markers: markers}
		}
		return tree.LeftPadded[tree.UnaryOperator]{Before: before, Element: elem.(tree.UnaryOperator), Markers: markers}
	case tree.LeftPadded[tree.Space]:
		return tree.LeftPadded[tree.Space]{Before: before, Element: elem.(tree.Space), Markers: markers}
	case tree.LeftPadded[tree.AssignOp]:
		if s, ok := elem.(string); ok {
			return tree.LeftPadded[tree.AssignOp]{Before: before, Element: tree.ParseAssignOp(s), Markers: markers}
		}
		return tree.LeftPadded[tree.AssignOp]{Before: before, Element: elem.(tree.AssignOp), Markers: markers}
	case tree.LeftPadded[string]:
		return tree.LeftPadded[string]{Before: before, Element: elem.(string), Markers: markers}
	case tree.LeftPadded[bool]:
		return tree.LeftPadded[bool]{Before: before, Element: elem.(bool), Markers: markers}
	default:
		return lp
	}
}

func containerBefore(c any) any {
	switch v := c.(type) {
	case tree.Container[tree.J]:
		return v.Before
	case tree.Container[tree.Statement]:
		return v.Before
	case tree.Container[tree.Expression]:
		return v.Before
	case tree.Container[*tree.Import]:
		return v.Before
	default:
		return tree.EmptySpace
	}
}

func containerElements(c any) []any {
	switch v := c.(type) {
	case tree.Container[tree.J]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	case tree.Container[tree.Statement]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	case tree.Container[tree.Expression]:
		result := make([]any, len(v.Elements))
		for i, e := range v.Elements {
			result[i] = e
		}
		return result
	case tree.Container[*tree.Import]:
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
	case *tree.Identifier:
		return v.ID
	case *tree.VariableDeclarator:
		return v.ID
	case *tree.Import:
		return v.ID
	default:
		// For interface types, try to get ID via type switch on common types
		return extractID(v)
	}
}

func containerMarkers(c any) any {
	switch v := c.(type) {
	case tree.Container[tree.J]:
		return v.Markers
	case tree.Container[tree.Statement]:
		return v.Markers
	case tree.Container[tree.Expression]:
		return v.Markers
	case tree.Container[*tree.Import]:
		return v.Markers
	default:
		return tree.Markers{}
	}
}

func updateContainer(c any, before tree.Space, elements []any, markers tree.Markers) any {
	switch c.(type) {
	case tree.Container[tree.J]:
		// Factory-created fallback: detect element type from first element
		if len(elements) > 0 {
			switch elements[0].(type) {
			case tree.RightPadded[tree.Statement]:
				elems := make([]tree.RightPadded[tree.Statement], len(elements))
				for i, e := range elements {
					elems[i] = e.(tree.RightPadded[tree.Statement])
				}
				return tree.Container[tree.Statement]{Before: before, Elements: elems, Markers: markers}
			case tree.RightPadded[tree.Expression]:
				elems := make([]tree.RightPadded[tree.Expression], len(elements))
				for i, e := range elements {
					elems[i] = e.(tree.RightPadded[tree.Expression])
				}
				return tree.Container[tree.Expression]{Before: before, Elements: elems, Markers: markers}
			case tree.RightPadded[*tree.Import]:
				elems := make([]tree.RightPadded[*tree.Import], len(elements))
				for i, e := range elements {
					elems[i] = e.(tree.RightPadded[*tree.Import])
				}
				return tree.Container[*tree.Import]{Before: before, Elements: elems, Markers: markers}
			}
		}
		// Empty or unrecognized element type
		return tree.Container[tree.J]{Before: before, Markers: markers}
	case tree.Container[tree.Statement]:
		elems := make([]tree.RightPadded[tree.Statement], len(elements))
		for i, e := range elements {
			if rp, ok := e.(tree.RightPadded[tree.Statement]); ok {
				elems[i] = rp
			} else {
				elems[i] = coerceToStatementRP(e)
			}
		}
		return tree.Container[tree.Statement]{Before: before, Elements: elems, Markers: markers}
	case tree.Container[tree.Expression]:
		elems := make([]tree.RightPadded[tree.Expression], len(elements))
		for i, e := range elements {
			if rp, ok := e.(tree.RightPadded[tree.Expression]); ok {
				elems[i] = rp
			} else {
				// Coerce from RightPadded[Statement] or RightPadded[J] when element implements Expression
				elems[i] = coerceToExpressionRP(e)
			}
		}
		return tree.Container[tree.Expression]{Before: before, Elements: elems, Markers: markers}
	case tree.Container[*tree.Import]:
		elems := make([]tree.RightPadded[*tree.Import], len(elements))
		for i, e := range elements {
			elems[i] = e.(tree.RightPadded[*tree.Import])
		}
		return tree.Container[*tree.Import]{Before: before, Elements: elems, Markers: markers}
	default:
		return c
	}
}
