/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
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

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// GoMod is a SourceFile but not a J node, so — like java.ParseError — it is
// special-cased ahead of the J-dispatch in GoSender.Visit / GoReceiver.Visit
// and serialized by this self-contained codec rather than the generic
// padding helpers (which assume J elements).
//
// The field order below is the single source of truth: the Go send/receive
// here and the Java GoMod#rpcSend/#rpcReceive must all agree on it.

func init() {
	RegisterValueType(reflect.TypeOf((*golang.GoMod)(nil)), "org.openrewrite.golang.tree.GoMod")
	RegisterValueType(reflect.TypeOf((*golang.GoModDirective)(nil)), "org.openrewrite.golang.tree.GoMod$Directive")
	RegisterValueType(reflect.TypeOf((*golang.GoModBlock)(nil)), "org.openrewrite.golang.tree.GoMod$Block")
	RegisterValueType(reflect.TypeOf((*golang.GoModValue)(nil)), "org.openrewrite.golang.tree.GoMod$Value")

	RegisterFactory("org.openrewrite.golang.tree.GoMod", func() any { return &golang.GoMod{} })
	RegisterFactory("org.openrewrite.golang.tree.GoMod$Directive", func() any { return &golang.GoModDirective{} })
	RegisterFactory("org.openrewrite.golang.tree.GoMod$Block", func() any { return &golang.GoModBlock{} })
	RegisterFactory("org.openrewrite.golang.tree.GoMod$Value", func() any { return &golang.GoModValue{} })
}

// --- send ---

func sendGoMod(gm *golang.GoMod, q *SendQueue) {
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).Ident.String() }, nil)
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).Prefix },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).SourcePath }, nil)
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).Charset }, nil)
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).CharsetBomMarked }, nil)
	q.GetAndSend(gm, func(_ any) any { return nil }, nil) // checksum
	q.GetAndSend(gm, func(_ any) any { return nil }, nil) // fileAttributes
	q.GetAndSendList(gm,
		func(v any) []any { return goModStmtSlice(v.(*golang.GoMod).Statements) },
		goModStmtRPID,
		func(v any) { sendGoModRightPadded(v, q) })
	q.GetAndSend(gm, func(v any) any { return v.(*golang.GoMod).Eof },
		func(v any) { sendSpace(v.(java.Space), q) })
}

func sendGoModRightPadded(rp any, q *SendQueue) {
	q.GetAndSend(rp, func(v any) any { return v.(java.RightPadded[golang.GoModStatement]).Element },
		func(v any) { sendGoModStatement(v.(golang.GoModStatement), q) })
	q.GetAndSend(rp, func(v any) any { return v.(java.RightPadded[golang.GoModStatement]).After },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(rp, func(v any) any { return v.(java.RightPadded[golang.GoModStatement]).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

func sendGoModStatement(s golang.GoModStatement, q *SendQueue) {
	switch s.(type) {
	case *golang.GoModDirective:
		sendGoModDirective(s.(*golang.GoModDirective), q)
	case *golang.GoModBlock:
		sendGoModBlock(s.(*golang.GoModBlock), q)
	}
}

func sendGoModDirective(d *golang.GoModDirective, q *SendQueue) {
	q.GetAndSend(d, func(v any) any { return v.(*golang.GoModDirective).Ident.String() }, nil)
	q.GetAndSend(d, func(v any) any { return v.(*golang.GoModDirective).Prefix },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(d, func(v any) any { return v.(*golang.GoModDirective).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(d, func(v any) any { return v.(*golang.GoModDirective).Keyword }, nil)
	q.GetAndSendList(d,
		func(v any) []any { return goModValueSlice(v.(*golang.GoModDirective).Values) },
		goModValueID,
		func(v any) { sendGoModValue(v.(*golang.GoModValue), q) })
}

func sendGoModBlock(b *golang.GoModBlock, q *SendQueue) {
	q.GetAndSend(b, func(v any) any { return v.(*golang.GoModBlock).Ident.String() }, nil)
	q.GetAndSend(b, func(v any) any { return v.(*golang.GoModBlock).Prefix },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(b, func(v any) any { return v.(*golang.GoModBlock).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(b, func(v any) any { return v.(*golang.GoModBlock).Keyword }, nil)
	q.GetAndSend(b, func(v any) any { return v.(*golang.GoModBlock).BeforeLParen },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSendList(b,
		func(v any) []any { return goModStmtSlice(v.(*golang.GoModBlock).Entries) },
		goModStmtRPID,
		func(v any) { sendGoModRightPadded(v, q) })
	q.GetAndSend(b, func(v any) any { return v.(*golang.GoModBlock).BeforeRParen },
		func(v any) { sendSpace(v.(java.Space), q) })
}

func sendGoModValue(val *golang.GoModValue, q *SendQueue) {
	q.GetAndSend(val, func(v any) any { return v.(*golang.GoModValue).Ident.String() }, nil)
	q.GetAndSend(val, func(v any) any { return v.(*golang.GoModValue).Prefix },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(val, func(v any) any { return v.(*golang.GoModValue).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(val, func(v any) any { return v.(*golang.GoModValue).Text }, nil)
}

// --- receive ---

func receiveGoMod(gm *golang.GoMod, q *ReceiveQueue) *golang.GoMod {
	gm.Ident = recvGoModID(q, gm.Ident)
	gm.Prefix = recvGoModSpace(q, gm.Prefix)
	gm.Markers = recvGoModMarkers(q, gm.Markers)
	gm.SourcePath = receiveScalar[string](q, gm.SourcePath)
	gm.Charset = receiveScalar[string](q, gm.Charset)
	gm.CharsetBomMarked = receiveScalar[bool](q, gm.CharsetBomMarked)
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	gm.Statements = recvGoModStmtList(q, gm.Statements)
	gm.Eof = recvGoModSpace(q, gm.Eof)
	return gm
}

func recvGoModStmtList(q *ReceiveQueue, before []java.RightPadded[golang.GoModStatement]) []java.RightPadded[golang.GoModStatement] {
	afterAny := q.ReceiveList(goModStmtSlice(before), func(v any) any { return recvGoModRightPadded(v, q) })
	if afterAny == nil {
		return nil
	}
	out := make([]java.RightPadded[golang.GoModStatement], len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(java.RightPadded[golang.GoModStatement])
	}
	return out
}

func recvGoModRightPadded(v any, q *ReceiveQueue) any {
	var beforeElem golang.GoModStatement
	beforeAfter := java.EmptySpace
	var beforeMarkers java.Markers
	if rp, ok := v.(java.RightPadded[golang.GoModStatement]); ok {
		beforeElem, beforeAfter, beforeMarkers = rp.Element, rp.After, rp.Markers
	}
	elemAny := q.Receive(beforeElem, func(e any) any { return recvGoModStatement(e, q) })
	after := recvGoModSpace(q, beforeAfter)
	markers := recvGoModMarkers(q, beforeMarkers)
	var elem golang.GoModStatement
	if elemAny != nil {
		elem = elemAny.(golang.GoModStatement)
	}
	return java.RightPadded[golang.GoModStatement]{Element: elem, After: after, Markers: markers}
}

func recvGoModStatement(baseline any, q *ReceiveQueue) any {
	switch b := baseline.(type) {
	case *golang.GoModDirective:
		return recvGoModDirective(b, q)
	case *golang.GoModBlock:
		return recvGoModBlock(b, q)
	default:
		return baseline
	}
}

func recvGoModDirective(d *golang.GoModDirective, q *ReceiveQueue) *golang.GoModDirective {
	d.Ident = recvGoModID(q, d.Ident)
	d.Prefix = recvGoModSpace(q, d.Prefix)
	d.Markers = recvGoModMarkers(q, d.Markers)
	d.Keyword = receiveScalar[string](q, d.Keyword)
	d.Values = recvGoModValueList(q, d.Values)
	return d
}

func recvGoModBlock(b *golang.GoModBlock, q *ReceiveQueue) *golang.GoModBlock {
	b.Ident = recvGoModID(q, b.Ident)
	b.Prefix = recvGoModSpace(q, b.Prefix)
	b.Markers = recvGoModMarkers(q, b.Markers)
	b.Keyword = receiveScalar[string](q, b.Keyword)
	b.BeforeLParen = recvGoModSpace(q, b.BeforeLParen)
	b.Entries = recvGoModStmtList(q, b.Entries)
	b.BeforeRParen = recvGoModSpace(q, b.BeforeRParen)
	return b
}

func recvGoModValueList(q *ReceiveQueue, before []*golang.GoModValue) []*golang.GoModValue {
	afterAny := q.ReceiveList(goModValueSlice(before), func(v any) any { return recvGoModValue(v, q) })
	if afterAny == nil {
		return nil
	}
	out := make([]*golang.GoModValue, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(*golang.GoModValue)
	}
	return out
}

func recvGoModValue(baseline any, q *ReceiveQueue) any {
	v, ok := baseline.(*golang.GoModValue)
	if !ok {
		v = &golang.GoModValue{}
	}
	v.Ident = recvGoModID(q, v.Ident)
	v.Prefix = recvGoModSpace(q, v.Prefix)
	v.Markers = recvGoModMarkers(q, v.Markers)
	v.Text = receiveScalar[string](q, v.Text)
	return v
}

// --- shared helpers ---

func recvGoModID(q *ReceiveQueue, before uuid.UUID) uuid.UUID {
	idStr := receiveScalar[string](q, before.String())
	if idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			return parsed
		}
	}
	return before
}

func recvGoModSpace(q *ReceiveQueue, before java.Space) java.Space {
	v := q.Receive(before, func(x any) any { return receiveSpace(x.(java.Space), q) })
	if v == nil {
		return java.EmptySpace
	}
	return v.(java.Space)
}

func recvGoModMarkers(q *ReceiveQueue, before java.Markers) java.Markers {
	v := q.Receive(before, func(x any) any { return receiveMarkersCodec(q, x.(java.Markers)) })
	if v == nil {
		return before
	}
	return v.(java.Markers)
}

func goModStmtSlice(s []java.RightPadded[golang.GoModStatement]) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func goModValueSlice(s []*golang.GoModValue) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func goModStmtRPID(rp any) any {
	elem := rp.(java.RightPadded[golang.GoModStatement]).Element
	switch e := elem.(type) {
	case *golang.GoModDirective:
		return e.Ident.String()
	case *golang.GoModBlock:
		return e.Ident.String()
	default:
		return nil
	}
}

func goModValueID(v any) any {
	return v.(*golang.GoModValue).Ident.String()
}
