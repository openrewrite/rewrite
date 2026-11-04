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
import org.openrewrite.internal.AdaptiveRadixTree;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Compares {@link AdaptiveRadixTree} insert/search throughput and per-operation allocation.
 * <p>
 * Two key shapes are exercised:
 * <ul>
 *   <li><b>signatures</b> — synthetic Java type signatures with heavy shared prefixes, modelling
 *       the {@code JavaTypeCache} workload (the only production user of this structure).</li>
 *   <li><b>deepChain</b> — strict extensions ("a", "aa", "aaa", ...), which build a chain of
 *       {@code keyLength==0} internal nodes one level per byte. This isolates the per-descent-level
 *       cost, which is where the recursive→iterative rewrite differs most.</li>
 * </ul>
 * Run with the {@code gc} profiler to compare {@code ·gc.alloc.rate.norm} (bytes/op).
 */
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class AdaptiveRadixTreeBenchmark {

    @State(Scope.Benchmark)
    public static class Data {
        List<byte[]> signatures;
        List<byte[]> deepChain;

        AdaptiveRadixTree<Integer> prebuiltSignatures;
        AdaptiveRadixTree<Integer> prebuiltDeepChain;

        @Setup(Level.Trial)
        public void setup() {
            signatures = generateSignatures();
            deepChain = generateDeepChain(2_000);

            prebuiltSignatures = build(signatures);
            prebuiltDeepChain = build(deepChain);
        }

        private static AdaptiveRadixTree<Integer> build(List<byte[]> keys) {
            AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
            for (int i = 0; i < keys.size(); i++) {
                tree.insert(keys.get(i), i);
            }
            return tree;
        }

        private static List<byte[]> generateSignatures() {
            String[] roots = {
                    "java.lang", "java.util", "java.util.concurrent", "java.util.stream",
                    "java.io", "java.nio.file", "com.example.app.service", "com.example.app.model",
                    "org.openrewrite.java.tree", "org.openrewrite.java.internal",
            };
            String[] returns = {"void", "boolean", "int", "java.lang.String", "java.util.List", "java.lang.Object"};
            List<byte[]> keys = new ArrayList<>();
            for (String root : roots) {
                for (int c = 0; c < 25; c++) {
                    String cls = root + ".Class" + c;
                    keys.add(cls.getBytes(StandardCharsets.UTF_8));
                    for (int f = 0; f < 6; f++) {
                        keys.add((cls + " field" + f).getBytes(StandardCharsets.UTF_8));
                    }
                    for (int m = 0; m < 12; m++) {
                        String ret = returns[m % returns.length];
                        String params = m % 3 == 0 ? "" :
                                m % 3 == 1 ? "java.lang.String" :
                                        "java.lang.String,int,java.util.List";
                        keys.add((cls + "{name=method" + m + ",return=" + ret +
                                  ",parameters=[" + params + "]}").getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            return keys;
        }

        private static List<byte[]> generateDeepChain(int depth) {
            List<byte[]> keys = new ArrayList<>(depth);
            StringBuilder sb = new StringBuilder(depth);
            for (int i = 0; i < depth; i++) {
                sb.append('a');
                keys.add(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            return keys;
        }
    }

    @Benchmark
    public AdaptiveRadixTree<Integer> insertSignatures(Data data) {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        List<byte[]> keys = data.signatures;
        for (int i = 0; i < keys.size(); i++) {
            tree.insert(keys.get(i), i);
        }
        return tree;
    }

    @Benchmark
    public void searchSignatures(Data data, Blackhole bh) {
        List<byte[]> keys = data.signatures;
        for (int i = 0; i < keys.size(); i++) {
            bh.consume(data.prebuiltSignatures.search(keys.get(i)));
        }
    }

    @Benchmark
    public AdaptiveRadixTree<Integer> insertDeepChain(Data data) {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        List<byte[]> keys = data.deepChain;
        for (int i = 0; i < keys.size(); i++) {
            tree.insert(keys.get(i), i);
        }
        return tree;
    }

    @Benchmark
    public void searchDeepChain(Data data, Blackhole bh) {
        List<byte[]> keys = data.deepChain;
        for (int i = 0; i < keys.size(); i++) {
            bh.consume(data.prebuiltDeepChain.search(keys.get(i)));
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AdaptiveRadixTreeBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
