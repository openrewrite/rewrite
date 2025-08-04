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
import org.openrewrite.java.internal.JavaTypeCache;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("NotNullFieldNotInitialized")
@State(Scope.Benchmark)
public class JavaCompilationUnitState {
    JavaParser.Builder<? extends JavaParser, ?> javaParser;
    List<SourceFile> sourceFiles;
    List<Path> inputs;
    SnappyJavaTypeCache snappyTypeCache;
    JavaTypeCache radixMapTypeCache;
    MapJavaTypeCache typeCache;

    public static void main(String[] args) throws URISyntaxException {
        JavaCompilationUnitState javaCompiler = new JavaCompilationUnitState();
        javaCompiler.setup();
        javaCompiler.printMemory();
    }

    @Setup(Level.Trial)
    public void setup() throws URISyntaxException {
        Path rewriteRoot = Paths.get(ChangeTypeBenchmark.class.getResource("./")
                .toURI()).resolve("../../../../../../../../").normalize();

        inputs = Arrays.asList(
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/Nullable.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/NullUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/AdaptiveRadixTree.java"),
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
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Incubating.java"),
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
                .collect(toList());

        radixMapTypeCache = new JavaTypeCache();
        for (Map.Entry<String, Object> entry : typeCache.map().entrySet()) {
            radixMapTypeCache.put(entry.getKey(), entry.getValue());
        }

        snappyTypeCache = new SnappyJavaTypeCache();
        for (Map.Entry<String, Object> entry : typeCache.map().entrySet()) {
            snappyTypeCache.put(entry.getKey(), entry.getValue());
        }
    }

    void printMemory() {
        long retainedSize = GraphLayout.parseInstance(radixMapTypeCache).totalSize();
        System.out.printf("Retained AdaptiveRadixTree size: %10d bytes\n", retainedSize);
        retainedSize = GraphLayout.parseInstance(snappyTypeCache).totalSize();
        System.out.printf("Retained Snappy size:            %10d bytes\n", retainedSize);
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

    static class SnappyJavaTypeCache extends JavaTypeCache {

        // empirical value: below this size, the compressed key is larger or only slightly smaller
        // although also note that a String object has a 24 bytes overhead vs. the 16 bytes of a BytesKey object
        public static final int COMPRESSION_THRESHOLD = 50;

        @SuppressWarnings("ClassCanBeRecord")
        private static class BytesKey {
            private final byte[] data;
            BytesKey(byte[] data) {
                this.data = data;
            }
        }

        Map<Object, Object> typeCache = new HashMap<>();

        @Override
        public <T> @Nullable T get(String signature) {
            //noinspection unchecked
            return (T) typeCache.get(key(signature));
        }

        @Override
        public void put(String signature, Object o) {
            typeCache.put(key(signature), o);
        }

        private static boolean snappyUsable = true;

        private Object key(String signature) {
            if (signature.length() > COMPRESSION_THRESHOLD && snappyUsable) {
                try {
                    return new BytesKey(Snappy.compress(signature.getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (NoClassDefFoundError e) {
                    // Some systems fail to load Snappy native components, so fall back to not compressing
                    snappyUsable = false;
                }
            }
            return signature;
        }

        @Override
        public void clear() {
            typeCache.clear();
        }

        @Override
        public SnappyJavaTypeCache clone() {
            SnappyJavaTypeCache clone = (SnappyJavaTypeCache) super.clone();
            clone.typeCache = new HashMap<>(this.typeCache);
            return clone;
        }
    }
}
