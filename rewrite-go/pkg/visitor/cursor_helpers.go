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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Reports whether the block at the cursor is a function's body (its parent is a
// function declaration) rather than a nested block such as a loop or if body.
func IsFunctionBodyBlock(c *Cursor) bool {
	parent := c.Parent()
	if parent == nil {
		return false
	}
	_, ok := parent.Value().(*java.MethodDeclaration)
	return ok
}

// Reports whether the If at the cursor is the inner statement of a
// golang.StatementWithInit, i.e. it carried an `if init; cond` init clause.
func IsInitWrappedIf(c *Cursor) bool {
	parent := c.Parent()
	if parent == nil {
		return false
	}
	_, ok := parent.Value().(*golang.StatementWithInit)
	return ok
}
