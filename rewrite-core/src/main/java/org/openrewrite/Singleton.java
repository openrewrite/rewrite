/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.SearchResult;

import java.util.HashMap;

public class Singleton extends Recipe {
    @Getter
    String displayName = "Singleton";

    @Language("markdown")
    @Getter
    String description =
            "Used as a precondition to ensure that a recipe attempts to make changes only once. " +
            "Accidentally including multiple copies/instances of the same large composite recipes is a common mistake. " +
            "If those recipes are marked with this precondition the performance penalty is limited. " +
            "This recipe does nothing useful run on its own.\n\n" +
            "## Usage in Java recipes\n\n" +
            "Wrap visitors with `Singleton.singleton(this, visitor)` to ensure only the first *equivalent* recipe instance makes changes:\n\n" +
            "```java\n" +
            "@Override\n" +
            "public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {\n" +
            "    return singleton(this, new TreeVisitor<Tree, ExecutionContext>() {\n" +
            "        @Override\n" +
            "        public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {\n" +
            "            // Your transformation logic\n" +
            "            return tree;\n" +
            "        }\n" +
            "    });\n" +
            "}\n" +
            "@Override\n" +
            "public Collection<SourceFile> generate(Accumulator acc, ExecutionContext ctx) {\n" +
            "    if (!isSingleton(this, ctx)) {\n" +
            "        return Collections.emptyList();\n" +
            "    }\n" +
            "    // Generate new sources\n" +
            "    return results;\n" +
            "}\n\n" +
            "@Override\n" +
            "public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {\n" +
            "    return singleton(this, new TreeVisitor<Tree, ExecutionContext>() {\n" +
            "        // Visitor logic\n" +
            "    });\n" +
            "}\n" +
            "```\n\n" +
            "**Note:** Singleton status is determined by the recipe's `equals()` and `hashCode()` methods. " +
            "If equivalent instances of a recipe are not considered singletons, ensure your recipe class correctly implements these methods. " +
            "The easiest way is to use Lombok's `@Value` annotation on your recipe class, which automatically " +
            "generates correct `equals()` and `hashCode()` implementations based on all fields.\n\n" +
            "## Usage in YAML recipes\n\n" +
            "Add `org.openrewrite.Singleton` as a precondition:\n\n" +
            "```yaml\n" +
            "---\n" +
            "type: specs.openrewrite.org/v1beta/recipe\n" +
            "name: com.example.Append\n" +
            "displayName: My recipe\n" +
            "preconditions:\n" +
            "  - org.openrewrite.Singleton\n" +
            "recipeList:\n" +
            "  - org.openrewrite.text.AppendToTextFile:\n" +
            "      relativeFileName: report.txt\n" +
            "      content: 'Recipe executed'\n" +
            "```";

    @Nullable Integer recipeIndex;

    public boolean isSingleton(ExecutionContext ctx) {
        if (recipeIndex == null) {
            recipeIndex = ctx.getCycleDetails().getRecipePosition();
        }
        return recipeIndex == ctx.getCycleDetails().getRecipePosition();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (isSingleton(ctx)) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        };
    }

    /**
     * Wrap the provided recipe in a Singleton precondition shared amongst all equivalent instances.
     * Recipes which do not override equals() or hashCode() get the default identity comparison and so will each get their own
     * Singleton, which defeats the purpose.
     */
    public static Singleton singleton(Recipe recipe, ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(Singleton.class.getName(), key -> new HashMap<Recipe, Singleton>())
                .computeIfAbsent(recipe, k -> new Singleton());
    }

    /**
     * Evaluate whether the recipe is allowed to make changes according to the singleton precondition.
     * Uses the recipe's equals()/hashCode() to identify equivalent instances and ensures only
     * the first instance encountered (based on recipe position) is allowed to make changes.
     *
     * @param recipe the recipe to check for uniqueness
     * @param ctx the execution context containing the recipe position and singleton instance cache
     * @return true if this is the first equivalent recipe instance and it should make changes, false otherwise
     */
    public static boolean isSingleton(Recipe recipe, ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(Singleton.class.getName(), key -> new HashMap<Recipe, Singleton>())
                .computeIfAbsent(recipe, k -> new Singleton())
                .isSingleton(ctx);
    }

    /**
     * Look up or create a Singleton instance for the provided recipe, using its equals()/hashCode() method to identify it,
     * and return a visitor wrapped in a precondition which ensures that only one gets to make changes in a recipe run.
     * @param recipe recipe to be made unique
     * @param treeVisitor the visitor to wrap in a singleton precondition
     * @return visitor wrapped in singleton precondition
     */
    public static TreeVisitor<?, ExecutionContext> singleton(Recipe recipe, TreeVisitor<?, ExecutionContext> treeVisitor) {
        return new SingletonDecoratedVisitor(recipe, treeVisitor);
    }

    @AllArgsConstructor
    public static class SingletonDecoratedVisitor extends TreeVisitor<Tree, ExecutionContext> {
        Recipe recipe;
        TreeVisitor<?, ExecutionContext> delegate;

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
            return delegate.isAcceptable(sourceFile, ctx);
        }

        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (isSingleton(recipe, ctx)) {
                return delegate.visit(tree, ctx);
            }
            return tree;
        }

        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
            if (isSingleton(recipe, ctx)) {
                return delegate.visit(tree, ctx, parent);
            }
            return tree;
        }
    }
}
