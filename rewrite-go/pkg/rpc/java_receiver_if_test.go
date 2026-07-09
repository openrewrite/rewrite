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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestJavaReceiverVisitIfUsesExistingElsePartAsReceiveBaseline(t *testing.T) {
	elseType := "org.openrewrite.java.tree.J$If$Else"
	beforeElseBody := &java.Block{}
	beforeElsePadding := java.Space{Whitespace: " "}
	before := &java.If{
		ElsePart: &java.Else{
			Prefix: beforeElsePadding,
			Body: java.RightPadded[java.Statement]{
				Element: beforeElseBody,
			},
		},
	}
	// This is the wire shape Java emits when the else node changes, but its
	// nested prefix and body fields are unchanged relative to the prior tree.
	messages := []RpcObjectData{
		{State: NoChange},                     // ifCondition
		{State: NoChange},                     // thenPart
		{State: Change, ValueType: &elseType}, // elsePart
		{State: NoChange},                     // else.id
		{State: NoChange},                     // else.prefix
		{State: NoChange},                     // else.markers
		{State: NoChange},                     // else.body
	}
	q := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		return messages
	})

	got := NewGoReceiver().VisitIf(before, q).(*java.If)

	if got.ElsePart == nil {
		t.Fatal("ElsePart: got nil, want non-nil")
	}
	if !reflect.DeepEqual(got.ElsePart.Prefix, beforeElsePadding) {
		t.Fatalf("ElsePart.Prefix: got %+v, want %+v", got.ElsePart.Prefix, beforeElsePadding)
	}
	if got.ElsePart.Body.Element != beforeElseBody {
		t.Fatalf("ElsePart.Body.Element: got %p, want existing body %p", got.ElsePart.Body.Element, beforeElseBody)
	}
}
