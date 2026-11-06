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

import org.jspecify.annotations.Nullable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Per-node cost of the visitor framework itself over real Java sources. {@code noop}, {@code recipe} and
 * {@code cursorEveryNode} each run one visitor class in their fork, so every call site inside {@link JavaVisitor}
 * sees a single receiver. {@code mixed} runs six visitor classes per op, as a recipe run does, so those call sites
 * see many.
 */
@Fork(value = 1, jvmArgsAppend = {"-Xms2g", "-Xmx2g"})
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class VisitorOverheadBenchmark {
    List<J.CompilationUnit> cus;
    ExecutionContext ctx = new InMemoryExecutionContext();

    @Setup(Level.Trial)
    public void setup() {
        // A fixed corpus lets the arms of an A/B that edit rewrite-core parse the same sources
        String corpus = System.getProperty("visitorCorpus");
        Path srcDir = corpus != null ? Paths.get(corpus) :
                repoRoot().resolve("rewrite-core/src/main/java/org/openrewrite");
        List<Path> inputs;
        try (Stream<Path> walk = Files.walk(srcDir)) {
            inputs = walk.filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .limit(120)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parse(inputs, null, new InMemoryExecutionContext())
                .collect(Collectors.toList());
        cus = parsed.stream()
                .filter(J.CompilationUnit.class::isInstance)
                .map(J.CompilationUnit.class::cast)
                .collect(Collectors.toList());
        if (cus.size() != inputs.size()) {
            throw new IllegalStateException("Parsed " + cus.size() + " of " + inputs.size() + " sources");
        }
        Census census = new Census();
        for (J.CompilationUnit cu : cus) {
            census.visit(cu, 0);
        }
        System.out.println("\n" + census);
    }

    @Benchmark
    public void noop(Blackhole bh) {
        JavaIsoVisitor<ExecutionContext> v = new JavaIsoVisitor<>();
        for (J.CompilationUnit cu : cus) {
            bh.consume(v.visit(cu, ctx));
        }
    }

    @Benchmark
    public void recipe(Blackhole bh) {
        RecipeShaped v = new RecipeShaped();
        for (J.CompilationUnit cu : cus) {
            bh.consume(v.visit(cu, ctx));
        }
        bh.consume(v.hits);
    }

    // Every node asks for its cursor, the worst case for building cursors on demand
    @Benchmark
    public void cursorEveryNode(Blackhole bh) {
        CursorEveryNode v = new CursorEveryNode();
        for (J.CompilationUnit cu : cus) {
            bh.consume(v.visit(cu, ctx));
        }
        bh.consume(v.hits);
    }

    @Benchmark
    public void mixed(Blackhole bh) {
        RecipeShaped recipe = new RecipeShaped();
        FormatterShaped formatter = new FormatterShaped();
        TypeShaped types = new TypeShaped();
        PreconditionShaped precondition = new PreconditionShaped();
        StatementShaped statements = new StatementShaped();
        JavaIsoVisitor<ExecutionContext> noop = new JavaIsoVisitor<>();
        for (J.CompilationUnit cu : cus) {
            bh.consume(noop.visit(cu, ctx));
            bh.consume(recipe.visit(cu, ctx));
            bh.consume(formatter.visit(cu, ctx));
            bh.consume(types.visit(cu, ctx));
            bh.consume(precondition.visit(cu, ctx));
            bh.consume(statements.visit(cu, ctx));
        }
        bh.consume(recipe.hits + formatter.hits + types.hits + precondition.hits + statements.hits);
    }

    /**
     * Matches a few kinds of node by name and, only on a match, asks the cursor where it is, the way most
     * search and refactoring recipes do.
     */
    static class RecipeShaped extends JavaIsoVisitor<ExecutionContext> {
        int hits;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            getCursor().putMessage("class", classDecl.getSimpleName());
            return super.visitClassDeclaration(classDecl, ctx);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (getCursor().getParentTreeCursor().getValue() instanceof J.Block) {
                hits++;
            }
            if ("equals".equals(m.getSimpleName()) || "get".equals(m.getSimpleName())) {
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                String cls = getCursor().getNearestMessage("class");
                if (enclosing != null && cls != null) {
                    hits++;
                }
            }
            return m;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            J.Identifier i = super.visitIdentifier(identifier, ctx);
            if ("cursor".equals(i.getSimpleName()) &&
                getCursor().getParentTreeCursor().getValue() instanceof J.VariableDeclarations.NamedVariable) {
                hits++;
            }
            return i;
        }
    }

    static class CursorEveryNode extends JavaIsoVisitor<ExecutionContext> {
        int hits;

        @Override
        public J preVisit(J tree, ExecutionContext ctx) {
            if (getCursor().getValue() == tree) {
                hits++;
            }
            return tree;
        }
    }

    /**
     * Inspects every whitespace and right-padded element and consults the cursor for each, the way the
     * formatting visitors do.
     */
    static class FormatterShaped extends JavaIsoVisitor<ExecutionContext> {
        int hits;

        @Override
        public Space visitSpace(@Nullable Space space, Space.Location loc, ExecutionContext ctx) {
            if (space != null && space.getWhitespace().indexOf('\n') >= 0 && getCursor().getValue() instanceof J.Block) {
                hits++;
            }
            return super.visitSpace(space, loc, ctx);
        }

        @Override
        public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, ExecutionContext ctx) {
            if (loc == JRightPadded.Location.BLOCK_STATEMENT) {
                hits++;
            }
            return super.visitRightPadded(right, loc, ctx);
        }
    }

    /**
     * Overrides the type hook, the way type-changing recipes do, and never reads the cursor.
     */
    static class TypeShaped extends JavaIsoVisitor<ExecutionContext> {
        int hits;

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
            if (javaType instanceof JavaType.Class) {
                hits++;
            }
            return javaType;
        }
    }

    /**
     * A non-isomorphic visitor that overrides {@code visit} itself and {@code preVisit}, as precondition and
     * search visitors often do.
     */
    static class PreconditionShaped extends JavaVisitor<ExecutionContext> {
        int hits;

        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                hits++;
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J preVisit(J tree, ExecutionContext ctx) {
            if (tree instanceof J.Literal) {
                hits++;
            }
            return tree;
        }
    }

    /**
     * Overrides the statement and expression hooks.
     */
    static class StatementShaped extends JavaIsoVisitor<ExecutionContext> {
        int hits;

        @Override
        public Statement visitStatement(Statement statement, ExecutionContext ctx) {
            hits++;
            return super.visitStatement(statement, ctx);
        }

        @Override
        public Expression visitExpression(Expression expression, ExecutionContext ctx) {
            hits++;
            return super.visitExpression(expression, ctx);
        }

        @Override
        public Markers visitMarkers(@Nullable Markers markers, ExecutionContext ctx) {
            return super.visitMarkers(markers, ctx);
        }
    }

    /**
     * Counts the framework events of one walk, so per-op numbers can be read per node.
     */
    static class Census extends JavaIsoVisitor<Integer> {
        long trees, rightPadded, leftPadded, containers, spaces, nonTrivialSpaces, markers, nonEmptyMarkers;
        int maxDepth;

        @Override
        public J preVisit(J tree, Integer p) {
            trees++;
            int depth = 0;
            for (Cursor c = getCursor(); c != null; c = c.getParent()) {
                depth++;
            }
            maxDepth = Math.max(maxDepth, depth);
            return tree;
        }

        @Override
        public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, Integer p) {
            if (right != null) {
                rightPadded++;
            }
            return super.visitRightPadded(right, loc, p);
        }

        @Override
        public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, Integer p) {
            if (left != null) {
                leftPadded++;
            }
            return super.visitLeftPadded(left, loc, p);
        }

        @Override
        public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, Integer p) {
            if (container != null) {
                containers++;
            }
            return super.visitContainer(container, loc, p);
        }

        @Override
        public Space visitSpace(@Nullable Space space, Space.Location loc, Integer p) {
            spaces++;
            if (space != null && space != Space.EMPTY && space != Space.SINGLE_SPACE) {
                nonTrivialSpaces++;
            }
            return super.visitSpace(space, loc, p);
        }

        @Override
        public Markers visitMarkers(@Nullable Markers m, Integer p) {
            markers++;
            if (m != null && !m.getMarkers().isEmpty()) {
                nonEmptyMarkers++;
            }
            return super.visitMarkers(m, p);
        }

        @Override
        public String toString() {
            return String.format("census per op: trees=%d rightPadded=%d leftPadded=%d containers=%d " +
                                 "visitSpace=%d (non-trivial %d) visitMarkers=%d (non-empty %d) maxCursorDepth=%d",
                    trees, rightPadded, leftPadded, containers, spaces, nonTrivialSpaces, markers, nonEmptyMarkers, maxDepth);
        }
    }

    private static Path repoRoot() {
        for (Path p = Paths.get("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("rewrite-core/src/main/java/org/openrewrite/TreeVisitor.java"))) {
                return p;
            }
        }
        throw new IllegalStateException("Could not locate the repository root from " + Paths.get("").toAbsolutePath());
    }
}
