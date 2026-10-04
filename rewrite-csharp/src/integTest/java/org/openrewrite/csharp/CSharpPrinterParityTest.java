/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.csharp;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.csharp.tree.Linq;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.rpc.request.Print;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

/**
 * {@link CSharpPrinter} is a port of the native printer. These tests are what holds the two
 * together: every tree is printed by both, and the output compared to the other's and to the source.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
class CSharpPrinterParityTest {

    static final List<PrintOutputCapture.MarkerPrinter> MARKER_PRINTERS = List.of(
      PrintOutputCapture.MarkerPrinter.DEFAULT,
      PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY,
      PrintOutputCapture.MarkerPrinter.FENCED,
      PrintOutputCapture.MarkerPrinter.SANITIZED
    );

    private static final Pattern FENCE = Pattern.compile("\\{\\{[0-9a-f-]{36}}}");

    @BeforeAll
    static void setUpFactory() {
        CSharpRewriteRpc.setFactory(CSharpRewriteRpc.builder()
          .csharpServerEntry(csharpDir().resolve("OpenRewrite.Tool/OpenRewrite.Tool.csproj"))
          .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-printer-parity.log")));
    }

    @AfterEach
    void tearDown() {
        CSharpRewriteRpc.resetCurrent();
    }

    @AfterAll
    static void shutDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    /**
     * The module is the working directory under Gradle and the repository root when a run is
     * launched from there.
     */
    static Path csharpDir() {
        Path base = Paths.get(System.getProperty("user.dir"));
        return Stream.of(base.resolve("csharp"), base.resolve("rewrite-csharp/csharp"))
          .filter(dir -> Files.exists(dir.resolve("OpenRewrite.Tool/OpenRewrite.Tool.csproj")))
          .findFirst()
          .map(dir -> dir.toAbsolutePath().normalize())
          .orElseThrow(() -> new IllegalStateException("Could not find the C# Rewrite project"));
    }

    /**
     * The native half of this module is the largest body of C# in the repository.
     */
    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void ownSources() throws IOException {
        Path root = csharpDir();
        Map<Path, String> sources = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(CSharpPrinterParityTest::isSource).sorted().collect(toList())) {
                sources.put(root.relativize(file), read(file));
            }
        }
        assertThat(sources).hasSizeGreaterThan(200);

        List<String> failures = new ArrayList<>();
        assertParity(sources).forEach((path, cu) -> compareMarkerPrinters(path, mark(cu), failures));
        assertThat(failures).isEmpty();
    }

    private static boolean isSource(Path file) {
        if (!file.toString().endsWith(".cs")) {
            return false;
        }
        for (Path segment : file) {
            if ("bin".equals(segment.toString()) || "obj".equals(segment.toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * A tree type that is added without a source here to exercise it fails this test.
     */
    @Test
    void everyTreeType() {
        Set<Class<?>> printed = new TreeSet<>(Comparator.comparing(Class::getName));
        Collection<Cs.CompilationUnit> compilationUnits = assertParity(SYNTAX).values();
        for (Cs.CompilationUnit cu : compilationUnits) {
            printed.addAll(treeTypes(cu));
        }

        // The parser does not produce these, so there is no source to compare them to.
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        Cursor cursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), compilationUnits.iterator().next());
        for (J tree : treesWithoutSyntax()) {
            assertThat(tree.print(cursor))
              .as(tree.getClass().getSimpleName())
              .isEqualTo(rpc.print(tree, cursor));
            printed.addAll(treeTypes(tree));
        }

        Set<Class<?>> expected = new TreeSet<>(Comparator.comparing(Class::getName));
        addTreeTypes(Cs.class, expected);
        addTreeTypes(Linq.class, expected);
        for (Method method : CSharpPrinter.class.getDeclaredMethods()) {
            if (method.getName().startsWith("visit") && Modifier.isPublic(method.getModifiers()) &&
                method.getParameterCount() == 2 && J.class.isAssignableFrom(method.getParameterTypes()[0])) {
                expected.add(method.getParameterTypes()[0]);
            }
        }
        // printed as part of the tree that holds them
        expected.addAll(List.of(J.Try.Catch.class, J.If.Else.class, J.Modifier.class, J.TypeParameters.class,
          J.VariableDeclarations.NamedVariable.class, J.ForLoop.Control.class, J.ForEachLoop.Control.class,
          J.Lambda.Parameters.class));

        expected.removeAll(printed);
        assertThat(expected).as("tree types that no source exercises").isEmpty();
    }

    private static void addTreeTypes(Class<?> declaring, Set<Class<?>> types) {
        for (Class<?> declared : declaring.getDeclaredClasses()) {
            if (J.class.isAssignableFrom(declared) && !declared.isInterface()) {
                types.add(declared);
            }
            addTreeTypes(declared, types);
        }
    }

    private static Set<Class<?>> treeTypes(J tree) {
        Set<Class<?>> types = new TreeSet<>(Comparator.comparing(Class::getName));
        new CSharpVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                types.add(tree.getClass());
                return tree;
            }
        }.visit(tree, 0);
        return types;
    }

    private static List<J> treesWithoutSyntax() {
        J.Identifier a = identifier("a");
        J.Identifier b = identifier("b").withPrefix(Space.SINGLE_SPACE);
        return List.of(
          new Cs.NameColon(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, JRightPadded.build(a).withAfter(Space.SINGLE_SPACE)),
          new Cs.ArrayRankSpecifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
            JContainer.build(Space.SINGLE_SPACE, List.of(JRightPadded.<Expression>build(a), JRightPadded.<Expression>build(b)), Markers.EMPTY)),
          new Cs.PointerFieldAccess(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, a,
            JLeftPadded.build(b).withBefore(Space.SINGLE_SPACE), null),
          new J.Package(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, b, emptyList()),
          new J.EnumValueSet(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, List.of(
            JRightPadded.build(new J.EnumValue(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), a, null)).withAfter(Space.SINGLE_SPACE),
            JRightPadded.build(new J.EnumValue(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), b, null))), false)
        );
    }

    private static J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }

    private static J.Literal literalWithoutSource(@Nullable Object value, JavaType.Primitive type) {
        return new J.Literal(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, value, null, null, type);
    }

    @Test
    void preprocessorDirectives() {
        assertParity(DIRECTIVES);
    }

    /**
     * Only the caller's marker printer may add to the source, and that holds for the branches
     * of a conditional directive as much as for the rest of the file.
     */
    @Test
    void markers() {
        Map<Path, String> sources = sources(SYNTAX, DIRECTIVES);
        List<String> failures = new ArrayList<>();
        Set<String> unprinted = new TreeSet<>();
        parse(sources).forEach((path, cu) -> {
            Map<UUID, String> added = new LinkedHashMap<>();
            Cs.CompilationUnit marked = mark(cu, added);
            compareMarkerPrinters(path, marked, failures);

            assertThat(marked.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
              .as("%s sanitized", path)
              .isEqualTo(sources.get(path));
            String fenced = marked.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.FENCED));
            assertThat(FENCE.matcher(fenced).replaceAll(""))
              .as("%s without its fences", path)
              .isEqualTo(sources.get(path));

            // only one branch of a conditional directive is printed for any part of the file
            if (!sources.get(path).contains("#if")) {
                added.forEach((id, on) -> {
                    if (fenced.split(Pattern.quote("{{" + id + "}}"), -1).length != 3) {
                        unprinted.add(on);
                    }
                });
            }
        });
        assertThat(failures).isEmpty();
        assertThat(unprinted).as("trees whose markers are not printed before and after them").isEmpty();
    }

    /**
     * The native side prints the tree it parsed until it is reset, and only then the one it is sent.
     */
    @Test
    void sentTrees() {
        Map<Path, String> sources = sources(SYNTAX, DIRECTIVES);
        Map<Path, Cs.CompilationUnit> parsed = parse(sources);
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        rpc.reset();
        parsed.forEach((path, cu) -> assertThat(rpc.print(cu)).as(path.toString()).isEqualTo(sources.get(path)));
    }

    /**
     * A markup prints its message, and its detail only to a verbose marker printer.
     */
    @Test
    void markupDetail() {
        Cs.CompilationUnit cu = parse(sources("markup", "class C { }\n")).get(Paths.get("markup.cs"));
        cu = cu.withMarkers(cu.getMarkers().add(new Markup.Info(randomId(), "message", "detail")));

        assertThat(cu.printAll()).isEqualTo("/*~~(message)~~>*/class C { }\n");
        assertThat(CSharpRewriteRpc.getOrStart().print(cu)).isEqualTo("/*~~(message)~~>*/class C { }\n");
    }

    /**
     * A literal a recipe built from a value alone has no source to print, so it is written out
     * as the C# for that value.
     */
    @Test
    void literalsWithoutSource() {
        Map<J.Literal, String> literals = new LinkedHashMap<>();
        literals.put(literalWithoutSource(1.0, JavaType.Primitive.Double), " 1.0");
        literals.put(literalWithoutSource(-2.5, JavaType.Primitive.Double), " -2.5");
        literals.put(literalWithoutSource(1e20, JavaType.Primitive.Double), " 1.0E20");
        literals.put(literalWithoutSource(12345678.0, JavaType.Primitive.Double), " 1.2345678E7");
        literals.put(literalWithoutSource(0.00012, JavaType.Primitive.Double), " 1.2E-4");
        literals.put(literalWithoutSource(0.5, JavaType.Primitive.Double), " 0.5");
        literals.put(literalWithoutSource(42, JavaType.Primitive.Int), " 42");
        literals.put(literalWithoutSource(1.5f, JavaType.Primitive.Float), " 1.5f");
        literals.put(literalWithoutSource(2, JavaType.Primitive.Float), " 2.0f");
        literals.put(literalWithoutSource(42L, JavaType.Primitive.Long), " 42L");
        literals.put(literalWithoutSource(true, JavaType.Primitive.Boolean), " true");
        literals.put(literalWithoutSource("say \"hi\"\n", JavaType.Primitive.String), " \"say \\\"hi\\\"\\n\"");
        literals.put(literalWithoutSource('\'', JavaType.Primitive.Char), " '\\''");
        literals.put(literalWithoutSource(null, JavaType.Primitive.Null), " null");

        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        Cursor cursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), parse(sources("literals", "")).get(Paths.get("literals.cs")));
        literals.forEach((literal, source) -> {
            assertThat(literal.print(cursor)).isEqualTo(source);
            assertThat(rpc.print(literal, cursor)).as("the native print of %s", source).isEqualTo(source);
        });

        // there is no decimal type to tell the native side what this is once it has been sent as a number
        assertThat(literalWithoutSource(new BigDecimal("1.50"), JavaType.Primitive.Double).print(cursor)).isEqualTo(" 1.50m");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void subtrees() {
        List<String> failures = new ArrayList<>();
        Set<String> ancestorDependent = new TreeSet<>();
        parse(sources(SYNTAX, DIRECTIVES)).forEach((path, cu) -> compareSubtrees(path, cu, failures, ancestorDependent));
        assertThat(failures).isEmpty();
        assertThat(ancestorDependent).as("subtrees that were also compared without their ancestors").isNotEmpty();
    }

    /**
     * Asserts that each source parses, and that the Java and native printers both reproduce it.
     */
    static Map<Path, Cs.CompilationUnit> assertParity(Map<Path, String> sources) {
        Map<Path, Cs.CompilationUnit> parsed = parse(sources);
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();

        List<String> failures = new ArrayList<>();
        parsed.forEach((path, cu) -> {
            String java = cu.printAll();
            String nativePrint = rpc.print(cu);
            if (!java.equals(nativePrint)) {
                failures.add(path + " prints differently in Java than natively\n" + firstDifference(nativePrint, java));
            } else if (!java.equals(sources.get(path))) {
                failures.add(path + " does not print back to its source\n" + firstDifference(sources.get(path), java));
            }
        });

        assertThat(failures)
          .as("%d of %d sources", failures.size(), sources.size())
          .isEmpty();
        return parsed;
    }

    /**
     * Puts a search result, a markup, or both on every node of the tree, including the nodes of
     * documentation comments, and on every other comment.
     */
    static Cs.CompilationUnit mark(Cs.CompilationUnit cu) {
        return mark(cu, new LinkedHashMap<>());
    }

    /**
     * @param added Collects the markers put on the tree, each with the kind of tree it is on and in.
     */
    static Cs.CompilationUnit mark(Cs.CompilationUnit cu, Map<UUID, String> added) {
        AtomicInteger count = new AtomicInteger();
        BiFunction<Markers, String, Markers> mark = (markers, marked) -> {
            List<Marker> next = new ArrayList<>();
            switch (count.getAndIncrement() % 4) {
                case 0:
                    next.add(new SearchResult(randomId(), null));
                    break;
                case 1:
                    next.add(new SearchResult(randomId(), "found"));
                    break;
                case 2:
                    next.add(new Markup.Info(randomId(), "note", "the detail of the note"));
                    break;
                default:
                    next.add(new Markup.Warn(randomId(), "careful", null));
                    next.add(new SearchResult(randomId(), "both"));
            }
            for (Marker marker : next) {
                added.put(marker.getId(), marked);
                markers = markers.add(marker);
            }
            return markers;
        };
        return (Cs.CompilationUnit) new CSharpVisitor<Integer>() {
            @Override
            public J postVisit(J tree, Integer p) {
                return tree.withMarkers(mark.apply(tree.getMarkers(), tree.getClass().getSimpleName() + " in " +
                                                                    getCursor().getParentTreeCursor().getValue().getClass().getSimpleName()));
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                Space s = super.visitSpace(space, loc, p);
                return s.withComments(ListUtils.map(s.getComments(), comment -> comment instanceof TextComment ?
                  comment.withMarkers(mark.apply(comment.getMarkers(), "TextComment")) :
                  comment));
            }

            @Override
            protected CsDocCommentVisitor<Integer> getCsDocCommentVisitor() {
                return new CsDocCommentVisitor<>(this) {
                    @Override
                    public CsDocComment postVisit(CsDocComment tree, Integer p) {
                        return tree.withMarkers(mark.apply(tree.getMarkers(), tree.getClass().getSimpleName()));
                    }
                };
            }
        }.visitNonNull(cu, 0);
    }

    /**
     * Compares the two printers on a tree in every marker printing mode.
     */
    static void compareMarkerPrinters(Path path, Cs.CompilationUnit cu, List<String> failures) {
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        for (PrintOutputCapture.MarkerPrinter markerPrinter : MARKER_PRINTERS) {
            Print.MarkerPrinter mode = Print.MarkerPrinter.from(markerPrinter);
            String java = cu.printAll(new PrintOutputCapture<>(0, markerPrinter));
            String nativePrint = rpc.print(cu, mode);
            if (!java.equals(nativePrint)) {
                failures.add(path + " prints differently in Java than natively with the " + mode + " marker printer\n" +
                             firstDifference(nativePrint, java));
            }
        }
    }

    /**
     * Compares the two printers on every subtree, printed from its cursor the way a recipe
     * prints one. Where the ancestors change what is printed, the two are also compared on the
     * subtree with nothing but its source file above it.
     */
    static void compareSubtrees(Path path, Cs.CompilationUnit cu, List<String> failures, Set<String> ancestorDependent) {
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        Cursor sourceFileOnly = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), cu);
        new CSharpVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                if (tree == cu) {
                    return tree;
                }
                Cursor parent = getCursor().getParentOrThrow();
                String kind = tree.getClass().getSimpleName() + " in " +
                              getCursor().getParentTreeCursor().getValue().getClass().getSimpleName();

                String java = compare(tree, parent, kind);
                if (!tree.print(sourceFileOnly).equals(java)) {
                    ancestorDependent.add(kind);
                    compare(tree, sourceFileOnly, kind + " without its ancestors");
                }
                return tree;
            }

            private String compare(J tree, Cursor parent, String kind) {
                String java = tree.print(parent);
                try {
                    String nativePrint = rpc.print(tree, parent);
                    if (!java.equals(nativePrint)) {
                        failures.add(path + " " + kind + " prints differently in Java than natively\n" +
                                     firstDifference(nativePrint, java));
                    }
                } catch (RuntimeException e) {
                    failures.add(path + " " + kind + " could not be printed natively: " + e.getMessage());
                }
                return java;
            }
        }.visit(cu, 0);
    }

    /**
     * Parses the sources as one project, which every one of them has to survive.
     */
    static Map<Path, Cs.CompilationUnit> parse(Map<Path, String> sources) {
        List<Parser.Input> inputs = new ArrayList<>();
        sources.forEach((path, source) -> inputs.add(Parser.Input.fromString(path, source)));

        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new AssertionError(t);
        });
        // a source the native printer cannot reproduce should fail the comparison, not the parse
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);

        Map<Path, SourceFile> parsed = new HashMap<>();
        try (Stream<SourceFile> sourceFiles = CSharpParser.builder().build().parseInputs(inputs, Paths.get(""), ctx)) {
            sourceFiles.forEach(sourceFile -> parsed.put(sourceFile.getSourcePath(), sourceFile));
        }

        Map<Path, Cs.CompilationUnit> compilationUnits = new LinkedHashMap<>();
        for (Path path : sources.keySet()) {
            SourceFile sourceFile = parsed.get(path);
            assertThat(sourceFile)
              .as(() -> path + (sourceFile == null ? "" : sourceFile.getMarkers()
                .findFirst(ParseExceptionResult.class)
                .map(e -> ": " + e.getMessage())
                .orElse("")))
              .isInstanceOf(Cs.CompilationUnit.class);
            compilationUnits.put(path, (Cs.CompilationUnit) sourceFile);
        }
        return compilationUnits;
    }

    static String firstDifference(String expected, String actual) {
        int i = 0;
        while (i < expected.length() && i < actual.length() && expected.charAt(i) == actual.charAt(i)) {
            i++;
        }
        int from = Math.max(0, i - 60);
        return "  at offset " + i + "\n" +
               "  expected: " + excerpt(expected, from, i + 60) + "\n" +
               "  actual:   " + excerpt(actual, from, i + 60);
    }

    private static String excerpt(String s, int from, int to) {
        return s.substring(Math.min(from, s.length()), Math.min(to, s.length()))
          .replace("\r", "\\r")
          .replace("\n", "\\n");
    }

    static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param namesAndSources Alternating file names, without their extension, and sources.
     */
    private static Map<Path, String> sources(String... namesAndSources) {
        Map<Path, String> sources = new LinkedHashMap<>();
        for (int i = 0; i < namesAndSources.length; i += 2) {
            sources.put(Paths.get(namesAndSources[i] + ".cs"), namesAndSources[i + 1]);
        }
        return sources;
    }

    @SafeVarargs
    private static Map<Path, String> sources(Map<Path, String>... sources) {
        Map<Path, String> all = new LinkedHashMap<>();
        for (Map<Path, String> some : sources) {
            all.putAll(some);
        }
        return all;
    }

    private static final String STATEMENTS = """
      class Statements
      {
          void Control(int x, object o, int[] xs)
          {
              if (x > 0)
                  x--;
              else if (x < 0) { x++; }
              else
                  x = 0;

              while (x < 10) x++;

              do { x--; } while (x > 0);

              do
                  x++;
              while (x < 5);

              for (int i = 0, j = 10; i < j; i++, j--) { }
              for (;;) { break; }
              for (x = 0, x++; ; ) break;
              for ( ; x < 3 ; x++ ) continue;

              foreach (var item in xs) { }
              foreach (int item in xs)
                  x += item;
              foreach (var (a, b) in Pairs()) { }
              foreach ((int c, var d) in Pairs()) { }

              ;

              retry:
              x++;
              if (x < 100) goto retry;

              lock (this) { x++; }
              lock (this)
                  x++;

              checked { x++; }
              unchecked
              {
                  x--;
              }

              return;
          }
      }
      """;

    static final Map<Path, String> SYNTAX = sources(
      "usingDirectives", """
        extern alias Legacy;
        global using System.Linq;
        global using static System.Console;
        using System;
        using System.Collections.Generic;
        using static System.Math;
        using Con = System.Console;
        using Pair = (int Left, int Right);
        using Legacy::Old.Types;
        using Text  =  global::System.Text ;
        using unsafe Ptr = int*;
        using  unsafe  Buffer = byte*;

        class C
        {
        }
        """,

      "namespaces", """
        namespace Outer
        {
            extern alias Inner;
            using System;

            namespace Nested.Deeper
            {
                class C { }
            }

            class D
            {
            }
        }

        namespace Second { }
        """,

      "fileScopedNamespace", """
        using System;

        namespace App.Models;

        public class Widget
        {
        }
        """,

      "classes", """
        public abstract partial class Shape : Base, IDisposable, IComparable<Shape>
        {
            protected internal static readonly int Count = 0;
            private protected volatile bool _disposed;
            public const string Kind = "shape", Other = "other";
            internal new int Hidden;
            int a = 1, b, c = 3;

            static Shape()
            {
            }

            protected Shape(int sides) : base(sides)
            {
            }

            public Shape() : this(0) { }

            ~Shape()
            {
            }

            public abstract double Area();
            public virtual void Dispose() { }
            public sealed override string ToString() => "shape";
            extern static void Native();
            partial void OnChanged();
            public unsafe void Raw(int* p) { }

            class Nested<T> { }
        }

        file sealed class Hidden { }

        static class Extensions
        {
            public static int Twice(this int x) => x * 2;
        }

        class Terminated { };
        """,

      "structsAndRecords", """
        public struct Point
        {
            public int X;
            public int Y;
        }

        public readonly struct Money
        {
            public decimal Amount { get; }
        }

        public ref struct Buffer
        {
        }

        public record Person(string First, string Last);

        public record class Employee(string First, string Last, int Id) : Person(First, Last)
        {
            public string Full => $"{First} {Last}";
        }

        public record struct Pair(int A, int B);

        public readonly record struct Rgb(byte R, byte G, byte B)
        {
        };

        record Empty;

        record Plain
        {
        }
        """,

      "primaryConstructors", """
        public class Service(ILogger logger, int retries = 3) : Base(logger), IService
        {
            public void Run() => logger.Log(retries);
        }

        public class Empty();

        public struct Vec(double x, double y)
        {
            public double X { get; } = x;
        }
        """,

      "interfacesAndEnums", """
        public interface IRepository<in TKey, out TValue> : IDisposable where TKey : notnull
        {
            TValue Get(TKey key);
            int Count { get; }
            event EventHandler Changed;
            TValue this[TKey key] { get; }
            static abstract TValue Create();
            void Log() { }
        }

        [Flags]
        public enum Color : byte
        {
            None = 0,
            [Description("red")]
            Red = 1 << 0,
            Green = 1 << 1,
            Blue = Red | Green,
        }

        enum Empty { }

        enum Single { One };
        """,

      "genericsAndConstraints", """
        class Repo<T, U, V>
            where V : struct
            where T : class?, IComparable<T>, new()
            where U : unmanaged
        {
            public TResult Map<TResult>(Func<T, TResult> f) where TResult : notnull => default;

            public void M<[Description("x")] A, B>() where B : default { }

            public T Create<T2>() where T2 : allows ref struct => default!;
        }

        class Node<T> where T : Node<T> { }

        class Twice<T> where T : class where T : new() { }

        class Undeclared where T : class { }
        """,

      "delegatesAndEvents", """
        public delegate void Handler(object sender, EventArgs e);

        [Obsolete]
        internal delegate TResult Converter<in T, out TResult>(T input) where T : class where TResult : new();

        class Publisher : INotify
        {
            public event Handler Raised;
            public static event EventHandler<EventArgs> Changed, Renamed;

            public event Handler Custom
            {
                add { Raised += value; }
                remove { Raised -= value; }
            }

            event Handler INotify.Notified
            {
                add => Raised += value;
                remove => Raised -= value;
            }
        }
        """,

      "properties", """
        abstract class Account : IAccount
        {
            private decimal _balance;

            public string Name { get; set; }
            public int Id { get; init; }
            public required string Owner { get; set; }
            public decimal Balance { get; private set; } = 0m;
            public bool IsEmpty => _balance == 0;
            public static int Count { get; } = 10;

            public decimal Checked
            {
                get { return _balance; }
                set { _balance = value; }
            }

            public decimal Arrow
            {
                get => _balance;
                [Obsolete] internal set => _balance = value;
            }

            string IAccount.Label { get ; set ; }

            public abstract int Abstract { get; }
        }
        """,

      "indexersAndOperators", """
        class Matrix : IMatrix
        {
            private readonly double[,] _cells = new double[3, 3];

            public double this[int row, int column]
            {
                get { return _cells[row, column]; }
                set { _cells[row, column] = value; }
            }

            public double this[int index] => _cells[index, 0];

            double IMatrix.this[string key] => 0;

            public static Matrix operator +(Matrix a, Matrix b) => a;
            public static Matrix operator -(Matrix a, Matrix b) { return a; }
            public static Matrix operator !(Matrix a) => a;
            public static Matrix operator ~(Matrix a) => a;
            public static Matrix operator ++(Matrix a) => a;
            public static Matrix operator --(Matrix a) => a;
            public static Matrix operator *(Matrix a, Matrix b) => a;
            public static Matrix operator /(Matrix a, Matrix b) => a;
            public static Matrix operator %(Matrix a, Matrix b) => a;
            public static Matrix operator <<(Matrix a, int shift) => a;
            public static Matrix operator >>(Matrix a, int shift) => a;
            public static Matrix operator >>>(Matrix a, int shift) => a;
            public static bool operator <(Matrix a, Matrix b) => true;
            public static bool operator >(Matrix a, Matrix b) => true;
            public static bool operator <=(Matrix a, Matrix b) => true;
            public static bool operator >=(Matrix a, Matrix b) => true;
            public static bool operator ==(Matrix a, Matrix b) => true;
            public static bool operator !=(Matrix a, Matrix b) => false;
            public static Matrix operator &(Matrix a, Matrix b) => a;
            public static Matrix operator |(Matrix a, Matrix b) => a;
            public static Matrix operator ^(Matrix a, Matrix b) => a;
            public static bool operator true(Matrix a) => true;
            public static bool operator false(Matrix a) => false;
            public static Matrix operator checked +(Matrix a, Matrix b) => a;
            static Matrix IMatrix.operator -(Matrix a) => a;

            [Obsolete]
            public static implicit operator double(Matrix m) => 0;
            public static explicit operator Matrix(double d)
            {
                return new Matrix();
            }
        }
        """,

      "methods", """
        abstract class Methods : IMethods
        {
            public void Empty( ) { }
            public int Add(int a, int b = 2) { return a + b; }
            public void Refs(ref int a, out int b, in int c, params int[] rest) { b = 0; }
            public static T Identity<T>(T value) => value;
            public async Task<int> RunAsync(CancellationToken token = default) { await Task.Yield(); return 1; }
            public abstract void Abstract();
            void IMethods.Explicit(int x) { }
            int IMethods.Generic<T>(T x) => 0;
            public void Attributes([NotNull] string a, [In, Out] ref int b) { }
            public (int, string) Tuple() => (1, "a");
            public ref readonly int RefReturn(ref int x) => ref x;
            public void Scoped(scoped ref int x) { }

            void Locals()
            {
                int Local(int x) => x * 2;
                static void Static() { }
                T Generic<T>(T t) where T : class { return t; }
                ref int y = ref Find();
                const int Limit = 10;
                Local(1);
            }
        }
        """,

      "statements", STATEMENTS,

      "switchStatements", """
        class Switches
        {
            string Describe(object o, int x)
            {
                switch (x)
                {
                    case 1:
                    case 2:
                        return "low";
                    case 3 when o != null:
                        goto case 1;
                    case > 10 and < 20:
                        goto default;
                    default:
                        break;
                }

                switch (o)
                {
                    case int i:
                        return "int";
                    case string { Length: > 0 } s when s.StartsWith("a"):
                        return s;
                    case (int a, int b):
                        return "tuple";
                    case null:
                        return "null";
                    case var other:
                        return other.ToString();
                }

                switch (x) { }

                switch (x, o)
                {
                    case (1, null):
                        break;
                }
                return "";
            }
        }
        """,

      "exceptions", """
        class Exceptions
        {
            void Run()
            {
                try
                {
                    Do();
                }
                catch (ArgumentException e) when (e.Message != null)
                {
                    throw;
                }
                catch (InvalidOperationException)
                {
                    throw new Exception("wrapped");
                }
                catch when (Filter())
                {
                }
                catch
                {
                }
                finally
                {
                    Cleanup();
                }

                try { } finally { }

                var s = Get() ?? throw new ArgumentNullException(nameof(s));
            }
        }
        """,

      "usingAndUnsafe", """
        unsafe class Resources
        {
            void Use(int[] data)
            {
                using (var a = Open())
                using (Stream b = Open(), c = Open())
                {
                }

                using (Open()) { }
                using (Open())
                    Use(data);

                using var d = Open();

                fixed (int* p = data, q = &data[1])
                {
                    *p = 1;
                    p[0] = *q;
                    int** pp = &p;
                    var size = sizeof(int) + sizeof(Point);
                }

                fixed (int* r = data)
                    *r = 2;

                unsafe
                {
                    Point* pt = stackalloc Point[1];
                    pt->X = 1;
                    pt->Offset(1);
                    (*pt).Y = 2;
                    Span<int> span = stackalloc int[] { 1, 2, 3 };
                    var raw = stackalloc[] { 1, 2 };
                    delegate*<int, void> fp = &Callback;
                    delegate* unmanaged[Cdecl, SuppressGCTransition]<int, int> native = null;
                    delegate* managed<void> managed = null;
                }
            }
        }
        """,

      "iterators", """
        class Iterators
        {
            IEnumerable<int> Numbers(bool stop)
            {
                yield return 1;
                if (stop)
                {
                    yield break;
                }
                yield return 2;
            }

            async IAsyncEnumerable<int> Stream()
            {
                await foreach (var x in Source()) { yield return x; }
                await using var d = Open();
                await Task.Delay(1);
            }
        }
        """,

      "literals", """
        class Literals
        {
            object[] values =
            {
                1, 0x1F, 0b1010_1010, 1_000_000L, 1u, 2UL, 1.5, 2.5f, 3.5m, 1e10, 'a', '\\n', '\\u0041',
                "text", "esc\\"aped\\\\", @"verbatim ""quoted""
        second line", true, false, null, default,
            };

            string raw = \"""
                raw "string"
                  indented
                \""";

            string single = \"""one line\""";
            byte[] utf8 = "bytes"u8.ToArray();
        }
        """,

      "interpolatedStrings", """
        class Interpolated
        {
            string M(string name, int n, double d)
            {
                var a = $"Hello {name}!";
                var b = $"{n,5}|{n,-5}|{d:F2}|{d,10:N1}";
                var c = $@"C:\\{name}\\file.txt";
                var e = @$"{{literal}} {name}";
                var f = $"{(n > 0 ? "pos" : "neg")} { name } {n:D3}";
                var g = $\"""
                    raw {name}
                    \""";
                var h = $$\"""
                    {{name}} and {literal}
                    \""";
                var i = $"nested {$"{n}"}";
                var j = $"{n ,5}|{d :F2}|{d , 10 :N1}|{n  ,  -5}";
                return a + b + c + e + f + g + h + i + j;
            }
        }
        """,

      "operators", """
        class Operators
        {
            void M(int a, int b, bool p, bool q, int? n, object o, string s, int[] xs)
            {
                var r = a + b - a * b / a % b;
                r = a << 2 >> 1 >>> 1;
                r = a & b | a ^ b;
                r = ~a + -b + +a;
                p = a < b || a > b && a <= b || a >= b || a == b || a != b;
                p = !q;
                a++; a--; ++a; --a;
                a += 1; a -= 1; a *= 2; a /= 2; a %= 2; a &= 1; a |= 1; a ^= 1; a <<= 1; a >>= 1; a >>>= 1;
                n ??= 0;
                r = n ?? a;
                r = p ? a : b;
                r = p ? a : q ? b : 0;
                s = o as string;
                p = o is string;
                p = o is not null;
                s = s!;
                r = s!.Length;
                r = (int)3.5 + (a);
                r = checked(a + b) + unchecked(a * b);
                var t = typeof(Dictionary<,>);
                var u = typeof(int);
                var size = sizeof(long);
                var name = nameof(M);
                var dflt = default(int);
                int dflt2 = default;
                var last = xs[^1];
                var slice = xs[1..^1];
                var head = xs[..2];
                var tail = xs[2..];
                var all = xs[..];
            }
        }
        """,

      "memberAccess", """
        class Access
        {
            void M(Customer c, int[,] grid, int[][] jagged, Dictionary<string, int> map)
            {
                var a = c.Address.City.Length;
                var b = c?.Address?.City;
                var d = c?.Orders?[0]?.Total;
                var e = c?.GetOrders()?.Count ?? 0;
                var f = grid[1, 2] + jagged[0][1] + cube[1, 2, 3];
                var g = map["key"];
                var h = c ?. Address ?[ 0 ];
                c?.Notify();
                c.Changed?.Invoke(c, EventArgs.Empty);
                var i = global::System.String.Empty;
                var j = System.Math.Max(1, 2);
                handler(1);
                this.field = base.ToString();
                var k = c!.Address!.City;
                var l = c.Callback(1)(2);
                var m = grid[ 1 , 2 ];
                var n = Generic<int>.Value;
                var o = c.Method<int, string>(1);
                var p = grid ?[ 1 , 2 ];
                Action q = c.Notify;
                var s = c.Method<string>;
                var r = c?
                    .Address?
                    .City;
            }
        }
        """,

      "invocations", """
        class Calls
        {
            void M()
            {
                Print(1, name: "a", flag: true);
                Parse("1", out var result);
                Parse("2", out int typed);
                Parse("3", out _);
                Swap(ref a, ref b);
                Read(in a);
                Generic<int, string>(1, "a");
                list.Select(x => x).Where(x => x > 0)
                    .ToList();
                Empty( );
                var del = new Func<int, int>(Twice);
                var v = del(2) + del.Invoke(3);
            }
        }
        """,

      "objectCreation", """
        class Creation
        {
            void M(Foo p)
            {
                var a = new Foo();
                var b = new Foo(1, "two") { Name = "n", Age = 3, };
                var c = new Foo { Name = "n" };
                Foo d = new();
                Foo e = new(1) { Name = "x" };
                var f = new List<int> { 1, 2, 3 };
                var g = new Dictionary<string, int> { ["a"] = 1, ["b"] = 2 };
                var h = new Dictionary<string, int> { { "a", 1 }, { "b", 2 } };
                var i = new { Name = "anon", Age = 1 };
                var j = new { p.Name, Age = 2, };
                var k = new int[3];
                var l = new int[] { 1, 2, 3 };
                var m = new[] { 1, 2, 3 };
                var n = new int[2, 3];
                var o = new int[2][];
                var q = new int[,] { { 1, 2 }, { 3, 4 } };
                var r = new int[2, 3, 4];
                int[] s = { 1, 2, };
                int[] t = { };
                var u = new Foo { };
                var v = new Outer.Inner<int>();
                var w = new Foo { Child = { Name = "c" }, Items = { 1, 2 } };
                var x = p with { Name = "copy" };
                var y = p with { };
            }
        }
        """,

      "collectionsAndTuples", """
        class Collections
        {
            (int Id, string Name) field;

            void M(int[] xs)
            {
                int[] a = [1, 2, 3];
                int[] b = [];
                int[] c = [..xs, 4, ..xs];
                List<int> d = [ 1, 2, ];
                var t = (1, "a");
                var named = (id: 1, name: "a");
                (int x, int y) = (1, 2);
                var (p, q) = named;
                var (first, _) = named;
                (var r, _) = t;
                (x, y) = (y, x);
                (int, (string, bool)) nested = (1, ("a", true));
                Func<(int, int), (int a, int b)> f = z => z;
            }
        }
        """,

      "patterns", """
        class Patterns
        {
            bool M(object o, int[] xs, Point p)
            {
                if (o is int i) return true;
                if (o is string { Length: > 3 and < 10 } s) return true;
                if (o is not (int or long)) return false;
                if (p is { X: 0, Y: var y, }) return y > 0;
                if (p is (0, 0)) return true;
                if (p is Point(var a, var b) { X: > 0 } named) return true;
                if (o is { }) return true;
                if (o is { } notNull) return true;
                if (xs is [1, 2, ..]) return true;
                if (xs is [var first, .. var rest]) return true;
                if (xs is [_, _, ..] list) return true;
                if (xs is []) return false;
                if (o is var anything) return true;
                if (o is null or "") return false;
                if (p is { Inner.Value: 1 }) return true;
                if (o is >= 0 and <= 9) return true;
                if (o is (> 0) or (< -10)) return true;
                return o is Type t && t.IsClass;
            }
        }
        """,

      "switchExpressions", """
        class SwitchExpressions
        {
            string M(object o, int x, Point p) => o switch
            {
                int i when i > 0 => "positive",
                int => "int",
                string { Length: 0 } => "empty",
                (int a, int b) => "pair",
                null => "null",
                _ => "other",
            };

            int N(int x) => x switch { 1 => 10, 2 => 20, _ => 0 };

            int Nested(int x, int y) => x switch
            {
                > 0 => y switch { 0 => 1, _ => 2 },
                _ => 0
            };

            Func<int> StatementInArm(int x, int y) => x switch
            {
                > 0 => () =>
                {
                    switch (y)
                    {
                        case 1: return 1;
                        case > 1 when x > y: return y switch { 2 => 2, _ => 3 };
                        default: return 0;
                    }
                },
                _ => () => 0,
            };

            int ExpressionInSection(int x, int y)
            {
                switch (x)
                {
                    case 1:
                        return y switch { 0 => 1, _ => 2 };
                    default:
                        return 0;
                }
            }
        }
        """,

      "lambdas", """
        class Lambdas
        {
            void M()
            {
                Func<int, int> a = x => x + 1;
                Func<int, int, int> b = (x, y) => x + y;
                Func<int, int> c = (int x) => { return x; };
                Action d = () => { };
                Func<Task> e = async () => await Task.Delay(1);
                Func<int, int> f = static x => x;
                var g = [Obsolete] int (int x) => x;
                var h = ( x , y ) => x;
                Func<int, int, int> i = (_, _) => 0;
                Action<int> j = delegate (int x) { };
                Action k = delegate { };
                Func<int, int> l = async delegate (int x) { return x; };
                var m = (ref int x, out int y) => y = x;
                var n = (int x = 1, params int[] rest) => x;
                var o = object (bool b) => b ? 1 : "one";
            }
        }
        """,

      "linq", """
        class Queries
        {
            void M(List<Customer> customers, List<Order> orders)
            {
                var a = from c in customers
                        where c.Age > 18
                        orderby c.Name ascending, c.Age descending, c.Id
                        select c.Name;

                var b = from c in customers
                        join o in orders on c.Id equals o.CustomerId
                        join o2 in orders on c.Id equals o2.CustomerId into grouped
                        let total = grouped.Sum(g => g.Total)
                        from Order typed in orders
                        group c by c.City into byCity
                        select new { City = byCity.Key, Count = byCity.Count() };

                var d = from c in customers
                        group c by c.City;

                var e = from c in customers select c into d2 where d2.Age > 1 select d2;

                var f = from c in customers orderby c.Id, -c.Age select c;
            }
        }
        """,

      "attributes", """
        [assembly: AssemblyVersion("1.0")]
        [module: Marker]

        [Serializable, Obsolete("old", error: false)]
        [type: Description("d")]
        class Attributed
        {
            [field: NonSerialized]
            public int Value { get; set; }

            [return: NotNull]
            [method: Obsolete]
            public string M([param: NotNull] string s, [CallerMemberName] string caller = "") => s;

            [Conditional("DEBUG"), DebuggerStepThrough]
            void N() { }

            [Obsolete()]
            int field;

            [ Custom ( 1 , Name = "n" ) ]
            void Spaced() { }
        }
        """,

      "types", """
        unsafe class Types
        {
            int? a;
            int[] b;
            int[,] c;
            int[][] d;
            int[,,][] e;
            List<Dictionary<string, int?>> f;
            (int, string)[] g;
            int?[] h;
            int* i;
            void** j;
            dynamic k;
            nint l; nuint m;
            object n; string o; decimal p; uint q; ulong r; ushort s; sbyte t; byte u; short v; long w; float x; double y; char z; bool aa;
            System.Collections.Generic.List<int> ab;
            global::System.Int32 ac;
            Outer<int>.Inner<string> ad;
            string? ae;
            ref int Field => ref af;
            Action<int, string> ag;
            Nullable<int> ah;
        }
        """,

      "commentsAndTrivia", """
        // leading line comment
        /* leading block comment */
        using System; // trailing

        /// <summary>
        /// Doc comment with <see cref="Trivia{T}"/> and <paramref name="x"/>.
        /// </summary>
        /// <typeparam name="T">type</typeparam>
        class Trivia<T> /* after name */ : /* before base */ Base // end of line
        {
            /** block doc */
            int /* a */ field /* b */ = /* c */ 1 /* d */ ; // e

            /// <param name="x">the x</param>
            /// <returns>nothing <c>useful</c></returns>
            void M( /* empty */ ) { /* inside */ }

            void N(int x /* after param */, /* before param */ int y)
            {
                // only a comment
            }
            // before closing brace
        }
        // trailing comment at end of file
        """,

      "crlf", STATEMENTS.replace("\n", "\r\n"),

      "tabs", STATEMENTS.replace("    ", "\t"),

      "noTrailingNewline", "class C { }",

      "byteOrderMark", "\uFEFFclass C { }\n",

      "empty", "",

      "directives", """
        #define FEATURE
        #undef LEGACY
        #nullable enable
        #pragma warning disable CS0168, CS0219
        #pragma warning restore CS0168
        #pragma  warning  disable
        #pragma checksum "file.cs" "{406EA660-64CF-4C82-B6F0-42D48172A799}" "ab007f1d23d9"
        using System;

        #region Types
        class Directives
        {
            #region Members
            void M()
            {
        #nullable disable warnings
        #line 200 "Special.cs"
                int x;
        #line hidden
                x = 1;
        #line default
        #nullable restore
        #warning Fix this later
            }
            #endregion Members

        #if FEATURE
            void Feature() { }
        #else
            void Fallback() { }
        #endif
        }
        #endregion
        """,

      "errorDirective", """
        class Broken
        {
        #error Not supported
        }
        """
    );

    static final Map<Path, String> DIRECTIVES = sources(
      "conditionalUsing", """
        using System;
        #if DEBUG
        using System.Diagnostics;
        #endif

        class Conditional
        {
        #if DEBUG
            void Debug() { }
        #else
            void Release() { }
        #endif
        }
        """,

      "multipleBranches", """
        class Platform
        {
            string Name()
            {
        #if NET8_0_OR_GREATER
                return "modern";
        #elif NETSTANDARD2_0
                return "standard";
        #elif NETFRAMEWORK
                return "framework";
        #else
                return "unknown";
        #endif
            }
        }
        """,

      "nested", """
        #if A
        #if B
        class AB { }
        #else
        class ANotB { }
        #endif
        #elif C
        class C1
        {
        #if D
            int d;
        #endif
        }
        #else
        class None { }
        #endif
        """,

      "withinExpressions", """
        class Mixed
        {
            int Value(int x)
            {
                return x
        #if DOUBLE
                    * 2
        #else
                    * 1
        #endif
                    ;
            }

            void Args()
            {
                Call(1,
        #if EXTRA
                    2,
        #endif
                    3);
            }
        }
        """,

      "withOtherDirectives", """
        #define LOCAL
        #nullable enable
        using System;

        #region Platform
        #if LOCAL && !REMOTE
        #pragma warning disable CS0168
        class Local
        {
            #region Members
            int x; // a comment
            #endregion
        }
        #pragma warning restore CS0168
        #elif REMOTE || (CLOUD && !LOCAL)
        class Remote { }
        #endif
        #endregion
        """,

      "indented", """
        class Indented
        {
            void M()
            {
                #if TRACE
                Trace();
                #endif
                  #if CHECKED // why
                Check();
                  #else
                Skip();
                  #endif // CHECKED
            }
        }
        """
    );
}
