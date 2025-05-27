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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.TreeVisitorAdapter;
import org.openrewrite.java.JavaVisitor;

import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
public class TreeVisitorAdapterBenchmark {

    @Benchmark
    public void adaptToJava(JavaCompilationUnitState cus) {
        for (SourceFile cu : cus.getSourceFiles()) {
            //noinspection unchecked
            TreeVisitorAdapter.adapt(new TreeVisitor<Tree, Integer>() {
                @Override
                public Tree preVisit(Tree tree, Integer p) {
                    return super.preVisit(tree, p);
                }
            }, JavaVisitor.class).visitNonNull(cu, 0);
        }
    }

    @Benchmark
    public void noAdaptation(JavaCompilationUnitState cus) {
        for (SourceFile cu : cus.getSourceFiles()) {
            new JavaVisitor<>().visitNonNull(cu, 0);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TreeVisitorAdapterBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
