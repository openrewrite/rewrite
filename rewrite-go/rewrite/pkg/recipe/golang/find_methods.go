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

// FindMethods finds all method invocations matching a pattern.
// Delegates to the Java FindMethods recipe via RPC.
type FindMethods struct {
	recipe.Base
	MethodPattern string
}

func (r *FindMethods) Name() string        { return "org.openrewrite.golang.search.FindMethods" }
func (r *FindMethods) DisplayName() string  { return "Find methods" }
func (r *FindMethods) Description() string  { return "Find all method invocations matching a method pattern." }
func (r *FindMethods) JavaRecipeName() string { return "org.openrewrite.java.search.FindMethods" }
func (r *FindMethods) JavaOptions() map[string]any {
	return map[string]any{
		"methodPattern": r.MethodPattern,
	}
}
func (r *FindMethods) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("methodPattern", "Method pattern", "A method pattern to match method invocations.").
			WithExample("fmt.Println(..)").WithValue(r.MethodPattern),
	}
}
