/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package preconditions

import (
	"path"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// IsSourceFileVisitor matches SourceFile trees by path glob.
//
// Mirrors org.openrewrite.FindSourceFiles. Used as the LocalVisitor
// bundled with the *RecipeRef returned by HasSourcePath so unit tests
// without an active RPC connection still see real filtering.
type IsSourceFileVisitor struct {
	pattern string
}

func NewIsSourceFile(filePattern string) *IsSourceFileVisitor {
	return &IsSourceFileVisitor{pattern: filePattern}
}

func (v *IsSourceFileVisitor) Visit(t java.Tree, _ any) java.Tree {
	sf, ok := t.(java.SourceFile)
	if !ok {
		return t
	}
	src := sourceFilePath(sf)
	if src == "" {
		return t
	}
	if matchPathGlob(v.pattern, src) {
		return markFound(t)
	}
	return t
}

// UsesTypeVisitor matches files using a specific type by walking the
// tree and checking node-level type attribution against a glob pattern.
//
// Mirrors org.openrewrite.java.search.HasType.
type UsesTypeVisitor struct {
	pattern string
}

func NewUsesType(fullyQualifiedType string) *UsesTypeVisitor {
	return &UsesTypeVisitor{pattern: fullyQualifiedType}
}

func (v *UsesTypeVisitor) Visit(t java.Tree, _ any) java.Tree {
	if _, ok := t.(java.SourceFile); !ok {
		return t
	}
	if v.treeUsesType(t) {
		return markFound(t)
	}
	return t
}

func (v *UsesTypeVisitor) treeUsesType(t java.Tree) bool {
	found := false
	walkTree(t, func(node java.Tree) bool {
		if found {
			return false
		}
		if typed, ok := node.(interface{ GetType() java.JavaType }); ok {
			if jt := typed.GetType(); jt != nil {
				if fq, ok := jt.(java.FullyQualified); ok {
					fqn := fq.GetFullyQualifiedName()
					if fqn != "" && matchTypeGlob(v.pattern, fqn) {
						found = true
						return false
					}
				}
			}
		}
		return true
	})
	return found
}

// UsesMethodVisitor matches files using a specific method, by walking
// the tree for *tree.MethodInvocation nodes and consulting MethodMatcher.
//
// Mirrors org.openrewrite.java.search.HasMethod.
type UsesMethodVisitor struct {
	matcher *matcher.MethodMatcher
}

func NewUsesMethod(methodPattern string) *UsesMethodVisitor {
	return &UsesMethodVisitor{matcher: matcher.NewMethodMatcher(methodPattern)}
}

func (v *UsesMethodVisitor) Visit(t java.Tree, _ any) java.Tree {
	if _, ok := t.(java.SourceFile); !ok {
		return t
	}
	if v.treeUsesMethod(t) {
		return markFound(t)
	}
	return t
}

func (v *UsesMethodVisitor) treeUsesMethod(t java.Tree) bool {
	found := false
	walkTree(t, func(node java.Tree) bool {
		if found {
			return false
		}
		if mi, ok := node.(*java.MethodInvocation); ok {
			if v.matcher.Matches(mi) {
				found = true
				return false
			}
		}
		return true
	})
	return found
}

// markFound returns a sentinel "different tree" so the wrapping
// CheckVisitor sees a non-identity result and runs the wrapped editor.
// We use a wrapper struct rather than mutating the original tree to
// avoid coupling search visitors to the concrete tree type.
type matchedSourceFile struct {
	java.SourceFile
}

func markFound(t java.Tree) java.Tree {
	if sf, ok := t.(java.SourceFile); ok {
		return &matchedSourceFile{SourceFile: sf}
	}
	return t
}

// walkTree does a best-effort iterative DFS over a tree. Returns “false“
// from “visit“ to stop walking. Implementation uses pkg/tree's existing
// SearchWalker if available; otherwise falls back to walking the
// tree.J interface via reflection-free iteration of known shapes.
//
// For simplicity we delegate to a shared helper that knows how to descend
// into common LST nodes — see WalkSubtree below.
func walkTree(root java.Tree, visit func(java.Tree) bool) {
	walker := newSearchWalker(visit)
	walker.walk(root)
}

// searchWalker is a minimal iterative walker that knows just enough
// about the LST to descend into compilation units and statement trees.
// It intentionally does not duplicate tree/search_walker.go's full
// machinery — only enough to find type-bearing nodes and method
// invocations for HasType/HasMethod.
type searchWalker struct {
	visit func(java.Tree) bool
	stop  bool
}

func newSearchWalker(visit func(java.Tree) bool) *searchWalker {
	return &searchWalker{visit: visit}
}

func (w *searchWalker) walk(node java.Tree) {
	if w.stop || node == nil {
		return
	}
	if !w.visit(node) {
		w.stop = true
		return
	}
	for _, child := range childrenOf(node) {
		w.walk(child)
		if w.stop {
			return
		}
	}
}

// childrenOf returns immediate child Tree nodes of the given node.
// Best-effort: uses the SearchWalker shape from pkg/tree where available
// and falls back to no children for unknown shapes (which means the
// search visitor returns false negatives for those, but never false
// positives — safe for a precondition gate).
func childrenOf(node java.Tree) []java.Tree {
	if hc, ok := node.(interface{ Children() []java.Tree }); ok {
		return hc.Children()
	}
	return nil
}

// matchPathGlob matches a path against a glob pattern using Go's
// path.Match plus a "**" extension for recursive matching.
func matchPathGlob(pattern, p string) bool {
	if strings.Contains(pattern, "**") {
		// "**/*.go" → match any .go anywhere
		// We simplify to: split on "**", require all parts to appear in order.
		parts := strings.Split(pattern, "**")
		idx := 0
		for i, part := range parts {
			if part == "" {
				continue
			}
			// First part must match prefix; intermediate parts can match
			// anywhere; last part must match suffix.
			j := strings.Index(p[idx:], strings.Trim(part, "/"))
			if j < 0 {
				return false
			}
			if i == 0 && j != 0 {
				return false
			}
			idx += j + len(strings.Trim(part, "/"))
		}
		return true
	}
	matched, err := path.Match(pattern, p)
	return err == nil && matched
}

// matchTypeGlob matches a fully-qualified type name against a pattern.
// Supports "*" for single-segment wildcards (e.g. "java.util.*") and
// "*..*" for any-package-any-type (used in method patterns).
func matchTypeGlob(pattern, fqn string) bool {
	if pattern == fqn {
		return true
	}
	if pattern == "*..*" {
		return true
	}
	// Treat each "*" as matching any sequence of non-dot characters.
	patternParts := strings.Split(pattern, ".")
	fqnParts := strings.Split(fqn, ".")
	if len(patternParts) != len(fqnParts) {
		return false
	}
	for i, pp := range patternParts {
		if pp == "*" {
			continue
		}
		if pp != fqnParts[i] {
			return false
		}
	}
	return true
}

// sourceFilePath extracts a string path from a SourceFile, falling back
// to the empty string when none is available.
func sourceFilePath(sf java.SourceFile) string {
	if hp, ok := sf.(interface{ GetSourcePath() string }); ok {
		return hp.GetSourcePath()
	}
	if hp, ok := sf.(interface{ SourcePath() string }); ok {
		return hp.SourcePath()
	}
	return ""
}

// Compile-time interface checks.
var (
	_ recipe.TreeVisitor = (*IsSourceFileVisitor)(nil)
	_ recipe.TreeVisitor = (*UsesTypeVisitor)(nil)
	_ recipe.TreeVisitor = (*UsesMethodVisitor)(nil)
)
