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
	"time"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// TreeVisitor can visit and transform a tree node. Visitors that need
// ancestor context (a "cursor") expose it as state — typically by
// embedding visitor.GoVisitor and calling its Cursor() accessor — to
// match the Java OpenRewrite visitor pattern.
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

	// Preconditions returns sub-recipes that gate execution: this recipe only runs
	// when all preconditions match. May be nil.
	Preconditions() []Recipe

	// DataTables returns descriptors for any DataTable rows this recipe emits.
	// May be nil. The runtime for writing rows is provided by the DataTable
	// type and DataTableStore (separate from these descriptors).
	DataTables() []DataTableDescriptor

	// Maintainers returns the people responsible for the recipe. May be nil.
	Maintainers() []Maintainer

	// Contributors returns the people who have contributed code or design.
	// May be nil.
	Contributors() []Contributor

	// Examples returns before/after examples illustrating the recipe.
	// May be nil.
	Examples() []Example
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
func (Base) Preconditions() []Recipe                     { return nil }
func (Base) DataTables() []DataTableDescriptor           { return nil }
func (Base) Maintainers() []Maintainer                   { return nil }
func (Base) Contributors() []Contributor                 { return nil }
func (Base) Examples() []Example                         { return nil }

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

// TypeName returns a Java-style type name derived from the option's Value.
// Used to populate the marketplace option `type` wire field.
func (o OptionDescriptor) TypeName() string {
	if o.Value == nil {
		return "String"
	}
	switch reflect.TypeOf(o.Value).Kind() {
	case reflect.Bool:
		return "Boolean"
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32:
		return "Integer"
	case reflect.Int64:
		return "Long"
	case reflect.Float32, reflect.Float64:
		return "Double"
	case reflect.Slice, reflect.Array:
		return "List"
	default:
		return "String"
	}
}

// DataTableDescriptor describes a DataTable a recipe emits.
type DataTableDescriptor struct {
	Name        string
	DisplayName string
	Description string
	Columns     []ColumnDescriptor
}

// ColumnDescriptor describes a column within a DataTable.
type ColumnDescriptor struct {
	Name        string
	DisplayName string
	Description string
	Type        string // e.g. "String", "Integer", "Long"
}

// Maintainer represents a recipe maintainer.
type Maintainer struct {
	Name  string
	Email string
	Logo  string
}

// Contributor represents someone who has contributed to the recipe.
type Contributor struct {
	Name      string
	Email     string
	LineCount int
}

// Example is a before/after example illustrating the recipe.
type Example struct {
	Description string
	Sources     []ExampleSource
	Parameters  []string
}

// ExampleSource is one before/after pair within an Example.
type ExampleSource struct {
	Before   string
	After    string
	Path     string
	Language string
}

// RecipeDescriptor provides metadata about a recipe for display and serialization.
type RecipeDescriptor struct {
	Name                         string
	DisplayName                  string
	Description                  string
	Tags                         []string
	EstimatedEffortPerOccurrence time.Duration
	Options                      []OptionDescriptor
	RecipeList                   []RecipeDescriptor
	Preconditions                []RecipeDescriptor
	DataTables                   []DataTableDescriptor
	Maintainers                  []Maintainer
	Contributors                 []Contributor
	Examples                     []Example
}

// Describe creates a RecipeDescriptor from a Recipe. Recursive descriptors
// (RecipeList, Preconditions) are protected against cycles via a visited set
// keyed by recipe name; if a recipe name re-appears in the descent, a stub
// descriptor with just the name and display name is returned in its place.
func Describe(r Recipe) RecipeDescriptor {
	return describe(r, map[string]bool{})
}

func describe(r Recipe, seen map[string]bool) RecipeDescriptor {
	name := r.Name()
	if seen[name] {
		return RecipeDescriptor{Name: name, DisplayName: r.DisplayName()}
	}
	seen[name] = true
	defer delete(seen, name)

	desc := RecipeDescriptor{
		Name:                         name,
		DisplayName:                  r.DisplayName(),
		Description:                  r.Description(),
		Tags:                         r.Tags(),
		EstimatedEffortPerOccurrence: r.EstimatedEffortPerOccurrence(),
		Options:                      r.Options(),
		DataTables:                   r.DataTables(),
		Maintainers:                  r.Maintainers(),
		Contributors:                 r.Contributors(),
		Examples:                     r.Examples(),
	}
	if subs := r.RecipeList(); len(subs) > 0 {
		desc.RecipeList = make([]RecipeDescriptor, len(subs))
		for i, sub := range subs {
			desc.RecipeList[i] = describe(sub, seen)
		}
	}
	if pres := r.Preconditions(); len(pres) > 0 {
		desc.Preconditions = make([]RecipeDescriptor, len(pres))
		for i, pre := range pres {
			desc.Preconditions[i] = describe(pre, seen)
		}
	}
	return desc
}
