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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static java.util.stream.StreamSupport.stream;

public class ClasspathScanningLoader implements ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ClasspathScanningLoader.class);

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();

    /**
     * Construct a ClasspathScanningLoader scans the runtime classpath of the current java process for recipes
     *
     * @param properties Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(Properties properties, String[] acceptPackages) {
        scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"), properties, null);
        scanClasses(new ClassGraph(), acceptPackages);
    }

    /**
     * Construct a ClasspathScanningLoader that scans the specified compile classpath for recipes
     *
     * @param compileClasspath Classpath to scan
     * @param properties Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(Iterable<Path> compileClasspath, Properties properties, String[] acceptPackages) {
        URLClassLoader classpathLoader = new URLClassLoader(
                stream(compileClasspath.spliterator(), false)
                        .map(cc -> {
                            try {
                                return cc.toUri().toURL();
                            } catch (MalformedURLException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .toArray(URL[]::new),
                getClass().getClassLoader()
        );

        scanYaml(new ClassGraph()
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classpathLoader)
                .acceptPaths("META-INF/rewrite"), properties, classpathLoader);

        scanClasses(new ClassGraph()
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classpathLoader), acceptPackages);
    }

    /**
     * Construct a ClasspathScanningLoader that scans the specified jar name, which must be on the the compile classpath.
     * The classpath is used to provide symbols, but the scan is limited to just recipes contained within the jar.
     *
     * @param jarName Name of jar on classpath to scan for recipes
     * @param compileClasspath Classpath to scan
     * @param properties Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(String jarName, Iterable<Path> compileClasspath, Properties properties, String[] acceptPackages) {
        URLClassLoader classpathLoader = new URLClassLoader(
                stream(compileClasspath.spliterator(), false)
                        .map(cc -> {
                            try {
                                return cc.toUri().toURL();
                            } catch (MalformedURLException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .toArray(URL[]::new),
                getClass().getClassLoader()
        );

        scanYaml(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classpathLoader)
                .acceptPaths("META-INF/rewrite"), properties, classpathLoader);

        scanClasses(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classpathLoader), acceptPackages);
    }

    private void scanYaml(ClassGraph classGraph, Properties properties, @Nullable ClassLoader classLoader) {
        try (ScanResult scanResult = classGraph.enableMemoryMapping().scan()) {
            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) -> {
                YamlResourceLoader resourceLoader = new YamlResourceLoader(input, res.getURI(), properties, classLoader);
                recipes.addAll(resourceLoader.listRecipes());
                recipeDescriptors.addAll(resourceLoader.listRecipeDescriptors());
                styles.addAll(resourceLoader.listStyles());
            });
        }
    }


    private void scanClasses(ClassGraph classGraph, String[] acceptPackages) {
        try (ScanResult result = classGraph
                .ignoreClassVisibility()
                .acceptPackages(acceptPackages)
                .scan()) {
            for (ClassInfo classInfo : result.getSubclasses(Recipe.class.getName())) {
                Class<?> recipeClass = classInfo.loadClass();
                if (recipeClass.equals(DeclarativeRecipe.class) || recipeClass.getEnclosingClass() != null) {
                    continue;
                }
                try {
                    recipeDescriptors.add(RecipeIntrospectionUtils.recipeDescriptorFromRecipeClass(recipeClass));
                    Constructor<?> primaryConstructor = RecipeIntrospectionUtils.getPrimaryConstructor(recipeClass);

                    if (primaryConstructor.getParameterCount() == 0) {
                        primaryConstructor.setAccessible(true);
                        recipes.add((Recipe) primaryConstructor.newInstance());
                    }
                } catch (Exception e) {
                    logger.warn("Unable to configure {}", recipeClass.getName(), e);
                }
            }
            for (ClassInfo classInfo : result.getSubclasses(NamedStyles.class.getName())) {
                Class<?> styleClass = classInfo.loadClass();
                try {
                    for (Constructor<?> constructor : styleClass.getConstructors()) {
                        if (constructor.getParameterCount() == 0) {
                            constructor.setAccessible(true);
                            styles.add((NamedStyles) constructor.newInstance());
                            break;
                        }
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
    public Collection<NamedStyles> listStyles() {
        return styles;
    }
}
