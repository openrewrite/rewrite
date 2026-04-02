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

package rpc

import (
	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// nodeID extracts and formats the ID from any AST node for serialization.
func nodeID(v any) any {
	id := extractID(v)
	if id == nil {
		return nil
	}
	// Convert uuid.UUID to string for JSON serialization
	if u, ok := id.(uuid.UUID); ok {
		return u.String()
	}
	return id
}

// nodePrefix extracts the prefix Space from any AST node (G or J).
func nodePrefix(v any) any {
	switch n := v.(type) {
	// G nodes
	case *tree.CompilationUnit:
		return n.Prefix
	case *tree.GoStmt:
		return n.Prefix
	case *tree.Defer:
		return n.Prefix
	case *tree.Send:
		return n.Prefix
	case *tree.Goto:
		return n.Prefix
	case *tree.Fallthrough:
		return n.Prefix
	case *tree.Composite:
		return n.Prefix
	case *tree.KeyValue:
		return n.Prefix
	case *tree.Slice:
		return n.Prefix
	case *tree.MapType:
		return n.Prefix
	case *tree.PointerType:
		return n.Prefix
	case *tree.Channel:
		return n.Prefix
	case *tree.FuncType:
		return n.Prefix
	case *tree.StructType:
		return n.Prefix
	case *tree.InterfaceType:
		return n.Prefix
	case *tree.TypeList:
		return n.Prefix
	case *tree.TypeDecl:
		return n.Prefix
	case *tree.MultiAssignment:
		return n.Prefix
	case *tree.CommClause:
		return n.Prefix
	case *tree.IndexList:
		return n.Prefix
	// J nodes
	case *tree.Identifier:
		return n.Prefix
	case *tree.Literal:
		return n.Prefix
	case *tree.Binary:
		return n.Prefix
	case *tree.Block:
		return n.Prefix
	case *tree.Return:
		return n.Prefix
	case *tree.If:
		return n.Prefix
	case *tree.Else:
		return n.Prefix
	case *tree.Assignment:
		return n.Prefix
	case *tree.AssignmentOperation:
		return n.Prefix
	case *tree.MethodDeclaration:
		return n.Prefix
	case *tree.ForLoop:
		return n.Prefix
	case *tree.ForControl:
		return n.Prefix
	case *tree.ForEachLoop:
		return n.Prefix
	case *tree.ForEachControl:
		return n.Prefix
	case *tree.Switch:
		return n.Prefix
	case *tree.Case:
		return n.Prefix
	case *tree.Break:
		return n.Prefix
	case *tree.Continue:
		return n.Prefix
	case *tree.Label:
		return n.Prefix
	case *tree.Empty:
		return n.Prefix
	case *tree.Unary:
		return n.Prefix
	case *tree.FieldAccess:
		return n.Prefix
	case *tree.MethodInvocation:
		return n.Prefix
	case *tree.VariableDeclarations:
		return n.Prefix
	case *tree.VariableDeclarator:
		return n.Prefix
	case *tree.ArrayType:
		return n.Prefix
	case *tree.ArrayAccess:
		return n.Prefix
	case *tree.ArrayDimension:
		return n.Prefix
	case *tree.Parentheses:
		return n.Prefix
	case *tree.TypeCast:
		return n.Prefix
	case *tree.ControlParentheses:
		return n.Prefix
	case *tree.Import:
		return n.Prefix
	default:
		return tree.EmptySpace
	}
}

// nodeMarkers extracts the Markers from any AST node (G or J).
func nodeMarkers(v any) any {
	switch n := v.(type) {
	// G nodes
	case *tree.CompilationUnit:
		return n.Markers
	case *tree.GoStmt:
		return n.Markers
	case *tree.Defer:
		return n.Markers
	case *tree.Send:
		return n.Markers
	case *tree.Goto:
		return n.Markers
	case *tree.Fallthrough:
		return n.Markers
	case *tree.Composite:
		return n.Markers
	case *tree.KeyValue:
		return n.Markers
	case *tree.Slice:
		return n.Markers
	case *tree.MapType:
		return n.Markers
	case *tree.PointerType:
		return n.Markers
	case *tree.Channel:
		return n.Markers
	case *tree.FuncType:
		return n.Markers
	case *tree.StructType:
		return n.Markers
	case *tree.InterfaceType:
		return n.Markers
	case *tree.TypeList:
		return n.Markers
	case *tree.TypeDecl:
		return n.Markers
	case *tree.MultiAssignment:
		return n.Markers
	case *tree.CommClause:
		return n.Markers
	case *tree.IndexList:
		return n.Markers
	// J nodes
	case *tree.Identifier:
		return n.Markers
	case *tree.Literal:
		return n.Markers
	case *tree.Binary:
		return n.Markers
	case *tree.Block:
		return n.Markers
	case *tree.Return:
		return n.Markers
	case *tree.If:
		return n.Markers
	case *tree.Else:
		return n.Markers
	case *tree.Assignment:
		return n.Markers
	case *tree.AssignmentOperation:
		return n.Markers
	case *tree.MethodDeclaration:
		return n.Markers
	case *tree.ForLoop:
		return n.Markers
	case *tree.ForControl:
		return n.Markers
	case *tree.ForEachLoop:
		return n.Markers
	case *tree.ForEachControl:
		return n.Markers
	case *tree.Switch:
		return n.Markers
	case *tree.Case:
		return n.Markers
	case *tree.Break:
		return n.Markers
	case *tree.Continue:
		return n.Markers
	case *tree.Label:
		return n.Markers
	case *tree.Empty:
		return n.Markers
	case *tree.Unary:
		return n.Markers
	case *tree.FieldAccess:
		return n.Markers
	case *tree.MethodInvocation:
		return n.Markers
	case *tree.VariableDeclarations:
		return n.Markers
	case *tree.VariableDeclarator:
		return n.Markers
	case *tree.ArrayType:
		return n.Markers
	case *tree.ArrayAccess:
		return n.Markers
	case *tree.ArrayDimension:
		return n.Markers
	case *tree.Parentheses:
		return n.Markers
	case *tree.TypeCast:
		return n.Markers
	case *tree.ControlParentheses:
		return n.Markers
	case *tree.Import:
		return n.Markers
	default:
		return tree.Markers{}
	}
}

// setPrefix sets the prefix Space on any AST node (G or J).
func setPrefix(v any, prefix tree.Space) {
	switch n := v.(type) {
	// G nodes
	case *tree.CompilationUnit:
		n.Prefix = prefix
	case *tree.GoStmt:
		n.Prefix = prefix
	case *tree.Defer:
		n.Prefix = prefix
	case *tree.Send:
		n.Prefix = prefix
	case *tree.Goto:
		n.Prefix = prefix
	case *tree.Fallthrough:
		n.Prefix = prefix
	case *tree.Composite:
		n.Prefix = prefix
	case *tree.KeyValue:
		n.Prefix = prefix
	case *tree.Slice:
		n.Prefix = prefix
	case *tree.MapType:
		n.Prefix = prefix
	case *tree.PointerType:
		n.Prefix = prefix
	case *tree.Channel:
		n.Prefix = prefix
	case *tree.FuncType:
		n.Prefix = prefix
	case *tree.StructType:
		n.Prefix = prefix
	case *tree.InterfaceType:
		n.Prefix = prefix
	case *tree.TypeList:
		n.Prefix = prefix
	case *tree.TypeDecl:
		n.Prefix = prefix
	case *tree.MultiAssignment:
		n.Prefix = prefix
	case *tree.CommClause:
		n.Prefix = prefix
	case *tree.IndexList:
		n.Prefix = prefix
	// J nodes
	case *tree.Identifier:
		n.Prefix = prefix
	case *tree.Literal:
		n.Prefix = prefix
	case *tree.Binary:
		n.Prefix = prefix
	case *tree.Block:
		n.Prefix = prefix
	case *tree.Return:
		n.Prefix = prefix
	case *tree.If:
		n.Prefix = prefix
	case *tree.Else:
		n.Prefix = prefix
	case *tree.Assignment:
		n.Prefix = prefix
	case *tree.AssignmentOperation:
		n.Prefix = prefix
	case *tree.MethodDeclaration:
		n.Prefix = prefix
	case *tree.ForLoop:
		n.Prefix = prefix
	case *tree.ForControl:
		n.Prefix = prefix
	case *tree.ForEachLoop:
		n.Prefix = prefix
	case *tree.ForEachControl:
		n.Prefix = prefix
	case *tree.Switch:
		n.Prefix = prefix
	case *tree.Case:
		n.Prefix = prefix
	case *tree.Break:
		n.Prefix = prefix
	case *tree.Continue:
		n.Prefix = prefix
	case *tree.Label:
		n.Prefix = prefix
	case *tree.Empty:
		n.Prefix = prefix
	case *tree.Unary:
		n.Prefix = prefix
	case *tree.FieldAccess:
		n.Prefix = prefix
	case *tree.MethodInvocation:
		n.Prefix = prefix
	case *tree.VariableDeclarations:
		n.Prefix = prefix
	case *tree.VariableDeclarator:
		n.Prefix = prefix
	case *tree.ArrayType:
		n.Prefix = prefix
	case *tree.ArrayAccess:
		n.Prefix = prefix
	case *tree.ArrayDimension:
		n.Prefix = prefix
	case *tree.Parentheses:
		n.Prefix = prefix
	case *tree.TypeCast:
		n.Prefix = prefix
	case *tree.ControlParentheses:
		n.Prefix = prefix
	case *tree.Import:
		n.Prefix = prefix
	}
}

// setMarkers sets the Markers on any AST node (G or J).
func setMarkers(v any, markers tree.Markers) {
	switch n := v.(type) {
	// G nodes
	case *tree.CompilationUnit:
		n.Markers = markers
	case *tree.GoStmt:
		n.Markers = markers
	case *tree.Defer:
		n.Markers = markers
	case *tree.Send:
		n.Markers = markers
	case *tree.Goto:
		n.Markers = markers
	case *tree.Fallthrough:
		n.Markers = markers
	case *tree.Composite:
		n.Markers = markers
	case *tree.KeyValue:
		n.Markers = markers
	case *tree.Slice:
		n.Markers = markers
	case *tree.MapType:
		n.Markers = markers
	case *tree.PointerType:
		n.Markers = markers
	case *tree.Channel:
		n.Markers = markers
	case *tree.FuncType:
		n.Markers = markers
	case *tree.StructType:
		n.Markers = markers
	case *tree.InterfaceType:
		n.Markers = markers
	case *tree.TypeList:
		n.Markers = markers
	case *tree.TypeDecl:
		n.Markers = markers
	case *tree.MultiAssignment:
		n.Markers = markers
	case *tree.CommClause:
		n.Markers = markers
	case *tree.IndexList:
		n.Markers = markers
	// J nodes
	case *tree.Identifier:
		n.Markers = markers
	case *tree.Literal:
		n.Markers = markers
	case *tree.Binary:
		n.Markers = markers
	case *tree.Block:
		n.Markers = markers
	case *tree.Return:
		n.Markers = markers
	case *tree.If:
		n.Markers = markers
	case *tree.Else:
		n.Markers = markers
	case *tree.Assignment:
		n.Markers = markers
	case *tree.AssignmentOperation:
		n.Markers = markers
	case *tree.MethodDeclaration:
		n.Markers = markers
	case *tree.ForLoop:
		n.Markers = markers
	case *tree.ForControl:
		n.Markers = markers
	case *tree.ForEachLoop:
		n.Markers = markers
	case *tree.ForEachControl:
		n.Markers = markers
	case *tree.Switch:
		n.Markers = markers
	case *tree.Case:
		n.Markers = markers
	case *tree.Break:
		n.Markers = markers
	case *tree.Continue:
		n.Markers = markers
	case *tree.Label:
		n.Markers = markers
	case *tree.Empty:
		n.Markers = markers
	case *tree.Unary:
		n.Markers = markers
	case *tree.FieldAccess:
		n.Markers = markers
	case *tree.MethodInvocation:
		n.Markers = markers
	case *tree.VariableDeclarations:
		n.Markers = markers
	case *tree.VariableDeclarator:
		n.Markers = markers
	case *tree.ArrayType:
		n.Markers = markers
	case *tree.ArrayAccess:
		n.Markers = markers
	case *tree.ArrayDimension:
		n.Markers = markers
	case *tree.Parentheses:
		n.Markers = markers
	case *tree.TypeCast:
		n.Markers = markers
	case *tree.ControlParentheses:
		n.Markers = markers
	case *tree.Import:
		n.Markers = markers
	}
}

// extractID extracts the uuid.UUID ID from any AST node (G or J).
func extractID(v any) any {
	switch t := v.(type) {
	// J nodes
	case *tree.Identifier:
		return t.ID
	case *tree.Literal:
		return t.ID
	case *tree.Binary:
		return t.ID
	case *tree.Block:
		return t.ID
	case *tree.MethodDeclaration:
		return t.ID
	case *tree.MethodInvocation:
		return t.ID
	case *tree.VariableDeclarations:
		return t.ID
	case *tree.VariableDeclarator:
		return t.ID
	case *tree.Assignment:
		return t.ID
	case *tree.Return:
		return t.ID
	case *tree.If:
		return t.ID
	case *tree.Else:
		return t.ID
	case *tree.ForLoop:
		return t.ID
	case *tree.ForEachLoop:
		return t.ID
	case *tree.Switch:
		return t.ID
	case *tree.Case:
		return t.ID
	case *tree.Import:
		return t.ID
	case *tree.Empty:
		return t.ID
	case *tree.Unary:
		return t.ID
	case *tree.FieldAccess:
		return t.ID
	case *tree.ArrayAccess:
		return t.ID
	case *tree.ArrayType:
		return t.ID
	case *tree.Parentheses:
		return t.ID
	case *tree.TypeCast:
		return t.ID
	// G nodes
	case *tree.CompilationUnit:
		return t.ID
	case *tree.GoStmt:
		return t.ID
	case *tree.Defer:
		return t.ID
	case *tree.Send:
		return t.ID
	case *tree.Goto:
		return t.ID
	case *tree.Fallthrough:
		return t.ID
	case *tree.Composite:
		return t.ID
	case *tree.KeyValue:
		return t.ID
	case *tree.Slice:
		return t.ID
	case *tree.MapType:
		return t.ID
	case *tree.PointerType:
		return t.ID
	case *tree.Channel:
		return t.ID
	case *tree.FuncType:
		return t.ID
	case *tree.StructType:
		return t.ID
	case *tree.InterfaceType:
		return t.ID
	case *tree.TypeList:
		return t.ID
	case *tree.TypeDecl:
		return t.ID
	case *tree.MultiAssignment:
		return t.ID
	case *tree.CommClause:
		return t.ID
	case *tree.IndexList:
		return t.ID
	case *tree.StatementExpression:
		return t.ID
	// Additional J nodes
	case *tree.AssignmentOperation:
		return t.ID
	case *tree.ForControl:
		return t.ID
	case *tree.ForEachControl:
		return t.ID
	case *tree.Break:
		return t.ID
	case *tree.Continue:
		return t.ID
	case *tree.Label:
		return t.ID
	case *tree.ArrayDimension:
		return t.ID
	case *tree.ControlParentheses:
		return t.ID
	default:
		return uuid.Nil
	}
}
