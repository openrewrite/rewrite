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
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.java.JavaParser;

import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
public class StarImportBenchmark {

    @Benchmark
    public void starImport() {
        //language=java
        JavaParser.fromJavaVersion().build()
                .parse("" +
                       "import java.util.*;" +
                       "import java.awt.*;" +
                       "import java.time.*;" +
                       "import java.io.*;" +
                       "import java.math.*;" +
                       "import java.nio.*;" +
                       "import java.security.*;" +
                       "class Test {" +
                       "    Collection<File> s;" +
                       "    Color c;" +
                       "    DateTimeException dte;" +
                       "    BigInteger b;" +
                       "    ByteBuffer bb;" +
                       "    Principal p;" +
                       "}"
                );
    }

    @Benchmark
    public void noStarImport() {
        //language=java
        JavaParser.fromJavaVersion().build()
                .parse("" +
                       "import java.util.Collection;" +
                       "import java.awt.Color;" +
                       "import java.time.DateTimeException;" +
                       "import java.io.File;" +
                       "import java.math.BigInteger;" +
                       "import java.nio.ByteBuffer;" +
                       "import java.security.Principal;" +
                       "class Test {" +
                       "    Collection<File> s;" +
                       "    Color c;" +
                       "    DateTimeException dte;" +
                       "    BigInteger b;" +
                       "    ByteBuffer bb;" +
                       "    Principal p;" +
                       "}"
                );
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(StarImportBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }
}
