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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.graphql.internal.grammar.GraphQLBaseVisitor;
import org.openrewrite.graphql.internal.grammar.GraphQLLexer;
import org.openrewrite.graphql.internal.grammar.GraphQLParser;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.graphql.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.graphql.tree.GraphQlRightPadded;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GraphQlParserVisitor extends GraphQLBaseVisitor<GraphQl> {
    private final Path path;
    private final @Nullable Charset charset;
    private final String source;
    private int cursor = 0;

    public GraphQlParserVisitor(Path path, @Nullable Charset charset, String source) {
        this.path = path;
        this.charset = charset;
        this.source = source;
    }

    @Override
    public GraphQl.Document visitDocument(GraphQLParser.DocumentContext ctx) {
        // For empty documents, capture all content as prefix
        Space documentPrefix = Space.EMPTY;
        if (ctx.definition().isEmpty()) {
            // Empty document - capture all whitespace/comments
            documentPrefix = Space.format(source);
            cursor = source.length();
        }
        
        List<GraphQl.Definition> definitions = new ArrayList<>();
        for (GraphQLParser.DefinitionContext defCtx : ctx.definition()) {
            definitions.add((GraphQl.Definition) visit(defCtx));
        }
        
        // EOF space is from current cursor to end of file
        Space eof = cursor < source.length() ? Space.format(source.substring(cursor)) : Space.EMPTY;
        
        return new GraphQl.Document(
            UUID.randomUUID(),
            path,
            null,
            documentPrefix,
            Markers.EMPTY,
            charset != null ? charset.name() : null,
            false,
            null,
            definitions,
            eof
        );
    }

    @Override
    public GraphQl visitDefinition(GraphQLParser.DefinitionContext ctx) {
        if (ctx.executableDefinition() != null) {
            return visit(ctx.executableDefinition());
        } else if (ctx.typeSystemDefinition() != null) {
            return visit(ctx.typeSystemDefinition());
        } else if (ctx.typeSystemExtension() != null) {
            return visit(ctx.typeSystemExtension());
        }
        throw new IllegalStateException("Unknown definition type");
    }

    @Override
    public GraphQl visitExecutableDefinition(GraphQLParser.ExecutableDefinitionContext ctx) {
        if (ctx.operationDefinition() != null) {
            return visit(ctx.operationDefinition());
        } else if (ctx.fragmentDefinition() != null) {
            return visit(ctx.fragmentDefinition());
        }
        throw new IllegalStateException("Unknown executable definition type");
    }

    @Override
    public GraphQl.OperationDefinition visitOperationDefinition(GraphQLParser.OperationDefinitionContext ctx) {
        return convert(ctx, (operationDef, prefix) -> {
            GraphQl.OperationType operationType = null;
            GraphQl.Name name = null;
            List<GraphQl.VariableDefinition> variableDefinitions = null;
            Space variableDefinitionsEnd = null;
            List<GraphQl.Directive> directives = null;
            
            if (operationDef.operationType() != null) {
                operationType = parseOperationType(operationDef.operationType());
                if (operationDef.name() != null) {
                    name = visitName(operationDef.name());
                }
                if (operationDef.variableDefinitions() != null) {
                    VariableDefinitionsResult result = parseVariableDefinitions(operationDef.variableDefinitions());
                    variableDefinitions = result.definitions;
                    variableDefinitionsEnd = result.end;
                }
                if (operationDef.directives() != null) {
                    directives = parseDirectives(operationDef.directives());
                }
            }
            
            GraphQl.SelectionSet selectionSet = visitSelectionSet(operationDef.selectionSet());
            
            return new GraphQl.OperationDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                operationType,
                name,
                variableDefinitions,
                variableDefinitionsEnd,
                directives,
                selectionSet
            );
        });
    }

    private GraphQl.OperationType parseOperationType(GraphQLParser.OperationTypeContext ctx) {
        String opType = ctx.getText();
        // Advance cursor past the operation type token
        if (ctx.stop != null) {
            advanceCursor(ctx.stop.getStopIndex() + 1);
        }
        switch (opType) {
            case "query":
                return GraphQl.OperationType.QUERY;
            case "mutation":
                return GraphQl.OperationType.MUTATION;
            case "subscription":
                return GraphQl.OperationType.SUBSCRIPTION;
            default:
                throw new IllegalArgumentException("Unknown operation type: " + opType);
        }
    }

    @Override
    public GraphQl.SelectionSet visitSelectionSet(GraphQLParser.SelectionSetContext ctx) {
        Space prefix = prefix(ctx);
        
        // Consume the opening brace and any whitespace after it
        sourceBefore("{");
        
        List<GraphQl.Selection> selections = new ArrayList<>();
        if (ctx.selection() != null) {
            for (GraphQLParser.SelectionContext selCtx : ctx.selection()) {
                selections.add((GraphQl.Selection) visit(selCtx));
            }
        }
        
        // Capture whitespace before closing brace
        Space end = sourceBefore("}");
        
        // Advance cursor to the end of the context
        if (ctx.stop != null) {
            advanceCursor(ctx.stop.getStopIndex() + 1);
        }
        
        return new GraphQl.SelectionSet(
            UUID.randomUUID(),
            prefix,
            Markers.EMPTY,
            selections,
            end
        );
    }

    @Override
    public GraphQl visitSelection(GraphQLParser.SelectionContext ctx) {
        if (ctx.field() != null) {
            return visit(ctx.field());
        } else if (ctx.fragmentSpread() != null) {
            return visit(ctx.fragmentSpread());
        } else if (ctx.inlineFragment() != null) {
            return visit(ctx.inlineFragment());
        }
        throw new IllegalStateException("Unknown selection type");
    }

    @Override
    public GraphQl.Field visitField(GraphQLParser.FieldContext ctx) {
        return convert(ctx, (field, prefix) -> {
            GraphQl.Name alias = null;
            if (field.alias() != null) {
                alias = visitName(field.alias().name());
                sourceBefore(":");
            }
            
            GraphQl.Name name = visitName(field.name());
            
            GraphQl.Arguments arguments = null;
            if (field.arguments() != null) {
                arguments = parseArguments(field.arguments());
            }
            
            List<GraphQl.Directive> directives = null;
            if (field.directives() != null) {
                directives = parseDirectives(field.directives());
            }
            
            GraphQl.SelectionSet selectionSet = null;
            if (field.selectionSet() != null) {
                selectionSet = visitSelectionSet(field.selectionSet());
            }
            
            return new GraphQl.Field(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                alias,
                name,
                arguments,
                directives,
                selectionSet
            );
        });
    }

    @Override
    public GraphQl.Name visitName(GraphQLParser.NameContext ctx) {
        return convert(ctx, (name, prefix) -> {
            // The name rule has many alternatives (NAME token and various keywords)
            // We need to get the text regardless of which alternative matched
            String nameText = name.getText();
            
            return new GraphQl.Name(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                nameText
            );
        });
    }

    private GraphQl.Arguments parseArguments(GraphQLParser.ArgumentsContext ctx) {
        Space prefix = sourceBefore("(");
        List<GraphQlRightPadded<GraphQl.Argument>> arguments = new ArrayList<>();
        if (ctx.argument() != null) {
            for (int i = 0; i < ctx.argument().size(); i++) {
                GraphQl.Argument arg = visitArgument(ctx.argument(i));
                
                // Capture everything after this argument up to the next argument or closing paren
                Space after = Space.EMPTY;
                if (i < ctx.argument().size() - 1) {
                    // Not the last argument - capture whitespace and optional comma
                    int startPos = cursor;
                    whitespace();
                    
                    // Check if there's a comma
                    boolean hasComma = cursor < source.length() && source.charAt(cursor) == ',';
                    if (hasComma) {
                        cursor++; // consume comma
                        whitespace(); // whitespace after comma
                    }
                    
                    // Capture everything from start to current position
                    after = Space.format(source.substring(startPos, cursor));
                } else {
                    // Last argument - check for trailing comma
                    int startPos = cursor;
                    whitespace();
                    
                    if (cursor < source.length() && source.charAt(cursor) == ',') {
                        cursor++; // consume comma
                        whitespace(); // whitespace after comma
                        after = Space.format(source.substring(startPos, cursor));
                    } else {
                        // No trailing comma, just use the whitespace
                        after = Space.format(source.substring(startPos, cursor));
                    }
                }
                
                arguments.add(new GraphQlRightPadded<>(arg, after, Markers.EMPTY));
            }
        }
        Space end = sourceBefore(")");
        return new GraphQl.Arguments(
            UUID.randomUUID(),
            Space.EMPTY,
            Markers.EMPTY,
            arguments,
            end
        );
    }

    @Override
    public GraphQl.Argument visitArgument(GraphQLParser.ArgumentContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.Name name = visitName(c.name());
            Space afterName = sourceBefore(":");
            GraphQl.Value value = (GraphQl.Value) visit(c.value());
            
            return new GraphQl.Argument(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                new GraphQlRightPadded<>(name, afterName, Markers.EMPTY),
                value
            );
        });
    }

    @Override
    public GraphQl visitValue(GraphQLParser.ValueContext ctx) {
        if (ctx.variable() != null) {
            return visit(ctx.variable());
        } else if (ctx.intValue() != null) {
            return parseIntValue(ctx.intValue());
        } else if (ctx.floatValue() != null) {
            return parseFloatValue(ctx.floatValue());
        } else if (ctx.stringValue() != null) {
            return parseStringValue(ctx.stringValue());
        } else if (ctx.booleanValue() != null) {
            return parseBooleanValue(ctx.booleanValue());
        } else if (ctx.nullValue() != null) {
            return parseNullValue(ctx.nullValue());
        } else if (ctx.enumValue() != null) {
            return parseEnumValue(ctx.enumValue());
        } else if (ctx.listValue() != null) {
            return visit(ctx.listValue());
        } else if (ctx.objectValue() != null) {
            return visit(ctx.objectValue());
        }
        throw new IllegalStateException("Unknown value type");
    }

    @Override
    public GraphQl.Variable visitVariable(GraphQLParser.VariableContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("$");  // Consume the '$' symbol
            GraphQl.Name name = visitName(c.name());
            
            return new GraphQl.Variable(
                UUID.randomUUID(),
                prefix,  
                Markers.EMPTY,
                name
            );
        });
    }

    private GraphQl.IntValue parseIntValue(GraphQLParser.IntValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String value = c.INT().getText();
            
            return new GraphQl.IntValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                value
            );
        });
    }

    private GraphQl.FloatValue parseFloatValue(GraphQLParser.FloatValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String value = c.FLOAT().getText();
            
            return new GraphQl.FloatValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                value
            );
        });
    }

    private GraphQl.StringValue parseStringValue(GraphQLParser.StringValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String rawValue;
            boolean isBlock = false;
            
            if (c.STRING() != null) {
                rawValue = c.STRING().getText();
                // Remove quotes
                rawValue = rawValue.substring(1, rawValue.length() - 1);
                // Unescape
                rawValue = unescapeString(rawValue);
            } else {
                rawValue = c.BLOCK_STRING().getText();
                // Remove triple quotes
                rawValue = rawValue.substring(3, rawValue.length() - 3);
                isBlock = true;
            }
            
            return new GraphQl.StringValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                rawValue,
                isBlock
            );
        });
    }
    
    @Override
    public GraphQl.StringValue visitStringValue(GraphQLParser.StringValueContext ctx) {
        return parseStringValue(ctx);
    }

    private GraphQl.BooleanValue parseBooleanValue(GraphQLParser.BooleanValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            boolean value = c.getText().equals("true");
            
            return new GraphQl.BooleanValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                value
            );
        });
    }

    private GraphQl.NullValue parseNullValue(GraphQLParser.NullValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            return new GraphQl.NullValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY
            );
        });
    }

    private GraphQl.EnumValue parseEnumValue(GraphQLParser.EnumValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String value = c.name().getText();
            
            return new GraphQl.EnumValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                value
            );
        });
    }

    @Override
    public GraphQl.ListValue visitListValue(GraphQLParser.ListValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("[");
            
            List<GraphQl.Value> values = new ArrayList<>();
            for (int i = 0; i < c.value().size(); i++) {
                if (i > 0) {
                    // Check if there's a comma to consume
                    if (cursor < source.length() && source.charAt(cursor) == ',') {
                        sourceBefore(",");
                    }
                }
                values.add((GraphQl.Value) visit(c.value(i)));
            }
            
            // Check for trailing comma
            if (cursor < source.length() && source.charAt(cursor) == ',') {
                sourceBefore(",");
            }
            
            Space end = sourceBefore("]");
            
            return new GraphQl.ListValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                values,
                end
            );
        });
    }

    @Override
    public GraphQl.ObjectValue visitObjectValue(GraphQLParser.ObjectValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("{");
            
            List<GraphQl.ObjectField> fields = new ArrayList<>();
            for (int i = 0; i < c.objectField().size(); i++) {
                if (i > 0) {
                    // Check if there's a comma to consume
                    if (cursor < source.length() && source.charAt(cursor) == ',') {
                        sourceBefore(",");
                    }
                }
                fields.add(visitObjectField(c.objectField(i)));
            }
            
            // Check for trailing comma
            if (cursor < source.length() && source.charAt(cursor) == ',') {
                sourceBefore(",");
            }
            
            Space end = sourceBefore("}");
            
            return new GraphQl.ObjectValue(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                fields,
                end
            );
        });
    }

    @Override
    public GraphQl.ObjectField visitObjectField(GraphQLParser.ObjectFieldContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.Name name = visitName(c.name());
            Space afterName = sourceBefore(":");
            GraphQl.Value value = (GraphQl.Value) visit(c.value());
            
            return new GraphQl.ObjectField(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                new GraphQlRightPadded<>(name, afterName, Markers.EMPTY),
                value
            );
        });
    }

    private static class VariableDefinitionsResult {
        final List<GraphQl.VariableDefinition> definitions;
        final Space end;
        
        VariableDefinitionsResult(List<GraphQl.VariableDefinition> definitions, Space end) {
            this.definitions = definitions;
            this.end = end;
        }
    }
    
    private VariableDefinitionsResult parseVariableDefinitions(GraphQLParser.VariableDefinitionsContext ctx) {
        sourceBefore("(");
        List<GraphQl.VariableDefinition> definitions = new ArrayList<>();
        for (int i = 0; i < ctx.variableDefinition().size(); i++) {
            if (i > 0) {
                // Check if there's a comma to consume
                if (cursor < source.length() && source.charAt(cursor) == ',') {
                    sourceBefore(",");
                }
            }
            definitions.add(visitVariableDefinition(ctx.variableDefinition(i)));
        }
        // Check for trailing comma
        if (cursor < source.length() && source.charAt(cursor) == ',') {
            sourceBefore(",");
        }
        Space end = sourceBefore(")");
        return new VariableDefinitionsResult(definitions, end);
    }

    @Override
    public GraphQl.VariableDefinition visitVariableDefinition(GraphQLParser.VariableDefinitionContext ctx) {
        return convert(ctx, (varDef, prefix) -> {
            GraphQl.Variable variable = visitVariable(varDef.variable());
            sourceBefore(":");
            GraphQl.Type type = (GraphQl.Type) visit(varDef.type());
            
            GraphQl.Value defaultValue = null;
            Space defaultValuePrefix = null;
            if (varDef.defaultValue() != null) {
                defaultValuePrefix = sourceBefore("=");
                defaultValue = (GraphQl.Value) visit(varDef.defaultValue().value());
            }
            
            List<GraphQl.Directive> directives = null;
            if (varDef.directives() != null) {
                directives = parseDirectives(varDef.directives());
            }
            
            return new GraphQl.VariableDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                variable,
                type,
                defaultValuePrefix,
                defaultValue,
                directives
            );
        });
    }

    @Override
    public GraphQl visitType(GraphQLParser.TypeContext ctx) {
        if (ctx.namedType() != null) {
            return visit(ctx.namedType());
        } else if (ctx.listType() != null) {
            return visit(ctx.listType());
        } else if (ctx.nonNullType() != null) {
            return visit(ctx.nonNullType());
        }
        throw new IllegalStateException("Unknown type");
    }

    @Override
    public GraphQl.NamedType visitNamedType(GraphQLParser.NamedTypeContext ctx) {
        return convert(ctx, (namedType, prefix) -> new GraphQl.NamedType(
            UUID.randomUUID(),
            prefix,
            Markers.EMPTY,
            visitName(namedType.name())
        ));
    }

    @Override
    public GraphQl.ListType visitListType(GraphQLParser.ListTypeContext ctx) {
        return convert(ctx, (listType, prefix) -> {
            sourceBefore("[");
            GraphQl.Type type = (GraphQl.Type) visit(listType.type());
            sourceBefore("]");
            
            return new GraphQl.ListType(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                type
            );
        });
    }

    @Override
    public GraphQl.NonNullType visitNonNullType(GraphQLParser.NonNullTypeContext ctx) {
        return convert(ctx, (nonNullType, prefix) -> {
            GraphQl.Type type;
            if (nonNullType.namedType() != null) {
                type = visitNamedType(nonNullType.namedType());
            } else {
                type = visitListType(nonNullType.listType());
            }
            sourceBefore("!");
            
            return new GraphQl.NonNullType(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                type
            );
        });
    }

    private List<GraphQl.Directive> parseDirectives(GraphQLParser.DirectivesContext ctx) {
        List<GraphQl.Directive> directives = new ArrayList<>();
        for (GraphQLParser.DirectiveContext dirCtx : ctx.directive()) {
            directives.add(visitDirective(dirCtx));
        }
        return directives;
    }

    @Override
    public GraphQl.Directive visitDirective(GraphQLParser.DirectiveContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("@");
            GraphQl.Name name = visitName(c.name());
            
            GraphQl.Arguments arguments = null;
            if (c.arguments() != null) {
                arguments = parseArguments(c.arguments());
            }
            
            return new GraphQl.Directive(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                name,
                arguments
            );
        });
    }

    @Override
    public GraphQl.FragmentDefinition visitFragmentDefinition(GraphQLParser.FragmentDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("fragment");
            GraphQl.Name name = visitName(c.fragmentName().name());
            Space onPrefix = sourceBefore("on");
            GraphQl.NamedType typeCondition = visitNamedType(c.typeCondition().namedType());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            GraphQl.SelectionSet selectionSet = visitSelectionSet(c.selectionSet());
            
            return new GraphQl.FragmentDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                name,
                onPrefix,
                typeCondition,
                directives,
                selectionSet
            );
        });
    }

    @Override
    public GraphQl.FragmentSpread visitFragmentSpread(GraphQLParser.FragmentSpreadContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("...");
            GraphQl.Name name = visitName(c.fragmentName().name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            return new GraphQl.FragmentSpread(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                name,
                directives
            );
        });
    }

    @Override
    public GraphQl.InlineFragment visitInlineFragment(GraphQLParser.InlineFragmentContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("...");
            
            Space onPrefix = null;
            GraphQl.NamedType typeCondition = null;
            if (c.typeCondition() != null) {
                onPrefix = sourceBefore("on");
                typeCondition = visitNamedType(c.typeCondition().namedType());
            }
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            GraphQl.SelectionSet selectionSet = visitSelectionSet(c.selectionSet());
            
            return new GraphQl.InlineFragment(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                onPrefix,
                typeCondition,
                directives,
                selectionSet
            );
        });
    }

    // Type System Definitions
    @Override
    public GraphQl visitTypeSystemDefinition(GraphQLParser.TypeSystemDefinitionContext ctx) {
        if (ctx.schemaDefinition() != null) {
            return visit(ctx.schemaDefinition());
        } else if (ctx.typeDefinition() != null) {
            return visit(ctx.typeDefinition());
        } else if (ctx.directiveDefinition() != null) {
            return visit(ctx.directiveDefinition());
        }
        throw new IllegalStateException("Unknown type system definition");
    }

    @Override
    public GraphQl visitTypeDefinition(GraphQLParser.TypeDefinitionContext ctx) {
        if (ctx.scalarTypeDefinition() != null) {
            return visit(ctx.scalarTypeDefinition());
        } else if (ctx.objectTypeDefinition() != null) {
            return visit(ctx.objectTypeDefinition());
        } else if (ctx.interfaceTypeDefinition() != null) {
            return visit(ctx.interfaceTypeDefinition());
        } else if (ctx.unionTypeDefinition() != null) {
            return visit(ctx.unionTypeDefinition());
        } else if (ctx.enumTypeDefinition() != null) {
            return visit(ctx.enumTypeDefinition());
        } else if (ctx.inputObjectTypeDefinition() != null) {
            return visit(ctx.inputObjectTypeDefinition());
        }
        throw new IllegalStateException("Unknown type definition");
    }

    @Override
    public GraphQl.ScalarTypeDefinition visitScalarTypeDefinition(GraphQLParser.ScalarTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("scalar");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            return new GraphQl.ScalarTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                directives
            );
        });
    }

    @Override
    public GraphQl.ObjectTypeDefinition visitObjectTypeDefinition(GraphQLParser.ObjectTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("type");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQlRightPadded<GraphQl.NamedType>> implementsInterfaces = null;
            Space implementsPrefix = null;
            if (c.implementsInterfaces() != null) {
                ImplementsInterfacesResult result = parseImplementsInterfaces(c.implementsInterfaces());
                implementsPrefix = result.implementsPrefix;
                implementsInterfaces = result.interfaces;
            }
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.FieldDefinition> fields = null;
            Space fieldsPrefix = null;
            if (c.fieldsDefinition() != null) {
                FieldsDefinitionResult result = parseFieldsDefinition(c.fieldsDefinition());
                fieldsPrefix = result.prefix;
                fields = result.fields;
            }
            
            return new GraphQl.ObjectTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                implementsPrefix,
                implementsInterfaces,
                directives,
                fieldsPrefix,
                fields
            );
        });
    }

    private ImplementsInterfacesResult parseImplementsInterfaces(GraphQLParser.ImplementsInterfacesContext ctx) {
        List<GraphQlRightPadded<GraphQl.NamedType>> interfaces = new ArrayList<>();
        Space implementsPrefix = sourceBefore("implements");
        
        // The grammar is right-recursive, so we need to collect all contexts first
        List<GraphQLParser.ImplementsInterfacesContext> contexts = new ArrayList<>();
        GraphQLParser.ImplementsInterfacesContext current = ctx;
        
        while (current != null) {
            contexts.add(0, current); // Add at beginning to reverse the order
            current = current.implementsInterfaces();
        }
        
        // Now process them in the correct order
        for (int i = 0; i < contexts.size(); i++) {
            GraphQLParser.ImplementsInterfacesContext implCtx = contexts.get(i);
            
            // For subsequent interfaces (not the first), consume the '&' separator
            if (i > 0) {
                sourceBefore("&");
            } else {
                // For the first interface, check if there's an optional '&' after 'implements'
                for (int j = 0; j < implCtx.getChildCount(); j++) {
                    if (implCtx.getChild(j).getText().equals("&")) {
                        sourceBefore("&");
                        break;
                    }
                }
            }
            
            // Find and visit the namedType
            GraphQl.NamedType namedType = null;
            for (int j = 0; j < implCtx.getChildCount(); j++) {
                ParseTree child = implCtx.getChild(j);
                if (child instanceof GraphQLParser.NamedTypeContext) {
                    namedType = visitNamedType((GraphQLParser.NamedTypeContext) child);
                    break;
                }
            }
            
            if (namedType != null) {
                // Capture the after space (whitespace after this interface, not including the next &)
                Space after = Space.EMPTY;
                if (i < contexts.size() - 1) {
                    // Not the last interface - capture any whitespace after this interface
                    after = whitespace();
                }
                
                interfaces.add(new GraphQlRightPadded<>(namedType, after, Markers.EMPTY));
            }
        }
        
        return new ImplementsInterfacesResult(implementsPrefix, interfaces);
    }

    private static class FieldsDefinitionResult {
        final Space prefix;
        final List<GraphQl.FieldDefinition> fields;
        
        FieldsDefinitionResult(Space prefix, List<GraphQl.FieldDefinition> fields) {
            this.prefix = prefix;
            this.fields = fields;
        }
    }
    
    private FieldsDefinitionResult parseFieldsDefinition(GraphQLParser.FieldsDefinitionContext ctx) {
        Space prefix = sourceBefore("{");
        List<GraphQl.FieldDefinition> fields = new ArrayList<>();
        if (ctx.fieldDefinition() != null) {
            for (GraphQLParser.FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                fields.add(visitFieldDefinition(fieldCtx));
            }
        }
        sourceBefore("}");
        return new FieldsDefinitionResult(prefix, fields);
    }

    @Override
    public GraphQl.FieldDefinition visitFieldDefinition(GraphQLParser.FieldDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQlRightPadded<GraphQl.InputValueDefinition>> arguments = null;
            Space argumentsEnd = null;
            if (c.argumentsDefinition() != null) {
                ArgumentsDefinitionRightPaddedResult result = parseArgumentsDefinitionRightPadded(c.argumentsDefinition());
                arguments = result.arguments;
                argumentsEnd = result.end;
            }
            
            sourceBefore(":");
            GraphQl.Type type = (GraphQl.Type) visit(c.type());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            return new GraphQl.FieldDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                arguments,
                argumentsEnd,
                type,
                directives
            );
        });
    }

    private static class ArgumentsDefinitionResult {
        List<GraphQl.InputValueDefinition> arguments;
        Space end;
        
        ArgumentsDefinitionResult(List<GraphQl.InputValueDefinition> arguments, Space end) {
            this.arguments = arguments;
            this.end = end;
        }
    }
    
    private static class ImplementsInterfacesResult {
        Space implementsPrefix;
        List<GraphQlRightPadded<GraphQl.NamedType>> interfaces;
        
        ImplementsInterfacesResult(Space implementsPrefix, List<GraphQlRightPadded<GraphQl.NamedType>> interfaces) {
            this.implementsPrefix = implementsPrefix;
            this.interfaces = interfaces;
        }
    }
    
    private static class UnionMemberTypesResult {
        Space equalsPrefix;
        List<GraphQlRightPadded<GraphQl.NamedType>> memberTypes;
        
        UnionMemberTypesResult(Space equalsPrefix, List<GraphQlRightPadded<GraphQl.NamedType>> memberTypes) {
            this.equalsPrefix = equalsPrefix;
            this.memberTypes = memberTypes;
        }
    }
    
    private ArgumentsDefinitionResult parseArgumentsDefinitionWithEnd(GraphQLParser.ArgumentsDefinitionContext ctx) {
        sourceBefore("(");
        List<GraphQl.InputValueDefinition> arguments = new ArrayList<>();
        if (ctx.inputValueDefinition() != null) {
            for (int i = 0; i < ctx.inputValueDefinition().size(); i++) {
                if (i > 0) {
                    // Check if there's a comma to consume
                    if (cursor < source.length() && source.charAt(cursor) == ',') {
                        sourceBefore(",");
                    }
                }
                arguments.add(visitInputValueDefinition(ctx.inputValueDefinition(i)));
            }
        }
        // Check for trailing comma
        if (cursor < source.length() && source.charAt(cursor) == ',') {
            sourceBefore(",");
        }
        Space end = sourceBefore(")");
        return new ArgumentsDefinitionResult(arguments, end);
    }
    
    private static class ArgumentsDefinitionRightPaddedResult {
        final List<GraphQlRightPadded<GraphQl.InputValueDefinition>> arguments;
        final Space end;
        
        ArgumentsDefinitionRightPaddedResult(List<GraphQlRightPadded<GraphQl.InputValueDefinition>> arguments, Space end) {
            this.arguments = arguments;
            this.end = end;
        }
    }
    
    private ArgumentsDefinitionRightPaddedResult parseArgumentsDefinitionRightPadded(GraphQLParser.ArgumentsDefinitionContext ctx) {
        sourceBefore("(");
        List<GraphQlRightPadded<GraphQl.InputValueDefinition>> arguments = new ArrayList<>();
        if (ctx.inputValueDefinition() != null) {
            for (int i = 0; i < ctx.inputValueDefinition().size(); i++) {
                GraphQl.InputValueDefinition arg = visitInputValueDefinition(ctx.inputValueDefinition(i));
                
                // Capture whitespace after the argument
                Space after = Space.EMPTY;
                if (i < ctx.inputValueDefinition().size() - 1) {
                    // Not the last argument - capture whitespace up to and including comma
                    int startPos = cursor;
                    whitespace();
                    
                    if (cursor < source.length() && source.charAt(cursor) == ',') {
                        cursor++; // consume the comma
                        whitespace(); // and any space after it
                    }
                    
                    after = Space.format(source.substring(startPos, cursor));
                } else {
                    // Last argument - check for trailing comma
                    int startPos = cursor;
                    whitespace();
                    
                    if (cursor < source.length() && source.charAt(cursor) == ',') {
                        cursor++; // consume the trailing comma
                        whitespace(); // and any space after it
                    }
                    
                    if (cursor > startPos) {
                        after = Space.format(source.substring(startPos, cursor));
                    }
                }
                
                arguments.add(new GraphQlRightPadded<>(arg, after, Markers.EMPTY));
            }
        }
        // Check for trailing comma (already handled above)
        Space end = sourceBefore(")");
        return new ArgumentsDefinitionRightPaddedResult(arguments, end);
    }
    
    private List<GraphQl.InputValueDefinition> parseArgumentsDefinition(GraphQLParser.ArgumentsDefinitionContext ctx) {
        return parseArgumentsDefinitionWithEnd(ctx).arguments;
    }

    @Override
    public GraphQl.InputValueDefinition visitInputValueDefinition(GraphQLParser.InputValueDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            GraphQl.Name name = visitName(c.name());
            sourceBefore(":");
            GraphQl.Type type = (GraphQl.Type) visit(c.type());
            
            GraphQl.Value defaultValue = null;
            Space defaultValuePrefix = null;
            if (c.defaultValue() != null) {
                defaultValuePrefix = sourceBefore("=");
                defaultValue = (GraphQl.Value) visit(c.defaultValue().value());
            }
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            return new GraphQl.InputValueDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                type,
                defaultValuePrefix,
                defaultValue,
                directives
            );
        });
    }

    @Override
    public GraphQl.SchemaDefinition visitSchemaDefinition(GraphQLParser.SchemaDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("schema");
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.RootOperationTypeDefinition> operationTypes = new ArrayList<>();
            Space operationTypesPrefix = null;
            if (c.rootOperationTypeDefinition() != null) {
                operationTypesPrefix = sourceBefore("{");
                for (GraphQLParser.RootOperationTypeDefinitionContext opCtx : c.rootOperationTypeDefinition()) {
                    operationTypes.add(visitRootOperationTypeDefinition(opCtx));
                }
                sourceBefore("}");
            }
            
            return new GraphQl.SchemaDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                directives,
                operationTypesPrefix,
                operationTypes
            );
        });
    }

    @Override
    public GraphQl.RootOperationTypeDefinition visitRootOperationTypeDefinition(GraphQLParser.RootOperationTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.OperationType operationType = parseOperationType(c.operationType());
            sourceBefore(":");
            GraphQl.NamedType type = visitNamedType(c.namedType());
            
            return new GraphQl.RootOperationTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                operationType,
                type
            );
        });
    }

    @Override
    public GraphQl.InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQLParser.InterfaceTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("interface");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQlRightPadded<GraphQl.NamedType>> implementsInterfaces = null;
            Space implementsPrefix = null;
            if (c.implementsInterfaces() != null) {
                ImplementsInterfacesResult result = parseImplementsInterfaces(c.implementsInterfaces());
                implementsPrefix = result.implementsPrefix;
                implementsInterfaces = result.interfaces;
            }
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.FieldDefinition> fields = null;
            Space fieldsPrefix = null;
            if (c.fieldsDefinition() != null) {
                FieldsDefinitionResult result = parseFieldsDefinition(c.fieldsDefinition());
                fieldsPrefix = result.prefix;
                fields = result.fields;
            }
            
            return new GraphQl.InterfaceTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                implementsPrefix,
                implementsInterfaces,
                directives,
                fieldsPrefix,
                fields
            );
        });
    }

    @Override
    public GraphQl.UnionTypeDefinition visitUnionTypeDefinition(GraphQLParser.UnionTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("union");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQlRightPadded<GraphQl.NamedType>> memberTypes = null;
            Space memberTypesPrefix = null;
            if (c.unionMemberTypes() != null) {
                UnionMemberTypesResult result = parseUnionMemberTypes(c.unionMemberTypes());
                memberTypesPrefix = result.equalsPrefix;
                memberTypes = result.memberTypes;
            }
            
            return new GraphQl.UnionTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                directives,
                memberTypesPrefix,
                memberTypes
            );
        });
    }

    private UnionMemberTypesResult parseUnionMemberTypes(GraphQLParser.UnionMemberTypesContext ctx) {
        List<GraphQlRightPadded<GraphQl.NamedType>> types = new ArrayList<>();
        Space equalsPrefix = sourceBefore("=");
        
        // Capture the space after the equals sign
        int afterEqualsStart = cursor;
        Space afterEquals = whitespace();
        
        // Collect all union member contexts first (they're in reverse order due to right recursion)
        List<GraphQLParser.UnionMemberTypesContext> contexts = new ArrayList<>();
        GraphQLParser.UnionMemberTypesContext current = ctx;
        while (current != null) {
            contexts.add(current);
            current = current.unionMemberTypes();
        }
        
        // Process them in reverse order to get the correct sequence
        for (int i = contexts.size() - 1; i >= 0; i--) {
            GraphQLParser.UnionMemberTypesContext memberCtx = contexts.get(i);
            
            // Handle space for the first type
            if (i == contexts.size() - 1) {
                // For the first member type, check for optional '|' after '='
                if (cursor < source.length() && source.charAt(cursor) == '|') {
                    cursor++; // skip the pipe
                    Space afterPipe = whitespace();
                    // Include everything from after equals to current position as the first type's prefix
                    afterEquals = Space.format(source.substring(afterEqualsStart, cursor));
                }
                // Reset cursor to before visiting the named type so it gets the full prefix
                cursor = afterEqualsStart;
            }
            
            if (memberCtx.namedType() != null) {
                GraphQl.NamedType namedType = visitNamedType(memberCtx.namedType());
                
                // Capture whitespace and pipe after this type
                Space after = Space.EMPTY;
                if (i > 0) {
                    // Not the last type - capture whitespace and the next pipe
                    int startPos = cursor;
                    whitespace();
                    
                    // Check if there's a pipe
                    if (cursor < source.length() && source.charAt(cursor) == '|') {
                        cursor++; // consume the pipe
                        whitespace(); // and any space after it
                    }
                    
                    after = Space.format(source.substring(startPos, cursor));
                }
                
                types.add(new GraphQlRightPadded<>(namedType, after, Markers.EMPTY));
            }
        }
        
        return new UnionMemberTypesResult(equalsPrefix, types);
    }

    @Override
    public GraphQl.EnumTypeDefinition visitEnumTypeDefinition(GraphQLParser.EnumTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("enum");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.EnumValueDefinition> values = null;
            if (c.enumValuesDefinition() != null) {
                values = parseEnumValuesDefinition(c.enumValuesDefinition());
            }
            
            return new GraphQl.EnumTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                directives,
                values
            );
        });
    }

    private List<GraphQl.EnumValueDefinition> parseEnumValuesDefinition(GraphQLParser.EnumValuesDefinitionContext ctx) {
        sourceBefore("{");
        List<GraphQl.EnumValueDefinition> values = new ArrayList<>();
        if (ctx.enumValueDefinition() != null) {
            for (GraphQLParser.EnumValueDefinitionContext valCtx : ctx.enumValueDefinition()) {
                values.add(visitEnumValueDefinition(valCtx));
            }
        }
        sourceBefore("}");
        return values;
    }

    @Override
    public GraphQl.EnumValueDefinition visitEnumValueDefinition(GraphQLParser.EnumValueDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            // EnumValueDefinition expects a Name, not an EnumValue
            GraphQl.Name enumValue = visitName(c.enumValue().name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            return new GraphQl.EnumValueDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                enumValue,
                directives
            );
        });
    }

    @Override
    public GraphQl.InputObjectTypeDefinition visitInputObjectTypeDefinition(GraphQLParser.InputObjectTypeDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("input");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.InputValueDefinition> fields = null;
            if (c.inputFieldsDefinition() != null) {
                fields = parseInputFieldsDefinition(c.inputFieldsDefinition());
            }
            
            return new GraphQl.InputObjectTypeDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                directives,
                fields
            );
        });
    }

    private List<GraphQl.InputValueDefinition> parseInputFieldsDefinition(GraphQLParser.InputFieldsDefinitionContext ctx) {
        sourceBefore("{");
        List<GraphQl.InputValueDefinition> fields = new ArrayList<>();
        if (ctx.inputValueDefinition() != null) {
            for (GraphQLParser.InputValueDefinitionContext fieldCtx : ctx.inputValueDefinition()) {
                fields.add(visitInputValueDefinition(fieldCtx));
            }
        }
        sourceBefore("}");
        return fields;
    }

    @Override
    public GraphQl.DirectiveDefinition visitDirectiveDefinition(GraphQLParser.DirectiveDefinitionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            GraphQl.StringValue description = null;
            if (c.description() != null) {
                description = visitStringValue(c.description().stringValue());
            }
            
            sourceBefore("directive");
            sourceBefore("@");
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.InputValueDefinition> arguments = null;
            Space argumentsEnd = null;
            if (c.argumentsDefinition() != null) {
                ArgumentsDefinitionResult result = parseArgumentsDefinitionWithEnd(c.argumentsDefinition());
                arguments = result.arguments;
                argumentsEnd = result.end;
            }
            
            // Check for repeatable directive
            boolean repeatable = false;
            Space repeatablePrefix = null;
            if (c.getChildCount() > 0) {
                for (int i = 0; i < c.getChildCount(); i++) {
                    if (c.getChild(i).getText().equals("repeatable")) {
                        repeatable = true;
                        repeatablePrefix = sourceBefore("repeatable");
                        break;
                    }
                }
            }
            
            Space onPrefix = sourceBefore("on");
            List<GraphQlRightPadded<GraphQl.DirectiveLocationValue>> locations = parseDirectiveLocations(c.directiveLocations());
            
            return new GraphQl.DirectiveDefinition(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                description,
                name,
                arguments,
                argumentsEnd,
                repeatablePrefix,
                repeatable,
                onPrefix,
                locations
            );
        });
    }

    private List<GraphQlRightPadded<GraphQl.DirectiveLocationValue>> parseDirectiveLocations(GraphQLParser.DirectiveLocationsContext ctx) {
        List<GraphQlRightPadded<GraphQl.DirectiveLocationValue>> locations = new ArrayList<>();
        
        // The grammar is: directiveLocations : '|'? directiveLocation | directiveLocations '|' directiveLocation
        // This is right-recursive, so we need to process it in the correct order
        
        // Collect all directive location contexts in order
        List<GraphQLParser.DirectiveLocationContext> locationContexts = new ArrayList<>();
        GraphQLParser.DirectiveLocationsContext current = ctx;
        
        while (current != null) {
            // Add the directiveLocation from this context if it exists
            if (current.directiveLocation() != null) {
                locationContexts.add(0, current.directiveLocation()); // Add at beginning to reverse the order
            }
            // Move to the nested directiveLocations if it exists
            current = current.directiveLocations();
        }
        
        // Check for leading pipe before first location
        int startCursor = cursor;
        whitespace();
        boolean hasLeadingPipe = cursor < source.length() && source.charAt(cursor) == '|';
        if (hasLeadingPipe) {
            cursor++; // consume the pipe
            whitespace();
        }
        
        // For the first location, capture the space after "on" (including any leading pipe)
        Space firstLocationPrefix = Space.EMPTY;
        if (!locationContexts.isEmpty()) {
            // Always capture from startCursor to preserve the space after "on"
            firstLocationPrefix = Space.format(source.substring(startCursor, cursor));
        }
        
        // Now process them in order
        for (int i = 0; i < locationContexts.size(); i++) {
            GraphQLParser.DirectiveLocationContext locCtx = locationContexts.get(i);
            
            Space locationPrefix;
            if (i == 0) {
                // Use the prefix that includes space after "on" (and any leading pipe)
                locationPrefix = firstLocationPrefix;
            } else {
                locationPrefix = prefix(locCtx);
            }
            
            GraphQl.DirectiveLocation enumValue = parseDirectiveLocation(locCtx);
            GraphQl.DirectiveLocationValue location = new GraphQl.DirectiveLocationValue(
                UUID.randomUUID(),
                locationPrefix,
                Markers.EMPTY,
                enumValue
            );
            
            // Capture whitespace and pipe after this location
            Space after = Space.EMPTY;
            if (i < locationContexts.size() - 1) {
                // Not the last location - capture whitespace and pipe
                int startPos = cursor;
                whitespace();
                
                // Look for pipe separator
                if (cursor < source.length() && source.charAt(cursor) == '|') {
                    cursor++; // consume the pipe
                    whitespace(); // and any space after it
                }
                
                after = Space.format(source.substring(startPos, cursor));
            }
            
            locations.add(new GraphQlRightPadded<>(location, after, Markers.EMPTY));
        }
        
        return locations;
    }

    private GraphQl.DirectiveLocation parseDirectiveLocation(GraphQLParser.DirectiveLocationContext ctx) {
        String location = ctx.getText().toUpperCase();
        
        // Advance cursor past the location text
        advanceCursor(ctx.getStop().getStopIndex() + 1);
        
        try {
            return GraphQl.DirectiveLocation.valueOf(location);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown directive location: " + location);
        }
    }

    @Override
    public GraphQl visitTypeSystemExtension(GraphQLParser.TypeSystemExtensionContext ctx) {
        if (ctx.schemaExtension() != null) {
            return visit(ctx.schemaExtension());
        } else if (ctx.typeExtension() != null) {
            return visit(ctx.typeExtension());
        }
        throw new IllegalStateException("Unknown type system extension");
    }

    @Override
    public GraphQl visitSchemaExtension(GraphQLParser.SchemaExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("schema");
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.RootOperationTypeDefinition> operationTypes = null;
            Space operationTypesPrefix = null;
            if (!c.rootOperationTypeDefinition().isEmpty()) {
                operationTypesPrefix = sourceBefore("{");
                operationTypes = new ArrayList<>();
                for (GraphQLParser.RootOperationTypeDefinitionContext opType : c.rootOperationTypeDefinition()) {
                    operationTypes.add(visitRootOperationTypeDefinition(opType));
                }
                sourceBefore("}");
            }
            
            return new GraphQl.SchemaExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                directives,
                operationTypesPrefix,
                operationTypes
            );
        });
    }

    @Override
    public GraphQl visitTypeExtension(GraphQLParser.TypeExtensionContext ctx) {
        if (ctx.scalarTypeExtension() != null) {
            return visit(ctx.scalarTypeExtension());
        } else if (ctx.objectTypeExtension() != null) {
            return visit(ctx.objectTypeExtension());
        } else if (ctx.interfaceTypeExtension() != null) {
            return visit(ctx.interfaceTypeExtension());
        } else if (ctx.unionTypeExtension() != null) {
            return visit(ctx.unionTypeExtension());
        } else if (ctx.enumTypeExtension() != null) {
            return visit(ctx.enumTypeExtension());
        } else if (ctx.inputObjectTypeExtension() != null) {
            return visit(ctx.inputObjectTypeExtension());
        }
        throw new IllegalStateException("Unknown type extension");
    }

    @Override
    public GraphQl visitScalarTypeExtension(GraphQLParser.ScalarTypeExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("scalar");
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            return new GraphQl.ScalarTypeExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                name,
                directives
            );
        });
    }

    @Override
    public GraphQl visitObjectTypeExtension(GraphQLParser.ObjectTypeExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("type");
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQlRightPadded<GraphQl.NamedType>> implementsInterfaces = null;
            Space implementsPrefix = null;
            if (c.implementsInterfaces() != null) {
                ImplementsInterfacesResult result = parseImplementsInterfaces(c.implementsInterfaces());
                implementsPrefix = result.implementsPrefix;
                implementsInterfaces = result.interfaces;
            }
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.FieldDefinition> fields = null;
            Space fieldsPrefix = null;
            if (c.fieldsDefinition() != null) {
                FieldsDefinitionResult result = parseFieldsDefinition(c.fieldsDefinition());
                fieldsPrefix = result.prefix;
                fields = result.fields;
            }
            
            return new GraphQl.ObjectTypeExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                name,
                implementsPrefix,
                implementsInterfaces,
                directives,
                fieldsPrefix,
                fields
            );
        });
    }

    @Override
    public GraphQl visitInterfaceTypeExtension(GraphQLParser.InterfaceTypeExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("interface");
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQlRightPadded<GraphQl.NamedType>> implementsInterfaces = null;
            Space implementsPrefix = null;
            if (c.implementsInterfaces() != null) {
                ImplementsInterfacesResult result = parseImplementsInterfaces(c.implementsInterfaces());
                implementsPrefix = result.implementsPrefix;
                implementsInterfaces = result.interfaces;
            }
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.FieldDefinition> fields = null;
            Space fieldsPrefix = null;
            if (c.fieldsDefinition() != null) {
                FieldsDefinitionResult result = parseFieldsDefinition(c.fieldsDefinition());
                fieldsPrefix = result.prefix;
                fields = result.fields;
            }
            
            return new GraphQl.InterfaceTypeExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                name,
                implementsPrefix,
                implementsInterfaces,
                directives,
                fieldsPrefix,
                fields
            );
        });
    }

    @Override
    public GraphQl visitUnionTypeExtension(GraphQLParser.UnionTypeExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("union");
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQlRightPadded<GraphQl.NamedType>> memberTypes = null;
            Space memberTypesPrefix = null;
            if (c.unionMemberTypes() != null) {
                UnionMemberTypesResult result = parseUnionMemberTypes(c.unionMemberTypes());
                memberTypesPrefix = result.equalsPrefix;
                memberTypes = result.memberTypes;
            }
            
            return new GraphQl.UnionTypeExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                name,
                directives,
                memberTypesPrefix,
                memberTypes
            );
        });
    }

    @Override
    public GraphQl visitEnumTypeExtension(GraphQLParser.EnumTypeExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("enum");
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.EnumValueDefinition> values = null;
            if (c.enumValuesDefinition() != null) {
                values = parseEnumValuesDefinition(c.enumValuesDefinition());
            }
            
            return new GraphQl.EnumTypeExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                name,
                directives,
                values
            );
        });
    }

    @Override
    public GraphQl visitInputObjectTypeExtension(GraphQLParser.InputObjectTypeExtensionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("extend");
            Space typeKeywordPrefix = sourceBefore("input");
            
            GraphQl.Name name = visitName(c.name());
            
            List<GraphQl.Directive> directives = null;
            if (c.directives() != null) {
                directives = parseDirectives(c.directives());
            }
            
            List<GraphQl.InputValueDefinition> fields = null;
            if (c.inputFieldsDefinition() != null) {
                fields = parseInputFieldsDefinition(c.inputFieldsDefinition());
            }
            
            return new GraphQl.InputObjectTypeExtension(
                UUID.randomUUID(),
                prefix,
                Markers.EMPTY,
                typeKeywordPrefix,
                name,
                directives,
                fields
            );
        });
    }

    // Utility methods
    private int advanceCursor(int newIndex) {
        // ANTLR provides character indices based on Java's char array indexing
        cursor = newIndex;
        return cursor;
    }

    private Space prefix(Token token) {
        if (token == null) {
            return Space.EMPTY;
        }
        int start = token.getStartIndex();
        if (start < cursor) {
            return Space.EMPTY;
        }
        return Space.format(source, cursor, advanceCursor(start));
    }

    private Space prefix(ParserRuleContext ctx) {
        if (ctx == null || ctx.start == null) {
            return Space.EMPTY;
        }
        return prefix(ctx.start);
    }

    private <C extends ParserRuleContext, T> T convert(C ctx, java.util.function.BiFunction<C, Space, T> conversion) {
        if (ctx == null) {
            return null;
        }
        
        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.stop != null) {
            int targetIndex = ctx.stop.getStopIndex() + 1;
            // Don't advance if we're already past this point
            if (targetIndex > cursor) {
                advanceCursor(targetIndex);
            }
        }
        
        return t;
    }
    
    private <T> T convert(TerminalNode node, java.util.function.BiFunction<TerminalNode, Space, T> conversion) {
        if (node == null) {
            return null;
        }
        
        T t = conversion.apply(node, prefix(node.getSymbol()));
        int targetIndex = node.getSymbol().getStopIndex() + 1;
        // Don't advance if we're already past this point
        if (targetIndex > cursor) {
            advanceCursor(targetIndex);
        }
        return t;
    }
    
    private Space whitespace() {
        int start = cursor;
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return Space.format(source.substring(start, cursor));
    }

    private Space sourceBefore(String untilDelim) {
        int delimIndex = source.indexOf(untilDelim, cursor);
        if (delimIndex < 0) {
            return Space.EMPTY;
        }
        
        Space space = Space.format(source, cursor, delimIndex);
        cursor = delimIndex + untilDelim.length();
        
        return space;
    }
    
    private void skip(String expected) {
        whitespace();
        if (cursor + expected.length() <= source.length() && 
            source.substring(cursor, cursor + expected.length()).equals(expected)) {
            cursor += expected.length();
        }
    }

    private String unescapeString(String str) {
        // Process escape sequences in order, but preserve Unicode escapes
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '\"':
                        result.append('\"');
                        i += 2;
                        break;
                    case '\\':
                        result.append('\\');
                        i += 2;
                        break;
                    case 'b':
                        result.append('\b');
                        i += 2;
                        break;
                    case 'f':
                        result.append('\f');
                        i += 2;
                        break;
                    case 'n':
                        result.append('\n');
                        i += 2;
                        break;
                    case 'r':
                        result.append('\r');
                        i += 2;
                        break;
                    case 't':
                        result.append('\t');
                        i += 2;
                        break;
                    case 'u':
                        // Unicode escape - convert to actual character
                        if (i + 5 < str.length()) {
                            String hex = str.substring(i + 2, i + 6);
                            try {
                                int codePoint = Integer.parseInt(hex, 16);
                                result.append((char) codePoint);
                                i += 6;
                            } catch (NumberFormatException e) {
                                // Invalid Unicode escape, keep as-is
                                result.append('\\');
                                i++;
                            }
                        } else {
                            // Not enough characters for Unicode escape
                            result.append('\\');
                            i++;
                        }
                        break;
                    default:
                        // Unknown escape, keep as-is
                        result.append('\\');
                        i++;
                        break;
                }
            } else {
                result.append(str.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}
