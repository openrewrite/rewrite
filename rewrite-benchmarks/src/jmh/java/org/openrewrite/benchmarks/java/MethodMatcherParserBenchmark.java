/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.java.MethodMatcher;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Benchmark for MethodMatcher pattern parsing performance.
 * <p>
 * Tests the performance of MethodMatcher construction with various pattern complexities,
 * including simple patterns, wildcards, varargs, and complex real-world patterns.
 */
@Fork(1)
@Measurement(iterations = 3, time = 2)
@Warmup(iterations = 3, time = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class MethodMatcherParserBenchmark {

    private Consumer<String> patternProcessor;

    @Setup(Level.Trial)
    public void setup(Blackhole bh) {
        this.patternProcessor = pattern -> bh.consume(new MethodMatcher(pattern));
    }

    /**
     * Representative method patterns from MethodMatcherTest covering various complexity levels
     */
    private static final String[] METHOD_PATTERNS = {
            // Simple patterns
            "A foo()",
            "A foo(int)",
            "A foo(String)",
            "a.A getInt()",

            // Wildcard method names
            "A *oo()",
            "A fo*()",
            "A assert*()",
            "* foo(..)",

            // Wildcard type patterns
            "*..* build()",
            "*..*Service find*(..)",
            "javax..* build()",
            "*..MyClass foo()",

            // Complex argument patterns
            "A foo(..)",
            "A foo(*, *)",
            "A foo(int, ..)",
            "A foo(.., int)",
            "A foo(int, .., double)",
            "A foo(int, *, double)",

            // Array and varargs
            "A foo(int[])",
            "A foo(String[])",
            "A foo(String, Object...)",

            // Package wildcards in arguments
            "A foo(java..*)",
            "A foo(java.util.*)",
            "A foo(*.util.*)",
            "A foo(*..*)",

            // Real-world patterns
            "java.util.List add(..)",
            "java.util.Collections *(..)",
            "org.junit.Assert assertTrue(boolean)",
            "org.junit.Assert assertTrue(*, String)",
            "java.lang.String substring(int)",
            "java.util.Map put(java.lang.Object, java.lang.Object)",

            // Constructor patterns
            "a.A <constructor>()",
            "a.A <init>()",

            // JavaScript/TypeScript patterns
            "@types/lodash..* map(..)",

            // Complex real patterns
            "org.springframework.web.bind.annotation.*Mapping *(..)",
            "javax.servlet.http.HttpServlet do*(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)",
            "java.util.concurrent.* submit(java.util.concurrent.Callable)",
            "*..*Controller *(.., javax.servlet.http.HttpServletResponse)"
    };

    /**
     * Benchmark processing all patterns sequentially.
     * This measures the overall performance with various pattern complexities.
     */
    @Benchmark
    public void allPatterns() {
        for (String pattern : METHOD_PATTERNS) {
            patternProcessor.accept(pattern);
        }
    }

    /**
     * Benchmark processing simple patterns (no wildcards or special constructs).
     * These should be the fastest to process.
     */
    @Benchmark
    public void simplePatterns() {
        patternProcessor.accept("A foo()");
        patternProcessor.accept("A foo(int)");
        patternProcessor.accept("a.A getInt()");
        patternProcessor.accept("java.lang.String substring(..)");
    }

    /**
     * Benchmark processing complex patterns with wildcards and '..' constructs.
     * These patterns trigger more complex logic.
     */
    @Benchmark
    public void complexPatterns() {
        patternProcessor.accept("*..*Service build(a.b.C, ..)");
        patternProcessor.accept("A foo(int, .., double)");
        patternProcessor.accept("org.junit.Assert assertTrue(*, String)");
        patternProcessor.accept("*..*Controller *(.., javax.servlet.http.HttpServletResponse)");
    }

    /**
     * Benchmark processing patterns with '..' (varargs).
     * Tests the optimized formalsPattern rule.
     */
    @Benchmark
    public void varargsPatterns() {
        patternProcessor.accept("foo.bar.A foo(..)");
        patternProcessor.accept("A foo(int, ..)");
        patternProcessor.accept("A foo(.., int)");
        patternProcessor.accept("A foo(int, .., double)");
    }

    /**
     * Benchmark processing patterns with wildcard method names.
     * Tests the optimized simpleNamePattern rule.
     */
    @Benchmark
    public void wildcardMethodPatterns() {
        patternProcessor.accept("A *oo()");
        patternProcessor.accept("A fo*()");
        patternProcessor.accept("A assert*()");
        patternProcessor.accept("* foo(..)");
    }

    /**
     * Run this benchmark from the command line or IDE.
     * <p>
     * To run from command line:
     * ./gradlew :rewrite-benchmarks:jmh -Pjmh.includes=MethodSignatureParserBenchmark
     * <p>
     * To run specific benchmarks:
     * ./gradlew :rewrite-benchmarks:jmh -Pjmh.includes=MethodSignatureParserBenchmark.simplePatterns
     * <p>
     * To compare parser vs MethodMatcher:
     * Results will show both modes for each benchmark method
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MethodMatcherParserBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
