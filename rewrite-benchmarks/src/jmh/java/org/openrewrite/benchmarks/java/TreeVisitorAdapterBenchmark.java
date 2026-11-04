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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.marker.Markers;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

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

    // What a Java visitor pays at every JS node it reaches through an overridden visit method
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object adaptPerNode(AdaptState state) {
        return state.visitor.adapt(JavaScriptVisitor.class);
    }

    // As above, plus the adapted visitor's own visit of a tiny tree
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object adaptAndVisitPerNode(AdaptState state) {
        //noinspection unchecked
        JavaScriptVisitor<Integer> adapted = state.visitor.adapt(JavaScriptVisitor.class);
        return adapted.visit(state.identifier, 0);
    }

    @State(Scope.Thread)
    public static class AdaptState {
        final JavaIsoVisitor<Integer> visitor = new MethodInvocationVisitor();
        final J.Identifier identifier = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "x", null, null);
    }

    static class MethodInvocationVisitor extends JavaIsoVisitor<Integer> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
            return super.visitMethodInvocation(method, p);
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
