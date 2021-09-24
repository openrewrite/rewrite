/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.config;

import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Source;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.polyglot.Polyglot;
import org.openrewrite.polyglot.PolyglotParser;
import org.openrewrite.polyglot.PolyglotRecipe;
import org.openrewrite.polyglot.PolyglotVisitor;
import org.openrewrite.style.NamedStyles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PolyglotResourceLoader implements ResourceLoader {

    private final List<PolyglotRecipes> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();
    private final List<RecipeExample> recipeExamples = new ArrayList<>();

    public PolyglotResourceLoader(Source... sources) {
        for (Source src : sources) {
            try {
                evalPolyglotRecipe(src);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Override
    public Collection<Recipe> listRecipes() {
        return recipes.stream()
                .flatMap(rs -> rs.getRecipes().stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return recipeDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        return styles;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return categoryDescriptors;
    }

    @Override
    public Collection<RecipeExample> listRecipeExamples() {
        return recipeExamples;
    }

    public void evalPolyglotRecipe(Source moduleSrc) throws IOException {
        recipes.add(new PolyglotRecipes(moduleSrc));
        recipes.stream().flatMap(r -> r.getRecipes().stream())
                .forEach(r -> {
                    recipeDescriptors.add(r.getRecipeDescriptor());
                    categoryDescriptors.addAll(r.getCategoryDescriptors());
                    styles.addAll(r.getNamedStyles());
                });
    }

    @lombok.Value
    @RequiredArgsConstructor
    private static class PolyglotRecipes {
        PolyglotParser parser = new PolyglotParser();
        Source source;

        ThreadLocal<List<PolyglotRecipe>> perThreadRecipes = new InheritableThreadLocal<List<PolyglotRecipe>>() {
            @Override
            protected List<PolyglotRecipe> initialValue() {
                List<Polyglot.Source> sources = parser.parse(new InMemoryExecutionContext(), source);
                List<PolyglotRecipe> recipes = new ArrayList<>();

                PolyglotVisitor<List<PolyglotRecipe>> recipesVisitor = new PolyglotVisitor<List<PolyglotRecipe>>() {
                    @Override
                    public Polyglot visitInstantiable(Polyglot.Instantiable instantiable, List<PolyglotRecipe> l) {
                        Polyglot.Instance inst = instantiable.instantiate();
                        inst.as(PolyglotRecipe.class).ifPresent(l::add);
                        return inst;
                    }
                };
                for (Polyglot.Source src : sources) {
                    recipesVisitor.visitMembers(src.getMembers(), recipes);
                }

                return recipes;
            }
        };

        public List<PolyglotRecipe> getRecipes() {
            return perThreadRecipes.get();
        }
    }

}
