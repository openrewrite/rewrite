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

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.openrewrite.HiddenRecipe;
import org.openrewrite.Recipe;
import org.openrewrite.Option;
import org.openrewrite.style.NamedStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

    public ClasspathScanningLoader(Iterable<Path> compileClasspath, Properties properties, String[] acceptPackages) {
        scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"), properties);
        scanClasses(new ClassGraph(), acceptPackages);

        if (compileClasspath.iterator().hasNext()) {
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
                    .acceptPaths("META-INF/rewrite"), properties);

            scanClasses(new ClassGraph()
                    .ignoreParentClassLoaders()
                    .overrideClassLoaders(classpathLoader), acceptPackages);
        }
    }

    private void scanYaml(ClassGraph classGraph, Properties properties) {
        try (ScanResult scanResult = classGraph.enableMemoryMapping().scan()) {
            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) -> {
                YamlResourceLoader resourceLoader = new YamlResourceLoader(input, res.getURI(), properties);
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
                if (HiddenRecipe.class.isAssignableFrom(recipeClass) || recipeClass.equals(DeclarativeRecipe.class)) {
                    continue;
                }
                try {
                    List<OptionDescriptor> options = new ArrayList<>();

                    for (Field field : recipeClass.getDeclaredFields()) {
                        Option option = field.getAnnotation(Option.class);
                        if (option != null) {
                            options.add(new OptionDescriptor(field.getName(), field.getType().getSimpleName(),
                                    option.displayName(), option.description()));
                        }
                    }

                    Constructor<?> primaryConstructor = null;
                    Constructor<?>[] constructors = recipeClass.getConstructors();
                    if (constructors.length == 0) {
                        // kotlin object declarations have no constructors at all
                        continue;
                    } else if (recipeClass.getConstructors().length == 1) {
                        primaryConstructor = recipeClass.getConstructors()[0];
                    } else {
                        for (Constructor<?> constructor : constructors) {
                            if (constructor.isAnnotationPresent(JsonCreator.class)) {
                                primaryConstructor = constructor;
                                break;
                            }
                        }
                    }
                    if (primaryConstructor == null) {
                        throw new IllegalStateException("Unable to locate primary constructor for Recipe " + recipeClass);
                    }
                    Object[] constructorArgs = new Object[primaryConstructor.getParameterCount()];
                    for (int i = 0; i < primaryConstructor.getParameters().length; i++) {
                        java.lang.reflect.Parameter param = primaryConstructor.getParameters()[i];
                        if (param.getType().isPrimitive()) {
                            constructorArgs[i] = getPrimitiveDefault(param.getType());
                        } else {
                            constructorArgs[i] = null;
                        }
                    }
                    primaryConstructor.setAccessible(true);
                    Recipe recipe = (Recipe) primaryConstructor.newInstance(constructorArgs);

                    if (primaryConstructor.getParameterCount() == 0) {
                        primaryConstructor.setAccessible(true);
                        recipes.add((Recipe) primaryConstructor.newInstance());
                    }
                    recipeDescriptors.add(new RecipeDescriptor(classInfo.getName(), recipe.getDisplayName(), recipe.getDescription(), options));
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

    private Object getPrimitiveDefault(Class<?> t) {
        if (t.equals(byte.class)) {
            return (byte) 0;
        } else if (t.equals(short.class)) {
            return (short) 0;
        } else if (t.equals(int.class)) {
            return 0;
        } else if (t.equals(long.class)) {
            return 0L;
        } else if (t.equals(float.class)) {
            return 0.0f;
        } else if (t.equals(double.class)) {
            return 0.0d;
        } else if (t.equals(char.class)) {
            return '\u0000';
        } else if (t.equals(boolean.class)) {
            return false;
        } else {
            throw new IllegalArgumentException(t.getCanonicalName() + " is not a supported primitive type");
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
