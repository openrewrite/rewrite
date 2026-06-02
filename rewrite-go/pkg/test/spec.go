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

package test

import (
	"math"
	"path"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// SourceSpec describes a Go source file for testing.
type SourceSpec struct {
	Before      string
	After       *string // nil means no change expected (parse-print idempotence only)
	Path        string
	Markers     []java.Marker                                  // markers attached to the parsed source after parse
	AfterRecipe func(t *testing.T, cu *golang.CompilationUnit) // optional post-parse assertion callback (.go files only)
}

// Sources is the unit RewriteRun consumes. Single SourceSpecs and project
// wrappers (GoProject, etc.) both satisfy it so the same harness call can
// mix flat .go files with multi-file projects.
type Sources interface {
	Expand() []SourceSpec
}

// Expand makes a SourceSpec usable wherever a Sources is expected.
func (s SourceSpec) Expand() []SourceSpec { return []SourceSpec{s} }

// WithPath returns a copy of s with Path set. Use this in multi-package
// projects so the test harness can locate each file's package directory.
func (s SourceSpec) WithPath(p string) SourceSpec {
	s.Path = p
	return s
}

// WithAfterRecipe returns a copy of s with the post-parse callback set.
// The callback fires after parsing (with type attribution wired) and
// before recipe application; recipes can read s.Markers off the cu and
// assert on resolved types.
func (s SourceSpec) WithAfterRecipe(fn func(t *testing.T, cu *golang.CompilationUnit)) SourceSpec {
	s.AfterRecipe = fn
	return s
}

// project wraps a set of sources and tags each with a GoProject marker on
// expansion. Mirrors Assertions.goProject(name, ...) on the Java side.
type project struct {
	name  string
	inner []Sources
}

func (p project) Expand() []SourceSpec {
	marker := golang.NewGoProject(p.name)
	var out []SourceSpec
	for _, s := range p.inner {
		for _, ss := range s.Expand() {
			ss.Markers = append(append([]java.Marker{}, ss.Markers...), marker)
			out = append(out, ss)
		}
	}
	mergeGoSumIntoGoMod(out)
	propagateModuleResolution(out)
	return out
}

// propagateModuleResolution copies the go.mod's GoResolutionResult
// marker onto every sibling .go SourceSpec in the project. Recipes
// that need module context per file (e.g. RenamePackage's
// fileBelongsTo check) can then read it directly off the
// CompilationUnit's Markers without re-walking the project. Java does
// the equivalent at parse time by attaching the parsed-go.mod marker
// to each CU.
func propagateModuleResolution(specs []SourceSpec) {
	var mrr *golang.GoResolutionResult
	for i := range specs {
		if specs[i].Path == "go.mod" {
			if found := FindGoResolutionResult(specs[i]); found != nil && found.ModulePath != "" {
				mrr = found
				break
			}
		}
	}
	if mrr == nil {
		return
	}
	for i := range specs {
		if !strings.HasSuffix(specs[i].Path, ".go") {
			continue
		}
		// Skip if already present (e.g. caller pre-attached one).
		alreadyHas := false
		for _, m := range specs[i].Markers {
			if _, ok := m.(golang.GoResolutionResult); ok {
				alreadyHas = true
				break
			}
		}
		if alreadyHas {
			continue
		}
		specs[i].Markers = append(append([]java.Marker{}, specs[i].Markers...), *mrr)
	}
}

// mergeGoSumIntoGoMod finds a sibling go.sum spec inside the same expanded
// project and merges its parsed ResolvedDependencies into the sibling
// go.mod's GoResolutionResult marker. Mirrors the Java side, where
// GoModParser#parseSumSibling reads go.sum off disk during parse — but Go
// tests don't write to disk, so we do the merge in memory.
func mergeGoSumIntoGoMod(specs []SourceSpec) {
	var sumIdx, modIdx, markerIdx = -1, -1, -1
	for i, s := range specs {
		switch s.Path {
		case "go.sum":
			sumIdx = i
		case "go.mod":
			modIdx = i
			for j, m := range s.Markers {
				if _, ok := m.(golang.GoResolutionResult); ok {
					markerIdx = j
				}
			}
		}
	}
	if sumIdx < 0 || modIdx < 0 || markerIdx < 0 {
		return
	}
	resolved := parser.ParseGoSum(specs[sumIdx].Before)
	if len(resolved) == 0 {
		return
	}
	mrr := specs[modIdx].Markers[markerIdx].(golang.GoResolutionResult)
	mrr.ResolvedDependencies = resolved
	specs[modIdx].Markers[markerIdx] = mrr
}

// GoProject groups a go.mod and one or more .go SourceSpecs as siblings of
// a single project. Every child receives a golang.GoProject marker. Mirrors
// the Java-side Assertions.goProject(name, sources...).
//
// Example:
//
//	spec.RewriteRun(t,
//	    test.GoProject("foo",
//	        test.GoMod("module example.com/foo\ngo 1.22\n"),
//	        test.Golang("package main\nfunc main(){}\n"),
//	    ),
//	)
func GoProject(name string, sources ...Sources) Sources {
	return project{name: name, inner: sources}
}

// GoMod creates a SourceSpec for go.mod content. The content is dedented
// the same way Golang(...) is and parsed (via parser.ParseGoMod) at
// construction time so the resulting golang.GoResolutionResult marker is
// already attached to spec.Markers — recipes / tests can read module
// path, requires, replaces, etc. without re-parsing.
//
// If the content fails to parse the spec is returned with no
// GoResolutionResult marker; the test still round-trips the content
// verbatim, mirroring the Java goMod test helper's behavior on bad input.
//
// When a sibling GoSum(...) exists in the same project, its parsed
// ResolvedDependencies are merged into the GoResolutionResult marker at
// project-expansion time (see project.Expand).
func GoMod(before string, after ...string) SourceSpec {
	content := TrimIndent(before)
	spec := SourceSpec{
		Before: content,
		Path:   "go.mod",
	}
	if mrr, err := parser.ParseGoMod("go.mod", content); err == nil && mrr != nil {
		spec.Markers = append(spec.Markers, *mrr)
	}
	if len(after) > 0 {
		a := TrimIndent(after[0])
		spec.After = &a
	}
	return spec
}

// GoSum creates a SourceSpec for go.sum content. The harness round-trips
// the content verbatim (no recipe processing today) and, when a sibling
// GoMod(...) is present in the same GoProject, merges the parsed
// ResolvedDependencies into the GoMod's GoResolutionResult marker.
func GoSum(before string, after ...string) SourceSpec {
	content := TrimIndent(before)
	spec := SourceSpec{
		Before: content,
		Path:   "go.sum",
	}
	if len(after) > 0 {
		a := TrimIndent(after[0])
		spec.After = &a
	}
	return spec
}

// FindGoResolutionResult walks a SourceSpec's markers for the parsed
// go.mod marker. Returns nil if not present (e.g. on a Golang(...) source).
func FindGoResolutionResult(spec SourceSpec) *golang.GoResolutionResult {
	for _, m := range spec.Markers {
		if mrr, ok := m.(golang.GoResolutionResult); ok {
			return &mrr
		}
	}
	return nil
}

// parsePackageGroups groups .go SourceSpecs by package directory and
// parses each group together via parser.ParsePackage so files in the same
// package share a types.Info — file A can see file B's symbols. Returns
// a map from the spec's index in `flat` to its CompilationUnit so two
// specs sharing the same Path don't clobber each other in the result.
func parsePackageGroups(t *testing.T, p *parser.GoParser, flat []SourceSpec) map[int]*golang.CompilationUnit {
	t.Helper()
	type indexed struct {
		idx   int
		input parser.FileInput
	}
	byDir := map[string][]indexed{}
	for i, s := range flat {
		if !strings.HasSuffix(s.Path, ".go") {
			continue
		}
		dir := path.Dir(s.Path)
		byDir[dir] = append(byDir[dir], indexed{idx: i, input: parser.FileInput{Path: s.Path, Content: s.Before}})
	}

	out := map[int]*golang.CompilationUnit{}
	for dir, group := range byDir {
		// Pre-filter against BuildContext so post-parse `cus` aligns
		// with the included subset of `group`.
		included := make([]indexed, 0, len(group))
		files := make([]parser.FileInput, 0, len(group))
		for _, g := range group {
			if !parser.MatchBuildContext(p.BuildContext, path.Base(g.input.Path), g.input.Content) {
				continue
			}
			included = append(included, g)
			files = append(files, g.input)
		}
		if len(files) == 0 {
			continue
		}
		cus, err := p.ParsePackage(files)
		if err != nil {
			t.Fatalf("parse error in package %s: %v", dir, err)
		}
		for i, cu := range cus {
			out[included[i].idx] = cu
		}
	}
	return out
}

// buildProjectImporter scans a flattened source list for a go.mod with
// a GoResolutionResult marker. If found, registers every sibling .go
// file AND every go.mod-declared require with a ProjectImporter so:
//   - intra-project imports type-check against real sources;
//   - imports of declared third-party modules resolve to stub packages.
//
// Returns nil when there's no module context — the caller should fall
// back to importer.Default().
func buildProjectImporter(flat []SourceSpec) *parser.ProjectImporter {
	var mrr *golang.GoResolutionResult
	for _, s := range flat {
		if found := FindGoResolutionResult(s); found != nil && found.ModulePath != "" {
			mrr = found
			break
		}
	}
	if mrr == nil {
		return nil
	}
	pi := parser.NewProjectImporter(mrr.ModulePath, nil)
	for _, req := range mrr.Requires {
		pi.AddRequire(req.ModulePath)
	}
	for _, s := range flat {
		if !strings.HasSuffix(s.Path, ".go") {
			continue
		}
		pi.AddSource(s.Path, s.Before)
	}
	return pi
}

// Golang creates a SourceSpec for Go source code.
// The source string is automatically dedented: the common leading
// whitespace across all non-empty lines is stripped, and a leading/trailing
// blank line (from the backtick) is removed. This lets test strings be
// indented to match their surrounding code:
//
//	Golang(`
//		package main
//
//		func hello() {
//			return "hi"
//		}
//	`)
func Golang(before string, after ...string) SourceSpec {
	spec := SourceSpec{
		Before: TrimIndent(before),
		Path:   "test.go",
	}
	if len(after) > 0 {
		a := TrimIndent(after[0])
		spec.After = &a
	}
	return spec
}

// GolangRaw creates a SourceSpec from raw Go source (no indent trimming).
func GolangRaw(before string, after ...string) SourceSpec {
	spec := SourceSpec{
		Before: before,
		Path:   "test.go",
	}
	if len(after) > 0 {
		spec.After = &after[0]
	}
	return spec
}

// TrimIndent removes the common leading whitespace from all non-empty lines,
// and strips a single leading and trailing blank line (artifacts of backtick
// string literals that start/end on their own line).
func TrimIndent(s string) string {
	lines := strings.Split(s, "\n")

	// Strip leading blank line (the newline right after opening backtick)
	if len(lines) > 0 && strings.TrimSpace(lines[0]) == "" {
		lines = lines[1:]
	}
	// Strip trailing blank line (the newline right before closing backtick)
	if len(lines) > 0 && strings.TrimSpace(lines[len(lines)-1]) == "" {
		lines = lines[:len(lines)-1]
	}

	// Find minimum indentation across non-empty lines
	minIndent := math.MaxInt
	for _, line := range lines {
		if strings.TrimSpace(line) == "" {
			continue
		}
		indent := len(line) - len(strings.TrimLeft(line, " \t"))
		if indent < minIndent {
			minIndent = indent
		}
	}
	if minIndent == math.MaxInt {
		minIndent = 0
	}

	// Strip the common indent
	for i, line := range lines {
		if len(line) >= minIndent {
			lines[i] = line[minIndent:]
		}
	}

	return strings.Join(lines, "\n") + "\n"
}

// ValidateSpaces walks the tree and returns errors for any Space that
// contains non-whitespace content (which would indicate a parser bug).
// Thin shim over golang.WhitespaceValidationService — recipes call the
// service directly; tests use this name for source-level continuity.
func ValidateSpaces(root java.Tree) []string {
	return (&recipes.WhitespaceValidationService{}).Validate(root)
}

// JavaRecipeConfig holds config for a Java-delegated recipe test.
type JavaRecipeConfig struct {
	RecipeName string
	Options    map[string]any
}

// RecipeSpec configures a test run.
type RecipeSpec struct {
	CheckParsePrintIdempotence bool
	Recipe                     recipe.Recipe
	JavaRecipe                 *JavaRecipeConfig     // when set, delegates to Java RPC
	JavaRpcClient              *JavaRpcClient        // injected Java RPC client
	MarkerPrinter              printer.MarkerPrinter // printer for cross-cutting markers in recipe output; defaults to printer.DefaultMarkerPrinter
}

// NewRecipeSpec creates a new RecipeSpec with default settings.
func NewRecipeSpec() *RecipeSpec {
	return &RecipeSpec{
		CheckParsePrintIdempotence: true,
	}
}

// WithRecipe sets the recipe to apply during the test run.
func (spec *RecipeSpec) WithRecipe(r recipe.Recipe) *RecipeSpec {
	spec.Recipe = r
	return spec
}

// WithJavaRecipe configures the test to delegate to a Java-defined recipe via RPC.
// Requires a JavaRpcClient to be set on the spec.
func (spec *RecipeSpec) WithJavaRecipe(recipeName string, options map[string]any) *RecipeSpec {
	spec.JavaRecipe = &JavaRecipeConfig{
		RecipeName: recipeName,
		Options:    options,
	}
	return spec
}

// WithJavaRpcClient sets the Java RPC client for recipe delegation.
func (spec *RecipeSpec) WithJavaRpcClient(client *JavaRpcClient) *RecipeSpec {
	spec.JavaRpcClient = client
	return spec
}

// WithMarkerPrinter overrides the MarkerPrinter used when printing recipe output.
// Mirrors RewriteTest's spec.markerPrinter(...) on the Java side. When unset,
// RewriteRun uses printer.DefaultMarkerPrinter so SearchResult/Markup markers
// surface as /*~~>*/ comments in the expected "after" source — matching Java
// and TypeScript test conventions.
func (spec *RecipeSpec) WithMarkerPrinter(mp printer.MarkerPrinter) *RecipeSpec {
	spec.MarkerPrinter = mp
	return spec
}

// RewriteRun parses each source, checks parse-print idempotence, attaches
// any markers contributed by project wrappers (GoProject), and (if
// configured) applies a recipe and checks the result. Accepts both bare
// SourceSpec values and project wrappers like GoProject — they both
// implement Sources.
func (spec *RecipeSpec) RewriteRun(t *testing.T, sources ...Sources) {
	t.Helper()

	// Flatten any project wrappers into a flat list of SourceSpecs, with
	// project markers already attached.
	var flat []SourceSpec
	for _, s := range sources {
		flat = append(flat, s.Expand()...)
	}

	p := parser.NewGoParser()

	// Only treat sources as a multi-file package when there's an explicit
	// project context (a goMod sibling). Without it, bare Golang(...)
	// specs may share the default Path="test.go" without intending to be
	// siblings — preserve per-file parsing in that case.
	var parsedByIdx map[int]*golang.CompilationUnit
	if pi := buildProjectImporter(flat); pi != nil {
		p.Importer = pi
		parsedByIdx = parsePackageGroups(t, p, flat)
	}

	for i, src := range flat {
		if !strings.HasSuffix(src.Path, ".go") {
			// go.mod parses into a lossless LST, so recipes can run against
			// it. Other non-Go sources (e.g. go.sum) have no LST yet and
			// round-trip verbatim.
			if path.Base(src.Path) == "go.mod" {
				spec.rewriteGoMod(t, src)
			} else if src.After != nil && *src.After != src.Before {
				t.Errorf("non-Go source %q: harness cannot apply recipes to it yet", src.Path)
			}
			continue
		}

		var cu *golang.CompilationUnit
		if parsedByIdx != nil {
			cu = parsedByIdx[i]
		}
		if cu == nil {
			// No project context (or project parse missed this source) —
			// parse this file in isolation so two bare specs sharing a
			// default Path don't clobber each other.
			parsed, err := p.Parse(src.Path, src.Before)
			if err != nil {
				t.Fatalf("parse error: %v", err)
			}
			cu = parsed
		}

		// Attach any project markers contributed by GoProject(...) wrappers.
		for _, m := range src.Markers {
			cu.Markers = java.AddMarker(cu.Markers, m)
		}

		// Validate that no Space contains non-whitespace syntax
		if errs := ValidateSpaces(cu); len(errs) > 0 {
			for _, e := range errs {
				t.Errorf("space validation: %s", e)
			}
		}

		if src.AfterRecipe != nil {
			src.AfterRecipe(t, cu)
		}

		if spec.CheckParsePrintIdempotence {
			printed := printer.Print(cu)
			if printed != src.Before {
				t.Errorf("parse-print idempotence failed\n\nexpected:\n%s\n\nactual:\n%s\n\ndiff positions:", src.Before, printed)
				showDiff(t, src.Before, printed)
			}
		}

		// Apply recipe if configured
		if spec.Recipe != nil {
			result := runRecipe(spec.Recipe, cu)
			if result == nil {
				if src.After != nil {
					t.Error("recipe returned nil (deleted source file) but expected an after state")
				}
				continue
			}
			mp := spec.MarkerPrinter
			if mp == nil {
				mp = printer.DefaultMarkerPrinter
			}
			actual := printer.PrintWithMarkers(result, mp)
			if src.After != nil {
				if actual != *src.After {
					t.Errorf("recipe result mismatch\n\nexpected:\n%s\n\nactual:\n%s", *src.After, actual)
					showDiff(t, *src.After, actual)
				}
			} else {
				// No after state: expect no changes
				if actual != src.Before {
					t.Errorf("recipe made unexpected changes\n\nexpected (no change):\n%s\n\nactual:\n%s", src.Before, actual)
				}
			}
		} else if src.After != nil {
			t.Error("after state specified but no recipe configured")
		}
	}
}

// rewriteGoMod parses a go.mod source into its lossless LST, checks
// parse-print idempotence, and (if a recipe is configured) applies it and
// compares the printed result to the expected after-state. Mirrors the
// .go path in RewriteRun, but uses the go.mod-specific parser/printer since
// GoMod is a SourceFile that is not a *golang.CompilationUnit.
func (spec *RecipeSpec) rewriteGoMod(t *testing.T, src SourceSpec) {
	t.Helper()

	gm, err := parser.ParseGoModFile(src.Path, src.Before)
	if err != nil {
		t.Fatalf("go.mod parse error: %v", err)
	}

	// Attach any markers contributed by GoProject(...) wrappers (e.g. the
	// GoResolutionResult / GoProject markers) so recipes can read them.
	for _, m := range src.Markers {
		gm = gm.WithMarkers(java.AddMarker(gm.Markers, m))
	}

	if spec.CheckParsePrintIdempotence {
		if printed := printer.PrintGoMod(gm); printed != src.Before {
			t.Errorf("go.mod parse-print idempotence failed\n\nexpected:\n%s\n\nactual:\n%s\n\ndiff positions:", src.Before, printed)
			showDiff(t, src.Before, printed)
		}
	}

	if spec.Recipe == nil {
		if src.After != nil {
			t.Error("after state specified but no recipe configured")
		}
		return
	}

	result := runRecipe(spec.Recipe, gm)
	if result == nil {
		if src.After != nil {
			t.Error("recipe returned nil (deleted source file) but expected an after state")
		}
		return
	}
	resultGoMod, ok := result.(*golang.GoMod)
	if !ok {
		t.Fatalf("recipe returned %T, want *golang.GoMod", result)
	}

	mp := spec.MarkerPrinter
	if mp == nil {
		mp = printer.DefaultMarkerPrinter
	}
	actual := printer.PrintGoModWithMarkers(resultGoMod, mp)
	if src.After != nil {
		if actual != *src.After {
			t.Errorf("recipe result mismatch\n\nexpected:\n%s\n\nactual:\n%s", *src.After, actual)
			showDiff(t, *src.After, actual)
		}
	} else if actual != src.Before {
		t.Errorf("recipe made unexpected changes\n\nexpected (no change):\n%s\n\nactual:\n%s", src.Before, actual)
	}
}

// runRecipe applies a recipe (including composite recipes with sub-recipes) to a tree.
func runRecipe(r recipe.Recipe, t java.Tree) java.Tree {
	ctx := recipe.NewExecutionContext()

	// Apply this recipe's own editor, then drain any queued after-visits
	// (e.g. ImportService.AddImportVisitor inserted via DoAfterVisit).
	if editor := r.Editor(); editor != nil {
		result := editor.Visit(t, ctx)
		if result != nil {
			t = result
		}
		t = visitor.DrainAfterVisits(editor, t, ctx)
	}

	// Apply sub-recipes
	for _, sub := range r.RecipeList() {
		result := runRecipe(sub, t)
		if result != nil {
			t = result
		}
	}

	return t
}

func showDiff(t *testing.T, expected, actual string) {
	t.Helper()
	minLen := len(expected)
	if len(actual) < minLen {
		minLen = len(actual)
	}
	for i := 0; i < minLen; i++ {
		if expected[i] != actual[i] {
			start := i - 20
			if start < 0 {
				start = 0
			}
			end := i + 20
			if end > minLen {
				end = minLen
			}
			t.Errorf("first difference at byte %d:\n  expected: %q\n  actual:   %q",
				i, expected[start:end], actual[start:end])
			break
		}
	}
	if len(expected) != len(actual) {
		t.Errorf("length difference: expected %d, got %d", len(expected), len(actual))
	}
}
