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
	"time"

	"github.com/openrewrite/rewrite/pkg/tree"
)

// TreeVisitor can visit and transform a tree node.
type TreeVisitor interface {
	Visit(t tree.Tree, p any) tree.Tree
}

// Recipe defines a source code transformation.
type Recipe interface {
	// Name returns the fully qualified recipe name (e.g., "org.openrewrite.golang.ChangePackageName").
	Name() string

	// DisplayName returns a human-readable name for display in UIs.
	DisplayName() string

	// Description returns a markdown-enabled description of the recipe's purpose.
	Description() string

	// Tags returns categorization tags (e.g., "cleanup", "security"). May be nil.
	Tags() []string

	// EstimatedEffortPerOccurrence returns the estimated manual effort per change.
	EstimatedEffortPerOccurrence() time.Duration

	// Editor returns the visitor that performs the transformation.
	// Returns nil for recipes that only compose sub-recipes via RecipeList.
	Editor() TreeVisitor

	// RecipeList returns sub-recipes that run as part of this recipe. May be nil.
	RecipeList() []Recipe

	// Options returns descriptors for this recipe's configurable options.
	Options() []OptionDescriptor
}

// Base provides default implementations for optional Recipe methods.
// Embed it in your recipe struct to avoid implementing every method:
//
//	type MyRecipe struct {
//	    recipe.Base
//	}
type Base struct{}

func (Base) Tags() []string                              { return nil }
func (Base) EstimatedEffortPerOccurrence() time.Duration { return 5 * time.Minute }
func (Base) Editor() TreeVisitor                         { return nil }
func (Base) RecipeList() []Recipe                        { return nil }
func (Base) Options() []OptionDescriptor                 { return nil }

// DelegatesTo marks a recipe that delegates entirely to a Java-side recipe.
// When the Java host calls PrepareRecipe, the Go server includes the
// delegation info in the response, and Java loads the recipe locally
// instead of wrapping it in an RpcRecipe.
type DelegatesTo interface {
	Recipe

	// JavaRecipeName returns the fully qualified Java recipe name
	// (e.g., "org.openrewrite.java.ChangeType").
	JavaRecipeName() string

	// JavaOptions returns options to configure the Java recipe, keyed by option name.
	JavaOptions() map[string]any
}

// OptionDescriptor describes a configurable option on a recipe.
type OptionDescriptor struct {
	// Name is the programmatic name (e.g., "oldPackageName").
	Name string

	// DisplayName is the human-readable label.
	DisplayName string

	// Description explains the option's purpose.
	Description string

	// Example shows a sample value.
	Example string

	// Required indicates the option must be set.
	Required bool

	// Valid lists allowed values (nil means any value is accepted).
	Valid []string

	// Value is the current value, if set.
	Value any
}

// Option creates a required OptionDescriptor with the given name, display name, and description.
// Use the fluent With* methods to customize further:
//
//	recipe.Option("oldName", "Old name", "The identifier to rename.").WithValue(r.OldName)
//	recipe.Option("style", "Style", "The formatting style.").WithValid("tabs", "spaces").WithExample("tabs")
//	recipe.Option("debug", "Debug", "Enable debug output.").AsOptional()
func Option(name, displayName, description string) OptionDescriptor {
	return OptionDescriptor{Name: name, DisplayName: displayName, Description: description, Required: true}
}

// WithValue sets the current value of the option.
func (o OptionDescriptor) WithValue(v any) OptionDescriptor { o.Value = v; return o }

// WithExample sets an example value for the option.
func (o OptionDescriptor) WithExample(e string) OptionDescriptor { o.Example = e; return o }

// WithValid sets the allowed values for the option.
func (o OptionDescriptor) WithValid(v ...string) OptionDescriptor { o.Valid = v; return o }

// AsOptional marks the option as not required.
func (o OptionDescriptor) AsOptional() OptionDescriptor { o.Required = false; return o }

// RecipeDescriptor provides metadata about a recipe for display and serialization.
type RecipeDescriptor struct {
	Name                         string
	DisplayName                  string
	Description                  string
	Tags                         []string
	EstimatedEffortPerOccurrence time.Duration
	Options                      []OptionDescriptor
	RecipeList                   []RecipeDescriptor
}

// Describe creates a RecipeDescriptor from a Recipe.
func Describe(r Recipe) RecipeDescriptor {
	desc := RecipeDescriptor{
		Name:                         r.Name(),
		DisplayName:                  r.DisplayName(),
		Description:                  r.Description(),
		Tags:                         r.Tags(),
		EstimatedEffortPerOccurrence: r.EstimatedEffortPerOccurrence(),
		Options:                      r.Options(),
	}
	if subs := r.RecipeList(); len(subs) > 0 {
		desc.RecipeList = make([]RecipeDescriptor, len(subs))
		for i, sub := range subs {
			desc.RecipeList[i] = Describe(sub)
		}
	}
	return desc
}
