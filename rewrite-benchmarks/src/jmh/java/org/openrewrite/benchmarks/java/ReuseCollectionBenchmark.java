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
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ReuseCollectionBenchmark {
    private static final List<String> ALL_DAYS_STATIC = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private static int nextDay = 0;
    private static final String nextDayOfWeek() {
        nextDay = (nextDay + 1) % 7;
        return ALL_DAYS_STATIC.get(nextDay);
    }

    @Benchmark
    public boolean inlineSetCreation() {
        String day = nextDayOfWeek();
        return Set.of("Saturday", "Sunday").contains(day);
    }

    private static final Set<String> WEEKEND_DAYS_STATIC = Set.of("Saturday", "Sunday");

    @Benchmark
    public boolean staticFinalSet() {
        String day = nextDayOfWeek();
        return WEEKEND_DAYS_STATIC.contains(day);
    }

    @Benchmark
    public boolean orComparison() {
        String day = nextDayOfWeek();
        return "Saturday".equals(day) || "Sunday".equals(day);
    }
}
