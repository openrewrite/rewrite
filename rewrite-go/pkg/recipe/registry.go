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

type CategoryDescriptor struct {
	DisplayName string
	Description string
}

type RecipeConstructor func(options map[string]any) Recipe

type Registration struct {
	Descriptor  RecipeDescriptor
	Constructor RecipeConstructor

	// Categories is the category path from shallowest to deepest
	// (e.g., []CategoryDescriptor{{DisplayName: "Go"}, {DisplayName: "Cleanup"}}).
	Categories []CategoryDescriptor
}

type Category struct {
	CategoryDescriptor
	Recipes       []Registration
	Subcategories []*Category
}

// Create one with NewRegistry and populate it via Activate:
//
//	registry := recipe.NewRegistry()
//	registry.Activate(golang.Activate, cleanup.Activate)
type Registry struct {
	mu     sync.RWMutex
	root   Category
	byName map[string]*Registration

	// subByName holds the children contributed by composite recipes'
	// RecipeList(). These are resolvable by name (so the CLI can prepare a
	// composite's children individually) but are intentionally kept out of
	// byName / the category tree so they don't surface as standalone
	// marketplace entries. FindRecipe falls back to this map.
	subByName map[string]*Registration
}

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

func NewRegistry() *Registry {
	return &Registry{
		byName:    make(map[string]*Registration),
		subByName: make(map[string]*Registration),
	}
}

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

	// Composite recipes advertise their RecipeList() children in the
	// descriptor (see Describe), so the CLI prepares each child by name.
	// Register those children so FindRecipe can resolve them — but only into
	// subByName, never byName or the category tree, so they don't appear as
	// standalone marketplace recipes.
	r.registerSubRecipes(prototype)
}

// registerSubRecipes recursively records a recipe's RecipeList() children in
// subByName so they are resolvable by name. The traversal is guarded against
// cycles (and shared sub-recipes) via the subByName presence check.
func (r *Registry) registerSubRecipes(rec Recipe) {
	for _, sub := range rec.RecipeList() {
		name := sub.Name()
		if _, exists := r.subByName[name]; !exists {
			// A child that is also registered as a top-level recipe is
			// already resolvable via byName; FindRecipe checks byName first.
			child := sub
			r.subByName[name] = &Registration{
				Descriptor:  Describe(child),
				Constructor: func(map[string]any) Recipe { return child },
			}
			r.registerSubRecipes(child)
		}
	}
}

func (r *Registry) RegisterDescriptor(desc RecipeDescriptor) {
	r.RegisterWithCategories(desc, nil)
}

func (r *Registry) RegisterWithCategories(desc RecipeDescriptor, categories []CategoryDescriptor) {
	r.mu.Lock()
	defer r.mu.Unlock()

	// Don't overwrite an existing registration that has a real constructor
	// (e.g., registered via Activate in the custom binary) with a nil
	// constructor from the installer's descriptor-only registration.
	if existing, ok := r.byName[desc.Name]; ok && existing.Constructor != nil {
		testInstance := existing.Constructor(nil)
		if testInstance != nil {
			// Existing registration has a real implementation — keep it,
			// just update categories if provided.
			if len(categories) > 0 {
				existing.Categories = categories
			}
			return
		}
	}
	reg := &Registration{
		Descriptor:  desc,
		Constructor: func(options map[string]any) Recipe { return nil },
		Categories:  categories,
	}
	r.byName[desc.Name] = reg
}

func (r *Registry) FindRecipe(name string) (*Registration, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	if reg, ok := r.byName[name]; ok {
		return reg, true
	}
	// Fall back to composite children (e.g. "Parent$Keep") contributed via
	// RecipeList(), which are not standalone marketplace recipes.
	reg, ok := r.subByName[name]
	return reg, ok
}

func (r *Registry) AllRecipes() []RecipeDescriptor {
	r.mu.RLock()
	defer r.mu.RUnlock()
	result := make([]RecipeDescriptor, 0, len(r.byName))
	for _, reg := range r.byName {
		result = append(result, reg.Descriptor)
	}
	return result
}

func (r *Registry) AllRegistrations() []Registration {
	r.mu.RLock()
	defer r.mu.RUnlock()
	result := make([]Registration, 0, len(r.byName))
	for _, reg := range r.byName {
		result = append(result, *reg)
	}
	return result
}

func (r *Registry) Categories() []*Category {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return r.root.Subcategories
}

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
