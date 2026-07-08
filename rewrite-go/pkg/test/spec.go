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

type SourceSpec struct {
	Before      string
	After       *string // nil means no change expected (parse-print idempotence only)
	Path        string
	Markers     []java.Marker                                  // markers attached to the parsed source after parse
	AfterRecipe func(t *testing.T, cu *golang.CompilationUnit) // optional post-parse assertion callback (.go files only)
	generated   bool                                           // true for a file the recipe is expected to Generate (see Generated)
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

// Generated declares a source file the recipe under test is expected to
// create in its ScanningRecipe.Generate phase. It carries no "before"
// content — only the expected output keyed by its source path. RewriteRun
// matches each generated tree to a Generated spec by source path, so a
// scanning recipe that emits new files can be asserted alongside the files
// it edits. Fails the test if the recipe generates a file with no matching
// Generated spec, or a Generated spec matches nothing.
//
//	spec.RewriteRun(t,
//	    test.GoProject("foo",
//	        test.GoMod("module example.com/foo\ngo 1.22\n"),
//	        test.Golang("package main\nfunc main(){}\n"),
//	    ),
//	    test.Generated("tools.go", "package main\n"),
//	)
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

type JavaRecipeConfig struct {
	RecipeName string
	Options    map[string]any
}

type RecipeSpec struct {
	CheckParsePrintIdempotence bool
	Recipe                     recipe.Recipe
	JavaRecipe                 *JavaRecipeConfig     // when set, delegates to Java RPC
	JavaRpcClient              *JavaRpcClient        // injected Java RPC client
	MarkerPrinter              printer.MarkerPrinter // printer for cross-cutting markers in recipe output; defaults to printer.DefaultMarkerPrinter
}

func NewRecipeSpec() *RecipeSpec {
	return &RecipeSpec{
		CheckParsePrintIdempotence: true,
	}
}

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

// parsedSource pairs a SourceSpec with the tree the harness parsed for it.
// tree is nil for sources that have no LST yet (e.g. go.sum). result holds
// the tree after recipe application; it defaults to tree.
type parsedSource struct {
	spec   SourceSpec
	tree   java.Tree
	result java.Tree
}

// RewriteRun parses each source, checks parse-print idempotence, attaches
// any markers contributed by project wrappers (GoProject), and (if
// configured) applies a recipe and checks the result. Accepts both bare
// SourceSpec values and project wrappers like GoProject — they both
// implement Sources.
//
// Recipe application runs in two passes so cross-file ScanningRecipes work:
// every source is parsed first, then the recipe is applied over the whole
// set. A ScanningRecipe is driven through its real
// InitialValue -> Scanner -> Generate -> EditorWithData lifecycle (mirroring
// cmd/rpc/main.go and the JavaScript run scheduler); a plain recipe keeps the
// historical per-source Editor() application.
func (spec *RecipeSpec) RewriteRun(t *testing.T, sources ...Sources) {
	t.Helper()

	// Flatten any project wrappers into a flat list of SourceSpecs, with
	// project markers already attached. Generated(...) specs describe files
	// the recipe is expected to emit, not inputs — hold them aside.
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
		parsedByIdx = parsePackageGroups(t, p, flat)
	}

	// Pass 1: parse every source and run its per-source validations and
	// parse-print idempotence check. Recipe application is deferred to
	// pass 2 so a ScanningRecipe can observe every source before editing
	// any single file.
	parsed := make([]parsedSource, len(flat))
	for i, src := range flat {
		parsed[i].spec = src
		switch {
		case strings.HasSuffix(src.Path, ".go"):
			parsed[i].tree = spec.parseGoSource(t, p, parsedByIdx, i, src)
		case path.Base(src.Path) == "go.mod":
			// go.mod parses into a lossless LST, so recipes can run
			// against it.
			parsed[i].tree = spec.parseGoMod(t, src)
		default:
			// Other non-Go sources (e.g. go.sum) have no LST yet and
			// round-trip verbatim; tree stays nil.
		}
	}

	// Pass 2: apply the recipe over the parsed set.
	var generated []java.Tree
	if spec.Recipe != nil {
		for i := range parsed {
			parsed[i].result = parsed[i].tree
		}
		if recipeIsScanning(spec.Recipe) {
			generated = spec.runScanningLifecycle(parsed)
		} else {
			for i := range parsed {
				if parsed[i].tree != nil {
					parsed[i].result = runRecipe(spec.Recipe, parsed[i].tree)
				}
			}
		}
	}

	// Pass 3: compare each source (and any generated files) to expectations.
	for i := range parsed {
		spec.compareSource(t, parsed[i])
	}
	spec.compareGenerated(t, genSpecs, generated)
}

// parseGoSource parses one .go SourceSpec (reusing the shared package parse
// when a project context supplied one), attaches project markers, runs the
// whitespace/idempotence validations, and fires the AfterRecipe callback.
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
		if err != nil {
			t.Fatalf("parse error: %v", err)
		}
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

	return cu
}

// parseGoMod parses a go.mod source into its lossless LST, attaches project
// markers, and checks parse-print idempotence. GoMod is a SourceFile that is
// not a *golang.CompilationUnit, so it uses the go.mod-specific parser.
func (spec *RecipeSpec) parseGoMod(t *testing.T, src SourceSpec) *golang.GoMod {
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

	return gm
}

// compareSource prints a source's recipe result and asserts it against the
// spec's expected after-state (or, when none is given, that nothing changed).
func (spec *RecipeSpec) compareSource(t *testing.T, ps parsedSource) {
	t.Helper()
	src := ps.spec

	if ps.tree == nil {
		// Non-LST source (e.g. go.sum): round-trips verbatim; the harness
		// can't apply recipes to it yet.
		if src.After != nil && *src.After != src.Before {
			t.Errorf("non-Go source %q: harness cannot apply recipes to it yet", src.Path)
		}
		return
	}

	if spec.Recipe == nil {
		if src.After != nil {
			t.Error("after state specified but no recipe configured")
		}
		return
	}

	if ps.result == nil {
		if src.After != nil {
			t.Error("recipe returned nil (deleted source file) but expected an after state")
		}
		return
	}

	actual := printTree(ps.result, spec.markerPrinter())
	if src.After != nil {
		if actual != *src.After {
			t.Errorf("recipe result mismatch\n\nexpected:\n%s\n\nactual:\n%s", *src.After, actual)
			showDiff(t, *src.After, actual)
		}
		return
	}
	// No after state: expect no changes.
	if actual != src.Before {
		t.Errorf("recipe made unexpected changes\n\nexpected (no change):\n%s\n\nactual:\n%s", src.Before, actual)
	}
}

// compareGenerated matches each Generated(...) spec to a tree the recipe
// emitted (by source path) and asserts its printed form. It flags both a
// Generated spec that matched nothing and a generated tree with no spec, so
// generation stays fully covered rather than silently ignored.
func (spec *RecipeSpec) compareGenerated(t *testing.T, specs []SourceSpec, generated []java.Tree) {
	t.Helper()
	if len(specs) == 0 && len(generated) == 0 {
		return
	}

	mp := spec.markerPrinter()
	matched := make([]bool, len(generated))
	for _, gs := range specs {
		found := false
		for j, gt := range generated {
			if matched[j] || gt == nil || generatedSourcePath(gt) != gs.Path {
				continue
			}
			matched[j] = true
			found = true
			if actual := printTree(gt, mp); gs.After != nil && actual != *gs.After {
				t.Errorf("generated file %q mismatch\n\nexpected:\n%s\n\nactual:\n%s", gs.Path, *gs.After, actual)
				showDiff(t, *gs.After, actual)
			}
			break
		}
		if !found {
			t.Errorf("expected recipe to generate file %q, but it was not generated", gs.Path)
		}
	}
	for j, gt := range generated {
		if !matched[j] && gt != nil {
			t.Errorf("recipe generated an unexpected file %q; add a test.Generated(...) spec to assert it", generatedSourcePath(gt))
		}
	}
}

// markerPrinter returns the configured MarkerPrinter, defaulting to
// DefaultMarkerPrinter so SearchResult/Markup markers render as /*~~>*/.
func (spec *RecipeSpec) markerPrinter() printer.MarkerPrinter {
	if spec.MarkerPrinter != nil {
		return spec.MarkerPrinter
	}
	return printer.DefaultMarkerPrinter
}

// printTree renders a recipe result, picking the go.mod printer for a GoMod
// and the Go source printer for everything else.
func printTree(t java.Tree, mp printer.MarkerPrinter) string {
	if gm, ok := t.(*golang.GoMod); ok {
		return printer.PrintGoModWithMarkers(gm, mp)
	}
	return printer.PrintWithMarkers(t, mp)
}

func generatedSourcePath(t java.Tree) string {
	if sp, ok := t.(interface{ GetSourcePath() string }); ok {
		return sp.GetSourcePath()
	}
	return ""
}

// recipeIsScanning reports whether r, or any recipe in its RecipeList,
// implements ScanningRecipe.
func recipeIsScanning(r recipe.Recipe) bool {
	if _, ok := r.(recipe.ScanningRecipe); ok {
		return true
	}
	for _, sub := range r.RecipeList() {
		if recipeIsScanning(sub) {
			return true
		}
	}
	return false
}

// runScanningLifecycle drives spec.Recipe (and any nested recipes) through
// the real scan-then-edit lifecycle over the whole parsed source set,
// sharing one ExecutionContext so accumulators live across phases — exactly
// as the RPC server does. Existing sources are edited in place (results
// written back onto parsed[i].result); Generate outputs are returned so the
// caller can assert them against Generated(...) specs.
func (spec *RecipeSpec) runScanningLifecycle(parsed []parsedSource) []java.Tree {
	ctx := recipe.NewExecutionContext()

	working := make([]java.Tree, 0, len(parsed))
	idx := make([]int, 0, len(parsed))
	for i := range parsed {
		if parsed[i].tree != nil {
			working = append(working, parsed[i].tree)
			idx = append(idx, i)
		}
	}
	nExisting := len(working)

	var walk func(r recipe.Recipe)
	walk = func(r recipe.Recipe) {
		if sr, ok := r.(recipe.ScanningRecipe); ok {
			acc := sr.InitialValue(ctx)
			if scanner := sr.Scanner(acc); scanner != nil {
				for _, tr := range working {
					if tr != nil {
						scanner.Visit(tr, ctx)
					}
				}
			}
			for _, g := range sr.Generate(acc, ctx) {
				if g != nil {
					working = append(working, g)
				}
			}
			if editor := sr.EditorWithData(acc); editor != nil {
				applyEditorAcross(editor, working, ctx)
			}
		} else if editor := r.Editor(); editor != nil {
			applyEditorAcross(editor, working, ctx)
		}
		for _, sub := range r.RecipeList() {
			walk(sub)
		}
	}
	walk(spec.Recipe)

	for k, i := range idx {
		parsed[i].result = working[k]
	}
	return working[nExisting:]
}

// applyEditorAcross visits every tree with editor, draining after-visits per
// tree, and writes results back in place. Mirrors runRecipe's single-file
// editor handling: a nil visit result keeps the original tree.
func applyEditorAcross(editor recipe.TreeVisitor, trees []java.Tree, ctx *recipe.ExecutionContext) {
	for i, tr := range trees {
		if tr == nil {
			continue
		}
		result := editor.Visit(tr, ctx)
		if result != nil {
			tr = result
		}
		trees[i] = visitor.DrainAfterVisits(editor, tr, ctx)
	}
}

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
