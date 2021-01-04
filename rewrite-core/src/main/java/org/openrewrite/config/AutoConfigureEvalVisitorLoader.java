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
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.AutoConfigure;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Style;
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
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class AutoConfigureEvalVisitorLoader implements ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(AutoConfigureEvalVisitorLoader.class);

    private final Iterable<Path> compileClasspath;
    private final String[] acceptVisitorPackages;

    public AutoConfigureEvalVisitorLoader(Iterable<Path> compileClasspath, String... acceptVisitorPackages) {
        this.compileClasspath = compileClasspath;
        this.acceptVisitorPackages = acceptVisitorPackages;
    }

    public Collection<? extends TreeProcessor<?>> loadVisitors() {
        List<TreeProcessor<?>> visitors = new ArrayList<>(loadVisitors(new ClassGraph()));

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

            visitors.addAll(loadVisitors(new ClassGraph()
                    .ignoreParentClassLoaders()
                    .overrideClassLoaders(
                            classpathLoader
                    )
            ));
        }

        return visitors;
    }

    @NotNull
    private List<TreeProcessor<?>> loadVisitors(ClassGraph classGraph) {
        if (acceptVisitorPackages != null && acceptVisitorPackages.length > 0) {
            classGraph = classGraph.acceptPackages(acceptVisitorPackages);
        }

        try (ScanResult scanResult = classGraph
                .enableMemoryMapping()
                .enableAnnotationInfo()
                .ignoreClassVisibility()
                .scan()) {
            return scanResult.getClassesWithAnnotation(AutoConfigure.class.getName()).stream()
                    .map(classInfo -> {
                        Class<?> visitorClass = classInfo.loadClass();
                        try {
                            Constructor<?> constructor = visitorClass.getConstructor();
                            constructor.setAccessible(true);
                            return (TreeProcessor<?>) constructor.newInstance();
                        } catch (Exception e) {
                            logger.warn("Unable to configure {}", visitorClass.getName(), e);
                        }
                        return null;
                    })
                    .collect(toList());
        }
    }

    @Override
    public Map<String, Collection<Style>> loadStyles() {
        return emptyMap();
    }

    @Override
    public Collection<RecipeConfiguration> loadRecipes() {
        return emptyList();
    }
}
