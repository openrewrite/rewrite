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

package template

import (
	"strings"
	"time"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// --- Functional builder API ---

// RecipeOption configures a template recipe via NewRecipe.
type RecipeOption func(*templateRecipeConfig)

// BeforeOption configures a single before-pattern.
type BeforeOption func(*beforeSpec)

type beforeSpec struct {
	code    string
	imports []string
	kind    *ScaffoldKind // nil = auto-detect
}

type templateRecipeConfig struct {
	name        string
	displayName string
	description string
	tags        []string
	befores     []beforeSpec
	afterCode   string
	afterImports []string
	afterKind   *ScaffoldKind
	captures    []*Capture
	kind        *ScaffoldKind // global override
}

// RecipeName sets the fully qualified recipe name.
func RecipeName(name string) RecipeOption {
	return func(c *templateRecipeConfig) { c.name = name }
}

// WithDisplayName sets the human-readable display name.
func WithDisplayName(name string) RecipeOption {
	return func(c *templateRecipeConfig) { c.displayName = name }
}

// WithDescription sets the recipe description.
func WithDescription(desc string) RecipeOption {
	return func(c *templateRecipeConfig) { c.description = desc }
}

// WithTags sets categorization tags.
func WithTags(tags ...string) RecipeOption {
	return func(c *templateRecipeConfig) { c.tags = tags }
}

// WithBefore adds a before-pattern. Can be called multiple times for
// Refaster anyOf-style matching (first match wins).
func WithBefore(code string, opts ...BeforeOption) RecipeOption {
	return func(c *templateRecipeConfig) {
		spec := beforeSpec{code: code}
		for _, opt := range opts {
			opt(&spec)
		}
		c.befores = append(c.befores, spec)
	}
}

// WithAfter sets the after-template code.
func WithAfter(code string, opts ...BeforeOption) RecipeOption {
	return func(c *templateRecipeConfig) {
		c.afterCode = code
		spec := beforeSpec{code: code}
		for _, opt := range opts {
			opt(&spec)
		}
		c.afterImports = spec.imports
		c.afterKind = spec.kind
	}
}

// WithCaptures declares the capture placeholders used in templates.
func WithCaptures(caps ...*Capture) RecipeOption {
	return func(c *templateRecipeConfig) { c.captures = caps }
}

// AsExpression forces all templates to be parsed as expressions.
func AsExpression() RecipeOption {
	return func(c *templateRecipeConfig) {
		k := ScaffoldExpression
		c.kind = &k
	}
}

// AsStatement forces all templates to be parsed as statements.
func AsStatement() RecipeOption {
	return func(c *templateRecipeConfig) {
		k := ScaffoldStatement
		c.kind = &k
	}
}

// Imports adds required imports for a before or after template.
func Imports(pkgs ...string) BeforeOption {
	return func(s *beforeSpec) { s.imports = append(s.imports, pkgs...) }
}

// NewRecipe creates a recipe.Recipe from declarative before/after templates.
//
// Example:
//
//	s := template.Expr("s")
//	r := template.NewRecipe(
//	    template.RecipeName("org.openrewrite.golang.RemoveRedundantSprintf"),
//	    template.WithDisplayName("Remove redundant fmt.Sprintf"),
//	    template.WithBefore(fmt.Sprintf(`fmt.Sprintf("%%s", %s)`, s), template.Imports("fmt")),
//	    template.WithAfter(fmt.Sprintf(`%s`, s)),
//	    template.WithCaptures(s),
//	)
func NewRecipe(opts ...RecipeOption) recipe.Recipe {
	cfg := &templateRecipeConfig{}
	for _, opt := range opts {
		opt(cfg)
	}
	return buildRecipe(cfg)
}

func buildRecipe(cfg *templateRecipeConfig) recipe.Recipe {
	caps := cfg.captures

	// Build before patterns
	var befores []*GoPattern
	for _, bs := range cfg.befores {
		kind := resolveKind(bs.kind, cfg.kind, bs.code)
		befores = append(befores, buildPattern(bs.code, caps, bs.imports, kind))
	}

	// Build after template
	afterKind := resolveKind(cfg.afterKind, cfg.kind, cfg.afterCode)
	after := buildTemplate(cfg.afterCode, caps, cfg.afterImports, afterKind)

	v := newTemplateRecipeVisitor(befores, after)

	return &builtTemplateRecipe{
		name:        cfg.name,
		displayName: cfg.displayName,
		description: cfg.description,
		tags:        cfg.tags,
		editor:      v,
	}
}

func resolveKind(specific *ScaffoldKind, global *ScaffoldKind, code string) ScaffoldKind {
	if specific != nil {
		return *specific
	}
	if global != nil {
		return *global
	}
	return detectScaffoldKind(code)
}

func buildPattern(code string, caps []*Capture, imports []string, kind ScaffoldKind) *GoPattern {
	switch kind {
	case ScaffoldStatement:
		return StatementPattern(code).Captures(caps...).Imports(imports...).Build()
	case ScaffoldTopLevel:
		return TopLevel(code).Captures(caps...).Imports(imports...).Build()
	default:
		return Expression(code).Captures(caps...).Imports(imports...).Build()
	}
}

func buildTemplate(code string, caps []*Capture, imports []string, kind ScaffoldKind) *GoTemplate {
	switch kind {
	case ScaffoldStatement:
		return StatementTemplate(code).Captures(caps...).Imports(imports...).Build()
	case ScaffoldTopLevel:
		return TopLevelTemplate(code).Captures(caps...).Imports(imports...).Build()
	default:
		return ExpressionTemplate(code).Captures(caps...).Imports(imports...).Build()
	}
}

// detectScaffoldKind infers whether code is an expression, statement, or
// top-level declaration based on leading keywords.
func detectScaffoldKind(code string) ScaffoldKind {
	trimmed := strings.TrimSpace(code)
	stmtKeywords := []string{"if ", "for ", "switch ", "select ", "go ", "defer ", "return "}
	for _, kw := range stmtKeywords {
		if strings.HasPrefix(trimmed, kw) {
			return ScaffoldStatement
		}
	}
	topKeywords := []string{"func ", "type ", "var ", "const "}
	for _, kw := range topKeywords {
		if strings.HasPrefix(trimmed, kw) {
			return ScaffoldTopLevel
		}
	}
	return ScaffoldExpression
}

// --- Multi-before visitor ---

// templateRecipeVisitor tries each before pattern in order; first match wins.
type templateRecipeVisitor struct {
	visitor.GoVisitor
	befores []*GoPattern
	after   *GoTemplate
}

func newTemplateRecipeVisitor(befores []*GoPattern, after *GoTemplate) *templateRecipeVisitor {
	v := &templateRecipeVisitor{befores: befores, after: after}
	v.Self = v
	return v
}

func (v *templateRecipeVisitor) Visit(t tree.Tree, p any) tree.Tree {
	result := v.GoVisitor.Visit(t, p)
	if result == nil {
		return nil
	}

	j, ok := result.(tree.J)
	if !ok {
		return result
	}

	for _, before := range v.befores {
		match := before.Match(j, nil)
		if match == nil {
			continue
		}
		replaced := v.after.Apply(nil, match)
		if replaced != nil {
			return setLeadingPrefix(replaced, getLeadingPrefix(j))
		}
	}
	return result
}

// --- Built recipe (returned by NewRecipe) ---

type builtTemplateRecipe struct {
	name        string
	displayName string
	description string
	tags        []string
	editor      recipe.TreeVisitor
}

func (r *builtTemplateRecipe) Name() string        { return r.name }
func (r *builtTemplateRecipe) DisplayName() string  { return r.displayName }
func (r *builtTemplateRecipe) Description() string  { return r.description }
func (r *builtTemplateRecipe) Tags() []string       { return r.tags }
func (r *builtTemplateRecipe) EstimatedEffortPerOccurrence() time.Duration { return 5 * time.Minute }
func (r *builtTemplateRecipe) Editor() recipe.TreeVisitor  { return r.editor }
func (r *builtTemplateRecipe) RecipeList() []recipe.Recipe  { return nil }
func (r *builtTemplateRecipe) Options() []recipe.OptionDescriptor { return nil }

// --- Embeddable TemplateRecipe struct ---

// TemplateRecipe is an embeddable base type for struct-based template recipes.
// Embed it and call Init() to wire up the before/after patterns.
//
// Example:
//
//	type MyRecipe struct {
//	    template.TemplateRecipe
//	}
//
//	func NewMyRecipe() *MyRecipe {
//	    s := template.Expr("s")
//	    r := &MyRecipe{}
//	    r.InitTemplate(
//	        template.WithBefore(fmt.Sprintf(`old(%s)`, s)),
//	        template.WithAfter(fmt.Sprintf(`new(%s)`, s)),
//	        template.WithCaptures(s),
//	    )
//	    return r
//	}
type TemplateRecipe struct {
	recipe.Base
	editor recipe.TreeVisitor
}

// InitTemplate configures the template recipe with before/after patterns.
// Must be called before the recipe is used.
func (tr *TemplateRecipe) InitTemplate(opts ...RecipeOption) {
	cfg := &templateRecipeConfig{}
	for _, opt := range opts {
		opt(cfg)
	}
	caps := cfg.captures

	var befores []*GoPattern
	for _, bs := range cfg.befores {
		kind := resolveKind(bs.kind, cfg.kind, bs.code)
		befores = append(befores, buildPattern(bs.code, caps, bs.imports, kind))
	}

	afterKind := resolveKind(cfg.afterKind, cfg.kind, cfg.afterCode)
	after := buildTemplate(cfg.afterCode, caps, cfg.afterImports, afterKind)

	tr.editor = newTemplateRecipeVisitor(befores, after)
}

// Editor returns the auto-generated visitor.
func (tr *TemplateRecipe) Editor() recipe.TreeVisitor {
	return tr.editor
}
