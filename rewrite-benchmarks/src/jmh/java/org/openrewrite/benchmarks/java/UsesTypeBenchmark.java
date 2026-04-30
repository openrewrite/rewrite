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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Compares {@link TypesInUse#hasType(String, boolean)} (the closure-cached path) against
 * the iteration that {@code UsesType.visit} performed before the cache was added. Each
 * benchmark issues 100 successive queries against a single parsed compilation unit using
 * different FQNs.
 */
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class UsesTypeBenchmark {

    /**
     * 100 FQNs to query: a mix of hits (reachable through types-in-use, imports, or supertype
     * walks) and misses (types not used by the sample file). The proportions are roughly
     * representative of recipe preconditions in a mixed migration composite — most queries miss.
     */
    static final List<String> QUERIES = Arrays.asList(
            // ~30 likely hits (common JDK types reachable via imports + supertype walks)
            "java.lang.Object", "java.lang.String", "java.lang.Comparable", "java.lang.CharSequence",
            "java.lang.Throwable", "java.lang.Exception", "java.lang.RuntimeException", "java.lang.Iterable",
            "java.lang.Number", "java.lang.Integer", "java.lang.Boolean", "java.lang.Long",
            "java.util.Collection", "java.util.List", "java.util.ArrayList", "java.util.Map",
            "java.util.HashMap", "java.util.Set", "java.util.HashSet", "java.util.Iterator",
            "java.util.function.Function", "java.util.function.Predicate", "java.util.function.Consumer",
            "java.util.function.Supplier", "java.util.stream.Stream", "java.util.Optional",
            "java.io.Serializable", "java.lang.AutoCloseable", "java.util.AbstractList", "java.util.AbstractMap",
            // ~70 likely misses (types not in the sample file)
            "javax.swing.JFrame", "javax.swing.JButton", "javax.swing.JPanel", "javax.swing.JTable",
            "java.awt.Color", "java.awt.Graphics", "java.awt.event.ActionListener",
            "java.sql.Connection", "java.sql.PreparedStatement", "java.sql.ResultSet",
            "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpServlet", "javax.servlet.ServletContext",
            "org.springframework.context.ApplicationContext", "org.springframework.beans.factory.BeanFactory",
            "org.springframework.web.bind.annotation.RestController", "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Component", "org.springframework.stereotype.Repository",
            "org.springframework.boot.SpringApplication", "org.springframework.data.jpa.repository.JpaRepository",
            "org.springframework.transaction.annotation.Transactional",
            "org.junit.jupiter.api.Test", "org.junit.jupiter.api.BeforeEach", "org.junit.jupiter.api.AfterEach",
            "org.junit.jupiter.api.BeforeAll", "org.junit.jupiter.api.AfterAll", "org.junit.jupiter.api.Disabled",
            "org.mockito.Mockito", "org.mockito.Mock", "org.mockito.InjectMocks", "org.mockito.Spy",
            "org.assertj.core.api.Assertions", "org.assertj.core.api.AssertionsForClassTypes",
            "org.hibernate.Session", "org.hibernate.SessionFactory", "org.hibernate.Transaction",
            "javax.persistence.Entity", "javax.persistence.Id", "javax.persistence.Column", "javax.persistence.Table",
            "javax.persistence.GeneratedValue", "javax.persistence.OneToMany", "javax.persistence.ManyToOne",
            "com.fasterxml.jackson.annotation.JsonProperty", "com.fasterxml.jackson.databind.ObjectMapper",
            "com.fasterxml.jackson.core.JsonGenerator", "com.fasterxml.jackson.databind.JsonNode",
            "ch.qos.logback.classic.Logger", "ch.qos.logback.classic.LoggerContext",
            "org.slf4j.Logger", "org.slf4j.LoggerFactory",
            "io.netty.channel.Channel", "io.netty.channel.ChannelHandler", "io.netty.bootstrap.ServerBootstrap",
            "reactor.core.publisher.Mono", "reactor.core.publisher.Flux",
            "kotlinx.coroutines.flow.Flow", "kotlin.coroutines.Continuation",
            "scala.collection.immutable.List", "scala.Option", "scala.concurrent.Future",
            "groovy.lang.Closure", "groovy.lang.MetaClass",
            "com.example.MadeUpType", "com.example.AnotherMadeUp", "com.example.foo.Bar", "com.example.foo.Baz",
            "org.example.never.exists.Foo", "org.example.never.exists.Bar"
    );

    JavaSourceFile sample;
    Field trieField;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Use a non-trivial Java file: many imports, a class hierarchy, and a healthy amount of
        // type usage. Inlined here so the benchmark is self-contained and reproducible.
        @org.intellij.lang.annotations.Language("java")
        String source = """
                package org.openrewrite.benchmarks.sample;
                
                import java.util.ArrayList;
                import java.util.Collection;
                import java.util.Collections;
                import java.util.HashMap;
                import java.util.HashSet;
                import java.util.LinkedHashMap;
                import java.util.LinkedList;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;
                import java.util.Set;
                import java.util.TreeMap;
                import java.util.UUID;
                import java.util.concurrent.ConcurrentHashMap;
                import java.util.function.Function;
                import java.util.function.Predicate;
                import java.util.stream.Collectors;
                import java.util.stream.Stream;
                import java.io.IOException;
                import java.io.UncheckedIOException;
                import java.nio.file.Path;
                import java.nio.file.Paths;
                import java.nio.file.Files;
                
                public class Sample {
                    private final Map<String, List<UUID>> grouped = new HashMap<>();
                    private final Set<String> seen = new HashSet<>();
                    private final ConcurrentHashMap<String, Integer> counters = new ConcurrentHashMap<>();
                
                    public Optional<List<UUID>> lookup(String key) {
                        return Optional.ofNullable(grouped.get(key));
                    }
                
                    public List<String> sortedKeys(Predicate<String> filter) {
                        return grouped.keySet().stream()
                                .filter(filter)
                                .sorted()
                                .collect(Collectors.toList());
                    }
                
                    public Map<String, Integer> counts(Function<String, Integer> mapper) {
                        Map<String, Integer> out = new LinkedHashMap<>();
                        for (String key : seen) {
                            out.put(key, mapper.apply(key));
                        }
                        return out;
                    }
                
                    public void incrementAll(Collection<String> keys) {
                        for (String key : keys) {
                            counters.merge(key, 1, Integer::sum);
                        }
                    }
                
                    public Stream<Path> walkTree(Path root) {
                        try {
                            return Files.walk(root);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                
                    public TreeMap<String, LinkedList<String>> grouped(List<String> input) {
                        TreeMap<String, LinkedList<String>> by = new TreeMap<>();
                        for (String s : input) {
                            by.computeIfAbsent(s.substring(0, 1), k -> new LinkedList<>()).add(s);
                        }
                        return by;
                    }
                
                    public List<UUID> ids(int n) {
                        List<UUID> out = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            out.add(UUID.randomUUID());
                        }
                        return Collections.unmodifiableList(out);
                    }
                
                    public static Path resolve(String first, String... more) {
                        return Paths.get(first, more);
                    }
                }
                """;

        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parse(new InMemoryExecutionContext(), source)
                .toList();
        sample = (JavaSourceFile) parsed.getFirst();

        // Ensure type information is realized once up front so it doesn't pollute the first
        // measurement iteration. Doesn't build the closure — that's the cache we're measuring.
        sample.getTypesInUse().getTypesInUse().size();

        trieField = TypesInUse.class.getDeclaredField("trie");
        trieField.setAccessible(true);
    }

    /**
     * Closure already built (warmed up). Measures the steady-state cost of 100 successive
     * {@code hasType} queries — pure {@code Map.get}.
     */
    @Benchmark
    public void cachedHot(Blackhole bh) {
        TypesInUse tiu = sample.getTypesInUse();
        for (String fqn : QUERIES) {
            bh.consume(tiu.hasType(fqn, false));
        }
    }

    /**
     * Trie cleared before each invocation. Measures the cost of building the trie once plus 100
     * successive queries against it. This is the worst case for the cached path.
     */
    @Benchmark
    public void cachedCold(Blackhole bh) throws IllegalAccessException {
        TypesInUse tiu = sample.getTypesInUse();
        trieField.set(tiu, null);
        for (String fqn : QUERIES) {
            bh.consume(tiu.hasType(fqn, false));
        }
    }

    /**
     * Replicates the iteration {@code UsesType.visit} performed before this change: per query,
     * iterate types-in-use and imports calling {@code TypeUtils.isAssignableTo}. This is what
     * 100 successive {@code UsesType} invocations cost without any caching.
     */
    @Benchmark
    public void uncachedIteration(Blackhole bh) {
        for (String fqn : QUERIES) {
            bh.consume(legacyHasType(sample, fqn));
        }
    }

    private static boolean legacyHasType(JavaSourceFile cu, String fqn) {
        for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
            JavaType checkType = type instanceof JavaType.Primitive ? type : TypeUtils.asFullyQualified(type);
            if (checkType != null && TypeUtils.isAssignableTo(fqn, checkType)) {
                return true;
            }
        }
        for (J.Import anImport : cu.getImports()) {
            JavaType target = anImport.isStatic()
                    ? anImport.getQualid().getTarget().getType()
                    : anImport.getQualid().getType();
            JavaType checkType = TypeUtils.asFullyQualified(target);
            if (checkType != null && TypeUtils.isAssignableTo(fqn, checkType)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UsesTypeBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
