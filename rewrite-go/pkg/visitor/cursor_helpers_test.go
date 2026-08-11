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

package visitor

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestIsFunctionBodyBlock(t *testing.T) {
	block := &java.Block{}
	tests := []struct {
		name   string
		parent java.Tree
		want   bool
	}{
		{"java method declaration parent", &java.MethodDeclaration{}, true},
		{"golang method declaration parent", &golang.MethodDeclaration{}, true},
		{"nested block parent", &java.Block{}, false},
		{"no parent", nil, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var c *Cursor
			if tt.parent == nil {
				c = NewCursor(nil, block)
			} else {
				c = NewCursor(NewCursor(nil, tt.parent), block)
			}
			if got := IsFunctionBodyBlock(c); got != tt.want {
				t.Errorf("IsFunctionBodyBlock() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsInitWrappedIf(t *testing.T) {
	ifStmt := &java.If{}
	tests := []struct {
		name   string
		parent java.Tree
		want   bool
	}{
		{"statement with init parent", &golang.StatementWithInit{}, true},
		{"plain block parent", &java.Block{}, false},
		{"no parent", nil, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var c *Cursor
			if tt.parent == nil {
				c = NewCursor(nil, ifStmt)
			} else {
				c = NewCursor(NewCursor(nil, tt.parent), ifStmt)
			}
			if got := IsInitWrappedIf(c); got != tt.want {
				t.Errorf("IsInitWrappedIf() = %v, want %v", got, tt.want)
			}
		})
	}
}
