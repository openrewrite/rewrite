/*
 * Copyright 2021 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("rawtypes")
class AddImportTest implements RewriteTest {

    @DocumentExample
    @Test
    void importIsAddedToCorrectBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.mockito.junit.jupiter.MockitoExtension", null, false))),
          java(
            """
              import java.util.List;

              import org.junit.jupiter.api.extension.ExtendWith;
              import org.mockito.Mock;

              public class MyTest {
              }
              """,
            """
              import java.util.List;

              import org.junit.jupiter.api.extension.ExtendWith;
              import org.mockito.Mock;
              import org.mockito.junit.jupiter.MockitoExtension;

              public class MyTest {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2155")
    @Test
    void addImportBeforeImportWithSameInsertIndex() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.junit.jupiter.api.Assertions", "assertFalse", false))),
          java(
            """
              import static org.junit.jupiter.api.Assertions.assertTrue;

              import org.junit.Test;

              public class MyTest {
              }
              """,
            """
              import static org.junit.jupiter.api.Assertions.assertFalse;
              import static org.junit.jupiter.api.Assertions.assertTrue;

              import org.junit.Test;

              public class MyTest {
              }
              """
          )
        );
    }

    @Test
    void dontDuplicateImports() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new AddImport<>("org.springframework.http.HttpStatus", null, false)),
            toRecipe(() -> new AddImport<>("org.springframework.http.HttpStatus.Series", null, false))
          ),
          java(
            "class A {}",
            """
              import org.springframework.http.HttpStatus;
              import org.springframework.http.HttpStatus.Series;

              class A {}
              """
          )
        );
    }

    @Test
    void dontDuplicateImports2() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.junit.jupiter.api.Test", null, false))),
          java(
            """
              import org.junit.jupiter.api.AfterEach;
              import org.junit.jupiter.api.Assertions;
              import org.junit.jupiter.api.BeforeAll;
              import org.junit.jupiter.api.BeforeEach;
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class A {}
              """,
            """
              import org.junit.jupiter.api.*;
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class A {}
              """
          )
        );
    }

    @Test
    void dontDuplicateImports3() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.junit.jupiter.api.Assertions", "assertNull", false)))
            .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api")),
          java(
            """
              import static org.junit.jupiter.api.Assertions.assertFalse;
              import static org.junit.jupiter.api.Assertions.assertTrue;

              import java.util.List;

              class A {}
              """,
            """
              import static org.junit.jupiter.api.Assertions.*;

              import java.util.List;

              class A {}
              """
          )
        );
    }

    @Test
    void dontImportYourself() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("com.myorg.A", null, false))),
          java(
            """
              package com.myorg;

              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/540")
    @Test
    void forceImportNonJavaLangRecord() {
        // Add import for a class named `Record`, even within the same package, to avoid conflicts with java.lang.Record
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("com.acme.bank.Record", null, false)))
            .parser(JavaParser.fromJavaVersion().dependsOn("package com.acme.bank; public class Record {}")),
          //language=java
          java(
            """
              package com.acme.bank;

              class Foo {
              }
              """,
            """
              package com.acme.bank;

              import com.acme.bank.Record;

              class Foo {
              }
              """,
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/540")
    @Test
    void forceImportNonJavaLangRecordFromWildcardImport() {
        // Add import for a class named `Record`, even within the same package, to avoid conflicts with java.lang.Record
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("com.acme.bank.Record", null, false)))
            .parser(JavaParser.fromJavaVersion().dependsOn("package com.acme.bank; public class Record {}")),
          //language=java
          java(
            """
              package com.acme.bank;

              import com.acme.bank.*;

              class Foo {
              }
              """,
            """
              package com.acme.bank;

              import com.acme.bank.*;

              import com.acme.bank.Record;

              class Foo {
              }
              """,
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/540")
    @Test
    void notForceImportJavaRecord() {
        // Do not add import for java.lang.Record by default
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.lang.Record", null, false))),
          //language=java
          java(
            """
              package com.acme.bank;

              class Foo {
              }
              """,
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @Test
    void dontImportJavaLang() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.lang.String", null, false))),
          java(
            """
              package com.myorg;

              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2809")
    @Test
    void staticImportsFromJavaLangShouldWork() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.lang.System", "lineSeparator", false))),
          java(
            """
              class A {}
              """,
            """
              import static java.lang.System.lineSeparator;

              class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1156")
    @Test
    void dontImportJavaLangWhenUsingDefaultPackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.lang.String", null, false))),
          java(
            """
              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/777")
    @Test
    void dontImportFromSamePackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("com.myorg.B", null, false))),
          java(
            """
              package com.myorg;

              class B {
              }
              """
          ),
          java(
            """
              package com.myorg;

              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/772")
    @Test
    void importOrderingIssue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.springframework.http.HttpHeaders", null, false))),
          java(
            """
              import javax.ws.rs.core.Response.ResponseBuilder;
              import java.util.Locale;

              class A {}
              """,
            """
              import org.springframework.http.HttpHeaders;

              import javax.ws.rs.core.Response.ResponseBuilder;
              import java.util.Locale;

              class A {}
              """
          )
        );
    }

    @Test
    void addMultipleImports() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new AddImport<>("java.util.List", null, false)),
            toRecipe(() -> new AddImport<>("java.util.Set", null, false))
          ),
          java(
            """
              class A {}
              """,
            """
              import java.util.List;
              import java.util.Set;

              class A {}
              """
          )
        );
    }

    @Test
    void addNamedImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java("class A {}",
            """
              import java.util.List;

              class A {}
              """
          )
        );
    }

    @Test
    void doNotAddImportIfNotReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, true))),
          java(
            """
              package a;

              class A {}
              """
          )
        );
    }

    @Test
    void addImportInsertsNewMiddleBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              import com.sun.naming.*;

              import static java.util.Collections.*;

              class A {}
              """,
            """
              package a;

              import com.sun.naming.*;

              import java.util.List;

              import static java.util.Collections.*;

              class A {}
              """
          )
        );
    }

    @Test
    void addFirstImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              class A {}
              """,
            """
              package a;

              import java.util.List;

              class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/484")
    @Test
    void addImportIfReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() ->
            new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                    maybeAddImport("java.math.BigDecimal");
                    maybeAddImport("java.math.RoundingMode");
                    return JavaTemplate.builder("BigDecimal d = BigDecimal.valueOf(1).setScale(1, RoundingMode.HALF_EVEN);")
                      .imports("java.math.BigDecimal", "java.math.RoundingMode")
                      .build()
                      .apply(
                        updateCursor(c),
                        c.getBody().getCoordinates().lastStatement()
                      );
                }
            }
          ).withMaxCycles(1)),
          java(
            """
              package a;

              class A {
              }
              """,
            """
              package a;

              import java.math.BigDecimal;
              import java.math.RoundingMode;

              class A {
                  BigDecimal d = BigDecimal.valueOf(1).setScale(1, RoundingMode.HALF_EVEN);
              }
              """
          )
        );
    }

    @Test
    void doNotAddWildcardImportIfNotReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.*", null, true))),
          java(
            """
              package a;

              class A {}
              """
          )
        );
    }

    @Test
    void lastImportWhenFirstClassDeclarationHasJavadoc() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.Collections", "*", false))),
          java(
            """
              import java.util.List;

              /**
               * My type
               */
              class A {}
              """,
            """
              import java.util.List;

              import static java.util.Collections.*;

              /**
               * My type
               */
              class A {}
              """
          )
        );
    }

    @Test
    void namedImportAddedAfterPackageDeclaration() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;
              class A {}
              """,
            """
              package a;

              import java.util.List;

              class A {}
              """
          )
        );
    }

    @Test
    void importsAddedInAlphabeticalOrder() {
        List<String> otherPackages = List.of("c", "c.c", "c.c.c");
        List<SourceSpecs> otherImports = new ArrayList<>();
        for (int i = 0; i < otherPackages.size(); i++) {
            String pkg = otherPackages.get(i);
            otherImports.add(java("package " + pkg + ";\npublic class C" + i + " {}", SourceSpec::skip));
        }

        List<String> packages = List.of("b", "c.b", "c.c.b");
        for (int order = 0; order < packages.size(); order++) {
            String pkg = packages.get(order);

            List<String> expectedImports = new ArrayList<>();
            for (int i = 0; i < otherPackages.size(); i++) {
                String otherPackage = otherPackages.get(i);
                expectedImports.add(otherPackage + ".C" + i);
            }
            expectedImports.add(order, pkg + ".B");

            List<SourceSpecs> sources = new ArrayList<>(otherImports);
            sources.add(java(
              String.format("""
                    package %s;
                    public class B {}
                """, pkg),
              SourceSpec::skip
            ));

            sources.add(
              java(
                """
                  package a;

                  import c.C0;
                  import c.c.C1;
                  import c.c.c.C2;

                  class A {}
                  """,
                String.format("""
                    package a;

                    %s

                    class A {}
                    """,
                  expectedImports.stream().map(i -> "import " + i + ";").collect(joining("\n"))
                )
              )
            );

            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new AddImport<>(pkg, "B", null, null, false))),
              sources.toArray(new SourceSpecs[0])
            );
        }
    }

    @Test
    void doNotAddImportIfAlreadyExists() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              import java.util.List;
              class A {}
              """
          )
        );
    }

    @Test
    void doNotAddImportIfCoveredByStarImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              import java.util.*;
              class A {}
              """
          )
        );
    }

    @Test
    void dontAddImportWhenClassHasNoPackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("C", null, false))),
          java("class A {}")
        );
    }

    @Test
    void dontAddImportForPrimitive() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("int", null, false))),
          java("class A {}")
        );
    }

    @Test
    void addNamedImportIfStarStaticImportExists() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              import static java.util.List.*;
              class A {}
              """,
            """
              package a;

              import java.util.List;

              import static java.util.List.*;

              class A {}
              """
          )
        );
    }

    @Test
    void addNamedStaticImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.Collections", "emptyList", false))),
          java(
            """
              import java.util.*;
              class A {}
              """,
            """
              import java.util.*;

              import static java.util.Collections.emptyList;

              class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/108")
    @Test
    void addStaticImportForUnreferencedField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("mycompany.Type", "FIELD", false))),
          java(
            """
              package mycompany;

              public class Type {
                  public static String FIELD;
              }
              """
          ),
          java(
            "class A {}",
            """
              import static mycompany.Type.FIELD;

              class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1030")
    @Test
    void addStaticImportForReferencedField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if (!classDecl.getBody().getStatements().isEmpty()) {
                        return classDecl;
                    }

                    maybeAddImport("java.time.temporal.ChronoUnit");
                    maybeAddImport("java.time.temporal.ChronoUnit", "MILLIS");

                    return JavaTemplate.builder("ChronoUnit unit = MILLIS;")
                      .contextSensitive()
                      .imports("java.time.temporal.ChronoUnit")
                      .staticImports("java.time.temporal.ChronoUnit.MILLIS")
                      .build()
                      .apply(getCursor(), classDecl.getBody().getCoordinates().lastStatement());
                }
            }
          )),
          java(
            """
              public class A {

              }
              """,
            """
              import java.time.temporal.ChronoUnit;

              import static java.time.temporal.ChronoUnit.MILLIS;

              public class A {
                  ChronoUnit unit = MILLIS;

              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1030")
    @Test
    void dontAddImportToStaticFieldWithNamespaceConflict() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.time.temporal.ChronoUnit", "MILLIS", true))),
          java(
            """
              package a;

              import java.time.temporal.ChronoUnit;

              class A {
                  static final int MILLIS = 1;
                  ChronoUnit unit = ChronoUnit.MILLIS;
              }
              """
          )
        );
    }

    @Test
    void dontAddStaticWildcardImportIfNotReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.Collections", "*", true))),
          java(
            """
              package a;

              class A {}
              """
          )
        );
    }

    @Test
    void addNamedStaticImportWhenReferenced() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    return method.withSelect(null);
                }
            }),
            toRecipe(() -> new AddImport<>("java.util.Collections", "emptyList", true))
          ),
          java(
            """
              package a;

              import java.util.List;

              class A {
                  public A() {
                      List<String> list = java.util.Collections.emptyList();
                  }
              }
              """,
            """
              package a;

              import java.util.List;

              import static java.util.Collections.emptyList;

              class A {
                  public A() {
                      List<String> list = emptyList();
                  }
              }
              """
          )
        );
    }

    @Test
    void addNamedStaticImportWhenReferenced2() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  method = super.visitMethodDeclaration(method, ctx);
                  method = JavaTemplate.builder("List<Builder> list = new ArrayList<>();")
                    .imports("java.util.ArrayList", "java.util.List")
                    .staticImports("java.util.Calendar.Builder")
                    .build()
                    .apply(getCursor(), method.getBody().getCoordinates().firstStatement());
                  maybeAddImport("java.util.ArrayList");
                  maybeAddImport("java.util.List");
                  maybeAddImport("java.util.Calendar", "Builder");
                  return method;
              }
          }).withMaxCycles(1)),
          java(
            """
              import static java.util.Calendar.Builder;

              class A {
                  public A() {
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;

              import static java.util.Calendar.Builder;

              class A {
                  public A() {
                      List<Builder> list = new ArrayList<>();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotAddNamedStaticImportIfNotReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.Collections", "emptyList", true))),
          java(
            """
              package a;

              class A {}
              """
          )
        );
    }

    @Test
    void addStaticWildcardImportWhenReferenced() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("emptyList".equals(method.getName().getSimpleName())) {
                        return method.withSelect(null);
                    }
                    return method;
                }
            }),
            toRecipe(() -> new AddImport<>("java.util.Collections", "*", true))
          ),
          java(
            """
              package a;

              import java.util.List;

              class A {
                  public A() {
                      List<String> list = java.util.Collections.emptyList();
                  }
              }
              """,
            """
              package a;

              import java.util.List;

              import static java.util.Collections.*;

              class A {
                  public A() {
                      List<String> list = emptyList();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/477")
    @Test
    void dontAddImportForStaticImportsIndirectlyReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  maybeAddImport("java.io.File");
                  return super.visitCompilationUnit(cu, ctx);
              }
          })),
          java(
            """
              import java.io.File;
              class Helper {
                  static File FILE;
              }
              """
          ),
          java(
            """
              class Test {
                  void test() {
                      Helper.FILE.exists();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/776")
    @Test
    void addImportAndFoldIntoWildcard() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.ArrayList", null, false))),
          java(
            """
              package foo;
              public class B {
              }
              public class C {
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import foo.B;
              import foo.C;

              import java.util.Collections;
              import java.util.List;
              import java.util.HashSet;
              import java.util.HashMap;
              import java.util.Map;
              import java.util.Set;

              class A {
                  B b = new B();
                  C c = new C();
                  Map<String, String> map = new HashMap<>();
                  Set<String> set = new HashSet<>();
                  List<String> test = Collections.singletonList("test");
                  List<String> test2 = new java.util.ArrayList<>();
              }
              """,
            """
              import foo.B;
              import foo.C;

              import java.util.*;

              class A {
                  B b = new B();
                  C c = new C();
                  Map<String, String> map = new HashMap<>();
                  Set<String> set = new HashSet<>();
                  List<String> test = Collections.singletonList("test");
                  List<String> test2 = new ArrayList<>();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/780")
    @Test
    void addImportWhenDuplicatesExist() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.springframework.http.MediaType", null, false))),
          java(
            """
              import javax.ws.rs.Path;
              import javax.ws.rs.Path;

              class A {}
              """,
            """
              import org.springframework.http.MediaType;

              import javax.ws.rs.Path;
              import javax.ws.rs.Path;

              class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/933")
    @Test
    void unorderedImportsWithNewBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.time.Duration", null, false))),
          java(
            """
              import org.foo.B;
              import org.foo.A;

              class A {}
              """,
            """
              import org.foo.B;
              import org.foo.A;

              import java.time.Duration;

              class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/880")
    @Test
    void doNotFoldNormalImportWithNamespaceConflict() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false)))
            .beforeRecipe(addTypesToSourceSet("main")),
          srcMainJava(
            java(
              """
                import java.awt.*; // contains a List class
                import java.util.Collection;
                import java.util.Collections;
                import java.util.Map;
                import java.util.Set;

                @SuppressWarnings("ALL")
                class Test {
                    List list;
                }
                """,
              """
                import java.awt.*; // contains a List class
                import java.util.Collection;
                import java.util.Collections;
                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                @SuppressWarnings("ALL")
                class Test {
                    List list;
                }
                """
            )
          )
        );
    }

    private static Consumer<RecipeSpec> importLayout(String type, @Nullable String statik, ImportLayoutStyle.Builder layout) {
        return spec -> spec.recipe(toRecipe(() -> new AddImport<>(type, statik, false)))
          .parser(JavaParser.fromJavaVersion().styles(
            singletonList(new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(), singletonList(layout.build())
            )))
          );
    }

    @Test
    void foldPackageWithEmptyImports() {
        rewriteRun(
          importLayout(
            "java.util.List", null,
            ImportLayoutStyle.builder()
              .packageToFold("java.util.*", false)
              .importAllOthers()
              .importStaticAllOthers()),
          java(
            """
              """,
            """
              import java.util.*;
              """
          )
        );
    }

    @Test
    void foldPackageWithExistingImports
      () {
        rewriteRun(
          importLayout(
            "java.util.Map", null,
            ImportLayoutStyle.builder()
              .packageToFold("java.util.*", false)
              .importAllOthers()
              .importStaticAllOthers()
          ),
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

    @Test
    void foldSubPackageWithExistingImports() {
        rewriteRun(
          importLayout(
            "java.util.concurrent.ConcurrentHashMap", null,
            ImportLayoutStyle.builder()
              .packageToFold("java.util.*", true)
              .importAllOthers()
              .importStaticAllOthers()
          ),
          java(
            """
              import java.util.List;
              """,
            """
              import java.util.List;
              import java.util.concurrent.*;
              """
          )
        );
    }

    @Test
    void foldStaticSubPackageWithEmptyImports() {
        rewriteRun(
          importLayout(
            "java.util.Collections", "emptyMap",
            ImportLayoutStyle.builder()
              .staticPackageToFold("java.util.*", true)
              .importAllOthers()
              .importStaticAllOthers()
          ),
          java(
            """
              """,
            """
              import static java.util.Collections.*;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1687")
    @Test
    void noImportLayout() {
        rewriteRun(
          importLayout(
            "java.util.List", null,
            ImportLayoutStyle.builder()
              .importAllOthers()
              .importStaticAllOthers()
              .classCountToUseStarImport(999)
              .nameCountToUseStarImport(999)
          ),
          java(
            """
              """,
            """
              import java.util.List;
              """
          )
        );
    }

    @Test
    void foldStaticSubPackageWithExistingImports
      () {
        rewriteRun(
          importLayout(
            "java.util.Collections", "emptyMap",
            ImportLayoutStyle.builder()
              .staticPackageToFold("java.util.*", true)
              .importAllOthers()
              .importStaticAllOthers()
          ),
          java(
            """
              import java.util.List;
              """,
            """
              import java.util.List;

              import static java.util.Collections.*;
              """
          )
        );
    }

    @Test
    void crlfNewLinesWithoutPreviousImports() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;
              class A {}
              """.replace("\n", "\r\n"),
            """
              package a;

              import java.util.List;

              class A {}
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void crlfNewLinesWithPreviousImports() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              import java.util.Set;

              class A {}
              """.replace("\n", "\r\n"),
            """
              package a;

              import java.util.List;
              import java.util.Set;

              class A {}
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void crlfNewLinesWithPreviousImportsNoPackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              import java.util.Set;

              class A {}
              """.replace("\n", "\r\n"),
            """
              import java.util.List;
              import java.util.Set;

              class A {}
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void crlfNewLinesWithPreviousImportsNoClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              package a;

              import java.util.Arrays;
              import java.util.Set;
              """.replace("\n", "\r\n"),
            """
              package a;

              import java.util.Arrays;
              import java.util.List;
              import java.util.Set;
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void crlfNewLinesWithPreviousImportsNoPackageNoClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              import java.util.Arrays;
              import java.util.Set;
              """.replace("\n", "\r\n"),
            """
              import java.util.Arrays;
              import java.util.List;
              import java.util.Set;
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void crlfNewLinesInComments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              /*
               * Copyright 2023 the original author or authors.
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
              """.replace("\n", "\r\n") +
              """
                import java.util.Arrays;
                import java.util.Set;
                """,
            """
              /*
               * Copyright 2023 the original author or authors.
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
              import java.util.Arrays;
              import java.util.List;
              import java.util.Set;
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void crlfNewLinesInJavadoc() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          java(
            """
              import java.util.Arrays;
              import java.util.Set;

              """ +
              """
                /**
                 * Copyright 2023 the original author or authors.
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
                """.replace("\n", "\r\n") +
              "class Foo {}",
            """
              import java.util.Arrays;
              import java.util.List;
              import java.util.Set;

              /**
               * Copyright 2023 the original author or authors.
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
              class Foo {}
              """.replace("\n", "\r\n")
          )
        );
    }

    @Test
    void fullyQualifyOnAmbiguousImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block body, ExecutionContext ctx) {
                  maybeAddImport("java.sql.Date");
                  JavaTemplate template = JavaTemplate.builder(
                      "Date sqlDate = new Date(System.currentTimeMillis());")
                    .imports("java.sql.Date")
                    .build();
                  return template.apply(updateCursor(body), body.getCoordinates().firstStatement());
              }
          }).withMaxCycles(1)),
          java(
            """
              import java.util.Date;

              class Ambiguous {
                  Date date = new Date(System.currentTimeMillis());
              }
              """,
            """
              import java.util.Date;

              class Ambiguous {
                  java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
                  Date date = new Date(System.currentTimeMillis());
              }
              """
          )
        );
    }

    @Test
    void fullyQualifyOnAmbiguousStaticFieldImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.Block visitBlock(J.Block body, ExecutionContext ctx) {
                    maybeAddImport("java.awt.Color", "RED", true);
                    maybeAddImport("java.awt.Color", true);
                    JavaTemplate template = JavaTemplate.builder(
                        "Color color = RED;")
                      .imports("java.awt.Color")
                      .staticImports("java.awt.Color.RED")
                      .build();
                    return template.apply(updateCursor(body), body.getCoordinates().firstStatement());
                }
            }).withMaxCycles(1))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package com.example;

                public class CustomColor {
                    public static final String RED = "red";
                }
                """
            )),
          java(
            """
              import static com.example.CustomColor.RED;

              class Ambiguous {
                  void method() {
                      // RED is from com.example.CustomColor
                      System.out.println(RED);
                  }
              }
              """,
            """
              import java.awt.Color;

              import static com.example.CustomColor.RED;

              class Ambiguous {
                  Color color = Color.RED;
                  void method() {
                      // RED is from com.example.CustomColor
                      System.out.println(RED);
                  }
              }
              """
          )
        );
    }

    @Test
    void fullyQualifyOnAmbiguousStaticMethodImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block body, ExecutionContext ctx) {
                  maybeAddImport("java.util.Arrays", "sort", true);
                  maybeAddImport("java.util.List", true);
                  JavaTemplate template = JavaTemplate.builder(
                      "List<Integer> list = sort(new int[]{1, 2, 3});")
                    .imports("java.util.List")
                    .staticImports("java.util.Arrays.sort")
                    .build();
                  return template.apply(updateCursor(body), body.getCoordinates().firstStatement());
              }
          }).withMaxCycles(1)),
          java(
            """
              import static java.util.Collections.sort;

              import java.util.ArrayList;

              class Ambiguous {
                  void method() {
                      sort(new ArrayList<String>());
                  }
              }
              """,
            """
              import static java.util.Collections.sort;

              import java.util.ArrayList;
              import java.util.List;

              class Ambiguous {
                  List<Integer> list = java.util.Arrays.sort(new int[]{1, 2, 3});
                  void method() {
                      sort(new ArrayList<String>());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5530")
    @Test
    void importWithNestedClass() {
        @Language("java") final String auxSource = """
          package com.example;

          public interface A {
              enum DataType {
                  TYPE_1,
                  TYPE_2
              }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.Block visitBlock(J.Block body, ExecutionContext ctx) {
                    JavaTemplate template = JavaTemplate.builder(
                        "DataType d2 = DataType.TYPE_2;")
                      .javaParser(JavaParser.fromJavaVersion().dependsOn(auxSource))
                      .imports("com.example.A.DataType")
                      .build();
                    maybeAddImport("com.example.A.DataType");
                    return template.apply(updateCursor(body), body.getCoordinates().firstStatement());
                }
            }).withMaxCycles(1))
            .parser(JavaParser.fromJavaVersion().dependsOn(auxSource)),
          java(
            """
              import com.example.A;
              import com.example.A.DataType;

              class Test {
                  DataType d1 = DataType.TYPE_1;
                  A a;
              }
              """,
            """
              import com.example.A;
              import com.example.A.DataType;

              class Test {
                  DataType d2 = DataType.TYPE_2;
                  DataType d1 = DataType.TYPE_1;
                  A a;
              }
              """
          )
        );
    }

    @Test
    void codeSanityCheck() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.ex.app.config;
              public class OldA {}
              """,
            """
              package com.ex.app.task;
              public class OldB {}
              """,
            """
              package com.ex.app.task;
              public class OldC {}
              """,
            """
              package com.ex.app.task;
              public class OldD {}
              """,
            """
              package com.ex.app.task;
              public class OldE {}
              """
          )).recipes(
            new ChangeType("com.ex.app.config.OldA", "com.ex.app.config.NewA", null),
            new ChangeType("com.ex.app.task.OldB", "com.ex.app.task.NewB", null),
            new ChangeType("com.ex.app.task.OldC", "com.ex.app.task.NewC", null),
            new ChangeType("com.ex.app.task.OldD", "com.ex.app.task.NewD", null),
            new ChangeType("com.ex.app.task.OldE", "com.ex.app.task.NewE", null)
          ),
          java(
            """
              package sample;

              import com.ex.app.config.OldA;
              import com.ex.app.task.OldB;
              import com.ex.app.task.OldC;
              import com.ex.app.task.OldD;
              import com.ex.app.task.OldE;

              public class A {
                  private final OldA a;
                  private final OldB b;
                  private final OldC c;
                  private final OldD d;
                  private final OldE e;

                  public A(OldA a, OldB b, OldC c, OldD d, OldE e) {
                      this.a = a;
                      this.b = b;
                      this.c = c;
                      this.d = d;
                      this.e = e;
                  }
              }
              """,
            """
          package sample;

          import com.ex.app.config.NewA;
          import com.ex.app.task.NewB;
          import com.ex.app.task.NewC;
          import com.ex.app.task.NewD;
          import com.ex.app.task.NewE;

          public class A {
              private final NewA a;
              private final NewB b;
              private final NewC c;
              private final NewD d;
              private final NewE e;

              public A(NewA a, NewB b, NewC c, NewD d, NewE e) {
                  this.a = a;
                  this.b = b;
                  this.c = c;
                  this.d = d;
                  this.e = e;
              }
          }
          """
          )
        );
    }

}
