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

package golang

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// RenameXToFlag is a test recipe that renames identifier "x" to "flag".
// Used to validate Go-native recipe execution via the RPC path.
type RenameXToFlag struct {
	recipe.Base
}

func (r *RenameXToFlag) Name() string        { return "org.openrewrite.golang.test.RenameXToFlag" }
func (r *RenameXToFlag) DisplayName() string { return "Rename x to flag (test)" }
func (r *RenameXToFlag) Description() string {
	return "Test recipe that renames identifier x to flag."
}

func (r *RenameXToFlag) Editor() recipe.TreeVisitor {
	v := &renameXVisitor{}
	v.Self = v
	return v
}

type renameXVisitor struct {
	visitor.GoVisitor
}

func (v *renameXVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	ident = v.GoVisitor.VisitIdentifier(ident, p).(*tree.Identifier)
	if ident.Name == "x" {
		c := *ident
		c.Name = "flag"
		return &c
	}
	return ident
}
