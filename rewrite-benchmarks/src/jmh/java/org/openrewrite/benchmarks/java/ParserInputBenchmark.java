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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(4)
public class ParserInputBenchmark {

    @Benchmark
    public void detectCharset(JavaFiles state, Blackhole bh) {
        JavaParser.fromJavaVersion().build()
                .parseInputs(state.getSourceFiles().stream()
                                .map(Parser.Input::fromFile)
                                .collect(toList()),
                        null,
                        new InMemoryExecutionContext()
                )
                .forEach(bh::consume);
    }

    @Benchmark
    public void knownCharset(JavaFiles state, Blackhole bh) {
        ParsingExecutionContextView ctx = ParsingExecutionContextView.view(new InMemoryExecutionContext())
                .setCharset(StandardCharsets.UTF_8);
        JavaParser.fromJavaVersion().build()
                .parseInputs(state.getSourceFiles().stream()
                                .map(Parser.Input::fromFile)
                                .collect(toList()),
                        null,
                        ctx
                )
                .forEach(bh::consume);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ParserInputBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
