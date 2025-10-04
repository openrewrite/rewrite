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
package org.openrewrite.graphql.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.graphql.GraphQlIsoVisitor;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.marker.SearchResult;

import java.util.regex.Pattern;

/**
 * Finds GraphQL field definitions that do not have a specified directive.
 * This is useful for auditing schemas to ensure all fields have required directives
 * like @deprecated, @auth, @cost, etc.
 * <p>
 * Example usage:
 * <pre>
 * // Find all fields without @deprecated directive
 * rewriteRun(
 *   spec -> spec.recipe(new FindFieldsWithoutDirective("deprecated", null, null)),
 *   graphQl("""
 *     type User {
 *       id: ID!
 *       name: String @deprecated(reason: "Use fullName")
 *       email: String  # This will be found
 *     }
 *   """)
 * );
 * </pre>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindFieldsWithoutDirective extends Recipe {
    
    @Option(displayName = "Directive name",
            description = "The name of the directive to search for (without @ prefix)",
            example = "deprecated")
    String directiveName;
    
    @Option(displayName = "Type name pattern",
            description = "Only search in types matching this regex pattern. " +
                         "When not specified, all types will be searched.",
            required = false,
            example = ".*Query")
    @Nullable
    String typePattern;
    
    @Option(displayName = "Field name pattern",
            description = "Only search fields matching this regex pattern. " +
                         "When not specified, all fields will be searched.",
            required = false,
            example = "get.*")
    @Nullable
    String fieldPattern;

    @Override
    public String getDisplayName() {
        return "Find fields without directive";
    }

    @Override
    public String getDescription() {
        return "Find GraphQL field definitions that do not have a specified directive.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern typeRegex = typePattern != null ? Pattern.compile(typePattern) : null;
        Pattern fieldRegex = fieldPattern != null ? Pattern.compile(fieldPattern) : null;
        
        return new GraphQlIsoVisitor<ExecutionContext>() {
            private String currentTypeName = null;

            @Override
            public GraphQl.ObjectTypeDefinition visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectType, ExecutionContext ctx) {
                String previousTypeName = currentTypeName;
                currentTypeName = objectType.getName().getValue();
                GraphQl.ObjectTypeDefinition result = super.visitObjectTypeDefinition(objectType, ctx);
                currentTypeName = previousTypeName;
                return result;
            }

            @Override
            public GraphQl.InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceType, ExecutionContext ctx) {
                String previousTypeName = currentTypeName;
                currentTypeName = interfaceType.getName().getValue();
                GraphQl.InterfaceTypeDefinition result = super.visitInterfaceTypeDefinition(interfaceType, ctx);
                currentTypeName = previousTypeName;
                return result;
            }

            @Override
            public GraphQl.ObjectTypeExtension visitObjectTypeExtension(GraphQl.ObjectTypeExtension extension, ExecutionContext ctx) {
                String previousTypeName = currentTypeName;
                currentTypeName = extension.getName().getValue();
                GraphQl.ObjectTypeExtension result = super.visitObjectTypeExtension(extension, ctx);
                currentTypeName = previousTypeName;
                return result;
            }

            @Override
            public GraphQl.InterfaceTypeExtension visitInterfaceTypeExtension(GraphQl.InterfaceTypeExtension extension, ExecutionContext ctx) {
                String previousTypeName = currentTypeName;
                currentTypeName = extension.getName().getValue();
                GraphQl.InterfaceTypeExtension result = super.visitInterfaceTypeExtension(extension, ctx);
                currentTypeName = previousTypeName;
                return result;
            }

            @Override
            public GraphQl.FieldDefinition visitFieldDefinition(GraphQl.FieldDefinition field, ExecutionContext ctx) {
                // Check if type name matches pattern (if specified)
                if (typeRegex != null && currentTypeName != null && !typeRegex.matcher(currentTypeName).matches()) {
                    return super.visitFieldDefinition(field, ctx);
                }

                // Check if field name matches pattern (if specified)
                if (fieldRegex != null && !fieldRegex.matcher(field.getName().getValue()).matches()) {
                    return super.visitFieldDefinition(field, ctx);
                }

                // Check if field has the specified directive
                boolean hasDirective = false;
                if (field.getDirectives() != null) {
                    for (GraphQl.Directive directive : field.getDirectives()) {
                        if (directiveName.equals(directive.getName().getValue())) {
                            hasDirective = true;
                            break;
                        }
                    }
                }

                // Mark field if it doesn't have the directive
                if (!hasDirective) {
                    return SearchResult.found(field);
                }

                return super.visitFieldDefinition(field, ctx);
            }
        };
    }
}