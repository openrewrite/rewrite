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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Field order here is the single source of truth shared with the Java
// GoSumRpcCodec; both must agree exactly.

func init() {
	RegisterValueType(reflect.TypeOf((*golang.GoSum)(nil)), "org.openrewrite.golang.tree.GoSum")
	RegisterValueType(reflect.TypeOf((*golang.GoSumLine)(nil)), "org.openrewrite.golang.tree.GoSum$Line")

	RegisterFactory("org.openrewrite.golang.tree.GoSum", func() any { return &golang.GoSum{} })
	RegisterFactory("org.openrewrite.golang.tree.GoSum$Line", func() any { return &golang.GoSumLine{} })
}

func sendGoSum(gs *golang.GoSum, q *SendQueue) {
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).Ident.String() }, nil)
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).Prefix },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).SourcePath }, nil)
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).Charset }, nil)
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).CharsetBomMarked }, nil)
	q.GetAndSend(gs, func(_ any) any { return nil }, nil) // checksum
	q.GetAndSend(gs, func(_ any) any { return nil }, nil) // fileAttributes
	q.GetAndSendList(gs,
		func(v any) []any { return goSumLineSlice(v.(*golang.GoSum).Lines) },
		goSumLineRPID,
		func(v any) { sendGoSumRightPadded(v, q) })
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoSum).Eof },
		func(v any) { sendSpace(v.(java.Space), q) })
}

func sendGoSumRightPadded(rp any, q *SendQueue) {
	q.GetAndSend(rp, func(v any) any { return v.(java.RightPadded[*golang.GoSumLine]).Element },
		func(v any) { sendGoSumLine(v.(*golang.GoSumLine), q) })
	q.GetAndSend(rp, func(v any) any { return v.(java.RightPadded[*golang.GoSumLine]).After },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(rp, func(v any) any { return v.(java.RightPadded[*golang.GoSumLine]).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
}

func sendGoSumLine(l *golang.GoSumLine, q *SendQueue) {
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).Ident.String() }, nil)
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).Prefix },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).ModulePath }, nil)
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).Version }, nil)
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).GoMod }, nil)
	q.GetAndSend(l, func(v any) any { return v.(*golang.GoSumLine).Hash }, nil)
}

func receiveGoSum(gs *golang.GoSum, q *ReceiveQueue) *golang.GoSum {
	gs.Ident = recvGoModID(q, gs.Ident)
	gs.Prefix = recvGoModSpace(q, gs.Prefix)
	gs.Markers = recvGoModMarkers(q, gs.Markers)
	gs.SourcePath = receiveScalar[string](q, gs.SourcePath)
	gs.Charset = receiveScalar[string](q, gs.Charset)
	gs.CharsetBomMarked = receiveScalar[bool](q, gs.CharsetBomMarked)
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	gs.Lines = recvGoSumLineList(q, gs.Lines)
	gs.Eof = recvGoModSpace(q, gs.Eof)
	return gs
}

func recvGoSumLineList(q *ReceiveQueue, before []java.RightPadded[*golang.GoSumLine]) []java.RightPadded[*golang.GoSumLine] {
	afterAny := q.ReceiveList(goSumLineSlice(before), func(v any) any { return recvGoSumRightPadded(v, q) })
	if afterAny == nil {
		return nil
	}
	out := make([]java.RightPadded[*golang.GoSumLine], len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(java.RightPadded[*golang.GoSumLine])
	}
	return out
}

func recvGoSumRightPadded(v any, q *ReceiveQueue) any {
	var beforeElem *golang.GoSumLine
	beforeAfter := java.EmptySpace
	var beforeMarkers java.Markers
	if rp, ok := v.(java.RightPadded[*golang.GoSumLine]); ok {
		beforeElem, beforeAfter, beforeMarkers = rp.Element, rp.After, rp.Markers
	}
	elemAny := q.Receive(beforeElem, func(e any) any { return recvGoSumLine(e, q) })
	after := recvGoModSpace(q, beforeAfter)
	markers := recvGoModMarkers(q, beforeMarkers)
	var elem *golang.GoSumLine
	if elemAny != nil {
		elem = elemAny.(*golang.GoSumLine)
	}
	return java.RightPadded[*golang.GoSumLine]{Element: elem, After: after, Markers: markers}
}

func recvGoSumLine(baseline any, q *ReceiveQueue) any {
	l, ok := baseline.(*golang.GoSumLine)
	if !ok {
		l = &golang.GoSumLine{}
	}
	l.Ident = recvGoModID(q, l.Ident)
	l.Prefix = recvGoModSpace(q, l.Prefix)
	l.Markers = recvGoModMarkers(q, l.Markers)
	l.ModulePath = receiveScalar[string](q, l.ModulePath)
	l.Version = receiveScalar[string](q, l.Version)
	l.GoMod = receiveScalar[bool](q, l.GoMod)
	l.Hash = receiveScalar[string](q, l.Hash)
	return l
}

func goSumLineSlice(s []java.RightPadded[*golang.GoSumLine]) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func goSumLineRPID(rp any) any {
	return rp.(java.RightPadded[*golang.GoSumLine]).Element.Ident.String()
}
