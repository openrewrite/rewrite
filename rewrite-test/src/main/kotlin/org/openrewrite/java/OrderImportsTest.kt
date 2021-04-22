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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Tree
import org.openrewrite.Tree.randomId
import org.openrewrite.java.style.ImportLayoutStyle
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
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            import java.util.Set;
            import org.openrewrite.java.tree.JavaType;
            import org.openrewrite.java.tree.TypeUtils;
            
            class A {}
        """,
        after = """
            import org.openrewrite.java.tree.JavaType;
            import org.openrewrite.java.tree.TypeUtils;
            
            import java.util.ArrayList;
            import java.util.Objects;
            import java.util.Set;
            import java.util.regex.Pattern;
            
            import static java.util.stream.Collectors.joining;
            
            class A {}
        """
    )

    @Test
    fun sortInnerAndOuterClassesInTheSamePackage(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import com.netflix.appinfo.AmazonInfo;
            import com.netflix.appinfo.AmazonInfo.MetaDataKey;
            import com.netflix.appinfo.ApplicationInfoManager;
            import com.netflix.appinfo.DataCenterInfo.Name;
            import com.netflix.appinfo.InstanceInfo;
            
            class Test {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/259")
    @Test
    fun multipleClassesWithTheSameNameButDifferentPackages(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import org.another.Comment;
            import org.openrewrite.java.tree.Comment;
            import org.openrewrite.java.tree.Coordinates;
            import org.openrewrite.java.tree.Expression;
            import org.openrewrite.java.tree.Flag;
            import org.openrewrite.java.tree.JavaType;
            
            class Test {}
        """,
        dependsOn = arrayOf(
            """
            package org.another;
            public class Comment {}
        """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/352")
    @Test
    fun innerClasses(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import org.openrewrite.java.tree.Comment;
            import org.openrewrite.java.tree.Coordinates;
            import org.openrewrite.java.tree.Expression;
            import org.openrewrite.java.tree.Flag;
            import org.openrewrite.java.tree.J.Assert;
            import org.openrewrite.java.tree.J.ClassDeclaration;
            import org.openrewrite.java.tree.J.MethodDeclaration;
            import org.openrewrite.java.tree.J.NewArray;
            import org.openrewrite.java.tree.J.NewClass;
            import org.openrewrite.java.tree.JavaType;
            
            class Test {}
        """,
        after = """
            import org.openrewrite.java.tree.*;
            import org.openrewrite.java.tree.J.*;
            
            class Test {}
        """
    )

    @Test
    fun blankLineThenEmptyBlockThenNonEmptyBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.ArrayList;
            import java.util.Objects;

            import org.openrewrite.java.tree.JavaType;
            
            class A {}
        """,
        after = """
            import org.openrewrite.java.tree.JavaType;
            
            import java.util.ArrayList;
            import java.util.Objects;
            
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
        recipe = recipe.withRemoveUnused(true),
        before = """
            import java.util.*;
            
            class A {
                List list;
                List list2;
            }
        """,
        after = """
            import java.util.List;
            
            class A {
                List list;
                List list2;
            }
        """
    )

    @Test
    fun unfoldStarMultiple(jp: JavaParser) = assertChanged(
        jp,
        recipe = recipe.withRemoveUnused(true),
        before = """
            import java.util.*;
            
            class A {
                List list;
                List list2;
                Map map;
            }
        """,
        after = """
            import java.util.List;
            import java.util.Map;
            
            class A {
                List list;
                List list2;
                Map map;
            }
        """
    )

    @Test
    fun removeUnused(jp: JavaParser) = assertChanged(
        jp,
        recipe = recipe.withRemoveUnused(true),
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
        recipe = recipe.withRemoveUnused(true),
        before = """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {
                List list = emptyList();
            }
        """,
        after = """
            import java.util.List;
            
            import static java.util.Collections.emptyList;
            
            class A {
                List list = emptyList();
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
                void bar() {
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
        recipe = recipe.withRemoveUnused(true),
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
                void bar() {
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
        recipe = recipe.withRemoveUnused(true),
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
        recipe = recipe.withRemoveUnused(true),
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
        recipe = recipe.withRemoveUnused(true),
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

    @Disabled
    @Issue("#352")
    @Test
    fun groupImportsIsAwareOfNestedClasses(jp: JavaParser) = assertChanged(
        jp,
        recipe = recipe.withRemoveUnused(false),
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
                    List l = Arrays.asList(new A(), new B(), new C(), new D(), new E(), new F(), new G(), new H(), new H1());
                }
            }
        """,
        after = """
            package org.bar;
    
            import org.abc.*;
            import org.abc.H.H1;
            import java.util.Arrays;
            import java.util.List;
    
    
            public class C {
                void c() {
                    List l = Arrays.asList(new A(), new B(), new C(), new D(), new E(), new F(), new G(), new H(), new H1());
                }
            }
        """
    )
}

