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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@State(Scope.Benchmark)
public class JavaCompilationUnitState {
    /*~~>*/List<J.CompilationUnit> sourceFiles;

    @Setup(Level.Trial)
    public void setup() throws URISyntaxException {
        Path rewriteRoot = Paths.get(ChangeTypeBenchmark.class.getResource("./")
                .toURI()).resolve("../../../../../../../../").normalize();

        /*~~>*/List<Path> inputs = Arrays.asList(
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/Nullable.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/NullUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/MetricsHelper.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/ListUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/PropertyPlaceholderHelper.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/RecipeIntrospectionUtils.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Tree.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/ExecutionContext.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/InMemoryExecutionContext.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/marker/Marker.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/marker/Markers.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/style/Style.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/DeclarativeNamedStyles.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/style/NamedStyles.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/TreePrinter.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Option.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/OptionDescriptor.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/RecipeDescriptor.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Result.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/SourceFile.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Recipe.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Validated.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/DeclarativeRecipe.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/ResourceLoader.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/YamlResourceLoader.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/ClasspathScanningLoader.java"),
                rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/RecipeIntrospectionException.java")
        );

        sourceFiles = JavaParser.fromJavaVersion()
                .classpath("jsr305", "classgraph", "jackson-annotations", "micrometer-core", "slf4j-api",
                        "org.eclipse.jgit")
//                .logCompilationWarningsAndErrors(true)
                .build()
                .parse(inputs, null, new InMemoryExecutionContext(Throwable::printStackTrace));
    }

    @TearDown(Level.Trial)
    public void tearDown(Blackhole hole) {
        hole.consume(sourceFiles.size());
    }

    public /*~~>*/List<J.CompilationUnit> getSourceFiles() {
        return sourceFiles;
    }
}
