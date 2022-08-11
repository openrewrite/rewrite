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

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Tree.randomId
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style

interface OrderImportsTest : JavaRecipeTest {
    override val recipe: OrderImports
        get() = OrderImports(false)

    @Test
    fun sortInnerAndOuterClassesInTheSamePackage(jp: JavaParser) = assertUnchanged(
        jp,
        before = "class Test {}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/259")
    @Test
    fun multipleClassesWithTheSameNameButDifferentPackages(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.awt.List;
            import java.util.List;
            
            class Test {}
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
        """,
        after = """
            import java.util.*;
            import java.util.regex.Pattern;
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
        """,
        after = """
            import java.util.*;
            import java.util.regex.Pattern;
        """
    )

    @Test
    fun idempotence(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.*;
            import java.util.regex.Pattern;
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
        """,
        after = ""
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
        """
    )

    @Test
    fun twoImportsFollowedByStar(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            import java.io.UncheckedIOException;
            import java.nio.files.*;
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
        """,
        after = """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import r.core.Flux;
            import reactor.core.publisher.Mono;
            import s.core.Flux;
        """
    )

    @Test
    fun foldGroupOfStaticImportsThatAppearLast(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.stream.Collectors.toList;
            import static java.util.stream.Collectors.toMap;
            import static java.util.stream.Collectors.toSet;
        """,
        after = """
            import static java.util.stream.Collectors.*;
        """
    )

    @Test
    fun preservesStaticStarImportWhenRemovingUnused(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(true),
        before = """
            import static java.util.Collections.*;
            
            class Test {
                Object[] o = new Object[] { emptyList(), emptyMap(), emptySet() };
            }
        """
    )

    @Test
    fun preservesStaticInheritanceImport(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(true),
        dependsOn = arrayOf("package my; public class MyCollections extends java.util.Collections {}"),
        before = """
            import static my.MyCollections.*;
            
            class Test {
                Object[] o = new Object[] { emptyList(), emptyMap(), emptySet() };
            }
        """
    )

    @Test
    fun preservesStaticMethodArguments(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(true),
        before = """
            import static java.util.Collections.*;
            
            class Test {
                Object[] o = new Object[] { emptyList(), emptyMap(), emptySet() };
            }
        """
    )

    @Test
    fun preservesDifferentStaticImportsFromSamePackage(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptyMap;
            import static java.util.GregorianCalendar.getAvailableCalendarTypes;
            import static java.util.GregorianCalendar.getAvailableLocales;
        """
    )

    @Test
    fun collapsesDifferentStaticImportsFromSamePackage(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptyMap;
            import static java.util.Collections.emptySet;
            import static java.util.GregorianCalendar.getAvailableCalendarTypes;
            import static java.util.GregorianCalendar.getAvailableLocales;
            import static java.util.GregorianCalendar.getInstance;
        """,
        after = """
            import static java.util.Collections.*;
            import static java.util.GregorianCalendar.*;
        """
    )

    @Test
    fun removesRedundantImports(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;
            import java.util.List;
        """,
        after = """
            import java.util.List;
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
        before = """
            import org.openrewrite.java.J.CompilationUnit;
            import org.openrewrite.java.J;
            import org.openrewrite.java.JavaVisitor;
            import org.openrewrite.java.JavaPrinter;
            import org.openrewrite.java.ChangeMethodName;
            import org.openrewrite.java.ChangeType;
        """,
        after = """
            import org.openrewrite.java.*;
            import org.openrewrite.java.J.CompilationUnit;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/733")
    @Test
    fun detectBlockPattern(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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
    fun orderAndDoNotFoldStaticClasses(jp: JavaParser) = assertChanged(
        jp,
        recipe = OrderImports(false),
        before = """
            import static org.openrewrite.java.J.CompilationUnit.*;
            import static org.openrewrite.java.J.ClassDeclaration.*;
            import static org.openrewrite.java.J.MethodInvocation.*;
            import static org.openrewrite.java.J.MethodDeclaration.*;
            import static org.openrewrite.java.J.If.*;
        """,
        after = """
            import static org.openrewrite.java.J.ClassDeclaration.*;
            import static org.openrewrite.java.J.CompilationUnit.*;
            import static org.openrewrite.java.J.If.*;
            import static org.openrewrite.java.J.MethodDeclaration.*;
            import static org.openrewrite.java.J.MethodInvocation.*;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/860")
    @Test
    fun orderAndDoNotFoldImports(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.aws.List;
            import java.awt.Panel;
            import java.util.Collection;
            import java.util.List;
            import java.awt.Point;
            import java.awt.Robot;
            import java.util.TreeMap;
            import java.util.Map;
            import java.util.Set;
            import java.awt.Polygon;
        """,
        after = """
            import java.aws.List;
            import java.awt.Panel;
            import java.awt.Point;
            import java.awt.Polygon;
            import java.awt.Robot;
            import java.util.Collection;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            import java.util.TreeMap;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/859")
    @Test
    fun doNotFoldPackageWithJavaLangClassNames(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = OrderImports(false),
        executionContext = executionContext.apply {putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, false)},
        before = """
            import kotlin.DeepRecursiveFunction;
            import kotlin.Function;
            import kotlin.Lazy;
            import kotlin.Pair;
            import kotlin.String;
        """
    )

    @Test
    fun foldPackageWithExistingImports() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*", false)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = OrderImports(false),
        before = """
            import java.util.List;
        """,
        after = """
            import java.util.*;
        """
    )
}
