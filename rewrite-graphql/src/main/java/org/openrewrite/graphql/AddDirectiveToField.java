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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.graphql.GraphQlIsoVisitor;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.graphql.tree.GraphQlRightPadded;
import org.openrewrite.graphql.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
@With
public class AddDirectiveToField extends Recipe {
    
    @JsonCreator
    public AddDirectiveToField(
        @JsonProperty("typeName") @Nullable String typeName,
        @JsonProperty("fieldName") String fieldName,
        @JsonProperty("directive") String directive,
        @JsonProperty("overrideExisting") @Nullable Boolean overrideExisting
    ) {
        this.typeName = typeName;
        this.fieldName = fieldName;
        this.directive = directive;
        this.overrideExisting = overrideExisting;
    }
    
    // Constructor for backward compatibility
    public AddDirectiveToField(@Nullable String typeName, String fieldName, String directive) {
        this(typeName, fieldName, directive, null);
    }

    @Option(displayName = "Type name",
            description = "The name of the type containing the field. If null, the directive will be added to all fields with the specified name.",
            example = "User",
            required = false)
    @Nullable
    String typeName;

    @Option(displayName = "Field name",
            description = "The name of the field to add the directive to.",
            example = "email")
    String fieldName;

    @Option(displayName = "Directive",
            description = "The directive to add, including the @ symbol and any arguments.",
            example = "@auth(role: ADMIN)")
    String directive;

    @Option(displayName = "Override existing",
            description = "If true, override any existing directive with the same name. If false (default), skip fields that already have the directive.",
            example = "false",
            required = false)
    @Nullable
    Boolean overrideExisting;

    @Override
    public String getDisplayName() {
        return "Add directive to field";
    }

    @Override
    public String getDescription() {
        return "Add a directive to a specific field in a GraphQL schema.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GraphQlIsoVisitor<ExecutionContext>() {
            @Override
            public GraphQl.ObjectTypeDefinition visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectTypeDef, ExecutionContext ctx) {
                GraphQl.ObjectTypeDefinition o = super.visitObjectTypeDefinition(objectTypeDef, ctx);
                
                if (shouldProcessType(o.getName().getValue())) {
                    return o.withFields(ListUtils.map(o.getFields(), field -> processField(field)));
                }
                
                return o;
            }

            @Override
            public GraphQl.InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceTypeDef, ExecutionContext ctx) {
                GraphQl.InterfaceTypeDefinition i = super.visitInterfaceTypeDefinition(interfaceTypeDef, ctx);
                
                if (shouldProcessType(i.getName().getValue())) {
                    return i.withFields(ListUtils.map(i.getFields(), field -> processField(field)));
                }
                
                return i;
            }

            @Override
            public GraphQl.ObjectTypeExtension visitObjectTypeExtension(GraphQl.ObjectTypeExtension objectTypeExt, ExecutionContext ctx) {
                GraphQl.ObjectTypeExtension o = super.visitObjectTypeExtension(objectTypeExt, ctx);
                
                if (shouldProcessType(o.getName().getValue())) {
                    return o.withFields(ListUtils.map(o.getFields(), field -> processField(field)));
                }
                
                return o;
            }

            @Override
            public GraphQl.InterfaceTypeExtension visitInterfaceTypeExtension(GraphQl.InterfaceTypeExtension interfaceTypeExt, ExecutionContext ctx) {
                GraphQl.InterfaceTypeExtension i = super.visitInterfaceTypeExtension(interfaceTypeExt, ctx);
                
                if (shouldProcessType(i.getName().getValue())) {
                    return i.withFields(ListUtils.map(i.getFields(), field -> processField(field)));
                }
                
                return i;
            }

            private boolean shouldProcessType(String currentTypeName) {
                return typeName == null || typeName.equals(currentTypeName);
            }

            private GraphQl.FieldDefinition processField(GraphQl.FieldDefinition field) {
                if (!field.getName().getValue().equals(fieldName)) {
                    return field;
                }

                // Check if the directive already exists
                String directiveName = extractDirectiveName(directive);
                int existingIndex = findDirectiveIndex(field.getDirectives(), directiveName);
                
                if (existingIndex >= 0 && !Boolean.TRUE.equals(overrideExisting)) {
                    // Directive exists and we're not overriding
                    return field;
                }

                // Parse the directive string
                GraphQl.Directive newDirective = parseDirective(directive);
                List<GraphQl.Directive> directives = field.getDirectives();
                
                if (directives == null) {
                    directives = new ArrayList<>();
                    directives.add(newDirective);
                } else {
                    directives = new ArrayList<>(directives);
                    if (existingIndex >= 0) {
                        // Replace existing directive, preserving its prefix
                        GraphQl.Directive existing = directives.get(existingIndex);
                        directives.set(existingIndex, newDirective.withPrefix(existing.getPrefix()));
                    } else {
                        // Add new directive
                        directives.add(newDirective);
                    }
                }
                
                return field.withDirectives(directives);
            }

            private String extractDirectiveName(String directiveString) {
                String trimmed = directiveString.trim();
                if (!trimmed.startsWith("@")) {
                    throw new IllegalArgumentException("Directive must start with @");
                }
                
                int endIndex = trimmed.length();
                for (int i = 1; i < trimmed.length(); i++) {
                    char c = trimmed.charAt(i);
                    if (c == '(' || c == ' ') {
                        endIndex = i;
                        break;
                    }
                }
                
                return trimmed.substring(1, endIndex);
            }

            private int findDirectiveIndex(List<GraphQl.Directive> directives, String name) {
                if (directives == null) {
                    return -1;
                }
                
                for (int i = 0; i < directives.size(); i++) {
                    if (directives.get(i).getName().getValue().equals(name)) {
                        return i;
                    }
                }
                
                return -1;
            }

            private GraphQl.Directive parseDirective(String directiveString) {
                String trimmed = directiveString.trim();
                if (!trimmed.startsWith("@")) {
                    throw new IllegalArgumentException("Directive must start with @");
                }
                
                // Create a temporary GraphQL fragment to parse the directive
                String tempQuery = "type Temp { field: String " + trimmed + " }";
                
                GraphQlParser parser = GraphQlParser.builder().build();
                Object parsed = parser.parse(tempQuery)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Failed to parse directive: " + directiveString));
                
                GraphQl.Document doc;
                if (parsed instanceof org.openrewrite.tree.ParseError) {
                    org.openrewrite.tree.ParseError error = (org.openrewrite.tree.ParseError) parsed;
                    if (error.getErroneous() instanceof GraphQl.Document) {
                        doc = (GraphQl.Document) error.getErroneous();
                    } else {
                        throw new IllegalArgumentException("Failed to parse directive: " + directiveString);
                    }
                } else {
                    doc = (GraphQl.Document) parsed;
                }
                
                GraphQl.ObjectTypeDefinition typeDef = (GraphQl.ObjectTypeDefinition) doc.getDefinitions().get(0);
                GraphQl.FieldDefinition fieldDef = typeDef.getFields().get(0);
                
                if (fieldDef.getDirectives() == null || fieldDef.getDirectives().isEmpty()) {
                    throw new IllegalArgumentException("Failed to parse directive: " + directiveString);
                }
                
                // Get the parsed directive and adjust its prefix
                GraphQl.Directive parsedDirective = fieldDef.getDirectives().get(0);
                return parsedDirective.withPrefix(Space.format(" "));
            }

        };
    }
}