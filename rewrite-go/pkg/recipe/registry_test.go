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

package recipe

import "testing"

// leafRecipe is a minimal standalone recipe used as a composite child.
type leafRecipe struct {
	Base
	name string
}

func (l *leafRecipe) Name() string        { return l.name }
func (l *leafRecipe) DisplayName() string { return l.name }
func (l *leafRecipe) Description() string { return "Leaf recipe " + l.name }

// compositeRecipe composes child recipes via RecipeList, mirroring the Java
// getRecipeList() composition pattern (and Go template.NewRecipe sub-recipes).
type compositeRecipe struct {
	Base
	children []Recipe
}

func (c *compositeRecipe) Name() string         { return "com.example.Composite" }
func (c *compositeRecipe) DisplayName() string  { return "Composite" }
func (c *compositeRecipe) Description() string  { return "A composite recipe." }
func (c *compositeRecipe) RecipeList() []Recipe { return c.children }

func newComposite() *compositeRecipe {
	return &compositeRecipe{
		children: []Recipe{
			&leafRecipe{name: "com.example.Composite$Keep"},
			&leafRecipe{name: "com.example.Composite$Negate"},
		},
	}
}

// TestRegisterResolvesCompositeChildren verifies that the children returned by
// a recipe's RecipeList() are resolvable by FindRecipe (so the CLI can prepare
// them by name) even though only the prototype is registered via Register.
func TestRegisterResolvesCompositeChildren(t *testing.T) {
	r := NewRegistry()
	r.Register(newComposite(), CategoryDescriptor{DisplayName: "Example"})

	// Parent resolves.
	if _, ok := r.FindRecipe("com.example.Composite"); !ok {
		t.Fatal("expected parent recipe to resolve")
	}

	// Children with '$' in their names resolve and round-trip correctly.
	for _, child := range []string{"com.example.Composite$Keep", "com.example.Composite$Negate"} {
		reg, ok := r.FindRecipe(child)
		if !ok {
			t.Fatalf("expected child recipe %q to resolve via FindRecipe", child)
		}
		inst := reg.Constructor(nil)
		if inst == nil {
			t.Fatalf("expected child recipe %q constructor to return an instance", child)
		}
		if inst.Name() != child {
			t.Errorf("child %q resolved to instance with name %q", child, inst.Name())
		}
	}
}

// TestCompositeChildrenNotInMarketplace verifies that composite children are not
// surfaced as standalone marketplace entries: they must not appear in
// AllRegistrations/AllRecipes nor in the category tree.
func TestCompositeChildrenNotInMarketplace(t *testing.T) {
	r := NewRegistry()
	r.Register(newComposite(), CategoryDescriptor{DisplayName: "Example"})

	childNames := map[string]bool{
		"com.example.Composite$Keep":   true,
		"com.example.Composite$Negate": true,
	}

	for _, reg := range r.AllRegistrations() {
		if childNames[reg.Descriptor.Name] {
			t.Errorf("child recipe %q must not appear in AllRegistrations (marketplace)", reg.Descriptor.Name)
		}
	}
	for _, desc := range r.AllRecipes() {
		if childNames[desc.Name] {
			t.Errorf("child recipe %q must not appear in AllRecipes", desc.Name)
		}
	}

	// And not in the category tree.
	var walk func(c *Category)
	walk = func(c *Category) {
		for _, reg := range c.Recipes {
			if childNames[reg.Descriptor.Name] {
				t.Errorf("child recipe %q must not appear in the category tree", reg.Descriptor.Name)
			}
		}
		for _, sub := range c.Subcategories {
			walk(sub)
		}
	}
	for _, cat := range r.Categories() {
		walk(cat)
	}
}
