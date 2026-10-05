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
package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A peer sends a cursor as the ids of its path, the innermost value first,
// with ids of its own for the padding and the root marker between the trees.
func TestBuildCursorReadsThePathInnermostFirst(t *testing.T) {
	method := &golang.MethodDeclaration{ID: uuid.New()}
	cu := &golang.CompilationUnit{ID: uuid.New()}
	objects := map[string]any{
		method.ID.String(): method,
		"padding-1":        java.RightPadded[java.Statement]{Element: method},
		cu.ID.String():     cu,
		"root-1":           "root",
	}
	var fetched []string
	fetch := func(id string) any {
		fetched = append(fetched, id)
		return objects[id]
	}

	cursor := buildCursor([]string{method.ID.String(), "padding-1", cu.ID.String(), "root-1"}, fetch)

	require.NotNil(t, cursor)
	assert.Same(t, method, cursor.Value())
	require.NotNil(t, cursor.Parent())
	assert.Same(t, cu, cursor.Parent().Value())
	assert.Nil(t, cursor.Parent().Parent())
	assert.Equal(t, []string{cu.ID.String(), method.ID.String()}, fetched, "only trees are fetched")
}

func TestBuildCursorWithoutIdsIsNoCursor(t *testing.T) {
	assert.Nil(t, buildCursor(nil, func(string) any { return nil }))
}

func TestPrintTakesFromTheCursorItIsSent(t *testing.T) {
	cu, err := goparser.NewGoParser().Parse("m.go", "package main\n\nfunc (t T) M() {}\n")
	require.NoError(t, err)
	wrapper := cu.Statements[0].Element.(*golang.MethodDeclaration)
	// each tree of a cursor arrives as an object of its own, so the parent holds another instance of the tree
	method := *wrapper.Declaration

	printed := func(cursor []string) any {
		s, _ := newResilienceTestServer(t)
		s.localObjects[method.ID.String()] = &method
		s.localObjects[wrapper.ID.String()] = wrapper
		s.localObjects[cu.ID.String()] = cu
		// the engine asks the peer for the tree and each tree of the cursor, none of which has changed
		var replies []byte
		for i := 0; i < 3; i++ {
			replies = append(replies, frameReverseGetObjectReply(t,
				[]map[string]any{{"state": "NO_CHANGE"}, {"state": "END_OF_OBJECT"}})...)
		}
		s.reader = bufio.NewReader(bytes.NewReader(replies))
		s.writer = &bytes.Buffer{}

		request := map[string]any{
			"treeId":         method.ID.String(),
			"sourcePath":     "m.go",
			"sourceFileType": "org.openrewrite.golang.tree.Go$CompilationUnit",
		}
		if cursor != nil {
			request["cursor"] = cursor
		}
		params, err := json.Marshal(request)
		require.NoError(t, err)
		out, rpcErr := s.handlePrint(params)
		require.Nil(t, rpcErr)
		return out
	}

	assert.Equal(t, "\n\nfunc (t T) M() {}",
		printed([]string{wrapper.ID.String(), "padding-1", cu.ID.String(), "root-1"}))
	assert.Equal(t, "func M() {}", printed(nil), "a request without a cursor")
}
