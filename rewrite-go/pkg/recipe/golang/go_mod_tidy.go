/*
 * Copyright 2026 the original author or authors.
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

package golang

import (
	"sort"
	"strconv"
	"strings"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang/internal"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoModTidy emulates `go mod tidy`'s effect on go.mod. It computes the exact
// require set the toolchain would write — adding missing requires, removing
// unused ones, classifying each as direct or `// indirect`, and including the
// go>=1.17 pruning-completeness roots — then rewrites the `require` blocks
// (sorted by module path/version, the toolchain's key).
//
// The require set is computed at recipe time from the resolved module graph,
// the imports scanned from the project's .go files, and a dependency ModSource
// (the local module cache plus a write-through GOPROXY reached over the CLI
// HttpSender). When no source is available or resolution cannot complete (e.g.
// offline), it degrades to an LST-only pass that re-marks and sorts the
// existing requires without dropping anything. go.sum is NOT recomputed.
//
// GoModTidy is a ScanningRecipe: the scan phase records, per module, the
// imports of every .go file it owns; the edit phase rewrites each go.mod
// against only its own module's files (so multi-module repositories tidy
// each go.mod independently).
type GoModTidy struct {
	recipe.ScanningBase
}

func (r *GoModTidy) Name() string        { return "org.openrewrite.golang.GoModTidy" }
func (r *GoModTidy) DisplayName() string { return "Tidy go.mod" }
func (r *GoModTidy) Description() string {
	return "Emulate `go mod tidy`'s effect on go.mod: add missing requires, remove unused ones, " +
		"classify each as direct or `// indirect` (including go>=1.17 pruning-completeness roots), " +
		"and sort the `require` blocks. Does not recompute go.sum."
}

// tidyAcc is the accumulator threaded across the scan phase. Data is kept
// PER-MODULE (keyed by the directory of each go.mod) so that a repository with
// several modules — e.g. a root module plus a nested `internal/tools` module —
// tidies each go.mod against only its own files. Without this, a nested
// module's imports (such as the classic `//go:build tools` tools.go) would leak
// into the root module's require set and get misclassified as direct.
type tidyAcc struct {
	// goModDirs is the set of directories that contain a go.mod (module roots).
	goModDirs map[string]bool
	// modulePathByDir maps a module's directory to its declared module path.
	modulePathByDir map[string]string
	// requireModsByDir maps a module's directory to its declared require set.
	requireModsByDir map[string]map[string]bool
	// fileImports maps each .go file's source path to the imports it declares.
	fileImports map[string][]string
}

func (r *GoModTidy) InitialValue(ctx *recipe.ExecutionContext) any {
	return &tidyAcc{
		goModDirs:        map[string]bool{},
		modulePathByDir:  map[string]string{},
		requireModsByDir: map[string]map[string]bool{},
		fileImports:      map[string][]string{},
	}
}

// sourceDir returns the directory portion of a repo-relative source path
// ("internal/tools/go.mod" -> "internal/tools"; "main.go" -> "").
func sourceDir(p string) string {
	if i := strings.LastIndex(p, "/"); i >= 0 {
		return p[:i]
	}
	return ""
}

// ownerDir returns the directory of the most-specific module that contains the
// file at fileDir — the longest go.mod directory that is fileDir or an ancestor
// of it. The root module (dir "") owns anything no nested module claims.
func ownerDir(fileDir string, dirs map[string]bool) string {
	best, bestLen := "", -1
	for d := range dirs {
		if d == "" || fileDir == d || strings.HasPrefix(fileDir, d+"/") {
			if len(d) > bestLen {
				best, bestLen = d, len(d)
			}
		}
	}
	return best
}

func (r *GoModTidy) Scanner(acc any) recipe.TreeVisitor {
	return visitor.Init(&goModTidyScanner{acc: acc.(*tidyAcc)})
}

func (r *GoModTidy) EditorWithData(acc any) recipe.TreeVisitor {
	return visitor.Init(&goModTidyEditor{acc: acc.(*tidyAcc)})
}

// --- scan phase ---
//
// The scanner records, per source file, the imports of each Go.CompilationUnit,
// plus each module's declared path and require set. Note that `go mod tidy`
// unions imports across ALL build configurations (every GOOS/GOARCH and tag),
// whereas the parser type-checks under one host build context — so files the
// host context excludes (e.g. a `//go:build windows` file on Linux) are not
// parsed into a CompilationUnit and their imports are not scanned here. That is
// an accepted limitation: it only matters for a module imported SOLELY by a
// platform-gated file, which is rare. (Recovering those would mean parsing the
// excluded files imports-only — see ParsePackage, which currently omits them.)

type goModTidyScanner struct {
	visitor.GoVisitor
	acc *tidyAcc
}

func (v *goModTidyScanner) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	if cu.Imports != nil {
		imps := make([]string, 0, len(cu.Imports.Elements))
		for _, rp := range cu.Imports.Elements {
			if path := internal.ImportPath(rp.Element); path != "" {
				imps = append(imps, path)
			}
		}
		v.acc.fileImports[cu.SourcePath] = imps
	}
	return v.GoVisitor.VisitCompilationUnit(cu, p)
}

// VisitGoMod records each module's declared path and require set, keyed by the
// go.mod's directory, so the editor can tidy each module against only its own
// files.
func (v *goModTidyScanner) VisitGoMod(gm *golang.GoMod, p any) java.Tree {
	dir := sourceDir(gm.SourcePath)
	v.acc.goModDirs[dir] = true
	modulePath, requires := parseGoModDeclared(gm)
	if modulePath != "" {
		v.acc.modulePathByDir[dir] = modulePath
	}
	v.acc.requireModsByDir[dir] = requires
	return v.GoVisitor.VisitGoMod(gm, p)
}

// parseGoModDeclared extracts a go.mod's module path and the set of module paths
// it declares in `require` (both single-line directives and block entries).
func parseGoModDeclared(gm *golang.GoMod) (modulePath string, requires map[string]bool) {
	requires = map[string]bool{}
	for _, st := range gm.Statements {
		switch s := st.Element.(type) {
		case *golang.GoModDirective:
			switch s.Keyword {
			case "module":
				if len(s.Values) > 0 {
					modulePath = s.Values[0].Text
				}
			case "require":
				if len(s.Values) > 0 {
					requires[s.Values[0].Text] = true
				}
			}
		case *golang.GoModBlock:
			if s.Keyword == "require" {
				for _, e := range s.Entries {
					if path, _ := moduleOf(e.Element); path != "" {
						requires[path] = true
					}
				}
			}
		}
	}
	return modulePath, requires
}

// --- edit phase ---

type goModTidyEditor struct {
	visitor.GoVisitor
	acc *tidyAcc
	// Per-module scope for the go.mod currently being edited, set at the top of
	// VisitGoMod so this module is tidied against only its own files.
	curModulePath  string
	curRequireMods map[string]bool
	curImports     []string
}

// reqEntry is a single collected require entry during the rebuild.
type reqEntry struct {
	path     string
	version  string
	indirect bool
}

// ownedImports returns the deduped imports of every scanned .go file whose
// nearest-ancestor go.mod is the module at dir — i.e. the files that belong to
// this module and not to a nested one.
func (v *goModTidyEditor) ownedImports(dir string) []string {
	seen := map[string]bool{}
	var out []string
	for path, imps := range v.acc.fileImports {
		if ownerDir(sourceDir(path), v.acc.goModDirs) != dir {
			continue
		}
		for _, imp := range imps {
			if !seen[imp] {
				seen[imp] = true
				out = append(out, imp)
			}
		}
	}
	return out
}

func (v *goModTidyEditor) VisitGoMod(gm *golang.GoMod, p any) java.Tree {
	gm = v.GoVisitor.VisitGoMod(gm, p).(*golang.GoMod)

	// Scope all import/require data to THIS module's directory.
	dir := sourceDir(gm.SourcePath)
	v.curModulePath = v.acc.modulePathByDir[dir]
	v.curRequireMods = v.acc.requireModsByDir[dir]
	if v.curRequireMods == nil {
		v.curRequireMods = map[string]bool{}
	}
	v.curImports = v.ownedImports(dir)

	separateIndirect := goVersionAtLeast(gm, 1, 17)

	// Strip the existing require statements, remembering where the first one
	// was and its leading whitespace.
	firstIdx := -1
	var firstPrefix java.Space
	var collected []reqEntry
	kept := make([]java.RightPadded[golang.GoModStatement], 0, len(gm.Statements))
	for _, st := range gm.Statements {
		switch s := st.Element.(type) {
		case *golang.GoModBlock:
			if s.Keyword == "require" {
				if firstIdx == -1 {
					firstIdx = len(kept)
					firstPrefix = s.Prefix
				}
				for _, e := range s.Entries {
					if path, version := moduleOf(e.Element); path != "" {
						collected = append(collected, reqEntry{path, version, false})
					}
				}
				continue
			}
		case *golang.GoModDirective:
			if s.Keyword == "require" && len(s.Values) > 0 {
				if firstIdx == -1 {
					firstIdx = len(kept)
					firstPrefix = s.Prefix
				}
				version := ""
				if len(s.Values) > 1 {
					version = s.Values[1].Text
				}
				collected = append(collected, reqEntry{s.Values[0].Text, version, false})
				continue
			}
		}
		kept = append(kept, st)
	}

	// Determine the entries to emit, in priority order:
	//  1. Compute the exact tidy require set NOW via the Java resolver (the
	//     scanned imports + the current go.mod, with dependency metadata fetched
	//     on the host through the CLI HttpSender). This is the production path.
	//  2. LST-only: re-mark the declared requires by direct-import and sort them,
	//     preserving the existing set (the offline / no-resolver degradation).
	var entries []reqEntry
	if computed, ok := v.computeTidySet(gm, separateIndirect, p); ok {
		entries = computed
	} else {
		direct := v.directModules()
		for _, e := range collected {
			entries = append(entries, reqEntry{e.path, e.version, !direct[e.path]})
		}
	}

	if len(entries) == 0 {
		if firstIdx == -1 {
			return gm // nothing to do
		}
		return gm.WithStatements(kept) // requires all pruned away
	}
	if firstIdx == -1 {
		// No existing require statement to anchor on (e.g. all were missing
		// and the graph added them); insert after the header directives.
		firstIdx = headerInsertIndex(kept)
		firstPrefix = java.Space{Whitespace: "\n"}
	}

	// Partition into the require statements the toolchain would emit: for
	// go >= 1.17, a direct-only statement followed by an indirect-only one;
	// otherwise a single mixed statement. Each group is sorted by path.
	var groups [][]reqEntry
	if separateIndirect {
		var directGroup, indirectGroup []reqEntry
		for _, e := range entries {
			if e.indirect {
				indirectGroup = append(indirectGroup, e)
			} else {
				directGroup = append(directGroup, e)
			}
		}
		if len(directGroup) > 0 {
			groups = append(groups, directGroup)
		}
		if len(indirectGroup) > 0 {
			groups = append(groups, indirectGroup)
		}
	} else {
		groups = append(groups, entries)
	}

	reqStmts := make([]java.RightPadded[golang.GoModStatement], 0, len(groups))
	for i, g := range groups {
		sortEntries(g)
		prefix := java.Space{Whitespace: "\n"}
		if i == 0 {
			prefix = firstPrefix
		}
		reqStmts = append(reqStmts, buildRequireStatement(g, prefix))
	}

	final := make([]java.RightPadded[golang.GoModStatement], 0, len(kept)+len(reqStmts))
	final = append(final, kept[:firstIdx]...)
	final = append(final, reqStmts...)
	final = append(final, kept[firstIdx:]...)
	return gm.WithStatements(final)
}

// sortEntries orders require entries by module path, then version.
func sortEntries(es []reqEntry) {
	sort.SliceStable(es, func(i, j int) bool {
		if es[i].path != es[j].path {
			return es[i].path < es[j].path
		}
		return es[i].version < es[j].version
	})
}

// buildRequireStatement renders a sorted group of entries as a single
// require statement: a single-line directive when the group has one entry,
// or a factored `require ( … )` block otherwise.
func buildRequireStatement(group []reqEntry, prefix java.Space) java.RightPadded[golang.GoModStatement] {
	if len(group) == 1 {
		e := group[0]
		d := &golang.GoModDirective{
			Ident:   uuid.New(),
			Prefix:  prefix,
			Keyword: "require",
			Values: []*golang.GoModValue{
				{Ident: uuid.New(), Prefix: java.SingleSpace, Text: e.path},
				{Ident: uuid.New(), Prefix: java.SingleSpace, Text: e.version},
			},
		}
		return java.RightPadded[golang.GoModStatement]{
			Element: d,
			After:   setIndirectComment(java.Space{}, e.indirect),
		}
	}

	blockEntries := make([]java.RightPadded[golang.GoModStatement], len(group))
	for i, e := range group {
		entryPrefix := java.Space{Whitespace: "\t"}
		if i == 0 {
			entryPrefix = java.Space{Whitespace: "\n\t"}
		}
		d := &golang.GoModDirective{
			Ident:  uuid.New(),
			Prefix: entryPrefix,
			Values: []*golang.GoModValue{
				{Ident: uuid.New(), Text: e.path},
				{Ident: uuid.New(), Prefix: java.SingleSpace, Text: e.version},
			},
		}
		blockEntries[i] = java.RightPadded[golang.GoModStatement]{
			Element: d,
			After:   setIndirectComment(java.Space{}, e.indirect),
		}
	}
	blk := &golang.GoModBlock{
		Ident:        uuid.New(),
		Prefix:       prefix,
		Keyword:      "require",
		BeforeLParen: java.SingleSpace,
		Entries:      blockEntries,
		BeforeRParen: java.Space{},
	}
	return java.RightPadded[golang.GoModStatement]{
		Element: blk,
		After:   java.Space{Whitespace: "\n"},
	}
}

// goVersionAtLeast reports whether the go.mod's `go` directive is >= the
// given major.minor. Absent or unparseable versions are treated as recent
// (the toolchain default), so the separate-indirect-block form is used.
func goVersionAtLeast(gm *golang.GoMod, major, minor int) bool {
	for _, st := range gm.Statements {
		d, ok := st.Element.(*golang.GoModDirective)
		if !ok || d.Keyword != "go" || len(d.Values) == 0 {
			continue
		}
		parts := strings.SplitN(d.Values[0].Text, ".", 3)
		if len(parts) < 2 {
			return true
		}
		gMaj, err1 := strconv.Atoi(parts[0])
		gMin, err2 := strconv.Atoi(parts[1])
		if err1 != nil || err2 != nil {
			return true
		}
		return gMaj > major || (gMaj == major && gMin >= minor)
	}
	return true
}

// headerInsertIndex returns the index just after the last leading header
// directive (module / go / toolchain) in stmts, the natural spot to insert a
// require block when none exists yet.
func headerInsertIndex(stmts []java.RightPadded[golang.GoModStatement]) int {
	idx := 0
	for i, st := range stmts {
		if d, ok := st.Element.(*golang.GoModDirective); ok {
			switch d.Keyword {
			case "module", "go", "toolchain":
				idx = i + 1
			}
		}
	}
	return idx
}

// computeTidySet computes the exact go.mod require set at recipe time by
// delegating to the pure-Java module resolver over RPC: it sends the current
// go.mod text, the imports scanned from the project's .go files, and the module
// path, and receives back the direct/indirect require set. All dependency
// metadata (go.mod/zip) is fetched on the host through the CLI HttpSender, so no
// network egress originates from this peer.
//
// Returns ok=false when no resolver is installed (running outside the CLI) or
// resolution could not complete (truly offline + cold cache, or a private
// module), so the caller falls back to the LST-only set, which PRESERVES the
// existing require/// indirect block rather than dropping unconfirmed deps.
func (v *goModTidyEditor) computeTidySet(gm *golang.GoMod, separateIndirect bool, p any) ([]reqEntry, bool) {
	ctx, ok := p.(*recipe.ExecutionContext)
	if !ok {
		return nil, false
	}
	resolve := TidyResolverFrom(ctx)
	if resolve == nil {
		return nil, false
	}

	content := printer.PrintGoMod(gm)
	rs, ok := resolve(content, v.curImports, v.curModulePath, separateIndirect)
	if !ok || !rs.Complete {
		return nil, false
	}

	entries := make([]reqEntry, 0, len(rs.Direct)+len(rs.Indirect))
	for mod, ver := range rs.Direct {
		entries = append(entries, reqEntry{mod, ver, false})
	}
	for mod, ver := range rs.Indirect {
		entries = append(entries, reqEntry{mod, ver, true})
	}
	return entries, true
}

// directModules returns the set of required module paths that are directly
// imported by some .go file belonging to the module currently being edited.
func (v *goModTidyEditor) directModules() map[string]bool {
	direct := map[string]bool{}
	for _, imp := range v.curImports {
		if internal.IsStdlib(imp) || internal.IsLocal(imp, v.curModulePath) {
			continue
		}
		if m := providingModule(imp, v.curRequireMods); m != "" {
			direct[m] = true
		}
	}
	return direct
}

// moduleOf returns the (path, version) of a require entry directive.
func moduleOf(st golang.GoModStatement) (string, string) {
	d, ok := st.(*golang.GoModDirective)
	if !ok || len(d.Values) == 0 {
		return "", ""
	}
	path := d.Values[0].Text
	version := ""
	if len(d.Values) > 1 {
		version = d.Values[1].Text
	}
	return path, version
}

// setIndirectComment reconstructs an entry's trailing After so that it
// carries (or omits) a `// indirect` line comment, preserving any other
// trailing comments. The line always ends in a newline.
func setIndirectComment(after java.Space, indirect bool) java.Space {
	var others []java.Comment
	for _, c := range after.Comments {
		if strings.TrimSpace(c.Text) == "// indirect" {
			continue
		}
		others = append(others, c)
	}
	if indirect {
		others = append(others, java.Comment{Kind: java.LineComment, Text: "// indirect", Suffix: "\n"})
		return java.Space{Whitespace: " ", Comments: others}
	}
	if len(others) > 0 {
		return java.Space{Whitespace: " ", Comments: others}
	}
	return java.Space{Whitespace: "\n"}
}

// providingModule returns the longest declared module path that is a prefix
// of importPath (the module that provides the imported package), or "".
func providingModule(importPath string, requireMods map[string]bool) string {
	best := ""
	for m := range requireMods {
		if importPath == m || strings.HasPrefix(importPath, m+"/") {
			if len(m) > len(best) {
				best = m
			}
		}
	}
	return best
}
