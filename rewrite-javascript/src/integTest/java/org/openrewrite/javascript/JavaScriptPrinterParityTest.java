/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.PrintOutputCapture.MarkerPrinter;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.javascript.tree.JSX;
import org.openrewrite.javascript.tree.JsContainer;
import org.openrewrite.javascript.tree.JsLeftPadded;
import org.openrewrite.javascript.tree.JsRightPadded;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.rpc.request.Print;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.openrewrite.Tree.randomId;

/**
 * {@link JavaScriptPrinter} is a port of the TypeScript printer, which stays the reference. Everything here is
 * printed by both, so that a change to one that is not made to the other fails. A smaller comparison runs
 * from the TypeScript side in {@code rewrite/test/rpc/java-printer-parity.test.ts}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 5, unit = TimeUnit.MINUTES)
class JavaScriptPrinterParityTest {

    /**
     * Files that between them hold every type of tree the printer prints.
     */
    private final List<Source> syntax = new ArrayList<>();

    /**
     * The TypeScript sources of this module, as a body of real code.
     */
    private final List<Source> corpus = new ArrayList<>();

    record Source(JS.CompilationUnit cu, String text) {
    }

    @BeforeAll
    void parse() throws IOException, URISyntaxException {
        parse(Paths.get(getClass().getResource("/printer-parity").toURI()), syntax);
        parse(Paths.get("rewrite/src").toAbsolutePath(), corpus);
        parse(Paths.get("rewrite/test").toAbsolutePath(), corpus);
    }

    private void parse(Path directory, List<Source> sources) throws IOException {
        JavaScriptParser parser = JavaScriptParser.builder().build();
        List<Parser.Input> inputs = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).filter(parser::accept).sorted().collect(toList())) {
                String text = Files.readString(file);
                inputs.add(Parser.Input.fromString(directory.relativize(file), text));
                texts.add(text);
            }
        }

        List<SourceFile> parsed = parser.parseInputs(inputs, directory, new InMemoryExecutionContext()).collect(toList());
        assertThat(parsed).isNotEmpty().hasSameSizeAs(inputs);
        for (int i = 0; i < parsed.size(); i++) {
            assertThat(parsed.get(i)).as(inputs.get(i).getPath().toString()).isInstanceOf(JS.CompilationUnit.class);
            sources.add(new Source((JS.CompilationUnit) parsed.get(i), texts.get(i)));
        }
    }

    @AfterAll
    void shutdown() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    Stream<?> sources() {
        return Stream.concat(syntax.stream(), corpus.stream())
          .map(source -> named(source.cu().getSourcePath().toString(), source));
    }

    @ParameterizedTest
    @MethodSource("sources")
    void sourceFile(Source source) {
        String printed = source.cu().printAll();
        assertThat(printed).isEqualTo(source.text());
        assertThat(printed).isEqualTo(rpc().print(source.cu()));
    }

    @ParameterizedTest
    @MethodSource("sources")
    void markers(Source source) {
        JS.CompilationUnit marked = mark(source.cu());
        for (Print.MarkerPrinter markerPrinter : Print.MarkerPrinter.values()) {
            assertThat(marked.printAll(new PrintOutputCapture<>(0, javaMarkerPrinter(markerPrinter))))
              .as(markerPrinter.name())
              .isEqualTo(rpc().print(marked, markerPrinter));
        }
        assertThat(marked.printAll(new PrintOutputCapture<>(0, MarkerPrinter.FENCED)))
          .as("markers are printed")
          .isNotEqualTo(source.text());
    }

    /**
     * What is printed for a tree can depend on what encloses it, so both printers are given its cursor.
     */
    @ParameterizedTest
    @MethodSource("sources")
    void subtrees(Source source) {
        // every tree of the syntax files, but of the much larger corpus only the first of each type in each position
        boolean all = syntax.contains(source);
        Set<String> printed = new HashSet<>();
        new JavaScriptVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                if (tree instanceof SourceFile) {
                    return tree;
                }
                Cursor parent = getCursor().getParentOrThrow();
                Object enclosing = getCursor().getParentTreeCursor().getValue();
                if (all || printed.add(tree.getClass().getName() + enclosing.getClass().getName())) {
                    assertThat(tree.print(parent))
                      .as("%s in %s", tree.getClass().getSimpleName(), enclosing.getClass().getSimpleName())
                      .isEqualTo(rpc().print(tree, parent));
                }
                return tree;
            }
        }.visit(source.cu(), 0);
    }

    @Test
    void subtreesWhoseTextDependsOnWhatEnclosesThem() {
        JS.CompilationUnit cu = (JS.CompilationUnit) JavaScriptParser.builder().build()
          .parseInputs(List.of(Parser.Input.fromString(Paths.get("cursor.ts"),
            "const literal = { a: 1, b: 2 }, cast = <string>literal, first = items?.[0], called = a?.b();")),
            null, new InMemoryExecutionContext())
          .findFirst()
          .orElseThrow();

        List<String> printed = new ArrayList<>();
        List<String> withoutCursor = new ArrayList<>();
        new JavaScriptVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                Object enclosing = getCursor().getParentTreeCursor().getValue();
                if (tree instanceof J.Block && enclosing instanceof J.NewClass ||
                    tree instanceof J.ControlParentheses && enclosing instanceof J.TypeCast ||
                    tree instanceof J.Identifier && !tree.getMarkers().getMarkers().isEmpty() &&
                    (enclosing instanceof J.ArrayAccess || enclosing instanceof J.MethodInvocation)) {
                    Cursor parent = getCursor().getParentOrThrow();
                    printed.add(tree.print(parent));
                    assertThat(tree.print(parent)).isEqualTo(rpc().print(tree, parent));

                    PrintOutputCapture<Integer> capture = new PrintOutputCapture<>(0);
                    new JavaScriptPrinter<Integer>().visit(tree, capture);
                    withoutCursor.add(capture.getOut());
                }
                return tree;
            }
        }.visit(cu, 0);

        assertThat(printed).containsExactly("{ a: 1, b: 2 }", "<string>", "items?.", "a");
        assertThat(withoutCursor).containsExactly("{ a: 1 b: 2 }", "(string)", "items?", "a?");
    }

    /**
     * A tree type that is added to the model, or that the printer gains a method for, needs a file under
     * {@code printer-parity} that uses it.
     */
    @Test
    void everyPrintedTreeTypeIsCompared() {
        Set<Class<?>> compared = new HashSet<>();
        new JavaScriptVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                compared.add(tree.getClass());
                return tree;
            }
        }.visit(syntax.stream().map(Source::cu).collect(toList()), 0);

        Set<Class<?>> printed = new HashSet<>();
        addTreeTypes(JS.class, printed);
        addTreeTypes(JSX.class, printed);
        for (Method method : JavaScriptPrinter.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) &&
                method.getName().startsWith("visit") && method.getParameterCount() == 2 &&
                isTreeType(method.getParameterTypes()[0])) {
                printed.add(method.getParameterTypes()[0]);
            }
        }
        assertThat(compared).containsAll(printed);
    }

    private static void addTreeTypes(Class<?> declaring, Set<Class<?>> treeTypes) {
        for (Class<?> declared : declaring.getDeclaredClasses()) {
            if (isTreeType(declared)) {
                treeTypes.add(declared);
            }
            addTreeTypes(declared, treeTypes);
        }
    }

    private static boolean isTreeType(Class<?> type) {
        return J.class.isAssignableFrom(type) && !type.isInterface() && !Modifier.isAbstract(type.getModifiers());
    }

    private static JavaScriptRewriteRpc rpc() {
        return JavaScriptRewriteRpc.getOrStart();
    }

    private static MarkerPrinter javaMarkerPrinter(Print.MarkerPrinter markerPrinter) {
        switch (markerPrinter) {
            case SEARCH_MARKERS_ONLY:
                return MarkerPrinter.SEARCH_MARKERS_ONLY;
            case FENCED:
                return MarkerPrinter.FENCED;
            case SANITIZED:
                return MarkerPrinter.SANITIZED;
            default:
                return MarkerPrinter.DEFAULT;
        }
    }

    /**
     * Marks every tree, and every third comment and padding, as a recipe would.
     */
    private static JS.CompilationUnit mark(JS.CompilationUnit cu) {
        return (JS.CompilationUnit) new JavaScriptVisitor<Integer>() {
            int count;

            @Override
            public J preVisit(J tree, Integer p) {
                switch (count++ % 4) {
                    case 0:
                        return SearchResult.found(tree);
                    case 1:
                        return SearchResult.found(tree, "found");
                    case 2:
                        return Markup.info(tree, "info", "detail");
                    default:
                        return Markup.debug(tree, "debug");
                }
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                return space.withComments(ListUtils.map(space.getComments(), comment ->
                  comment.withMarkers(mark(comment.getMarkers()))));
            }

            @Override
            public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, Integer p) {
                JRightPadded<T> r = super.visitRightPadded(right, loc, p);
                return r == null ? null : r.withMarkers(mark(r.getMarkers()));
            }

            @Override
            public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JsRightPadded.Location loc, Integer p) {
                JRightPadded<T> r = super.visitRightPadded(right, loc, p);
                return r == null ? null : r.withMarkers(mark(r.getMarkers()));
            }

            @Override
            public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, Integer p) {
                JLeftPadded<T> l = super.visitLeftPadded(left, loc, p);
                return l == null ? null : l.withMarkers(mark(l.getMarkers()));
            }

            @Override
            public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JsLeftPadded.Location loc, Integer p) {
                JLeftPadded<T> l = super.visitLeftPadded(left, loc, p);
                return l == null ? null : l.withMarkers(mark(l.getMarkers()));
            }

            @Override
            public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, Integer p) {
                JContainer<J2> c = super.visitContainer(container, loc, p);
                return c == null ? null : c.withMarkers(mark(c.getMarkers()));
            }

            @Override
            public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JsContainer.Location loc, Integer p) {
                JContainer<J2> c = super.visitContainer(container, loc, p);
                return c == null ? null : c.withMarkers(mark(c.getMarkers()));
            }

            private Markers mark(Markers markers) {
                return count++ % 3 == 0 ? markers.addIfAbsent(new SearchResult(randomId(), "found")) : markers;
            }
        }.visitNonNull(cu, 0);
    }
}
