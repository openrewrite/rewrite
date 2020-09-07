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
import org.openrewrite.RefactorVisitor;
import org.openrewrite.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class AutoConfigureRefactorVisitorLoader implements ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(AutoConfigureRefactorVisitorLoader.class);

    private final Iterable<Path> compileClasspath;
    private final String[] acceptVisitorPackages;

    public AutoConfigureRefactorVisitorLoader(Iterable<Path> compileClasspath, String... acceptVisitorPackages) {
        this.compileClasspath = compileClasspath;
        this.acceptVisitorPackages = acceptVisitorPackages;
    }

    public Collection<? extends RefactorVisitor<?>> loadVisitors() {
        List<RefactorVisitor<?>> visitors = new ArrayList<>(loadVisitors(new ClassGraph()));

        if(compileClasspath.iterator().hasNext()) {
            visitors.addAll(loadVisitors(new ClassGraph().overrideClasspath(compileClasspath)));
        }

        return visitors;
    }

    @NotNull
    private List<RefactorVisitor<?>> loadVisitors(ClassGraph classGraph) {
        if(acceptVisitorPackages != null && acceptVisitorPackages.length > 0) {
            classGraph = classGraph.acceptPackages(acceptVisitorPackages);
        }

        try(ScanResult scanResult = classGraph
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
                            return (RefactorVisitor<?>) constructor.newInstance();
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
