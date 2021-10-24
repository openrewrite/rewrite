/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Tree.randomId
import org.openrewrite.java.marker.JavaSourceSet
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style

interface OrderImportsTest : JavaRecipeTest {
    override val recipe: OrderImports
        get() = OrderImports(false)

    companion object {
        const val a = """
            package com.foo;
            
            public class A {
                public static int one() { return 1;}
                public static int plusOne(int n) { return n + 1; }
                public static int three() { return 3; }
            }
        """

        const val b = """
            package com.foo;
            
            public class B {
                public static int two() { return 2; }
                public static int multiply(int n, int n2) { return n * n2; }
                public static int four() { return 4; }
            }    
        """
    }

    @Test
    fun orderImports(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.stream.Collectors.joining;
            
            class A {}
        """,
        after = """
            
            import static java.util.stream.Collectors.joining;
            
            class A {}
        """
    )

    @Test
    fun sortInnerAndOuterClassesInTheSamePackage(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/259")
    @Test
    fun multipleClassesWithTheSameNameButDifferentPackages(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import org.another.Comment;
            import org.openrewrite.java.tree.CoordinatesBuilder;
            
            class Test {}
        """,
        dependsOn = arrayOf(
            """
            package org.another;
            public class Comment {}
        """
        )
    )

    @Test
    fun blankLineThenEmptyBlockThenNonEmptyBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            
            class A {}
        """,
        after = """
            
            class A {}
        """
    )

    @Test
    fun foldIntoStar(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            import java.util.Set;
            import java.util.Map;
            
            class A {}
        """,
        after = """
            import java.util.*;
            import java.util.regex.Pattern;
            
            class A {}
        """
    )

    @Test
    fun blankLinesNotFollowedByBlockArentAdded(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {}
        """
    )

    @Test
    fun foldIntoExistingStar(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.*;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            
            class A {}
        """,
        after = """
            import java.util.*;
            import java.util.regex.Pattern;
            
            class A {}
        """
    )

    @Test
    fun idempotence(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.*;
            import java.util.regex.Pattern;

            class A {}
        """
    )

    @Test
    fun unfoldStar(jp: JavaParser) = assertChanged(
        jp,
        recipe = OrderImports(true),
        before = """
            import java.util.*;
            
            class A {
                List<Integer> list;
                List<Integer> list2;
            }
        """,
        after = """
            import java.util.List;
            
            class A {
                List<Integer> list;
                List<Integer> list2;
            }
        """
    )

    @Test
    fun unfoldStarMultiple(jp: JavaParser) = assertChanged(
        jp,
        recipe = OrderImports(true),
        before = """
            import java.util.*;
            
            class A {
                List<Integer> list;
                List<Integer> list2;
                Map<Integer, Integer> map;
            }
        """,
        after = """
            import java.util.List;
            import java.util.Map;
            
            class A {
                List<Integer> list;
                List<Integer> list2;
                Map<Integer, Integer> map;
            }
        """
    )

    @Test
    fun removeUnused(jp: JavaParser) = assertChanged(
        jp,
        recipe = OrderImports(true),
        before = """
            import java.util.*;
            
            class A {
            }
        """,
        after = """
            class A {
            }
        """
    )

    @Test
    fun unfoldStaticStar(jp: JavaParser) = assertChanged(
        jp,
        recipe = OrderImports(true),
        before = """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {
                List<Integer> list = emptyList();
            }
        """,
        after = """
            import java.util.List;
            
            import static java.util.Collections.emptyList;
            
            class A {
                List<Integer> list = emptyList();
            }
        """
    )

    @Test
    fun packagePatternEscapesDots(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import javax.annotation.Nonnull;
            
            class A {}
        """
    )

    @Test
    fun twoImportsFollowedByStar(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            import java.io.UncheckedIOException;
            import java.nio.files.*;
            
            class A {}
        """
    )

    @Test
    fun springCloudFormat() = assertUnchanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    randomId(), "spring", "spring", "spring", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .classCountToUseStarImport(999)
                            .nameCountToUseStarImport(999)
                            .importPackage("java.*")
                            .blankLine()
                            .importPackage("javax.*")
                            .blankLine()
                            .importAllOthers()
                            .blankLine()
                            .importPackage("org.springframework.*")
                            .blankLine()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        before = """
            import java.io.ByteArrayOutputStream;
            import java.nio.charset.StandardCharsets;
            import java.util.Collections;
            import java.util.zip.GZIPOutputStream;
            
            import javax.servlet.ReadListener;
            import javax.servlet.ServletInputStream;
            import javax.servlet.ServletOutputStream;
            
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;
            
            import org.springframework.core.io.buffer.DataBuffer;
            import org.springframework.core.io.buffer.DataBufferFactory;
            import org.springframework.http.HttpHeaders;
            import org.springframework.util.MultiValueMap;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.server.ServerWebExchange;
            
            import static java.util.Arrays.stream;
            import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.toAsyncPredicate;
            
            class A {}
        """
    )

    @Test
    fun importSorting(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import r.core.Flux;
            import s.core.Flux;
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;
            
            class A {}
        """,
        after = """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import r.core.Flux;
            import reactor.core.publisher.Mono;
            import s.core.Flux;
            
            class A {}
        """
    )

    @Test
    fun foldGroupOfStaticImportsThatAppearLast(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.stream.Collectors.toList;
            import static java.util.stream.Collectors.toMap;
            import static java.util.stream.Collectors.toSet;
            
            class A {}
        """,
        after = """
            import static java.util.stream.Collectors.*;
            
            class A {}
        """
    )

    @Test
    fun preservesStaticStarImportWhenRemovingUnused(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.foo;
                
                public class A {
                    public static void foo() {}
                    public static void bar() {}
                    public static void baz() {}
                    public static void foo2() {}
                    public static void bar2() {}
                    public static void baz2() {}
                }
            """
        ),
        before = """
            package org.bar;
            
            import static com.foo.A.*;
            
            class B {
                void barB() {
                    foo();
                    bar();
                    baz();
                    foo2();
                    bar2();
                    baz2();
                }
            }
        """
    )


    @Test
    fun preservesStaticInheritanceImport(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(true),
        dependsOn = arrayOf(
            """
                package com.baz;
                
                public class A {
                    public static void foo() {}
                    public static void bar() {}
                    public static void baz() {}
                    public static void foo2() {}
                    public static void bar2() {}
                    public static void baz2() {}
                }
            """,
            """
                package com.foo;
                
                import com.baz.A; 
                
                public class B extends A { }
            """
        ),
        before = """
            package org.bar;
            
            import static com.foo.B.*;
            
            class C {
                void method() {
                    foo();
                    bar();
                    baz();
                    foo2();
                    bar2();
                    baz2();
                }
            }
        """
    )

    @Test
    fun preservesStaticMethodArguments(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(true),
        dependsOn = arrayOf(
            """
                package com.foo;
                
                public class A {
                    public static void foo(String bar) {}
                    public static String stringify(Integer baz) { return baz.toString(); }
                    public static Integer numberOne() { return 1; }
                    public static Integer plusOne(Integer n) { return n + 1; }
                    public static Integer timesTwo(Integer n) { return n * 2; }
                    public static Integer numberTwo() { return 2; }
                }
            """
        ),
        before = """
            package org.bar;
            
            import static com.foo.A.*;
            
            class B {
                void bar() {
                    foo(stringify(numberOne()));
                    timesTwo(plusOne(numberTwo()));
                }
            }
        """
    )

    @Test
    fun preservesDifferentStaticImportsFromSamePackage(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(true),
        dependsOn = arrayOf(a, b),
        before = """
            package org.bar;
            
            import static com.foo.A.one;
            import static com.foo.A.plusOne;
            import static com.foo.B.multiply;
            import static com.foo.B.two;
            
            public class C {
                void c() {
                    multiply(plusOne(one()), two());
                }
            }
        """
    )

    @Test
    fun collapsesDifferentStaticImportsFromSamePackage(jp: JavaParser) = assertChanged(
        jp,
        recipe = OrderImports(true),
        dependsOn = arrayOf(a, b),
        before = """
            package org.bar;
            
            import static com.foo.A.one;
            import static com.foo.A.plusOne;
            import static com.foo.A.three;
            import static com.foo.B.multiply;
            import static com.foo.B.two;
            import static com.foo.B.four;
            
            public class C {
                void c() {
                    multiply(plusOne(one()), two());
                    three();
                    four();
                }
            }
        """,
        after = """
            package org.bar;
            
            import static com.foo.A.*;
            import static com.foo.B.*;
            
            public class C {
                void c() {
                    multiply(plusOne(one()), two());
                    three();
                    four();
                }
            }
        """
    )

    @Test
    fun removesRedundantImports(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a, b),
        before = """
            package org.bar;
            
            import com.foo.B;
            import com.foo.B;
            
            import static com.foo.A.one;
            import static com.foo.A.one;
            
            public class C {
                void c() {
                    one();
                    B.two();
                }
            }
        """,
        after = """
            package org.bar;
            
            import com.foo.B;
            
            import static com.foo.A.one;
            
            public class C {
                void c() {
                    one();
                    B.two();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/474")
    @Test
    fun blankLinesBetweenImports(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(
            listOf(
                NamedStyles(
                    randomId(),
                    "custom",
                    "custom style",
                    null,
                    emptySet(),
                    listOf(
                        ImportLayoutStyle.builder()
                            .classCountToUseStarImport(9999)
                            .nameCountToUseStarImport(9999)
                            .importStaticAllOthers()
                            .blankLine()
                            .importAllOthers()
                            .blankLine()
                            .build() as Style
                    )
                )
            )
        ).build(),
        before = """
            import java.util.List;
            import static java.util.Collections.singletonList;
            class Test {
            }
        """,
        after = """
            import static java.util.Collections.singletonList;
            
            import java.util.List;
            
            class Test {
            }
        """
    )

    @Issue("#352")
    @Test
    fun groupImportsIsAwareOfNestedClasses(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
            package org.abc;
            public class A {}
            public class B {}
            public class C {}
            public class D {}
            public class E {}
            public class F {}
            public class G {}
            public class H {
                public class H1 {}
            }
        """
        ),
        before = """
            package org.bar;
            
            import org.abc.A;
            import org.abc.B;
            import org.abc.C;
            import org.abc.D;
            import org.abc.E;
            import org.abc.F;
            import org.abc.G;
            import org.abc.H;
            import org.abc.H.H1;
            import java.util.Arrays;
            import java.util.List;
            
            public class C {
                void c() {
                    List<?> l = Arrays.asList(new A(), new B(), new C(), new D(), new E(), new F(), new G(), new H(), new H1());
                }
            }
        """.trimIndent(),
        after = """
            package org.bar;
            
            import org.abc.*;
            import org.abc.H.H1;
            
            import java.util.Arrays;
            import java.util.List;
            
            public class C {
                void c() {
                    List<?> l = Arrays.asList(new A(), new B(), new C(), new D(), new E(), new F(), new G(), new H(), new H1());
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/733")
    @Test
    fun detectBlockPattern(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package org.bar;
            
            // org.slf4j should be detected as a block pattern, and not be moved to all other imports.
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            import java.util.Arrays;
            import java.util.List;
            
            public class C {
            }
        """
    )

    @Test
    fun doNotFoldImports(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(
            listOf(
                NamedStyles(
                    randomId(),
                    "custom",
                    "custom style",
                    null,
                    emptySet(),
                    listOf(
                        ImportLayoutStyle.builder()
                            .classCountToUseStarImport(2147483647)
                            .nameCountToUseStarImport(2147483647)
                            .importPackage("java.*")
                            .blankLine()
                            .importPackage("javax.*")
                            .blankLine()
                            .importAllOthers()
                            .blankLine()
                            .importStaticAllOthers()
                            .build() as Style
                    )
                )
            )
        ).build(),
        before = """
            package org.bar;
            
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            
            import javax.persistence.Entity;
            import javax.persistence.FetchType;
            import javax.persistence.JoinColumn;
            import javax.persistence.JoinTable;
            import javax.persistence.ManyToMany;
            import javax.persistence.Table;
            
            public class C {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/860")
    @Test
    fun orderAndDoNotFoldImports(jp: JavaParser) {
        val inputs = arrayOf(
            """
            package org.test;
            
            import org.foo.FooB;
            import org.bar.BarB;
            import org.foo.FooD;
            import org.foo.FooC;
            import org.bar.BarC;
            import org.foo.FooA;
            import org.foo.FooE;
            import org.bar.BarD;
            import org.bar.BarA;
            import org.bar.BarE;
            
            public class Test {
                FooA fooA = new FooA();
                FooB fooB = new FooB();
                FooC fooC = new FooC();
                FooD fooD = new FooD();
                FooE fooE = new FooE();
                
                BarA barA = new BarA();
                BarB barB = new BarB();
                BarC barC = new BarC();
                BarD barD = new BarD();
                BarE barE = new BarE();
            }
            """.trimIndent(),
            """package org.foo; public class Shared {}""".trimIndent(),
            """package org.foo; public class FooA {}""".trimIndent(),
            """package org.foo; public class FooB {}""".trimIndent(),
            """package org.foo; public class FooC {}""".trimIndent(),
            """package org.foo; public class FooD {}""".trimIndent(),
            """package org.foo; public class FooE {}""".trimIndent(),
            """package org.bar; public class Shared {}""".trimIndent(),
            """package org.bar; public class BarA {}""".trimIndent(),
            """package org.bar; public class BarB {}""".trimIndent(),
            """package org.bar; public class BarC {}""".trimIndent(),
            """package org.bar; public class BarD {}""".trimIndent(),
            """package org.bar; public class BarE {}""".trimIndent()
        )

        val sourceFiles = parser.parse(executionContext, *inputs)

        val classNames = arrayOf(
            "org.foo.Shared", "org.foo.FooA", "org.foo.FooB", "org.foo.FooC", "org.foo.FooD", "org.foo.FooE",
            "org.bar.Shared", "org.bar.BarA", "org.bar.BarB", "org.bar.BarC", "org.bar.BarD", "org.bar.BarE")

        val fqns: MutableSet<JavaType.FullyQualified> = mutableSetOf()
        classNames.forEach { fqns.add(JavaType.Class.build(it)) }
        val sourceSet = JavaSourceSet(randomId(),"main", fqns)
        val markedFiles: MutableList<J.CompilationUnit> = mutableListOf()
        sourceFiles.forEach { markedFiles.add(it.withMarkers(it.markers.addIfAbsent(sourceSet))) }

        val recipe = OrderImports(false).visitor
        val result = recipe.visit(markedFiles[0], InMemoryExecutionContext())
        assertThat((result as J.CompilationUnit).imports.size == 10).isTrue
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/860")
    @Test
    fun orderAndDoNotFoldStaticClasses(jp: JavaParser) {
        val inputs = arrayOf(
            """
            package org.test;
            
            import static org.faz.Faz.FooA;
            import static org.faz.Faz.FooB;
            import static org.faz.Faz.FooC;
            import static org.baz.Baz.BarA;
            import static org.baz.Baz.BarB;
            import static org.baz.Baz.BarC;
            
            public class Test {
                FooA fooA;
                FooB fooB;
                FooC fooC;
                BarA barA;
                BarB barB;
                BarC barC;
            }
            """.trimIndent(),
            """
            package org.faz;
            public class Faz {
                public static class Shared {}
                public static class FooA {}
                public static class FooB {}
                public static class FooC {}
            }
            """.trimIndent()
            ,
            """
            package org.baz;
            public class Baz {
                public static class Shared {}
                public static class BarA {}
                public static class BarB {}
                public static class BarC {}
            }
            """.trimIndent(),
        )

        val sourceFiles = parser.parse(executionContext, *inputs)

        // The '$' is a ClassGraph Delimiter for inner classes.
        val classNames = arrayOf(
            "org.faz.Faz\$Shared",
            "org.faz.Faz\$FooA",
            "org.faz.Faz\$FooB",
            "org.faz.Faz\$FooC",
            "org.baz.Baz\$Shared",
            "org.baz.Baz\$BarA",
            "org.baz.Baz\$BarB",
            "org.baz.Baz\$BarC")

        val fqns: MutableSet<JavaType.FullyQualified> = mutableSetOf()
        classNames.forEach { fqns.add(JavaType.Class.build(it)) }
        val sourceSet = JavaSourceSet(randomId(),"main", fqns)
        val markedFiles: MutableList<J.CompilationUnit> = mutableListOf()
        sourceFiles.forEach { markedFiles.add(it.withMarkers(it.markers.addIfAbsent(sourceSet))) }

        val recipe = OrderImports(false).visitor
        val result = recipe.visit(markedFiles[0], InMemoryExecutionContext())
        assertThat((result as J.CompilationUnit).imports.size == 6).isTrue
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/859")
    @Test
    fun doNotFoldPackageWithJavaLangClassNames(jp: JavaParser) {
        val inputs = arrayOf(
            """
            package org.test;
            
            import org.foo.Process;
            import org.foo.FooD;
            import org.foo.FooC;
            import org.foo.Thread;
            import org.foo.FooE;
            
            public class Test {
                Thread thread = new Thread();
                Process process = new Process();
                FooC fooC = new FooC();
                FooD fooD = new FooD();
                FooE fooE = new FooE();
            }
            """.trimIndent(),
            """package org.foo; public class Thread {}""".trimIndent(),
            """package org.foo; public class Process {}""".trimIndent(),
            """package org.foo; public class FooC {}""".trimIndent(),
            """package org.foo; public class FooD {}""".trimIndent(),
            """package org.foo; public class FooE {}""".trimIndent()
        )

        val sourceFiles = parser.parse(executionContext, *inputs)

        val classNames = arrayOf("java.lang.Thread", "org.foo.Thread", "org.foo.Process", "org.foo.FooC", "org.foo.FooD", "org.foo.FooE")

        val fqns: MutableSet<JavaType.FullyQualified> = mutableSetOf()
        classNames.forEach { fqns.add(JavaType.Class.build(it)) }
        val sourceSet = JavaSourceSet(randomId(),"main", fqns)
        val markedFiles: MutableList<J.CompilationUnit> = mutableListOf()
        sourceFiles.forEach { markedFiles.add(it.withMarkers(it.markers.addIfAbsent(sourceSet))) }

        val recipe = OrderImports(false).visitor
        val result = recipe.visit(markedFiles[0], InMemoryExecutionContext())
        assertThat((result as J.CompilationUnit).imports.size == 5).isTrue
    }
}
