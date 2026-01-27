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
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.SearchResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.*;

public class Unique extends Recipe {
    @Override
    public String getDisplayName() {
        return "Unique";
    }

    @Override
    public String getDescription() {
        return "Used as a precondition to ensure that only one instance of a recipe makes changes. " +
                "Accidentally including multiple copies of the same large composite recipes is a common mistake. " +
                "A mistake that can be mitigated by use of this precondition." +
                "This does nothing useful when run on its own.";
    }

    @Nullable Integer recipeIndex;

    public boolean isAllowedToMakeChanges(ExecutionContext ctx) {
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
                if (isAllowedToMakeChanges(ctx)) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        };
    }

    private static final Map<Recipe, Recipe> recipeToDecorated = new HashMap<>();

    /**
     * Wrap the provided recipe in a Unique precondition shared amongst all equivalent instances.
     * Recipes which do not override equals() & hashCode() get the default identity comparison and so will each get their own
     * Unique, which defeats the purpose.
     * As most recipes are wrapped in @Value
     */
    public static Recipe decorate(Recipe recipe) {
        return recipeToDecorated.computeIfAbsent(recipe, k -> {
            if (recipe instanceof UniqueDecoratedRecipe || recipe instanceof UniqueDecoratedScanningRecipe) {
                return recipe;
            }
            if (recipe instanceof ScanningRecipe) {
                return new UniqueDecoratedScanningRecipe<>((ScanningRecipe<?>) recipe);
            }
            return new UniqueDecoratedRecipe(recipe);
        });
    }



    @AllArgsConstructor
    public static class UniqueDecoratedRecipe extends DecoratedRecipe {
        @Getter
        Recipe delegate;

        Unique unique;

        public UniqueDecoratedRecipe(Recipe delegate) {
            this.delegate = delegate;
            this.unique = new Unique();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(unique, delegate.getVisitor());
        }
    }

    @AllArgsConstructor
    public static class UniqueDecoratedScanningRecipe<T> extends DecoratedScanningRecipe<T> {
        @Getter
        ScanningRecipe<T> delegate;

        Unique unique;

        public UniqueDecoratedScanningRecipe(ScanningRecipe<T> delegate) {
            this.delegate = delegate;
            this.unique = new Unique();
        }


        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(T acc) {
            return Preconditions.check(unique, delegate.getScanner(acc));
        }

        @Override
        public Collection<? extends SourceFile> generate(T acc, ExecutionContext ctx) {
            if (unique.isAllowedToMakeChanges(ctx)) {
                return delegate.generate(acc, ctx);
            }
            return emptyList();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(T acc) {
            return Preconditions.check(unique, delegate.getVisitor(acc));
        }
    }
}
