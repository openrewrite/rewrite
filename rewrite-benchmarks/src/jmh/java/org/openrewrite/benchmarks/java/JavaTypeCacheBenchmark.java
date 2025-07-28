/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.java.internal.JavaTypeCache;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 3, time = 5)
@Warmup(iterations = 3, time = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(4)
public class JavaTypeCacheBenchmark {

    @Benchmark
    public void writeSnappy(JavaCompilationUnitState state, Blackhole bh) {
        JavaTypeCache typeCache = new JavaCompilationUnitState.SnappyJavaTypeCache();
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            typeCache.put(entry.getKey(), entry.getValue());
        }
    }

//    @Benchmark
    public void writeHash(JavaCompilationUnitState state, Blackhole bh) {
        JavaTypeCache typeCache = new JavaCompilationUnitState.MapJavaTypeCache();
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            typeCache.put(new String(entry.getKey()), entry.getValue());
        }
    }

    @Benchmark
    public void writeAdaptiveRadix(JavaCompilationUnitState state, Blackhole bh) {
        JavaTypeCache typeCache = new JavaTypeCache();
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            typeCache.put(entry.getKey(), entry.getValue());
        }
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = {"--add-opens", "java.base/java.lang=ALL-UNNAMED"})
    public void writeAdaptiveRadixAddOpens(JavaCompilationUnitState state, Blackhole bh) {
        JavaTypeCache typeCache = new JavaTypeCache();
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            typeCache.put(entry.getKey(), entry.getValue());
        }
    }

    @Benchmark
    public void readSnappy(JavaCompilationUnitState state, Blackhole bh) {
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            bh.consume(state.snappyTypeCache.get(entry.getKey()));
        }
    }

//    @Benchmark
    public void readHash(JavaCompilationUnitState state, Blackhole bh) {
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            bh.consume(state.typeCache.get(entry.getKey()));
        }
    }

    @Benchmark
    public void readAdaptiveRadix(JavaCompilationUnitState state, Blackhole bh) {
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            bh.consume(state.radixMapTypeCache.get(entry.getKey()));
        }
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = {"--add-opens", "java.base/java.lang=ALL-UNNAMED"})
    public void readAdaptiveRadixAddOpens(JavaCompilationUnitState state, Blackhole bh) {
        for (Map.Entry<String, Object> entry : state.typeCache.map().entrySet()) {
            bh.consume(state.radixMapTypeCache.get(entry.getKey()));
        }
    }

    public static void main(String[] args) throws RunnerException, URISyntaxException {
        Options opt = new OptionsBuilder()
                .include(JavaTypeCacheBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .shouldFailOnError(true)
                .build();
        new Runner(opt).run();
        JavaCompilationUnitState state = new JavaCompilationUnitState();
        state.setup();
        state.printMemory();
    }
}
