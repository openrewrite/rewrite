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
package org.openrewrite.python;

import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.marker.KeywordArguments;
import org.openrewrite.python.marker.KeywordOnlyArguments;
import org.openrewrite.python.marker.SuppressNewline;
import org.openrewrite.python.rpc.ParseOptions;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.python.tree.PyComment;
import org.openrewrite.rpc.request.Print;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.Tree.randomId;

/**
 * {@link PythonPrinter} is a port of the printer in {@code rewrite/python/printer.py}. These tests
 * parse through the Python process and hold the two printers to the same output, so that a change
 * to either one that is not mirrored in the other fails here.
 */
@Python3Only
class PythonPrinterParityTest {

    /**
     * Syntax every supported interpreter parses. Between them, these have to reach every tree type
     * the printer renders; {@link #everyPrintedTreeType()} fails when one is left out.
     */
    private static final List<String> SYNTAX = List.of(
      """
        #!/usr/bin/env python3
        \"""Module docstring.\"""
        from __future__ import annotations

        import os, sys as system
        import os.path as osp
        import a.b.c
        from . import sibling
        from .. import parent as p
        from ...pkg.mod import (a, b as c,)
        from typing import *
        from os import path , environ

        x = 1; y = 2
        z = 3;
        pass
        del x, y
        del (z)
        global g
        assert x, "message"
        assert x
        """,
      """
        a = b = c = 3
        a: int = 1
        a.b: int = 2
        a : "str"
        a: 'str' = "s"
        a: None = None
        a: r"int" = 1
        a += 1; a -= 1; a *= 2; a /= 2; a %= 2
        a &= 1; a |= 1; a ^= 1; a <<= 1; a >>= 1
        a **= 2; a //= 2; a @= m
        x = a if b else c
        x = not a
        x = -a + +b - ~c
        x = a < b <= c > d >= e == f != g
        x = a and b or c
        x = a & b | c ^ d << 1 >> 2
        x = a + b - c * d / e % f // g ** h @ i
        x = a in b, a not in b, a is b, a is not b, a not  in b, a is  not b
        x = "a" "b" 'c'
        x = (a := 1)
        print(n := 10)
        if (m := len(a)) > 1: pass
        x = lambda: 0
        x = lambda a, b=1, *args, c, **kwargs: (a, b)
        x = lambda *, a: a
        x = a.b.c(d, e=1, *f, **g)[0][1:2][::-1][a:b:c][:, 1][...]
        x = a[1:], a[:1], a[:], a[::], a[1:2:], a[::2], a[ 1 : 2 : 3 ]
        x = ...
        x = None, True, False, 1, 1.5, 1j, 0x1F, 0o7, 0b1, 1_000, b"bytes", r"raw", u"uni"
        x = (1), (1,), (), (1, 2,), 1, 2
        x = [], [1], [1, 2,], [*a, *b]
        x = {}, {1}, {1, 2,}, {"a": 1, **b, "c": 3,}, {*a}
        x = [i for i in a], {i for i in a}, {k: v for k, v in a}, (i for i in a)
        x = [i for i in a if i if not i for j in i]
        x = sum(i for i in a)
        x = f(a)(b)
        x = f ( a , b = 1 , )
        """,
      """
        a = f"{x}"
        a = f"{x!r} {y!s} {z!a} {w=} {v = } {u:>10} {t!r:^{width}.{precision}} {{literal}}"
        a = f"{x!s  }" f"{y!r  :10.10}" f"{z = !a }"
        a = f"{x = # why
          }" f"{y!r # which
          }"
        a = f'''multi
        {line}'''
        a = rf"\\d{x}" F"{x}" fr'{x}'
        a = f"{x}" "plain" f"{y}"
        a = f"{'nested' + f'{deep}'}"
        a = f"{x:%Y-%m-%d}"
        a = f"{d['key']}" f"{obj.attr.method()}"
        a = f"{ x }" f"{x,}" f"{(lambda: 1)()}"
        a = "\\ud83d" "\uD83D\uDE00\\udc00" '\\N{BULLET}'
        a = \"""doc
        string\"""
        a = b'\\x00' rb'\\d'
        """,
      """
        @decorator
        @decorator.attr(1, key="v")
        def f(a, /, b: int, c=1, *args: str, d, e: int = 2, **kwargs) -> None:
            \"""Docstring.\"""
            global g
            def inner():
                nonlocal a
                return a
            return

        async def g(*, a):
            await a
            async for i in a:
                pass
            else:
                pass
            async with a as b, c:
                pass
            x = [i async for i in a if await i]
            yield a

        def gen():
            x = yield
            y = yield 1, 2
            yield from range(3)
            return x, y

        class A: pass
        class B(): pass
        class C(A, B, metaclass=M):
            x: int = 1
            def m(self): ...
            @staticmethod
            def s(): return 1
            @property
            def p(self) -> "C": return self
        class D(A,): x = 1; y = 2

        def f(a, *, b): pass
        def f(*args, **kwargs): pass
        def f(a = 1, /): pass
        def f(
            a,  # comment
            b,
        ): pass
        """,
      """
        if a:
            pass
        elif b:
            pass
        else:
            pass
        if a: pass
        else: pass
        if a: x = 1
        while a:
            break
        else:
            pass
        for i, j in a:
            continue
        else:
            pass
        for i in a: x = i
        for (i, j) in a: pass
        for x in *a, *b: pass
        try:
            pass
        except A:
            pass
        except (B, C) as e:
            raise
        except:
            raise E from e
        else:
            pass
        finally:
            pass
        try:
            pass
        finally:
            pass
        try:
            pass
        except* A as e:
            pass
        except *B:
            pass
        except  *  (C, D) as e:
            pass
        with a: pass
        with a as b, c as d: pass
        with (a as b, c as d,): pass
        with (a): pass
        with a, b: pass
        with open(f) as (x, y): pass
        raise E
        raise E("x") from None
        """,
      """
        match command:
            case 1 | 2:
                pass
            case "a" | b"b" | None | True:
                pass
            case -1 | 1.5 | 1 + 2j:
                pass
            case _:
                pass
        match point:
            case Point(x=0, y=0) if x > 0:
                pass
            case Point(1, y=yy) | Other():
                pass
            case [a, b, *rest]:
                pass
            case (a, b) | (a,):
                pass
            case a, *_:
                pass
            case {"k": v, **rest}:
                pass
            case {} | { } | [] | [ ] | () | ( ):
                pass
            case {"k": {}} if point:
                pass
            case {0: (0 | 1 | 2 as z)}:
                pass
            case {0: ([1, {}] | False)} | {1: [[]]}:
                pass
            case ((a as b, c as d) as e) as w, ((f as g, h) as i) as z:
                pass
            case [[a]], [b], (c),:
                pass
            case A.B.C:
                pass
            case (x) as y:
                pass
            case [1, 2] as pair:
                pass
            case str() | int(n):
                pass
            case x:
                pass
        match a, b:
            case _: pass
        """,
      """
        type X = int
        type Y[T] = list[T]
        type Z[T: int, *Ts, **P] = Callable[P, tuple[T, *Ts]]
        def f[T](a: T) -> T: return a
        def g[T: (int, str), *Ts, **P](*args: *Ts) -> None: pass
        class C[T]: pass
        class D[T: int](Base[T]): pass
        x: int | None = None
        y: list[int] | dict[str, int | None]
        def h(a: "int", b: list[int] = [], *c: int, **d: str) -> dict[str, int]: ...
        z: Literal[1, "a", -1] = 1
        w: Callable[[int], str]
        v: tuple[int, ...]
        """,
      """
        # leading comment
        x = 1  # trailing
        def f(  # after paren
            a,  # after a
            # own line
            b,
        ):  # after colon
            # body comment
            return a  # ret
        x = [
            1,  # one
            2,
        ]
        y = (1 +  # plus
             2)
        z = a \\
            + b
        if a:
        \tpass
        x = 1 ; y = 2 ;
        # trailing comment""",
      """
        @False or a
        def f(): pass
        @d := a
        def g(): pass
        @lambda f: a(f)
        def h(): pass
        @[..., a, ...][1]
        def i(): pass
        @a(a)(a)
        def j(): pass
        @a if b else c
        class C(List[T][U][V]): ...
        x: f()[int] = 1
        y: (A)[int]
        """,
      "x = 1",
      // the space ahead of each colon is padding the parameter looks up in its parent
      "def spaced(a : int, b  :str = '', *c : int, **d :  int) -> int : pass\n",
      "x = 1\r\ny = 2\r\nif x:\r\n    pass\r\n",
      "\n\n# only a comment\n\n",
      ""
    );

    /**
     * Syntax newer than the oldest supported interpreter. Each source is compared wherever the
     * interpreter running the tests accepts its probe, a minimal use of the same feature.
     */
    private record NewerSyntax(String probe, String source) {
    }

    private static final List<NewerSyntax> NEWER_SYNTAX = List.of(
      new NewerSyntax("def f[T = int](): pass\n",
        """
          def f[T = int](): pass
          class C[T: int = str, *Ts = *tuple[int], **P = [int]]: pass
          type A[T = int] = list[T]
          """),
      new NewerSyntax("a = t\"\"\n",
        """
          a = t"hello {name}"
          a = t'{x!r:>10} {y=}'
          a = rt"\\d{x}" t"{x}"
          a = t\"""multi
          {line}\"""
          """)
    );

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void nativeModuleSources() throws IOException {
        Path root = Paths.get("rewrite").toAbsolutePath();
        List<Path> files;
        try (Stream<Path> src = Files.walk(root.resolve("src")); Stream<Path> tests = Files.walk(root.resolve("tests"))) {
            files = Stream.concat(src, tests).filter(p -> p.toString().endsWith(".py")).sorted().collect(toList());
        }

        // a process of its own, as the shared one traces every message and this many trees fill the log
        PythonRewriteRpc rpc = PythonRewriteRpc.builder().get();
        List<Path> compared = new ArrayList<>();
        try {
            SoftAssertions.assertSoftly(softly -> {
                // in batches, so that no single parse request outlasts the RPC timeout
                for (int i = 0; i < files.size(); i += 25) {
                    List<Path> batch = files.subList(i, Math.min(files.size(), i + 25));
                    List<SourceFile> parsed = rpc.parse(batch, ParseOptions.builder().relativeTo(root).build(),
                      new InMemoryExecutionContext()).collect(toList());
                    for (int j = 0; j < parsed.size(); j++) {
                        // what the interpreter cannot parse there is nothing to compare for
                        if (parsed.get(j) instanceof Py.CompilationUnit cu) {
                            compared.add(batch.get(j));
                            String printed = cu.printAll();
                            softly.assertThat(printed).as("%s against its source", cu.getSourcePath()).isEqualTo(read(batch.get(j)));
                            softly.assertThat(printed).as("%s against the Python printer", cu.getSourcePath()).isEqualTo(rpc.print(cu));
                        }
                    }
                }
            });
        } finally {
            rpc.shutdown();
        }
        assertThat(compared).as("files the Python parser accepted").hasSizeGreaterThan(files.size() * 9 / 10);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void everyPrintedTreeType() {
        Set<Class<?>> exercised = new HashSet<>();
        SoftAssertions.assertSoftly(softly -> {
            List<SourceFile> parsed = parse(SYNTAX);
            for (int i = 0; i < parsed.size(); i++) {
                assertThat(parsed.get(i)).as(SYNTAX.get(i)).isInstanceOf(Py.CompilationUnit.class);
                assertParity(softly, (Py.CompilationUnit) parsed.get(i), SYNTAX.get(i));
                exercised.addAll(treeTypes(parsed.get(i)));
            }
            for (NewerSyntax newer : NEWER_SYNTAX) {
                List<SourceFile> probed = parse(List.of(newer.probe(), newer.source()));
                if (probed.get(0) instanceof Py.CompilationUnit) {
                    assertThat(probed.get(1)).as(newer.source()).isInstanceOf(Py.CompilationUnit.class);
                    assertParity(softly, (Py.CompilationUnit) probed.get(1), newer.source());
                }
            }
            for (Py.CompilationUnit cu : treesTheParserDoesNotProduce(softly)) {
                softly.assertThat(cu.printAll()).isEqualTo(rpc().print(cu));
                exercised.addAll(treeTypes(cu));
            }
        });
        assertThat(exercised).as("tree types with a parity case").containsAll(printedTreeTypes());
    }

    @Test
    void byteOrderMark(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bom.py");
        Files.writeString(file, "\uFEFF# comment\nx = 1\n");
        SourceFile cu = PythonParser.builder().build()
          .parse(List.of(file), dir, new InMemoryExecutionContext()).findFirst().orElseThrow();
        assertThat(cu).isInstanceOf(Py.CompilationUnit.class);
        SoftAssertions.assertSoftly(softly -> assertParity(softly, (Py.CompilationUnit) cu, read(file)));
    }

    @Test
    void declaredEncoding(@TempDir Path dir) throws IOException {
        String source = "# -*- coding: latin-1 -*-\nname = '\u00e9t\u00e9'  # \u00e9t\u00e9\nvalue = 1\n";
        Path file = dir.resolve("latin1.py");
        Files.write(file, source.getBytes(StandardCharsets.ISO_8859_1));
        SourceFile cu = PythonParser.builder().build()
          .parse(List.of(file), dir, new InMemoryExecutionContext()).findFirst().orElseThrow();
        assertThat(cu).isInstanceOf(Py.CompilationUnit.class);
        assertThat(cu.getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
        assertThat(cu.printAllAsBytes()).isEqualTo(Files.readAllBytes(file));
        SoftAssertions.assertSoftly(softly -> assertParity(softly, (Py.CompilationUnit) cu, source));
    }

    @ParameterizedTest
    @MethodSource("markerPrinters")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void markers(String name, PrintOutputCapture.MarkerPrinter markerPrinter) {
        SoftAssertions.assertSoftly(softly -> {
            for (SourceFile parsed : parse(SYNTAX)) {
                Py.CompilationUnit cu = markEveryTree((Py.CompilationUnit) parsed);
                softly.assertThat(cu.printAll(new PrintOutputCapture<>(0, markerPrinter)))
                  .as("%s markers in %s", name, parsed.printAll())
                  .isEqualTo(rpc().print(cu, Print.MarkerPrinter.from(markerPrinter)));
            }
        });
    }

    static Stream<Arguments> markerPrinters() {
        return Stream.of(
          Arguments.of("DEFAULT", PrintOutputCapture.MarkerPrinter.DEFAULT),
          Arguments.of("SEARCH_MARKERS_ONLY", PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY),
          Arguments.of("FENCED", PrintOutputCapture.MarkerPrinter.FENCED),
          Arguments.of("SANITIZED", PrintOutputCapture.MarkerPrinter.SANITIZED)
        );
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void markersBuiltOnTheHost() {
        Set<Class<?>> built = new HashSet<>();
        SoftAssertions.assertSoftly(softly -> {
            List<SourceFile> sources = new ArrayList<>(parse(SYNTAX));
            sources.addAll(treesTheParserDoesNotProduce(softly));
            for (SourceFile parsed : sources) {
                // under a new id a marker is sent whole, where one the parser attached is only referred back to
                SourceFile rebuilt = (SourceFile) new PythonVisitor<Integer>() {
                    @Override
                    public <M extends Marker> M visitMarker(Marker marker, Integer p) {
                        built.add(marker.getClass());
                        return marker.withId(randomId());
                    }
                }.visitNonNull(parsed, 0);
                softly.assertThat(rpc().print(rebuilt)).isEqualTo(parsed.printAll());
            }
        });
        assertThat(built).contains(KeywordArguments.class, KeywordOnlyArguments.class, TrailingComma.class,
          OmitParentheses.class, Semicolon.class, SuppressNewline.class);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void subtrees() {
        SoftAssertions.assertSoftly(softly -> {
            List<SourceFile> sources = new ArrayList<>(parse(SYNTAX));
            sources.addAll(treesTheParserDoesNotProduce(softly));
            for (SourceFile parsed : sources) {
                assertThat(parsed).isInstanceOf(Py.CompilationUnit.class);
                new PythonVisitor<Integer>() {
                    @Override
                    public J preVisit(J tree, Integer p) {
                        if (!(tree instanceof SourceFile)) {
                            Cursor parent = getCursor().getParentOrThrow();
                            softly.assertThat(tree.print(parent))
                              .as("%s in %s", tree.getClass().getName(), parsed.printAll())
                              .isEqualTo(rpc().print(tree, parent));
                        }
                        return tree;
                    }
                }.visit(parsed, 0);
            }
        });
    }

    @Test
    void subtreesWithTheirCursor() {
        SourceFile cu = parse(List.of(
          """
            from a import b
            x = 1
            def f(a: int) -> int:
                return a
            if x:
                pass
            elif y:
                pass
            """
        )).get(0);

        Map<Class<?>, String> printed = new HashMap<>();
        Map<Class<?>, String> withoutAncestors = new HashMap<>();
        new PythonVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                if (tree instanceof J.Assignment || tree instanceof Py.TypeHint || tree instanceof J.Import ||
                    tree instanceof J.If.Else || tree instanceof J.VariableDeclarations.NamedVariable) {
                    // the cursor a visitor is at has padding in it, the one most recipes pass has none
                    printed.put(tree.getClass(), tree.print(getCursor().getParentOrThrow()));
                    assertThat(tree.print(getCursor().getParentTreeCursor())).isEqualTo(printed.get(tree.getClass()));

                    PrintOutputCapture<Integer> alone = new PrintOutputCapture<>(0);
                    new PythonPrinter<Integer>().visit(tree, alone);
                    withoutAncestors.put(tree.getClass(), alone.getOut());
                }
                return tree;
            }
        }.visit(cu, 0);

        assertThat(printed).containsOnly(
          entry(J.Import.class, "b"),
          entry(J.Assignment.class, "\nx = 1"),
          entry(J.VariableDeclarations.NamedVariable.class, "a: int"),
          entry(Py.TypeHint.class, " -> int"),
          entry(J.If.Else.class, "\nelif y:\n    pass")
        );
        // what the Python process prints for a request that carries no cursor
        assertThat(withoutAncestors).containsOnly(
          entry(J.Import.class, "importb"),
          entry(J.Assignment.class, "\nx := 1"),
          entry(J.VariableDeclarations.NamedVariable.class, "a"),
          entry(Py.TypeHint.class, " : int"),
          entry(J.If.Else.class, "\nelse:if y:\n    pass")
        );
    }

    @ParameterizedTest
    @MethodSource("markerPrinters")
    void droppedMarker(String name, PrintOutputCapture.MarkerPrinter markerPrinter) {
        // what an LST stored while a marker had no codec holds in that marker's place
        Py.CompilationUnit cu = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitAssignment(J.Assignment assignment, Integer p) {
                return assignment.withMarkers(assignment.getMarkers().withMarkers(singletonList(null)));
            }
        }.visitNonNull(parse(List.of("x = 1\n")).get(0), 0);

        assertThat(cu.printAll(new PrintOutputCapture<>(0, markerPrinter))).isEqualTo("x = 1\n");
    }

    private static void assertParity(SoftAssertions softly, Py.CompilationUnit cu, String source) {
        String printed = cu.printAll();
        softly.assertThat(printed).as("%s against its source", cu.getSourcePath()).isEqualTo(source);
        softly.assertThat(printed).as("%s against the Python printer", cu.getSourcePath()).isEqualTo(rpc().print(cu));
    }

    private static List<SourceFile> parse(List<String> sources) {
        return PythonParser.builder().build().parse(sources.toArray(new String[0])).collect(toList());
    }

    private static PythonRewriteRpc rpc() {
        return PythonRewriteRpc.getOrStart();
    }

    private static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<Class<?>> treeTypes(Tree tree) {
        Set<Class<?>> types = new HashSet<>();
        new PythonVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                types.add(tree.getClass());
                return tree;
            }
        }.visit(tree, 0);
        return types;
    }

    /**
     * Every tree in {@link Py}, and every tree the printer has a rendering of its own for.
     */
    private static Set<Class<?>> printedTreeTypes() {
        Set<Class<?>> types = new HashSet<>();
        collectTrees(Py.class, types);
        List<Class<?>> printers = new ArrayList<>(List.of(PythonPrinter.class.getDeclaredClasses()));
        assertThat(printers).anyMatch(JavaPrinter.class::isAssignableFrom);
        printers.add(PythonPrinter.class);
        for (Class<?> printer : printers) {
            for (Method method : printer.getDeclaredMethods()) {
                if (method.getName().startsWith("visit") && method.getParameterCount() == 2) {
                    Class<?> tree = method.getParameterTypes()[0];
                    if (J.class.isAssignableFrom(tree) && tree != J.class) {
                        types.add(tree);
                    }
                }
            }
        }
        return types;
    }

    private static void collectTrees(Class<?> enclosing, Set<Class<?>> types) {
        for (Class<?> nested : enclosing.getDeclaredClasses()) {
            if (Py.class.isAssignableFrom(nested)) {
                types.add(nested);
                collectTrees(nested, types);
            }
        }
    }

    private static List<Py.CompilationUnit> treesTheParserDoesNotProduce(SoftAssertions softly) {
        List<SourceFile> parsed = parse(List.of(
          "x = [1, 2]\n",
          "def f(args, kwargs): pass\n",
          "def f[T: int](): pass\n",
          "def f[T](): pass\n",
          "x = 1\n",
          "def g():\n    yield\n",
          "if a:\n    x = 1\nelse:\n    y = 2\nwhile a:\n    z = 3\nfor i in a:\n    w = 4\n"
        ));

        Py.CompilationUnit newArray = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitCollectionLiteral(Py.CollectionLiteral literal, Integer p) {
                return new J.NewArray(randomId(), literal.getPrefix(), literal.getMarkers(), null, emptyList(),
                  literal.getPadding().getElements(), null);
            }
        }.visitNonNull(parsed.get(0), 0);
        softly.assertThat(newArray.printAll()).isEqualTo("x = [1, 2]\n");

        Py.CompilationUnit specialParameters = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                Py.SpecialParameter.Kind kind = "args".equals(multiVariable.getVariables().get(0).getSimpleName()) ?
                  Py.SpecialParameter.Kind.ARGS : Py.SpecialParameter.Kind.KWARGS;
                return multiVariable.withTypeExpression(
                  new Py.SpecialParameter(randomId(), Space.EMPTY, Markers.EMPTY, kind, null, null));
            }
        }.visitNonNull(parsed.get(1), 0);
        softly.assertThat(specialParameters.printAll()).isEqualTo("def f(*args, **kwargs): pass\n");

        // a type parameter default, which only Python 3.13 and later can parse
        J.Identifier str = new J.Identifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), "str", null, null);
        Py.CompilationUnit constraintAndDefault = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitTypeParameter(J.TypeParameter typeParam, Integer p) {
                JContainer<TypeTree> bounds = typeParam.getPadding().getBounds();
                List<JRightPadded<TypeTree>> elements = new ArrayList<>();
                elements.add(bounds.getPadding().getElements().get(0).withAfter(Space.SINGLE_SPACE));
                elements.add(JRightPadded.build(str));
                return typeParam.getPadding().withBounds(bounds.getPadding().withElements(elements));
            }
        }.visitNonNull(parsed.get(2), 0);
        softly.assertThat(constraintAndDefault.printAll()).isEqualTo("def f[T: int = str](): pass\n");

        Py.CompilationUnit defaultOnly = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitTypeParameter(J.TypeParameter typeParam, Integer p) {
                List<JRightPadded<TypeTree>> elements = new ArrayList<>();
                elements.add(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)));
                elements.add(JRightPadded.build(str));
                return typeParam.getPadding().withBounds(JContainer.build(Space.SINGLE_SPACE, elements, Markers.EMPTY));
            }
        }.visitNonNull(parsed.get(3), 0);
        softly.assertThat(defaultOnly.printAll()).isEqualTo("def f[T = str](): pass\n");

        Py.CompilationUnit suppressedNewline = (Py.CompilationUnit) parsed.get(4);
        suppressedNewline = suppressedNewline.withMarkers(suppressedNewline.getMarkers().add(new SuppressNewline(randomId())));
        softly.assertThat(suppressedNewline.printAll()).isEqualTo("x = 1");

        Py.CompilationUnit wrappedAssignment = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitAssignment(J.Assignment assignment, Integer p) {
                return new Py.ExpressionStatement(randomId(), assignment);
            }
        }.visitNonNull(parsed.get(4), 0);
        softly.assertThat(wrappedAssignment.printAll()).isEqualTo("x = 1\n");

        Py.CompilationUnit docstringComment = withComment(parsed.get(4), new TextComment(true, "doc", "\n", Markers.EMPTY));
        softly.assertThat(docstringComment.printAll()).isEqualTo("\"\"\"doc\"\"\"\nx = 1\n");

        // the comment the JVM's own whitespace helpers build
        Py.CompilationUnit pyComment = withComment(parsed.get(4), new PyComment(" note", "\n", false, Markers.EMPTY));
        softly.assertThat(pyComment.printAll()).isEqualTo("# note\nx = 1\n");

        J.Identifier hinted = new J.Identifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), "int", null, null);
        Py.CompilationUnit hintedSpecialParameter = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                Py.SpecialParameter special = (Py.SpecialParameter) multiVariable.getTypeExpression();
                return "args".equals(multiVariable.getVariables().get(0).getSimpleName()) ?
                  multiVariable.withTypeExpression(special.withTypeHint(
                    new Py.TypeHint(randomId(), Space.EMPTY, Markers.EMPTY, hinted, null))) :
                  multiVariable;
            }
        }.visitNonNull(specialParameters, 0);
        softly.assertThat(hintedSpecialParameter.printAll()).isEqualTo("def f(*args: int, **kwargs): pass\n");

        // a marker that prints on the statement an expression wraps, which both are asked for
        Py.CompilationUnit markedStatementExpression = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitStatementExpression(Py.StatementExpression statementExpression, Integer p) {
                return statementExpression.withMarkers(Markers.EMPTY.add(new Semicolon(randomId())));
            }
        }.visitNonNull(parsed.get(5), 0);
        softly.assertThat(markedStatementExpression.printAll()).isEqualTo("def g():\n    ;yield\n");

        // source a recipe on the JVM left unmapped
        Py.CompilationUnit unknown = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitLiteral(J.Literal literal, Integer p) {
                return new J.Unknown(randomId(), literal.getPrefix(), Markers.EMPTY,
                  new J.Unknown.Source(randomId(), Space.EMPTY, Markers.EMPTY, "@@"));
            }
        }.visitNonNull(parsed.get(4), 0);
        softly.assertThat(unknown.printAll()).isEqualTo("x = @@\n");

        // an assignment that is a statement only because it is the very body of its parent
        Py.CompilationUnit bodiesWithoutBlocks = (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitBlock(J.Block block, Integer p) {
                return block.getStatements().get(0).withPrefix(Space.SINGLE_SPACE);
            }
        }.visitNonNull(parsed.get(6), 0);
        softly.assertThat(bodiesWithoutBlocks.printAll())
          .isEqualTo("if a: x = 1\nelse: y = 2\nwhile a: z = 3\nfor i in a: w = 4\n");

        return List.of(newArray, specialParameters, constraintAndDefault, defaultOnly,
          suppressedNewline, wrappedAssignment, docstringComment, pyComment, hintedSpecialParameter,
          markedStatementExpression, unknown, bodiesWithoutBlocks);
    }

    private static Py.CompilationUnit withComment(SourceFile cu, Comment comment) {
        return (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public J visitAssignment(J.Assignment assignment, Integer p) {
                return assignment.withPrefix(Space.build("", List.of(comment)));
            }
        }.visitNonNull(cu, 0);
    }

    private static Py.CompilationUnit markEveryTree(Py.CompilationUnit cu) {
        AtomicInteger count = new AtomicInteger();
        return (Py.CompilationUnit) new PythonVisitor<Integer>() {
            @Override
            public @Nullable J postVisit(J tree, Integer p) {
                return tree.withMarkers(tree.getMarkers().add(marker(count.getAndIncrement())));
            }
        }.visitNonNull(cu, 0);
    }

    private static Marker marker(int n) {
        switch (n % 6) {
            case 0:
                return new SearchResult(randomId(), null);
            case 1:
                return new SearchResult(randomId(), n % 4 == 1 ? "" : "found " + n);
            case 2:
                return new Markup.Warn(randomId(), "warn " + n, "detail");
            case 3:
                return new Markup.Error(randomId(), "error " + n, "detail");
            case 4:
                return new Markup.Info(randomId(), "info " + n, null);
            default:
                return new Markup.Debug(randomId(), "debug " + n, null);
        }
    }
}
