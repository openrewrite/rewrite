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
package org.openrewrite.graphql;

import org.openrewrite.graphql.tree.GraphQl;

public class GraphQlIsoVisitor<P> extends GraphQlVisitor<P> {
    @Override
    public GraphQl.Document visitDocument(GraphQl.Document document, P p) {
        return (GraphQl.Document) super.visitDocument(document, p);
    }

    @Override
    public GraphQl.OperationDefinition visitOperationDefinition(GraphQl.OperationDefinition operationDef, P p) {
        return (GraphQl.OperationDefinition) super.visitOperationDefinition(operationDef, p);
    }

    @Override
    public GraphQl.SelectionSet visitSelectionSet(GraphQl.SelectionSet selectionSet, P p) {
        return (GraphQl.SelectionSet) super.visitSelectionSet(selectionSet, p);
    }

    @Override
    public GraphQl.Field visitField(GraphQl.Field field, P p) {
        return (GraphQl.Field) super.visitField(field, p);
    }

    @Override
    public GraphQl.Name visitName(GraphQl.Name name, P p) {
        return (GraphQl.Name) super.visitName(name, p);
    }

    @Override
    public GraphQl.Arguments visitArguments(GraphQl.Arguments arguments, P p) {
        return (GraphQl.Arguments) super.visitArguments(arguments, p);
    }

    @Override
    public GraphQl.Argument visitArgument(GraphQl.Argument argument, P p) {
        return (GraphQl.Argument) super.visitArgument(argument, p);
    }

    @Override
    public GraphQl.StringValue visitStringValue(GraphQl.StringValue stringValue, P p) {
        return (GraphQl.StringValue) super.visitStringValue(stringValue, p);
    }

    @Override
    public GraphQl.IntValue visitIntValue(GraphQl.IntValue intValue, P p) {
        return (GraphQl.IntValue) super.visitIntValue(intValue, p);
    }

    @Override
    public GraphQl.FloatValue visitFloatValue(GraphQl.FloatValue floatValue, P p) {
        return (GraphQl.FloatValue) super.visitFloatValue(floatValue, p);
    }

    @Override
    public GraphQl.BooleanValue visitBooleanValue(GraphQl.BooleanValue booleanValue, P p) {
        return (GraphQl.BooleanValue) super.visitBooleanValue(booleanValue, p);
    }

    @Override
    public GraphQl.NullValue visitNullValue(GraphQl.NullValue nullValue, P p) {
        return (GraphQl.NullValue) super.visitNullValue(nullValue, p);
    }

    @Override
    public GraphQl.EnumValue visitEnumValue(GraphQl.EnumValue enumValue, P p) {
        return (GraphQl.EnumValue) super.visitEnumValue(enumValue, p);
    }

    @Override
    public GraphQl.ListValue visitListValue(GraphQl.ListValue listValue, P p) {
        return (GraphQl.ListValue) super.visitListValue(listValue, p);
    }

    @Override
    public GraphQl.ObjectValue visitObjectValue(GraphQl.ObjectValue objectValue, P p) {
        return (GraphQl.ObjectValue) super.visitObjectValue(objectValue, p);
    }

    @Override
    public GraphQl.ObjectField visitObjectField(GraphQl.ObjectField objectField, P p) {
        return (GraphQl.ObjectField) super.visitObjectField(objectField, p);
    }

    @Override
    public GraphQl.Variable visitVariable(GraphQl.Variable variable, P p) {
        return (GraphQl.Variable) super.visitVariable(variable, p);
    }

    @Override
    public GraphQl.VariableDefinition visitVariableDefinition(GraphQl.VariableDefinition variableDef, P p) {
        return (GraphQl.VariableDefinition) super.visitVariableDefinition(variableDef, p);
    }

    @Override
    public GraphQl.NamedType visitNamedType(GraphQl.NamedType namedType, P p) {
        return (GraphQl.NamedType) super.visitNamedType(namedType, p);
    }

    @Override
    public GraphQl.ListType visitListType(GraphQl.ListType listType, P p) {
        return (GraphQl.ListType) super.visitListType(listType, p);
    }

    @Override
    public GraphQl.NonNullType visitNonNullType(GraphQl.NonNullType nonNullType, P p) {
        return (GraphQl.NonNullType) super.visitNonNullType(nonNullType, p);
    }

    @Override
    public GraphQl.Directive visitDirective(GraphQl.Directive directive, P p) {
        return (GraphQl.Directive) super.visitDirective(directive, p);
    }

    @Override
    public GraphQl.FragmentDefinition visitFragmentDefinition(GraphQl.FragmentDefinition fragmentDef, P p) {
        return (GraphQl.FragmentDefinition) super.visitFragmentDefinition(fragmentDef, p);
    }

    @Override
    public GraphQl.FragmentSpread visitFragmentSpread(GraphQl.FragmentSpread fragmentSpread, P p) {
        return (GraphQl.FragmentSpread) super.visitFragmentSpread(fragmentSpread, p);
    }

    @Override
    public GraphQl.InlineFragment visitInlineFragment(GraphQl.InlineFragment inlineFragment, P p) {
        return (GraphQl.InlineFragment) super.visitInlineFragment(inlineFragment, p);
    }

    @Override
    public GraphQl.SchemaDefinition visitSchemaDefinition(GraphQl.SchemaDefinition schemaDef, P p) {
        return (GraphQl.SchemaDefinition) super.visitSchemaDefinition(schemaDef, p);
    }

    @Override
    public GraphQl.RootOperationTypeDefinition visitRootOperationTypeDefinition(GraphQl.RootOperationTypeDefinition rootOpType, P p) {
        return (GraphQl.RootOperationTypeDefinition) super.visitRootOperationTypeDefinition(rootOpType, p);
    }

    @Override
    public GraphQl.ScalarTypeDefinition visitScalarTypeDefinition(GraphQl.ScalarTypeDefinition scalarType, P p) {
        return (GraphQl.ScalarTypeDefinition) super.visitScalarTypeDefinition(scalarType, p);
    }

    @Override
    public GraphQl.ObjectTypeDefinition visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectType, P p) {
        return (GraphQl.ObjectTypeDefinition) super.visitObjectTypeDefinition(objectType, p);
    }

    @Override
    public GraphQl.FieldDefinition visitFieldDefinition(GraphQl.FieldDefinition fieldDef, P p) {
        return (GraphQl.FieldDefinition) super.visitFieldDefinition(fieldDef, p);
    }

    @Override
    public GraphQl.InputValueDefinition visitInputValueDefinition(GraphQl.InputValueDefinition inputValueDef, P p) {
        return (GraphQl.InputValueDefinition) super.visitInputValueDefinition(inputValueDef, p);
    }

    @Override
    public GraphQl.InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceType, P p) {
        return (GraphQl.InterfaceTypeDefinition) super.visitInterfaceTypeDefinition(interfaceType, p);
    }

    @Override
    public GraphQl.UnionTypeDefinition visitUnionTypeDefinition(GraphQl.UnionTypeDefinition unionType, P p) {
        return (GraphQl.UnionTypeDefinition) super.visitUnionTypeDefinition(unionType, p);
    }

    @Override
    public GraphQl.EnumTypeDefinition visitEnumTypeDefinition(GraphQl.EnumTypeDefinition enumType, P p) {
        return (GraphQl.EnumTypeDefinition) super.visitEnumTypeDefinition(enumType, p);
    }

    @Override
    public GraphQl.EnumValueDefinition visitEnumValueDefinition(GraphQl.EnumValueDefinition enumValueDef, P p) {
        return (GraphQl.EnumValueDefinition) super.visitEnumValueDefinition(enumValueDef, p);
    }

    @Override
    public GraphQl.InputObjectTypeDefinition visitInputObjectTypeDefinition(GraphQl.InputObjectTypeDefinition inputObjectType, P p) {
        return (GraphQl.InputObjectTypeDefinition) super.visitInputObjectTypeDefinition(inputObjectType, p);
    }

    @Override
    public GraphQl.DirectiveDefinition visitDirectiveDefinition(GraphQl.DirectiveDefinition directiveDef, P p) {
        return (GraphQl.DirectiveDefinition) super.visitDirectiveDefinition(directiveDef, p);
    }
    
    @Override
    public GraphQl.SchemaExtension visitSchemaExtension(GraphQl.SchemaExtension schemaExt, P p) {
        return (GraphQl.SchemaExtension) super.visitSchemaExtension(schemaExt, p);
    }
    
    @Override
    public GraphQl.ScalarTypeExtension visitScalarTypeExtension(GraphQl.ScalarTypeExtension scalarExt, P p) {
        return (GraphQl.ScalarTypeExtension) super.visitScalarTypeExtension(scalarExt, p);
    }
    
    @Override
    public GraphQl.ObjectTypeExtension visitObjectTypeExtension(GraphQl.ObjectTypeExtension objectExt, P p) {
        return (GraphQl.ObjectTypeExtension) super.visitObjectTypeExtension(objectExt, p);
    }
    
    @Override
    public GraphQl.InterfaceTypeExtension visitInterfaceTypeExtension(GraphQl.InterfaceTypeExtension interfaceExt, P p) {
        return (GraphQl.InterfaceTypeExtension) super.visitInterfaceTypeExtension(interfaceExt, p);
    }
    
    @Override
    public GraphQl.UnionTypeExtension visitUnionTypeExtension(GraphQl.UnionTypeExtension unionExt, P p) {
        return (GraphQl.UnionTypeExtension) super.visitUnionTypeExtension(unionExt, p);
    }
    
    @Override
    public GraphQl.EnumTypeExtension visitEnumTypeExtension(GraphQl.EnumTypeExtension enumExt, P p) {
        return (GraphQl.EnumTypeExtension) super.visitEnumTypeExtension(enumExt, p);
    }
    
    @Override
    public GraphQl.InputObjectTypeExtension visitInputObjectTypeExtension(GraphQl.InputObjectTypeExtension inputExt, P p) {
        return (GraphQl.InputObjectTypeExtension) super.visitInputObjectTypeExtension(inputExt, p);
    }
}