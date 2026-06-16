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
	"reflect"
	"regexp"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// isSourceFile reports whether t is a source-file root for gating purposes.
// It duck-types IsSourceFile() rather than asserting java.SourceFile so that
// non-J roots (e.g. golang.GoMod, which models go.mod and is intentionally
// not a J node) are still recognized as source files.
func isSourceFile(t java.Tree) bool {
	_, ok := t.(interface{ IsSourceFile() })
	return ok
}

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
	if !isSourceFile(t) {
		return t
	}
	src := sourceFilePath(t)
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
	java.WalkTree(t, func(node java.Tree) bool {
		jt := nodeType(node)
		if jt == nil {
			return true
		}
		if fq, ok := jt.(java.FullyQualified); ok {
			fqn := fq.GetFullyQualifiedName()
			if fqn != "" && matchTypeGlob(v.pattern, fqn) {
				found = true
				return false
			}
		}
		return true
	})
	return found
}

// nodeType extracts the JavaType attribution from a node via its exported
// "Type" field. LST nodes expose type attribution as a `Type JavaType`
// field rather than through a common accessor method (mirroring
// matcher.TypeOfExpression), so we read it reflectively and uniformly
// across every node shape that carries one.
func nodeType(node java.Tree) java.JavaType {
	v := reflect.ValueOf(node)
	if v.Kind() == reflect.Ptr {
		if v.IsNil() {
			return nil
		}
		v = v.Elem()
	}
	if v.Kind() != reflect.Struct {
		return nil
	}
	f := v.FieldByName("Type")
	if !f.IsValid() || !f.CanInterface() {
		return nil
	}
	jt, _ := f.Interface().(java.JavaType)
	return jt
}

// UsesMethodVisitor matches files using a specific method, by walking
// the tree for *java.MethodInvocation nodes and consulting MethodMatcher.
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
	java.WalkTree(t, func(node java.Tree) bool {
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
// We wrap rather than mutate the original tree to avoid coupling search
// visitors to the concrete tree type; any java.Tree (including non-J
// roots like golang.GoMod) can be wrapped.
type matchedTree struct {
	java.Tree
}

func markFound(t java.Tree) java.Tree {
	return &matchedTree{Tree: t}
}

// matchPathGlob matches a path against an Ant-style glob pattern. It mirrors
// the matching semantics of org.openrewrite.FindSourceFiles:
//   - "**" matches any number of path segments (crossing "/")
//   - "*"  matches any run of characters within a single segment
//   - "?"  matches a single non-separator character
//
// The pattern is translated to an anchored regexp once per call.
func matchPathGlob(pattern, p string) bool {
	var b strings.Builder
	b.WriteByte('^')
	for i := 0; i < len(pattern); i++ {
		c := pattern[i]
		switch c {
		case '*':
			if i+1 < len(pattern) && pattern[i+1] == '*' {
				b.WriteString(".*")
				i++
				// Swallow a trailing "/" so "**/x" also matches a bare "x".
				if i+1 < len(pattern) && pattern[i+1] == '/' {
					i++
				}
			} else {
				b.WriteString("[^/]*")
			}
		case '?':
			b.WriteString("[^/]")
		case '.', '+', '(', ')', '|', '[', ']', '{', '}', '^', '$', '\\':
			b.WriteByte('\\')
			b.WriteByte(c)
		default:
			b.WriteByte(c)
		}
	}
	b.WriteByte('$')
	re, err := regexp.Compile(b.String())
	return err == nil && re.MatchString(p)
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

// sourceFilePath extracts a string path from a source-file root, falling
// back to the empty string when none is available. It duck-types the
// path accessor so it works for both J source files (CompilationUnit)
// and non-J roots (GoMod).
func sourceFilePath(t java.Tree) string {
	if hp, ok := t.(interface{ GetSourcePath() string }); ok {
		return hp.GetSourcePath()
	}
	if hp, ok := t.(interface{ SourcePath() string }); ok {
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
