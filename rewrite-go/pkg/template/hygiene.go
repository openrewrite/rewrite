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
	"strconv"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// hygienic renames what the template declares for its own use, in a block, a
// clause or a func literal of its own, where a name a bound subtree refers to
// would otherwise resolve to it once spliced in. A declaration the template
// leads with is in scope at the site, so it keeps the name the author gave it,
// as does one the replaced code declares too: a subtree captured from inside
// it refers to that declaration, which the template stands in for.
func hygienic(tree java.J, values *MatchResult, replaced java.J) java.J {
	if values == nil || len(values.bindings) == 0 {
		return tree
	}
	inner := innerDeclarations(tree)
	if len(inner) == 0 {
		return tree
	}
	for name := range declarationsIn(replaced) {
		delete(inner, name)
	}
	if len(inner) == 0 {
		return tree
	}
	referenced := map[string]bool{}
	for _, binding := range values.bindings {
		switch b := binding.(type) {
		case java.J:
			referencesIn(referenced, b)
		case []java.J:
			for _, j := range b {
				referencesIn(referenced, j)
			}
		}
	}
	taken := map[string]bool{}
	identifiersIn(taken, tree)
	for name := range referenced {
		taken[name] = true
	}
	renames := map[string]string{}
	for name := range inner {
		if !referenced[name] {
			continue
		}
		fresh := name
		for i := 1; taken[fresh]; i++ {
			fresh = name + strconv.Itoa(i)
		}
		taken[fresh] = true
		renames[name] = fresh
	}
	if len(renames) == 0 {
		return tree
	}
	r := visitor.Init(&renamer{renames: renames})
	if renamed, ok := r.Visit(tree, nil).(java.J); ok {
		return renamed
	}
	return tree
}

// declarationsIn names everything tree declares, itself included.
func declarationsIn(tree java.J) map[string]bool {
	names := innerDeclarations(tree)
	declaredBy(names, tree)
	return names
}

// innerDeclarations names what tree declares in a scope of its own.
func innerDeclarations(tree java.J) map[string]bool {
	names := map[string]bool{}
	visitor.Walk(tree, func(t java.Tree) bool {
		switch n := t.(type) {
		case *java.Block:
			for _, stmt := range java.UnwrapRightPadded(n.Statements) {
				declaredBy(names, stmt)
			}
		case *java.Case:
			for _, stmt := range java.UnwrapRightPadded(n.Body.Elements) {
				declaredBy(names, stmt)
			}
		case *golang.CommClause:
			declaredBy(names, n.Comm)
			for _, stmt := range java.UnwrapRightPadded(n.Body) {
				declaredBy(names, stmt)
			}
		case *java.ForControl:
			if n.Init != nil {
				declaredBy(names, n.Init.Element)
			}
		case *java.ForEachControl:
			declaredBy(names, n.Variable.Element)
		case *golang.StatementWithInit:
			declaredBy(names, n.Init.Element)
		case *java.MethodDeclaration:
			for _, param := range java.UnwrapRightPadded(n.Parameters.Elements) {
				declaredBy(names, param)
			}
			if results, ok := n.ReturnType.(*golang.TypeList); ok {
				for _, result := range java.UnwrapRightPadded(results.Types.Elements) {
					declaredBy(names, result)
				}
			}
		}
		return true
	})
	delete(names, "_")
	for name := range names {
		if IsPlaceholder(name) {
			delete(names, name)
		}
	}
	return names
}

func identifiersIn(names map[string]bool, j java.J) {
	visitor.Walk(j, func(t java.Tree) bool {
		if ident, ok := t.(*java.Identifier); ok && ident.Name != "" {
			names[ident.Name] = true
		}
		return true
	})
}

// referencesIn adds the names j refers to as variables, leaving out the member
// a selector names and the key of a composite literal's element.
func referencesIn(names map[string]bool, j java.J) {
	members := map[*java.Identifier]bool{}
	visitor.Walk(j, func(t java.Tree) bool {
		switch n := t.(type) {
		case *java.FieldAccess:
			members[n.Name.Element] = true
		case *java.MethodInvocation:
			if n.Select != nil {
				members[n.Name] = true
			}
		case *golang.KeyValue:
			if key, ok := n.Key.(*java.Identifier); ok {
				members[key] = true
			}
		case *java.Identifier:
			if !members[n] && n.Name != "" {
				names[n.Name] = true
			}
		}
		return true
	})
}

// renamer renames identifiers, other than the member a selector names and the
// key of a composite literal's element, which name a field rather than a
// variable.
type renamer struct {
	visitor.GoVisitor
	renames map[string]string
}

func (r *renamer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	fresh, ok := r.renames[ident.Name]
	if !ok || r.namesAMember(ident) {
		return ident
	}
	c := *ident
	c.Name = fresh
	return &c
}

func (r *renamer) namesAMember(ident *java.Identifier) bool {
	parent := r.Cursor().Parent()
	if parent == nil {
		return false
	}
	switch p := parent.Value().(type) {
	case *java.FieldAccess:
		return p.Name.Element == ident
	case *java.MethodInvocation:
		return p.Select != nil && p.Name == ident
	case *golang.KeyValue:
		return any(p.Key) == any(ident)
	}
	return false
}
