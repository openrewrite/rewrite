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

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GraphQLParser}.
 */
public interface GraphQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#document}.
	 * @param ctx the parse tree
	 */
	void enterDocument(GraphQLParser.DocumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#document}.
	 * @param ctx the parse tree
	 */
	void exitDocument(GraphQLParser.DocumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#definition}.
	 * @param ctx the parse tree
	 */
	void enterDefinition(GraphQLParser.DefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#definition}.
	 * @param ctx the parse tree
	 */
	void exitDefinition(GraphQLParser.DefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#executableDefinition}.
	 * @param ctx the parse tree
	 */
	void enterExecutableDefinition(GraphQLParser.ExecutableDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#executableDefinition}.
	 * @param ctx the parse tree
	 */
	void exitExecutableDefinition(GraphQLParser.ExecutableDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#operationDefinition}.
	 * @param ctx the parse tree
	 */
	void enterOperationDefinition(GraphQLParser.OperationDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#operationDefinition}.
	 * @param ctx the parse tree
	 */
	void exitOperationDefinition(GraphQLParser.OperationDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#operationType}.
	 * @param ctx the parse tree
	 */
	void enterOperationType(GraphQLParser.OperationTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#operationType}.
	 * @param ctx the parse tree
	 */
	void exitOperationType(GraphQLParser.OperationTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#selectionSet}.
	 * @param ctx the parse tree
	 */
	void enterSelectionSet(GraphQLParser.SelectionSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#selectionSet}.
	 * @param ctx the parse tree
	 */
	void exitSelectionSet(GraphQLParser.SelectionSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#selection}.
	 * @param ctx the parse tree
	 */
	void enterSelection(GraphQLParser.SelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#selection}.
	 * @param ctx the parse tree
	 */
	void exitSelection(GraphQLParser.SelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(GraphQLParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(GraphQLParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#alias}.
	 * @param ctx the parse tree
	 */
	void enterAlias(GraphQLParser.AliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#alias}.
	 * @param ctx the parse tree
	 */
	void exitAlias(GraphQLParser.AliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(GraphQLParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(GraphQLParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(GraphQLParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(GraphQLParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fragmentSpread}.
	 * @param ctx the parse tree
	 */
	void enterFragmentSpread(GraphQLParser.FragmentSpreadContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fragmentSpread}.
	 * @param ctx the parse tree
	 */
	void exitFragmentSpread(GraphQLParser.FragmentSpreadContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inlineFragment}.
	 * @param ctx the parse tree
	 */
	void enterInlineFragment(GraphQLParser.InlineFragmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inlineFragment}.
	 * @param ctx the parse tree
	 */
	void exitInlineFragment(GraphQLParser.InlineFragmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFragmentDefinition(GraphQLParser.FragmentDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFragmentDefinition(GraphQLParser.FragmentDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void enterFragmentName(GraphQLParser.FragmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void exitFragmentName(GraphQLParser.FragmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeCondition}.
	 * @param ctx the parse tree
	 */
	void enterTypeCondition(GraphQLParser.TypeConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeCondition}.
	 * @param ctx the parse tree
	 */
	void exitTypeCondition(GraphQLParser.TypeConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(GraphQLParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(GraphQLParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(GraphQLParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(GraphQLParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#variableDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterVariableDefinitions(GraphQLParser.VariableDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#variableDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitVariableDefinitions(GraphQLParser.VariableDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#variableDefinition}.
	 * @param ctx the parse tree
	 */
	void enterVariableDefinition(GraphQLParser.VariableDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#variableDefinition}.
	 * @param ctx the parse tree
	 */
	void exitVariableDefinition(GraphQLParser.VariableDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(GraphQLParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(GraphQLParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(GraphQLParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(GraphQLParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#namedType}.
	 * @param ctx the parse tree
	 */
	void enterNamedType(GraphQLParser.NamedTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#namedType}.
	 * @param ctx the parse tree
	 */
	void exitNamedType(GraphQLParser.NamedTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#listType}.
	 * @param ctx the parse tree
	 */
	void enterListType(GraphQLParser.ListTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#listType}.
	 * @param ctx the parse tree
	 */
	void exitListType(GraphQLParser.ListTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void enterNonNullType(GraphQLParser.NonNullTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void exitNonNullType(GraphQLParser.NonNullTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directives}.
	 * @param ctx the parse tree
	 */
	void enterDirectives(GraphQLParser.DirectivesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directives}.
	 * @param ctx the parse tree
	 */
	void exitDirectives(GraphQLParser.DirectivesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterDirective(GraphQLParser.DirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitDirective(GraphQLParser.DirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeSystemDefinition}.
	 * @param ctx the parse tree
	 */
	void enterTypeSystemDefinition(GraphQLParser.TypeSystemDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeSystemDefinition}.
	 * @param ctx the parse tree
	 */
	void exitTypeSystemDefinition(GraphQLParser.TypeSystemDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeSystemExtension}.
	 * @param ctx the parse tree
	 */
	void enterTypeSystemExtension(GraphQLParser.TypeSystemExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeSystemExtension}.
	 * @param ctx the parse tree
	 */
	void exitTypeSystemExtension(GraphQLParser.TypeSystemExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#schemaDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSchemaDefinition(GraphQLParser.SchemaDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#schemaDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSchemaDefinition(GraphQLParser.SchemaDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#rootOperationTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterRootOperationTypeDefinition(GraphQLParser.RootOperationTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#rootOperationTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitRootOperationTypeDefinition(GraphQLParser.RootOperationTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#schemaExtension}.
	 * @param ctx the parse tree
	 */
	void enterSchemaExtension(GraphQLParser.SchemaExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#schemaExtension}.
	 * @param ctx the parse tree
	 */
	void exitSchemaExtension(GraphQLParser.SchemaExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterTypeDefinition(GraphQLParser.TypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitTypeDefinition(GraphQLParser.TypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeExtension}.
	 * @param ctx the parse tree
	 */
	void enterTypeExtension(GraphQLParser.TypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeExtension}.
	 * @param ctx the parse tree
	 */
	void exitTypeExtension(GraphQLParser.TypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#scalarTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterScalarTypeDefinition(GraphQLParser.ScalarTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#scalarTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitScalarTypeDefinition(GraphQLParser.ScalarTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#scalarTypeExtension}.
	 * @param ctx the parse tree
	 */
	void enterScalarTypeExtension(GraphQLParser.ScalarTypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#scalarTypeExtension}.
	 * @param ctx the parse tree
	 */
	void exitScalarTypeExtension(GraphQLParser.ScalarTypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterObjectTypeDefinition(GraphQLParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitObjectTypeDefinition(GraphQLParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#objectTypeExtension}.
	 * @param ctx the parse tree
	 */
	void enterObjectTypeExtension(GraphQLParser.ObjectTypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#objectTypeExtension}.
	 * @param ctx the parse tree
	 */
	void exitObjectTypeExtension(GraphQLParser.ObjectTypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#implementsInterfaces}.
	 * @param ctx the parse tree
	 */
	void enterImplementsInterfaces(GraphQLParser.ImplementsInterfacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#implementsInterfaces}.
	 * @param ctx the parse tree
	 */
	void exitImplementsInterfaces(GraphQLParser.ImplementsInterfacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFieldsDefinition(GraphQLParser.FieldsDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFieldsDefinition(GraphQLParser.FieldsDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fieldDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFieldDefinition(GraphQLParser.FieldDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fieldDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFieldDefinition(GraphQLParser.FieldDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#argumentsDefinition}.
	 * @param ctx the parse tree
	 */
	void enterArgumentsDefinition(GraphQLParser.ArgumentsDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#argumentsDefinition}.
	 * @param ctx the parse tree
	 */
	void exitArgumentsDefinition(GraphQLParser.ArgumentsDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inputValueDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInputValueDefinition(GraphQLParser.InputValueDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inputValueDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInputValueDefinition(GraphQLParser.InputValueDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#interfaceTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceTypeDefinition(GraphQLParser.InterfaceTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#interfaceTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceTypeDefinition(GraphQLParser.InterfaceTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#interfaceTypeExtension}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceTypeExtension(GraphQLParser.InterfaceTypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#interfaceTypeExtension}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceTypeExtension(GraphQLParser.InterfaceTypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#unionTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterUnionTypeDefinition(GraphQLParser.UnionTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#unionTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitUnionTypeDefinition(GraphQLParser.UnionTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#unionMemberTypes}.
	 * @param ctx the parse tree
	 */
	void enterUnionMemberTypes(GraphQLParser.UnionMemberTypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#unionMemberTypes}.
	 * @param ctx the parse tree
	 */
	void exitUnionMemberTypes(GraphQLParser.UnionMemberTypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#unionTypeExtension}.
	 * @param ctx the parse tree
	 */
	void enterUnionTypeExtension(GraphQLParser.UnionTypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#unionTypeExtension}.
	 * @param ctx the parse tree
	 */
	void exitUnionTypeExtension(GraphQLParser.UnionTypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumTypeDefinition(GraphQLParser.EnumTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumTypeDefinition(GraphQLParser.EnumTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#enumValuesDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumValuesDefinition(GraphQLParser.EnumValuesDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#enumValuesDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumValuesDefinition(GraphQLParser.EnumValuesDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#enumValueDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumValueDefinition(GraphQLParser.EnumValueDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#enumValueDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumValueDefinition(GraphQLParser.EnumValueDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#enumTypeExtension}.
	 * @param ctx the parse tree
	 */
	void enterEnumTypeExtension(GraphQLParser.EnumTypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#enumTypeExtension}.
	 * @param ctx the parse tree
	 */
	void exitEnumTypeExtension(GraphQLParser.EnumTypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inputObjectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInputObjectTypeDefinition(GraphQLParser.InputObjectTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inputObjectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInputObjectTypeDefinition(GraphQLParser.InputObjectTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inputFieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInputFieldsDefinition(GraphQLParser.InputFieldsDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inputFieldsDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInputFieldsDefinition(GraphQLParser.InputFieldsDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inputObjectTypeExtension}.
	 * @param ctx the parse tree
	 */
	void enterInputObjectTypeExtension(GraphQLParser.InputObjectTypeExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inputObjectTypeExtension}.
	 * @param ctx the parse tree
	 */
	void exitInputObjectTypeExtension(GraphQLParser.InputObjectTypeExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directiveDefinition}.
	 * @param ctx the parse tree
	 */
	void enterDirectiveDefinition(GraphQLParser.DirectiveDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directiveDefinition}.
	 * @param ctx the parse tree
	 */
	void exitDirectiveDefinition(GraphQLParser.DirectiveDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directiveLocations}.
	 * @param ctx the parse tree
	 */
	void enterDirectiveLocations(GraphQLParser.DirectiveLocationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directiveLocations}.
	 * @param ctx the parse tree
	 */
	void exitDirectiveLocations(GraphQLParser.DirectiveLocationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directiveLocation}.
	 * @param ctx the parse tree
	 */
	void enterDirectiveLocation(GraphQLParser.DirectiveLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directiveLocation}.
	 * @param ctx the parse tree
	 */
	void exitDirectiveLocation(GraphQLParser.DirectiveLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#executableDirectiveLocation}.
	 * @param ctx the parse tree
	 */
	void enterExecutableDirectiveLocation(GraphQLParser.ExecutableDirectiveLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#executableDirectiveLocation}.
	 * @param ctx the parse tree
	 */
	void exitExecutableDirectiveLocation(GraphQLParser.ExecutableDirectiveLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeSystemDirectiveLocation}.
	 * @param ctx the parse tree
	 */
	void enterTypeSystemDirectiveLocation(GraphQLParser.TypeSystemDirectiveLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeSystemDirectiveLocation}.
	 * @param ctx the parse tree
	 */
	void exitTypeSystemDirectiveLocation(GraphQLParser.TypeSystemDirectiveLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(GraphQLParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(GraphQLParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#description}.
	 * @param ctx the parse tree
	 */
	void enterDescription(GraphQLParser.DescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#description}.
	 * @param ctx the parse tree
	 */
	void exitDescription(GraphQLParser.DescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(GraphQLParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(GraphQLParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#nullValue}.
	 * @param ctx the parse tree
	 */
	void enterNullValue(GraphQLParser.NullValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#nullValue}.
	 * @param ctx the parse tree
	 */
	void exitNullValue(GraphQLParser.NullValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void enterEnumValue(GraphQLParser.EnumValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#enumValue}.
	 * @param ctx the parse tree
	 */
	void exitEnumValue(GraphQLParser.EnumValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#listValue}.
	 * @param ctx the parse tree
	 */
	void enterListValue(GraphQLParser.ListValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#listValue}.
	 * @param ctx the parse tree
	 */
	void exitListValue(GraphQLParser.ListValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void enterObjectValue(GraphQLParser.ObjectValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#objectValue}.
	 * @param ctx the parse tree
	 */
	void exitObjectValue(GraphQLParser.ObjectValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#objectField}.
	 * @param ctx the parse tree
	 */
	void enterObjectField(GraphQLParser.ObjectFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#objectField}.
	 * @param ctx the parse tree
	 */
	void exitObjectField(GraphQLParser.ObjectFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#intValue}.
	 * @param ctx the parse tree
	 */
	void enterIntValue(GraphQLParser.IntValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#intValue}.
	 * @param ctx the parse tree
	 */
	void exitIntValue(GraphQLParser.IntValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#floatValue}.
	 * @param ctx the parse tree
	 */
	void enterFloatValue(GraphQLParser.FloatValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#floatValue}.
	 * @param ctx the parse tree
	 */
	void exitFloatValue(GraphQLParser.FloatValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void enterStringValue(GraphQLParser.StringValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#stringValue}.
	 * @param ctx the parse tree
	 */
	void exitStringValue(GraphQLParser.StringValueContext ctx);
}