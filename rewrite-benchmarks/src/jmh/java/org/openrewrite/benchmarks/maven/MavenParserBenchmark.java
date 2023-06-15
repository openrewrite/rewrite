/*
 * Copyright 2021 the original author or authors.
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

import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 1)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class MavenParserBenchmark {
    CompositeMavenPomCache pomCache = new CompositeMavenPomCache(
            new InMemoryMavenPomCache(),
            new RocksdbMavenPomCache(Paths.get(System.getProperty("user.home")))
    );

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MavenParserBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void parse(Blackhole blackhole) {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setPomCache(pomCache);

        Optional<SourceFile> maven = MavenParser.builder().build().parse(ctx,
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
                "      <artifactId>spring-boot-starter-webflux</artifactId>" +
                "    </dependency>" +
                "    <dependency>" +
                "      <groupId>org.springframework.cloud</groupId>" +
                "      <artifactId>spring-cloud-dataflow-tasklauncher</artifactId>" +
                "      <version>2.9.2</version>" +
                "    </dependency>" +
                "  </dependencies>" +
                "</project>"
        ).findFirst();

        blackhole.consume(maven);
    }
}
