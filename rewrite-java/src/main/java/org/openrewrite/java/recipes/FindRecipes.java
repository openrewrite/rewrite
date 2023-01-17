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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.RewriteRecipeSource;

import static java.util.Objects.requireNonNull;

public class FindRecipes extends Recipe {
    RewriteRecipeSource recipeSource = new RewriteRecipeSource(this);

    @Override
    public String getDisplayName() {
        return "Find OpenRewrite recipes";
    }

    @Override
    public String getDescription() {
        return "This recipe finds all OpenRewrite recipes, primarily to produce a data table that is being used " +
               "to experiment with fine-tuning a large language model to produce more recipes.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("org.openrewrite.Recipe");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher getDisplayName = new MethodMatcher("org.openrewrite.Recipe getDisplayName()", true);
        MethodMatcher getDescription = new MethodMatcher("org.openrewrite.Recipe getDescription()", true);

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", classDecl.getType())) {
                    recipeSource.insertRow(ctx, new RewriteRecipeSource.Row(
                            getCursor().getMessage("displayName"),
                            getCursor().getMessage("description"),
                            RewriteRecipeSource.RecipeType.Java,
                            getCursor().firstEnclosingOrThrow(J.CompilationUnit.class).printAllTrimmed()
                    ));
                    return classDecl.withName(SearchResult.found(classDecl.getName()));
                }
                return cd;
            }

            @Override
            public J.Return visitReturn(J.Return aReturn, ExecutionContext ctx) {
                J.MethodDeclaration method = getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class);
                if (getDisplayName.matches(method.getMethodType()) && aReturn.getExpression() instanceof J.Literal) {
                    getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "displayName",
                            requireNonNull(((J.Literal) aReturn.getExpression()).getValue()));
                }
                if (getDescription.matches(method.getMethodType()) && aReturn.getExpression() instanceof J.Literal) {
                    getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "description",
                            requireNonNull(((J.Literal) aReturn.getExpression()).getValue()));
                }
                return super.visitReturn(aReturn, ctx);
            }
        };
    }
}
