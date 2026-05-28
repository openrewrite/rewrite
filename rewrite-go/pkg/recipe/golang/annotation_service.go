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
	"strings"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// AnnotationService exposes Go's annotation-equivalents (struct field
// tags + `//go:` / `//lint:` directives) as a uniform J.Annotation
// surface. Mirrors org.openrewrite.java.service.AnnotationService.
//
// In Go, annotation-equivalents live on:
//   - Struct fields: each `key:"value"` tag pair is one Annotation
//     on the field's VariableDeclarations.LeadingAnnotations.
//   - Top-level decls: each `//go:noinline`, `//go:linkname`,
//     `//lint:ignore`, etc. is one Annotation on the enclosing
//     MethodDeclaration / TypeDecl / VariableDeclarations
//     LeadingAnnotations.
//
// Recipes get one via recipe.Service:
//
//	svc := recipe.Service[*golang.AnnotationService](cu)
//	if svc.IsAnnotatedWith(node, "json") { ... }
//
// Or for matcher-based queries:
//
//	if svc.Matches(v.Cursor(), golang.NewAnnotationMatcher("go:linkname")) { ... }
type AnnotationService struct{}

// AllAnnotations returns every annotation attached to the cursor's
// nearest-enclosing decl (the cursor's value if it's a decl, else the
// closest decl ancestor). Mirrors Java's getAllAnnotations(Cursor).
//
// Supported decl types: VariableDeclarations, MethodDeclaration,
// TypeDecl. For other node types, returns nil.
func (s *AnnotationService) AllAnnotations(c *visitor.Cursor) []*tree.Annotation {
	if c == nil {
		return nil
	}
	for cur := c; cur != nil; cur = cur.Parent() {
		switch n := cur.Value().(type) {
		case *tree.VariableDeclarations:
			return n.LeadingAnnotations
		case *tree.MethodDeclaration:
			return n.LeadingAnnotations
		case *tree.TypeDecl:
			return n.LeadingAnnotations
		}
	}
	return nil
}

// AnnotationMatcher matches annotations by name. The pattern can be:
//   - An exact name match: "json", "go:noinline", "lint:ignore".
//   - A wildcard pattern with "*" at the end: "go:*" matches any
//     `//go:` directive; "*" matches any annotation.
type AnnotationMatcher struct {
	pattern string
}

// NewAnnotationMatcher constructs a matcher for the given pattern.
// See AnnotationMatcher for pattern semantics.
func NewAnnotationMatcher(pattern string) AnnotationMatcher {
	return AnnotationMatcher{pattern: pattern}
}

// Matches reports whether the given annotation's type-name matches
// the pattern. Returns false if the annotation has no resolvable name
// (defensive — every annotation we emit has a Identifier or
// FieldAccess as its AnnotationType).
func (m AnnotationMatcher) Matches(ann *tree.Annotation) bool {
	if ann == nil {
		return false
	}
	name := annotationName(ann)
	if name == "" {
		return false
	}
	if m.pattern == "*" {
		return true
	}
	if strings.HasSuffix(m.pattern, "*") {
		return strings.HasPrefix(name, m.pattern[:len(m.pattern)-1])
	}
	return name == m.pattern
}

// annotationName extracts the annotation's type-name. For Identifier
// returns its Name; for FieldAccess (qualified) returns "Target.Name".
func annotationName(ann *tree.Annotation) string {
	switch t := ann.AnnotationType.(type) {
	case *tree.Identifier:
		return t.Name
	case *tree.FieldAccess:
		// Walk the FieldAccess chain — uncommon for Go annotations but
		// included for cross-language symmetry with Java FQNs.
		var b strings.Builder
		appendFieldAccessName(&b, t)
		return b.String()
	}
	return ""
}

func appendFieldAccessName(b *strings.Builder, fa *tree.FieldAccess) {
	switch t := fa.Target.(type) {
	case *tree.Identifier:
		b.WriteString(t.Name)
	case *tree.FieldAccess:
		appendFieldAccessName(b, t)
	}
	if fa.Name.Element != nil {
		if b.Len() > 0 {
			b.WriteByte('.')
		}
		b.WriteString(fa.Name.Element.Name)
	}
}

// Matches reports whether the cursor's enclosing decl carries an
// annotation matching the matcher.
func (s *AnnotationService) Matches(c *visitor.Cursor, matcher AnnotationMatcher) bool {
	for _, a := range s.AllAnnotations(c) {
		if matcher.Matches(a) {
			return true
		}
	}
	return false
}

// IsAnnotatedWith reports whether the given decl carries an annotation
// whose name equals (or wildcard-matches) the given pattern. Convenience
// over Matches with an inline matcher. Common patterns:
//
//	svc.IsAnnotatedWith(field, "json")          // exact key match on a struct field
//	svc.IsAnnotatedWith(funcDecl, "go:noinline") // exact directive match
//	svc.IsAnnotatedWith(funcDecl, "go:*")        // any //go: directive
func (s *AnnotationService) IsAnnotatedWith(t tree.Tree, pattern string) bool {
	matcher := NewAnnotationMatcher(pattern)
	for _, a := range annotationsOn(t) {
		if matcher.Matches(a) {
			return true
		}
	}
	return false
}

// FindAnnotations returns all annotations on the given decl that match
// the pattern.
func (s *AnnotationService) FindAnnotations(t tree.Tree, pattern string) []*tree.Annotation {
	matcher := NewAnnotationMatcher(pattern)
	var out []*tree.Annotation
	for _, a := range annotationsOn(t) {
		if matcher.Matches(a) {
			out = append(out, a)
		}
	}
	return out
}

// annotationsOn returns LeadingAnnotations directly from a decl node,
// without cursor traversal. Used by the IsAnnotatedWith / FindAnnotations
// surface, which take the decl node directly rather than a cursor.
func annotationsOn(t tree.Tree) []*tree.Annotation {
	switch n := t.(type) {
	case *tree.VariableDeclarations:
		return n.LeadingAnnotations
	case *tree.MethodDeclaration:
		return n.LeadingAnnotations
	case *tree.TypeDecl:
		return n.LeadingAnnotations
	}
	return nil
}

// AddAnnotationVisitor returns a visitor that appends an Annotation to
// the LeadingAnnotations of every matching declaration in the visited
// tree. The annotation's `Prefix` is set to a single newline so it
// renders on its own line above the decl (directive convention).
//
// matcher selects which decls receive the annotation. Pass
// `func(t tree.Tree) bool { return true }` to apply universally
// (rare; most recipes scope by decl name or context).
//
// For struct field tags, recipes typically construct the annotation
// manually and append directly rather than using this visitor — tags
// have no leading-newline convention.
func (s *AnnotationService) AddAnnotationVisitor(matcher func(tree.Tree) bool, ann *tree.Annotation) recipe.TreeVisitor {
	return visitor.Init(&addAnnotationVisitor{match: matcher, ann: ann})
}

// RemoveAnnotationVisitor returns a visitor that removes any
// annotation matching the given pattern from every decl in the
// visited tree.
func (s *AnnotationService) RemoveAnnotationVisitor(pattern string) recipe.TreeVisitor {
	return visitor.Init(&removeAnnotationVisitor{matcher: NewAnnotationMatcher(pattern)})
}

type addAnnotationVisitor struct {
	visitor.GoVisitor
	match func(tree.Tree) bool
	ann   *tree.Annotation
}

func (v *addAnnotationVisitor) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	md = v.GoVisitor.VisitMethodDeclaration(md, p).(*tree.MethodDeclaration)
	if v.match(md) {
		clone := positionDirectiveAnnotation(v.ann, &md.Prefix, len(md.LeadingAnnotations) == 0)
		md = md.WithLeadingAnnotations(append(append([]*tree.Annotation{}, md.LeadingAnnotations...), clone))
	}
	return md
}

func (v *addAnnotationVisitor) VisitTypeDecl(td *tree.TypeDecl, p any) tree.J {
	td = v.GoVisitor.VisitTypeDecl(td, p).(*tree.TypeDecl)
	if v.match(td) {
		clone := positionDirectiveAnnotation(v.ann, &td.Prefix, len(td.LeadingAnnotations) == 0)
		td = td.WithLeadingAnnotations(append(append([]*tree.Annotation{}, td.LeadingAnnotations...), clone))
	}
	return td
}

func (v *addAnnotationVisitor) VisitVariableDeclarations(vd *tree.VariableDeclarations, p any) tree.J {
	vd = v.GoVisitor.VisitVariableDeclarations(vd, p).(*tree.VariableDeclarations)
	if v.match(vd) {
		clone := positionDirectiveAnnotation(v.ann, &vd.Prefix, len(vd.LeadingAnnotations) == 0)
		vd = vd.WithLeadingAnnotations(append(append([]*tree.Annotation{}, vd.LeadingAnnotations...), clone))
	}
	return vd
}

// positionDirectiveAnnotation produces a clone of `template` with its
// Prefix set so it sits on its own line above the decl. Mutates
// `*declPrefix` in place when this annotation is the first to be
// added: the outer leading whitespace migrates from the decl's Prefix
// onto the new annotation, and the decl's Prefix becomes a single
// newline (the separator between the directive line and the
// keyword). For non-first annotations, the decl's Prefix is left
// alone and the new annotation gets a `\n` prefix so it stacks below
// existing directives.
func positionDirectiveAnnotation(template *tree.Annotation, declPrefix *tree.Space, isFirst bool) *tree.Annotation {
	clone := cloneAnnotation(template)
	if isFirst {
		clone.Prefix = *declPrefix
		*declPrefix = tree.Space{Whitespace: "\n"}
	} else {
		clone.Prefix = tree.Space{Whitespace: "\n"}
	}
	return clone
}

type removeAnnotationVisitor struct {
	visitor.GoVisitor
	matcher AnnotationMatcher
}

func (v *removeAnnotationVisitor) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	md = v.GoVisitor.VisitMethodDeclaration(md, p).(*tree.MethodDeclaration)
	return md.WithLeadingAnnotations(filterAnnotations(md.LeadingAnnotations, v.matcher))
}

func (v *removeAnnotationVisitor) VisitTypeDecl(td *tree.TypeDecl, p any) tree.J {
	td = v.GoVisitor.VisitTypeDecl(td, p).(*tree.TypeDecl)
	return td.WithLeadingAnnotations(filterAnnotations(td.LeadingAnnotations, v.matcher))
}

func (v *removeAnnotationVisitor) VisitVariableDeclarations(vd *tree.VariableDeclarations, p any) tree.J {
	vd = v.GoVisitor.VisitVariableDeclarations(vd, p).(*tree.VariableDeclarations)
	return vd.WithLeadingAnnotations(filterAnnotations(vd.LeadingAnnotations, v.matcher))
}

func filterAnnotations(in []*tree.Annotation, m AnnotationMatcher) []*tree.Annotation {
	if len(in) == 0 {
		return in
	}
	out := make([]*tree.Annotation, 0, len(in))
	for _, a := range in {
		if !m.Matches(a) {
			out = append(out, a)
		}
	}
	if len(out) == len(in) {
		return in
	}
	return out
}

// cloneAnnotation produces a fresh Annotation with new UUIDs for the
// outer node and its inner Identifier/Literal so the same template
// can be applied to multiple decls without ID collisions.
func cloneAnnotation(ann *tree.Annotation) *tree.Annotation {
	if ann == nil {
		return nil
	}
	c := *ann
	c.ID = uuid.New()
	if id, ok := ann.AnnotationType.(*tree.Identifier); ok {
		idClone := *id
		idClone.ID = uuid.New()
		c.AnnotationType = &idClone
	}
	if ann.Arguments != nil {
		args := *ann.Arguments
		newElems := make([]tree.RightPadded[tree.Expression], len(args.Elements))
		for i, rp := range args.Elements {
			rp2 := rp
			if lit, ok := rp.Element.(*tree.Literal); ok {
				litClone := *lit
				litClone.ID = uuid.New()
				rp2.Element = &litClone
			}
			newElems[i] = rp2
		}
		args.Elements = newElems
		c.Arguments = &args
	}
	return &c
}

func init() {
	recipe.RegisterService[*AnnotationService](func() any { return &AnnotationService{} })
}
