/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.tree.JavaType;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.tree.JavaType.ShallowClass.build;

@Fork(1)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@State(Scope.Benchmark)
public class MethodMatcherBenchmark {

    // Simple exact match patterns
    @State(Scope.Benchmark)
    public static class SimpleExactMatch {
        MethodMatcher matcher = new MethodMatcher("java.util.List add(java.lang.Object)");
        JavaType.Method method = newMethodType("java.util.List", "add", "java.lang.Object");
        JavaType.Method nonMatchingMethod = newMethodType("java.util.List", "remove", "java.lang.Object");
    }

    @Benchmark
    public void simpleExactMatch(SimpleExactMatch state, Blackhole bh) {
        bh.consume(state.matcher.matches(state.method));
    }

    @Benchmark
    public void simpleExactMatchNegative(SimpleExactMatch state, Blackhole bh) {
        bh.consume(state.matcher.matches(state.nonMatchingMethod));
    }

    // Wildcard type patterns
    @State(Scope.Benchmark)
    public static class WildcardType {
        MethodMatcher matcher = new MethodMatcher("java.util.* add(java.lang.Object)");
        JavaType.Method listMethod = newMethodType("java.util.List", "add", "java.lang.Object");
        JavaType.Method setMethod = newMethodType("java.util.Set", "add", "java.lang.Object");
        JavaType.Method mapMethod = newMethodType("java.util.Map", "put", "java.lang.Object", "java.lang.Object");
    }

    @Benchmark
    public void wildcardTypeMatch(WildcardType state, Blackhole bh) {
        bh.consume(state.matcher.matches(state.listMethod));
        bh.consume(state.matcher.matches(state.setMethod));
    }

    @Benchmark
    public void wildcardTypeNonMatch(WildcardType state, Blackhole bh) {
        bh.consume(state.matcher.matches(state.mapMethod));
    }

    // Wildcard method name patterns
    @State(Scope.Benchmark)
    public static class WildcardMethodName {
        MethodMatcher matcher = new MethodMatcher("java.io.PrintStream print*(..)");
        JavaType.Method printMethod = newMethodType("java.io.PrintStream", "print", "java.lang.String");
        JavaType.Method printlnMethod = newMethodType("java.io.PrintStream", "println", "java.lang.String");
        JavaType.Method printfMethod = newMethodType("java.io.PrintStream", "printf", "java.lang.String", "java.lang.Object[]");
        JavaType.Method flushMethod = newMethodType("java.io.PrintStream", "flush");
    }

    @Benchmark
    public void wildcardMethodNameMatch(WildcardMethodName state, Blackhole bh) {
        bh.consume(state.matcher.matches(state.printMethod));
        bh.consume(state.matcher.matches(state.printlnMethod));
        bh.consume(state.matcher.matches(state.printfMethod));
    }

    @Benchmark
    public void wildcardMethodNameNonMatch(WildcardMethodName state, Blackhole bh) {
        bh.consume(state.matcher.matches(state.flushMethod));
    }

    // Varargs patterns
    @State(Scope.Benchmark)
    public static class VarargsPattern {
        MethodMatcher anyArgsPattern = new MethodMatcher("java.util.List *(..)");
        MethodMatcher prefixArgsPattern = new MethodMatcher("java.lang.String format(java.lang.String, ..)");
        MethodMatcher suffixArgsPattern = new MethodMatcher("org.junit.Assert assertTrue(.., java.lang.String)");

        JavaType.Method noArgsMethod = newMethodType("java.util.List", "clear");
        JavaType.Method oneArgMethod = newMethodType("java.util.List", "add", "java.lang.Object");
        JavaType.Method twoArgsMethod = newMethodType("java.util.List", "add", "int", "java.lang.Object");

        JavaType.Method formatNoVarargs = newMethodType("java.lang.String", "format", "java.lang.String");
        JavaType.Method formatWithVarargs = newMethodType("java.lang.String", "format", "java.lang.String", "java.lang.Object", "java.lang.Object");

        JavaType.Method assertWithMessage = newMethodType("org.junit.Assert", "assertTrue", "boolean", "java.lang.String");
        JavaType.Method assertNoMessage = newMethodType("org.junit.Assert", "assertTrue", "boolean");
    }

    @Benchmark
    public void varargsAnyMatch(VarargsPattern state, Blackhole bh) {
        bh.consume(state.anyArgsPattern.matches(state.noArgsMethod));
        bh.consume(state.anyArgsPattern.matches(state.oneArgMethod));
        bh.consume(state.anyArgsPattern.matches(state.twoArgsMethod));
    }

    @Benchmark
    public void varargsPrefixMatch(VarargsPattern state, Blackhole bh) {
        bh.consume(state.prefixArgsPattern.matches(state.formatNoVarargs));
        bh.consume(state.prefixArgsPattern.matches(state.formatWithVarargs));
    }

    @Benchmark
    public void varargsSuffixMatch(VarargsPattern state, Blackhole bh) {
        bh.consume(state.suffixArgsPattern.matches(state.assertWithMessage));
    }

    @Benchmark
    public void varargsSuffixNonMatch(VarargsPattern state, Blackhole bh) {
        bh.consume(state.suffixArgsPattern.matches(state.assertNoMessage));
    }

    // Complex patterns with multiple wildcards
    @State(Scope.Benchmark)
    public static class ComplexPattern {
        MethodMatcher servicePattern = new MethodMatcher("*..*Service *find*(..)");
        MethodMatcher controllerPattern = new MethodMatcher("*..*Controller *(.., javax.servlet.http.HttpServletResponse)");

        JavaType.Method[] serviceMethods = new JavaType.Method[] {
            newMethodType("com.example.UserService", "findById", "long"),
            newMethodType("com.example.data.ProductService", "findByName", "java.lang.String"),
            newMethodType("org.company.CustomerService", "findAll"),
            newMethodType("com.example.UserService", "saveUser", "com.example.User"),
            newMethodType("com.example.UserRepository", "findById", "long")
        };

        JavaType.Method controllerMethod = newMethodType("com.example.web.UserController", "handleRequest",
            "java.lang.String", "javax.servlet.http.HttpServletResponse");
        JavaType.Method controllerNoResponse = newMethodType("com.example.web.UserController", "handleRequest",
            "java.lang.String");
    }

    @Benchmark
    public void complexServicePattern(ComplexPattern state, Blackhole bh) {
        for (JavaType.Method method : state.serviceMethods) {
            bh.consume(state.servicePattern.matches(method));
        }
    }

    @Benchmark
    public void complexControllerPattern(ComplexPattern state, Blackhole bh) {
        bh.consume(state.controllerPattern.matches(state.controllerMethod));
        bh.consume(state.controllerPattern.matches(state.controllerNoResponse));
    }

    // Single wildcard argument patterns
    @State(Scope.Benchmark)
    public static class WildcardArguments {
        MethodMatcher singleWildcard = new MethodMatcher("org.junit.Assert assertEquals(*, *)");
        MethodMatcher mixedWildcard = new MethodMatcher("java.util.Map put(java.lang.String, *)");

        JavaType.Method assertEquals = newMethodType("org.junit.Assert", "assertEquals", "java.lang.Object", "java.lang.Object");
        JavaType.Method assertEqualsInt = newMethodType("org.junit.Assert", "assertEquals", "int", "int");
        JavaType.Method assertEqualsDelta = newMethodType("org.junit.Assert", "assertEquals", "double", "double", "double");

        JavaType.Method mapPut = newMethodType("java.util.Map", "put", "java.lang.String", "java.lang.Object");
        JavaType.Method mapPutWrongKey = newMethodType("java.util.Map", "put", "java.lang.Integer", "java.lang.Object");
    }

    @Benchmark
    public void wildcardArgumentsMatch(WildcardArguments state, Blackhole bh) {
        bh.consume(state.singleWildcard.matches(state.assertEquals));
        bh.consume(state.singleWildcard.matches(state.assertEqualsInt));
    }

    @Benchmark
    public void wildcardArgumentsNonMatch(WildcardArguments state, Blackhole bh) {
        bh.consume(state.singleWildcard.matches(state.assertEqualsDelta));
    }

    @Benchmark
    public void mixedWildcardMatch(WildcardArguments state, Blackhole bh) {
        bh.consume(state.mixedWildcard.matches(state.mapPut));
    }

    @Benchmark
    public void mixedWildcardNonMatch(WildcardArguments state, Blackhole bh) {
        bh.consume(state.mixedWildcard.matches(state.mapPutWrongKey));
    }

    // Subpackage patterns
    @State(Scope.Benchmark)
    public static class SubpackagePattern {
        MethodMatcher subpackagePattern = new MethodMatcher("com.example..* save(..)");
        MethodMatcher deepSubpackagePattern = new MethodMatcher("org.springframework..* *(..)");

        JavaType.Method[] methods = new JavaType.Method[] {
            newMethodType("com.example.UserService", "save", "com.example.User"),
            newMethodType("com.example.data.ProductRepository", "save", "com.example.Product"),
            newMethodType("com.example.web.controller.AdminController", "save"),
            newMethodType("com.other.UserService", "save", "com.other.User"),
            newMethodType("org.springframework.web.bind.annotation.GetMapping", "value"),
            newMethodType("org.springframework.data.jpa.repository.JpaRepository", "save", "java.lang.Object")
        };
    }

    @Benchmark
    public void subpackagePatternMatch(SubpackagePattern state, Blackhole bh) {
        for (JavaType.Method method : state.methods) {
            bh.consume(state.subpackagePattern.matches(method));
        }
    }

    @Benchmark
    public void deepSubpackagePatternMatch(SubpackagePattern state, Blackhole bh) {
        for (JavaType.Method method : state.methods) {
            bh.consume(state.deepSubpackagePattern.matches(method));
        }
    }

    // Helper method to create JavaType.Method instances
    private static JavaType.Method newMethodType(String type, String method, String... parameterTypes) {
        List<JavaType> parameterTypeList = Stream.of(parameterTypes)
            .map(name -> {
                JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(name);
                if (primitive != null) {
                    return primitive;
                } else if ("int".equals(name)) {
                    return JavaType.Primitive.Int;
                } else if ("long".equals(name)) {
                    return JavaType.Primitive.Long;
                } else if ("double".equals(name)) {
                    return JavaType.Primitive.Double;
                } else if ("boolean".equals(name)) {
                    return JavaType.Primitive.Boolean;
                } else {
                    return JavaType.ShallowClass.build(name);
                }
            })
            .map(JavaType.class::cast)
            .toList();

        return new JavaType.Method(
            null,
            1L,
            build(type),
            method,
            null,
            null,
            parameterTypeList,
            emptyList(),
            emptyList(),
            emptyList(),
            null
        );
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MethodMatcherBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
