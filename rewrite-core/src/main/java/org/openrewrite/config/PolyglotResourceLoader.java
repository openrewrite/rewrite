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
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.openrewrite.Recipe;
import org.openrewrite.polyglot.PolyglotRecipe;
import org.openrewrite.polyglot.PolyglotUtils;
import org.openrewrite.style.NamedStyles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.polyglot.PolyglotUtils.maybeInstantiateOrInvoke;

public class PolyglotResourceLoader implements ResourceLoader {

    private static final ThreadLocal<Engine> ENGINES = new InheritableThreadLocal<Engine>() {
        @Override
        protected Engine initialValue() {
            return Engine.newBuilder()
                    .allowExperimentalOptions(true)
                    .build();
        }
    };

    private final List<PolyglotRecipes> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();
    private final List<RecipeExample> recipeExamples = new ArrayList<>();

    public PolyglotResourceLoader(Source... sources) {
        for (Source src : sources) {
            try {
                evalPolyglotRecipe(src.getName(), src);
            } catch (IOException e) {
                throw new RuntimeException(e);
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

    public void evalPolyglotRecipe(String name, Source src) throws IOException {
        String language = PolyglotUtils.getLanguage(src);

        recipes.add(new PolyglotRecipes(ctx -> {
            ctx.eval(src);
            Value bindings = ctx.getBindings(language);
            return bindings.getMemberKeys().stream()
                    .flatMap(exportName -> maybeInstantiateOrInvoke(bindings, exportName)
                            .map(exportVal -> exportVal.getMemberKeys().stream()
                                    .map(recipeName -> maybeInstantiateOrInvoke(exportVal, recipeName)
                                            .map(v -> new PolyglotRecipe(name + "/" + recipeName, v))))
                            .orElseGet(Stream::empty))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }));
    }

    @lombok.Value
    @RequiredArgsConstructor
    private static class PolyglotRecipes {
        ThreadLocal<Context> context = new InheritableThreadLocal<Context>() {
            @Override
            protected Context initialValue() {
                return Context.newBuilder()
                        .engine(ENGINES.get())
                        .allowAllAccess(true)
                        .allowExperimentalOptions(true)
                        .build();
            }
        };

        Function<Context, List<PolyglotRecipe>> recipes;

        ThreadLocal<List<PolyglotRecipe>> perThreadRecipes = new InheritableThreadLocal<List<PolyglotRecipe>>() {
            @Override
            protected List<PolyglotRecipe> initialValue() {
                return recipes.apply(context.get());
            }
        };

        public List<PolyglotRecipe> getRecipes() {
            return perThreadRecipes.get();
        }
    }

}
