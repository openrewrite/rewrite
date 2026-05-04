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

// Package internal contains primitives shared across the import-management
// recipes (AddImport, RemoveImport, RemoveUnusedImports, OrderImports). It
// is not part of the public API — callers depend on the recipe types
// themselves, not on these helpers.
package internal

import (
	"sort"
	"strings"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// ImportGroup identifies which gofmt-style group an import belongs to.
type ImportGroup int

const (
	// Stdlib imports — paths with no `.` in their first segment
	// (e.g. "fmt", "net/http").
	Stdlib ImportGroup = iota
	// ThirdParty imports — paths with a `.` in their first segment
	// (e.g. "github.com/x/y") that are not local to the current module.
	ThirdParty
	// Local imports — paths under the current module's path.
	Local
)

// ImportPath returns the unquoted import path of an Import (e.g. "fmt").
// Returns "" when Qualid isn't a Literal (defensive — shouldn't happen
// for well-formed Go source).
//
// The Go parser stores the raw quoted source in Literal.Value (and
// .Source) — `"fmt"` not `fmt` — so this helper always strips the
// surrounding quote pair before returning.
func ImportPath(imp *tree.Import) string {
	if imp == nil {
		return ""
	}
	lit, ok := imp.Qualid.(*tree.Literal)
	if !ok || lit == nil {
		return ""
	}
	raw := ""
	if s, ok := lit.Value.(string); ok {
		raw = s
	} else {
		raw = lit.Source
	}
	return strings.Trim(raw, `"`+"`")
}

// AliasName returns the alias used by an Import: a custom identifier for
// `import alias "path"`, "_" for blank imports, "." for dot imports, or
// "" when the import uses the default (last segment of the path).
func AliasName(imp *tree.Import) string {
	if imp == nil || imp.Alias == nil {
		return ""
	}
	if imp.Alias.Element == nil {
		return ""
	}
	return imp.Alias.Element.Name
}

// FindImport returns the first import in cu whose path equals importPath.
// Returns nil if no match.
func FindImport(cu *tree.CompilationUnit, importPath string) *tree.Import {
	if cu == nil || cu.Imports == nil {
		return nil
	}
	for _, rp := range cu.Imports.Elements {
		if ImportPath(rp.Element) == importPath {
			return rp.Element
		}
	}
	return nil
}

// HasImport returns true when an import with the given path (and matching
// alias semantics) is already present.
//   - alias == nil: any non-blank, non-dot form counts as a hit
//   - alias != nil: requires an exact alias match (use "_" for blank,
//     "." for dot)
func HasImport(cu *tree.CompilationUnit, importPath string, alias *string) bool {
	if cu == nil || cu.Imports == nil {
		return false
	}
	for _, rp := range cu.Imports.Elements {
		if ImportPath(rp.Element) != importPath {
			continue
		}
		existingAlias := AliasName(rp.Element)
		if alias == nil {
			if existingAlias == "_" || existingAlias == "." {
				continue
			}
			return true
		}
		if *alias == existingAlias {
			return true
		}
	}
	return false
}

// GroupOf returns the ImportGroup an import path belongs to relative to
// the current module's path. modulePath may be empty when the file is
// outside any known module — local detection then falls back to "any
// non-stdlib path with a `.`" → ThirdParty.
func GroupOf(importPath, modulePath string) ImportGroup {
	if IsLocal(importPath, modulePath) {
		return Local
	}
	if IsThirdParty(importPath) {
		return ThirdParty
	}
	return Stdlib
}

// IsStdlib reports whether importPath looks like a stdlib package (no `.`
// in the first path segment). Mirrors goimports/gofmt's heuristic.
func IsStdlib(importPath string) bool {
	first := importPath
	if i := strings.Index(importPath, "/"); i >= 0 {
		first = importPath[:i]
	}
	return !strings.Contains(first, ".")
}

// IsThirdParty reports whether importPath is a non-stdlib, non-local
// dependency. Equivalent to `!IsStdlib && !IsLocal`.
func IsThirdParty(importPath string) bool {
	return !IsStdlib(importPath)
}

// IsLocal reports whether importPath belongs to the current module
// (modulePath itself or any sub-package under it).
func IsLocal(importPath, modulePath string) bool {
	if modulePath == "" {
		return false
	}
	return importPath == modulePath || strings.HasPrefix(importPath, modulePath+"/")
}

// ReferencedPackages walks cu and returns the set of import paths that
// are referenced by some identifier in the file body. Used by
// RemoveUnusedImports to drop imports whose alias is never read.
//
// Detection is driven by the Type attribution that the parser threads
// onto each Identifier:
//   - For an `Identifier` whose `Type` is a `JavaTypeClass` and whose
//     `FullyQualifiedName` carries an import path (path-shaped FQN),
//     that path is added to the set.
//   - For a `MethodInvocation`, the `MethodType.DeclaringType.FullyQualifiedName`
//     is used.
//
// Aliases and dot imports are handled uniformly — the package's import
// path is what we track, regardless of how the user named it.
func ReferencedPackages(cu *tree.CompilationUnit) map[string]bool {
	refs := map[string]bool{}
	if cu == nil {
		return refs
	}
	v := visitor.Init(&referencedPackagesVisitor{refs: refs})
	v.Visit(cu, nil)
	return refs
}

type referencedPackagesVisitor struct {
	visitor.GoVisitor
	refs map[string]bool
}

func (v *referencedPackagesVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	if ident.Type != nil {
		if fq, ok := ident.Type.(tree.FullyQualified); ok {
			if path := pkgPathOf(fq.GetFullyQualifiedName()); path != "" {
				v.refs[path] = true
			}
		}
	}
	return ident
}

func (v *referencedPackagesVisitor) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	if mi.MethodType != nil && mi.MethodType.DeclaringType != nil {
		if path := pkgPathOf(mi.MethodType.DeclaringType.FullyQualifiedName); path != "" {
			v.refs[path] = true
		}
	}
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

// pkgPathOf returns the package import path implied by an FQN. The
// type-mapper produces FQNs in two shapes:
//   - "<importPath>"               — for package aliases (the `y` in
//     `y.Hello()` after `import "github.com/x/y"`).
//   - "<importPath>.<TypeName>"    — for named types in that package.
//
// Both shapes share an import-path prefix (the leading segment up to the
// last `.`); we return that prefix so RemoveUnusedImports can match
// against the literal import path.
func pkgPathOf(fqn string) string {
	if fqn == "" {
		return ""
	}
	// FQNs that are already an import path (no trailing `.TypeName`)
	// contain a `/` and no `.` after the last `/`. Detect that shape and
	// return the FQN as-is.
	if strings.Contains(fqn, "/") {
		lastSlash := strings.LastIndex(fqn, "/")
		tail := fqn[lastSlash+1:]
		if !strings.Contains(tail, ".") {
			return fqn
		}
		// `.../pkg.TypeName` shape — strip the trailing `.TypeName`.
		return fqn[:lastSlash+1+strings.Index(tail, ".")]
	}
	// Stdlib paths (e.g. "fmt") and "fmt.Println"-style FQNs.
	if dot := strings.Index(fqn, "."); dot >= 0 {
		return fqn[:dot]
	}
	return fqn
}

// AddToBlock returns a copy of cu with imp inserted into the existing
// import container. If cu has no imports container yet, one is created
// with default formatting (single ungrouped `import "path"` line).
//
// The insertion preserves group ordering: imp is placed at the end of
// its own group block (stdlib / third-party / local) so OrderImports'
// invariant holds even when AddImport is invoked alone.
//
// Whitespace for the empty-container case: the printer concatenates
// `<Container.Before>import<element traversal>`. To produce
// `\n\nimport "fmt"` between `package main` and the first statement,
// the new Container's Before is `"\n\n"`, the Import's Prefix is empty,
// and the Qualid Literal carries a leading space (printed as the space
// between `import` and the path string). The first Statement's existing
// Prefix supplies the trailing blank line before `func`.
func AddToBlock(cu *tree.CompilationUnit, imp *tree.Import, modulePath string) *tree.CompilationUnit {
	if cu == nil {
		return cu
	}
	c := *cu
	if c.Imports == nil {
		c.Imports = &tree.Container[*tree.Import]{
			Before: tree.Space{Whitespace: "\n\n"},
		}
		// Wire the leading-space convention onto the new import: the
		// space between `import` and the path lives on the Qualid
		// literal's Prefix.
		if lit, ok := imp.Qualid.(*tree.Literal); ok {
			cloned := *lit
			cloned.Prefix = tree.Space{Whitespace: " "}
			imp.Qualid = &cloned
		}
	}
	imps := *c.Imports

	// Adding a second import to an ungrouped single-import file
	// promotes it to the grouped `import (...)` form — that's the
	// only legal Go syntax for multiple imports in one block.
	if len(imps.Elements) == 1 && tree.FindMarker[tree.GroupedImport](imps.Markers) == nil {
		promoteToGrouped(&imps)
	}

	imps.Elements = insertGrouped(imps.Elements, imp, modulePath)
	c.Imports = &imps
	return &c
}

// promoteToGrouped converts an ungrouped single-import block to the
// grouped `import (...)` form. Adds the GroupedImport marker and
// rewrites the existing element's Prefix / After so the printer emits
// it indented inside parens.
func promoteToGrouped(imps *tree.Container[*tree.Import]) {
	imps.Markers = tree.AddMarker(imps.Markers, tree.GroupedImport{
		Ident:  uuid.New(),
		Before: tree.Space{Whitespace: " "}, // space between `import` and `(`
	})
	if len(imps.Elements) == 0 {
		return
	}
	// The previously-ungrouped element had Qualid.Prefix=" " (space
	// between `import` and the path). Inside parens we want the import
	// indented onto its own line: imp.Prefix="\n\t", Qualid.Prefix="".
	rp := &imps.Elements[0]
	if rp.Element != nil {
		imp := *rp.Element
		imp.Prefix = tree.Space{Whitespace: "\n\t"}
		if lit, ok := imp.Qualid.(*tree.Literal); ok {
			cloned := *lit
			cloned.Prefix = tree.EmptySpace
			imp.Qualid = &cloned
		}
		rp.Element = &imp
	}
	rp.After = tree.Space{Whitespace: "\n"} // newline before `)`
}

// RemoveFromBlock returns a copy of cu with imp deleted from the imports
// container. If the container becomes empty as a result, it's nil-ed out
// so the printer doesn't emit an empty `import ()` block.
//
// Whitespace handling: the removed entry's trailing space (the
// `RightPadded.After` field, which contains the newline before the next
// element or the closing `)`) is donated to the new last element so the
// block keeps its closing-paren-on-its-own-line shape.
func RemoveFromBlock(cu *tree.CompilationUnit, imp *tree.Import) *tree.CompilationUnit {
	if cu == nil || cu.Imports == nil || imp == nil {
		return cu
	}
	c := *cu
	imps := *c.Imports
	removedLastAfter := tree.Space{}
	removedWasLast := false
	out := make([]tree.RightPadded[*tree.Import], 0, len(imps.Elements))
	for i, rp := range imps.Elements {
		if rp.Element != nil && rp.Element.ID == imp.ID {
			if i == len(imps.Elements)-1 {
				removedLastAfter = rp.After
				removedWasLast = true
			}
			continue
		}
		out = append(out, rp)
	}
	if removedWasLast && len(out) > 0 {
		// Donate the closing-paren-on-own-line whitespace to the new
		// last element so the block keeps its tidy shape.
		out[len(out)-1].After = removedLastAfter
	}
	imps.Elements = out
	if len(out) == 0 {
		c.Imports = nil
	} else {
		c.Imports = &imps
	}
	return &c
}

// insertGrouped places imp at the end of its own group while preserving
// the relative order of pre-existing imports. New groups appear in
// stdlib / third-party / local order.
//
// Whitespace handling: the new import inherits a sibling's Prefix (the
// `\n\t` indent inside an `import (...)` block) so the printer renders
// it on its own line. When inserting into an empty block (no siblings),
// a sensible default is used.
func insertGrouped(elements []tree.RightPadded[*tree.Import], imp *tree.Import, modulePath string) []tree.RightPadded[*tree.Import] {
	target := GroupOf(ImportPath(imp), modulePath)
	insertAt := len(elements)
	for i, rp := range elements {
		g := GroupOf(ImportPath(rp.Element), modulePath)
		if g > target {
			insertAt = i
			break
		}
	}
	if imp.Prefix.Whitespace == "" && len(elements) > 0 {
		// Borrow the surrounding indent. If we're inserting in front,
		// take the first sibling's prefix; otherwise the previous
		// sibling's. Both reliably end with `\n\t` in a grouped block.
		var donor *tree.Import
		if insertAt < len(elements) {
			donor = elements[insertAt].Element
		} else {
			donor = elements[len(elements)-1].Element
		}
		if donor != nil {
			imp.Prefix = donor.Prefix
		}
	}
	wrapped := tree.RightPadded[*tree.Import]{Element: imp}
	out := make([]tree.RightPadded[*tree.Import], 0, len(elements)+1)
	out = append(out, elements[:insertAt]...)
	out = append(out, wrapped)
	out = append(out, elements[insertAt:]...)

	// Re-balance trailing whitespace: in a grouped block the last
	// element's After holds the space before `)`. When we appended at
	// the end, the previous tail's After is now between two siblings
	// (and is redundant with the new sibling's leading Prefix); donate
	// it forward so the new tail sits before `)` correctly.
	if insertAt == len(elements) && len(out) >= 2 {
		prev := &out[len(out)-2]
		newTail := &out[len(out)-1]
		newTail.After = prev.After
		prev.After = tree.Space{}
	}
	return out
}

// NewImport builds an Import LST node for `import [alias] "path"`. Pass
// alias=nil for a regular import, "_" for a blank import, "." for a dot
// import, or any identifier name for an aliased import.
func NewImport(path string, alias *string) *tree.Import {
	imp := &tree.Import{
		ID:     uuid.New(),
		Qualid: &tree.Literal{ID: uuid.New(), Source: `"` + path + `"`, Value: path, Kind: tree.StringLiteral},
	}
	if alias != nil {
		imp.Alias = &tree.LeftPadded[*tree.Identifier]{
			Before: tree.Space{Whitespace: " "},
			Element: &tree.Identifier{
				ID:   uuid.New(),
				Name: *alias,
			},
		}
	}
	return imp
}

// SortByGroup returns the imports re-ordered into stdlib / third-party /
// local sequence, alphabetized within each group, with a blank line
// inserted between non-empty groups. Mirrors `goimports -w` output.
//
// Whitespace handling:
//   - Element `Prefix` carries the per-line indent (typically `\n\t`).
//     The first element of each non-leading non-empty group gets a leading
//     `\n` prepended to its indent so the group separator is a blank line.
//   - `RightPadded.After` is anchored to a position (between-elements vs.
//     before-`)`) rather than to its element. When the order changes the
//     anchor changes too — the element that was last is no longer last.
//     This re-balances After so all but the new tail use the original
//     between-element spacing and the new tail uses the original close-paren
//     spacing.
func SortByGroup(elements []tree.RightPadded[*tree.Import], modulePath string) []tree.RightPadded[*tree.Import] {
	if len(elements) <= 1 {
		return elements
	}
	betweenAfter := elements[0].After
	closingAfter := elements[len(elements)-1].After

	// Re-derive the per-line indent prefix from the first non-blank-line
	// element so the blank-line separator below can prepend a single \n
	// to it. The smallest existing prefix that ends in \n + indent wins.
	indentPrefix := tree.Space{Whitespace: "\n\t"}
	for _, rp := range elements {
		if rp.Element == nil {
			continue
		}
		ws := rp.Element.Prefix.Whitespace
		// Strip a leading blank-line newline so we get the canonical
		// "\n\t" indent rather than "\n\n\t".
		canonical := ws
		for strings.HasPrefix(canonical, "\n\n") {
			canonical = canonical[1:]
		}
		if strings.HasPrefix(canonical, "\n") {
			indentPrefix = tree.Space{Whitespace: canonical}
			break
		}
	}

	type bucket struct {
		group ImportGroup
		items []tree.RightPadded[*tree.Import]
	}
	buckets := []bucket{
		{group: Stdlib},
		{group: ThirdParty},
		{group: Local},
	}
	for _, rp := range elements {
		g := GroupOf(ImportPath(rp.Element), modulePath)
		buckets[int(g)].items = append(buckets[int(g)].items, rp)
	}
	for i := range buckets {
		items := buckets[i].items
		sort.SliceStable(items, func(a, b int) bool {
			return ImportPath(items[a].Element) < ImportPath(items[b].Element)
		})
	}

	out := make([]tree.RightPadded[*tree.Import], 0, len(elements))
	groupSeparatorPrefix := tree.Space{Whitespace: "\n" + indentPrefix.Whitespace}
	for _, b := range buckets {
		if len(b.items) == 0 {
			continue
		}
		for j, item := range b.items {
			if item.Element == nil {
				continue
			}
			cloned := *item.Element
			switch {
			case j == 0 && len(out) > 0:
				cloned.Prefix = groupSeparatorPrefix
			default:
				cloned.Prefix = indentPrefix
			}
			item.Element = &cloned
			b.items[j] = item
		}
		out = append(out, b.items...)
	}
	for i := range out {
		if i == len(out)-1 {
			out[i].After = closingAfter
		} else {
			out[i].After = betweenAfter
		}
	}
	return out
}

// FindModulePath extracts the GoResolutionResult marker's ModulePath
// from the cu (or its sibling go.mod, if attached). Returns "" when no
// marker is present (which is fine — IsLocal handles empty modulePath
// by reporting false uniformly).
func FindModulePath(cu *tree.CompilationUnit) string {
	if cu == nil {
		return ""
	}
	for _, m := range cu.Markers.Entries {
		if mrr, ok := m.(tree.GoResolutionResult); ok {
			return mrr.ModulePath
		}
	}
	return ""
}

// _ tree.GoResolutionResult is referenced via FindModulePath; this
// silences the unused-import linter when callers don't pull in the tree
// package explicitly.
var _ = uuid.UUID{}
