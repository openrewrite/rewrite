/*
 * Copyright 2025 the original author or authors.
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static org.openrewrite.java.tree.JavaType.ShallowClass.build;

/**
 * Isolates {@link TypesInUse#hasMethodUse(MethodMatcher)} against a synthetic used-method set whose size
 * varies with {@code methodCount}. The indexed path binary-searches a per-set array sorted by canonical
 * declaring-type FQN to the matcher's literal declaring-type prefix; the {@code scan} baseline replicates the
 * pre-change behavior of testing every method in the set. {@code indexedCold} additionally pays the one-time
 * O(n log n) sort, the worst case for the indexed path.
 */
@Fork(1)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@State(Scope.Benchmark)
public class HasMethodUseBenchmark {

    @State(Scope.Benchmark)
    public static class MethodSet {
        @Param({"100", "1000", "5000"})
        int methodCount;

        JavaSourceFile cu;
        TypesInUse typesInUse;
        MethodMatcher matcher;
        Field usedMethodIndex;

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException {
            List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parse(new InMemoryExecutionContext(), "class A {}")
                    .toList();
            cu = (JavaSourceFile) parsed.get(0);

            Set<JavaType.Method> usedMethods = newSetFromMap(new IdentityHashMap<>());
            for (int i = 0; i < methodCount; i++) {
                usedMethods.add(newMethodType("com.example.svc" + i + ".Service" + i, "find", "long"));
            }
            typesInUse = TypesInUse.of(cu, emptySet(), emptySet(), usedMethods, emptySet());

            // A literal-prefix pattern targeting the middle declaring type, so the indexed path narrows
            // to a single candidate while the scan must walk the whole set on a negative-leaning query.
            int mid = methodCount / 2;
            matcher = new MethodMatcher("com.example.svc" + mid + ".Service" + mid + " find(..)");

            usedMethodIndex = TypesInUse.class.getDeclaredField("usedMethodIndex");
            usedMethodIndex.setAccessible(true);

            // Build and cache the index once so indexedHot measures only the binary search.
            typesInUse.hasMethodUse(matcher);
        }
    }

    /** Index already built; measures the steady-state binary-search-and-confirm cost. */
    @Benchmark
    public void indexedHot(MethodSet state, Blackhole bh) {
        bh.consume(state.typesInUse.hasMethodUse(state.matcher));
    }

    /** Index cleared before each invocation; measures the one-time sort plus a single query. */
    @Benchmark
    public void indexedCold(MethodSet state, Blackhole bh) throws IllegalAccessException {
        state.usedMethodIndex.set(state.typesInUse, null);
        bh.consume(state.typesInUse.hasMethodUse(state.matcher));
    }

    /** Replicates the pre-change behavior: scan every used method calling {@link MethodMatcher#matches}. */
    @Benchmark
    public void scan(MethodSet state, Blackhole bh) {
        boolean found = false;
        for (JavaType.Method m : state.typesInUse.getUsedMethods()) {
            if (state.matcher.matches(m)) {
                found = true;
                break;
            }
        }
        bh.consume(found);
    }

    private static JavaType.Method newMethodType(String type, String method, String... parameterTypes) {
        List<JavaType> parameterTypeList = new java.util.ArrayList<>(parameterTypes.length);
        for (String name : parameterTypes) {
            JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(name);
            parameterTypeList.add(primitive != null ? primitive : JavaType.ShallowClass.build(name));
        }
        return new JavaType.Method(
                null,
                1L,
                build(type),
                method,
                null,
                null,
                parameterTypeList,
                emptyList(),
                emptyList(),
                emptyList(),
                null
        );
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(HasMethodUseBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
