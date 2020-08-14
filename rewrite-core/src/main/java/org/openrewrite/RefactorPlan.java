/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.config.*;
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class RefactorPlan {
    private static final Logger logger = LoggerFactory.getLogger(RefactorPlan.class);

    private final Map<String, Recipe> recipesByName;
    private final Collection<RefactorVisitor<?>> visitors;

    public RefactorPlan(Collection<Recipe> recipes, Collection<RefactorVisitor<?>> visitors) {
        this.recipesByName = recipes.stream().collect(toMap(Recipe::getName, identity()));
        this.visitors = visitors;
    }

    public <T extends Tree, R extends RefactorVisitor<T>> R configure(R visitor, String... recipes) {
        return configure(visitor, Arrays.asList(recipes));
    }

    public <T extends Tree, R extends RefactorVisitor<T>> R configure(R visitor, Iterable<String> recipes) {
        return loadedRecipes(recipes).stream()
                .reduce(visitor, (v2, recipe) -> recipe.configure(v2), (v1, v2) -> v1);
    }

    @Nullable
    public <S extends Style> S style(Class<S> styleClass, Iterable<String> recipes) {
        return loadedRecipes(recipes).stream()
                .map(recipe -> recipe.getStyles().stream()
                        .filter(styleClass::isInstance)
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(styleClass::cast)
                .findFirst()
                .orElse(null);
    }

    public Collection<RefactorVisitor<?>> visitors(String... recipes) {
        return visitors(Arrays.asList(recipes));
    }

    public Collection<RefactorVisitor<?>> visitors(Iterable<String> recipes) {
        List<Recipe> loadedRecipes = loadedRecipes(recipes);
        return visitors.stream()
                .map(v -> loadedRecipes.stream().reduce(v, (v2, recipe) -> recipe.configure(v2), (v1, v2) -> v1))
                .filter(v -> loadedRecipes.stream().anyMatch(p -> p.accept(v).equals(Recipe.FilterReply.ACCEPT)))
                .collect(toList());
    }

    private List<Recipe> loadedRecipes(Iterable<String> recipes) {
        return stream(recipes.spliterator(), false)
                .map(recipesByName::get)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, RecipeConfiguration> recipesConfigurations = new HashMap<>();
        private final Collection<RefactorVisitor<?>> visitors = new ArrayList<>();
        private Iterable<Path> compileClasspath = emptyList();

        public Builder compileClasspath(Iterable<Path> compileClasspath) {
            this.compileClasspath = emptyList();
            return this;
        }

        public Builder scanResources() {
            ClasspathResourceLoader classpathResourceLoader = new ClasspathResourceLoader(compileClasspath);
            loadVisitors(classpathResourceLoader);
            loadRecipes(classpathResourceLoader);
            return this;
        }

        public Builder scanUserHome() {
            File userHomeRewriteConfig = new File(System.getProperty("user.home") + "/.rewrite/rewrite.yml");
            if (userHomeRewriteConfig.exists()) {
                try (FileInputStream is = new FileInputStream(userHomeRewriteConfig)) {
                    YamlResourceLoader resourceLoader = new YamlResourceLoader(is);
                    loadVisitors(resourceLoader);
                    loadRecipes(resourceLoader);
                } catch (IOException e) {
                    logger.warn("Unable to load ~/.rewrite/rewrite.yml.", e);
                }
            }
            return this;
        }

        public Builder scanVisitors(String... acceptVisitorPackages) {
            visitors.addAll(new AutoConfigureRefactorVisitorLoader(acceptVisitorPackages).loadVisitors());
            return this;
        }

        public Builder loadVisitors(RefactorVisitorLoader refactorVisitorLoader) {
            visitors.addAll(refactorVisitorLoader.loadVisitors());
            return this;
        }

        public Builder loadVisitors(Collection<? extends RefactorVisitor<?>> visitors) {
            this.visitors.addAll(visitors);
            return this;
        }

        public Builder visitor(RefactorVisitor<?> visitor) {
            this.visitors.add(visitor);
            return this;
        }

        public Builder loadRecipes(RecipeConfigurationLoader recipeConfigurationLoader) {
            recipeConfigurationLoader.loadRecipes().forEach(this::loadRecipe);
            return this;
        }

        public Builder loadRecipe(RecipeConfiguration recipeConfiguration) {
            recipesConfigurations.compute(recipeConfiguration.getName(),
                    (name, existing) -> recipeConfiguration.merge(existing));
            return this;
        }

        public RefactorPlan build() {
            visitors.addAll(new AutoConfigureRefactorVisitorLoader("org.openrewrite").loadVisitors());

            return new RefactorPlan(recipesConfigurations.values().stream()
                    .map(pc -> pc.build(recipesConfigurations.values()))
                    .collect(toList()),
                    visitors);
        }
    }
}
