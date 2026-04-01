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

// ChangeMethodName renames method invocations matching a pattern.
// Delegates to the Java ChangeMethodName recipe via RPC.
type ChangeMethodName struct {
	recipe.Base
	MethodPattern string
	NewMethodName string
}

func (r *ChangeMethodName) Name() string        { return "org.openrewrite.golang.ChangeMethodName" }
func (r *ChangeMethodName) DisplayName() string  { return "Change method name" }
func (r *ChangeMethodName) Description() string  { return "Rename method invocations matching a method pattern." }
func (r *ChangeMethodName) JavaRecipeName() string { return "org.openrewrite.java.ChangeMethodName" }
func (r *ChangeMethodName) JavaOptions() map[string]any {
	return map[string]any{
		"methodPattern": r.MethodPattern,
		"newMethodName": r.NewMethodName,
	}
}
func (r *ChangeMethodName) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("methodPattern", "Method pattern", "A method pattern to match method invocations.").
			WithExample("fmt.Println(..)").WithValue(r.MethodPattern),
		recipe.Option("newMethodName", "New method name", "The new name for the method.").
			WithExample("Printf").WithValue(r.NewMethodName),
	}
}
