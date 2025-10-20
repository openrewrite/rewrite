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
package org.openrewrite.config;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeException;
import org.openrewrite.style.NamedStyles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.config.ResourceLoader.RecipeDetail.EXAMPLES;

public class Environment {
    private final Collection<? extends ResourceLoader> resourceLoaders;
    private final Collection<? extends ResourceLoader> dependencyResourceLoaders;

    public List<Recipe> listRecipes() {
        List<Recipe> dependencyRecipes = new ArrayList<>();
        for (ResourceLoader dependencyResourceLoader : dependencyResourceLoaders) {
            dependencyRecipes.addAll(dependencyResourceLoader.listRecipes());
        }
        Map<String, Recipe> dependencyRecipeMap = new HashMap<>();
        dependencyRecipes.forEach(r -> dependencyRecipeMap.putIfAbsent(r.getName(), r));

        Map<String, List<RecipeExample>> recipeExamples = new HashMap<>();
        for (ResourceLoader r : resourceLoaders) {
            if (r instanceof YamlResourceLoader) {
                recipeExamples.putAll(r.listRecipeExamples());
            }
        }

        List<Recipe> recipes = new ArrayList<>();
        for (ResourceLoader r : resourceLoaders) {
            recipes.addAll(r.listRecipes());
        }
        for (Recipe recipe : dependencyRecipes) {
            if (recipe instanceof DeclarativeRecipe) {
                ((DeclarativeRecipe) recipe).initialize(dependencyRecipeMap::get);
            }
        }

        Map<String, Recipe> availableRecipeMap = new HashMap<>(dependencyRecipeMap);
        recipes.forEach(r -> availableRecipeMap.putIfAbsent(r.getName(), r));

        for (Recipe recipe : recipes) {
            if (recipeExamples.containsKey(recipe.getName())) {
                recipe.setExamples(recipeExamples.get(recipe.getName()));
            }

            if (recipe instanceof DeclarativeRecipe) {
                ((DeclarativeRecipe) recipe).initialize(availableRecipeMap::get);
            }
        }
        return recipes;
    }

    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return resourceLoaders.stream()
                .flatMap(r -> r.listCategoryDescriptors().stream())
                .collect(toList());
    }

    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        Map<String, List<RecipeExample>> recipeToExamples = new HashMap<>();
        for (ResourceLoader r : resourceLoaders) {
            if (r instanceof YamlResourceLoader) {
                recipeToExamples.putAll(r.listRecipeExamples());
            } else if (r instanceof ClasspathScanningLoader) {
                ClasspathScanningLoader classpathScanningLoader = (ClasspathScanningLoader) r;
                Map<String, List<RecipeExample>> examplesMap = classpathScanningLoader.listRecipeExamples();
                for (String key : examplesMap.keySet()) {
                    if (recipeToExamples.containsKey(key)) {
                        recipeToExamples.get(key).addAll(examplesMap.get(key));
                    } else {
                        recipeToExamples.put(key, examplesMap.get(key));
                    }
                }
            }
        }

        List<RecipeDescriptor> result = new ArrayList<>();
        for (ResourceLoader r : resourceLoaders) {
            if (r instanceof YamlResourceLoader) {
                result.addAll((((YamlResourceLoader) r).listRecipeDescriptors(emptyList(), recipeToExamples)));
            } else {
                Collection<RecipeDescriptor> descriptors = r.listRecipeDescriptors();
                for (RecipeDescriptor descriptor : descriptors) {
                    if (recipeToExamples.containsKey(descriptor.getName())) {
                        descriptor.getExamples().addAll(recipeToExamples.get(descriptor.getName()));
                    }
                }
                result.addAll(descriptors);
            }
        }
        return result;
    }

    @Deprecated
    public Recipe activateRecipes(Iterable<String> activeRecipes) {
        List<String> asList = new ArrayList<>();
        for (String activeRecipe : activeRecipes) {
            asList.add(activeRecipe);
        }
        return activateRecipes(asList);
    }

    public Recipe activateRecipes(Collection<String> activeRecipes) {
        List<Recipe> recipes;
        if (activeRecipes.isEmpty()) {
            recipes = emptyList();
        } else if (activeRecipes.size() == 1) {
            Recipe recipe = loadRecipe(activeRecipes.iterator().next(), EXAMPLES);
            if (recipe != null) {
                return recipe;
            }
            recipes = listRecipes();
        } else {
            recipes = listRecipes();
        }

        Map<String, Recipe> recipesByName = recipes.stream().collect(toMap(Recipe::getName, identity()));
        List<String> recipesNotFound = new ArrayList<>();
        List<Recipe> activatedRecipes = new ArrayList<>();
        for (String activeRecipe : activeRecipes) {
            Recipe recipe = recipesByName.get(activeRecipe);
            if (recipe == null) {
                recipesNotFound.add(activeRecipe);
            } else {
                activatedRecipes.add(recipe);
            }
        }
        if (!recipesNotFound.isEmpty()) {
            @SuppressWarnings("deprecation")
            List<String> suggestions = recipesNotFound.stream()
                    .map(r -> recipesByName.keySet().stream()
                            .min(comparingInt(a -> LevenshteinDistance.getDefaultInstance().apply(a, r)))
                            .orElse(r))
                    .collect(toList());
            String message = String.format("Recipe(s) not found: %s\nDid you mean: %s",
                    String.join(", ", recipesNotFound),
                    String.join(", ", suggestions));
            throw new RecipeException(message);
        }
        if (activatedRecipes.isEmpty()) {
            return Recipe.noop();
        }
        if (activatedRecipes.size() == 1) {
            return activatedRecipes.get(0);
        }
        return new CompositeRecipe(activatedRecipes);
    }

    public Recipe activateRecipes(String... activeRecipes) {
        return activateRecipes(Arrays.asList(activeRecipes));
    }

    private @Nullable Recipe loadRecipe(String recipeName, ResourceLoader.RecipeDetail... details) {
        Recipe recipe = null;
        for (ResourceLoader loader : resourceLoaders) {
            recipe = loader.loadRecipe(recipeName, details);
            if (recipe != null) {
                break;
            }
        }
        if (recipe == null) {
            return null;
        }

        boolean includeExamples = ResourceLoader.RecipeDetail.EXAMPLES.includedIn(details);

        Map<String, List<RecipeExample>> recipeExamples = new HashMap<>();
        if (includeExamples) {
            for (ResourceLoader r : resourceLoaders) {
                if (r instanceof YamlResourceLoader) {
                    recipeExamples.putAll(r.listRecipeExamples());
                }
            }
        }

        Map<String, Recipe> dependencyRecipes = new HashMap<>();
        for (ResourceLoader dependencyResourceLoader : dependencyResourceLoaders) {
            for (Recipe listedRecipe : dependencyResourceLoader.listRecipes()) {
                dependencyRecipes.putIfAbsent(listedRecipe.getName(), listedRecipe);
            }
        }
        for (Recipe dependency : dependencyRecipes.values()) {
            if (dependency instanceof DeclarativeRecipe) {
                ((DeclarativeRecipe) dependency).initialize(dependencyRecipes::get);
            }
        }

        if (includeExamples && recipeExamples.containsKey(recipe.getName())) {
            recipe.setExamples(recipeExamples.get(recipe.getName()));
        }

        if (recipe instanceof DeclarativeRecipe) {
            Function<String, @Nullable Recipe> loadFunction = key -> {
                if (dependencyRecipes.containsKey(key)) {
                    return dependencyRecipes.get(key);
                }
                for (ResourceLoader resourceLoader : resourceLoaders) {
                    Recipe r = resourceLoader.loadRecipe(key, details);
                    if (r != null) {
                        return r;
                    }
                }
                return null;
            };
            ((DeclarativeRecipe) recipe).initialize(loadFunction);
        }
        return recipe;
    }

    /**
     * @return A list of validations of style names that could be activated.
     */
    public List<NamedStyles> listStyles() {
        return resourceLoaders.stream()
                .flatMap(r -> r.listStyles().stream())
                .collect(toList());
    }

    public List<NamedStyles> activateStyles(Iterable<String> activeStyles) {
        List<NamedStyles> activated = new ArrayList<>();
        List<NamedStyles> styles = listStyles();
        for (String activeStyle : activeStyles) {
            for (NamedStyles style : styles) {
                if (style.getName().equals(activeStyle)) {
                    activated.add(style);
                }
            }
        }
        return activated;
    }

    @SuppressWarnings("unused")
    public List<NamedStyles> activateStyles(String... activeStyles) {
        return activateStyles(Arrays.asList(activeStyles));
    }

    public Environment(Collection<? extends ResourceLoader> resourceLoaders) {
        this.resourceLoaders = resourceLoaders;
        this.dependencyResourceLoaders = emptyList();
    }

    public Environment(Collection<? extends ResourceLoader> resourceLoaders,
                       Collection<? extends ResourceLoader> dependencyResourceLoaders) {
        this.resourceLoaders = resourceLoaders;
        this.dependencyResourceLoaders = dependencyResourceLoaders;
    }

    public static Builder builder(Properties properties) {
        return new Builder(properties);
    }

    public static Builder builder() {
        return new Builder(new Properties());
    }

    public static class Builder {
        private final Properties properties;
        private final Collection<ResourceLoader> resourceLoaders = new ArrayList<>();
        private final Collection<ResourceLoader> dependencyResourceLoaders = new ArrayList<>();

        public Builder(Properties properties) {
            this.properties = properties;
        }

        public Builder scanRuntimeClasspath(String... acceptPackages) {
            return load(new ClasspathScanningLoader(properties, acceptPackages));
        }

        @SuppressWarnings("unused")
        public Builder scanClassLoader(ClassLoader classLoader) {
            return load(new ClasspathScanningLoader(properties, classLoader));
        }

        public Builder scanYamlResources() {
            return load(ClasspathScanningLoader.onlyYaml(properties));
        }

        /**
         * @param jar         A path to a jar file to scan.
         * @param classLoader A classloader that is populated with the transitive dependencies of the jar.
         * @return This builder.
         */
        @SuppressWarnings("unused")
        public Builder scanJar(Path jar, Collection<Path> dependencies, ClassLoader classLoader) {
            List<ClasspathScanningLoader> firstPassLoaderList = new ArrayList<>();
            for (Path dep : dependencies) {
                firstPassLoaderList.add(new ClasspathScanningLoader(dep, properties, emptyList(), classLoader));
            }

            /*
             * Second loader creation pass where the firstPassLoaderList is passed as the
             * dependencyResourceLoaders list to ensure that we can resolve transitive
             * dependencies using the loaders we just created. This is necessary because
             * the first pass may have missing recipes since the full list of loaders was
             * not provided.
             */
            List<ClasspathScanningLoader> secondPassLoaderList = new ArrayList<>();
            for (Path dep : dependencies) {
                secondPassLoaderList.add(new ClasspathScanningLoader(dep, properties, firstPassLoaderList, classLoader));
            }
            return load(new ClasspathScanningLoader(jar, properties, secondPassLoaderList, classLoader), secondPassLoaderList);
        }

        @SuppressWarnings("unused")
        public Builder scanUserHome() {
            File userHomeRewriteConfig = new File(System.getProperty("user.home") + "/.rewrite/rewrite.yml");
            if (userHomeRewriteConfig.exists()) {
                try (FileInputStream is = new FileInputStream(userHomeRewriteConfig)) {
                    return load(new YamlResourceLoader(is, userHomeRewriteConfig.toURI(), properties));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this;
        }

        public Builder load(ResourceLoader resourceLoader) {
            resourceLoaders.add(resourceLoader);
            return this;
        }

        public Builder load(ResourceLoader resourceLoader, Collection<? extends ResourceLoader> dependencyResourceLoaders) {
            resourceLoaders.add(resourceLoader);
            this.dependencyResourceLoaders.addAll(dependencyResourceLoaders);
            return this;
        }

        public Environment build() {
            return new Environment(resourceLoaders, dependencyResourceLoaders);
        }
    }
}
