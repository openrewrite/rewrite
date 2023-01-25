/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.RecipeSourceCode;

import static java.util.Objects.requireNonNull;

public class FindRecipes extends Recipe {
    private final transient RecipeSourceCode recipeSource = new RecipeSourceCode(this);

    @Override
    public String getDisplayName() {
        return "A recipe to find recipes";
    }

    @Override
    public String getDescription() {
        return "I heard you like recipes, so I wrote a recipe to help you find recipes.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", c.getType())) {
                    String displayName = null;
                    String description = null;

                    for (Statement statement : classDecl.getBody().getStatements()) {
                        if (statement instanceof J.MethodDeclaration) {
                            J.MethodDeclaration method = (J.MethodDeclaration) statement;
                            if (method.getSimpleName().equals("getDisplayName")) {
                                displayName = getLiteralReturn(method);
                            } else if (method.getSimpleName().equals("getDescription")) {
                                description = getLiteralReturn(method);
                            }
                        }
                    }

                    if(displayName != null && description != null) {
                        recipeSource.insertRow(ctx, new RecipeSourceCode.Row(
                                requireNonNull(c.getType()).getFullyQualifiedName(),
                                displayName,
                                description,
                                getCursor().firstEnclosingOrThrow(J.CompilationUnit.class)
                                        .withPrefix(Space.EMPTY) // trim the license header
                                        .printAllTrimmed()
                        ));
                    }
                    return c.withName(SearchResult.found(c.getName()));
                }
                return c;
            }

            @Nullable
            private String getLiteralReturn(J.MethodDeclaration methodDeclaration) {
                if (methodDeclaration.getBody() == null) {
                    return null;
                }
                for (Statement statement : methodDeclaration.getBody().getStatements()) {
                    if (statement instanceof J.Return) {
                        J.Return r = (J.Return) statement;
                        if (r.getExpression() instanceof J.Literal) {
                            return requireNonNull(((J.Literal) r.getExpression()).getValue()).toString();
                        }
                    }
                }
                return null;
            }
        };
    }
}
