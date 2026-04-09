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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
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

func (v *substitutionVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	name, ok := FromPlaceholder(ident.Name)
	if !ok {
		return v.GoVisitor.VisitIdentifier(ident, p)
	}

	val := v.values.Get(name)
	if val == nil {
		return v.GoVisitor.VisitIdentifier(ident, p)
	}

	// Preserve the placeholder's prefix whitespace on the substituted node.
	// Use setLeadingPrefix to handle compound nodes where the prefix
	// lives on the first child (e.g., MethodInvocation's Select).
	return setLeadingPrefix(val, ident.Prefix)
}

// substitute applies the substitution visitor to the template tree,
// replacing all placeholder identifiers with captured values.
func substitute(templateTree tree.J, values *MatchResult) tree.J {
	v := newSubstitutionVisitor(values)
	result := v.Visit(templateTree, nil)
	if result == nil {
		return nil
	}
	return result.(tree.J)
}

// setPrefix creates a copy of the node with the given prefix Space.
func setPrefix(j tree.J, prefix tree.Space) tree.J {
	switch n := j.(type) {
	case *tree.Identifier:
		return n.WithPrefix(prefix)
	case *tree.Literal:
		return n.WithPrefix(prefix)
	case *tree.Binary:
		return n.WithPrefix(prefix)
	case *tree.Unary:
		return n.WithPrefix(prefix)
	case *tree.FieldAccess:
		return n.WithPrefix(prefix)
	case *tree.MethodInvocation:
		return n.WithPrefix(prefix)
	case *tree.Assignment:
		return n.WithPrefix(prefix)
	case *tree.AssignmentOperation:
		return n.WithPrefix(prefix)
	case *tree.Block:
		return n.WithPrefix(prefix)
	case *tree.Return:
		return n.WithPrefix(prefix)
	case *tree.If:
		return n.WithPrefix(prefix)
	case *tree.MethodDeclaration:
		return n.WithPrefix(prefix)
	case *tree.VariableDeclarations:
		return n.WithPrefix(prefix)
	case *tree.VariableDeclarator:
		return n.WithPrefix(prefix)
	case *tree.Parentheses:
		return n.WithPrefix(prefix)
	case *tree.TypeCast:
		return n.WithPrefix(prefix)
	case *tree.ControlParentheses:
		return n.WithPrefix(prefix)
	case *tree.ArrayAccess:
		return n.WithPrefix(prefix)
	case *tree.ArrayType:
		return n.WithPrefix(prefix)
	case *tree.ForLoop:
		return n.WithPrefix(prefix)
	case *tree.ForEachLoop:
		return n.WithPrefix(prefix)
	case *tree.Switch:
		return n.WithPrefix(prefix)
	case *tree.Case:
		return n.WithPrefix(prefix)
	case *tree.Break:
		return n.WithPrefix(prefix)
	case *tree.Continue:
		return n.WithPrefix(prefix)
	case *tree.Label:
		return n.WithPrefix(prefix)
	case *tree.Empty:
		return n.WithPrefix(prefix)
	case *tree.Import:
		return n.WithPrefix(prefix)
	case *tree.GoStmt:
		return n.WithPrefix(prefix)
	case *tree.Defer:
		return n.WithPrefix(prefix)
	case *tree.Send:
		return n.WithPrefix(prefix)
	case *tree.Goto:
		return n.WithPrefix(prefix)
	case *tree.Fallthrough:
		return n.WithPrefix(prefix)
	case *tree.Composite:
		return n.WithPrefix(prefix)
	case *tree.KeyValue:
		return n.WithPrefix(prefix)
	case *tree.Slice:
		return n.WithPrefix(prefix)
	case *tree.MapType:
		return n.WithPrefix(prefix)
	case *tree.Channel:
		return n.WithPrefix(prefix)
	case *tree.FuncType:
		return n.WithPrefix(prefix)
	case *tree.StructType:
		return n.WithPrefix(prefix)
	case *tree.InterfaceType:
		return n.WithPrefix(prefix)
	case *tree.TypeList:
		return n.WithPrefix(prefix)
	case *tree.TypeDecl:
		return n.WithPrefix(prefix)
	case *tree.MultiAssignment:
		return n.WithPrefix(prefix)
	case *tree.CommClause:
		return n.WithPrefix(prefix)
	case *tree.IndexList:
		return n.WithPrefix(prefix)
	case *tree.CompilationUnit:
		return n.WithPrefix(prefix)
	default:
		return j
	}
}
