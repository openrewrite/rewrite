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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.marker.GroupedImport;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoSum;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.request.Print;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.openrewrite.PrintOutputCapture.MarkerPrinter.DEFAULT;
import static org.openrewrite.PrintOutputCapture.MarkerPrinter.FENCED;
import static org.openrewrite.PrintOutputCapture.MarkerPrinter.SANITIZED;
import static org.openrewrite.PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY;
import static org.openrewrite.Tree.randomId;

/**
 * Holds {@link GolangPrinter}, {@link GoModPrinter} and {@link GoSumPrinter} to the native printers they
 * were ported from, which the RPC engine still prints with. Everything is parsed by the engine, then
 * printed on both sides.
 */
@Timeout(value = 15, unit = TimeUnit.MINUTES)
class PrinterParityTest {

    static Path engine;

    @BeforeAll
    static void startEngine() throws Exception {
        engine = BundledEngine.sources();
        BundledEngine.use();
    }

    @AfterAll
    static void stopEngine() {
        GoRewriteRpc.shutdownCurrent();
    }

    /**
     * The module's own Go sources and those of its dependencies, as packaged for the engine build, along
     * with the go.mod conformance fixtures.
     */
    @TestFactory
    Stream<DynamicTest> corpus() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        try (Stream<Path> files = Files.walk(engine)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().collect(toList())) {
                String name = file.getFileName().toString();
                if (name.endsWith(".go") || "go.mod".equals(name) || "go.sum".equals(name)) {
                    Path path = engine.relativize(file);
                    tests.add(dynamicTest(path.toString(), () -> assertPrintsLikeEngine(path, read(file))));
                }
            }
        }
        try (Stream<Path> fixtures = Files.list(Paths.get("src/test/resources/gomod-conformance"))) {
            for (Path file : fixtures.sorted().collect(toList())) {
                String name = file.getFileName().toString();
                if (name.endsWith(".gomod") || name.endsWith(".gosum")) {
                    Path path = Paths.get(name.endsWith(".gomod") ? "go.mod" : "go.sum");
                    tests.add(dynamicTest(name, () -> assertPrintsLikeEngine(path, read(file))));
                }
            }
        }
        assertThat(tests).hasSizeGreaterThan(100);
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> syntax() {
        return SYNTAX.entrySet().stream().map(source -> dynamicTest(source.getKey(),
                () -> assertPrintsLikeEngine(Paths.get(source.getKey()), source.getValue())));
    }

    /**
     * Fails when a tree type is added without a source in {@link #SYNTAX} that contains it.
     */
    @Test
    void everyTreeTypeHasAParityCase() {
        Set<Class<?>> exercised = new HashSet<>();
        SYNTAX.forEach((name, source) -> {
            for (Cursor node : nodes(parse(Paths.get(name), source))) {
                exercised.add(node.getValue().getClass());
            }
        });
        assertThat(exercised).containsAll(treeTypes());
    }

    // Every tree of go.mod and go.sum, every Go tree, and every J tree the Go printer prints.
    private static Set<Class<?>> treeTypes() {
        Set<Class<?>> types = new HashSet<>();
        types.add(GoMod.class);
        types.add(GoSum.class);
        Stream.of(Go.class, GoMod.class, GoSum.class)
                .flatMap(family -> Stream.of(family.getDeclaredClasses()))
                .filter(type -> Tree.class.isAssignableFrom(type) && !type.isInterface())
                .forEach(types::add);
        for (Method method : GolangPrinter.class.getDeclaredMethods()) {
            if (method.getName().startsWith("visit") && method.getParameterCount() == 2 &&
                    J.class.isAssignableFrom(method.getParameterTypes()[0])) {
                types.add(method.getParameterTypes()[0]);
            }
        }
        return types;
    }

    /**
     * Recipes print parts of a tree, handing the printer the cursor of the part's parent. From there a
     * method takes its receiver and an {@code if} or {@code switch} its init.
     */
    @TestFactory
    Stream<DynamicTest> subtrees() {
        return SYNTAX.entrySet().stream().map(source -> dynamicTest(source.getKey(), () -> {
            SourceFile sf = parse(Paths.get(source.getKey()), source.getValue());
            for (Cursor node : nodes(sf)) {
                Tree tree = node.getValue();
                if (tree != sf) {
                    Cursor parent = node.getParentOrThrow();
                    assertThat(tree.print(parent))
                            .as("%s in %s", tree.getClass().getName(), parent.getValue().getClass().getName())
                            .isEqualTo(GoRewriteRpc.getOrStart().print(tree, parent));
                }
            }
        }));
    }

    /**
     * Handed no more than the source file, neither side has the parent that holds a receiver or an init.
     */
    @Test
    void subtreeWithoutItsParent() {
        SourceFile sf = parse(Paths.get("method.go"), """
                package main

                type T struct {
                    X int `json:"x"`
                }

                func (t *T) M() int {
                    if v := 1; v > 0 {
                        return v
                    }
                    return 0
                }
                """);
        Cursor file = new Cursor(null, sf);
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        for (Cursor node : nodes(sf)) {
            Tree tree = node.getValue();
            if (tree != sf) {
                assertThat(tree.print(file)).isEqualTo(rpc.print(tree, file));
            }
        }
        assertThat(part(sf, J.MethodDeclaration.class).print(file)).startsWith("func M() int {");
        assertThat(part(sf, J.If.class).print(file)).isEqualTo("if v > 0 {\n        return v\n    }");
        assertThat(part(sf, J.VariableDeclarations.class).print(file)).isEqualTo("\n    X int `json:\"x\"`");
    }

    private static Tree part(SourceFile sf, Class<? extends Tree> type) {
        return nodes(sf).stream().map(Cursor::<Tree>getValue).filter(type::isInstance).findFirst().orElseThrow();
    }

    /**
     * A recipe may put whitespace where the parser never does, so with a comment added to every space a
     * visitor reaches, each is printed once and both sides agree on where.
     */
    @TestFactory
    Stream<DynamicTest> spaces() {
        return SYNTAX.entrySet().stream().map(source -> dynamicTest(source.getKey(), () -> {
            List<String> spaces = new ArrayList<>();
            Set<Integer> withoutAPlace = new HashSet<>();
            SourceFile sf = parse(Paths.get(source.getKey()), source.getValue());
            SourceFile commented;
            if (sf instanceof GoMod) {
                commented = (SourceFile) new GoModVisitor<Integer>() {
                    @Override
                    public Space visitSpace(Space space, Integer p) {
                        return numbered(space, spaces, "a space of " + getCursor());
                    }
                }.visitNonNull(sf, 0);
            } else if (sf instanceof GoSum) {
                commented = (SourceFile) new GoSumVisitor<Integer>() {
                    @Override
                    public Space visitSpace(Space space, Integer p) {
                        return numbered(space, spaces, "a space of " + getCursor());
                    }
                }.visitNonNull(sf, 0);
            } else {
                commented = (SourceFile) new GolangVisitor<Integer>() {
                    @Override
                    public Space visitSpace(Space space, Space.Location loc, Integer p) {
                        if (hasNoPlaceInGo(loc)) {
                            withoutAPlace.add(spaces.size());
                        }
                        return numbered(space, spaces, loc + " of " + getCursor());
                    }
                }.visitNonNull(sf, 0);
            }
            String printed = commented.printAll();

            int[] timesPrinted = new int[spaces.size()];
            Matcher numbered = Pattern.compile("/\\*s(\\d+)s\\*/").matcher(printed);
            while (numbered.find()) {
                timesPrinted[Integer.parseInt(numbered.group(1))]++;
            }
            assertThat(spaces).isNotEmpty();
            assertSoftly(softly -> {
                softly.assertThat(IntStream.range(0, spaces.size())
                                .filter(i -> timesPrinted[i] != (withoutAPlace.contains(i) ? 0 : 1))
                                .mapToObj(i -> spaces.get(i) + " printed " + timesPrinted[i] + " times"))
                        .isEmpty();
                softly.assertThat(printed).isEqualTo(GoRewriteRpc.getOrStart().print(commented));
            });
        }));
    }

    private static Space numbered(Space space, List<String> spaces, String where) {
        String text = "s" + spaces.size() + "s";
        spaces.add(where);
        return space.withComments(ListUtils.concat(space.getComments(),
                new TextComment(true, text, "", Markers.EMPTY)));
    }

    /**
     * Go has no {@code static} for the padding of a block or an import to follow, so neither side prints it.
     */
    private static boolean hasNoPlaceInGo(Space.Location loc) {
        return loc == Space.Location.STATIC_INIT_SUFFIX || loc == Space.Location.STATIC_IMPORT;
    }

    /**
     * A marker on every node, printed by each of the marker printers a host can ask for.
     */
    @TestFactory
    Stream<DynamicTest> markers() {
        return SYNTAX.entrySet().stream().flatMap(source -> Stream.of(Print.MarkerPrinter.values())
                .map(mode -> dynamicTest(source.getKey() + " " + mode, () -> {
                    SourceFile marked = mark(parse(Paths.get(source.getKey()), source.getValue()), i ->
                            i % 4 == 0 ? new SearchResult(randomId(), null) :
                                    i % 4 == 1 ? new SearchResult(randomId(), "found") :
                                            i % 4 == 2 ? new Markup.Warn(randomId(), "careful", "detail") :
                                                    new Markup.Info(randomId(), "note", null));
                    String printed = print(marked, markerPrinter(mode));
                    if (mode != Print.MarkerPrinter.SANITIZED) {
                        assertThat(printed).isNotEqualTo(source.getValue());
                    }
                    assertThat(printed).isEqualTo(GoRewriteRpc.getOrStart().print(marked, mode));
                })));
    }

    private static PrintOutputCapture.MarkerPrinter markerPrinter(Print.MarkerPrinter mode) {
        switch (mode) {
            case DEFAULT:
                return DEFAULT;
            case SEARCH_MARKERS_ONLY:
                return SEARCH_MARKERS_ONLY;
            case FENCED:
                return FENCED;
            default:
                return SANITIZED;
        }
    }

    /**
     * Some nodes and every comment are printed by the node that holds them, which then has to print their
     * markers too: a wrapper that only lends its prefix, the parentheses Go does not write, an else, a
     * variable among several declared together, a directive or struct tag.
     */
    @TestFactory
    Stream<DynamicTest> markersOnWhatAnotherNodePrints() {
        return Stream.of("declarations.go", "statements.go").map(name -> dynamicTest(name, () -> {
            AtomicInteger n = new AtomicInteger();
            SourceFile marked = (SourceFile) new GolangVisitor<Integer>() {
                @Override
                public J preVisit(J tree, Integer p) {
                    return tree instanceof Go.MethodDeclaration || tree instanceof Go.StatementWithInit ||
                            tree instanceof J.ControlParentheses || tree instanceof J.If.Else ||
                            tree instanceof J.VariableDeclarations.NamedVariable || tree instanceof J.Annotation ?
                            tree.withMarkers(tree.getMarkers().add(found())) : tree;
                }

                @Override
                public Space visitSpace(Space space, Space.Location loc, Integer p) {
                    return space.withComments(ListUtils.map(space.getComments(),
                            comment -> comment.withMarkers(comment.getMarkers().add(found()))));
                }

                private SearchResult found() {
                    return new SearchResult(randomId(), "m" + n.getAndIncrement());
                }
            }.visitNonNull(parse(Paths.get(name), SYNTAX.get(name)), 0);

            String printed = print(marked, DEFAULT);
            assertThat(n.get()).isGreaterThan(20);
            for (int i = 0; i < n.get(); i++) {
                assertThat(printed).containsOnlyOnce("/*~~(m" + i + ")~~>*/");
            }
            assertThat(printed).isEqualTo(GoRewriteRpc.getOrStart().print(marked, Print.MarkerPrinter.DEFAULT));
        }));
    }

    /**
     * A marker that either side cannot name or has no codec for crosses RPC as null. The engine checks the
     * markers it declares the same way.
     */
    @Test
    void everyMarkerHasACodecOnBothSides() throws Exception {
        String factories = read(engine.resolve("pkg/rpc/value_types.go"));
        Path classes = Paths.get(GroupedImport.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        int markers = 0;
        for (String pkg : new String[]{"marker", "tree"}) {
            try (Stream<Path> files = Files.list(classes.resolve("org/openrewrite/golang").resolve(pkg))) {
                for (Path file : files.collect(toList())) {
                    String name = file.getFileName().toString().replace(".class", "");
                    Class<?> type = Class.forName("org.openrewrite.golang." + pkg + "." + name);
                    if (Marker.class.isAssignableFrom(type)) {
                        markers++;
                        assertThat(RpcCodec.class).as(type.getName()).isAssignableFrom(type);
                        assertThat(factories).contains("RegisterFactory(\"" + type.getName() + "\"");
                    }
                }
            }
        }
        assertThat(markers).isGreaterThan(10);
    }

    /**
     * A tag a recipe changed is quoted anew, and both sides escape the same code points in it.
     */
    @Test
    void interpretedStructTagChangedByARecipe() {
        // a letter, a no-break space, a tab, a zero width space, an emoji, a private use and an unassigned code point
        String key = new String(new int[]{'k', 0xe9, 0xa0, '\t', 0x200b, 0x1f600, 0xe000, 0x378, 0x1c89}, 0, 9);
        SourceFile changed = (SourceFile) new GolangVisitor<Integer>() {
            @Override
            public J visitAnnotation(J.Annotation annotation, Integer p) {
                return annotation.withAnnotationType(((J.Identifier) annotation.getAnnotationType()).withSimpleName(key));
            }
        }.visitNonNull(parse(Paths.get("tag.go"), "package main\n\ntype T struct {\n\tA int \"json:\\\"a\\\"\"\n}\n"), 0);

        String printed = changed.printAll();
        assertThat(printed).contains("\\u00a0\\t\\u200b").contains("\\ue000\\u0378\\u1c89:\\\"a\\\"\"");
        assertThat(printed).isEqualTo(GoRewriteRpc.getOrStart().print(changed));
    }

    /**
     * An LST stored while {@code ChanDirMarker} had no codec holds a null where the marker was.
     */
    @Test
    void nullMarkerInAStoredLst() {
        String source = "package main\n\nvar send chan<- int\n";
        SourceFile stored = (SourceFile) new GolangVisitor<Integer>() {
            @Override
            public J visitChannel(Go.Channel channel, Integer p) {
                return channel.withMarkers(channel.getMarkers().withMarkers(singletonList(null)));
            }
        }.visitNonNull(parse(Paths.get("chan.go"), source), 0);

        assertThat(stored.printAll()).isEqualTo(source);
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        rpc.reset();
        assertThat(rpc.print(stored)).isEqualTo(source);
    }

    // What the engine prints from the tree it parsed, and again from the tree Java holds once it has forgotten its own.
    private static void assertPrintsLikeEngine(Path path, String source) {
        SourceFile sf = parse(path, source);
        for (Cursor node : nodes(sf)) {
            assertThat(node.<Tree>getValue().getMarkers().getMarkers()).doesNotContainNull();
        }
        assertThat(sf.printAll()).isEqualTo(source);
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        assertThat(rpc.print(sf)).isEqualTo(source);
        rpc.reset();
        assertThat(rpc.print(sf)).isEqualTo(source);
    }

    private static SourceFile parse(Path path, String source) {
        String name = path.getFileName().toString();
        Parser parser = "go.mod".equals(name) ? GoModParser.builder().build() :
                "go.sum".equals(name) ? GoSumParser.builder().build() :
                        GolangParser.builder().build();
        SourceFile sf = parser.parseInputs(singletonList(Parser.Input.fromString(path, source)), null,
                new InMemoryExecutionContext()).findFirst().orElseThrow();
        assertThat(sf).as("%s", path).isNotInstanceOf(ParseError.class);
        return sf;
    }

    private static String print(SourceFile sf, PrintOutputCapture.MarkerPrinter markerPrinter) {
        return sf.printAll(new PrintOutputCapture<>(0, markerPrinter));
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static List<Cursor> nodes(SourceFile sf) {
        List<Cursor> nodes = new ArrayList<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree tree, Integer p) {
                nodes.add(getCursor());
                return tree;
            }
        }.visit(sf, 0);
        return nodes;
    }

    private static SourceFile mark(SourceFile sf, IntFunction<Marker> markers) {
        AtomicInteger n = new AtomicInteger();
        return (SourceFile) new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree tree, Integer p) {
                return tree.withMarkers(tree.getMarkers().add(markers.apply(n.getAndIncrement())));
            }
        }.visitNonNull(sf, 0);
    }

    /**
     * Sources that between them contain every tree type, printed whole, in part and with markers.
     */
    private static final Map<String, String> SYNTAX = new LinkedHashMap<>();

    static {
        SYNTAX.put("declarations.go", """
                //go:build !ignore

                // Package main exercises declarations and types.
                package main

                import (
                    "fmt"
                    str "strings"
                )

                import _ "embed"

                import . "math"

                const Tau = 2 * Pi

                const (
                    A = iota
                    B
                    C
                )

                var (
                    x, y int = 1, 2
                    name     = "go"
                )

                var z float64

                //go:embed parity.txt
                var embedded string

                //go:generate stringer -type=Celsius
                type Celsius float64

                type (
                    Alias = Point
                    Grid  [3][3]int
                )

                type Embedded struct{}

                type Point struct {
                    X, Y int    `json:"x" yaml:"x"`
                    Name string "json:\\"name\\""
                    Escaped string "json:\\"\\u00e9\\x41\\""
                    *Embedded
                    Tags map[string][]string
                }

                type Shape interface {
                    Area() float64
                    fmt.Stringer
                }

                type Number interface {
                    ~int | ~float64
                }

                type List[T any] []T

                type Pair[K comparable, V any] struct {
                    Key K
                    Val V
                }

                type Handler func(int, string) (bool, error)

                type Pipes struct {
                    in   <-chan int
                    out  chan<- int
                    both chan int
                    ptr  (*int)
                }

                //go:noinline
                func Add(a, b int) int {
                    return a + b
                }

                func (p *Point) Move(dx, dy int) (int, error) {
                    p.X += dx
                    p.Y += dy
                    return p.X, nil
                }

                func (Point) String() string { return str.ToUpper(name) }

                func Map[T, U any](xs []T, f func(T) U) []U {
                    out := make([]U, 0, len(xs))
                    for _, v := range xs {
                        out = append(out, f(v))
                    }
                    return out
                }

                func Sum[T Number](xs ...T) (total T) {
                    for _, v := range xs {
                        total += v
                    }
                    return
                }

                func external(n int) int

                func main() {
                    p := Pair[string, int]{Key: "a", Val: 1}
                    l := List[int]{1, 2, 3}
                    convert := Map[int, string]
                    fmt.Println(p, l, Tau, A, B, C, x, y, z, Sqrt(2), embedded)
                    fmt.Println(convert(l, func(i int) string { return fmt.Sprint(i) }))
                    fmt.Println(Sum(1, 2, 3), Sum[float64]([]float64{1.5}...))
                }
                """);

        SYNTAX.put("statements.go", """
                package main

                import "fmt"

                type T struct {
                    a, b int
                    next *T
                }

                func pair() (int, error) { return 1, nil }

                func statements(ch chan int, done <-chan struct{}, xs []int, m map[string]int, v interface{}) (int, error) {
                    var arr [5]int
                    auto := [...]string{"a", "b"}
                    grid := [][]int{{1, 2}, {3, 4}}
                    t := &T{a: 1, b: 2}
                    lit := T{
                        a: 1,
                        b: 2,
                    }
                    a, b := 1, 2
                    a, b = b, a
                    a = b
                    a += 1
                    a -= 1
                    a *= 2
                    a /= 2
                    a %= 3
                    a &= 1
                    a |= 1
                    a ^= 1
                    a <<= 1
                    a >>= 1
                    a &^= b
                    a++
                    b--
                    c := a&^b + a&b | a ^ b<<1>>1
                    ok := a < b && a <= b || a > b && a >= b || a == b || a != b
                    neg := -a + +b - ^c
                    not := !ok
                    f := float64(a) * 1.5 / 2
                    r := []rune("hello")
                    ptr := (*T)(nil)
                    deref := *t
                    i, isInt := v.(int)
                    s := xs[1:2]
                    s = xs[:2:3]
                    s = xs[:]
                    first := xs[0]
                    arr[0] = (a + b) * c
                    fn := func(n int) int { return n * 2 }
                    go fn(1)
                    defer fmt.Println("done")
                    ch <- a
                    got := <-ch
                    <-done
                    (fn(2))

                    if a > b {
                        a = b
                    } else if n, err := pair(); err != nil {
                        return n, err
                    } else {
                        b = a
                    }

                    for i := 0; i < 10; i++ {
                        if i%2 == 0 {
                            continue
                        }
                        break
                    }
                    for a < b {
                        a++
                    }
                    for {
                        break
                    }
                    for range xs {
                    }
                    for k, val := range m {
                        fmt.Println(k, val)
                    }

                outer:
                    for _, x := range xs {
                        switch {
                        case x > 1:
                            continue outer
                        case x < 0, x == 0:
                            break outer
                        default:
                            goto end
                        }
                    }

                    switch y := a + b; y {
                    case 1:
                        fallthrough
                    case 2:
                    default:
                    }

                    switch u := v.(type) {
                    case int, int64:
                        fmt.Println(u)
                    case nil:
                    }

                    select {
                    case n := <-ch:
                        fmt.Println(n)
                    case ch <- 1:
                    case <-done:
                    default:
                    }

                    {
                        a = 1; b = 2
                    }
                end:
                    fmt.Println(arr, auto, grid, lit, not, neg, f, r, ptr, deref, i, isInt, s, first, got)
                    return a, nil
                }
                """);

        // a comment or odd spacing in every position that holds whitespace
        SYNTAX.put("spacing.go", """
                /* leading */ package /* p */ main // trailing

                import /* i */ (
                    /* a */ "fmt" /* b */ ; "os"
                    alias /* c */ "strings" // d
                ) /* e */

                var /* v */ (
                    a /* 1 */ , /* 2 */ b /* 3 */ int /* 4 */ = /* 5 */ 1 /* 6 */ , /* 7 */ 2 // 8
                ) /* 9 */

                type /* t */ S /* 1 */ [ /* 2 */ T /* 3 */ any /* 4 */ , /* 5 */ ] /* 6 */ struct /* 7 */ { /* 8 */
                    f /* 9 */ T /* 10 */ `k:"v"   j:"w"` /* 11 */
                    g  ,  h  chan  <-  int
                    i <-  chan int ; j [ 2 ] int
                    k map [ /* 12 */ string /* 13 */ ] /* 14 */ * /* 15 */ int
                    l func /* 16 */ ( /* 17 */ int /* 18 */ , /* 19 */ ... /* 20 */ string /* 21 */ , /* 22 */ ) /* 23 */ ( /* 24 */ int /* 25 */ , /* 26 */ error /* 27 */ , /* 28 */ )
                    m interface { M ( ) }
                    n [ /* 38 */ ] /* 39 */ int
                    o ( /* 40 */ int /* 41 */ )
                    p chan /* 43 */ <- int ; q <- /* 44 */ chan int
                } /* 42 */

                func /* 1 */ ( /* 2 */ s /* 3 */ * /* 4 */ S /* 5 */ [ /* 6 */ T /* 7 */ ] /* 8 */ ) /* 9 */ M /* 10 */ ( /* 11 */ x /* 12 */ int /* 13 */ , /* 14 */ ys /* 15 */ ... /* 16 */ int /* 17 */ ) /* 18 */ ( /* 19 */ r /* 20 */ int /* 21 */ ) /* 22 */ { /* 23 */
                    if /* 1 */ v /* 2 */ := /* 3 */ x /* 4 */ ; /* 5 */ v /* 6 */ > /* 7 */ 0 /* 8 */ { /* 9 */
                    } /* 10 */ else /* 11 */ { /* 12 */ }
                    for /* 1 */ i /* 2 */ := /* 3 */ 0 /* 4 */ ; /* 5 */ i /* 6 */ < /* 7 */ x /* 8 */ ; /* 9 */ i /* 10 */ ++ /* 11 */ { /* 12 */ }
                    for /* 1 */ ; /* 2 */ ; /* 3 */ { /* 4 */ break /* 5 */ }
                    for /* 1 */ x /* 2 */ > /* 3 */ 0 /* 4 */ { x -- }
                    for /* 1 */ k /* 2 */ , /* 3 */ v /* 4 */ := /* 5 */ range /* 6 */ ys /* 7 */ { _ , _ = k , v }
                    for /* 1 */ range /* 2 */ ys /* 3 */ { }
                    switch /* 1 */ y /* 2 */ := /* 3 */ x /* 4 */ ; /* 5 */ y /* 6 */ { /* 7 */
                    case /* 8 */ 1 /* 9 */ , /* 10 */ 2 /* 11 */ : /* 12 */
                        fallthrough /* 13 */
                    default /* 14 */ : /* 15 */
                    } /* 16 */
                    switch /* 1 */ { /* 2 */ }
                    switch /* 1 */ t /* 2 */ := /* 3 */ any /* 4 */ ( /* 5 */ x /* 6 */ , /* 7 */ ) /* 8 */ . /* 9 */ ( /* 10 */ type /* 11 */ ) /* 12 */ { /* 13 */
                    case /* 14 */ int /* 15 */ : /* 16 */ _ = t
                    }
                    c := make /* 1 */ ( /* 2 */ chan /* 3 */ int /* 4 */ , /* 5 */ 1 /* 6 */ , /* 7 */ )
                    select /* 1 */ { /* 2 */
                    case /* 3 */ c /* 4 */ <- /* 5 */ 1 /* 6 */ : /* 7 */
                    case /* 8 */ v /* 9 */ , /* 10 */ ok /* 11 */ := /* 12 */ <- /* 13 */ c /* 14 */ : /* 15 */ _ , _ = v , ok
                    default /* 16 */ : /* 17 */
                    } /* 18 */
                    z := [ /* 1 */ ... /* 2 */ ] /* 3 */ int /* 4 */ { /* 5 */ 0 /* 6 */ : /* 7 */ 1 /* 8 */ , /* 9 */ }
                    w := z /* 1 */ [ /* 2 */ 0 /* 3 */ : /* 4 */ 1 /* 5 */ : /* 6 */ 1 /* 7 */ ]
                    _ = w /* 1 */ [ /* 2 */ 0 /* 3 */ ] /* 4 */ &^ /* 5 */ ( /* 6 */ x /* 7 */ ) /* 8 */
                    x /* 1 */ &^= /* 2 */ 1 /* 3 */ ; x /* 4 */ += /* 5 */ 1
                    p /* 1 */ := /* 2 */ & /* 3 */ x ; _ = * /* 4 */ p + - /* 5 */ x
                    _ = fmt.Sprint ; s /* 1 */ . /* 2 */ M /* 3 */ ( /* 4 */ x , ys /* 5 */ ... /* 6 */ )
                    _ = alias /* 1 */ . /* 2 */ Repeat ; _ = os.Args
                    _ = int64 /* 1 */ ( /* 2 */ x /* 3 */ ) + ( /* 4 */ int64 /* 5 */ ) /* 6 */ ( /* 7 */ x /* 8 */ , /* 9 */ )
                    _ = func /* 1 */ ( /* 2 */ ) /* 3 */ { /* 4 */ } ; go /* 5 */ s.M ( 1 ) ; defer /* 6 */ s.M ( 2 )
                    _ = S /* 1 */ [ /* 2 */ int /* 3 */ , /* 4 */ ] /* 5 */ { /* 6 */ }
                L /* 1 */ : /* 2 */
                    goto /* 3 */ L /* 4 */
                    return /* 1 */ x /* 2 */
                } /* 24 */

                func two [ A /* 1 */ , /* 2 */ B /* 3 */ any ] ( ) ( int , int ) { return /* 1 */ 1 /* 2 */ , /* 3 */ 2 /* 4 */ }

                func use ( ) { a , b = two /* 1 */ [ /* 2 */ int /* 3 */ , /* 4 */ string /* 5 */ ] /* 6 */ ( /* 7 */ ) }

                type C interface /* 29 */ { /* 30 */ ~ /* 31 */ int /* 32 */ | /* 33 */ string /* 34 */ ; M /* 35 */ ( /* 36 */ ) /* 37 */ }
                // eof
                """);

        SYNTAX.put("go.mod", """
                // Deprecated: use example.com/new instead.
                module example.com/old // trailing

                go 1.22

                toolchain go1.22.3

                require example.com/single v1.0.0

                require (
                    example.com/a v1.2.3
                    example.com/b v0.0.0-20240101000000-abcdefabcdef // indirect
                    // standalone comment
                    example.com/c/v2 v2.0.0+incompatible
                )

                replace example.com/a => ../a

                replace (
                    example.com/b v0.0.0-20240101000000-abcdefabcdef => example.com/fork v1.0.0
                )

                exclude example.com/c/v2 v2.0.1

                retract (
                    v1.0.0 // bad
                    [v1.1.0, v1.2.0]
                )

                retract [ v0.1.0 , v0.2.0 ]
                """);

        SYNTAX.put("go.sum", """
                github.com/google/uuid v1.6.0 h1:NIvaJDMOsjHA8n1jAhLSgzrAzy1Hgr+hNrb57e+94F0=
                github.com/google/uuid v1.6.0/go.mod h1:TIyPZe4MgqvfeYDBFedMoGGpEw/LqOeaOT+nhxU+yHo=
                golang.org/x/mod v0.35.0 h1:Ww1D637e6Pg+Zb2KrWfHQUnH2dQRLBQyAtpr/haaJeM=
                golang.org/x/mod v0.35.0/go.mod h1:+GwiRhIInF8wPm+4AoT6L0FA1QWAad3OMdTRx4tFYlU=
                """);

        // `~` outside a constraint only fails type checking
        SYNTAX.put("tilde.go", """
                package main

                var a = 1
                var b = ~ a
                """);

        SYNTAX.put("empty-statements.go", """
                package main

                import `fmt`

                func f() {
                    ;
                    fmt.Println() ; ;
                L:
                    ;
                    goto L
                }
                """);

        // an empty group, a `;` inside one, and a path spelled with an escape
        SYNTAX.put("imports.go", """
                package main

                import ( /* 1 */ )

                import (
                    "fmt" ;
                    str /* 2 */ "strings"
                )

                import _ /* 3 */ "\\x65mbed"

                var _ = fmt.Sprint(str.ToUpper("a"))
                """);
    }
}
