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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.addTypesToSourceSet;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("rawtypes")
class AddImportTest implements RewriteTest {

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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1156")
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
        List<String> otherPackages = Arrays.asList("c", "c.c", "c.c.c");
        List<SourceSpecs> otherImports = new ArrayList<>();
        for (int i = 0; i < otherPackages.size(); i++) {
            String pkg = otherPackages.get(i);
            otherImports.add(java("package " + pkg + ";\npublic class C" + i + " {}", SourceSpec::skip));
        }

        List<String> packages = Arrays.asList("b", "c.b", "c.c.b");
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
                  expectedImports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"))
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
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
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
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                    if (method.getName().getSimpleName().equals("emptyList")) {
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
                  List<String> test2 = new java.util.ArrayList<>();
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
}
