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
	"reflect"
	"strings"

	"github.com/openrewrite/rewrite/pkg/tree"
)

func init() {
	// G (Go-specific) node types
	RegisterValueType(reflect.TypeOf((*tree.CompilationUnit)(nil)), "org.openrewrite.golang.tree.Go$CompilationUnit")
	RegisterValueType(reflect.TypeOf((*tree.GoStmt)(nil)), "org.openrewrite.golang.tree.Go$GoStatement")
	RegisterValueType(reflect.TypeOf((*tree.Defer)(nil)), "org.openrewrite.golang.tree.Go$Defer")
	RegisterValueType(reflect.TypeOf((*tree.Send)(nil)), "org.openrewrite.golang.tree.Go$Send")
	RegisterValueType(reflect.TypeOf((*tree.Goto)(nil)), "org.openrewrite.golang.tree.Go$Goto")
	RegisterValueType(reflect.TypeOf((*tree.Fallthrough)(nil)), "org.openrewrite.golang.tree.Go$Fallthrough")
	RegisterValueType(reflect.TypeOf((*tree.Composite)(nil)), "org.openrewrite.golang.tree.Go$Composite")
	RegisterValueType(reflect.TypeOf((*tree.KeyValue)(nil)), "org.openrewrite.golang.tree.Go$KeyValue")
	RegisterValueType(reflect.TypeOf((*tree.Slice)(nil)), "org.openrewrite.golang.tree.Go$SliceExpr")
	RegisterValueType(reflect.TypeOf((*tree.MapType)(nil)), "org.openrewrite.golang.tree.Go$MapType")
	RegisterValueType(reflect.TypeOf((*tree.Channel)(nil)), "org.openrewrite.golang.tree.Go$Channel")
	RegisterValueType(reflect.TypeOf((*tree.FuncType)(nil)), "org.openrewrite.golang.tree.Go$FuncType")
	RegisterValueType(reflect.TypeOf((*tree.StructType)(nil)), "org.openrewrite.golang.tree.Go$StructType")
	RegisterValueType(reflect.TypeOf((*tree.InterfaceType)(nil)), "org.openrewrite.golang.tree.Go$InterfaceType")
	RegisterValueType(reflect.TypeOf((*tree.TypeList)(nil)), "org.openrewrite.golang.tree.Go$TypeList")
	RegisterValueType(reflect.TypeOf((*tree.TypeDecl)(nil)), "org.openrewrite.golang.tree.Go$TypeDecl")
	RegisterValueType(reflect.TypeOf((*tree.MultiAssignment)(nil)), "org.openrewrite.golang.tree.Go$MultiAssignment")
	RegisterValueType(reflect.TypeOf((*tree.CommClause)(nil)), "org.openrewrite.golang.tree.Go$CommClause")
	RegisterValueType(reflect.TypeOf((*tree.IndexList)(nil)), "org.openrewrite.golang.tree.Go$IndexList")

	// J (shared Java-like) node types
	RegisterValueType(reflect.TypeOf((*tree.Identifier)(nil)), "org.openrewrite.java.tree.J$Identifier")
	RegisterValueType(reflect.TypeOf((*tree.Literal)(nil)), "org.openrewrite.java.tree.J$Literal")
	RegisterValueType(reflect.TypeOf((*tree.Binary)(nil)), "org.openrewrite.java.tree.J$Binary")
	RegisterValueType(reflect.TypeOf((*tree.Block)(nil)), "org.openrewrite.java.tree.J$Block")
	RegisterValueType(reflect.TypeOf((*tree.Return)(nil)), "org.openrewrite.java.tree.J$Return")
	RegisterValueType(reflect.TypeOf((*tree.If)(nil)), "org.openrewrite.java.tree.J$If")
	RegisterValueType(reflect.TypeOf((*tree.Else)(nil)), "org.openrewrite.java.tree.J$If$Else")
	RegisterValueType(reflect.TypeOf((*tree.Assignment)(nil)), "org.openrewrite.java.tree.J$Assignment")
	RegisterValueType(reflect.TypeOf((*tree.AssignmentOperation)(nil)), "org.openrewrite.java.tree.J$AssignmentOperation")
	RegisterValueType(reflect.TypeOf((*tree.MethodDeclaration)(nil)), "org.openrewrite.java.tree.J$MethodDeclaration")
	RegisterValueType(reflect.TypeOf((*tree.ForLoop)(nil)), "org.openrewrite.java.tree.J$ForLoop")
	RegisterValueType(reflect.TypeOf((*tree.ForControl)(nil)), "org.openrewrite.java.tree.J$ForLoop$Control")
	RegisterValueType(reflect.TypeOf((*tree.ForEachLoop)(nil)), "org.openrewrite.java.tree.J$ForEachLoop")
	RegisterValueType(reflect.TypeOf((*tree.ForEachControl)(nil)), "org.openrewrite.java.tree.J$ForEachLoop$Control")
	RegisterValueType(reflect.TypeOf((*tree.Switch)(nil)), "org.openrewrite.java.tree.J$Switch")
	RegisterValueType(reflect.TypeOf((*tree.Case)(nil)), "org.openrewrite.java.tree.J$Case")
	RegisterValueType(reflect.TypeOf((*tree.Break)(nil)), "org.openrewrite.java.tree.J$Break")
	RegisterValueType(reflect.TypeOf((*tree.Continue)(nil)), "org.openrewrite.java.tree.J$Continue")
	RegisterValueType(reflect.TypeOf((*tree.Label)(nil)), "org.openrewrite.java.tree.J$Label")
	RegisterValueType(reflect.TypeOf((*tree.Empty)(nil)), "org.openrewrite.java.tree.J$Empty")
	RegisterValueType(reflect.TypeOf((*tree.Unary)(nil)), "org.openrewrite.java.tree.J$Unary")
	RegisterValueType(reflect.TypeOf((*tree.FieldAccess)(nil)), "org.openrewrite.java.tree.J$FieldAccess")
	RegisterValueType(reflect.TypeOf((*tree.MethodInvocation)(nil)), "org.openrewrite.java.tree.J$MethodInvocation")
	RegisterValueType(reflect.TypeOf((*tree.VariableDeclarations)(nil)), "org.openrewrite.java.tree.J$VariableDeclarations")
	RegisterValueType(reflect.TypeOf((*tree.VariableDeclarator)(nil)), "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable")
	RegisterValueType(reflect.TypeOf((*tree.ArrayType)(nil)), "org.openrewrite.java.tree.J$ArrayType")
	RegisterValueType(reflect.TypeOf((*tree.ArrayAccess)(nil)), "org.openrewrite.java.tree.J$ArrayAccess")
	RegisterValueType(reflect.TypeOf((*tree.ArrayDimension)(nil)), "org.openrewrite.java.tree.J$ArrayDimension")
	RegisterValueType(reflect.TypeOf((*tree.Parentheses)(nil)), "org.openrewrite.java.tree.J$Parentheses")
	RegisterValueType(reflect.TypeOf((*tree.TypeCast)(nil)), "org.openrewrite.java.tree.J$TypeCast")
	RegisterValueType(reflect.TypeOf((*tree.ControlParentheses)(nil)), "org.openrewrite.java.tree.J$ControlParentheses")
	RegisterValueType(reflect.TypeOf((*tree.Import)(nil)), "org.openrewrite.java.tree.J$Import")

	// Non-tree types that Java needs valueType for
	RegisterValueType(reflect.TypeOf(tree.Space{}), "org.openrewrite.java.tree.Space")
	RegisterValueType(reflect.TypeOf(tree.Markers{}), "org.openrewrite.marker.Markers")
	RegisterValueType(reflect.TypeOf(tree.Comment{}), "org.openrewrite.java.tree.Comment")

	// Go-specific marker valueType registrations (for send-side type resolution)
	RegisterValueType(reflect.TypeOf(tree.GroupedImport{}), "org.openrewrite.golang.marker.GroupedImport")
	RegisterValueType(reflect.TypeOf(tree.ImportBlock{}), "org.openrewrite.golang.marker.ImportBlock")
	RegisterValueType(reflect.TypeOf(tree.ShortVarDecl{}), "org.openrewrite.golang.marker.ShortVarDecl")
	RegisterValueType(reflect.TypeOf(tree.VarKeyword{}), "org.openrewrite.golang.marker.VarKeyword")
	RegisterValueType(reflect.TypeOf(tree.ConstDecl{}), "org.openrewrite.golang.marker.ConstDecl")
	RegisterValueType(reflect.TypeOf(tree.GroupedSpec{}), "org.openrewrite.golang.marker.GroupedSpec")
	RegisterValueType(reflect.TypeOf(tree.InterfaceMethod{}), "org.openrewrite.golang.marker.InterfaceMethod")
	RegisterValueType(reflect.TypeOf(tree.SelectStmt{}), "org.openrewrite.golang.marker.SelectStmt")
	RegisterValueType(reflect.TypeOf(tree.TypeSwitchGuard{}), "org.openrewrite.golang.marker.TypeSwitchGuard")
	RegisterValueType(reflect.TypeOf(tree.StructTag{}), "org.openrewrite.golang.marker.StructTag")
	RegisterValueType(reflect.TypeOf(tree.TrailingComma{}), "org.openrewrite.golang.marker.TrailingComma")
	RegisterValueType(reflect.TypeOf(tree.SearchResult{}), "org.openrewrite.marker.SearchResult")

	// JavaType types
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeClass)(nil)), "org.openrewrite.java.tree.JavaType$Class")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeParameterized)(nil)), "org.openrewrite.java.tree.JavaType$Parameterized")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeGenericTypeVariable)(nil)), "org.openrewrite.java.tree.JavaType$GenericTypeVariable")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeArray)(nil)), "org.openrewrite.java.tree.JavaType$Array")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypePrimitive)(nil)), "org.openrewrite.java.tree.JavaType$Primitive")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeMethod)(nil)), "org.openrewrite.java.tree.JavaType$Method")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeVariable)(nil)), "org.openrewrite.java.tree.JavaType$Variable")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeAnnotation)(nil)), "org.openrewrite.java.tree.JavaType$Annotation")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeMultiCatch)(nil)), "org.openrewrite.java.tree.JavaType$MultiCatch")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeIntersection)(nil)), "org.openrewrite.java.tree.JavaType$Intersection")
	RegisterValueType(reflect.TypeOf((*tree.JavaTypeUnknown)(nil)), "org.openrewrite.java.tree.JavaType$Unknown")

	// Register factories for creating instances during deserialization
	RegisterFactory("org.openrewrite.golang.tree.Go$CompilationUnit", func() any { return &tree.CompilationUnit{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$GoStatement", func() any { return &tree.GoStmt{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Defer", func() any { return &tree.Defer{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Send", func() any { return &tree.Send{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Goto", func() any { return &tree.Goto{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Fallthrough", func() any { return &tree.Fallthrough{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Composite", func() any { return &tree.Composite{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$KeyValue", func() any { return &tree.KeyValue{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$SliceExpr", func() any { return &tree.Slice{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$MapType", func() any { return &tree.MapType{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Channel", func() any { return &tree.Channel{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$FuncType", func() any { return &tree.FuncType{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$StructType", func() any { return &tree.StructType{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$InterfaceType", func() any { return &tree.InterfaceType{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$TypeList", func() any { return &tree.TypeList{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$TypeDecl", func() any { return &tree.TypeDecl{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$MultiAssignment", func() any { return &tree.MultiAssignment{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$CommClause", func() any { return &tree.CommClause{} })
	RegisterFactory("org.openrewrite.golang.tree.Go$IndexList", func() any { return &tree.IndexList{} })

	RegisterFactory("org.openrewrite.java.tree.J$Identifier", func() any { return &tree.Identifier{} })
	RegisterFactory("org.openrewrite.java.tree.J$Literal", func() any { return &tree.Literal{} })
	RegisterFactory("org.openrewrite.java.tree.J$Binary", func() any { return &tree.Binary{} })
	RegisterFactory("org.openrewrite.java.tree.J$Block", func() any { return &tree.Block{} })
	RegisterFactory("org.openrewrite.java.tree.J$Return", func() any { return &tree.Return{} })
	RegisterFactory("org.openrewrite.java.tree.J$If", func() any { return &tree.If{} })
	RegisterFactory("org.openrewrite.java.tree.J$If$Else", func() any { return &tree.Else{} })
	RegisterFactory("org.openrewrite.java.tree.J$Assignment", func() any { return &tree.Assignment{} })
	RegisterFactory("org.openrewrite.java.tree.J$AssignmentOperation", func() any { return &tree.AssignmentOperation{} })
	RegisterFactory("org.openrewrite.java.tree.J$MethodDeclaration", func() any { return &tree.MethodDeclaration{} })
	RegisterFactory("org.openrewrite.java.tree.J$ForLoop", func() any { return &tree.ForLoop{} })
	RegisterFactory("org.openrewrite.java.tree.J$ForLoop$Control", func() any { return &tree.ForControl{} })
	RegisterFactory("org.openrewrite.java.tree.J$ForEachLoop", func() any { return &tree.ForEachLoop{} })
	RegisterFactory("org.openrewrite.java.tree.J$ForEachLoop$Control", func() any { return &tree.ForEachControl{} })
	RegisterFactory("org.openrewrite.java.tree.J$Switch", func() any { return &tree.Switch{} })
	RegisterFactory("org.openrewrite.java.tree.J$Case", func() any { return &tree.Case{} })
	RegisterFactory("org.openrewrite.java.tree.J$Break", func() any { return &tree.Break{} })
	RegisterFactory("org.openrewrite.java.tree.J$Continue", func() any { return &tree.Continue{} })
	RegisterFactory("org.openrewrite.java.tree.J$Label", func() any { return &tree.Label{} })
	RegisterFactory("org.openrewrite.java.tree.J$Empty", func() any { return &tree.Empty{} })
	RegisterFactory("org.openrewrite.java.tree.J$Unary", func() any { return &tree.Unary{} })
	RegisterFactory("org.openrewrite.java.tree.J$FieldAccess", func() any { return &tree.FieldAccess{} })
	RegisterFactory("org.openrewrite.java.tree.J$MethodInvocation", func() any { return &tree.MethodInvocation{} })
	RegisterFactory("org.openrewrite.java.tree.J$VariableDeclarations", func() any { return &tree.VariableDeclarations{} })
	RegisterFactory("org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable", func() any { return &tree.VariableDeclarator{} })
	RegisterFactory("org.openrewrite.java.tree.J$ArrayType", func() any { return &tree.ArrayType{} })
	RegisterFactory("org.openrewrite.java.tree.J$ArrayAccess", func() any { return &tree.ArrayAccess{} })
	RegisterFactory("org.openrewrite.java.tree.J$ArrayDimension", func() any { return &tree.ArrayDimension{} })
	RegisterFactory("org.openrewrite.java.tree.J$Parentheses", func() any { return &tree.Parentheses{} })
	RegisterFactory("org.openrewrite.java.tree.J$TypeCast", func() any { return &tree.TypeCast{} })
	RegisterFactory("org.openrewrite.java.tree.J$ControlParentheses", func() any { return &tree.ControlParentheses{} })
	RegisterFactory("org.openrewrite.java.tree.J$Import", func() any { return &tree.Import{} })

	// SourceFile-level types that implement RpcCodec
	RegisterFactory("org.openrewrite.Checksum", func() any { return tree.GenericMarker{} })
	RegisterFactory("org.openrewrite.FileAttributes", func() any { return tree.GenericMarker{} })

	// Java-side markers that may appear when recipes modify trees
	// RecipesThatMadeChanges: NOT an RpcCodec, serialized as raw value
	RegisterFactory("org.openrewrite.marker.RecipesThatMadeChanges", func() any { return tree.GenericMarker{} })
	// SearchResult: IS an RpcCodec, sends 2 sub-fields (id, description)
	RegisterFactory("org.openrewrite.marker.SearchResult", func() any { return tree.SearchResult{} })
	// GroupedImport: IS an RpcCodec, sends 2 sub-fields (id, before whitespace)
	RegisterFactory("org.openrewrite.golang.marker.GroupedImport", func() any { return tree.GroupedImport{} })
	// ImportBlock: IS an RpcCodec, sends 5 sub-fields (id, closePrevious, before, grouped, groupedBefore)
	RegisterFactory("org.openrewrite.golang.marker.ImportBlock", func() any { return tree.ImportBlock{} })
	// Go-specific markers: all are RpcCodec
	RegisterFactory("org.openrewrite.golang.marker.ShortVarDecl", func() any { return tree.ShortVarDecl{} })
	RegisterFactory("org.openrewrite.golang.marker.VarKeyword", func() any { return tree.VarKeyword{} })
	RegisterFactory("org.openrewrite.golang.marker.ConstDecl", func() any { return tree.ConstDecl{} })
	RegisterFactory("org.openrewrite.golang.marker.GroupedSpec", func() any { return tree.GroupedSpec{} })
	RegisterFactory("org.openrewrite.golang.marker.InterfaceMethod", func() any { return tree.InterfaceMethod{} })
	RegisterFactory("org.openrewrite.golang.marker.SelectStmt", func() any { return tree.SelectStmt{} })
	RegisterFactory("org.openrewrite.golang.marker.TypeSwitchGuard", func() any { return tree.TypeSwitchGuard{} })
	RegisterFactory("org.openrewrite.golang.marker.StructTag", func() any { return tree.StructTag{} })
	RegisterFactory("org.openrewrite.golang.marker.TrailingComma", func() any { return tree.TrailingComma{} })

	RegisterFactory("org.openrewrite.java.tree.Space", func() any { return tree.Space{} })
	RegisterFactory("org.openrewrite.marker.Markers", func() any { return tree.Markers{} })
	RegisterFactory("org.openrewrite.java.tree.Comment", func() any { return tree.Comment{} })

	// Padding types — needed when Java sends ADD messages for new padding
	// wrappers during bidirectional tree transfer (e.g., after a recipe
	// recreates a statement list element). Uses RightPadded[tree.J] etc.
	// as the broadest type that the padding accessors can handle.
	RegisterFactory("org.openrewrite.java.tree.JRightPadded", func() any { return tree.RightPadded[tree.J]{} })
	RegisterFactory("org.openrewrite.java.tree.JLeftPadded", func() any { return tree.LeftPadded[tree.J]{} })
	RegisterFactory("org.openrewrite.java.tree.JContainer", func() any { return tree.Container[tree.J]{} })


	RegisterFactory("org.openrewrite.java.tree.JavaType$Class", func() any { return &tree.JavaTypeClass{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Parameterized", func() any { return &tree.JavaTypeParameterized{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$GenericTypeVariable", func() any { return &tree.JavaTypeGenericTypeVariable{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Array", func() any { return &tree.JavaTypeArray{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Primitive", func() any { return &tree.JavaTypePrimitive{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Method", func() any { return &tree.JavaTypeMethod{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Variable", func() any { return &tree.JavaTypeVariable{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Annotation", func() any { return &tree.JavaTypeAnnotation{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$MultiCatch", func() any { return &tree.JavaTypeMultiCatch{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Intersection", func() any { return &tree.JavaTypeIntersection{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Unknown", func() any { return &tree.JavaTypeUnknown{} })
}

// getValueTypeForPadding returns the Java class name for padding types.
// Go generic types have no reflect.Name(), so we match by string representation.
func getValueTypeForPadding(v any) *string {
	ts := reflect.TypeOf(v).String()
	if strings.Contains(ts, "RightPadded[") {
		vt := "org.openrewrite.java.tree.JRightPadded"
		return &vt
	}
	if strings.Contains(ts, "LeftPadded[") {
		vt := "org.openrewrite.java.tree.JLeftPadded"
		return &vt
	}
	if strings.Contains(ts, "Container[") {
		vt := "org.openrewrite.java.tree.JContainer"
		return &vt
	}
	return nil
}
