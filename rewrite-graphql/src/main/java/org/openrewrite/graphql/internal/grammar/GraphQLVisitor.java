/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated from /Users/kylescully/Repos/rewrite/rewrite-graphql/src/main/antlr/GraphQL.g4 by ANTLR 4.13.2
package org.openrewrite.graphql.internal.grammar;

import java.util.*;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GraphQLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GraphQLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#document}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocument(GraphQLParser.DocumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefinition(GraphQLParser.DefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#executableDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecutableDefinition(GraphQLParser.ExecutableDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#operationDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationDefinition(GraphQLParser.OperationDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#operationType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationType(GraphQLParser.OperationTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#selectionSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectionSet(GraphQLParser.SelectionSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection(GraphQLParser.SelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(GraphQLParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlias(GraphQLParser.AliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(GraphQLParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(GraphQLParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#fragmentSpread}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentSpread(GraphQLParser.FragmentSpreadContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#inlineFragment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineFragment(GraphQLParser.InlineFragmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentDefinition(GraphQLParser.FragmentDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#fragmentName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentName(GraphQLParser.FragmentNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#typeCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeCondition(GraphQLParser.TypeConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(GraphQLParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(GraphQLParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#variableDefinitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDefinitions(GraphQLParser.VariableDefinitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#variableDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDefinition(GraphQLParser.VariableDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(GraphQLParser.DefaultValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(GraphQLParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#namedType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedType(GraphQLParser.NamedTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#listType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListType(GraphQLParser.ListTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#nonNullType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonNullType(GraphQLParser.NonNullTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectives(GraphQLParser.DirectivesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirective(GraphQLParser.DirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#typeSystemDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSystemDefinition(GraphQLParser.TypeSystemDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#typeSystemExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSystemExtension(GraphQLParser.TypeSystemExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#schemaDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchemaDefinition(GraphQLParser.SchemaDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#rootOperationTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRootOperationTypeDefinition(GraphQLParser.RootOperationTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#schemaExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchemaExtension(GraphQLParser.SchemaExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#typeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDefinition(GraphQLParser.TypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#typeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeExtension(GraphQLParser.TypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#scalarTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarTypeDefinition(GraphQLParser.ScalarTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#scalarTypeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarTypeExtension(GraphQLParser.ScalarTypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectTypeDefinition(GraphQLParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#objectTypeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectTypeExtension(GraphQLParser.ObjectTypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#implementsInterfaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImplementsInterfaces(GraphQLParser.ImplementsInterfacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#fieldsDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldsDefinition(GraphQLParser.FieldsDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#fieldDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDefinition(GraphQLParser.FieldDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#argumentsDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentsDefinition(GraphQLParser.ArgumentsDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#inputValueDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputValueDefinition(GraphQLParser.InputValueDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#interfaceTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceTypeDefinition(GraphQLParser.InterfaceTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#interfaceTypeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceTypeExtension(GraphQLParser.InterfaceTypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#unionTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionTypeDefinition(GraphQLParser.UnionTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#unionMemberTypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionMemberTypes(GraphQLParser.UnionMemberTypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#unionTypeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionTypeExtension(GraphQLParser.UnionTypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumTypeDefinition(GraphQLParser.EnumTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#enumValuesDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValuesDefinition(GraphQLParser.EnumValuesDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#enumValueDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueDefinition(GraphQLParser.EnumValueDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#enumTypeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumTypeExtension(GraphQLParser.EnumTypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#inputObjectTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputObjectTypeDefinition(GraphQLParser.InputObjectTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#inputFieldsDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputFieldsDefinition(GraphQLParser.InputFieldsDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#inputObjectTypeExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputObjectTypeExtension(GraphQLParser.InputObjectTypeExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#directiveDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectiveDefinition(GraphQLParser.DirectiveDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#directiveLocations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectiveLocations(GraphQLParser.DirectiveLocationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#directiveLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectiveLocation(GraphQLParser.DirectiveLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#executableDirectiveLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecutableDirectiveLocation(GraphQLParser.ExecutableDirectiveLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#typeSystemDirectiveLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSystemDirectiveLocation(GraphQLParser.TypeSystemDirectiveLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(GraphQLParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescription(GraphQLParser.DescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(GraphQLParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#nullValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullValue(GraphQLParser.NullValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#enumValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValue(GraphQLParser.EnumValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#listValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListValue(GraphQLParser.ListValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#objectValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValue(GraphQLParser.ObjectValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#objectField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectField(GraphQLParser.ObjectFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#intValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntValue(GraphQLParser.IntValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#floatValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatValue(GraphQLParser.FloatValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphQLParser#stringValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringValue(GraphQLParser.StringValueContext ctx);
}