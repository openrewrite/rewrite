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

package template

import (
	"path"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// localNames returns the names a function declares that are in scope at site:
// its receiver, type parameters, parameters and results, and what the
// statements ahead of site in each enclosing block and clause declare. A
// package-level declaration is not among them.
func localNames(site *visitor.Cursor) map[string]bool {
	names := map[string]bool{}
	child := site.Value()
	for c := site.Parent(); c != nil; c = c.Parent() {
		switch p := c.Value().(type) {
		case *golang.CompilationUnit:
			return names
		case *java.Block:
			declaredAhead(names, java.UnwrapRightPadded(p.Statements), child)
		case *java.Case:
			declaredAhead(names, java.UnwrapRightPadded(p.Body.Elements), child)
		case *golang.CommClause:
			if any(p.Comm) != child {
				declaredBy(names, p.Comm)
			}
			declaredAhead(names, java.UnwrapRightPadded(p.Body), child)
		case *java.ForControl:
			if p.Init != nil && any(p.Init.Element) != child {
				declaredBy(names, p.Init.Element)
			}
		case *java.ForLoop:
			if any(p.Body.Element) == child && p.Control.Init != nil {
				declaredBy(names, p.Control.Init.Element)
			}
		case *java.ForEachLoop:
			if any(p.Body.Element) == child {
				declaredBy(names, p.Control.Variable.Element)
			}
		case *golang.StatementWithInit:
			if any(p.Init.Element) != child {
				declaredBy(names, p.Init.Element)
			}
		case *java.MethodDeclaration:
			for _, param := range java.UnwrapRightPadded(p.Parameters.Elements) {
				declaredBy(names, param)
			}
			if results, ok := p.ReturnType.(*golang.TypeList); ok {
				for _, result := range java.UnwrapRightPadded(results.Types.Elements) {
					declaredBy(names, result)
				}
			}
			if p.TypeParameters != nil {
				for _, tp := range java.UnwrapRightPadded(p.TypeParameters.TypeParameters) {
					if param, ok := tp.(*java.TypeParameter); ok {
						if name, ok := param.Name.(*java.Identifier); ok {
							names[name.Name] = true
						}
					}
				}
			}
		case *golang.MethodDeclaration:
			for _, receiver := range java.UnwrapRightPadded(p.Receiver.Elements) {
				declaredBy(names, receiver)
			}
		}
		child = c.Value()
	}
	return names
}

// declaredAhead adds what the statements ahead of child declare.
func declaredAhead(names map[string]bool, stmts []java.Statement, child any) {
	for _, stmt := range stmts {
		if any(stmt) == child {
			return
		}
		declaredBy(names, stmt)
	}
}

// declaredBy adds the names stmt itself declares, not those of the blocks it
// holds.
func declaredBy(names map[string]bool, stmt java.J) {
	switch s := stmt.(type) {
	case *java.VariableDeclarations:
		for _, v := range java.UnwrapRightPadded(s.Variables) {
			names[v.Name.Name] = true
		}
	case *java.Assignment:
		if isShortVarDecl(s.Markers) {
			declaredIdentifier(names, s.Variable)
		}
	case *golang.MultiAssignment:
		if isShortVarDecl(s.Markers) {
			for _, v := range java.UnwrapRightPadded(s.Variables) {
				declaredIdentifier(names, v)
			}
		}
	case *golang.TypeDecl:
		if s.Name != nil {
			names[s.Name.Name] = true
		}
		if s.Specs != nil {
			for _, spec := range java.UnwrapRightPadded(s.Specs.Elements) {
				declaredBy(names, spec)
			}
		}
	case *golang.DeclarationBlock:
		if s.Specs != nil {
			for _, spec := range java.UnwrapRightPadded(s.Specs.Elements) {
				declaredBy(names, spec)
			}
		}
	}
}

func declaredIdentifier(names map[string]bool, e java.Expression) {
	if ident, ok := e.(*java.Identifier); ok && ident.Name != "_" {
		names[ident.Name] = true
	}
}

// qualifiers returns the names tree uses to qualify a member of a package it
// imports: the leading identifier of a selector, where it names a package
// through its attribution or, unattributed, through the name the import path
// declares or the alias it is imported under.
func qualifiers(tree java.J, imports, aliases []string) map[string]bool {
	paths := map[string]bool{}
	byName := map[string]bool{}
	for _, imp := range imports {
		paths[imp] = true
		byName[packageNameOf(imp)] = true
	}
	for _, alias := range aliases {
		byName[alias] = true
	}
	names := map[string]bool{}
	note := func(e java.Expression) {
		ident, ok := e.(*java.Identifier)
		if !ok || IsPlaceholder(ident.Name) {
			return
		}
		if class, ok := ident.Type.(*java.JavaTypeClass); ok && paths[class.FullyQualifiedName] {
			names[ident.Name] = true
		} else if ident.Type == nil && byName[ident.Name] {
			names[ident.Name] = true
		}
	}
	visitor.Walk(tree, func(t java.Tree) bool {
		switch n := t.(type) {
		case *java.FieldAccess:
			note(n.Target)
		case *java.MethodInvocation:
			if n.Select != nil {
				note(n.Select.Element)
			}
		}
		return true
	})
	return names
}

// renamedQualifiers gives the leading identifier of each selector in tree that
// renames names the new name.
func renamedQualifiers(tree java.J, renames map[string]string) java.J {
	r := visitor.Init(&qualifierRenamer{renames: renames})
	if renamed, ok := r.Visit(tree, nil).(java.J); ok {
		return renamed
	}
	return tree
}

type qualifierRenamer struct {
	visitor.GoVisitor
	renames map[string]string
}

func (r *qualifierRenamer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	fresh, ok := r.renames[ident.Name]
	if !ok || !r.qualifies(ident) {
		return ident
	}
	c := *ident
	c.Name = fresh
	return &c
}

func (r *qualifierRenamer) qualifies(ident *java.Identifier) bool {
	parent := r.Cursor().Parent()
	if parent == nil {
		return false
	}
	switch p := parent.Value().(type) {
	case *java.FieldAccess:
		return any(p.Target) == any(ident)
	case *java.MethodInvocation:
		return p.Select != nil && any(p.Select.Element) == any(ident)
	}
	return false
}

// packageNameOf is the name an import path conventionally declares: its last
// element, without a major version suffix or a `go-` prefix.
func packageNameOf(importPath string) string {
	elem := path.Base(importPath)
	if isMajorVersion(elem) {
		elem = path.Base(path.Dir(importPath))
	}
	if i := strings.Index(elem, ".v"); i > 0 && isMajorVersion(elem[i+1:]) {
		elem = elem[:i]
	}
	elem = strings.TrimPrefix(elem, "go-")
	return strings.ReplaceAll(elem, "-", "")
}

func isMajorVersion(s string) bool {
	if len(s) < 2 || s[0] != 'v' {
		return false
	}
	for _, r := range s[1:] {
		if r < '0' || r > '9' {
			return false
		}
	}
	return true
}

func compilationUnitOf(c *visitor.Cursor) *golang.CompilationUnit {
	for ; c != nil; c = c.Parent() {
		if cu, ok := c.Value().(*golang.CompilationUnit); ok {
			return cu
		}
	}
	return nil
}
