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
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.openrewrite.SourceVisitor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Scans for YAML definitions of {@link SourceVisitor} in META-INF/rewrite-definitions.
 */
public class ClasspathSourceVisitorLoader implements SourceVisitorLoader {
    @Override
    public Collection<SourceVisitor<?>> load() {
        try (ScanResult scanResult = new ClassGraph()
                .whitelistPaths("META-INF/rewrite-definitions")
                .enableMemoryMapping()
                .scan()) {
            ResourceList yml = scanResult.getResourcesWithExtension("yml");
            Collection<SourceVisitor<?>> sourceVisitors = new ArrayList<>();

            yml.forEachInputStreamIgnoringIOException((res, ymlIn) -> {
                String path = res.getPath();
                String name = path.substring(path.lastIndexOf('/') + 1)
                        .replaceAll(".yml$", "");
                sourceVisitors.addAll(new YamlSourceVisitorLoader(name, ymlIn).load());
            });

            return sourceVisitors;
        }
    }
}
