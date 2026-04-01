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

import "github.com/openrewrite/rewrite/pkg/recipe"

// ChangeType changes a Go type reference from one fully qualified name to another.
// Delegates to the Java ChangeType recipe via RPC.
type ChangeType struct {
	recipe.Base
	OldFullyQualifiedTypeName string
	NewFullyQualifiedTypeName string
}

func (r *ChangeType) Name() string        { return "org.openrewrite.golang.ChangeType" }
func (r *ChangeType) DisplayName() string  { return "Change type" }
func (r *ChangeType) Description() string  { return "Change a Go type reference from one fully qualified name to another." }
func (r *ChangeType) JavaRecipeName() string { return "org.openrewrite.java.ChangeType" }
func (r *ChangeType) JavaOptions() map[string]any {
	return map[string]any{
		"oldFullyQualifiedTypeName": r.OldFullyQualifiedTypeName,
		"newFullyQualifiedTypeName": r.NewFullyQualifiedTypeName,
	}
}
func (r *ChangeType) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("oldFullyQualifiedTypeName", "Old fully qualified type name", "The fully qualified Go type name to replace.").
			WithExample("github.com/old/pkg.MyType").WithValue(r.OldFullyQualifiedTypeName),
		recipe.Option("newFullyQualifiedTypeName", "New fully qualified type name", "The fully qualified Go type name to use instead.").
			WithExample("github.com/new/pkg.MyType").WithValue(r.NewFullyQualifiedTypeName),
	}
}
