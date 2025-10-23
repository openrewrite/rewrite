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
import org.openrewrite.*;
import org.openrewrite.graphql.GraphQlIsoVisitor;
import org.openrewrite.graphql.GraphQlVisitor;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindTypeUsages extends Recipe {
    
    @Option(displayName = "Type name",
            description = "The name of the type to find usages for.",
            example = "User")
    String typeName;

    @Override
    public String getDisplayName() {
        return "Find GraphQL type usages";
    }

    @Override
    public String getDescription() {
        return "Find all usages of a specific type in GraphQL schema and queries.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GraphQlIsoVisitor<ExecutionContext>() {
            @Override
            public GraphQl.NamedType visitNamedType(GraphQl.NamedType namedType, ExecutionContext ctx) {
                GraphQl.NamedType n = super.visitNamedType(namedType, ctx);
                if (n.getName().getValue().equals(typeName)) {
                    return SearchResult.found(n);
                }
                return n;
            }

            @Override
            public GraphQl.ObjectTypeDefinition visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectType, ExecutionContext ctx) {
                GraphQl.ObjectTypeDefinition o = super.visitObjectTypeDefinition(objectType, ctx);
                // Mark the type definition itself if it matches
                if (o.getName().getValue().equals(typeName)) {
                    return o.withName(SearchResult.found(o.getName()));
                }
                return o;
            }

            @Override
            public GraphQl.InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceType, ExecutionContext ctx) {
                GraphQl.InterfaceTypeDefinition i = super.visitInterfaceTypeDefinition(interfaceType, ctx);
                if (i.getName().getValue().equals(typeName)) {
                    return i.withName(SearchResult.found(i.getName()));
                }
                return i;
            }

            @Override
            public GraphQl.UnionTypeDefinition visitUnionTypeDefinition(GraphQl.UnionTypeDefinition unionType, ExecutionContext ctx) {
                GraphQl.UnionTypeDefinition u = super.visitUnionTypeDefinition(unionType, ctx);
                if (u.getName().getValue().equals(typeName)) {
                    return u.withName(SearchResult.found(u.getName()));
                }
                return u;
            }

            @Override
            public GraphQl.EnumTypeDefinition visitEnumTypeDefinition(GraphQl.EnumTypeDefinition enumType, ExecutionContext ctx) {
                GraphQl.EnumTypeDefinition e = super.visitEnumTypeDefinition(enumType, ctx);
                if (e.getName().getValue().equals(typeName)) {
                    return e.withName(SearchResult.found(e.getName()));
                }
                return e;
            }

            @Override
            public GraphQl.InputObjectTypeDefinition visitInputObjectTypeDefinition(GraphQl.InputObjectTypeDefinition inputObjectType, ExecutionContext ctx) {
                GraphQl.InputObjectTypeDefinition i = super.visitInputObjectTypeDefinition(inputObjectType, ctx);
                if (i.getName().getValue().equals(typeName)) {
                    return i.withName(SearchResult.found(i.getName()));
                }
                return i;
            }

            @Override
            public GraphQl.ScalarTypeDefinition visitScalarTypeDefinition(GraphQl.ScalarTypeDefinition scalarType, ExecutionContext ctx) {
                GraphQl.ScalarTypeDefinition s = super.visitScalarTypeDefinition(scalarType, ctx);
                if (s.getName().getValue().equals(typeName)) {
                    return s.withName(SearchResult.found(s.getName()));
                }
                return s;
            }


            @Override
            public GraphQl.FragmentDefinition visitFragmentDefinition(GraphQl.FragmentDefinition fragmentDef, ExecutionContext ctx) {
                GraphQl.FragmentDefinition f = super.visitFragmentDefinition(fragmentDef, ctx);
                // Check if fragment is on the target type
                if (f.getTypeCondition().getName().getValue().equals(typeName)) {
                    return f.withTypeCondition(SearchResult.found(f.getTypeCondition()));
                }
                return f;
            }




        };
    }

    /**
     * Find all usages of a type in a GraphQL document.
     * @param document The GraphQL document to search
     * @param typeName The name of the type to find
     * @return A set of GraphQL elements that reference or define the type
     */
    public static Set<GraphQl> find(GraphQl document, String typeName) {
        Set<GraphQl> results = new HashSet<>();
        new GraphQlIsoVisitor<Set<GraphQl>>() {
            @Override
            public GraphQl.NamedType visitNamedType(GraphQl.NamedType namedType, Set<GraphQl> results) {
                GraphQl.NamedType n = super.visitNamedType(namedType, results);
                if (n.getName().getValue().equals(typeName)) {
                    results.add(n);
                }
                return n;
            }

            @Override
            public GraphQl.ObjectTypeDefinition visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectType, Set<GraphQl> results) {
                GraphQl.ObjectTypeDefinition o = super.visitObjectTypeDefinition(objectType, results);
                if (o.getName().getValue().equals(typeName)) {
                    results.add(o);
                }
                return o;
            }

            @Override
            public GraphQl.InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceType, Set<GraphQl> results) {
                GraphQl.InterfaceTypeDefinition i = super.visitInterfaceTypeDefinition(interfaceType, results);
                if (i.getName().getValue().equals(typeName)) {
                    results.add(i);
                }
                return i;
            }

            @Override
            public GraphQl.UnionTypeDefinition visitUnionTypeDefinition(GraphQl.UnionTypeDefinition unionType, Set<GraphQl> results) {
                GraphQl.UnionTypeDefinition u = super.visitUnionTypeDefinition(unionType, results);
                if (u.getName().getValue().equals(typeName)) {
                    results.add(u);
                }
                return u;
            }

            @Override
            public GraphQl.EnumTypeDefinition visitEnumTypeDefinition(GraphQl.EnumTypeDefinition enumType, Set<GraphQl> results) {
                GraphQl.EnumTypeDefinition e = super.visitEnumTypeDefinition(enumType, results);
                if (e.getName().getValue().equals(typeName)) {
                    results.add(e);
                }
                return e;
            }

            @Override
            public GraphQl.InputObjectTypeDefinition visitInputObjectTypeDefinition(GraphQl.InputObjectTypeDefinition inputObjectType, Set<GraphQl> results) {
                GraphQl.InputObjectTypeDefinition i = super.visitInputObjectTypeDefinition(inputObjectType, results);
                if (i.getName().getValue().equals(typeName)) {
                    results.add(i);
                }
                return i;
            }

            @Override
            public GraphQl.ScalarTypeDefinition visitScalarTypeDefinition(GraphQl.ScalarTypeDefinition scalarType, Set<GraphQl> results) {
                GraphQl.ScalarTypeDefinition s = super.visitScalarTypeDefinition(scalarType, results);
                if (s.getName().getValue().equals(typeName)) {
                    results.add(s);
                }
                return s;
            }

            @Override
            public GraphQl.FragmentDefinition visitFragmentDefinition(GraphQl.FragmentDefinition fragmentDef, Set<GraphQl> results) {
                GraphQl.FragmentDefinition f = super.visitFragmentDefinition(fragmentDef, results);
                if (f.getTypeCondition().getName().getValue().equals(typeName)) {
                    results.add(f.getTypeCondition());
                }
                return f;
            }

            @Override
            public GraphQl.InlineFragment visitInlineFragment(GraphQl.InlineFragment inlineFragment, Set<GraphQl> results) {
                GraphQl.InlineFragment i = super.visitInlineFragment(inlineFragment, results);
                if (i.getTypeCondition() != null && i.getTypeCondition().getName().getValue().equals(typeName)) {
                    results.add(i.getTypeCondition());
                }
                return i;
            }

        }.visit(document, results);
        return results;
    }
}