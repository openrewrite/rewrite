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
	"fmt"
	"reflect"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// WhitespaceValidationService walks a tree and reports any Space whose
// Whitespace or Comment.Suffix contains non-whitespace characters, or
// any Comment.Text that doesn't begin with `//` or `/*`. Such content
// indicates a parser bug — the printer would otherwise re-emit
// non-whitespace as if it were spacing, silently corrupting the source.
//
// Recipes get one via recipe.Service:
//
//	svc := recipe.Service[*golang.WhitespaceValidationService](cu)
//	if errs := svc.Validate(cu); len(errs) > 0 { /* fail loudly */ }
//
// The test harness uses this via pkg/test, which delegates here so the
// validation logic has a single home and stays callable from recipes
// that want to self-check synthesized subtrees.
type WhitespaceValidationService struct{}

// Validate walks the tree rooted at root and returns one descriptive
// error per offending Space / Comment. Returns nil when the tree is
// well-formed.
//
// The walk is by reflection rather than through GoVisitor, which reaches
// only the Spaces it was written to reach — a Container's Markers, and
// so the TrailingComma marker's own spacing, it never visits.
func (s *WhitespaceValidationService) Validate(root java.Tree) []string {
	w := &spaceWalker{seen: map[uintptr]bool{}}
	w.walk(reflect.ValueOf(root), "")
	return w.errs
}

// IsValid is the boolean shorthand. Recipes that just want to assert
// "no parser corruption" can write `if !svc.IsValid(cu) { ... }`.
func (s *WhitespaceValidationService) IsValid(root java.Tree) bool {
	return len(s.Validate(root)) == 0
}

var (
	spaceType     = reflect.TypeOf(java.Space{})
	javaTypeIface = reflect.TypeOf((*java.JavaType)(nil)).Elem()
)

type spaceWalker struct {
	errs []string
	seen map[uintptr]bool
}

func (w *spaceWalker) walk(v reflect.Value, path string) {
	if !v.IsValid() {
		return
	}
	switch v.Kind() {
	case reflect.Ptr, reflect.Interface:
		if v.IsNil() {
			return
		}
		if v.Kind() == reflect.Ptr {
			// Type graphs are cyclic by construction and hold no Space;
			// stopping at them keeps the walk finite and cheap.
			if v.Type().Implements(javaTypeIface) {
				return
			}
			if w.seen[v.Pointer()] {
				return
			}
			w.seen[v.Pointer()] = true
			path = strings.TrimPrefix(v.Type().String(), "*")
		}
		w.walk(v.Elem(), path)
	case reflect.Slice, reflect.Array:
		for i := 0; i < v.Len(); i++ {
			w.walk(v.Index(i), path)
		}
	case reflect.Map:
		for _, k := range v.MapKeys() {
			w.walk(v.MapIndex(k), path)
		}
	case reflect.Struct:
		if v.Type() == spaceType {
			w.checkSpace(v.Interface().(java.Space), path)
			return
		}
		if v.Type().Implements(javaTypeIface) {
			return
		}
		// Padding and container wrappers keep the enclosing node's name,
		// which is what identifies the site to a reader.
		if n := v.Type().Name(); n != "" && !strings.HasPrefix(n, "Container") &&
			!strings.HasPrefix(n, "RightPadded") && !strings.HasPrefix(n, "LeftPadded") {
			path = v.Type().String()
		}
		for i := 0; i < v.NumField(); i++ {
			if v.Type().Field(i).PkgPath != "" {
				continue // unexported: neither reachable nor printed
			}
			w.walk(v.Field(i), path+"."+v.Type().Field(i).Name)
		}
	}
}

func (w *spaceWalker) checkSpace(s java.Space, path string) {
	if s.Whitespace != "" && !isWhitespaceOnly(s.Whitespace) {
		w.errs = append(w.errs, fmt.Sprintf("%s: Space.Whitespace contains non-whitespace: %q", path, truncateForError(s.Whitespace, 80)))
	}
	for i, c := range s.Comments {
		if c.Suffix != "" && !isWhitespaceOnly(c.Suffix) {
			w.errs = append(w.errs, fmt.Sprintf("%s: Comment[%d].Suffix contains non-whitespace: %q", path, i, truncateForError(c.Suffix, 80)))
		}
		if c.Text != "" && !strings.HasPrefix(c.Text, "//") && !strings.HasPrefix(c.Text, "/*") {
			w.errs = append(w.errs, fmt.Sprintf("%s: Comment[%d].Text is not a comment: %q", path, i, truncateForError(c.Text, 80)))
		}
	}
}

func isWhitespaceOnly(s string) bool {
	for _, c := range s {
		if c != ' ' && c != '\t' && c != '\n' && c != '\r' {
			return false
		}
	}
	return true
}

func truncateForError(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "..."
}

func init() {
	recipe.RegisterService[*WhitespaceValidationService](func() any { return &WhitespaceValidationService{} })
}
