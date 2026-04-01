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

import (
	"reflect"
	"strings"
	"sync"
)

// CategoryDescriptor describes a recipe category for hierarchical organization.
type CategoryDescriptor struct {
	DisplayName string
	Description string
}

// RecipeConstructor creates a new Recipe instance, optionally configured with options.
type RecipeConstructor func(options map[string]any) Recipe

// Registration holds a recipe constructor and its category path.
type Registration struct {
	// Descriptor provides metadata about the recipe.
	Descriptor RecipeDescriptor

	// Constructor creates new instances of the recipe with the given options.
	Constructor RecipeConstructor

	// Categories is the category path from shallowest to deepest
	// (e.g., []CategoryDescriptor{{DisplayName: "Go"}, {DisplayName: "Cleanup"}}).
	Categories []CategoryDescriptor
}

// Category is a node in the recipe category tree.
type Category struct {
	CategoryDescriptor
	Recipes       []Registration
	Subcategories []*Category
}

// Registry holds all registered recipes organized by category.
// Create one with NewRegistry and populate it via Activate:
//
//	registry := recipe.NewRegistry()
//	registry.Activate(golang.Activate, cleanup.Activate)
type Registry struct {
	mu     sync.RWMutex
	root   Category
	byName map[string]*Registration
}

// Activator is a function that registers recipes with a registry.
// Each module provides an Activate function that the caller passes
// to Registry.Activate:
//
//	// In package golang/recipes/cleanup:
//	func Activate(r *recipe.Registry) {
//	    r.Register(&RemoveUnusedImports{},
//	        recipe.CategoryDescriptor{DisplayName: "Go"},
//	        recipe.CategoryDescriptor{DisplayName: "Cleanup"})
//	}
//
//	// At the call site:
//	registry := recipe.NewRegistry()
//	registry.Activate(cleanup.Activate, security.Activate)
type Activator func(registry *Registry)

// NewRegistry creates an empty registry.
func NewRegistry() *Registry {
	return &Registry{
		byName: make(map[string]*Registration),
	}
}

// Register adds a recipe to the registry.
// The prototype is used to extract the recipe descriptor. A constructor is
// automatically derived via reflection: it creates a new instance of the
// prototype's type and sets exported fields from the options map. Option
// names are mapped to field names by capitalizing the first letter
// (e.g., option "oldName" sets field "OldName").
//
// For recipes without options, the prototype itself is returned.
func (r *Registry) Register(prototype Recipe, categories ...CategoryDescriptor) {
	r.mu.Lock()
	defer r.mu.Unlock()

	desc := Describe(prototype)
	constructor := newReflectConstructor(prototype)

	reg := &Registration{
		Descriptor:  desc,
		Constructor: constructor,
		Categories:  categories,
	}

	r.byName[desc.Name] = reg

	// Insert into category tree.
	cat := &r.root
	for _, cd := range categories {
		cat = r.findOrCreateSubcategory(cat, cd)
	}
	cat.Recipes = append(cat.Recipes, *reg)
}

// RegisterDescriptor registers a recipe from its descriptor (used for dynamically loaded recipes).
// The constructor returns nil since the recipe implementation lives in the external module.
func (r *Registry) RegisterDescriptor(desc RecipeDescriptor) {
	r.mu.Lock()
	defer r.mu.Unlock()

	reg := &Registration{
		Descriptor:  desc,
		Constructor: func(options map[string]any) Recipe { return nil },
	}
	r.byName[desc.Name] = reg
}

// FindRecipe looks up a registration by fully qualified recipe name.
func (r *Registry) FindRecipe(name string) (*Registration, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	reg, ok := r.byName[name]
	return reg, ok
}

// AllRecipes returns descriptors for all registered recipes.
func (r *Registry) AllRecipes() []RecipeDescriptor {
	r.mu.RLock()
	defer r.mu.RUnlock()
	result := make([]RecipeDescriptor, 0, len(r.byName))
	for _, reg := range r.byName {
		result = append(result, reg.Descriptor)
	}
	return result
}

// Categories returns the top-level categories.
func (r *Registry) Categories() []*Category {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return r.root.Subcategories
}

// Activate runs the given activators against this registry.
// Each activator calls Register to add its recipes. This is the
// single entry point for populating a registry.
//
//	registry := recipe.NewRegistry()
//	registry.Activate(cleanup.Activate, security.Activate)
func (r *Registry) Activate(activators ...Activator) {
	for _, a := range activators {
		a(r)
	}
}

func (r *Registry) findOrCreateSubcategory(parent *Category, desc CategoryDescriptor) *Category {
	for _, sub := range parent.Subcategories {
		if sub.DisplayName == desc.DisplayName {
			return sub
		}
	}
	sub := &Category{CategoryDescriptor: desc}
	parent.Subcategories = append(parent.Subcategories, sub)
	return sub
}

// newReflectConstructor creates a RecipeConstructor that instantiates new
// copies of the prototype's concrete type via reflection. Option names are
// mapped to exported struct fields by capitalizing the first letter
// (e.g., "oldName" → "OldName").
func newReflectConstructor(prototype Recipe) RecipeConstructor {
	t := reflect.TypeOf(prototype)
	isPtr := t.Kind() == reflect.Ptr
	if isPtr {
		t = t.Elem()
	}

	return func(options map[string]any) Recipe {
		v := reflect.New(t)
		elem := v.Elem()

		for name, val := range options {
			fieldName := strings.ToUpper(name[:1]) + name[1:]
			f := elem.FieldByName(fieldName)
			if f.IsValid() && f.CanSet() {
				f.Set(reflect.ValueOf(val))
			}
		}

		if isPtr {
			return v.Interface().(Recipe)
		}
		return v.Elem().Interface().(Recipe)
	}
}
