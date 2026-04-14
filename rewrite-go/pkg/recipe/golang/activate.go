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

import "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"

// Activate registers all Go recipes with the given registry.
func Activate(r *recipe.Registry) {
	golangCategory := recipe.CategoryDescriptor{DisplayName: "Go"}
	searchCategory := recipe.CategoryDescriptor{DisplayName: "Search"}

	r.Register(&ChangeType{}, golangCategory)
	r.Register(&ChangeMethodName{}, golangCategory)
	r.Register(&FindTypes{}, golangCategory, searchCategory)
	r.Register(&FindMethods{}, golangCategory, searchCategory)
	r.Register(&RenameXToFlag{}, golangCategory)
}
