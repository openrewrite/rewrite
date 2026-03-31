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

// FindTypes finds all references to a given type.
// Delegates to the Java FindTypes recipe via RPC.
type FindTypes struct {
	recipe.Base
	FullyQualifiedTypeName string
}

func (r *FindTypes) Name() string        { return "org.openrewrite.golang.search.FindTypes" }
func (r *FindTypes) DisplayName() string  { return "Find types" }
func (r *FindTypes) Description() string  { return "Find all references to a given type." }
func (r *FindTypes) JavaRecipeName() string { return "org.openrewrite.java.search.FindTypes" }
func (r *FindTypes) JavaOptions() map[string]any {
	return map[string]any{
		"fullyQualifiedTypeName": r.FullyQualifiedTypeName,
	}
}
func (r *FindTypes) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("fullyQualifiedTypeName", "Fully qualified type name", "The fully qualified type name to search for.").
			WithExample("fmt.Stringer").WithValue(r.FullyQualifiedTypeName),
	}
}
