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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Measures the wall-clock and allocation impact of cursor handling in the visitor. Two workloads share an
 * identical, sizable parsed tree:
 * <ul>
 *     <li>{@code traverseNoCursor} — a full traversal whose visitor never calls {@code getCursor()}. This is the
 *     best case for lazy cursor materialization: every per-node cursor (including padding cursors) is skipped.</li>
 *     <li>{@code changeType} — {@link ChangeType}, a representative transform that does consult the cursor, so most
 *     nodes still materialize.</li>
 * </ul>
 */
@Fork(1)
@Measurement(iterations = 5)
@Warmup(iterations = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
public class CursorBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CursorBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Benchmark)
    public static class Trees {
        List<SourceFile> sourceFiles;

        @Setup(Level.Trial)
        public void setup() {
            Path root = repoRoot();
            Path srcDir = root.resolve("rewrite-core/src/main/java/org/openrewrite");
            List<Path> inputs;
            try (Stream<Path> walk = Files.walk(srcDir)) {
                inputs = walk.filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .limit(120)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            sourceFiles = JavaParser.fromJavaVersion().build()
                    .parse(inputs, null, new InMemoryExecutionContext())
                    .collect(Collectors.toList());
            if (sourceFiles.isEmpty()) {
                throw new IllegalStateException("No sources parsed from " + srcDir);
            }
        }

        LargeSourceSet sourceSet() {
            return new InMemoryLargeSourceSet(sourceFiles);
        }

        private static Path repoRoot() {
            Path dir = Paths.get("").toAbsolutePath();
            for (Path p = dir; p != null; p = p.getParent()) {
                if (Files.exists(p.resolve("rewrite-core/src/main/java/org/openrewrite/TreeVisitor.java"))) {
                    return p;
                }
            }
            throw new IllegalStateException("Could not locate repo root from " + dir);
        }
    }

    /**
     * A search-style recipe: it fully traverses every node (so every padding/element cursor would be allocated
     * eagerly) but never requests a cursor, counting method invocations into a sink to defeat dead-code elimination.
     */
    static class CountInvocations extends Recipe {
        final AtomicLong count = new AtomicLong();

        @Override
        public String getDisplayName() {
            return "Count method invocations without using the cursor";
        }

        @Override
        public String getDescription() {
            return "Traverses the whole tree but never calls getCursor().";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    count.incrementAndGet();
                    return super.visitMethodInvocation(method, ctx);
                }
            };
        }
    }

    @Benchmark
    public void traverseNoCursor(Trees state, Blackhole hole) {
        CountInvocations recipe = new CountInvocations();
        recipe.run(state.sourceSet(), new InMemoryExecutionContext());
        hole.consume(recipe.count.get());
    }

    @Benchmark
    public void changeType(Trees state, Blackhole hole) {
        hole.consume(new ChangeType("java.util.List", "java.util.Collection", null)
                .run(state.sourceSet(), new InMemoryExecutionContext()));
    }
}
