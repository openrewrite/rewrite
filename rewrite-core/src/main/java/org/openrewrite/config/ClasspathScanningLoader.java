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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Contributor;
import org.openrewrite.Recipe;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.style.NamedStyles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.emptyMap;

public class ClasspathScanningLoader implements ResourceLoader {

    private final LinkedHashMap<String, Recipe> recipes = new LinkedHashMap<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final LinkedHashSet<RecipeDescriptor> recipeDescriptors = new LinkedHashSet<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();

    private final Map<String, List<Contributor>> recipeAttributions = new HashMap<>();
    private final Map<String, List<RecipeExample>> recipeExamples = new HashMap<>();

    private final ClassLoader classLoader;
    private final RecipeLoader recipeLoader;
    private @Nullable Runnable performScan;

    /**
     * Construct a ClasspathScanningLoader scans the runtime classpath of the current java process for recipes
     *
     * @param properties     Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(Properties properties, String[] acceptPackages) {
        this.classLoader = ClasspathScanningLoader.class.getClassLoader();
        this.recipeLoader = new RecipeLoader(classLoader);
        this.performScan = () -> {
            scanClasses(new ClassGraph().acceptPackages(acceptPackages), getClass().getClassLoader());
            scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"),
                    properties,
                    null,
                    null,
                    null);
        };
    }

    /**
     * Construct a ClasspathScanningLoader scans the provided classload for recipes
     *
     * @param properties  Yaml placeholder properties
     * @param classLoader Limit scan to classes loadable by this classloader
     */
    public ClasspathScanningLoader(Properties properties, ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.recipeLoader = new RecipeLoader(classLoader);
        this.performScan = () -> {
            scanClasses(new ClassGraph()
                    .ignoreParentClassLoaders()
                    .overrideClassLoaders(classLoader), classLoader);

            scanYaml(new ClassGraph()
                            .ignoreParentClassLoaders()
                            .overrideClassLoaders(classLoader)
                            .acceptPaths("META-INF/rewrite"),
                    properties,
                    null,
                    classLoader,
                    null);
        };
    }

    public ClasspathScanningLoader(Path jar, Properties properties, @Nullable ResourceLoader dependencyResourceLoader, ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.recipeLoader = new RecipeLoader(classLoader);

        this.performScan = () -> {
            // Scan entire classpath to get full inheritance hierarchy, but filter results to target jar
            scanClasses(new ClassGraph()
                    .ignoreParentClassLoaders()
                    .overrideClassLoaders(classLoader), classLoader, jar);

            scanYaml(new ClassGraph()
                    .ignoreParentClassLoaders()
                    .overrideClassLoaders(classLoader)
                    .acceptPaths("META-INF/rewrite"), properties, dependencyResourceLoader, classLoader, jar);
        };
    }

    public static ClasspathScanningLoader onlyYaml(Properties properties) {
        ClasspathScanningLoader classpathScanningLoader = new ClasspathScanningLoader();
        classpathScanningLoader.scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"),
                properties, null, null, null);
        return classpathScanningLoader;
    }

    private ClasspathScanningLoader() {
        this.classLoader = ClasspathScanningLoader.class.getClassLoader();
        this.recipeLoader = new RecipeLoader(classLoader);
    }

    /**
     * This must be called _after_ scanClasses or the descriptors of declarative recipes will be missing any
     * non-declarative recipes they depend on that would be discovered by scanClasses
     */
    private void scanYaml(ClassGraph classGraph, Properties properties, @Nullable ResourceLoader dependencyResourceLoader, @Nullable ClassLoader classLoader, @Nullable Path targetJar) {
        try (ScanResult scanResult = classGraph.scan()) {
            List<YamlResourceLoader> yamlResourceLoaders = new ArrayList<>();

            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) -> {
                if (targetJar == null || isFromJar(res.getClasspathElementURI(), targetJar)) {
                    yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI(), properties, classLoader, dependencyResourceLoader));
                }
            });
            scanResult.getResourcesWithExtension("yaml").forEachInputStreamIgnoringIOException((res, input) -> {
                if (targetJar == null || isFromJar(res.getClasspathElementURI(), targetJar)) {
                    yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI(), properties, classLoader, dependencyResourceLoader));
                }
            });
            // Extract in two passes so that the full list of recipes from all sources are known when computing recipe descriptors
            // Otherwise recipes which include recipes from other sources in their recipeList will have incomplete descriptors
            for (YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                for (Recipe recipe : resourceLoader.listRecipes()) {
                    recipes.put(recipe.getName(), recipe);
                }
                categoryDescriptors.addAll(resourceLoader.listCategoryDescriptors());
                styles.addAll(resourceLoader.listStyles());
                recipeAttributions.putAll(resourceLoader.listContributors());
                recipeExamples.putAll(resourceLoader.listRecipeExamples());
            }
            for (YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipeDescriptors.addAll(resourceLoader.listRecipeDescriptors(recipes.values(), recipeAttributions, recipeExamples));
            }
        }
    }

    private void scanClasses(ClassGraph classGraph, ClassLoader classLoader) {
        scanClasses(classGraph, classLoader, null);
    }

    private void scanClasses(ClassGraph classGraph, ClassLoader classLoader, @Nullable Path targetJarName) {
        try (ScanResult result = classGraph
                .ignoreClassVisibility()
                .overrideClassLoaders(classLoader)
                .scan()) {

            configureRecipes(result, Recipe.class.getName(), targetJarName);

            for (ClassInfo classInfo : result.getSubclasses(NamedStyles.class.getName())) {
                // Only process styles from the target jar if specified
                if (targetJarName != null && !isFromJar(classInfo.getClasspathElementURI(), targetJarName)) {
                    continue;
                }
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isFromJar(@Nullable URI classpathElementURI, Path jar) {
        try {
            return classpathElementURI != null && Files.isSameFile(Paths.get(classpathElementURI), jar);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void configureRecipes(ScanResult result, String className, @Nullable Path targetJar) {
        for (ClassInfo classInfo : result.getSubclasses(className)) {
            // Only process recipes from the target jar if specified
            if (targetJar != null && !isFromJar(classInfo.getClasspathElementURI(), targetJar)) {
                continue;
            }
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
                Recipe recipe = recipeLoader.load(recipeClass, emptyMap());
                recipeDescriptors.add(recipe.getDescriptor());
                recipes.put(recipe.getName(), recipe);
                MetricsHelper.successTags(builder.tags("recipe", "elided"));
            } catch (Throwable e) {
                MetricsHelper.errorTags(builder.tags("recipe", recipeClass.getName()), e);
            } finally {
                sample.stop(builder.register(Metrics.globalRegistry));
            }
        }
    }

    @Override
    public @Nullable Recipe loadRecipe(String recipeName, RecipeDetail... details) {
        if (performScan != null) {
            try {
                return recipeLoader.load(recipeName, null);
            } catch (NoClassDefFoundError | IllegalArgumentException ignored) {
                // it's probably declarative
            }
        }
        ensureScanned();
        return recipes.get(recipeName);
    }

    @Override
    public Collection<Recipe> listRecipes() {
        ensureScanned();
        return recipes.values();
    }

    private void ensureScanned() {
        if (performScan != null) {
            Runnable scan = performScan;
            performScan = null;
            scan.run();
        }
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        ensureScanned();
        return recipeDescriptors;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        ensureScanned();
        return categoryDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        ensureScanned();
        return styles;
    }

    @Override
    public Map<String, List<RecipeExample>> listRecipeExamples() {
        ensureScanned();
        return recipeExamples;
    }

    @Override
    public Map<String, List<Contributor>> listContributors() {
        ensureScanned();
        return recipeAttributions;
    }
}
