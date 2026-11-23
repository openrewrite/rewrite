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
package org.openrewrite.benchmarks.java;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Per-node cost of {@link org.openrewrite.TreeVisitor#visit} over a large compilation unit.
 */
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class TreeVisitorBenchmark {
    J.CompilationUnit cu;
    List<J.Identifier> identifiers;
    ExecutionContext ctx = new InMemoryExecutionContext();

    @Setup(Level.Trial)
    public void setup() {
        StringBuilder source = new StringBuilder("import java.util.*;\nclass Large {\n");
        for (int i = 0; i < 500; i++) {
            source.append("    int m").append(i).append("(List<String> in, int x) {\n")
                    .append("        int sum = x;\n")
                    .append("        for (String s : in) {\n")
                    .append("            if (s.length() > ").append(i).append(") {\n")
                    .append("                sum += s.hashCode() * ").append(i).append(";\n")
                    .append("            }\n")
                    .append("        }\n")
                    .append("        return sum;\n")
                    .append("    }\n");
        }
        source.append("}\n");
        cu = (J.CompilationUnit) JavaParser.fromJavaVersion().build()
                .parse(ctx, source.toString())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        identifiers = new JavaIsoVisitor<List<J.Identifier>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<J.Identifier> ids) {
                ids.add(identifier);
                return identifier;
            }
        }.reduce(cu, new ArrayList<>());
    }

    @Benchmark
    public J visit() {
        return new JavaIsoVisitor<ExecutionContext>().visitNonNull(cu, ctx);
    }

    // Helper visitors created per node pay any per-visitor setup cost many times over
    @Benchmark
    public void visitorPerNode(Blackhole bh) {
        for (J.Identifier identifier : identifiers) {
            bh.consume(new JavaIsoVisitor<ExecutionContext>().visit(identifier, ctx));
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TreeVisitorBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
