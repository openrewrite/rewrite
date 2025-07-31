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

import org.fastfilter.bloom.Bloom;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.SourceFile;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.newSetFromMap;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
public class WeighingJavaAstsBenchmark {

    @Benchmark
    public void bloomFilter(JavaCompilationUnitState state, Blackhole blackhole) {
        Bloom bloom = Bloom.construct(new long[0], 1);
        long weight = 0;
        for (SourceFile sourceFile : state.getSourceFiles()) {
            weight += sourceFile.getWeight(t -> {
                int id = System.identityHashCode(t);
                if (bloom.mayContain(id)) {
                    return false;
                }
                bloom.add(id);
                return true;
            });
        }
        blackhole.consume(weight);
    }

    @Benchmark
    public void identitySet(JavaCompilationUnitState state, Blackhole blackhole) {
        Set<Object> uniqueTypes = newSetFromMap(new IdentityHashMap<>());
        long weight = 0;
        for (SourceFile sourceFile : state.getSourceFiles()) {
            weight += sourceFile.getWeight(uniqueTypes::add);
        }
        blackhole.consume(weight);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WeighingJavaAstsBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
