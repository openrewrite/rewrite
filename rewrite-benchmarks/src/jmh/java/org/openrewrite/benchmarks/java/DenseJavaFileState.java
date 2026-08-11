/*
 * Copyright 2026 the original author or authors.
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

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * A large source file with hundreds of {@code java.util.List} references, so that a search's
 * per-file work is measured where matches are dense.
 */
@SuppressWarnings("NotNullFieldNotInitialized")
@State(Scope.Benchmark)
public class DenseJavaFileState {
    List<SourceFile> sourceFiles;

    private static final String DENSE_FILE = "rewrite-java/src/main/java/org/openrewrite/java/tree/J.java";

    @Setup
    public void setup() {
        sourceFiles = JavaParser.fromJavaVersion()
                .classpath("jsr305", "jackson-annotations", "jspecify", "lombok", "annotations")
                .build()
                .parse(singletonList(denseFile()), null, new InMemoryExecutionContext())
                .collect(toList());
    }

    /**
     * Located by walking up from the working directory, which holds whichever module Gradle
     * launched the benchmark from.
     */
    private static Path denseFile() {
        for (Path dir = Paths.get(System.getProperty("user.dir")); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(DENSE_FILE);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Cannot find " + DENSE_FILE + " above " + System.getProperty("user.dir"));
    }

    @TearDown
    public void tearDown(Blackhole hole) {
        hole.consume(sourceFiles.size());
    }

    public LargeSourceSet getSourceSet() {
        return new InMemoryLargeSourceSet(sourceFiles);
    }

    public SourceFile getDenseFile() {
        return sourceFiles.get(0);
    }
}
