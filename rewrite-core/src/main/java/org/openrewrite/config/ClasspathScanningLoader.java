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
import org.openrewrite.Recipe;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.style.NamedStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.openrewrite.internal.RecipeIntrospectionUtils.constructRecipe;
import static org.openrewrite.internal.RecipeIntrospectionUtils.recipeDescriptorFromRecipe;

public class ClasspathScanningLoader implements ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ClasspathScanningLoader.class);

    private final /*~~>*/List<Recipe> recipes = new ArrayList<>();
    private final /*~~>*/List<NamedStyles> styles = new ArrayList<>();

    private final /*~~>*/List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final /*~~>*/List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();
    private final /*~~>*/List<RecipeExample> recipeExamples = new ArrayList<>();

    /**
     * Construct a ClasspathScanningLoader scans the runtime classpath of the current java process for recipes
     *
     * @param properties Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(Properties properties, String[] acceptPackages) {
        scanClasses(new ClassGraph().acceptPackages(acceptPackages), getClass().getClassLoader());
        scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"), properties, null);
    }

    /**
     * Construct a ClasspathScanningLoader scans the provided classload for recipes
     *
     * @param properties Yaml placeholder properties
     * @param classLoader Limit scan to classes loadable by this classloader
     */
    public ClasspathScanningLoader(Properties properties, ClassLoader classLoader) {
        scanClasses(new ClassGraph()
                 .ignoreParentClassLoaders()
                 .overrideClassLoaders(classLoader), classLoader);

        scanYaml(new ClassGraph()
                 .ignoreParentClassLoaders()
                 .overrideClassLoaders(classLoader)
                 .acceptPaths("META-INF/rewrite"), properties, classLoader);
    }

    public ClasspathScanningLoader(Path jar, Properties properties, ClassLoader classLoader) {
        String jarName = jar.toFile().getName();

        scanClasses(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader), classLoader);

        scanYaml(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader)
                .acceptPaths("META-INF/rewrite"), properties, classLoader);
    }

    /**
     * This must be called _after_ scanClasses or the descriptors of declarative recipes will be missing any
     * non-declarative recipes they depend on that would be discovered by scanClasses
     */
    private void scanYaml(ClassGraph classGraph, Properties properties, @Nullable ClassLoader classLoader) {
        try (ScanResult scanResult = classGraph.enableMemoryMapping().scan()) {
            /*~~>*/List<YamlResourceLoader> yamlResourceLoaders = new ArrayList<>();

            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) -> {
                yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI(), properties, classLoader));
            });
            // Extract in two passes so that the full list of recipes from all sources are known when computing recipe descriptors
            // Otherwise recipes which include recipes from other sources in their recipeList will have incomplete descriptors
            for(YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipes.addAll(resourceLoader.listRecipes());
                categoryDescriptors.addAll(resourceLoader.listCategoryDescriptors());
                styles.addAll(resourceLoader.listStyles());
                recipeExamples.addAll(resourceLoader.listRecipeExamples());
            }
            for(YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipeDescriptors.addAll(resourceLoader.listRecipeDescriptors(recipes));
            }
        }
    }

    private void scanClasses(ClassGraph classGraph, ClassLoader classLoader) {
        try (ScanResult result = classGraph
                .ignoreClassVisibility()
                .overrideClassLoaders(classLoader)
                .scan()) {

            for (ClassInfo classInfo : result.getSubclasses(Recipe.class.getName())) {
                Class<?> recipeClass = classInfo.loadClass();
                if (recipeClass.getName().equals(DeclarativeRecipe.class.getName()) || recipeClass.getEnclosingClass() != null) {
                    continue;
                }
                try {
                    Recipe recipe = constructRecipe(recipeClass);
                    recipeDescriptors.add(recipeDescriptorFromRecipe(recipe));
                    recipes.add(recipe);
                } catch (Exception e) {
                    logger.warn("Unable to configure {}", recipeClass.getName(), e);
                }
            }
            for (ClassInfo classInfo : result.getSubclasses(NamedStyles.class.getName())) {
                Class<?> styleClass = classInfo.loadClass();
                try {
                    Constructor<?> constructor = RecipeIntrospectionUtils.getZeroArgsConstructor(styleClass);
                    if(constructor != null) {
                        constructor.setAccessible(true);
                        styles.add((NamedStyles) constructor.newInstance());
                    }
                } catch (Exception e) {
                    logger.warn("Unable to configure {}", styleClass.getName(), e);
                }
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
    public Collection<RecipeExample> listRecipeExamples() {
        return recipeExamples;
    }
}
