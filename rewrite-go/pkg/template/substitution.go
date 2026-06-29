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

	// Preserve the placeholder's prefix whitespace on the substituted node.
	// Use setLeadingPrefix to handle compound nodes where the prefix
	// lives on the first child (e.g., MethodInvocation's Select).
	return setLeadingPrefix(val, ident.Prefix)
}

// substitute applies the substitution visitor to the template tree,
// replacing all placeholder identifiers with captured values.
func substitute(templateTree java.J, values *MatchResult) java.J {
	v := newSubstitutionVisitor(values)
	result := v.Visit(templateTree, nil)
	if result == nil {
		return nil
	}
	return result.(java.J)
}

// setPrefix creates a copy of the node with the given prefix Space.
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
	case *golang.IndexList:
		return n.WithPrefix(prefix)
	case *golang.CompilationUnit:
		return n.WithPrefix(prefix)
	default:
		return j
	}
}
