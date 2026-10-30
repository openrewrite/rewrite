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
	"fmt"
	"math"
	"path"
	"reflect"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

type SourceSpec struct {
	Before      string
	After       *string // nil means no change expected (parse-print idempotence only)
	Path        string
	Markers     []java.Marker                                  // markers attached to the parsed source after parse
	AfterRecipe func(t *testing.T, cu *golang.CompilationUnit) // optional assertion callback on the recipe's result (.go files only)
	generated   bool                                           // true for a file the recipe is expected to Generate (see Generated)
	deleted     bool                                           // true for a file the recipe is expected to delete (see ExpectDeleted)
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

// WithAfterRecipe returns a copy of s with the after-recipe callback set.
// Once the source matches its expected after state, the callback receives
// the recipe's result, or the parsed source when no recipe is configured,
// so it can read s.Markers off the cu and assert on resolved types.
func (s SourceSpec) WithAfterRecipe(fn func(t *testing.T, cu *golang.CompilationUnit)) SourceSpec {
	s.AfterRecipe = fn
	return s
}

// ExpectDeleted returns a copy of s that expects the recipe to delete the
// source file.
func (s SourceSpec) ExpectDeleted() SourceSpec {
	s.After = nil
	s.deleted = true
	return s
}

// project wraps a set of sources and tags each with a GoProject marker on
// expansion. Mirrors Assertions.goProject(name, ...) on the Java side.
type project struct {
	name  string
	inner []Sources
}

func (p project) Expand() []SourceSpec {
	var out []SourceSpec
	for _, s := range p.inner {
		out = append(out, s.Expand()...)
	}
	mergeGoSumIntoGoMod(out)
	// GoProject rides on every source (like JavaProject on the Java side)
	// and carries the module path so recipes can classify imports without
	// reading the sibling go.mod. The full GoResolutionResult stays on the
	// go.mod/go.sum only, just as MavenResolutionResult lives on pom.xml.
	marker := golang.NewGoProject(p.name, projectModulePath(out))
	for i := range out {
		out[i].Markers = append(append([]java.Marker{}, out[i].Markers...), marker)
	}
	return out
}

// projectModulePath returns the module path from the project's go.mod
// GoResolutionResult marker, or "" if the project has no parseable go.mod.
func projectModulePath(specs []SourceSpec) string {
	for i := range specs {
		if specs[i].Path == "go.mod" {
			if found := FindGoResolutionResult(specs[i]); found != nil {
				return found.ModulePath
			}
		}
	}
	return ""
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

// GoModGraph decorates a GoMod SourceSpec with a resolved build list and
// package->module map, as if parse-time toolchain resolution (ResolveModuleGraph)
// had run. It lets recipes that consume ResolvedDependency.Deps / PackageModules
// be unit-tested without invoking the go toolchain (which the Go unit harness
// cannot run). No-op when the spec carries no GoResolutionResult marker.
//
// Example:
//
//	test.GoModGraph(
//	    test.GoMod("module example.com/foo\ngo 1.22\n"),
//	    []golang.GoResolvedDependency{{ModulePath: "example.com/foo", Main: true}},
//	    []golang.GoPackageModule{{ImportPath: "fmt", Standard: true}},
//	)
func GoModGraph(mod SourceSpec, resolved []golang.GoResolvedDependency, pkgs []golang.GoPackageModule) SourceSpec {
	markers := make([]java.Marker, len(mod.Markers))
	copy(markers, mod.Markers)
	for i, m := range markers {
		if mrr, ok := m.(golang.GoResolutionResult); ok {
			mrr.ResolvedDependencies = resolved
			mrr.PackageModules = pkgs
			markers[i] = mrr
			break
		}
	}
	mod.Markers = markers
	return mod
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
// specs sharing the same Path don't clobber each other in the result,
// along with the error of a package that did not parse.
func parsePackageGroups(p *parser.GoParser, flat []SourceSpec) (map[int]*golang.CompilationUnit, error) {
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
	var firstErr error
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
			if firstErr == nil {
				firstErr = fmt.Errorf("parse error in package %s: %w", dir, err)
			}
			continue
		}
		for i, cu := range cus {
			out[included[i].idx] = cu
		}
	}
	return out, firstErr
}

// buildProjectImporter scans a flattened source list for a go.mod with
// a GoResolutionResult marker. If found, registers every sibling .go
// file AND every go.mod-declared require with a ProjectImporter so:
//   - intra-project imports type-check against real sources;
//   - imports of declared third-party modules resolve to stub packages.
//
// SourceSpec paths may be absolute when tests model ParseProject's
// filesystem-discovery shape; ProjectImporter still needs module-relative
// paths so its import-path index matches the module path.
//
// Returns nil when there's no module context — the caller should fall
// back to importer.Default().
func buildProjectImporter(flat []SourceSpec) *parser.ProjectImporter {
	var mrr *golang.GoResolutionResult
	moduleRoot := ""
	for _, s := range flat {
		if found := FindGoResolutionResult(s); found != nil && found.ModulePath != "" {
			mrr = found
			if path.IsAbs(s.Path) {
				moduleRoot = path.Dir(path.Clean(s.Path))
			}
			break
		}
	}
	if mrr == nil {
		return nil
	}
	if moduleRoot == "" {
		moduleRoot = commonAbsoluteSourceRoot(flat)
	}
	pi := parser.NewProjectImporter(mrr.ModulePath, nil)
	for _, req := range mrr.Requires {
		pi.AddRequire(req.ModulePath)
		pi.AddModule(req.ModulePath, "", req.Version)
	}
	for _, s := range flat {
		if !strings.HasSuffix(s.Path, ".go") {
			continue
		}
		pi.AddSource(moduleRelativeSourcePath(s.Path, moduleRoot), s.Before)
	}
	return pi
}

func commonAbsoluteSourceRoot(flat []SourceSpec) string {
	root := ""
	for _, s := range flat {
		if !strings.HasSuffix(s.Path, ".go") || !path.IsAbs(s.Path) {
			continue
		}
		dir := path.Dir(path.Clean(s.Path))
		if root == "" {
			root = dir
		} else {
			root = commonPathPrefix(root, dir)
		}
	}
	return root
}

func commonPathPrefix(a, b string) string {
	if a == b {
		return a
	}
	aParts := strings.Split(strings.Trim(a, "/"), "/")
	bParts := strings.Split(strings.Trim(b, "/"), "/")
	n := 0
	for n < len(aParts) && n < len(bParts) && aParts[n] == bParts[n] {
		n++
	}
	if n == 0 {
		return "/"
	}
	return "/" + strings.Join(aParts[:n], "/")
}

func moduleRelativeSourcePath(sourcePath, moduleRoot string) string {
	if moduleRoot == "" {
		return sourcePath
	}
	cleaned := path.Clean(sourcePath)
	if !path.IsAbs(cleaned) {
		return sourcePath
	}
	root := path.Clean(moduleRoot)
	if strings.HasPrefix(cleaned, root+"/") {
		return strings.TrimPrefix(cleaned, root+"/")
	}
	return sourcePath
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

func Generated(sourcePath, after string) SourceSpec {
	a := TrimIndent(after)
	return SourceSpec{
		After:     &a,
		Path:      sourcePath,
		generated: true,
	}
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

type RecipeSpec struct {
	CheckParsePrintIdempotence bool
	// CheckReportingMarkersOnly asserts that a recipe adds only markers that
	// report, such as search results and markup: a marker type turning up where
	// neither the sources it was given nor a fresh parse of its output have one
	// fails the run.
	CheckReportingMarkersOnly bool
	// CheckUnchangedTreeIdentity asserts that a source whose printed output is
	// unchanged comes back as the same tree pointer. Turn it off for fixtures that
	// deliberately rebuild nodes into printed-identical replacements.
	CheckUnchangedTreeIdentity bool
	// Cycles is how many times the recipe runs, each run on the output of the one
	// before, by default one more than ExpectedCyclesThatMakeChanges and at least
	// two. The cycles after the expected ones, and a run on a fresh parse of the
	// output, fail the test if they change anything, so 1 turns these checks off.
	Cycles int
	// ExpectedCyclesThatMakeChanges is how many cycles the recipe needs to settle,
	// by default 1. A recipe that only reaches its result over several cycles
	// declares how many here, and the test then compares the output of the last
	// of them.
	ExpectedCyclesThatMakeChanges int
	Recipe                        recipe.Recipe
	MarkerPrinter                 printer.MarkerPrinter // printer for cross-cutting markers in recipe output; defaults to printer.DefaultMarkerPrinter
	// ExecutionContext is the context every cycle of the recipe shares, by
	// default a fresh one for each RewriteRun. Each RewriteRun installs a new
	// in-memory DataTableStore on it, holding the rows of the first cycle.
	ExecutionContext *recipe.ExecutionContext
	dataTables       []dataTableExpectation
}

func NewRecipeSpec() *RecipeSpec {
	return &RecipeSpec{
		CheckParsePrintIdempotence: true,
		CheckReportingMarkersOnly:  true,
		CheckUnchangedTreeIdentity: true,
	}
}

func (spec *RecipeSpec) WithRecipe(r recipe.Recipe) *RecipeSpec {
	spec.Recipe = r
	return spec
}

// WithExecutionContext sets the context the recipe runs with; see
// RecipeSpec.ExecutionContext.
func (spec *RecipeSpec) WithExecutionContext(ctx *recipe.ExecutionContext) *RecipeSpec {
	spec.ExecutionContext = ctx
	return spec
}

// WithCycles sets how many times the recipe runs; see RecipeSpec.Cycles.
func (spec *RecipeSpec) WithCycles(cycles int) *RecipeSpec {
	spec.Cycles = cycles
	return spec
}

// WithExpectedCyclesThatMakeChanges sets how many cycles the recipe needs to
// settle; see RecipeSpec.ExpectedCyclesThatMakeChanges.
func (spec *RecipeSpec) WithExpectedCyclesThatMakeChanges(cycles int) *RecipeSpec {
	spec.ExpectedCyclesThatMakeChanges = cycles
	return spec
}

func (spec *RecipeSpec) expectedCycles() int {
	if spec.ExpectedCyclesThatMakeChanges > 0 {
		return spec.ExpectedCyclesThatMakeChanges
	}
	return 1
}

func (spec *RecipeSpec) cycles() int {
	if spec.Cycles > 0 {
		return spec.Cycles
	}
	return spec.expectedCycles() + 1
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

type parsedSource struct {
	spec   SourceSpec
	tree   java.Tree
	result java.Tree
}

// recipeRun is what RewriteRun parsed and what the recipe made of it over its
// cycles.
type recipeRun struct {
	parsed                []parsedSource
	genSpecs              []SourceSpec
	generated             []java.Tree
	cyclesThatMadeChanges int
	dataTables            recipe.DataTableStore
	// freshParseContext holds the messages the ExecutionContext started with,
	// for the run on a fresh parse of the output.
	freshParseContext *recipe.ExecutionContext
	// unsettled is the first cycle after the expected ones that changed anything.
	unsettled *cycleChanges
}

type cycleChanges struct {
	cycle   int
	changes []change
}

// change is a source a run of the recipe changed; before is nil for a file it
// generated.
type change struct {
	before, after java.Tree
}

// RewriteRun parses each source, checks parse-print idempotence, attaches
// any markers contributed by project wrappers (GoProject), and (if
// configured) applies a recipe and checks the result, then hands each source's
// final tree to its AfterRecipe callback when every check passed. Accepts both bare
// SourceSpec values and project wrappers like GoProject — they both
// implement Sources.
func (spec *RecipeSpec) RewriteRun(t *testing.T, sources ...Sources) {
	t.Helper()
	run := spec.parseAndApply(t, sources)
	spec.verify(t, run)
	if !t.Failed() {
		spec.afterRecipe(t, run)
	}
}

func (spec *RecipeSpec) afterRecipe(t *testing.T, run *recipeRun) {
	t.Helper()
	for _, ps := range run.parsed {
		if cu, ok := ps.result.(*golang.CompilationUnit); ok && ps.spec.AfterRecipe != nil {
			ps.spec.AfterRecipe(t, cu)
		}
	}
	paired, _ := pairGenerated(run.genSpecs, run.generated)
	for k, gs := range run.genSpecs {
		if cu, ok := paired[k].(*golang.CompilationUnit); ok && gs.AfterRecipe != nil {
			gs.AfterRecipe(t, cu)
		}
	}
	spec.checkDataTables(t, run.dataTables)
}

// parseAndApply parses every source and applies the recipe over its cycles
// (Passes 1 and 2).
func (spec *RecipeSpec) parseAndApply(t *testing.T, sources []Sources) *recipeRun {
	t.Helper()

	// Flatten any project wrappers into a flat list of SourceSpecs, with
	// project markers already attached.
	var flat []SourceSpec
	var genSpecs []SourceSpec
	for _, s := range sources {
		for _, ss := range s.Expand() {
			if ss.generated {
				genSpecs = append(genSpecs, ss)
			} else {
				flat = append(flat, ss)
			}
		}
	}

	p := parser.NewGoParser()

	// Only treat sources as a multi-file package when there's an explicit
	// project context (a goMod sibling). Without it, bare Golang(...)
	// specs may share the default Path="test.go" without intending to be
	// siblings — preserve per-file parsing in that case.
	var parsedByIdx map[int]*golang.CompilationUnit
	if pi := buildProjectImporter(flat); pi != nil {
		p.Importer = pi
		var err error
		parsedByIdx, err = parsePackageGroups(p, flat)
		require.NoError(t, err)
	}

	// Pass 1: parse every source.
	parsed := make([]parsedSource, len(flat))
	for i, src := range flat {
		parsed[i].spec = src
		switch {
		case strings.HasSuffix(src.Path, ".go"):
			parsed[i].tree = spec.parseGoSource(t, p, parsedByIdx, i, src)
		case path.Base(src.Path) == "go.mod":
			parsed[i].tree = spec.parseGoMod(t, src)
		case path.Base(src.Path) == "go.sum":
			parsed[i].tree = spec.parseGoSum(t, src)
		}
	}

	// Pass 2: apply the recipe over the parsed set, cycle after cycle.
	run := &recipeRun{parsed: parsed, genSpecs: genSpecs}
	for i := range parsed {
		parsed[i].result = parsed[i].tree
	}
	if spec.Recipe != nil {
		spec.runCycles(run)
	}
	return run
}

// runCycles runs the recipe on the output of its previous run until it changes
// nothing or the cycles run out, keeping the output of the expected cycles and
// noting the first later cycle that changed anything.
func (spec *RecipeSpec) runCycles(run *recipeRun) {
	ctx := spec.ExecutionContext
	if ctx == nil {
		ctx = recipe.NewExecutionContext()
	}
	run.freshParseContext = recipe.NewExecutionContext()
	for _, k := range ctx.MessageKeys() {
		v, _ := ctx.GetMessage(k)
		run.freshParseContext.PutMessage(k, v)
	}
	run.freshParseContext.PutMessage(recipe.DataTableStoreKey, discardingStore{})
	run.dataTables = recipe.NewInMemoryDataTableStore()
	ctx.PutMessage(recipe.DataTableStoreKey, run.dataTables)
	defer ctx.PutMessage(recipe.DataTableStoreKey, run.dataTables)
	for cycle := 1; cycle <= spec.cycles(); cycle++ {
		if cycle == 2 {
			ctx.PutMessage(recipe.DataTableStoreKey, discardingStore{})
		}
		var live []java.Tree
		var slots []*java.Tree
		for i := range run.parsed {
			if run.parsed[i].result != nil {
				live = append(live, run.parsed[i].result)
				slots = append(slots, &run.parsed[i].result)
			}
		}
		for i := range run.generated {
			if run.generated[i] != nil {
				live = append(live, run.generated[i])
				slots = append(slots, &run.generated[i])
			}
		}
		results, generated := runCycle(spec.Recipe, live, ctx)
		changes := changesOf(live, results, generated)
		if len(changes) == 0 {
			return
		}
		if cycle > spec.expectedCycles() {
			run.unsettled = &cycleChanges{cycle: cycle, changes: changes}
			return
		}
		run.cyclesThatMadeChanges++
		for k, result := range results {
			*slots[k] = result
		}
		run.generated = append(run.generated, generated...)
	}
}

func changesOf(before, after, generated []java.Tree) []change {
	var changes []change
	for k := range before {
		if after[k] != before[k] {
			changes = append(changes, change{before: before[k], after: after[k]})
		}
	}
	for _, g := range generated {
		changes = append(changes, change{after: g})
	}
	return changes
}

// verify checks what the recipe produced. Markers come first, since a recipe that
// only added some returns a new tree printing the same as the old one, which Pass 3
// would otherwise put down to a needless rebuild.
func (spec *RecipeSpec) verify(t testing.TB, run *recipeRun) {
	t.Helper()
	if spec.Cycles > 0 && spec.Cycles < spec.expectedCycles() {
		t.Errorf("Cycles is %d, fewer than the %d ExpectedCyclesThatMakeChanges.", spec.Cycles, spec.expectedCycles())
		return
	}
	if expected := spec.expectedCycles(); expected > 1 && run.cyclesThatMadeChanges < expected {
		t.Errorf("Expected recipe to complete in %s, but it settled after %s. Lower ExpectedCyclesThatMakeChanges.",
			pluralCycles(expected), pluralCycles(run.cyclesThatMadeChanges))
		return
	}

	if spec.CheckReportingMarkersOnly {
		if failure := addedMarkersFailure(run.parsed, run.generated); failure != "" {
			t.Error(failure)
			return
		}
	}

	// Pass 3: compare each source (and any generated files) to expectations.
	for i := range run.parsed {
		spec.compareSource(t, run.parsed[i])
	}
	spec.compareGenerated(t, run.genSpecs, run.generated)
	if t.Failed() {
		return
	}

	// Pass 4: the recipe leaves its own output alone.
	if run.unsettled != nil && spec.expectNoFurtherChanges(t, run.unsettled.changes, fmt.Sprintf("Expected recipe to complete in %s, but cycle %d changed",
		pluralCycles(spec.expectedCycles()), run.unsettled.cycle), false) {
		return
	}
	if spec.cycles() > spec.expectedCycles() {
		spec.expectStableOnFreshParse(t, run)
	}
	if t.Failed() {
		return
	}

	spec.expectDataTablesWritten(t, run.dataTables)
}

// expectNoFurtherChanges reports whether it found changes to fail t with.
func (spec *RecipeSpec) expectNoFurtherChanges(t testing.TB, changes []change, headline string, sanitized bool) bool {
	t.Helper()
	mp := spec.markerPrinter()
	if sanitized {
		mp = printer.SanitizedMarkerPrinter
	}
	var found strings.Builder
	for _, c := range changes {
		if c.before == nil {
			fmt.Fprintf(&found, "  %s, which it generated\n", sourcePathOf(c.after))
			continue
		}
		if c.after == nil {
			fmt.Fprintf(&found, "  %s, which it deleted\n", sourcePathOf(c.before))
			continue
		}
		expected, actual := printTree(c.before, mp), printTree(c.after, mp)
		if expected != actual {
			fmt.Fprintf(&found, "  %s\nExpected:\n%s\n\nActual:\n%s\n", sourcePathOf(c.after), expected, actual)
		} else if !sanitized && spec.CheckUnchangedTreeIdentity {
			fmt.Fprintf(&found, "  %s, whose LST it changed without changing the printed source\n", sourcePathOf(c.after))
		}
	}
	if found.Len() == 0 {
		return false
	}
	t.Error(headline + " the sources again:\n" + found.String() +
		"A recipe should recognize code it has already changed and leave it alone, so that running it " +
		"again changes nothing. A recipe that is meant to need more cycles declares how many with " +
		"ExpectedCyclesThatMakeChanges.")
	return true
}

// expectStableOnFreshParse runs the recipe on a fresh parse of its output. The
// code a later run reads is parsed from the printed output, not the LST this run
// produced, and a recipe that recognizes its own output only by the shape of
// that LST changes it again.
func (spec *RecipeSpec) expectStableOnFreshParse(t testing.TB, run *recipeRun) {
	t.Helper()
	if run.cyclesThatMadeChanges == 0 {
		return
	}
	var finals []java.Tree
	var external [][]java.Marker
	for _, ps := range run.parsed {
		if ps.result != nil {
			finals = append(finals, ps.result)
			external = append(external, ps.spec.Markers)
		}
	}
	for _, g := range run.generated {
		if g != nil {
			finals = append(finals, g)
			external = append(external, nil)
		}
	}

	sources := freshParse(finals, external)
	if sources == nil {
		return
	}
	results, generated := runCycle(spec.Recipe, sources, run.freshParseContext)
	spec.expectNoFurtherChanges(t, changesOf(sources, results, generated),
		"Running the recipe on a fresh parse of its output changed", true)
}

// freshParse parses the printed source of each of finals as the harness parses
// its sources, keeping a final LST where its printed source does not parse back
// to the same text, or returns nil when none does. Of the markers on a final, it
// carries over to the fresh parse only those of a type given to the source from
// outside its text, the kind of each in external.
func freshParse(finals []java.Tree, external [][]java.Marker) []java.Tree {
	specs := make([]SourceSpec, len(finals))
	for k, f := range finals {
		specs[k] = SourceSpec{
			Before:  printTree(f, printer.SanitizedMarkerPrinter),
			Path:    sourcePathOf(f),
			Markers: carriedMarkers(markersOf(f), external[k]),
		}
	}
	p := parser.NewGoParser()
	var byIdx map[int]*golang.CompilationUnit
	if pi := buildProjectImporter(specs); pi != nil {
		p.Importer = pi
		byIdx, _ = parsePackageGroups(p, specs)
	}
	out := make([]java.Tree, len(finals))
	reparsed := false
	for k, s := range specs {
		out[k] = finals[k]
		var fresh java.Tree
		if cu := byIdx[k]; cu != nil {
			fresh = cu
		} else {
			fresh = reparse(p, s.Path, s.Before)
		}
		if fresh == nil || printTree(fresh, printer.SanitizedMarkerPrinter) != s.Before {
			continue
		}
		markers := markersOf(fresh)
		for _, m := range s.Markers {
			markers = java.AddMarker(markers, m)
		}
		out[k] = withMarkers(fresh, markers)
		reparsed = true
	}
	if !reparsed {
		return nil
	}
	return out
}

func carriedMarkers(markers java.Markers, external []java.Marker) []java.Marker {
	var carried []java.Marker
	for _, m := range markers.Entries() {
		for _, e := range external {
			if reflect.TypeOf(m) == reflect.TypeOf(e) {
				carried = append(carried, m)
				break
			}
		}
	}
	return carried
}

func markersOf(t java.Tree) java.Markers {
	switch sf := t.(type) {
	case *golang.CompilationUnit:
		return sf.Markers
	case *golang.GoMod:
		return sf.Markers
	case *golang.GoSum:
		return sf.Markers
	}
	return nil
}

func withMarkers(t java.Tree, markers java.Markers) java.Tree {
	switch sf := t.(type) {
	case *golang.CompilationUnit:
		return sf.WithMarkers(markers)
	case *golang.GoMod:
		return sf.WithMarkers(markers)
	case *golang.GoSum:
		return sf.WithMarkers(markers)
	}
	return t
}

func pluralCycles(count int) string {
	if count == 1 {
		return "1 cycle"
	}
	return fmt.Sprintf("%d cycles", count)
}

func (spec *RecipeSpec) parseGoSource(t *testing.T, p *parser.GoParser, parsedByIdx map[int]*golang.CompilationUnit, i int, src SourceSpec) *golang.CompilationUnit {
	t.Helper()

	var cu *golang.CompilationUnit
	if parsedByIdx != nil {
		cu = parsedByIdx[i]
	}
	if cu == nil {
		// No project context (or project parse missed this source) —
		// parse this file in isolation so two bare specs sharing a
		// default Path don't clobber each other.
		parsed, err := p.Parse(src.Path, src.Before)
		require.NoError(t, err, "parse error")
		cu = parsed
	}

	// Attach any project markers contributed by GoProject(...) wrappers.
	for _, m := range src.Markers {
		cu.Markers = java.AddMarker(cu.Markers, m)
	}

	// Validate that no Space contains non-whitespace syntax.
	if errs := ValidateSpaces(cu); len(errs) > 0 {
		for _, e := range errs {
			t.Errorf("space validation: %s", e)
		}
	}

	// Assert leading whitespace is attached to the outermost LST element
	// for every parsed source.
	if errs := WhitespaceAttachmentViolations(cu); len(errs) > 0 {
		for _, e := range errs {
			t.Errorf("whitespace attachment: %s", e)
		}
	}

	if spec.CheckParsePrintIdempotence {
		printed := printer.Print(cu)
		if printed != src.Before {
			t.Errorf("parse-print idempotence failed\n\nexpected:\n%s\n\nactual:\n%s\n\ndiff positions:", src.Before, printed)
			showDiff(t, src.Before, printed)
		}
	}

	return cu
}

func (spec *RecipeSpec) parseGoMod(t *testing.T, src SourceSpec) *golang.GoMod {
	t.Helper()

	gm, err := parser.ParseGoModFile(src.Path, src.Before)
	require.NoError(t, err, "go.mod parse error")

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

	return gm
}

func (spec *RecipeSpec) parseGoSum(t *testing.T, src SourceSpec) *golang.GoSum {
	t.Helper()

	gs, err := parser.ParseGoSumFile(src.Path, src.Before)
	require.NoError(t, err, "go.sum parse error")

	for _, m := range src.Markers {
		gs = gs.WithMarkers(java.AddMarker(gs.Markers, m))
	}

	if spec.CheckParsePrintIdempotence {
		if printed := printer.PrintGoSum(gs); printed != src.Before {
			t.Errorf("go.sum parse-print idempotence failed\n\nexpected:\n%s\n\nactual:\n%s\n\ndiff positions:", src.Before, printed)
			showDiff(t, src.Before, printed)
		}
	}

	return gs
}

func (spec *RecipeSpec) compareSource(t testing.TB, ps parsedSource) {
	t.Helper()
	src := ps.spec

	if ps.tree == nil {
		assert.Falsef(t, src.deleted || src.After != nil && *src.After != src.Before, "non-Go source %q: harness cannot apply recipes to it yet", src.Path)
		return
	}

	if spec.Recipe == nil {
		assert.Nil(t, src.After, "after state specified but no recipe configured")
		return
	}

	if ps.result == nil {
		switch {
		case src.deleted:
		case src.After == nil || *src.After == src.Before:
			t.Errorf("The recipe deleted a source file %q that was not expected to change", src.Path)
		default:
			t.Errorf("The recipe deleted a source file %q but should have changed it instead", src.Path)
		}
		return
	}
	if src.deleted {
		t.Errorf("Expected the recipe to delete %q", src.Path)
		return
	}

	actual := printTree(ps.result, spec.markerPrinter())
	if src.After != nil && *src.After != src.Before {
		if actual != *src.After {
			t.Errorf("recipe result mismatch\n\nexpected:\n%s\n\nactual:\n%s", *src.After, actual)
			showDiff(t, *src.After, actual)
		}
		return
	}
	// An after state restating the before means the same thing as omitting it.
	if !assert.Equal(t, src.Before, actual, "recipe made unexpected changes\n\nexpected (no change") {
		return
	}
	// The RPC layer derives its `modified` flag — and hence the SourcesFileResults
	// row credited to the recipe — from tree pointer identity, not printed text, so
	// an untouched source must come back as the very same pointer.
	if spec.CheckUnchangedTreeIdentity && ps.result != ps.tree {
		assert.Failf(t, "recipe returned a new tree without changing it",
			"recipe %q rebuilt %q; the visitor must return the original pointer when nothing changed",
			spec.Recipe.Name(), src.Path)
	}
}

func (spec *RecipeSpec) compareGenerated(t testing.TB, specs []SourceSpec, generated []java.Tree) {
	t.Helper()
	if len(specs) == 0 && len(generated) == 0 {
		return
	}

	mp := spec.markerPrinter()
	paired, matched := pairGenerated(specs, generated)
	for k, gs := range specs {
		gt := paired[k]
		if !assert.Truef(t, gt != nil, "expected recipe to generate file %q, but it was not generated", gs.Path) {
			continue
		}
		if actual := printTree(gt, mp); gs.After != nil && actual != *gs.After {
			t.Errorf("generated file %q mismatch\n\nexpected:\n%s\n\nactual:\n%s", gs.Path, *gs.After, actual)
			showDiff(t, *gs.After, actual)
		}
	}
	for j, gt := range generated {
		assert.Falsef(t, !matched[j] && gt != nil, "recipe generated an unexpected file %q; add a test.Generated(...) spec to assert it", sourcePathOf(gt))
	}
}

// pairGenerated pairs each spec with the first not yet paired generated file at
// its path, leaving nil for a spec the recipe generated nothing for.
func pairGenerated(specs []SourceSpec, generated []java.Tree) ([]java.Tree, []bool) {
	paired := make([]java.Tree, len(specs))
	matched := make([]bool, len(generated))
	for k, gs := range specs {
		for j, gt := range generated {
			if !matched[j] && gt != nil && sourcePathOf(gt) == gs.Path {
				matched[j] = true
				paired[k] = gt
				break
			}
		}
	}
	return paired, matched
}

func (spec *RecipeSpec) markerPrinter() printer.MarkerPrinter {
	if spec.MarkerPrinter != nil {
		return spec.MarkerPrinter
	}
	return printer.DefaultMarkerPrinter
}

func printTree(t java.Tree, mp printer.MarkerPrinter) string {
	if gm, ok := t.(*golang.GoMod); ok {
		return printer.PrintGoModWithMarkers(gm, mp)
	}
	if gs, ok := t.(*golang.GoSum); ok {
		return printer.PrintGoSumWithMarkers(gs, mp)
	}
	return printer.PrintWithMarkers(t, mp)
}

func sourcePathOf(t java.Tree) string {
	if sp, ok := t.(interface{ GetSourcePath() string }); ok {
		return sp.GetSourcePath()
	}
	return ""
}

func showDiff(t testing.TB, expected, actual string) {
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
