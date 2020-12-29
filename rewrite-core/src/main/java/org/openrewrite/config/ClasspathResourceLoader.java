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
import org.openrewrite.EvalVisitor;
import org.openrewrite.Style;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class ClasspathResourceLoader implements ResourceLoader {
    private final Map<URI, YamlResourceLoader> resourceLoaderByOrigin;

    public ClasspathResourceLoader(Iterable<Path> compileClasspath, Properties properties) {
        resourceLoaderByOrigin = new HashMap<>();

        try (ScanResult scanResult = new ClassGraph()
                .acceptPaths("META-INF/rewrite")
                .enableMemoryMapping()
                .scan()) {
            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                    resourceLoaderByOrigin.put(res.getURI(), new YamlResourceLoader(input, res.getURI(), properties)));
        }

        if (compileClasspath.iterator().hasNext()) {
            try (ScanResult scanResult = new ClassGraph()
                    .overrideClasspath(compileClasspath)
                    .acceptPaths("META-INF/rewrite")
                    .enableMemoryMapping()
                    .scan()) {
                scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                    resourceLoaderByOrigin.put(res.getURI(), new YamlResourceLoader(input, res.getURI(), properties)));
            }
        }
    }

    @Override
    public Collection<RecipeConfiguration> loadRecipes() {
        return resourceLoaderByOrigin.values().stream().flatMap(loader -> loader.loadRecipes().stream()).collect(toList());
    }

    @Override
    public Collection<? extends EvalVisitor<?>> loadVisitors() {
        return resourceLoaderByOrigin.values().stream().flatMap(loader -> loader.loadVisitors().stream()).collect(toList());
    }

    @Override
    public Map<String, Collection<Style>> loadStyles() {
        Map<String, Collection<Style>> styles = new HashMap<>();
        resourceLoaderByOrigin.values().forEach(loader -> styles.putAll(loader.loadStyles()));
        return styles;
    }
}
