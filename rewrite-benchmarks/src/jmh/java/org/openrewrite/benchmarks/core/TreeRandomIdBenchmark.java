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
package org.openrewrite.benchmarks.core;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.Tree;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class TreeRandomIdBenchmark {

    @Benchmark
    @Threads(1)
    public void uuidRandomUUID_singleThread(Blackhole bh) {
        bh.consume(UUID.randomUUID());
    }

    @Benchmark
    @Threads(8)
    public void uuidRandomUUID_multiThread(Blackhole bh) {
        bh.consume(UUID.randomUUID());
    }

    @Benchmark
    @Threads(1)
    public void treeRandomId_singleThread(Blackhole bh) {
        bh.consume(Tree.randomId());
    }

    @Benchmark
    @Threads(8)
    public void treeRandomId_multiThread(Blackhole bh) {
        bh.consume(Tree.randomId());
    }

    @Benchmark
    @Threads(1)
    public void inlineThreadLocalRandom_singleThread(Blackhole bh) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long msb = (r.nextLong() & 0xffffffffffff0fffL) | 0x0000000000004000L;
        long lsb = (r.nextLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
        bh.consume(new UUID(msb, lsb));
    }

    @Benchmark
    @Threads(8)
    public void inlineThreadLocalRandom_multiThread(Blackhole bh) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long msb = (r.nextLong() & 0xffffffffffff0fffL) | 0x0000000000004000L;
        long lsb = (r.nextLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
        bh.consume(new UUID(msb, lsb));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TreeRandomIdBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
