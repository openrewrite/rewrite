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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
)

func TestCommentSendsTextCommentValueType(t *testing.T) {
	// given
	cu, err := parser.NewGoParser().Parse("test.go", "package main\n\n// hello\nfunc hello() {\n}\n")
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	// when
	var messages []rpc.RpcObjectData
	q := rpc.NewSendQueue(1000, func(batch []rpc.RpcObjectData) {
		messages = append(messages, batch...)
	}, make(map[uintptr]int))
	rpc.NewGoSender().Visit(cu, q)
	q.Flush()

	// then
	// On the Java side, Comment is an interface and TextComment is the concrete class.
	// Sending "org.openrewrite.java.tree.Comment" causes InstantiationError
	// because objenesis cannot instantiate an interface.
	foundTextComment := false
	for _, msg := range messages {
		if msg.ValueType == nil {
			continue
		}
		if *msg.ValueType == "org.openrewrite.java.tree.Comment" {
			t.Fatal("Comment should be sent as org.openrewrite.java.tree.TextComment, not Comment (interface)")
		}
		if *msg.ValueType == "org.openrewrite.java.tree.TextComment" {
			foundTextComment = true
		}
	}
	if !foundTextComment {
		t.Fatal("expected at least one RPC message with valueType org.openrewrite.java.tree.TextComment")
	}
}
