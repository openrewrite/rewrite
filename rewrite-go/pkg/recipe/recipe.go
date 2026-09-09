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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TreeVisitor can visit and transform a tree node. Visitors that need
// ancestor context (a "cursor") expose it as state — typically by
// embedding visitor.GoVisitor and calling its Cursor() accessor — to
// match the Java OpenRewrite visitor pattern.
type TreeVisitor interface {
	Visit(t java.Tree, p any) java.Tree
}

// Recipe defines a source code transformation.
type Recipe interface {
	Name() string

	DisplayName() string

	Description() string

	Tags() []string

	EstimatedEffortPerOccurrence() time.Duration

	// Returns nil for recipes that only compose sub-recipes via RecipeList.
	Editor() TreeVisitor

	RecipeList() []Recipe

	Options() []OptionDescriptor

	// May be nil. The runtime for writing rows is provided by the DataTable
	// type and DataTableStore (separate from these descriptors).
	DataTables() []DataTableDescriptor

	Maintainers() []Maintainer

	// May be nil.
	Contributors() []Contributor

	// May be nil.
	Examples() []Example
}

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

	JavaRecipeName() string

	JavaOptions() map[string]any
}

type OptionDescriptor struct {
	Name        string
	DisplayName string
	Description string
	Example     string
	Required    bool
	Valid       []string
	Value       any
}

// Use the fluent With* methods to customize further:
//
//	recipe.Option("oldName", "Old name", "The identifier to rename.").WithValue(r.OldName)
//	recipe.Option("style", "Style", "The formatting style.").WithValid("tabs", "spaces").WithExample("tabs")
//	recipe.Option("debug", "Debug", "Enable debug output.").AsOptional()
func Option(name, displayName, description string) OptionDescriptor {
	return OptionDescriptor{Name: name, DisplayName: displayName, Description: description, Required: true}
}

func (o OptionDescriptor) WithValue(v any) OptionDescriptor { o.Value = v; return o }

func (o OptionDescriptor) WithExample(e string) OptionDescriptor { o.Example = e; return o }

func (o OptionDescriptor) WithValid(v ...string) OptionDescriptor { o.Valid = v; return o }

func (o OptionDescriptor) AsOptional() OptionDescriptor { o.Required = false; return o }

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

type DataTableDescriptor struct {
	Name        string
	DisplayName string
	Description string
	Columns     []ColumnDescriptor
}

type ColumnDescriptor struct {
	Name        string
	DisplayName string
	Description string
	Type        string // e.g. "String", "Integer", "Long"
}

type Maintainer struct {
	Name  string
	Email string
	Logo  string
}

type Contributor struct {
	Name      string
	Email     string
	LineCount int
}

type Example struct {
	Description string
	Sources     []ExampleSource
	Parameters  []string
}

type ExampleSource struct {
	Before   string
	After    string
	Path     string
	Language string
}

// Note: the Preconditions field carries declarative-recipe metadata
// (the Java RecipeDescriptor.preconditions list, for YAML
// DeclarativeRecipe). Runtime precondition gates are expressed via the
// preconditions package's Check wrapper around Editor() and travel
// through the editPreconditions wire slot, not this descriptor field.
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
// (RecipeList) are protected against cycles via a visited set keyed by
// recipe name; if a recipe name re-appears in the descent, a stub
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
	return desc
}
