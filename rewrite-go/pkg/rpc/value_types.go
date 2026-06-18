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

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func init() {
	// G (Go-specific) node types
	RegisterValueType(reflect.TypeOf((*golang.CompilationUnit)(nil)), "org.openrewrite.golang.tree.Go$CompilationUnit")
	RegisterValueType(reflect.TypeOf((*golang.GoStmt)(nil)), "org.openrewrite.golang.tree.Go$GoStatement")
	RegisterValueType(reflect.TypeOf((*golang.Defer)(nil)), "org.openrewrite.golang.tree.Go$Defer")
	RegisterValueType(reflect.TypeOf((*golang.Send)(nil)), "org.openrewrite.golang.tree.Go$Send")
	RegisterValueType(reflect.TypeOf((*golang.Goto)(nil)), "org.openrewrite.golang.tree.Go$Goto")
	RegisterValueType(reflect.TypeOf((*golang.Unary)(nil)), "org.openrewrite.golang.tree.Go$Unary")
	RegisterValueType(reflect.TypeOf((*golang.Binary)(nil)), "org.openrewrite.golang.tree.Go$Binary")
	RegisterValueType(reflect.TypeOf((*golang.AssignmentOperation)(nil)), "org.openrewrite.golang.tree.Go$AssignmentOperation")
	RegisterValueType(reflect.TypeOf((*golang.Variadic)(nil)), "org.openrewrite.golang.tree.Go$Variadic")
	RegisterValueType(reflect.TypeOf((*golang.Fallthrough)(nil)), "org.openrewrite.golang.tree.Go$Fallthrough")
	RegisterValueType(reflect.TypeOf((*golang.Composite)(nil)), "org.openrewrite.golang.tree.Go$Composite")
	RegisterValueType(reflect.TypeOf((*golang.KeyValue)(nil)), "org.openrewrite.golang.tree.Go$KeyValue")
	RegisterValueType(reflect.TypeOf((*golang.Slice)(nil)), "org.openrewrite.golang.tree.Go$SliceExpr")
	RegisterValueType(reflect.TypeOf((*golang.ArrayType)(nil)), "org.openrewrite.golang.tree.Go$ArrayType")
	RegisterValueType(reflect.TypeOf((*golang.MapType)(nil)), "org.openrewrite.golang.tree.Go$MapType")
	RegisterValueType(reflect.TypeOf((*golang.PointerType)(nil)), "org.openrewrite.golang.tree.Go$PointerType")
	RegisterValueType(reflect.TypeOf((*golang.Channel)(nil)), "org.openrewrite.golang.tree.Go$Channel")
	RegisterValueType(reflect.TypeOf((*golang.FuncType)(nil)), "org.openrewrite.golang.tree.Go$FuncType")
	RegisterValueType(reflect.TypeOf((*golang.StructType)(nil)), "org.openrewrite.golang.tree.Go$StructType")
	RegisterValueType(reflect.TypeOf((*golang.InterfaceType)(nil)), "org.openrewrite.golang.tree.Go$InterfaceType")
	RegisterValueType(reflect.TypeOf((*golang.TypeList)(nil)), "org.openrewrite.golang.tree.Go$TypeList")
	RegisterValueType(reflect.TypeOf((*golang.Union)(nil)), "org.openrewrite.golang.tree.Go$Union")
	RegisterValueType(reflect.TypeOf((*golang.UnderlyingType)(nil)), "org.openrewrite.golang.tree.Go$UnderlyingType")
	RegisterValueType(reflect.TypeOf((*golang.TypeDecl)(nil)), "org.openrewrite.golang.tree.Go$TypeDecl")
	RegisterValueType(reflect.TypeOf((*golang.DeclarationBlock)(nil)), "org.openrewrite.golang.tree.Go$DeclarationBlock")
	RegisterValueType(reflect.TypeOf((*golang.MultiAssignment)(nil)), "org.openrewrite.golang.tree.Go$MultiAssignment")
	RegisterValueType(reflect.TypeOf((*golang.Return)(nil)), "org.openrewrite.golang.tree.Go$Return")
	RegisterValueType(reflect.TypeOf((*golang.MethodDeclaration)(nil)), "org.openrewrite.golang.tree.Go$MethodDeclaration")
	RegisterValueType(reflect.TypeOf((*golang.StatementWithInit)(nil)), "org.openrewrite.golang.tree.Go$StatementWithInit")
	RegisterValueType(reflect.TypeOf((*golang.CommClause)(nil)), "org.openrewrite.golang.tree.Go$CommClause")
	RegisterValueType(reflect.TypeOf((*golang.IndexList)(nil)), "org.openrewrite.golang.tree.Go$IndexList")
	RegisterValueType(reflect.TypeOf((*golang.StatementExpression)(nil)), "org.openrewrite.golang.tree.Go$StatementExpression")

	// J (shared Java-like) node types
	RegisterValueType(reflect.TypeOf((*java.Identifier)(nil)), "org.openrewrite.java.tree.J$Identifier")
	RegisterValueType(reflect.TypeOf((*java.Literal)(nil)), "org.openrewrite.java.tree.J$Literal")
	RegisterValueType(reflect.TypeOf((*java.Binary)(nil)), "org.openrewrite.java.tree.J$Binary")
	RegisterValueType(reflect.TypeOf((*java.Block)(nil)), "org.openrewrite.java.tree.J$Block")
	RegisterValueType(reflect.TypeOf((*java.Return)(nil)), "org.openrewrite.java.tree.J$Return")
	RegisterValueType(reflect.TypeOf((*java.If)(nil)), "org.openrewrite.java.tree.J$If")
	RegisterValueType(reflect.TypeOf((*java.Else)(nil)), "org.openrewrite.java.tree.J$If$Else")
	RegisterValueType(reflect.TypeOf((*java.Assignment)(nil)), "org.openrewrite.java.tree.J$Assignment")
	RegisterValueType(reflect.TypeOf((*java.AssignmentOperation)(nil)), "org.openrewrite.java.tree.J$AssignmentOperation")
	RegisterValueType(reflect.TypeOf((*java.MethodDeclaration)(nil)), "org.openrewrite.java.tree.J$MethodDeclaration")
	RegisterValueType(reflect.TypeOf((*java.ForLoop)(nil)), "org.openrewrite.java.tree.J$ForLoop")
	RegisterValueType(reflect.TypeOf((*java.ForControl)(nil)), "org.openrewrite.java.tree.J$ForLoop$Control")
	RegisterValueType(reflect.TypeOf((*java.ForEachLoop)(nil)), "org.openrewrite.java.tree.J$ForEachLoop")
	RegisterValueType(reflect.TypeOf((*java.ForEachControl)(nil)), "org.openrewrite.java.tree.J$ForEachLoop$Control")
	RegisterValueType(reflect.TypeOf((*java.Switch)(nil)), "org.openrewrite.java.tree.J$Switch")
	RegisterValueType(reflect.TypeOf((*java.Case)(nil)), "org.openrewrite.java.tree.J$Case")
	RegisterValueType(reflect.TypeOf((*java.Break)(nil)), "org.openrewrite.java.tree.J$Break")
	RegisterValueType(reflect.TypeOf((*java.Continue)(nil)), "org.openrewrite.java.tree.J$Continue")
	RegisterValueType(reflect.TypeOf((*java.Label)(nil)), "org.openrewrite.java.tree.J$Label")
	RegisterValueType(reflect.TypeOf((*java.Empty)(nil)), "org.openrewrite.java.tree.J$Empty")
	RegisterValueType(reflect.TypeOf((*java.Annotation)(nil)), "org.openrewrite.java.tree.J$Annotation")
	RegisterValueType(reflect.TypeOf((*java.Unary)(nil)), "org.openrewrite.java.tree.J$Unary")
	RegisterValueType(reflect.TypeOf((*java.FieldAccess)(nil)), "org.openrewrite.java.tree.J$FieldAccess")
	RegisterValueType(reflect.TypeOf((*java.MethodInvocation)(nil)), "org.openrewrite.java.tree.J$MethodInvocation")
	RegisterValueType(reflect.TypeOf((*java.VariableDeclarations)(nil)), "org.openrewrite.java.tree.J$VariableDeclarations")
	RegisterValueType(reflect.TypeOf((*java.VariableDeclarator)(nil)), "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable")
	RegisterValueType(reflect.TypeOf((*java.ArrayType)(nil)), "org.openrewrite.java.tree.J$ArrayType")
	RegisterValueType(reflect.TypeOf((*java.ArrayAccess)(nil)), "org.openrewrite.java.tree.J$ArrayAccess")
	RegisterValueType(reflect.TypeOf((*java.ParameterizedType)(nil)), "org.openrewrite.java.tree.J$ParameterizedType")
	RegisterValueType(reflect.TypeOf((*java.TypeParameters)(nil)), "org.openrewrite.java.tree.J$TypeParameters")
	RegisterValueType(reflect.TypeOf((*java.TypeParameter)(nil)), "org.openrewrite.java.tree.J$TypeParameter")
	RegisterValueType(reflect.TypeOf((*java.ArrayDimension)(nil)), "org.openrewrite.java.tree.J$ArrayDimension")
	RegisterValueType(reflect.TypeOf((*java.Parentheses)(nil)), "org.openrewrite.java.tree.J$Parentheses")
	RegisterValueType(reflect.TypeOf((*java.TypeCast)(nil)), "org.openrewrite.java.tree.J$TypeCast")
	RegisterValueType(reflect.TypeOf((*java.ControlParentheses)(nil)), "org.openrewrite.java.tree.J$ControlParentheses")
	RegisterValueType(reflect.TypeOf((*java.Import)(nil)), "org.openrewrite.java.tree.J$Import")

	// ParseError
	RegisterValueType(reflect.TypeOf((*java.ParseError)(nil)), "org.openrewrite.tree.ParseError")

	// Non-tree types that Java needs valueType for
	RegisterValueType(reflect.TypeOf(java.Space{}), "org.openrewrite.java.tree.Space")
	RegisterValueType(reflect.TypeOf(java.Markers{}), "org.openrewrite.marker.Markers")
	RegisterValueType(reflect.TypeOf(java.Comment{}), "org.openrewrite.java.tree.TextComment")

	// Go-specific marker valueType registrations (for send-side type resolution)
	RegisterValueType(reflect.TypeOf(golang.GroupedImport{}), "org.openrewrite.golang.marker.GroupedImport")
	RegisterValueType(reflect.TypeOf(golang.ImportBlock{}), "org.openrewrite.golang.marker.ImportBlock")
	RegisterValueType(reflect.TypeOf(golang.ShortVarDecl{}), "org.openrewrite.golang.marker.ShortVarDecl")
	RegisterValueType(reflect.TypeOf(golang.VarKeyword{}), "org.openrewrite.golang.marker.VarKeyword")
	RegisterValueType(reflect.TypeOf(golang.ConstDecl{}), "org.openrewrite.golang.marker.ConstDecl")
	RegisterValueType(reflect.TypeOf(golang.GroupedSpec{}), "org.openrewrite.golang.marker.GroupedSpec")
	RegisterValueType(reflect.TypeOf(golang.InterfaceMethod{}), "org.openrewrite.golang.marker.InterfaceMethod")
	RegisterValueType(reflect.TypeOf(golang.SelectStmt{}), "org.openrewrite.golang.marker.SelectStmt")
	RegisterValueType(reflect.TypeOf(golang.TypeSwitchGuard{}), "org.openrewrite.golang.marker.TypeSwitchGuard")
	RegisterValueType(reflect.TypeOf(golang.StructTag{}), "org.openrewrite.golang.marker.StructTag")
	RegisterValueType(reflect.TypeOf(golang.TrailingComma{}), "org.openrewrite.golang.marker.TrailingComma")
	RegisterValueType(reflect.TypeOf(java.SearchResult{}), "org.openrewrite.marker.SearchResult")
	RegisterValueType(reflect.TypeOf(java.ParseExceptionResult{}), "org.openrewrite.ParseExceptionResult")
	RegisterValueType(reflect.TypeOf(golang.Semicolon{}), "org.openrewrite.java.marker.Semicolon")
	RegisterValueType(reflect.TypeOf(golang.GoProject{}), "org.openrewrite.golang.marker.GoProject")
	RegisterValueType(reflect.TypeOf(golang.GoResolutionResult{}), "org.openrewrite.golang.marker.GoResolutionResult")
	// Inner-class types of GoResolutionResult; Java emits them via
	// getAndSendListAsRef so each item's wire shape carries a valueType.
	RegisterValueType(reflect.TypeOf(golang.GoRequire{}), "org.openrewrite.golang.marker.GoResolutionResult$Require")
	RegisterValueType(reflect.TypeOf(golang.GoReplace{}), "org.openrewrite.golang.marker.GoResolutionResult$Replace")
	RegisterValueType(reflect.TypeOf(golang.GoExclude{}), "org.openrewrite.golang.marker.GoResolutionResult$Exclude")
	RegisterValueType(reflect.TypeOf(golang.GoRetract{}), "org.openrewrite.golang.marker.GoResolutionResult$Retract")
	RegisterValueType(reflect.TypeOf(golang.GoResolvedDependency{}), "org.openrewrite.golang.marker.GoResolutionResult$ResolvedDependency")
	RegisterValueType(reflect.TypeOf(golang.GoModule{}), "org.openrewrite.golang.marker.GoResolutionResult$GoModule")
	RegisterValueType(reflect.TypeOf(golang.GoModuleEdge{}), "org.openrewrite.golang.marker.GoResolutionResult$GoModuleEdge")

	// JavaType types
	RegisterValueType(reflect.TypeOf((*java.JavaTypeClass)(nil)), "org.openrewrite.java.tree.JavaType$Class")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeShallowClass)(nil)), "org.openrewrite.java.tree.JavaType$ShallowClass")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeParameterized)(nil)), "org.openrewrite.java.tree.JavaType$Parameterized")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeGenericTypeVariable)(nil)), "org.openrewrite.java.tree.JavaType$GenericTypeVariable")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeArray)(nil)), "org.openrewrite.java.tree.JavaType$Array")
	RegisterValueType(reflect.TypeOf((*java.JavaTypePrimitive)(nil)), "org.openrewrite.java.tree.JavaType$Primitive")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeMethod)(nil)), "org.openrewrite.java.tree.JavaType$Method")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeVariable)(nil)), "org.openrewrite.java.tree.JavaType$Variable")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeAnnotation)(nil)), "org.openrewrite.java.tree.JavaType$Annotation")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeMultiCatch)(nil)), "org.openrewrite.java.tree.JavaType$MultiCatch")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeIntersection)(nil)), "org.openrewrite.java.tree.JavaType$Intersection")
	RegisterValueType(reflect.TypeOf((*java.JavaTypeUnknown)(nil)), "org.openrewrite.java.tree.JavaType$Unknown")

	// Register factories for creating instances during deserialization.
	// Each J node gets a fresh random ID at construction time. The receiver
	// overwrites this with the wire ID via PreVisit/WithID, so this value is
	// transient. The point: if anything ever bypasses that override path,
	// the failure mode is a private bogus-but-unique ID rather than uuid.Nil
	// (which would collide across trees and cross-contaminate Java-side caches).
	RegisterFactory("org.openrewrite.golang.tree.Go$CompilationUnit", func() any { return &golang.CompilationUnit{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$GoStatement", func() any { return &golang.GoStmt{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Defer", func() any { return &golang.Defer{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Send", func() any { return &golang.Send{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Goto", func() any { return &golang.Goto{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Unary", func() any { return &golang.Unary{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Binary", func() any { return &golang.Binary{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$AssignmentOperation", func() any { return &golang.AssignmentOperation{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Variadic", func() any { return &golang.Variadic{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Fallthrough", func() any { return &golang.Fallthrough{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Composite", func() any { return &golang.Composite{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$KeyValue", func() any { return &golang.KeyValue{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$SliceExpr", func() any { return &golang.Slice{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$ArrayType", func() any { return &golang.ArrayType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$MapType", func() any { return &golang.MapType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$PointerType", func() any { return &golang.PointerType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Channel", func() any { return &golang.Channel{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$FuncType", func() any { return &golang.FuncType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$StructType", func() any { return &golang.StructType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$InterfaceType", func() any { return &golang.InterfaceType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$TypeList", func() any { return &golang.TypeList{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Union", func() any { return &golang.Union{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$UnderlyingType", func() any { return &golang.UnderlyingType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$TypeDecl", func() any { return &golang.TypeDecl{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$DeclarationBlock", func() any { return &golang.DeclarationBlock{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$MultiAssignment", func() any { return &golang.MultiAssignment{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$Return", func() any { return &golang.Return{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$MethodDeclaration", func() any { return &golang.MethodDeclaration{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$StatementWithInit", func() any { return &golang.StatementWithInit{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$CommClause", func() any { return &golang.CommClause{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$IndexList", func() any { return &golang.IndexList{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.golang.tree.Go$StatementExpression", func() any { return &golang.StatementExpression{ID: uuid.New()} })

	RegisterFactory("org.openrewrite.java.tree.J$Identifier", func() any { return &java.Identifier{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Literal", func() any { return &java.Literal{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Binary", func() any { return &java.Binary{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Block", func() any { return &java.Block{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Return", func() any { return &java.Return{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$If", func() any { return &java.If{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$If$Else", func() any { return &java.Else{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Assignment", func() any { return &java.Assignment{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$AssignmentOperation", func() any { return &java.AssignmentOperation{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$MethodDeclaration", func() any { return &java.MethodDeclaration{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ForLoop", func() any { return &java.ForLoop{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ForLoop$Control", func() any { return &java.ForControl{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ForEachLoop", func() any { return &java.ForEachLoop{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ForEachLoop$Control", func() any { return &java.ForEachControl{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Switch", func() any { return &java.Switch{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Case", func() any { return &java.Case{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Break", func() any { return &java.Break{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Continue", func() any { return &java.Continue{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Label", func() any { return &java.Label{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Empty", func() any { return &java.Empty{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Annotation", func() any { return &java.Annotation{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Unary", func() any { return &java.Unary{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$FieldAccess", func() any { return &java.FieldAccess{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$MethodInvocation", func() any { return &java.MethodInvocation{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$VariableDeclarations", func() any { return &java.VariableDeclarations{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable", func() any { return &java.VariableDeclarator{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ArrayType", func() any { return &java.ArrayType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ArrayAccess", func() any { return &java.ArrayAccess{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ParameterizedType", func() any { return &java.ParameterizedType{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$TypeParameters", func() any { return &java.TypeParameters{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$TypeParameter", func() any { return &java.TypeParameter{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ArrayDimension", func() any { return &java.ArrayDimension{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Parentheses", func() any { return &java.Parentheses{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$TypeCast", func() any { return &java.TypeCast{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$ControlParentheses", func() any { return &java.ControlParentheses{ID: uuid.New()} })
	RegisterFactory("org.openrewrite.java.tree.J$Import", func() any { return &java.Import{ID: uuid.New()} })

	// ParseError
	RegisterFactory("org.openrewrite.tree.ParseError", func() any { return &java.ParseError{Ident: uuid.New()} })
	RegisterFactory("org.openrewrite.ParseExceptionResult", func() any { return java.ParseExceptionResult{} })

	// SourceFile-level types that implement RpcCodec
	RegisterFactory("org.openrewrite.Checksum", func() any { return java.GenericMarker{JavaType: "org.openrewrite.Checksum"} })
	RegisterFactory("org.openrewrite.FileAttributes", func() any { return java.GenericMarker{JavaType: "org.openrewrite.FileAttributes"} })

	// Java-side markers that may appear when recipes modify trees or during LST writing.
	// These markers do NOT implement RpcCodec and are serialized as raw values.
	RegisterFactory("org.openrewrite.marker.RecipesThatMadeChanges", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.RecipesThatMadeChanges"} })
	RegisterFactory("org.openrewrite.marker.LstProvenance", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.LstProvenance"} })
	RegisterFactory("org.openrewrite.marker.BuildMetadata", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.BuildMetadata"} })
	RegisterFactory("org.openrewrite.marker.GitTreeEntry", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.GitTreeEntry"} })
	RegisterFactory("org.openrewrite.marker.BuildTool", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.BuildTool"} })
	RegisterFactory("org.openrewrite.marker.BuildToolFailure", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.BuildToolFailure"} })
	RegisterFactory("org.openrewrite.marker.Generated", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.Generated"} })
	RegisterFactory("org.openrewrite.marker.DeserializationError", func() any { return java.GenericMarker{JavaType: "org.openrewrite.marker.DeserializationError"} })
	// SearchResult: IS an RpcCodec, sends 2 sub-fields (id, description)
	RegisterFactory("org.openrewrite.marker.SearchResult", func() any { return java.SearchResult{} })
	// GroupedImport: IS an RpcCodec, sends 2 sub-fields (id, before whitespace)
	RegisterFactory("org.openrewrite.golang.marker.GroupedImport", func() any { return golang.GroupedImport{} })
	// ImportBlock: IS an RpcCodec, sends 5 sub-fields (id, closePrevious, before, grouped, groupedBefore)
	RegisterFactory("org.openrewrite.golang.marker.ImportBlock", func() any { return golang.ImportBlock{} })
	// Go-specific markers: all are RpcCodec
	RegisterFactory("org.openrewrite.golang.marker.ShortVarDecl", func() any { return golang.ShortVarDecl{} })
	RegisterFactory("org.openrewrite.golang.marker.VarKeyword", func() any { return golang.VarKeyword{} })
	RegisterFactory("org.openrewrite.golang.marker.ConstDecl", func() any { return golang.ConstDecl{} })
	RegisterFactory("org.openrewrite.golang.marker.GroupedSpec", func() any { return golang.GroupedSpec{} })
	RegisterFactory("org.openrewrite.golang.marker.InterfaceMethod", func() any { return golang.InterfaceMethod{} })
	RegisterFactory("org.openrewrite.golang.marker.SelectStmt", func() any { return golang.SelectStmt{} })
	RegisterFactory("org.openrewrite.golang.marker.TypeSwitchGuard", func() any { return golang.TypeSwitchGuard{} })
	RegisterFactory("org.openrewrite.golang.marker.StructTag", func() any { return golang.StructTag{} })
	RegisterFactory("org.openrewrite.golang.marker.TrailingComma", func() any { return golang.TrailingComma{} })
	// Semicolon: RpcCodec on the Java side; sends only `id`. Replaces the
	// previous GenericMarker fallback for the same Java FQN.
	RegisterFactory("org.openrewrite.java.marker.Semicolon", func() any { return golang.Semicolon{} })
	// GoProject + GoResolutionResult are RpcCodec on the Java side; codec
	// dispatch lives in space_rpc.go's sendMarkerCodecFields / receiveMarkersCodec.
	RegisterFactory("org.openrewrite.golang.marker.GoProject", func() any { return golang.GoProject{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult", func() any { return golang.GoResolutionResult{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$Require", func() any { return golang.GoRequire{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$Replace", func() any { return golang.GoReplace{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$Exclude", func() any { return golang.GoExclude{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$Retract", func() any { return golang.GoRetract{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$ResolvedDependency", func() any { return golang.GoResolvedDependency{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$GoModule", func() any { return golang.GoModule{} })
	RegisterFactory("org.openrewrite.golang.marker.GoResolutionResult$GoModuleEdge", func() any { return golang.GoModuleEdge{} })

	RegisterFactory("org.openrewrite.java.tree.Space", func() any { return java.Space{} })
	RegisterFactory("org.openrewrite.marker.Markers", func() any { return java.Markers{} })
	RegisterFactory("org.openrewrite.java.tree.TextComment", func() any { return java.Comment{} })

	// Padding types — needed when Java sends ADD messages for new padding
	// wrappers during bidirectional tree transfer (e.g., after a recipe
	// recreates a statement list element). Uses RightPadded[java.J] etc.
	// as the broadest type that the padding accessors can handle.
	RegisterFactory("org.openrewrite.java.tree.JRightPadded", func() any { return java.RightPadded[java.J]{} })
	RegisterFactory("org.openrewrite.java.tree.JLeftPadded", func() any { return java.LeftPadded[java.J]{} })
	RegisterFactory("org.openrewrite.java.tree.JContainer", func() any { return java.Container[java.J]{} })

	RegisterFactory("org.openrewrite.java.tree.JavaType$Class", func() any { return &java.JavaTypeClass{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$ShallowClass", func() any { return &java.JavaTypeShallowClass{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Parameterized", func() any { return &java.JavaTypeParameterized{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$GenericTypeVariable", func() any { return &java.JavaTypeGenericTypeVariable{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Array", func() any { return &java.JavaTypeArray{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Primitive", func() any { return &java.JavaTypePrimitive{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Method", func() any { return &java.JavaTypeMethod{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Variable", func() any { return &java.JavaTypeVariable{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Annotation", func() any { return &java.JavaTypeAnnotation{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$MultiCatch", func() any { return &java.JavaTypeMultiCatch{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Intersection", func() any { return &java.JavaTypeIntersection{} })
	RegisterFactory("org.openrewrite.java.tree.JavaType$Unknown", func() any { return &java.JavaTypeUnknown{} })
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
