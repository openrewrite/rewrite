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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.addTypesToSourceSet;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.version;

class OrderImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderImports(false));
    }

    @Test
    void sortInnerAndOuterClassesInTheSamePackage() {
        rewriteRun(
          java("class Test {}")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/259")
    @Test
    void multipleClassesWithTheSameNameButDifferentPackages() {
        rewriteRun(
          java(
            """
              import java.awt.List;
              import java.util.List;
                            
              class Test {}
              """
          )
        );
    }

    @DocumentExample
    @Test
    void foldIntoStar() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.ArrayList;
              import java.util.regex.Pattern;
              import java.util.Objects;
              import java.util.Set;
              import java.util.Map;
              """,
            """
              import java.util.*;
              import java.util.regex.Pattern;
              """
          )
        );
    }

    @Test
    void blankLinesNotFollowedByBlockArentAdded() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
              import static java.util.Collections.*;
                            
              class A {}
              """
          )
        );
    }

    @Test
    void foldIntoExistingStar() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.ArrayList;
              import java.util.regex.Pattern;
              import java.util.Objects;
              """,
            """
              import java.util.*;
              import java.util.regex.Pattern;
              """
          )
        );
    }

    @Test
    void idempotence() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.regex.Pattern;
              """
          )
        );
    }

    @Test
    void unfoldStar() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              import java.util.*;
                            
              class A {
                  List<Integer> list;
                  List<Integer> list2;
              }
              """,
            """
              import java.util.List;
                            
              class A {
                  List<Integer> list;
                  List<Integer> list2;
              }
              """
          )
        );
    }

    @Test
    void unfoldStarMultiple() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              import java.util.*;
                            
              class A {
                  List<Integer> list;
                  Map<Integer, Integer> map;
              }
              """,
            """
              import java.util.List;
              import java.util.Map;
                            
              class A {
                  List<Integer> list;
                  Map<Integer, Integer> map;
              }
              """
          )
        );
    }

    @Test
    void removeUnused() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              import java.util.*;
              """,
            "")
        );
    }

    @Test
    void unfoldStaticStar() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              import java.util.List;
                            
              import static java.util.Collections.*;
                            
              class A {
                  List<Integer> list = emptyList();
              }
              """,
            """
              import java.util.List;
                            
              import static java.util.Collections.emptyList;
                            
              class A {
                  List<Integer> list = emptyList();
              }
              """
          )
        );
    }

    @Test
    void packagePatternEscapesDots() {
        rewriteRun(
          java(
            """
              import javax.annotation.Nonnull;
              """
          )
        );
    }

    @Test
    void twoImportsFollowedByStar() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.io.UncheckedIOException;
              import java.nio.files.*;
              """
          )
        );
    }

    @Test
    void springCloudFormat() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                randomId(), "spring", "spring", "spring", emptySet(), singletonList(
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
          )),
          java(
            """
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
        );
    }

    @Test
    void importSorting() {
        rewriteRun(
          java(
            """
              import r.core.Flux;
              import s.core.Flux;
              import com.fasterxml.jackson.databind.ObjectMapper;
              import org.apache.commons.logging.Log;
              import reactor.core.publisher.Mono;
              """,
            """
              import com.fasterxml.jackson.databind.ObjectMapper;
              import org.apache.commons.logging.Log;
              import r.core.Flux;
              import reactor.core.publisher.Mono;
              import s.core.Flux;
              """
          )
        );
    }

    @Test
    void foldGroupOfStaticImportsThatAppearLast() {
        rewriteRun(
          java(
            """
              import static java.util.stream.Collectors.toList;
              import static java.util.stream.Collectors.toMap;
              import static java.util.stream.Collectors.toSet;
              """,
            """
              import static java.util.stream.Collectors.*;
              """
          )
        );
    }

    @Test
    void preservesStaticStarImportWhenRemovingUnused() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              import static java.util.Collections.*;
                            
              class Test {
                  Object[] o = new Object[] { emptyList(), emptyMap(), emptySet() };
              }
              """
          )
        );
    }

    @Test
    void preservesStaticInheritanceImport() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              package my;
              public class MyCollections extends java.util.Collections {}
              """
          ),
          java(
            """
              import static my.MyCollections.*;
                            
              class Test {
                  Object[] o = new Object[] { emptyList(), emptyMap(), emptySet() };
              }
              """
          )
        );
    }

    @Test
    void preservesStaticMethodArguments() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          java(
            """
              import static java.util.Collections.*;
                            
              class Test {
                  Object[] o = new Object[] { emptyList(), emptyMap(), emptySet() };
              }
              """
          )
        );
    }


    @Test
    void preservesDifferentStaticImportsFromSamePackage() {
        rewriteRun(
          java(
            """
              import static java.util.Collections.emptyList;
              import static java.util.Collections.emptyMap;
              import static java.util.GregorianCalendar.getAvailableCalendarTypes;
              import static java.util.GregorianCalendar.getAvailableLocales;
              """
          )
        );
    }

    @Test
    void collapsesDifferentStaticImportsFromSamePackage() {
        rewriteRun(
          java(
            """
              import static java.util.Collections.emptyList;
              import static java.util.Collections.emptyMap;
              import static java.util.Collections.emptySet;
              import static java.util.GregorianCalendar.getAvailableCalendarTypes;
              import static java.util.GregorianCalendar.getAvailableLocales;
              import static java.util.GregorianCalendar.getInstance;
              """,
            """
              import static java.util.Collections.*;
              import static java.util.GregorianCalendar.*;
              """
          )
        );
    }

    @Test
    void removesRedundantImports() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.List;
              """,
            """
              import java.util.List;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/474")
    @Test
    void blankLinesBetweenImports() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                randomId(),
                "custom",
                "custom style",
                null,
                emptySet(),
                singletonList(
                  ImportLayoutStyle.builder()
                    .classCountToUseStarImport(9999)
                    .nameCountToUseStarImport(9999)
                    .importStaticAllOthers()
                    .blankLine()
                    .importAllOthers()
                    .blankLine()
                    .build()
                )
              )
            ))),
          java(
            """
              import java.util.List;
              import static java.util.Collections.singletonList;
              class Test {
              }
              """,
            """
              import static java.util.Collections.singletonList;
                            
              import java.util.List;
                            
              class Test {
              }
              """
          )
        );
    }

    @Issue("#352")
    @Test
    void groupImportsIsAwareOfNestedClasses() {
        rewriteRun(
          java(
            """
              import org.openrewrite.java.J.CompilationUnit;
              import org.openrewrite.java.J;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.JavaPrinter;
              import org.openrewrite.java.ChangeMethodName;
              import org.openrewrite.java.ChangeType;
              """,
            """
              import org.openrewrite.java.*;
              import org.openrewrite.java.J.CompilationUnit;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/733")
    @Test
    void detectBlockPattern() {
        rewriteRun(
          java(
            """
              // org.slf4j should be detected as a block pattern, and not be moved to all other imports.
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;
                            
              import java.util.Arrays;
              import java.util.List;
                            
              public class C {
              }
              """
          )
        );
    }

    @Test
    void doNotFoldImports() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                randomId(),
                "custom",
                "custom style",
                null,
                emptySet(),
                singletonList(
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
                    .build()
                )
              )
            ))),
          java(
            """
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
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/860")
    @Test
    void orderAndDoNotFoldStaticClasses() {
        rewriteRun(
          java(
            """
              import static org.openrewrite.java.J.CompilationUnit.*;
              import static org.openrewrite.java.J.ClassDeclaration.*;
              import static org.openrewrite.java.J.MethodInvocation.*;
              import static org.openrewrite.java.J.MethodDeclaration.*;
              import static org.openrewrite.java.J.If.*;
              """,
            """
              import static org.openrewrite.java.J.ClassDeclaration.*;
              import static org.openrewrite.java.J.CompilationUnit.*;
              import static org.openrewrite.java.J.If.*;
              import static org.openrewrite.java.J.MethodDeclaration.*;
              import static org.openrewrite.java.J.MethodInvocation.*;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/860")
    @Test
    void orderAndDoNotFoldImports() {
        rewriteRun(
          java(
            """
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
            """
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
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2965")
    @Test
    void importReferencedByRecordComponentOnly() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true)),
          version(java(
            """
              import java.util.List;
              import java.util.UUID;
              
              record T(List<UUID> uuids) {
              }
              """
          ), 17)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/859")
    @Test
    void doNotFoldPackageWithJavaLangClassNames() {
        rewriteRun(
          spec -> spec.beforeRecipe(addTypesToSourceSet("main")),
          srcMainJava(
            java(
              """
                import kotlin.DeepRecursiveFunction;
                import kotlin.Function;
                import kotlin.Lazy;
                import kotlin.Pair;
                import kotlin.String;
                """
            )
          )
        );
    }

    @Test
    void foldPackageWithExistingImports() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                randomId(), "test", "test", "test", emptySet(), singletonList(
                  ImportLayoutStyle.builder()
                    .packageToFold("java.util.*", false)
                    .importAllOthers()
                    .importStaticAllOthers()
                    .build()
                )
              )
            ))),
          java(
            """
              import java.util.List;
              """,
            """
              import java.util.*;
              """
          )
        );
    }
}
