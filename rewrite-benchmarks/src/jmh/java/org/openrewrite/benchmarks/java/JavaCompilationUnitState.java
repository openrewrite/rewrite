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
package org.openrewrite.benchmarks.java;

import org.jspecify.annotations.Nullable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jol.info.GraphLayout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.AdaptiveRadixJavaTypeCache;
import org.openrewrite.java.internal.JavaTypeCache;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
public class JavaCompilationUnitState {
    JavaParser.Builder<? extends JavaParser, ?> javaParser;
    List<SourceFile> sourceFiles;
    List<Path> inputs;
    AdaptiveRadixJavaTypeCache radixMapTypeCache;
    MapJavaTypeCache typeCache;

    public static void main(String[] args) throws URISyntaxException {
        new JavaCompilationUnitState().setup();
    }

    @Setup(Level.Trial)
    public void setup() throws URISyntaxException {
        Path rewriteRoot = Paths.get(ChangeTypeBenchmark.class.getResource("./")
                .toURI()).resolve("../../../../../../../../").normalize();

        inputs = Arrays.asList(
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/Nullable.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/NullUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/MetricsHelper.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/ListUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/PropertyPlaceholderHelper.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/RecipeIntrospectionUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Tree.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/ExecutionContext.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/InMemoryExecutionContext.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/marker/Marker.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/marker/Markers.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/style/Style.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/DeclarativeNamedStyles.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/style/NamedStyles.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Option.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/OptionDescriptor.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/RecipeDescriptor.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Result.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/SourceFile.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Recipe.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/ScanningRecipe.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Validated.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/ValidationException.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/TreeVisitor.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/TreeObserver.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/DeclarativeRecipe.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/ResourceLoader.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/YamlResourceLoader.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/ClasspathScanningLoader.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/RecipeIntrospectionException.java")
        );

        javaParser = JavaParser.fromJavaVersion()
                .classpath("jsr305", "classgraph", "jackson-annotations", "micrometer-core",
                        "jgit", "jspecify", "lombok", "annotations");
//                .logCompilationWarningsAndErrors(true)

        typeCache = new MapJavaTypeCache();
        JavaParser parser = javaParser.typeCache(typeCache).build();
        sourceFiles = parser
                .parse(inputs, null, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        radixMapTypeCache = new AdaptiveRadixJavaTypeCache();
        for (Map.Entry<String, Object> entry : typeCache.map().entrySet()) {
            radixMapTypeCache.put(entry.getKey(), entry.getValue());
        }
    }

    void printMemory() {
        long retainedSize = GraphLayout.parseInstance(radixMapTypeCache).totalSize();
        System.out.printf("Retained AdaptiveRadixTree size: %10d bytes\n", retainedSize);
        retainedSize = GraphLayout.parseInstance(typeCache).totalSize();
        System.out.printf("Retained HashMap size:           %10d bytes\n", retainedSize);
    }

    @TearDown(Level.Trial)
    public void tearDown(Blackhole hole) {
        hole.consume(sourceFiles.size());
    }

    public LargeSourceSet getSourceSet() {
        return new InMemoryLargeSourceSet(sourceFiles);
    }

    public List<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

    static class MapJavaTypeCache extends JavaTypeCache {

        Map<String, Object> typeCache = new HashMap<>();

        @Override
        public <T> @Nullable T get(String signature) {
            //noinspection unchecked
            return (T) typeCache.get(signature);
        }

        @Override
        public void put(String signature, Object o) {
            typeCache.put(signature, o);
        }

        public Map<String, Object> map() {
            return typeCache;
        }

        @Override
        public void clear() {
            typeCache.clear();
        }

        @Override
        public MapJavaTypeCache clone() {
            MapJavaTypeCache clone = (MapJavaTypeCache) super.clone();
            clone.typeCache = new HashMap<>(this.typeCache);
            return clone;
        }
    }
}
