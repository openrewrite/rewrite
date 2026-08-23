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

package template

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// substitutionVisitor walks a template tree and replaces placeholder
// identifiers with the corresponding captured AST nodes from a MatchResult.
type substitutionVisitor struct {
	visitor.GoVisitor
	values *MatchResult
}

func newSubstitutionVisitor(values *MatchResult) *substitutionVisitor {
	v := &substitutionVisitor{values: values}
	v.Self = v
	return v
}

func (v *substitutionVisitor) VisitIdentifier(ident *java.Identifier, p any) java.J {
	name, ok := FromPlaceholder(ident.Name)
	if !ok {
		return v.GoVisitor.VisitIdentifier(ident, p)
	}

	val := v.values.Get(name)
	if val == nil {
		return v.GoVisitor.VisitIdentifier(ident, p)
	}
	// A bound subtree came from the source and stays there, so what is
	// spliced is a copy of it.
	val = withFreshIDs(val)

	// Preserve the placeholder's prefix whitespace on the substituted node.
	// Use setLeadingPrefix to handle compound nodes where the prefix
	// lives on the first child (e.g., MethodInvocation's Select).
	// The cursor here walks the template, so the position a capture lands in is
	// the placeholder's own.
	return parenthesized(setLeadingPrefix(val, ident.Prefix), v.Cursor())
}

// A list binding expands within the list its placeholder sits in, so the
// placeholder's own visit leaves it in place for the enclosing call or block to
// replace. Get holds only single bindings, and returns nil for a list.
func (v *substitutionVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	visited := v.GoVisitor.VisitMethodInvocation(mi, p)
	call, ok := visited.(*java.MethodInvocation)
	if !ok {
		return visited
	}
	expanded, changed := expandList(v, call.Arguments.Elements, expressionSeparator)
	if !changed {
		return call
	}
	c := *call
	c.Arguments.Elements = expanded
	return &c
}

func (v *substitutionVisitor) VisitBlock(block *java.Block, p any) java.J {
	visited := v.GoVisitor.VisitBlock(block, p)
	b, ok := visited.(*java.Block)
	if !ok {
		return visited
	}
	expanded, changed := expandList(v, b.Statements, statementSeparator)
	if !changed {
		return b
	}
	return b.WithStatements(expanded)
}

// separator supplies the leading whitespace of the nth element a placeholder
// expands to, given the whitespace the placeholder itself carried.
type separator func(n int, placeholder java.Space) java.Space

// Arguments are separated by the comma the printer emits plus a space, which is
// what the parser leaves on every argument after the first.
func expressionSeparator(n int, placeholder java.Space) java.Space {
	if n == 0 {
		return placeholder
	}
	return java.SingleSpace
}

// Statements are separated by the line break and indent the placeholder already
// stands at, so every one of them starts where it did.
func statementSeparator(_ int, placeholder java.Space) java.Space {
	return placeholder
}

// expandList replaces each list-bound placeholder with the subtrees bound to
// it. The run takes over the placeholder's padding: the whitespace before it
// goes to the run's first element, the whitespace after it to the last. An
// empty run hands the whitespace before it to the element taking its place.
func expandList[T java.J](v *substitutionVisitor, elements []java.RightPadded[T], sep separator) ([]java.RightPadded[T], bool) {
	changed := false
	expanded := make([]java.RightPadded[T], 0, len(elements))
	var vacated *java.Space

	for _, rp := range elements {
		prefix := rp.Element.GetPrefix()
		if vacated != nil {
			prefix, vacated = *vacated, nil
		}

		var values []java.J
		isList := false
		if name, isPlaceholder := placeholderName(java.J(rp.Element)); isPlaceholder {
			values, isList = v.values.listBinding(name)
		}

		if !isList {
			if !java.SpaceEqual(prefix, rp.Element.GetPrefix()) {
				element, ok := setPrefix(rp.Element, prefix).(T)
				if !ok {
					return elements, false
				}
				rp.Element = element
			}
			expanded = append(expanded, rp)
			continue
		}

		changed = true
		if len(values) == 0 {
			vacated = &prefix
			continue
		}
		for i, value := range values {
			// Returning the list untouched leaves the placeholder
			// standing, which substitute reads as the failure it is.
			element, ok := setPrefix(withFreshIDs(value), sep(i, prefix)).(T)
			if !ok {
				return elements, false
			}
			padded := java.RightPadded[T]{Element: element}
			if i == len(values)-1 {
				padded.After, padded.Markers = rp.After, rp.Markers
			}
			expanded = append(expanded, padded)
		}
	}
	return expanded, changed
}

// substitute applies the substitution visitor to the template tree, replacing
// all placeholder identifiers with captured values. It is nil unless every
// placeholder was replaced, a placeholder in the output being no valid Go.
func substitute(templateTree java.J, values *MatchResult) java.J {
	v := newSubstitutionVisitor(values)
	result := v.Visit(templateTree, nil)
	if result == nil {
		return nil
	}
	substituted, ok := result.(java.J)
	if !ok || holdsPlaceholder(substituted) {
		return nil
	}
	return substituted
}

func holdsPlaceholder(j java.J) bool {
	found := false
	f := &placeholderFinder{found: &found}
	f.Self = f
	f.Visit(j, nil)
	return found
}

type placeholderFinder struct {
	visitor.GoVisitor
	found *bool
}

func (v *placeholderFinder) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if IsPlaceholder(ident.Name) {
		*v.found = true
	}
	return v.GoVisitor.VisitIdentifier(ident, p)
}

func setPrefix(j java.J, prefix java.Space) java.J {
	switch n := j.(type) {
	case *java.Identifier:
		return n.WithPrefix(prefix)
	case *java.Literal:
		return n.WithPrefix(prefix)
	case *java.Binary:
		return n.WithPrefix(prefix)
	case *java.Unary:
		return n.WithPrefix(prefix)
	case *golang.Binary:
		return n.WithPrefix(prefix)
	case *golang.Unary:
		return n.WithPrefix(prefix)
	case *golang.AssignmentOperation:
		return n.WithPrefix(prefix)
	case *golang.PointerType:
		return n.WithPrefix(prefix)
	case *golang.Variadic:
		return n.WithPrefix(prefix)
	case *golang.StatementExpression:
		return n.WithPrefix(prefix)
	case *golang.ExpressionStatement:
		return n.WithPrefix(prefix)
	case *golang.DeclarationBlock:
		return n.WithPrefix(prefix)
	case *java.ParameterizedType:
		return n.WithPrefix(prefix)
	case *java.FieldAccess:
		return n.WithPrefix(prefix)
	case *java.MethodInvocation:
		return n.WithPrefix(prefix)
	case *java.Assignment:
		return n.WithPrefix(prefix)
	case *java.AssignmentOperation:
		return n.WithPrefix(prefix)
	case *java.Block:
		return n.WithPrefix(prefix)
	case *java.Return:
		return n.WithPrefix(prefix)
	case *golang.Return:
		return n.WithPrefix(prefix)
	case *java.If:
		return n.WithPrefix(prefix)
	case *golang.MethodDeclaration:
		return n.WithPrefix(prefix)
	case *golang.StatementWithInit:
		return n.WithPrefix(prefix)
	case *java.MethodDeclaration:
		return n.WithPrefix(prefix)
	case *java.VariableDeclarations:
		return n.WithPrefix(prefix)
	case *java.VariableDeclarator:
		return n.WithPrefix(prefix)
	case *java.Parentheses:
		return n.WithPrefix(prefix)
	case *java.ParenthesizedTypeTree:
		return n.WithPrefix(prefix)
	case *golang.TypeAssertion:
		return n.WithPrefix(prefix)
	case *java.TypeCast:
		return n.WithPrefix(prefix)
	case *java.ControlParentheses:
		return n.WithPrefix(prefix)
	case *java.ArrayAccess:
		return n.WithPrefix(prefix)
	case *java.ArrayType:
		return n.WithPrefix(prefix)
	case *golang.ArrayType:
		return n.WithPrefix(prefix)
	case *java.ForLoop:
		return n.WithPrefix(prefix)
	case *java.ForEachLoop:
		return n.WithPrefix(prefix)
	case *java.Switch:
		return n.WithPrefix(prefix)
	case *java.Case:
		return n.WithPrefix(prefix)
	case *java.Break:
		return n.WithPrefix(prefix)
	case *java.Continue:
		return n.WithPrefix(prefix)
	case *java.Label:
		return n.WithPrefix(prefix)
	case *java.Empty:
		return n.WithPrefix(prefix)
	case *java.Import:
		return n.WithPrefix(prefix)
	case *golang.GoStmt:
		return n.WithPrefix(prefix)
	case *golang.Defer:
		return n.WithPrefix(prefix)
	case *golang.Send:
		return n.WithPrefix(prefix)
	case *golang.Goto:
		return n.WithPrefix(prefix)
	case *golang.Fallthrough:
		return n.WithPrefix(prefix)
	case *golang.Composite:
		return n.WithPrefix(prefix)
	case *golang.KeyValue:
		return n.WithPrefix(prefix)
	case *golang.Slice:
		return n.WithPrefix(prefix)
	case *golang.MapType:
		return n.WithPrefix(prefix)
	case *golang.Channel:
		return n.WithPrefix(prefix)
	case *golang.FuncType:
		return n.WithPrefix(prefix)
	case *golang.StructType:
		return n.WithPrefix(prefix)
	case *golang.InterfaceType:
		return n.WithPrefix(prefix)
	case *golang.TypeList:
		return n.WithPrefix(prefix)
	case *golang.Union:
		return n.WithPrefix(prefix)
	case *golang.UnderlyingType:
		return n.WithPrefix(prefix)
	case *golang.TypeDecl:
		return n.WithPrefix(prefix)
	case *golang.MultiAssignment:
		return n.WithPrefix(prefix)
	case *golang.CommClause:
		return n.WithPrefix(prefix)
	case *golang.Select:
		return n.WithPrefix(prefix)
	case *golang.IndexList:
		return n.WithPrefix(prefix)
	case *golang.CompilationUnit:
		return n.WithPrefix(prefix)
	default:
		return j
	}
}
