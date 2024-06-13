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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.openrewrite.Contributor;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.RecipeIntrospectionUtils.constructRecipe;

public class ClasspathScanningLoader implements ResourceLoader {

    private final LinkedHashSet<Recipe> recipes = new LinkedHashSet<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final LinkedHashSet<RecipeDescriptor> recipeDescriptors = new LinkedHashSet<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();

    private final Map<String, List<Contributor>> recipeAttributions = new HashMap<>();
    private final Map<String, List<RecipeExample>> recipeExamples = new HashMap<>();

    /**
     * Construct a ClasspathScanningLoader scans the runtime classpath of the current java process for recipes
     *
     * @param properties     Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(Properties properties, String[] acceptPackages) {
        scanClasses(new ClassGraph().acceptPackages(acceptPackages), getClass().getClassLoader());
        scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"),
                properties,
                emptyList(),
                null);
    }

    /**
     * Construct a ClasspathScanningLoader scans the provided classload for recipes
     *
     * @param properties  Yaml placeholder properties
     * @param classLoader Limit scan to classes loadable by this classloader
     */
    public ClasspathScanningLoader(Properties properties, ClassLoader classLoader) {
        scanClasses(new ClassGraph()
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader), classLoader);

        scanYaml(new ClassGraph()
                        .ignoreParentClassLoaders()
                        .overrideClassLoaders(classLoader)
                        .acceptPaths("META-INF/rewrite"),
                properties,
                emptyList(),
                classLoader);
    }

    public ClasspathScanningLoader(Path jar, Properties properties, Collection<? extends ResourceLoader> dependencyResourceLoaders, ClassLoader classLoader) {
        String jarName = jar.toFile().getName();

        scanClasses(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader), classLoader);

        scanYaml(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader)
                .acceptPaths("META-INF/rewrite"), properties, dependencyResourceLoaders, classLoader);
    }

    public static ClasspathScanningLoader onlyYaml(Properties properties) {
        ClasspathScanningLoader classpathScanningLoader = new ClasspathScanningLoader();
        classpathScanningLoader.scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"),
                properties, emptyList(), null);
        return classpathScanningLoader;
    }

    private ClasspathScanningLoader() {
    }

    /**
     * This must be called _after_ scanClasses or the descriptors of declarative recipes will be missing any
     * non-declarative recipes they depend on that would be discovered by scanClasses
     */
    private void scanYaml(ClassGraph classGraph, Properties properties, Collection<? extends ResourceLoader> dependencyResourceLoaders, @Nullable ClassLoader classLoader) {
        try (ScanResult scanResult = classGraph.enableMemoryMapping().scan()) {
            List<YamlResourceLoader> yamlResourceLoaders = new ArrayList<>();

            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                    yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI(), properties, classLoader, dependencyResourceLoaders)));
            scanResult.getResourcesWithExtension("yaml").forEachInputStreamIgnoringIOException((res, input) ->
                    yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI(), properties, classLoader, dependencyResourceLoaders)));
            // Extract in two passes so that the full list of recipes from all sources are known when computing recipe descriptors
            // Otherwise recipes which include recipes from other sources in their recipeList will have incomplete descriptors
            for (YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipes.addAll(resourceLoader.listRecipes());
                categoryDescriptors.addAll(resourceLoader.listCategoryDescriptors());
                styles.addAll(resourceLoader.listStyles());
                recipeAttributions.putAll(resourceLoader.listContributors());
                recipeExamples.putAll(resourceLoader.listRecipeExamples());
            }
            for (YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipeDescriptors.addAll(resourceLoader.listRecipeDescriptors(recipes, recipeAttributions, recipeExamples));
            }
        }
    }

    private void scanClasses(ClassGraph classGraph, ClassLoader classLoader) {
        try (ScanResult result = classGraph
                .ignoreClassVisibility()
                .overrideClassLoaders(classLoader)
                .scan()) {

            configureRecipes(result, Recipe.class.getName());
            configureRecipes(result, ScanningRecipe.class.getName());

            for (ClassInfo classInfo : result.getSubclasses(NamedStyles.class.getName())) {
                Class<?> styleClass = classInfo.loadClass();
                    Constructor<?> constructor = RecipeIntrospectionUtils.getZeroArgsConstructor(styleClass);
                    if (constructor != null) {
                        constructor.setAccessible(true);
                        try {
                            styles.add((NamedStyles) constructor.newInstance());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

            }
        }
    }

    private void configureRecipes(ScanResult result, String className) {
        for (ClassInfo classInfo : result.getSubclasses(className)) {
            Class<?> recipeClass = classInfo.loadClass();
            if (recipeClass.getName().equals(DeclarativeRecipe.class.getName()) ||
                (recipeClass.getModifiers() & Modifier.PUBLIC) == 0 ||
                // `ScanningRecipe` is an example of an abstract `Recipe` subtype
                (recipeClass.getModifiers() & Modifier.ABSTRACT) != 0) {
                continue;
            }
            Timer.Builder builder = Timer.builder("rewrite.scan.configure.recipe");
            Timer.Sample sample = Timer.start();
            try {
                Recipe recipe = constructRecipe(recipeClass);
                recipeDescriptors.add(recipe.getDescriptor());
                recipes.add(recipe);
                MetricsHelper.successTags(builder.tags("recipe", "elided"));
            } catch (Throwable e) {
                MetricsHelper.errorTags(builder.tags("recipe", recipeClass.getName()), e);
            } finally {
                sample.stop(builder.register(Metrics.globalRegistry));
            }
        }
    }

    @Override
    public Collection<Recipe> listRecipes() {
        return recipes;
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return recipeDescriptors;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return categoryDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        return styles;
    }

    @Override
    public Map<String, List<RecipeExample>> listRecipeExamples() {
        return recipeExamples;
    }

    @Override
    public Map<String, List<Contributor>> listContributors() {
        return recipeAttributions;
    }
}
