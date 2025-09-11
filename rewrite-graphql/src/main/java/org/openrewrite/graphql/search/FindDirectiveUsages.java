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
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDirectiveUsages extends Recipe {
    
    @Option(displayName = "Directive name",
            description = "The name of the directive to find usages for (without the @ symbol).",
            example = "deprecated")
    String directiveName;

    @Override
    public String getDisplayName() {
        return "Find GraphQL directive usages";
    }

    @Override
    public String getDescription() {
        return "Find all usages of a specific directive in GraphQL schema and queries.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GraphQlIsoVisitor<ExecutionContext>() {
            @Override
            public GraphQl.Directive visitDirective(GraphQl.Directive directive, ExecutionContext ctx) {
                GraphQl.Directive d = super.visitDirective(directive, ctx);
                if (d.getName().getValue().equals(directiveName)) {
                    return SearchResult.found(d);
                }
                return d;
            }

            @Override
            public GraphQl.DirectiveDefinition visitDirectiveDefinition(GraphQl.DirectiveDefinition directiveDef, ExecutionContext ctx) {
                GraphQl.DirectiveDefinition d = super.visitDirectiveDefinition(directiveDef, ctx);
                // Mark the directive definition itself if it matches
                if (d.getName().getValue().equals(directiveName)) {
                    return SearchResult.found(d);
                }
                return d;
            }
        };
    }

    /**
     * Find all usages of a directive in a GraphQL document.
     * @param document The GraphQL document to search
     * @param directiveName The name of the directive to find (without @ symbol)
     * @return A set of GraphQL elements containing the directive usages
     */
    public static Set<GraphQl> find(GraphQl document, String directiveName) {
        Set<GraphQl> results = new HashSet<>();
        new GraphQlIsoVisitor<Set<GraphQl>>() {
            @Override
            public GraphQl.Directive visitDirective(GraphQl.Directive directive, Set<GraphQl> results) {
                GraphQl.Directive d = super.visitDirective(directive, results);
                if (d.getName().getValue().equals(directiveName)) {
                    results.add(d);
                }
                return d;
            }

            @Override
            public GraphQl.DirectiveDefinition visitDirectiveDefinition(GraphQl.DirectiveDefinition directiveDef, Set<GraphQl> results) {
                GraphQl.DirectiveDefinition d = super.visitDirectiveDefinition(directiveDef, results);
                if (d.getName().getValue().equals(directiveName)) {
                    results.add(d);
                }
                return d;
            }
        }.visit(document, results);
        return results;
    }
}