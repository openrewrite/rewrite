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
package org.openrewrite.benchmarks.maven;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.CompositeMavenPomCache;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.RocksdbMavenPomCache;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Exercises the hot {@link ResolvedDependency#findDependency} / {@link ResolvedDependency#findDependencies}
 * recursive graph walk against a realistic resolved {@code spring-boot-starter-web} tree. Run with the
 * {@code gc} profiler (the default in this module) to observe per-lookup allocation.
 */
@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class FindDependencyBenchmark {

    CompositeMavenPomCache pomCache = new CompositeMavenPomCache(
            new InMemoryMavenPomCache(),
            new RocksdbMavenPomCache(Paths.get(System.getProperty("user.home")))
    );

    ResolvedDependency root;

    /**
     * Coordinates of the deepest dependency in {@link #root}'s subtree, so a lookup walks the full graph
     * before finding a match.
     */
    String deepGroupId;
    String deepArtifactId;

    @Setup
    public void setup() {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setPomCache(pomCache);

        //noinspection OptionalGetWithoutIsPresent
        SourceFile pom = MavenParser.builder().build().parse(ctx,
                "" +
                "<project>" +
                "  <parent>" +
                "    <groupId>org.springframework.boot</groupId>" +
                "    <artifactId>spring-boot-starter-parent</artifactId>" +
                "    <version>2.6.3</version>" +
                "  </parent>" +
                "  <groupId>com.mycompany.app</groupId>" +
                "  <artifactId>my-app</artifactId>" +
                "  <version>1</version>" +
                "  <dependencies>" +
                "    <dependency>" +
                "      <groupId>org.springframework.boot</groupId>" +
                "      <artifactId>spring-boot-starter-web</artifactId>" +
                "    </dependency>" +
                "  </dependencies>" +
                "</project>"
        ).findFirst().get();

        MavenResolutionResult mrr = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        List<ResolvedDependency> compile = mrr.getDependencies().get(org.openrewrite.maven.tree.Scope.Compile);

        // The direct dependency with the largest subtree, to make the recursive walk meaningful.
        ResolvedDependency widest = null;
        int widestSize = -1;
        for (ResolvedDependency d : compile) {
            if (d.getDepth() == 0) {
                int size = countSubtree(d);
                if (size > widestSize) {
                    widestSize = size;
                    widest = d;
                }
            }
        }
        root = widest == null ? compile.get(0) : widest;

        ResolvedDependency deepest = deepest(root);
        deepGroupId = deepest.getGroupId();
        deepArtifactId = deepest.getArtifactId();
    }

    private static int countSubtree(ResolvedDependency d) {
        int count = 1;
        for (ResolvedDependency child : d.getDependencies()) {
            count += countSubtree(child);
        }
        return count;
    }

    /**
     * @return the dependency with the greatest depth in {@code root}'s subtree, so that looking it up by exact
     * coordinate forces the recursive walk to descend as far as possible before matching.
     */
    private static ResolvedDependency deepest(ResolvedDependency root) {
        ResolvedDependency deepest = root;
        Deque<ResolvedDependency> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ResolvedDependency d = stack.pop();
            if (d.getDepth() > deepest.getDepth()) {
                deepest = d;
            }
            for (ResolvedDependency child : d.getDependencies()) {
                stack.push(child);
            }
        }
        return deepest;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FindDependencyBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

    /**
     * Exact-coordinate lookup that walks the whole subtree before matching the deepest transitive dependency.
     */
    @Benchmark
    public void findDependencyDeepHit(Blackhole blackhole) {
        blackhole.consume(root.findDependency(deepGroupId, deepArtifactId));
    }

    /**
     * Exact-coordinate lookup for a coordinate that is absent, forcing a full graph walk.
     */
    @Benchmark
    public void findDependencyMiss(Blackhole blackhole) {
        blackhole.consume(root.findDependency("com.example.absent", "does-not-exist"));
    }

    /**
     * Glob lookup that collects every Spring artifact in the subtree.
     */
    @Benchmark
    public void findDependenciesGlob(Blackhole blackhole) {
        blackhole.consume(root.findDependencies("org.springframework*", "*"));
    }
}
