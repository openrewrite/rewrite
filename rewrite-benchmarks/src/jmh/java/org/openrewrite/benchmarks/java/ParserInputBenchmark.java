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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
public class ParserInputBenchmark {

    @Benchmark
    public void readFromDisk(JavaFiles state) {
        //language=java
        JavaParser.fromJavaVersion().build()
                .parseInputs(state.getSourceFiles().stream()
                                .map(sourceFile -> new Parser.Input(sourceFile, () -> {
                                            try {
                                                return Files.newInputStream(sourceFile);
                                            } catch (IOException e) {
                                                throw new UncheckedIOException(e);
                                            }
                                        })
                                )
                                .collect(toList()),
                        null,
                        new InMemoryExecutionContext());
    }

    @Benchmark
    public void readFromDiskWithBufferedInputStream(JavaFiles state) {
        //language=java
        JavaParser.fromJavaVersion().build()
                .parseInputs(state.getSourceFiles().stream()
                                .map(sourceFile -> new Parser.Input(sourceFile, () -> {
                                            try {
                                                return new BufferedInputStream(Files.newInputStream(sourceFile));
                                            } catch (IOException e) {
                                                throw new UncheckedIOException(e);
                                            }
                                        })
                                )
                                .collect(toList()),
                        null,
                        new InMemoryExecutionContext());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ParserInputBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
