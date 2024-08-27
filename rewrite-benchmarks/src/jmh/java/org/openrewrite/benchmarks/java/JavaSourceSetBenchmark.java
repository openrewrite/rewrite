package org.openrewrite.benchmarks.java;

import org.openjdk.jmh.annotations.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(4)
public class JavaSourceSetBenchmark {

    List<Path> classpath;

    @Setup
    public void setup() {
        classpath = JavaParser.runtimeClasspath();
    }

    @Benchmark
    public void jarIOBenchmark() {
        JavaSourceSet.build("main", classpath);
    }

    @Benchmark
    public void classgraphBenchmark() {
        //noinspection deprecation
        JavaSourceSet.build("main", classpath, new JavaTypeCache(), false);
    }
}
