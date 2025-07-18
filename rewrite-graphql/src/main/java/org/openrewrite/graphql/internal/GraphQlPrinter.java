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
package org.openrewrite.graphql.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.graphql.tree.GraphQlRightPadded;
import org.openrewrite.Tree;
import org.openrewrite.graphql.GraphQlVisitor;
import org.openrewrite.graphql.tree.Comment;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.graphql.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

public class GraphQlPrinter<P> extends GraphQlVisitor<PrintOutputCapture<P>> {

    private static final UnaryOperator<String> GRAPHQL_MARKER_WRAPPER =
            out -> "~~>" + out;

    private void printDescription(GraphQl.@Nullable StringValue description, PrintOutputCapture<P> p) {
        if (description != null) {
            visitStringValue(description, p);
            // Don't add a space - the next element's prefix will handle spacing
        }
    }

    @Override
    public GraphQl visitDocument(GraphQl.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        // Don't use visit() as it triggers base class traversal causing double printing
        for (GraphQl.Definition definition : document.getDefinitions()) {
            if (definition instanceof GraphQl.OperationDefinition) {
                visitOperationDefinition((GraphQl.OperationDefinition) definition, p);
            } else if (definition instanceof GraphQl.FragmentDefinition) {
                visitFragmentDefinition((GraphQl.FragmentDefinition) definition, p);
            } else if (definition instanceof GraphQl.SchemaDefinition) {
                visitSchemaDefinition((GraphQl.SchemaDefinition) definition, p);
            } else if (definition instanceof GraphQl.ScalarTypeDefinition) {
                visitScalarTypeDefinition((GraphQl.ScalarTypeDefinition) definition, p);
            } else if (definition instanceof GraphQl.ObjectTypeDefinition) {
                visitObjectTypeDefinition((GraphQl.ObjectTypeDefinition) definition, p);
            } else if (definition instanceof GraphQl.InterfaceTypeDefinition) {
                visitInterfaceTypeDefinition((GraphQl.InterfaceTypeDefinition) definition, p);
            } else if (definition instanceof GraphQl.UnionTypeDefinition) {
                visitUnionTypeDefinition((GraphQl.UnionTypeDefinition) definition, p);
            } else if (definition instanceof GraphQl.EnumTypeDefinition) {
                visitEnumTypeDefinition((GraphQl.EnumTypeDefinition) definition, p);
            } else if (definition instanceof GraphQl.InputObjectTypeDefinition) {
                visitInputObjectTypeDefinition((GraphQl.InputObjectTypeDefinition) definition, p);
            } else if (definition instanceof GraphQl.DirectiveDefinition) {
                visitDirectiveDefinition((GraphQl.DirectiveDefinition) definition, p);
            } else if (definition instanceof GraphQl.SchemaExtension) {
                visitSchemaExtension((GraphQl.SchemaExtension) definition, p);
            } else if (definition instanceof GraphQl.ScalarTypeExtension) {
                visitScalarTypeExtension((GraphQl.ScalarTypeExtension) definition, p);
            } else if (definition instanceof GraphQl.ObjectTypeExtension) {
                visitObjectTypeExtension((GraphQl.ObjectTypeExtension) definition, p);
            } else if (definition instanceof GraphQl.InterfaceTypeExtension) {
                visitInterfaceTypeExtension((GraphQl.InterfaceTypeExtension) definition, p);
            } else if (definition instanceof GraphQl.UnionTypeExtension) {
                visitUnionTypeExtension((GraphQl.UnionTypeExtension) definition, p);
            } else if (definition instanceof GraphQl.EnumTypeExtension) {
                visitEnumTypeExtension((GraphQl.EnumTypeExtension) definition, p);
            } else if (definition instanceof GraphQl.InputObjectTypeExtension) {
                visitInputObjectTypeExtension((GraphQl.InputObjectTypeExtension) definition, p);
            }
        }
        printSpace(document.getEof(), p);
        afterSyntax(document, p);
        return document;
    }

    @Override
    public GraphQl visitOperationDefinition(GraphQl.OperationDefinition operationDef, PrintOutputCapture<P> p) {
        beforeSyntax(operationDef, p);
        if (operationDef.getOperationType() != null) {
            p.append(operationDef.getOperationType().name().toLowerCase());
            if (operationDef.getName() != null) {
                // Don't append space - the name will have its own prefix
                visitName(operationDef.getName(), p);
            }
        }
        visitVariableDefinitions(operationDef.getVariableDefinitions(), operationDef.getVariableDefinitionsEnd(), p);
        visitList(operationDef.getDirectives(), p);
        if (operationDef.getSelectionSet() != null) {
            visitSelectionSet(operationDef.getSelectionSet(), p);
        }
        afterSyntax(operationDef, p);
        return operationDef;
    }

    @Override
    public GraphQl visitSelectionSet(GraphQl.SelectionSet selectionSet, PrintOutputCapture<P> p) {
        beforeSyntax(selectionSet, p);
        p.append("{");
        // Don't use visit() as it triggers base class traversal
        for (GraphQl.Selection selection : selectionSet.getSelections()) {
            if (selection instanceof GraphQl.Field) {
                visitField((GraphQl.Field) selection, p);
            } else if (selection instanceof GraphQl.FragmentSpread) {
                visitFragmentSpread((GraphQl.FragmentSpread) selection, p);
            } else if (selection instanceof GraphQl.InlineFragment) {
                visitInlineFragment((GraphQl.InlineFragment) selection, p);
            }
        }
        printSpace(selectionSet.getEnd(), p);
        p.append("}");
        afterSyntax(selectionSet, p);
        return selectionSet;
    }

    @Override
    public GraphQl visitField(GraphQl.Field field, PrintOutputCapture<P> p) {
        beforeSyntax(field, p);
        if (field.getAlias() != null) {
            visitName(field.getAlias(), p);
            p.append(":");
        }
        visitName(field.getName(), p);
        
        if (field.getArguments() != null) {
            visitArguments(field.getArguments(), p);
        }
        
        visitList(field.getDirectives(), p);
        if (field.getSelectionSet() != null) {
            visitSelectionSet(field.getSelectionSet(), p);
        }
        afterSyntax(field, p);
        return field;
    }

    @Override
    public GraphQl visitName(GraphQl.Name name, PrintOutputCapture<P> p) {
        beforeSyntax(name, p);
        p.append(name.getValue());
        afterSyntax(name, p);
        return name;
    }

    @Override
    public GraphQl visitArguments(GraphQl.Arguments arguments, PrintOutputCapture<P> p) {
        beforeSyntax(arguments, p);
        p.append("(");
        List<GraphQlRightPadded<GraphQl.Argument>> paddedArgs = arguments.getPadding().getArguments();
        if (paddedArgs != null) {
            for (GraphQlRightPadded<GraphQl.Argument> paddedArg : paddedArgs) {
                visitArgument(paddedArg.getElement(), p);
                // Print the after space which contains optional comma and whitespace
                printSpace(paddedArg.getAfter(), p);
            }
        }
        printSpace(arguments.getEnd(), p);
        p.append(")");
        afterSyntax(arguments, p);
        return arguments;
    }

    @Override
    public GraphQl visitArgument(GraphQl.Argument argument, PrintOutputCapture<P> p) {
        beforeSyntax(argument, p);
        printRightPadded(argument.getPadding().getName(), p);
        p.append(":");
        visitValue(argument.getValue(), p);
        afterSyntax(argument, p);
        return argument;
    }

    @Override
    public GraphQl visitStringValue(GraphQl.StringValue stringValue, PrintOutputCapture<P> p) {
        beforeSyntax(stringValue, p);
        if (stringValue.isBlock()) {
            p.append("\"\"\"");
            p.append(stringValue.getValue());
            p.append("\"\"\"");
        } else {
            p.append("\"");
            p.append(escapeString(stringValue.getValue()));
            p.append("\"");
        }
        afterSyntax(stringValue, p);
        return stringValue;
    }

    @Override
    public GraphQl visitIntValue(GraphQl.IntValue intValue, PrintOutputCapture<P> p) {
        beforeSyntax(intValue, p);
        p.append(intValue.getValue());
        afterSyntax(intValue, p);
        return intValue;
    }

    @Override
    public GraphQl visitFloatValue(GraphQl.FloatValue floatValue, PrintOutputCapture<P> p) {
        beforeSyntax(floatValue, p);
        p.append(floatValue.getValue());
        afterSyntax(floatValue, p);
        return floatValue;
    }

    @Override
    public GraphQl visitBooleanValue(GraphQl.BooleanValue booleanValue, PrintOutputCapture<P> p) {
        beforeSyntax(booleanValue, p);
        p.append(booleanValue.isValue() ? "true" : "false");
        afterSyntax(booleanValue, p);
        return booleanValue;
    }

    @Override
    public GraphQl visitNullValue(GraphQl.NullValue nullValue, PrintOutputCapture<P> p) {
        beforeSyntax(nullValue, p);
        p.append("null");
        afterSyntax(nullValue, p);
        return nullValue;
    }

    @Override
    public GraphQl visitEnumValue(GraphQl.EnumValue enumValue, PrintOutputCapture<P> p) {
        beforeSyntax(enumValue, p);
        p.append(enumValue.getName());
        afterSyntax(enumValue, p);
        return enumValue;
    }

    @Override
    public GraphQl visitListValue(GraphQl.ListValue listValue, PrintOutputCapture<P> p) {
        beforeSyntax(listValue, p);
        p.append("[");
        List<GraphQl.Value> values = listValue.getValues();
        for (int i = 0; i < values.size(); i++) {
            visitValue(values.get(i), p);
            
            // Check if we need to add a comma
            if (i < values.size() - 1) {
                GraphQl.Value nextValue = values.get(i + 1);
                String nextPrefix = nextValue.getPrefix().getWhitespace();
                
                // Add comma only if the next value is on the same line
                // (i.e., its prefix doesn't contain a newline)
                if (!nextPrefix.contains("\n")) {
                    p.append(",");
                }
            }
        }
        // Check if there's a trailing comma
        if (!values.isEmpty() && listValue.getEnd() != null) {
            String endWhitespace = listValue.getEnd().getWhitespace();
            if (endWhitespace.startsWith(",")) {
                p.append(",");
                // Print the rest of the whitespace after the comma
                if (endWhitespace.length() > 1) {
                    p.append(endWhitespace.substring(1));
                }
            } else {
                printSpace(listValue.getEnd(), p);
            }
        } else {
            printSpace(listValue.getEnd(), p);
        }
        p.append("]");
        afterSyntax(listValue, p);
        return listValue;
    }

    @Override
    public GraphQl visitObjectValue(GraphQl.ObjectValue objectValue, PrintOutputCapture<P> p) {
        beforeSyntax(objectValue, p);
        p.append("{");
        List<GraphQl.ObjectField> fields = objectValue.getFields();
        for (int i = 0; i < fields.size(); i++) {
            visitObjectField(fields.get(i), p);
            
            // Check if we need to add a comma
            if (i < fields.size() - 1) {
                GraphQl.ObjectField nextField = fields.get(i + 1);
                String nextPrefix = nextField.getPrefix().getWhitespace();
                
                // Add comma only if the next field is on the same line
                // (i.e., its prefix doesn't contain a newline)
                if (!nextPrefix.contains("\n")) {
                    p.append(",");
                }
            }
        }
        // Check if there's a trailing comma
        if (!fields.isEmpty() && objectValue.getEnd() != null) {
            String endWhitespace = objectValue.getEnd().getWhitespace();
            if (endWhitespace.startsWith(",")) {
                p.append(",");
                // Print the rest of the whitespace after the comma
                if (endWhitespace.length() > 1) {
                    p.append(endWhitespace.substring(1));
                }
            } else {
                printSpace(objectValue.getEnd(), p);
            }
        } else {
            printSpace(objectValue.getEnd(), p);
        }
        p.append("}");
        afterSyntax(objectValue, p);
        return objectValue;
    }

    @Override
    public GraphQl visitObjectField(GraphQl.ObjectField objectField, PrintOutputCapture<P> p) {
        beforeSyntax(objectField, p);
        printRightPadded(objectField.getPadding().getName(), p);
        p.append(":");
        visitValue(objectField.getValue(), p);
        afterSyntax(objectField, p);
        return objectField;
    }

    @Override
    public GraphQl visitVariable(GraphQl.Variable variable, PrintOutputCapture<P> p) {
        beforeSyntax(variable, p);
        p.append("$");
        visitName(variable.getName(), p);
        afterSyntax(variable, p);
        return variable;
    }

    @Override
    public GraphQl visitVariableDefinition(GraphQl.VariableDefinition variableDef, PrintOutputCapture<P> p) {
        beforeSyntax(variableDef, p);
        visitVariable(variableDef.getVariable(), p);
        p.append(":");
        visitType(variableDef.getType(), p);
        if (variableDef.getDefaultValue() != null) {
            if (variableDef.getDefaultValuePrefix() != null) {
                printSpace(variableDef.getDefaultValuePrefix(), p);
            }
            p.append("=");
            visitValue(variableDef.getDefaultValue(), p);
        }
        visitList(variableDef.getDirectives(), p);
        afterSyntax(variableDef, p);
        return variableDef;
    }

    @Override
    public GraphQl visitNamedType(GraphQl.NamedType namedType, PrintOutputCapture<P> p) {
        beforeSyntax(namedType, p);
        visitName(namedType.getName(), p);
        afterSyntax(namedType, p);
        return namedType;
    }

    @Override
    public GraphQl visitListType(GraphQl.ListType listType, PrintOutputCapture<P> p) {
        beforeSyntax(listType, p);
        p.append("[");
        visitType(listType.getType(), p);
        p.append("]");
        afterSyntax(listType, p);
        return listType;
    }

    @Override
    public GraphQl visitNonNullType(GraphQl.NonNullType nonNullType, PrintOutputCapture<P> p) {
        beforeSyntax(nonNullType, p);
        visitType(nonNullType.getType(), p);
        p.append("!");
        afterSyntax(nonNullType, p);
        return nonNullType;
    }

    @Override
    public GraphQl visitDirective(GraphQl.Directive directive, PrintOutputCapture<P> p) {
        beforeSyntax(directive, p);
        p.append("@");
        visitName(directive.getName(), p);
        if (directive.getArguments() != null) {
            visitArguments(directive.getArguments(), p);
        }
        afterSyntax(directive, p);
        return directive;
    }

    @Override
    public GraphQl visitFragmentDefinition(GraphQl.FragmentDefinition fragmentDef, PrintOutputCapture<P> p) {
        beforeSyntax(fragmentDef, p);
        p.append("fragment");
        visitName(fragmentDef.getName(), p);
        printSpace(fragmentDef.getOnPrefix(), p);
        p.append("on");
        visitNamedType(fragmentDef.getTypeCondition(), p);
        visitList(fragmentDef.getDirectives(), p);
        visitSelectionSet(fragmentDef.getSelectionSet(), p);
        afterSyntax(fragmentDef, p);
        return fragmentDef;
    }

    @Override
    public GraphQl visitFragmentSpread(GraphQl.FragmentSpread fragmentSpread, PrintOutputCapture<P> p) {
        beforeSyntax(fragmentSpread, p);
        p.append("...");
        visitName(fragmentSpread.getName(), p);
        visitList(fragmentSpread.getDirectives(), p);
        afterSyntax(fragmentSpread, p);
        return fragmentSpread;
    }

    @Override
    public GraphQl visitInlineFragment(GraphQl.InlineFragment inlineFragment, PrintOutputCapture<P> p) {
        beforeSyntax(inlineFragment, p);
        p.append("...");
        if (inlineFragment.getTypeCondition() != null) {
            printSpace(inlineFragment.getOnPrefix(), p);
            p.append("on");
            visitNamedType(inlineFragment.getTypeCondition(), p);
        }
        visitList(inlineFragment.getDirectives(), p);
        visitSelectionSet(inlineFragment.getSelectionSet(), p);
        afterSyntax(inlineFragment, p);
        return inlineFragment;
    }

    @Override
    public GraphQl visitSchemaDefinition(GraphQl.SchemaDefinition schemaDef, PrintOutputCapture<P> p) {
        beforeSyntax(schemaDef, p);
        printDescription(schemaDef.getDescription(), p);
        p.append("schema");
        visitList(schemaDef.getDirectives(), p);
        if (schemaDef.getOperationTypesPrefix() != null) {
            printSpace(schemaDef.getOperationTypesPrefix(), p);
        }
        p.append("{");
        List<GraphQl.RootOperationTypeDefinition> operationTypes = schemaDef.getOperationTypes();
        for (GraphQl.RootOperationTypeDefinition opType : operationTypes) {
            visitRootOperationTypeDefinition(opType, p);
        }
        
        // Check if we need a newline before the closing brace
        if (!operationTypes.isEmpty()) {
            GraphQl.RootOperationTypeDefinition lastOpType = operationTypes.get(operationTypes.size() - 1);
            String lastTypePrefix = lastOpType.getPrefix().getWhitespace();
            if (lastTypePrefix.contains("\n") && lastTypePrefix.trim().isEmpty()) {
                // Last operation type has indentation, so closing brace should be on new line with same indentation
                String indent = lastTypePrefix.substring(lastTypePrefix.lastIndexOf('\n') + 1);
                p.append("\n").append(indent.substring(0, Math.max(0, indent.length() - 2)));
            }
        }
        
        p.append("}");
        afterSyntax(schemaDef, p);
        return schemaDef;
    }

    @Override
    public GraphQl visitRootOperationTypeDefinition(GraphQl.RootOperationTypeDefinition rootOpType, PrintOutputCapture<P> p) {
        beforeSyntax(rootOpType, p);
        p.append(rootOpType.getOperationType().name().toLowerCase());
        p.append(":");
        visitNamedType(rootOpType.getType(), p);
        afterSyntax(rootOpType, p);
        return rootOpType;
    }

    @Override
    public GraphQl visitScalarTypeDefinition(GraphQl.ScalarTypeDefinition scalarType, PrintOutputCapture<P> p) {
        beforeSyntax(scalarType, p);
        printDescription(scalarType.getDescription(), p);
        p.append("scalar");
        visitName(scalarType.getName(), p);
        visitList(scalarType.getDirectives(), p);
        afterSyntax(scalarType, p);
        return scalarType;
    }

    @Override
    public GraphQl visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectType, PrintOutputCapture<P> p) {
        beforeSyntax(objectType, p);
        printDescription(objectType.getDescription(), p);
        p.append("type");
        visitName(objectType.getName(), p);
        visitImplementsInterfaces(objectType.getImplementsPrefix(), objectType.getPadding().getImplementsInterfaces(), p);
        visitList(objectType.getDirectives(), p);
        if (objectType.getFieldsPrefix() != null) {
            printSpace(objectType.getFieldsPrefix(), p);
            if (objectType.getFields() != null && !objectType.getFields().isEmpty()) {
                visitContainer("{", objectType.getFields(), "}", null, p);
            } else {
                // Empty object type definition, e.g., "type Empty {}"
                // We print "{}" as the preferred format for consistency.
                // Known limitation: Cannot preserve the original formatting (inline vs multiline braces)
                // because the AST doesn't store the closing brace's prefix space for empty field lists.
                // See GraphQlParserEdgeCasesTest.parseInterfaceWithNoFields for more details.
                p.append("{}");
            }
        }
        afterSyntax(objectType, p);
        return objectType;
    }

    @Override
    public GraphQl visitFieldDefinition(GraphQl.FieldDefinition fieldDef, PrintOutputCapture<P> p) {
        beforeSyntax(fieldDef, p);
        printDescription(fieldDef.getDescription(), p);
        visitName(fieldDef.getName(), p);
        visitRightPaddedInputValueDefinitions(fieldDef.getPadding().getArguments(), fieldDef.getArgumentsEnd(), p);
        p.append(":");
        visitType(fieldDef.getType(), p);
        visitList(fieldDef.getDirectives(), p);
        afterSyntax(fieldDef, p);
        return fieldDef;
    }

    @Override
    public GraphQl visitInputValueDefinition(GraphQl.InputValueDefinition inputValueDef, PrintOutputCapture<P> p) {
        beforeSyntax(inputValueDef, p);
        printDescription(inputValueDef.getDescription(), p);
        visitName(inputValueDef.getName(), p);
        p.append(":");
        visitType(inputValueDef.getType(), p);
        if (inputValueDef.getDefaultValue() != null) {
            if (inputValueDef.getDefaultValuePrefix() != null) {
                printSpace(inputValueDef.getDefaultValuePrefix(), p);
            }
            p.append("=");
            visitValue(inputValueDef.getDefaultValue(), p);
        }
        visitList(inputValueDef.getDirectives(), p);
        afterSyntax(inputValueDef, p);
        return inputValueDef;
    }

    @Override
    public GraphQl visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceType, PrintOutputCapture<P> p) {
        beforeSyntax(interfaceType, p);
        printDescription(interfaceType.getDescription(), p);
        p.append("interface");
        visitName(interfaceType.getName(), p);
        visitImplementsInterfaces(interfaceType.getImplementsPrefix(), interfaceType.getPadding().getImplementsInterfaces(), p);
        visitList(interfaceType.getDirectives(), p);
        if (interfaceType.getFieldsPrefix() != null) {
            printSpace(interfaceType.getFieldsPrefix(), p);
            if (interfaceType.getFields() != null && !interfaceType.getFields().isEmpty()) {
                visitContainer("{", interfaceType.getFields(), "}", null, p);
            } else {
                // Empty interface definition, e.g., "interface Empty {}"
                // We print "{}" as the preferred format for consistency.
                // Known limitation: Cannot preserve the original formatting (inline vs multiline braces)
                // because the AST doesn't store the closing brace's prefix space for empty field lists.
                // See GraphQlParserEdgeCasesTest.parseInterfaceWithNoFields for more details.
                p.append("{}");
            }
        }
        afterSyntax(interfaceType, p);
        return interfaceType;
    }

    @Override
    public GraphQl visitUnionTypeDefinition(GraphQl.UnionTypeDefinition unionType, PrintOutputCapture<P> p) {
        beforeSyntax(unionType, p);
        printDescription(unionType.getDescription(), p);
        p.append("union");
        visitName(unionType.getName(), p);
        visitList(unionType.getDirectives(), p);
        
        List<GraphQlRightPadded<GraphQl.NamedType>> paddedMemberTypes = unionType.getPadding().getMemberTypes();
        if (paddedMemberTypes != null && !paddedMemberTypes.isEmpty()) {
            if (unionType.getMemberTypesPrefix() != null) {
                printSpace(unionType.getMemberTypesPrefix(), p);
            }
            p.append("=");
            
            for (int i = 0; i < paddedMemberTypes.size(); i++) {
                GraphQlRightPadded<GraphQl.NamedType> paddedType = paddedMemberTypes.get(i);
                
                // Visit the type - its prefix contains the space after equals
                visitNamedType(paddedType.getElement(), p);
                
                // Print the after space which contains whitespace and possibly the pipe separator
                printSpace(paddedType.getAfter(), p);
            }
        }
        afterSyntax(unionType, p);
        return unionType;
    }

    @Override
    public GraphQl visitEnumTypeDefinition(GraphQl.EnumTypeDefinition enumType, PrintOutputCapture<P> p) {
        beforeSyntax(enumType, p);
        printDescription(enumType.getDescription(), p);
        p.append("enum");
        visitName(enumType.getName(), p);
        visitList(enumType.getDirectives(), p);
        visitContainer(" {", enumType.getValues(), "}", null, p);
        afterSyntax(enumType, p);
        return enumType;
    }

    @Override
    public GraphQl visitEnumValueDefinition(GraphQl.EnumValueDefinition enumValueDef, PrintOutputCapture<P> p) {
        beforeSyntax(enumValueDef, p);
        printDescription(enumValueDef.getDescription(), p);
        visitName(enumValueDef.getEnumValue(), p);
        visitList(enumValueDef.getDirectives(), p);
        afterSyntax(enumValueDef, p);
        return enumValueDef;
    }

    @Override
    public GraphQl visitInputObjectTypeDefinition(GraphQl.InputObjectTypeDefinition inputObjectType, PrintOutputCapture<P> p) {
        beforeSyntax(inputObjectType, p);
        printDescription(inputObjectType.getDescription(), p);
        p.append("input");
        visitName(inputObjectType.getName(), p);
        visitList(inputObjectType.getDirectives(), p);
        visitContainer(" {", inputObjectType.getFields(), "}", null, p);
        afterSyntax(inputObjectType, p);
        return inputObjectType;
    }

    @Override
    public GraphQl visitDirectiveDefinition(GraphQl.DirectiveDefinition directiveDef, PrintOutputCapture<P> p) {
        beforeSyntax(directiveDef, p);
        printDescription(directiveDef.getDescription(), p);
        p.append("directive @");
        visitName(directiveDef.getName(), p);
        visitInputValueDefinitions(directiveDef.getArguments(), directiveDef.getArgumentsEnd(), p);
        if (directiveDef.isRepeatable()) {
            if (directiveDef.getRepeatablePrefix() != null) {
                printSpace(directiveDef.getRepeatablePrefix(), p);
            }
            p.append("repeatable");
        }
        if (directiveDef.getOnPrefix() != null) {
            printSpace(directiveDef.getOnPrefix(), p);
        }
        p.append("on");
        for (int i = 0; i < directiveDef.getLocations().size(); i++) {
            GraphQlRightPadded<GraphQl.DirectiveLocationValue> location = directiveDef.getLocations().get(i);
            
            // Print the prefix of the location (whitespace before it)
            printSpace(location.getElement().getPrefix(), p);
            
            // Print the location name
            p.append(location.getElement().getLocation().name());
            
            // Print any trailing space (which includes the pipe separator if not last)
            printSpace(location.getAfter(), p);
        }
        afterSyntax(directiveDef, p);
        return directiveDef;
    }
    
    @Override
    public GraphQl visitSchemaExtension(GraphQl.SchemaExtension schemaExt, PrintOutputCapture<P> p) {
        beforeSyntax(schemaExt, p);
        p.append("extend");
        printSpace(schemaExt.getTypeKeywordPrefix(), p);
        p.append("schema");
        visitList(schemaExt.getDirectives(), p);
        if (schemaExt.getOperationTypes() != null && !schemaExt.getOperationTypes().isEmpty()) {
            if (schemaExt.getOperationTypesPrefix() != null) {
                printSpace(schemaExt.getOperationTypesPrefix(), p);
            }
            p.append("{");
            List<GraphQl.RootOperationTypeDefinition> operationTypes = schemaExt.getOperationTypes();
            for (GraphQl.RootOperationTypeDefinition opType : operationTypes) {
                visitRootOperationTypeDefinition(opType, p);
            }
            
            // Check if we need a newline before the closing brace
            GraphQl.RootOperationTypeDefinition lastOpType = operationTypes.get(operationTypes.size() - 1);
            String lastTypePrefix = lastOpType.getPrefix().getWhitespace();
            if (lastTypePrefix.contains("\n") && lastTypePrefix.trim().isEmpty()) {
                // Last operation type has indentation, so closing brace should be on new line with same indentation
                String indent = lastTypePrefix.substring(lastTypePrefix.lastIndexOf('\n') + 1);
                p.append("\n").append(indent.substring(0, Math.max(0, indent.length() - 2)));
            }
            
            p.append("}");
        }
        afterSyntax(schemaExt, p);
        return schemaExt;
    }
    
    @Override
    public GraphQl visitScalarTypeExtension(GraphQl.ScalarTypeExtension scalarExt, PrintOutputCapture<P> p) {
        beforeSyntax(scalarExt, p);
        p.append("extend");
        printSpace(scalarExt.getTypeKeywordPrefix(), p);
        p.append("scalar");
        visitName(scalarExt.getName(), p);
        visitList(scalarExt.getDirectives(), p);
        afterSyntax(scalarExt, p);
        return scalarExt;
    }
    
    @Override
    public GraphQl visitObjectTypeExtension(GraphQl.ObjectTypeExtension objectExt, PrintOutputCapture<P> p) {
        beforeSyntax(objectExt, p);
        p.append("extend");
        printSpace(objectExt.getTypeKeywordPrefix(), p);
        p.append("type");
        visitName(objectExt.getName(), p);
        visitImplementsInterfaces(objectExt.getImplementsPrefix(), objectExt.getPadding().getImplementsInterfaces(), p);
        visitList(objectExt.getDirectives(), p);
        if (objectExt.getFields() != null && !objectExt.getFields().isEmpty()) {
            printSpace(objectExt.getFieldsPrefix(), p);
            visitContainer("{", objectExt.getFields(), "}", null, p);
        }
        afterSyntax(objectExt, p);
        return objectExt;
    }
    
    @Override
    public GraphQl visitInterfaceTypeExtension(GraphQl.InterfaceTypeExtension interfaceExt, PrintOutputCapture<P> p) {
        beforeSyntax(interfaceExt, p);
        p.append("extend");
        printSpace(interfaceExt.getTypeKeywordPrefix(), p);
        p.append("interface");
        visitName(interfaceExt.getName(), p);
        visitImplementsInterfaces(interfaceExt.getImplementsPrefix(), interfaceExt.getPadding().getImplementsInterfaces(), p);
        visitList(interfaceExt.getDirectives(), p);
        if (interfaceExt.getFields() != null && !interfaceExt.getFields().isEmpty()) {
            printSpace(interfaceExt.getFieldsPrefix(), p);
            visitContainer("{", interfaceExt.getFields(), "}", null, p);
        }
        afterSyntax(interfaceExt, p);
        return interfaceExt;
    }
    
    @Override
    public GraphQl visitUnionTypeExtension(GraphQl.UnionTypeExtension unionExt, PrintOutputCapture<P> p) {
        beforeSyntax(unionExt, p);
        p.append("extend");
        printSpace(unionExt.getTypeKeywordPrefix(), p);
        p.append("union");
        visitName(unionExt.getName(), p);
        visitList(unionExt.getDirectives(), p);
        
        List<GraphQlRightPadded<GraphQl.NamedType>> paddedMemberTypes = unionExt.getPadding().getMemberTypes();
        if (paddedMemberTypes != null && !paddedMemberTypes.isEmpty()) {
            if (unionExt.getMemberTypesPrefix() != null) {
                printSpace(unionExt.getMemberTypesPrefix(), p);
            }
            p.append("=");
            
            for (int i = 0; i < paddedMemberTypes.size(); i++) {
                GraphQlRightPadded<GraphQl.NamedType> paddedType = paddedMemberTypes.get(i);
                
                // Visit the type - its prefix contains the space after equals
                visitNamedType(paddedType.getElement(), p);
                
                // Print the after space which contains whitespace and possibly the pipe separator
                printSpace(paddedType.getAfter(), p);
            }
        }
        afterSyntax(unionExt, p);
        return unionExt;
    }
    
    @Override
    public GraphQl visitEnumTypeExtension(GraphQl.EnumTypeExtension enumExt, PrintOutputCapture<P> p) {
        beforeSyntax(enumExt, p);
        p.append("extend");
        printSpace(enumExt.getTypeKeywordPrefix(), p);
        p.append("enum");
        visitName(enumExt.getName(), p);
        visitList(enumExt.getDirectives(), p);
        visitContainer(" {", enumExt.getValues(), "}", null, p);
        afterSyntax(enumExt, p);
        return enumExt;
    }
    
    @Override
    public GraphQl visitInputObjectTypeExtension(GraphQl.InputObjectTypeExtension inputExt, PrintOutputCapture<P> p) {
        beforeSyntax(inputExt, p);
        p.append("extend");
        printSpace(inputExt.getTypeKeywordPrefix(), p);
        p.append("input");
        visitName(inputExt.getName(), p);
        visitList(inputExt.getDirectives(), p);
        visitContainer(" {", inputExt.getFields(), "}", null, p);
        afterSyntax(inputExt, p);
        return inputExt;
    }

    private void visitImplementsInterfaces(@Nullable Space implementsPrefix, @Nullable List<GraphQlRightPadded<GraphQl.NamedType>> interfaces, PrintOutputCapture<P> p) {
        if (interfaces != null && !interfaces.isEmpty()) {
            if (implementsPrefix != null) {
                printSpace(implementsPrefix, p);
            }
            p.append("implements");
            for (int i = 0; i < interfaces.size(); i++) {
                GraphQlRightPadded<GraphQl.NamedType> padded = interfaces.get(i);
                if (i > 0) {
                    p.append("&");
                }
                visitNamedType(padded.getElement(), p);
                // Print the after space which contains whitespace (and potentially & whitespace for next interface)
                printSpace(padded.getAfter(), p);
            }
        }
    }

    private <T extends GraphQl> void visitList(@Nullable List<T> list, PrintOutputCapture<P> p) {
        if (list != null) {
            for (T item : list) {
                visitGraphQl(item, p);
            }
        }
    }

    private <T extends GraphQl> void visitContainer(@Nullable String before, @Nullable List<T> list, 
                                                   @Nullable String after, @Nullable String separator,
                                                   PrintOutputCapture<P> p) {
        if (list != null && !list.isEmpty()) {
            if (before != null) {
                p.append(before);
            }
            for (int i = 0; i < list.size(); i++) {
                if (i > 0 && separator != null) {
                    p.append(separator);
                    p.append(" ");
                }
                visitGraphQl(list.get(i), p);
            }
            if (after != null) {
                // Check if the last item had a newline in its prefix, indicating the closing brace should be on a new line
                if (!list.isEmpty()) {
                    T lastItem = list.get(list.size() - 1);
                    String lastItemPrefix = lastItem.getPrefix().getWhitespace();
                    if (lastItemPrefix.contains("\n")) {
                        // Extract the indentation from the last item
                        int lastNewline = lastItemPrefix.lastIndexOf('\n');
                        String baseIndent = lastItemPrefix.substring(lastNewline + 1);
                        // Reduce indent for closing brace (typically by 2 spaces)
                        if (baseIndent.length() >= 2) {
                            p.append("\n" + baseIndent.substring(0, baseIndent.length() - 2));
                        } else {
                            p.append("\n");
                        }
                    }
                }
                p.append(after);
            }
        }
    }

    
    private void visitVariableDefinitions(@Nullable List<GraphQl.VariableDefinition> varDefs, @Nullable Space end, PrintOutputCapture<P> p) {
        if (varDefs != null && !varDefs.isEmpty()) {
            p.append("(");
            for (int i = 0; i < varDefs.size(); i++) {
                GraphQl.VariableDefinition varDef = varDefs.get(i);
                visitVariableDefinition(varDef, p);
                
                // Check if we need to add a comma
                if (i < varDefs.size() - 1) {
                    GraphQl.VariableDefinition nextVarDef = varDefs.get(i + 1);
                    String nextPrefix = nextVarDef.getPrefix().getWhitespace();
                    
                    // Add comma only if the next variable definition is on the same line
                    // (i.e., its prefix doesn't contain a newline)
                    if (!nextPrefix.contains("\n")) {
                        p.append(",");
                    }
                }
            }
            printSpace(end, p);
            p.append(")");
        }
    }
    
    private void visitInputValueDefinitions(@Nullable List<GraphQl.InputValueDefinition> inputDefs, @Nullable Space end, PrintOutputCapture<P> p) {
        if (inputDefs != null && !inputDefs.isEmpty()) {
            p.append("(");
            for (int i = 0; i < inputDefs.size(); i++) {
                GraphQl.InputValueDefinition inputDef = inputDefs.get(i);
                visitInputValueDefinition(inputDef, p);
                
                // Print comma between input values when they're on the same line
                if (i < inputDefs.size() - 1) {
                    GraphQl.InputValueDefinition nextDef = inputDefs.get(i + 1);
                    // If the next input value doesn't start with a newline, we need a comma
                    if (!nextDef.getPrefix().getWhitespace().contains("\n")) {
                        p.append(",");
                    }
                }
            }
            printSpace(end, p);
            p.append(")");
        }
    }
    
    private void visitInputValueDefinitions(@Nullable List<GraphQl.InputValueDefinition> inputDefs, PrintOutputCapture<P> p) {
        visitInputValueDefinitions(inputDefs, null, p);
    }
    
    private void visitRightPaddedInputValueDefinitions(@Nullable List<GraphQlRightPadded<GraphQl.InputValueDefinition>> inputDefs, @Nullable Space end, PrintOutputCapture<P> p) {
        if (inputDefs != null && !inputDefs.isEmpty()) {
            p.append("(");
            for (GraphQlRightPadded<GraphQl.InputValueDefinition> paddedInput : inputDefs) {
                visitInputValueDefinition(paddedInput.getElement(), p);
                printSpace(paddedInput.getAfter(), p);
            }
            printSpace(end, p);
            p.append(")");
        }
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    @Override
    public @Nullable Space visitSpace(@Nullable Space space, PrintOutputCapture<P> p) {
        printSpace(space, p);
        return space;
    }
    
    private void printSpace(@Nullable Space space, PrintOutputCapture<P> p) {
        if (space == null) {
            return;
        }
        p.append(space.getWhitespace());
        
        for (Comment comment : space.getComments()) {
            // Don't call visitMarkers as it's not needed for printing
            // visitMarkers(comment.getMarkers(), p);
            p.append(comment.getText());
            p.append(comment.getSuffix());
        }
    }
    
    private <T extends GraphQl> void printRightPadded(GraphQlRightPadded<T> rightPadded, PrintOutputCapture<P> p) {
        visitGraphQl(rightPadded.getElement(), p);
        printSpace(rightPadded.getAfter(), p);
    }

    private void beforeSyntax(GraphQl graphQl, PrintOutputCapture<P> p) {
        beforeSyntax(graphQl.getPrefix(), graphQl.getMarkers(), p);
    }

    private void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), GRAPHQL_MARKER_WRAPPER));
        }
        printSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), GRAPHQL_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(GraphQl graphQl, PrintOutputCapture<P> p) {
        afterSyntax(graphQl.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), GRAPHQL_MARKER_WRAPPER));
        }
    }

    public Markers visitMarkers(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), GRAPHQL_MARKER_WRAPPER));
        }
        return markers;
    }

    private void visitGraphQl(GraphQl element, PrintOutputCapture<P> p) {
        // Dispatch to the appropriate visit method based on the element type
        if (element instanceof GraphQl.Document) {
            visitDocument((GraphQl.Document) element, p);
        } else if (element instanceof GraphQl.OperationDefinition) {
            visitOperationDefinition((GraphQl.OperationDefinition) element, p);
        } else if (element instanceof GraphQl.SelectionSet) {
            visitSelectionSet((GraphQl.SelectionSet) element, p);
        } else if (element instanceof GraphQl.Field) {
            visitField((GraphQl.Field) element, p);
        } else if (element instanceof GraphQl.Name) {
            visitName((GraphQl.Name) element, p);
        } else if (element instanceof GraphQl.Argument) {
            visitArgument((GraphQl.Argument) element, p);
        } else if (element instanceof GraphQl.StringValue) {
            visitStringValue((GraphQl.StringValue) element, p);
        } else if (element instanceof GraphQl.IntValue) {
            visitIntValue((GraphQl.IntValue) element, p);
        } else if (element instanceof GraphQl.FloatValue) {
            visitFloatValue((GraphQl.FloatValue) element, p);
        } else if (element instanceof GraphQl.BooleanValue) {
            visitBooleanValue((GraphQl.BooleanValue) element, p);
        } else if (element instanceof GraphQl.NullValue) {
            visitNullValue((GraphQl.NullValue) element, p);
        } else if (element instanceof GraphQl.EnumValue) {
            visitEnumValue((GraphQl.EnumValue) element, p);
        } else if (element instanceof GraphQl.ListValue) {
            visitListValue((GraphQl.ListValue) element, p);
        } else if (element instanceof GraphQl.ObjectValue) {
            visitObjectValue((GraphQl.ObjectValue) element, p);
        } else if (element instanceof GraphQl.ObjectField) {
            visitObjectField((GraphQl.ObjectField) element, p);
        } else if (element instanceof GraphQl.Variable) {
            visitVariable((GraphQl.Variable) element, p);
        } else if (element instanceof GraphQl.VariableDefinition) {
            visitVariableDefinition((GraphQl.VariableDefinition) element, p);
        } else if (element instanceof GraphQl.NamedType) {
            visitNamedType((GraphQl.NamedType) element, p);
        } else if (element instanceof GraphQl.ListType) {
            visitListType((GraphQl.ListType) element, p);
        } else if (element instanceof GraphQl.NonNullType) {
            visitNonNullType((GraphQl.NonNullType) element, p);
        } else if (element instanceof GraphQl.Directive) {
            visitDirective((GraphQl.Directive) element, p);
        } else if (element instanceof GraphQl.FragmentDefinition) {
            visitFragmentDefinition((GraphQl.FragmentDefinition) element, p);
        } else if (element instanceof GraphQl.FragmentSpread) {
            visitFragmentSpread((GraphQl.FragmentSpread) element, p);
        } else if (element instanceof GraphQl.InlineFragment) {
            visitInlineFragment((GraphQl.InlineFragment) element, p);
        } else if (element instanceof GraphQl.SchemaDefinition) {
            visitSchemaDefinition((GraphQl.SchemaDefinition) element, p);
        } else if (element instanceof GraphQl.RootOperationTypeDefinition) {
            visitRootOperationTypeDefinition((GraphQl.RootOperationTypeDefinition) element, p);
        } else if (element instanceof GraphQl.ScalarTypeDefinition) {
            visitScalarTypeDefinition((GraphQl.ScalarTypeDefinition) element, p);
        } else if (element instanceof GraphQl.ObjectTypeDefinition) {
            visitObjectTypeDefinition((GraphQl.ObjectTypeDefinition) element, p);
        } else if (element instanceof GraphQl.FieldDefinition) {
            visitFieldDefinition((GraphQl.FieldDefinition) element, p);
        } else if (element instanceof GraphQl.InputValueDefinition) {
            visitInputValueDefinition((GraphQl.InputValueDefinition) element, p);
        } else if (element instanceof GraphQl.InterfaceTypeDefinition) {
            visitInterfaceTypeDefinition((GraphQl.InterfaceTypeDefinition) element, p);
        } else if (element instanceof GraphQl.UnionTypeDefinition) {
            visitUnionTypeDefinition((GraphQl.UnionTypeDefinition) element, p);
        } else if (element instanceof GraphQl.EnumTypeDefinition) {
            visitEnumTypeDefinition((GraphQl.EnumTypeDefinition) element, p);
        } else if (element instanceof GraphQl.EnumValueDefinition) {
            visitEnumValueDefinition((GraphQl.EnumValueDefinition) element, p);
        } else if (element instanceof GraphQl.InputObjectTypeDefinition) {
            visitInputObjectTypeDefinition((GraphQl.InputObjectTypeDefinition) element, p);
        } else if (element instanceof GraphQl.DirectiveDefinition) {
            visitDirectiveDefinition((GraphQl.DirectiveDefinition) element, p);
        } else if (element instanceof GraphQl.SchemaExtension) {
            visitSchemaExtension((GraphQl.SchemaExtension) element, p);
        } else if (element instanceof GraphQl.ScalarTypeExtension) {
            visitScalarTypeExtension((GraphQl.ScalarTypeExtension) element, p);
        } else if (element instanceof GraphQl.ObjectTypeExtension) {
            visitObjectTypeExtension((GraphQl.ObjectTypeExtension) element, p);
        } else if (element instanceof GraphQl.InterfaceTypeExtension) {
            visitInterfaceTypeExtension((GraphQl.InterfaceTypeExtension) element, p);
        } else if (element instanceof GraphQl.UnionTypeExtension) {
            visitUnionTypeExtension((GraphQl.UnionTypeExtension) element, p);
        } else if (element instanceof GraphQl.EnumTypeExtension) {
            visitEnumTypeExtension((GraphQl.EnumTypeExtension) element, p);
        } else if (element instanceof GraphQl.InputObjectTypeExtension) {
            visitInputObjectTypeExtension((GraphQl.InputObjectTypeExtension) element, p);
        }
    }

    private void visitValue(GraphQl.Value value, PrintOutputCapture<P> p) {
        if (value instanceof GraphQl.StringValue) {
            visitStringValue((GraphQl.StringValue) value, p);
        } else if (value instanceof GraphQl.IntValue) {
            visitIntValue((GraphQl.IntValue) value, p);
        } else if (value instanceof GraphQl.FloatValue) {
            visitFloatValue((GraphQl.FloatValue) value, p);
        } else if (value instanceof GraphQl.BooleanValue) {
            visitBooleanValue((GraphQl.BooleanValue) value, p);
        } else if (value instanceof GraphQl.NullValue) {
            visitNullValue((GraphQl.NullValue) value, p);
        } else if (value instanceof GraphQl.EnumValue) {
            visitEnumValue((GraphQl.EnumValue) value, p);
        } else if (value instanceof GraphQl.ListValue) {
            visitListValue((GraphQl.ListValue) value, p);
        } else if (value instanceof GraphQl.ObjectValue) {
            visitObjectValue((GraphQl.ObjectValue) value, p);
        } else if (value instanceof GraphQl.Variable) {
            visitVariable((GraphQl.Variable) value, p);
        }
    }

    private void visitType(GraphQl.Type type, PrintOutputCapture<P> p) {
        if (type instanceof GraphQl.NamedType) {
            visitNamedType((GraphQl.NamedType) type, p);
        } else if (type instanceof GraphQl.ListType) {
            visitListType((GraphQl.ListType) type, p);
        } else if (type instanceof GraphQl.NonNullType) {
            visitNonNullType((GraphQl.NonNullType) type, p);
        }
    }
    
    @Override
    public GraphQl visitDirectiveLocationValue(GraphQl.DirectiveLocationValue locationValue, PrintOutputCapture<P> p) {
        beforeSyntax(locationValue, p);
        p.append(locationValue.getLocation().name());
        afterSyntax(locationValue, p);
        return locationValue;
    }

    private static final GraphQlPrinter<Void> INSTANCE = new GraphQlPrinter<>();

    public static String print(GraphQl graphQl) {
        return print(graphQl, new Cursor(null, "root"));
    }

    public static String print(GraphQl graphQl, Cursor cursor) {
        PrintOutputCapture<Void> p = new PrintOutputCapture<>(null);
        INSTANCE.visit(graphQl, p, cursor);
        return p.getOut();
    }
}