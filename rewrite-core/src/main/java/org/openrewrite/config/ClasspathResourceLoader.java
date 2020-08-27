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
import org.openrewrite.RefactorVisitor;
import org.openrewrite.Style;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class ClasspathResourceLoader implements ResourceLoader {
    private final Collection<YamlResourceLoader> yamlResourceLoaders;

    public ClasspathResourceLoader(Iterable<Path> compileClasspath) {
        yamlResourceLoaders = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                .acceptPaths("META-INF/rewrite")
                .enableMemoryMapping()
                .scan()) {
            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI())));
        }

        if (compileClasspath.iterator().hasNext()) {
            try (ScanResult scanResult = new ClassGraph()
                    .overrideClasspath(compileClasspath)
                    .acceptPaths("META-INF/rewrite")
                    .enableMemoryMapping()
                    .scan()) {
                scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                        yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI())));
            }
        }
    }

    @Override
    public Collection<RecipeConfiguration> loadRecipes() {
        return yamlResourceLoaders.stream().flatMap(loader -> loader.loadRecipes().stream()).collect(toList());
    }

    @Override
    public Collection<? extends RefactorVisitor<?>> loadVisitors() {
        return yamlResourceLoaders.stream().flatMap(loader -> loader.loadVisitors().stream()).collect(toList());
    }

    @Override
    public Map<String, Collection<Style>> loadStyles() {
        Map<String, Collection<Style>> styles = new HashMap<>();
        yamlResourceLoaders.forEach(loader -> styles.putAll(loader.loadStyles()));
        return styles;
    }
}
