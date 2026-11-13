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

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.markSourceSetDirty;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.withSourceTypesOnClasspath;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

@SuppressWarnings("ConstantConditions")
class ChangeTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("a.A1", "a.A2", true));
    }

    @Language("java")
    String a1 = """
      package a;
      public class A1 extends Exception {
          public static void stat() {}
          public void foo() {}
      }
      """;

    @Language("java")
    String a2 = """
      package a;
      public class A2 extends Exception {
          public static void stat() {}
          public void foo() {}
      }
      """;

    @DocumentExample
    @Test
    void doNotAddJavaLangWrapperImports() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.lang.Integer", "java.lang.Long", true)),
          java(
            "public class ThinkPositive { private Integer fred = 1;}",
            "public class ThinkPositive { private Long fred = 1;}"
          )
        );
    }

    @Test
    void okWithTopLevelType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.Map$Entry", "java.util.List", true)),
          java(
            """
              import java.util.Map;

              public class TestController {

                  public Map.Entry respond() {
                     return null;
                  }
              }
              """,
            """
              import java.util.List;

              public class TestController {

                  public List respond() {
                     return null;
                  }
              }
              """
          )
        );
    }

    @Disabled("A bug fix reported by the community but not yet fixed")
    @Test
    void conflictingImports() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "java.util.List", "kotlin.collections.Set", true)),
          java(
            """
              import java.util.*;
              class Test {
                  Set<String> s;
                  List<String> s;
              }
              """,
            """
              import java.util.*;
              class Test {
                  Set<String> s;
                  kotlin.collections.Set<String> s;
              }
              """
          )
        );
    }

    @SuppressWarnings({"deprecation", "KotlinRedundantDiagnosticSuppress"})
    @Test
    void starImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.logging.LoggingMXBean", "java.lang.management.PlatformLoggingMXBean", true)),
          java(
            """
              import java.util.logging.*;

              class Test {
                  static void method() {
                      LoggingMXBean loggingBean = null;
                  }
              }
              """,
            """
              import java.lang.management.PlatformLoggingMXBean;

              class Test {
                  static void method() {
                      PlatformLoggingMXBean loggingBean = null;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"deprecation", "KotlinRedundantDiagnosticSuppress"})
    @Test
    void starImportUnfoldedWhenOtherTypesUsed() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.logging.LoggingMXBean", "java.lang.management.PlatformLoggingMXBean", true)),
          java(
            """
              import java.util.logging.*;

              class Test {
                  static void method() {
                      LoggingMXBean loggingBean = null;
                      Logger logger = null;
                  }
              }
              """,
            """
              import java.lang.management.PlatformLoggingMXBean;
              import java.util.logging.Logger;

              class Test {
                  static void method() {
                      PlatformLoggingMXBean loggingBean = null;
                      Logger logger = null;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"deprecation", "KotlinRedundantDiagnosticSuppress"})
    @Test
    void allowJavaLangSubpackages() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.logging.LoggingMXBean", "java.lang.management.PlatformLoggingMXBean", true)),
          java(
            """
              import java.util.logging.LoggingMXBean;

              class Test {
                  static void method() {
                      LoggingMXBean loggingBean = null;
                  }
              }
              """,
            """
              import java.lang.management.PlatformLoggingMXBean;

              class Test {
                  static void method() {
                      PlatformLoggingMXBean loggingBean = null;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/788")
    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    void unnecessaryImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("test.Outer.Inner", "java.util.ArrayList", true)),
          java(
            """
              import test.Outer;

              class Test {
                  private Outer p = Outer.of();
                  private Outer p2 = test.Outer.of();
              }
              """
          ),
          java(
            """
              package test;

              public class Outer {
                  public static Outer of() {
                      return new Outer();
                  }

                  public static class Inner {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/868")
    @SuppressWarnings("rawtypes")
    @Test
    void changeInnerClassToOuterClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.Map$Entry", "java.util.List", true)),
          java(
            """
              import java.util.Map;
              import java.util.Map.Entry;

              class Test {
                  Entry p;
                  Map.Entry p2;
                  java.util.Map.Entry p3;
              }
              """,
            """
              import java.util.List;

              class Test {
                  List p;
                  List p2;
                  List p3;
              }
              """
          )
        );
    }

    @Test
    void leaveTheSameNestedNameOfAnotherOuterType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo$Builder", "b.Foo$Builder", true)),
          java("package a; public class Foo { public static class Builder {} }"),
          java("package b; public class Foo { public static class Builder {} }"),
          java("package c; public class Foo { public static class Builder {} }"),
          java(
            """
              import c.Foo;

              class D {
                  a.Foo.Builder x;
                  Foo.Builder y;
              }
              """,
            """
              import c.Foo;

              class D {
                  b.Foo.Builder x;
                  Foo.Builder y;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/768")
    @Test
    void changeStaticFieldAccess() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.io.File", "my.pkg.List", true)),
          java(
            """
              import java.io.File;

              class Test {
                  String p = File.separator;
              }
              """,
            """
              import my.pkg.List;

              class Test {
                  String p = List.separator;
              }
              """
          )
        );
    }

    @Test
    void dontAddImportWhenNoChangesWereMade() {
        rewriteRun(
          java("public class B {}")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/774")
    @SuppressWarnings("rawtypes")
    @Test
    void replaceWithNestedType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.io.File", "java.util.Map$Entry", true)),
          java(
            """
              import java.io.File;

              class Test {
                  File p;
              }
              """,
            """
              import java.util.Map;

              class Test {
                  Map.Entry p;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations p = (J.VariableDeclarations) cu.getClasses().getFirst().getBody()
                  .getStatements().getFirst();
                J.FieldAccess entry = (J.FieldAccess) requireNonNull(p.getTypeExpression());
                assertThat(TypeUtils.isOfClassType(entry.getName().getType(), "java.util.Map$Entry")).isTrue();
                assertThat(TypeUtils.isOfClassType(entry.getTarget().getType(), "java.util.Map")).isTrue();
            })
          )
        );
    }

    @Test
    void qualifiedJavadocReferenceToNestedTargetAddsNoImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old", "b.Outer$Inner", true)),
          java(
            """
              package a;
              public class Old {}
              """,
            SourceSpec::skip
          ),
          java(
            """
              /**
               * {@link a.Old}
               */
              class Qualified {
              }
              """,
            """
              /**
               * {@link b.Outer.Inner}
               */
              class Qualified {
              }
              """
          )
        );
    }

    @Test
    void javadocReferenceQualifiedForAnImportedNameAddsNoImport() {
        rewriteRun(
          // the second recipe imports `b.Outer` wherever it finds a name of that type
          spec -> spec.recipes(
            new ChangeType("a.Old", "b.Outer$Inner", true),
            new ChangeType("d.Other", "b.Outer", true)
          ),
          java("package a; public class Old {}", SourceSpec::skip),
          java("package c; public class Outer { public static class Inner {} }", SourceSpec::skip),
          java("package d; public class Other {}", SourceSpec::skip),
          java(
            """
              import a.Old;
              import c.Outer.Inner;

              /**
               * {@link Old} and {@link d.Other}
               */
              class Test {
                  Inner inner;
              }
              """,
            """
              import c.Outer.Inner;

              /**
               * {@link b.Outer.Inner} and {@link b.Outer}
               */
              class Test {
                  Inner inner;
              }
              """
          )
        );
    }

    @Test
    void nestedTargetKeepsItsOuterClassWhereAnotherTypeHasItsSimpleName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old", "b.Outer$Inner", true)),
          java("package a; public class Old<T> {}", SourceSpec::skip),
          java("package b; public class Outer { public static class Inner<T> {} }", SourceSpec::skip),
          java(
            """
              import a.Old;

              class Test {
                  static class Inner<T> {}
                  Old<String> moved;
                  Inner<String> local;
              }
              """,
            """
              import b.Outer;

              class Test {
                  static class Inner<T> {}
                  Outer.Inner<String> moved;
                  Inner<String> local;
              }
              """
          )
        );
    }

    @Test
    void nestedTargetIsWrittenThroughItsOuterClassHoweverTheOriginalWasWritten() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old", "b.Outer$Inner", true)),
          java("package a; public class Old<T> {}", SourceSpec::skip),
          java("package b; public class Outer { public static class Inner<T> {} }", SourceSpec::skip),
          java(
            """
              import a.Old;

              class Test {
                  Old<String> simple;
                  a.Old<String> qualified;
              }
              """,
            """
              import b.Outer;

              class Test {
                  Outer.Inner<String> simple;
                  Outer.Inner<String> qualified;
              }
              """
          )
        );
    }

    @Test
    void replaceWithNestedTypeQualifyingStaticMember() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old", "a.Holder$New", true)),
          java(
            """
              package a;
              public enum Old { X }
              """
          ),
          java(
            """
              import a.Old;

              class A {
                  Object o = Old.X;
              }
              """,
            """
              import a.Holder;

              class A {
                  Object o = Holder.New.X;
              }
              """
          )
        );
    }

    @Test
    void replaceNestedTypeQualifiedByItsOuterTypeWithNestedType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Serialize$Inclusion", "b.Include$Value", true)),
          java(
            """
              package a;
              public @interface Serialize {
                  enum Inclusion { NON_NULL }
              }
              """
          ),
          java(
            """
              package b;
              public @interface Include {
                  enum Value { NON_NULL }
              }
              """
          ),
          java(
            """
              import a.Serialize;

              class A {
                  Object o = Serialize.Inclusion.NON_NULL;
              }
              """,
            """
              import b.Include;

              class A {
                  Object o = Include.Value.NON_NULL;
              }
              """
          )
        );
    }

    @Test
    void replaceImportedNestedTypeWithNestedType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A.B", "b.X.Y", true)),
          java(
            """
              package a;
              public class A {
                  public static class B {}
              }
              """
          ),
          java(
            """
              import a.A.B;

              class Test {
                  B b;
              }
              """,
            """
              import b.X.Y;

              class Test {
                  Y b;
              }
              """
          )
        );
    }

    @Test
    void moveNestedTypeImportedThroughItsOuterTypeToAnotherOuterType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Strategy.Snake", "a.Strategies.Snake", true)),
          java(
            """
              package a;
              public class Strategy {
                  public static class Snake {}
              }
              """
          ),
          java(
            """
              package a;
              public class Strategies {
                  public static class Snake {}
              }
              """
          ),
          java(
            """
              import a.Strategy;

              class A {
                  Strategy.Snake s = new Strategy.Snake();
              }
              """,
            """
              import a.Strategies;

              class A {
                  Strategies.Snake s = new Strategies.Snake();
              }
              """
          )
        );
    }

    @Test
    void moveNestedTypeKeepsAStarImportOfItsPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Strategy.Snake", "a.Strategies.Snake", true)),
          java("package a; public class Strategy { public static class Snake {} }"),
          java("package a; public class Strategies { public static class Snake {} }"),
          java("package a; public class Other {}"),
          java(
            """
              import a.*;

              class A {
                  Strategy.Snake s = new Strategy.Snake();
                  Other o;
              }
              """,
            """
              import a.*;

              class A {
                  Strategies.Snake s = new Strategies.Snake();
                  Other o;
              }
              """
          )
        );
    }

    @Test
    void moveNestedTypeKeepsAnImportOfItsNewOuterTypeThatTheFileHad() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Strategy.Snake", "a.Strategies.Snake", true)),
          java("package a; public class Strategy { public static class Snake {} }"),
          java("package a; public class Strategies { public static class Snake {} }"),
          java(
            """
              import a.Strategies;
              import a.Strategy;

              class A {
                  Strategy.Snake s = new Strategy.Snake();
              }
              """,
            """
              import a.Strategies;

              class A {
                  Strategies.Snake s = new Strategies.Snake();
              }
              """
          )
        );
    }

    @Test
    void renameNestedTypeWithinItsOuterTypeImportedThroughTheOuterType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Strategy.Snake", "a.Strategy.Python", true)),
          java("package a; public class Strategy { public static class Snake {} public static class Python {} }"),
          java(
            """
              import a.Strategy;

              class A {
                  Strategy.Snake s = new Strategy.Snake();
              }
              """,
            """
              import a.Strategy;

              class A {
                  Strategy.Python s = new Strategy.Python();
              }
              """
          )
        );
    }

    @Test
    void renameNestedTypeWithinItsOuterTypeKeepsAStarImportOfItsPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Strategy.Snake", "a.Strategy.Python", true)),
          java("package a; public class Strategy { public static class Snake {} public static class Python {} }"),
          java("package a; public class Other {}"),
          java(
            """
              import a.*;

              class A {
                  Strategy.Snake s = new Strategy.Snake();
                  Other o;
              }
              """,
            """
              import a.*;

              class A {
                  Strategy.Python s = new Strategy.Python();
                  Other o;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2521")
    @Test
    void replacePrivateNestedType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A.B1", "a.A.B2", false)),
          java(
            """
              package a;

              class A {
                  private static class B1 {}
              }
              """,
            """
              package a;

              class A {
                  private static class B2 {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2521")
    @Test
    void deeplyNestedInnerClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A.B.C", "a.A.B.C2", false)),
          java(
            """
              package a;

              class A {
                  public static class B {
                      public static class C {
                      }
                  }
              }
              """,
            """
              package a;

              class A {
                  public static class B {
                      public static class C2 {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simpleName() {
        rewriteRun(
          java(
            """
              import a.A1;

              public class B extends A1 {}
              """,
            """
              import a.A2;

              public class B extends A2 {}
              """
          ),
          java(a1),
          java(a2)
        );
    }

    @Test
    void fullyQualifiedName() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            "public class B extends a.A1 {}",
            """
              import a.A2;

              public class B extends A2 {}
              """
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.c.A1", "a.b.d.A2", true)),
          java("package a.b.c;\npublic @interface A1 {}"),
          java("package a.b.d;\npublic @interface A2 {}"),
          java(
            """
              @a.b.c.A1 public class B {}
              """,
            """
              import a.b.d.A2;

              @A2 public class B {}
              """
          )
        );
    }

    @Test
    void fullyQualifiedAnnotationOnPackageDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.c.A1", "a.b.d.A2", true)),
          java("package a.b.c;\npublic @interface A1 {}"),
          java("package a.b.d;\npublic @interface A2 {}"),
          java(
            """
              @a.b.c.A1 package foo;
              """,
            """
              @A2 package foo;

              import a.b.d.A2;
              """,
            spec -> spec.path("foo/package-info.java")
          )
        );
    }

    @Test
    void array2() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.Pojo", "com.acme.product.v2.Pojo", true)),
          java(
            """
              package com.acme.product;

              public class Pojo {
              }
              """
          ),
          java(
            """
              package com.acme.project.impl;

              import com.acme.product.Pojo;

              public class UsePojo2 {
                  Pojo[] p;

                  void run() {
                      p[0] = null;
                  }
              }
              """,
            """
              package com.acme.project.impl;

              import com.acme.product.v2.Pojo;

              public class UsePojo2 {
                  Pojo[] p;

                  void run() {
                      p[0] = null;
                  }
              }
              """
          )
        );
    }

    // array types and new arrays
    @Test
    void array() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 A1[] a = new A1[0];
              }
              """,
            """
              import a.A2;

              public class B {
                 A2[] a = new A2[0];
              }
              """
          )
        );
    }

    @Test
    void multiDimensionalArray() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class A {
                  A1[][] multiDimensionalArray;
              }
              """,
            """
              import a.A2;

              public class A {
                  A2[][] multiDimensionalArray;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("a.A1")).isEmpty();
                assertThat(cu.findType("a.A2")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void classDecl() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeType("a.A1", "a.A2", true),
            new ChangeType("I1", "I2", true)
          ),
          java(a1),
          java(a2),
          java("public interface I1 {}"),
          java("public interface I2 {}"),
          java(
            """
              import a.A1;

              public class B extends A1 implements I1 {}
              """,
            """
              import a.A2;

              public class B extends A2 implements I2 {}
              """
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void method() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 public A1 foo() throws A1 { return null; }
              }
              """,
            """
              import a.A2;

              public class B {
                 public A2 foo() throws A2 { return null; }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationTypeParametersAndWildcard() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 public <T extends A1> T generic(T n, java.util.List<? super A1> in) {

                 }
                 public void test() {
                     A1.stat();
                     this.<A1>generic(null, java.util.List.of());
                 }
              }
              """,
            """
              import a.A2;

              public class B {
                 public <T extends A2> T generic(T n, java.util.List<? super A2> in) {

                 }
                 public void test() {
                     A2.stat();
                     this.<A2>generic(null, java.util.List.of());
                 }
              }
              """
          )
        );
    }

    @Test
    void typeVariableBoundThatRefersToTheVariable() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Node", "a.Node2", true))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package a;
                public interface Node<T extends Node<T>> {
                }
                """,
              """
                package a;
                public interface Node2<T extends Node2<T>> {
                }
                """
            )),
          java(
            """
              import a.Node;

              import java.util.List;

              class Test {
                  <T extends Node<T>> void sort(List<T> nodes) {
                  }
              }
              """,
            """
              import a.Node2;

              import java.util.List;

              class Test {
                  <T extends Node2<T>> void sort(List<T> nodes) {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"EmptyTryBlock", "CatchMayIgnoreException"})
    @Test
    void multiCatch() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 public void test() {
                     try {}
                     catch(A1 | RuntimeException e) {}
                 }
              }
              """,
            """
              import a.A2;

              public class B {
                 public void test() {
                     try {}
                     catch(A2 | RuntimeException e) {}
                 }
              }
              """
          )
        );
    }

    @Test
    void multiVariable() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 A1 f1, f2;
              }
              """,
            """
              import a.A2;

              public class B {
                 A2 f1, f2;
              }
              """
          )
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 A1 a = new A1();
              }
              """,
            """
              import a.A2;

              public class B {
                 A2 a = new A2();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/704")
    @SuppressWarnings("UnnecessaryLocalVariable")
    @Test
    void updateAssignments() {
        //noinspection UnnecessaryLocalVariable
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              class B {
                  void method(A1 param) {
                      A1 a = param;
                  }
              }
              """,
            """
              import a.A2;

              class B {
                  void method(A2 param) {
                      A2 a = param;
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterizedType() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              import java.util.Map;

              public class B {
                 Map<A1, A1> m;
              }
              """,
            """
              import a.A2;

              import java.util.Map;

              public class B {
                 Map<A2, A2> m;
              }
              """
          )
        );
    }

    @Test
    void typeVariableBoundedByItselfKeepsItsType() {
        AtomicReference<JavaType> before = new AtomicReference<>();
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              import java.util.List;

              class B {
                  A1 a;

                  <T extends Comparable<? super T>> void sort(List<T> list) {
                  }
              }
              """,
            """
              import a.A2;

              import java.util.List;

              class B {
                  A2 a;

                  <T extends Comparable<? super T>> void sort(List<T> list) {
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu -> before.set(firstParameterType(cu)))
              .afterRecipe(cu -> assertThat(firstParameterType(cu)).isSameAs(before.get()))
          )
        );
    }

    private static @Nullable JavaType firstParameterType(J.CompilationUnit cu) {
        J.MethodDeclaration method = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().get(1);
        return ((J.VariableDeclarations) method.getParameters().getFirst()).getType();
    }

    @Test
    void qualifiedNameWithoutTypeIsMatchedByItsText() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo$Builder", "b.Foo$Builder", true))
            // neither a.Foo nor b.Foo is on the classpath, so nothing in D has a type
            .typeValidationOptions(TypeValidation.none()),
          java(
            """
              import a.Foo;
              import a.Foo.Builder;

              class D {
                  Builder x;
                  Foo.Builder y;
              }
              """,
            """
              import b.Foo;
              import b.Foo.Builder;

              class D {
                  Builder x;
                  Foo.Builder y;
              }
              """
          )
        );
    }

    @Test
    void qualifiedNameWithoutTypeReplacedByTopLevelType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo$Builder", "b.Target", true))
            // a.Foo is not on the classpath, so `Foo.Builder` has no type
            .typeValidationOptions(TypeValidation.none())
            .afterTypeValidationOptions(TypeValidation.none().identifiers(true)),
          java(
            """
              import a.Foo;
              import a.Foo.Builder;

              class D {
                  Foo.Builder y;
              }
              """,
            """
              import b.Target;

              class D {
                  Target y;
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsWithinTypeArguments() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              import java.util.List;

              class B {
                  List<List<A1>> nested;
                  List<? extends List<A1>> bound;
                  List<A1[]> array;
              }
              """,
            """
              import a.A2;

              import java.util.List;

              class B {
                  List<List<A2>> nested;
                  List<? extends List<A2>> bound;
                  List<A2[]> array;
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantCast")
    @Test
    void typeCast() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 A1 a = (A1) null;
              }
              """,
            """
              import a.A2;

              public class B {
                 A2 a = (A2) null;
              }
              """
          )
        );
    }

    @Test
    void classReference() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class A {
                  Class<?> clazz = A1.class;
              }
              """,
            """
              import a.A2;

              public class A {
                  Class<?> clazz = A2.class;
              }
              """
          )
        );
    }

    @Test
    void methodSelect() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class B {
                 A1 a = null;
                 public void test() { a.foo(); }
              }
              """,
            """
              import a.A2;

              public class B {
                 A2 a = null;
                 public void test() { a.foo(); }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2302")
    @Test
    void staticImport() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import static a.A1.stat;

              public class B {
                  public void test() {
                      stat();
                  }
              }
              """,
            """
              import static a.A2.stat;

              public class B {
                  public void test() {
                      stat();
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImportsNextToImportOfTheType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old", "b.New", true)),
          java("package a; public class Old { public static Old of() { return null; } public static final int MAX = 1; }", SourceSpec::skip),
          java("package b; public class New { public static New of() { return null; } public static final int MAX = 1; }", SourceSpec::skip),
          java(
            """
              import a.Old;

              import static a.Old.MAX;
              import static a.Old.of;

              class Test {
                  Old method = of();
                  int field = MAX;
              }
              """,
            """
              import b.New;

              import static b.New.MAX;
              import static b.New.of;

              class Test {
                  New method = of();
                  int field = MAX;
              }
              """
          )
        );
    }

    @Test
    void declarationKeepsItsNameWhereAMoveKeepsTheSimpleNameAndTheFileHasAnother() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo", "b.Foo", false)),
          java(
            """
              package a;

              public class Foo {
                  <Foo> void m(Foo t) {}
              }
              """,
            """
              package b;

              public class Foo {
                  <Foo> void m(Foo t) {}
              }
              """,
            spec -> spec.path("a/Foo.java")
          )
        );
    }

    @Test
    void staticImportOfAConstantOfANestedTarget() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old$Typing", "b.New$Typing", true)),
          java("package a; public class Old { public enum Typing { STATIC } }", SourceSpec::skip),
          java("package b; public class New { public enum Typing { STATIC } }", SourceSpec::skip),
          java(
            """
              import static a.Old.Typing.STATIC;

              class Test {
                  Object typing = STATIC;
              }
              """,
            """
              import static b.New.Typing.STATIC;

              class Test {
                  Object typing = STATIC;
              }
              """
          ),
          java(
            """
              import a.Old.Typing;

              import static a.Old.Typing.STATIC;

              class AlsoImportsTheType {
                  Typing typing = STATIC;
              }
              """,
            """
              import b.New;

              import static b.New.Typing.STATIC;

              class AlsoImportsTheType {
                  New.Typing typing = STATIC;
              }
              """
          )
        );
    }

    @Test
    void staticStarImportNextToImportOfTheType() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              import static a.A1.*;

              public class B {
                  public void test() {
                      new A1().foo();
                      stat();
                  }
              }
              """,
            """
              import a.A2;

              import static a.A2.*;

              public class B {
                  public void test() {
                      new A2().foo();
                      stat();
                  }
              }
              """
          )
        );
    }

    @Test
    void staticStarImportNextToUnusedImportOfTheType() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              import static a.A1.*;

              public class B {
                  public void test() {
                      stat();
                  }
              }
              """,
            """
              import static a.A2.*;

              public class B {
                  public void test() {
                      stat();
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImports2() {
        rewriteRun(spec -> spec.recipe(new ChangeType("com.acme.product.RunnableFactory", "com.acme.product.v2.RunnableFactory", true)),
          java(
            """
              package com.acme.product;

              public class RunnableFactory {
                  public static String getString() {
                      return "hello";
                  }
              }
              """
          ),
          java(
            """
              package com.acme.project.impl;

              import static com.acme.product.RunnableFactory.getString;

              public class StaticImportWorker {
                  public void work() {
                      getString().toLowerCase();
                  }
              }
              """,
            """
              package com.acme.project.impl;

              import static com.acme.product.v2.RunnableFactory.getString;

              public class StaticImportWorker {
                  public void work() {
                      getString().toLowerCase();
                  }
              }
              """
          )
        );
    }

    @Test
    void staticConstant() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.RunnableFactory", "com.acme.product.v2.RunnableFactory", true)),
          java(
            """
              package com.acme.product;

              public class RunnableFactory {
                  public static final String CONSTANT = "hello";
              }
              """
          ),
          java(
            """
              package com.acme.project.impl;

              import static com.acme.product.RunnableFactory.CONSTANT;

              public class StaticImportWorker {
                  public void work() {
                      System.out.println(CONSTANT + " fred.");
                  }
              }
              """,
            """
              package com.acme.project.impl;

              import static com.acme.product.v2.RunnableFactory.CONSTANT;

              public class StaticImportWorker {
                  public void work() {
                      System.out.println(CONSTANT + " fred.");
                  }
              }
              """
          )
        );
    }

    @Disabled("https://github.com/openrewrite/rewrite/issues/62")
    @Test
    void primitiveToClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("int", "java.lang.Integer", true)),
          java(
            """
              class A {
                  int foo = 5;
                  int getFoo() {
                      return foo;
                  }
              }
              """,
            """
              class A {
                  Integer foo = 5;
                  Integer getFoo() {
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void classToPrimitive() {
        rewriteRun(
          // ChangeType keeps the original class as the type of what it changes to a primitive or `String`
          spec -> spec.recipe(new ChangeType("java.lang.Integer", "int", true))
            .recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build()),
          java(
            """
              class A {
                  Integer foo = 5;
                  Integer getFoo() {
                      return foo;
                  }
              }
              """,
            """
              class A {
                  int foo = 5;
                  int getFoo() {
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void classToStringKeyword() {
        rewriteRun(
          // ChangeType keeps the original class as the type of what it changes to a primitive or `String`
          spec -> spec.recipe(new ChangeType("java.lang.CharSequence", "String", true))
            .recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build()),
          java(
            """
              class A {
                  CharSequence foo = "";
              }
              """,
            """
              class A {
                  String foo = "";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/698")
    @Test
    void importOrdering() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.yourorg.a.A", "com.myorg.b.B", true)),
          java(
            """
              package com.yourorg.a;
              public class A {}
              """
          ),
          java(
            """
              package com.myorg.b;
              public class B {}
              """
          ),
          java(
            """
              package com.myorg;

              import java.util.ArrayList;
              import com.yourorg.a.A;
              import java.util.List;

              public class Foo {
                  List<A> a = new ArrayList<>();
              }
              """,
            """
              package com.myorg;

              import com.myorg.b.B;

              import java.util.ArrayList;
              import java.util.List;

              public class Foo {
                  List<B> a = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void changeTypeWithInnerClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.OuterClass", "com.acme.product.v2.OuterClass", true)),
          java(
            """
              package com.acme.product;

              public class OuterClass {
                  public static class InnerClass {

                  }
              }
              """
          ),
          java(
            """
              package de;

              import com.acme.product.OuterClass;
              import com.acme.product.OuterClass.InnerClass;

              public class UseInnerClass {
                  public String work() {
                      return new InnerClass().toString();
                  }

                  public String work2() {
                      return new OuterClass().toString();
                  }
              }
              """,
            """
              package de;

              import com.acme.product.v2.OuterClass;
              import com.acme.product.v2.OuterClass.InnerClass;

              public class UseInnerClass {
                  public String work() {
                      return new InnerClass().toString();
                  }

                  public String work2() {
                      return new OuterClass().toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void fileThatOnlyImportsANestedTypeOfTheOldType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.OuterClass", "com.acme.product.v2.OuterClass", true)),
          java("package com.acme.product; public class OuterClass { public static class InnerClass {} }"),
          java("package com.acme.product.v2; public class OuterClass { public static class InnerClass {} }"),
          java(
            """
              import com.acme.product.OuterClass.InnerClass;

              class A {
                  InnerClass i = new InnerClass();
              }
              """,
            """
              import com.acme.product.v2.OuterClass.InnerClass;

              class A {
                  InnerClass i = new InnerClass();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/925")
    @Test
    void uppercaseInPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", true)),
          java(
            """
              package com.acme.product.util.accessDecision;

              public enum AccessVote {
                  ABSTAIN
              }
              """
          ),
          java(
            """
              package de;

              import com.acme.product.util.accessDecision.AccessVote;

              public class ProjectVoter {
                  public AccessVote vote() {
                      return AccessVote.ABSTAIN;
                  }
              }
              """,
            """
              package de;

              import com.acme.product.v2.util.accessDecision.AccessVote;

              public class ProjectVoter {
                  public AccessVote vote() {
                      return AccessVote.ABSTAIN;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/934")
    @Test
    void lambda() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.Procedure", "com.acme.product.Procedure2", true)),
          java(
            """
              package com.acme.product;
              public interface Procedure {
                  void execute();
              }
              """
          ),
          java(
            """
              import com.acme.product.Procedure;

              public abstract class Worker {
                  void callWorker() {
                      worker(() -> {
                      });
                  }
                  abstract void worker(Procedure callback);
              }
              """,
            """
              import com.acme.product.Procedure2;

              public abstract class Worker {
                  void callWorker() {
                      worker(() -> {
                      });
                  }
                  abstract void worker(Procedure2 callback);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/932")
    @Test
    void assignment() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", true)),
          java(
            """
              package com.acme.product.util.accessDecision;

              public enum AccessVote {
                  ABSTAIN,
                  GRANT
              }
              """
          ),
          java(
            """
              package de;

              import com.acme.product.util.accessDecision.AccessVote;

              public class ProjectVoter {
                  public AccessVote vote(Object input) {
                      AccessVote fred;
                      fred = (AccessVote) input;
                      return fred;
                  }
              }
              """,
            """
              package de;

              import com.acme.product.v2.util.accessDecision.AccessVote;

              public class ProjectVoter {
                  public AccessVote vote(Object input) {
                      AccessVote fred;
                      fred = (AccessVote) input;
                      return fred;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/932")
    @Test
    void ternary() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", true)),
          java(
            """
              package com.acme.product.util.accessDecision;

              public enum AccessVote {
                  ABSTAIN,
                  GRANT
              }
              """
          ),
          java(
            """
              package de;

              import com.acme.product.util.accessDecision.AccessVote;

              public class ProjectVoter {
                  public AccessVote vote(Object input) {
                      return input == null ? AccessVote.GRANT : AccessVote.ABSTAIN;
                  }
              }
              """,
            """
              package de;

              import com.acme.product.v2.util.accessDecision.AccessVote;

              public class ProjectVoter {
                  public AccessVote vote(Object input) {
                      return input == null ? AccessVote.GRANT : AccessVote.ABSTAIN;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/775")
    @Test
    void changeTypeInTypeDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("de.Class2", "de.Class1", false)),
          java(
            """
              package de;
              public class Class2 {}
              """,
            """
              package de;
              public class Class1 {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1291")
    @Test
    void doNotChangeTypeInTypeDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("de.Class2", "de.Class1", true)),
          java(
            """
              package de;
              public class Class2 {}
              """
          )
        );
    }

    @Test
    void javadocs() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.List", "java.util.Collection", true)),
          java(
            """
              import java.util.List;

              /**
               * {@link List} here
               */
              class Test {
                  int n;
              }
              """,
            """
              import java.util.Collection;

              /**
               * {@link Collection} here
               */
              class Test {
                  int n;
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedJavadocLink() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", true)),
          java(
            """
              package a;
              public class A1 {
                  public void m() {}
              }
              """
          ),
          java(
            """
              /**
               * {@link a.A1} here
               */
              class Test {
              }
              """,
            """
              /**
               * {@link a.A2} here
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedJavadocSee() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", true)),
          java(
            """
              package a;
              public class A1 {
                  public void m() {}
              }
              """
          ),
          java(
            """
              /**
               * @see a.A1
               */
              class Test {
              }
              """,
            """
              /**
               * @see a.A2
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedJavadocMemberLink() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", true)),
          java(
            """
              package a;
              public class A1 {
                  public void m() {}
              }
              """
          ),
          java(
            """
              /**
               * {@link a.A1#m()} here
               */
              class Test {
              }
              """,
            """
              /**
               * {@link a.A2#m()} here
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/978")
    @Test
    void onlyUpdateApplicableImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.acme.product.factory.V1Factory", "com.acme.product.factory.V1FactoryA", true)),
          java(
            """
              package com.acme.product.factory;

              public class V1Factory {
                  public static String getItem() {
                      return "V1Factory";
                  }
              }
              """
          ),
          java(
            """
              package com.acme.product.factory;

              public class V2Factory {
                  public static String getItem() {
                      return "V2Factory";
                  }
              }
              """
          ),
          java(
            """
              package com.acme.product.factory;

              public class V1FactoryA {
                  public static String getItem() {
                      return "V1FactoryA";
                  }
              }
              """
          ),
          java(
            """
              import com.acme.product.factory.V1Factory;

              import static com.acme.product.factory.V2Factory.getItem;

              public class UseFactories {
                  static class MyV1Factory extends V1Factory {
                      static String getMyItemInherited() {
                          return getItem();
                      }
                  }

                  static String getMyItemStaticImport() {
                      return getItem();
                  }
              }
              """,
            """
              import com.acme.product.factory.V1FactoryA;

              import static com.acme.product.factory.V2Factory.getItem;

              public class UseFactories {
                  static class MyV1Factory extends V1FactoryA {
                      static String getMyItemInherited() {
                          return getItem();
                      }
                  }

                  static String getMyItemStaticImport() {
                      return getItem();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void filePathMatchWithNoMatchedClassFqn() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "x.y.Target", false)),
          java(
            """
              package a;
              public class NoMatch {
              }
              """,
            spec -> spec.path("a/b/Original.java").afterRecipe(cu ->
              assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("a/b/Original.java"))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void onlyChangeTypeWithoutMatchedFilePath() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "x.y.Target", false)),
          java(
            """
              package a.b;
              public class Original {
              }
              """,
            """
              package x.y;
              public class Target {
              }
              """,
            spec -> spec.path("a/b/NoMatch.java").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("a/b/NoMatch.java");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().getFirst().getType(), "x.y.Target")).isTrue();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void renameClassAndFilePath() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "x.y.Target", false)),
          java(
            """
              package a.b;
              public class Original {
              }
              """,
            """
              package x.y;
              public class Target {
              }
              """,
            spec -> spec.path("a/b/Original.java").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("x/y/Target.java");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().getFirst().getType(), "x.y.Target")).isTrue();
            })
          )
        );
    }

    @Test
    void renameTopLevelTypeInAPackageWithACapitalizedName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.MyCompany.Foo", "org.acme.Foo", false)),
          java(
            """
              package com.MyCompany;

              public class Foo {
              }
              """,
            """
              package org.acme;

              public class Foo {
              }
              """,
            spec -> spec.path("com/MyCompany/Foo.java").afterRecipe(cu ->
              assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("org/acme/Foo.java"))
          )
        );
    }

    @Test
    void moveTypeFromTheDefaultPackageIntoAPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("App", "b.App2", false)),
          java(
            """
              /*
               * License header
               */

              /**
               * The application.
               */
              class App {
              }
              """,
            """
              /*
               * License header
               */

              package b;

              /**
               * The application.
               */
              class App2 {
              }
              """,
            spec -> spec.path("MyApp/src/main/java/App.java").afterRecipe(cu ->
              assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString()))
                .isEqualTo("MyApp/src/main/java/b/App2.java"))
          )
        );
        rewriteRun(
          spec -> spec.recipe(new ChangeType("App", "b.App2", false)),
          java(
            """
              /*
               * License header
               */
              import java.util.List;

              /**
               * The application.
               */
              class App {
                  List<String> args;
              }
              """,
            """
              /*
               * License header
               */
              package b;

              import java.util.List;

              /**
               * The application.
               */
              class App2 {
                  List<String> args;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void updateImportPrefixWithEmptyPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "Target", false)),
          java(
            """
              package a.b;

              import java.util.List;

              class Original {
              }
              """,
            """
              import java.util.List;

              class Target {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void updateClassPrefixWithEmptyPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "Target", false)),
          java(
            """
              package a.b;

              class Original {
              }
              """,
            """
              class Target {
              }
              """
          )
        );
    }

    @Test
    void firstImportKeepsItsCommentsWhenThePackageIsRemoved() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "Target", false)),
          java(
            """
              /* License */
              package a.b;

              // why the list
              import java.util.List;

              class Original {
                  List<String> l;
              }
              """,
            """
              /* License */
              // why the list
              import java.util.List;

              class Target {
                  List<String> l;
              }
              """,
            spec -> spec.path("a/b/Original.java")
          )
        );
    }

    @Test
    void classesKeepTheirPrefixWhenThePackageIsRemoved() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "Target", false)),
          java(
            """
              package a.b;

              /**
               * The original.
               */
              class Original {
                  static class Inner {
                  }
              }
              """,
            """
              /**
               * The original.
               */
              class Target {
                  static class Inner {
                  }
              }
              """,
            spec -> spec.path("a/b/Original.java")
          )
        );
    }

    @Test
    void targetInDefaultPackageIsNeverImported() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "Target", true)),
          java(a1),
          java(
            """
              class Test {
                  a.A1 a;
              }
              """,
            """
              class Test {
                  Target a;
              }
              """
          ),
          java(
            """
              import static a.A1.stat;

              class StaticImport {
                  void m() {
                      stat();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void renameInnerClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.C$Original", "a.b.C$Target", false)),
          java(
            """
              package a.b;
              public class C {
                  public static class Original {
                      Original() {
                      }
                  }
              }
              """,
            """
              package a.b;
              public class C {
                  public static class Target {
                      Target() {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedTypeDefinitionStaysWhenItMovesToAnotherOuterType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("foo.A$Builder", "bar.B$Builder", false)),
          java("package bar; public class B { public static class Builder {} }"),
          java(
            """
              package foo;

              public class A {
                  public static Builder builder() {
                      return new Builder();
                  }

                  public static class Builder {
                      Builder name(String name) {
                          return this;
                      }
                  }
              }
              """,
            """
              package foo;

              import bar.B;

              public class A {
                  public static B.Builder builder() {
                      return new B.Builder();
                  }

                  public static class Builder {
                      Builder name(String name) {
                          return this;
                      }
                  }
              }
              """,
            spec -> spec.path("foo/A.java").afterRecipe(cu ->
              assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("foo/A.java"))
          )
        );
    }

    @Test
    void qualifyNestedTargetWhoseOuterClassNameIsTaken() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("foo.A$Builder", "bar.A$Builder", false)),
          java("package bar; public class A { public static class Builder {} }"),
          java(
            """
              package foo;

              public class A {
                  public static Builder builder() {
                      return new Builder();
                  }

                  public static class Builder {
                  }
              }
              """,
            """
              package foo;

              public class A {
                  public static bar.A.Builder builder() {
                      return new bar.A.Builder();
                  }

                  public static class Builder {
                  }
              }
              """,
            spec -> spec.path("foo/A.java")
          ),
          java(
            """
              package c;

              import foo.A.Builder;

              class C {
                  static class A {
                  }

                  Builder b;
              }
              """,
            """
              package c;

              class C {
                  static class A {
                  }

                  bar.A.Builder b;
              }
              """
          )
        );
    }

    @Test
    void shortenNestedTargetThroughTheOuterClassTheFileAlreadyUses() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("foo.A$Builder", "bar.A$Builder", true)),
          java("package foo; public class A { public static class Builder {} }"),
          java("package bar; public class A { public static class Builder {} }"),
          java(
            """
              package c;

              import bar.A;
              import foo.A.Builder;

              class D {
                  A a;
                  Builder b;
              }
              """,
            """
              package c;

              import bar.A;

              class D {
                  A a;
                  A.Builder b;
              }
              """
          )
        );
    }

    @Test
    void nameOfUnknownTypeImportedAsTheOriginalLeavesTheOuterClassNameFree() {
        rewriteRun(
          spec -> spec.recipes(
              toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                      // like a template that makes the method return the target's outer class, unattributed
                      TypeTree returnType = method.getReturnTypeExpression();
                      JavaType.Method methodType = method.getMethodType();
                      if (returnType == null || methodType == null ||
                          returnType.getType() instanceof JavaType.Unknown) {
                          return method;
                      }
                      methodType = methodType.withReturnType(JavaType.ShallowClass.build("b.Response"));
                      return method.withReturnTypeExpression(returnType.withType(JavaType.Unknown.getInstance()))
                        .withMethodType(methodType)
                        .withName(method.getName().withType(methodType));
                  }
              }),
              new ChangeType("a.Response", "b.Response$Builder", true)
            )
            .afterTypeValidationOptions(TypeValidation.none()),
          java("package a; public class Response {}"),
          java("package b; public class Response { public static class Builder {} }"),
          java(
            """
              import a.Response;

              class Test {
                  Response response() {
                      return null;
                  }

                  Response builder = new Response();
              }
              """,
            """
              import b.Response;

              class Test {
                  Response response() {
                      return null;
                  }

                  Response.Builder builder = new Response.Builder();
              }
              """
          )
        );
    }

    @Test
    void nameOfUnknownTypeThatNoImportExplainsTakesTheOuterClassName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Old", "b.Response$Builder", true))
            .typeValidationOptions(TypeValidation.none()),
          java("package a; public class Old {}"),
          // c.Response, like a class generated into package c, is on no classpath, so its name has an unknown type
          java(
            """
              package c;

              import a.Old;

              class Test {
                  Response response;
                  Old old = new Old();
              }
              """,
            """
              package c;

              class Test {
                  Response response;
                  b.Response.Builder old = new b.Response.Builder();
              }
              """
          )
        );
    }

    @Test
    void fileDeclaringANestedTypeThatStaysIsUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("foo.A$Builder", "bar.A$Builder", false)),
          java(
            """
              package foo;

              public class A {
                  public static class Builder {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2528")
    @Test
    void changePathOfNonPublicClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.C", "x.y.Z", false)),
          java(
            """
              package a.b;
              class C {
              }
              class D {
              }
              """,
            """
              package x.y;
              class Z {
              }
              class D {
              }
              """,
            spec -> spec.path("x/y/Z.java")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void renamePackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "x.y.Original", false)),
          java(
            """
              package a.b;
              class Original {
              }
              """,
            """
              package x.y;
              class Original {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    void renameClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.b.Original", "a.b.Target", false)),
          java(
            """
              package a.b;
              class Original {
              }
              """,
            """
              package a.b;
              class Target {
              }
              """
          )
        );
    }

    @Test
    void updateMethodType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", false)),
          java(
            """
              package a;
              public class A1 {
              }
              """,
            """
              package a;
              public class A2 {
              }
              """
          ),
          java(
            """
              package org.foo;

              import a.A1;

              public class Example {
                  public A1 method(A1 a1) {
                      return a1;
                  }
              }
              """,
            """
              package org.foo;

              import a.A2;

              public class Example {
                  public A2 method(A2 a1) {
                      return a1;
                  }
              }
              """
          ),
          java(
            """
              import a.A1;
              import org.foo.Example;

              public class Test {
                  A1 local = new Example().method(null);
              }
              """,
            """
              import a.A2;
              import org.foo.Example;

              public class Test {
                  A2 local = new Example().method(null);
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Method methodType = cu.getTypesInUse().getUsedMethods().stream()
                  .filter(m -> "a.A2".equals(TypeUtils.asFullyQualified(m.getReturnType()).getFullyQualifiedName()))
                  .findAny()
                  .orElseThrow(() -> new IllegalStateException("Not found"));

                assertThat(TypeUtils.asFullyQualified(methodType.getReturnType()).getFullyQualifiedName())
                  .isEqualTo("a.A2");
                assertThat(TypeUtils.asFullyQualified(methodType.getParameterTypes().getFirst()).getFullyQualifiedName())
                  .isEqualTo("a.A2");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/582")
    @Test
    void renameWhenInitializerTypeMatches() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", false)),
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  void test() {
                      List<Integer> list = new ArrayList<>();
                      list.add(1);
                      list.add(2);
                  }
              }
              """,
            """
              import java.util.*;

              class Test {
                  void test() {
                      List<Integer> list = new LinkedList<>();
                      list.add(1);
                      list.add(2);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRenameRandomVariablesMatchingClassName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", false)),
          java(
            """
              package a;
              public class A1 {
              }
              """,
            """
              package a;
              public class A2 {
              }
              """
          ),
          java(
            """
              package org.foo;

              import a.A1;

              public class Example {
                  public String method(A1 a, String a1) {
                      return a1;
                  }
              }
              """,
            """
              package org.foo;

              import a.A2;

              public class Example {
                  public String method(A2 a, String a1) {
                      return a1;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeToVariableNameWithoutChangeToType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", true)),
          java(
            """
              package a;
              public class A1 {
              }
              """
          ),
          java(
            """
              package a;
              public class A2 {
              }
              """
          ),
          java(
            """
              package org.foo;

              import a.A1;
              import a.A2;

              public class Example {
                  public A1 method1(A1 a1) {
                      return a1;
                  }
                  public A2 method2(A2 a1) {
                      return a1; // Unchanged
                  }
              }
              """,
            """
              package org.foo;

              import a.A2;

              public class Example {
                  public A2 method1(A2 a1) {
                      return a1;
                  }
                  public A2 method2(A2 a1) {
                      return a1; // Unchanged
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRenameVariableNeedlessly() {
        // Comparison to java.util.Optional: this method is equivalent to Java 8's Optional.orElse(null).
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.awt.List", "java.util.List", false)),
          //language=java
          java(
            """
              import java.awt.List;

              class A {
                  List foo(List list) {
                      return list;
                  }
              }
              """,
            """
              import java.util.List;

              class A {
                  List foo(List list) {
                      return list;
                  }
              }
              """
          )
        );
    }

    @Test
    void updateVariableType() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class Test {
                  A1 a;
              }
              """,
            """
              import a.A2;

              public class Test {
                  A2 a;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(TypeUtils.asFullyQualified(cu.getTypesInUse().getVariables().iterator().next().getType())
                  .getFullyQualifiedName()).isEqualTo("a.A2");
                assertThat(cu.findType("a.A1")).isEmpty();
                assertThat(cu.findType("a.A2")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void boundedGenericType() {
        rewriteRun(
          java(a1),
          java(a2),
          java(
            """
              import a.A1;

              public class Test {
                  <T extends A1> T method(T t) {
                      return t;
                  }
              }
              """,
            """
              import a.A2;

              public class Test {
                  <T extends A2> T method(T t) {
                      return t;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("a.A1")).isEmpty();
                assertThat(cu.findType("a.A2")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2034")
    @Test
    void changeConstructor() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "a.A2", false)),
          java(
            """
              package a;

              public class A1 {
                  public A1() {
                  }
              }
              """,
            """
              package a;

              public class A2 {
                  public A2() {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("a.A1")).isEmpty();
                assertThat(cu.findType("a.A2")).isNotEmpty();
            })
          )
        );
    }

    @Disabled("requires correct Kind.")
    @Issue("https://github.com/openrewrite/rewrite/issues/2478")
    @Test
    void updateJavaTypeClassKindAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("org.openrewrite.Test1", "org.openrewrite.Test2", false)),
          java(
            """
              package org.openrewrite;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test1 {}
              """
          ),
          java(
            """
              package org.openrewrite;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test2 {}
              """
          ),
          java(
            """
              import org.openrewrite.Test1;

              public class A {
                  @Test1
                  void method() {}
              }
              """,
            """
              import org.openrewrite.Test2;

              public class A {
                  @Test2
                  void method() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test1")).isEmpty();
                assertThat(cu.findType("org.openrewrite.Test2")).isNotEmpty();
            })
          )
        );
    }

    @Disabled("requires correct Kind.")
    @Issue("https://github.com/openrewrite/rewrite/issues/2478")
    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void changeJavaTypeClassKindEnum() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("org.openrewrite.MyEnum1", "org.openrewrite.MyEnum2", false)),
          java(
            """
              package org.openrewrite;
              public enum MyEnum1 {
                  A,
                  B
              }
              """,
            """
              package org.openrewrite;
              public enum MyEnum2 {
                  A,
                  B
              }
              """
          ),
          java(
            """
              package org.openrewrite;
              import static org.openrewrite.MyEnum1.A;
              import static org.openrewrite.MyEnum1.B;
              public class App {
                  public void test(String s) {
                      if (s.equals(" " + A + B)) {
                      }
                  }
              }
              """,
            """
              package org.openrewrite;
              import static org.openrewrite.MyEnum2.A;
              import static org.openrewrite.MyEnum2.B;
              public class App {
                  public void test(String s) {
                      if (s.equals(" " + A + B)) {
                      }
                  }
              }
              """,
            spec ->
              spec.afterRecipe(cu -> {
                  assertThat(cu.findType("org.openrewrite.MyEnum1")).isEmpty();
                  assertThat(cu.findType("org.openrewrite.MyEnum2")).isNotEmpty();
              })
          )
        );
    }

    @Test
    void thrownTypeVariableBoundedByTheChangedType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.OldException", "b.NewException", true)),
          java(
            """
              package a;
              public class OldException extends Exception {
              }
              """
          ),
          java(
            """
              package b;
              public class NewException extends Exception {
              }
              """
          ),
          java(
            """
              import a.OldException;

              class A {
                  <E extends OldException> void m() throws E {
                  }
              }
              """,
            """
              import b.NewException;

              class A {
                  <E extends NewException> void m() throws E {
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedTypeOfADottedOldNameInATypeArgument() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Outer.Inner", "b.Outer.Inner", true)),
          java("package a; public class Outer { public static class Inner { public static class Deep {} } }"),
          java("package b; public class Outer { public static class Inner { public static class Deep {} } }"),
          java(
            """
              import a.Outer;

              import java.util.List;

              class C {
                  Outer.Inner i;
                  List<Outer.Inner.Deep> l;
              }
              """,
            """
              import b.Outer;

              import java.util.List;

              class C {
                  Outer.Inner i;
                  List<Outer.Inner.Deep> l;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations l = (J.VariableDeclarations) cu.getClasses().getFirst().getBody().getStatements().get(1);
                assertThat(((JavaType.Parameterized) l.getType()).getTypeParameters().getFirst())
                  .extracting(t -> TypeUtils.toFullyQualifiedName(((JavaType.FullyQualified) t).getFullyQualifiedName()))
                  .isEqualTo("b.Outer.Inner.Deep");
            })
          )
        );
    }

    @Test
    void renameNestedTypeGivenWithDotsWithTheTypesNestedInIt() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Strategy.Snake", "a.Strategy.Python", false)),
          java(
            """
              package a;

              public class Strategy {
                  public static class Snake {
                      public static class Deep {
                      }
                  }
              }
              """,
            """
              package a;

              public class Strategy {
                  public static class Python {
                      public static class Deep {
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration python = (J.ClassDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
                J.ClassDeclaration deep = (J.ClassDeclaration) python.getBody().getStatements().getFirst();
                assertThat(deep.getType())
                  .extracting(t -> TypeUtils.toFullyQualifiedName(t.getFullyQualifiedName()))
                  .isEqualTo("a.Strategy.Python.Deep");
            })
          )
        );
    }

    @Test
    void doesNotModifyInnerClassesIfIgnoreDefinitionTrue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("Test.InnerA", "Test.InnerB", true)),
          java(
            """
              public class Test {
                  private class InnerA {
                      InnerA() {}
                      void m() {}
                      class Nested {}
                  }

                  private class InnerB {
                      InnerB() {}
                  }

                  public void test(String s) {
                      InnerA a = new InnerA();
                  }
              }
              """,
            """
              public class Test {
                  private class InnerA {
                      InnerA() {}
                      void m() {}
                      class Nested {}
                  }

                  private class InnerB {
                      InnerB() {}
                  }

                  public void test(String s) {
                      InnerB a = new InnerB();
                  }
              }
              """
          )
        );

        rewriteRun(
          spec -> spec.recipe(new ChangeType("p.Test$InnerA", "p.Test$InnerB", true)),
          java(
            """
              package p;

              public class Test {
                  class InnerA {
                      int f;

                      class Nested {
                      }
                  }

                  class InnerB {
                  }

                  InnerA a;
              }
              """,
            """
              package p;

              public class Test {
                  class InnerA {
                      int f;

                      class Nested {
                      }
                  }

                  class InnerB {
                  }

                  Test.InnerB a;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration innerA = (J.ClassDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
                J.VariableDeclarations f = (J.VariableDeclarations) innerA.getBody().getStatements().getFirst();
                assertThat(TypeUtils.asFullyQualified(f.getVariables().getFirst().getVariableType().getOwner()))
                  .extracting(JavaType.FullyQualified::getFullyQualifiedName).isEqualTo("p.Test$InnerA");
            })
          )
        );

    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4182")
    @Test
    void doesNotModifyPackageOfSibling() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("org.openrewrite.Test", "org.openrewrite.subpackage.Test", false)),
          java(
            """
              package org.openrewrite;
              public class Test {
              }
              """,
            """
              package org.openrewrite.subpackage;
              public class Test {
              }
              """
          ),
          java(
            """
              package org.openrewrite;

              import org.openrewrite.Test;

              public class Sibling {
                  public Test test() {
                      return new Test();
                  }
              }
              """,
            """
              package org.openrewrite;

              import org.openrewrite.subpackage.Test;

              public class Sibling {
                  public Test test() {
                      return new Test();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-jackson/pull/6")
    @Test
    void changeTypeOfStaticImportOfNestedEnumValueUsed() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipes(
            new ChangeType(
              "org.codehaus.jackson.map.ObjectMapper",
              "com.fasterxml.jackson.databind.ObjectMapper", true),
            new ChangeType(
              "org.codehaus.jackson.map.SerializationConfig$Feature",
              "com.fasterxml.jackson.databind.SerializationFeature", true)
          ),
          java(
            """
              package org.codehaus.jackson.map;
              public class SerializationConfig {
                  public static enum Feature {
                      WRAP_ROOT_VALUE
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.codehaus.jackson.map;
              public class ObjectMapper {
                  public ObjectMapper configure(SerializationConfig.Feature f, boolean state) {
                      return this;
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.codehaus.jackson.map.ObjectMapper;

              import static org.codehaus.jackson.map.SerializationConfig.Feature.WRAP_ROOT_VALUE;

              class A {
                  void test() {
                      ObjectMapper mapper = new ObjectMapper();
                      mapper.configure(WRAP_ROOT_VALUE, true);
                  }
              }
              """,
            """
              import com.fasterxml.jackson.databind.ObjectMapper;

              import static com.fasterxml.jackson.databind.SerializationFeature.WRAP_ROOT_VALUE;

              class A {
                  void test() {
                      ObjectMapper mapper = new ObjectMapper();
                      mapper.configure(WRAP_ROOT_VALUE, true);
                  }
              }
              """
          )
        );
    }

    @Disabled("flaky on CI")
    @Issue("https://github.com/openrewrite/rewrite/issues/4452")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "javax.annotation.Nonnull",
            "org.checkerframework.checker.nullness.qual.NonNull",
            null)),
          // language=java
          java(
            """
              import lombok.NonNull;
              import javax.annotation.Nonnull;
              import org.immutables.value.Value;

              @Value.Immutable
              @Value.Style(passAnnotations = Nonnull.class)
              interface ConflictingImports {
                      void lombokMethod(@NonNull final String lombokNonNull){}
              }
              """,
            """
              import lombok.NonNull;
              import org.immutables.value.Value;

              @Value.Immutable
              @Value.Style(passAnnotations = org.checkerframework.checker.nullness.qual.NonNull.class)
              interface ConflictingImports {
                      void lombokMethod(@NonNull final String lombokNonNull){}
              }
              """
          )
        );
    }

    @Test
    void shouldNotFullyQualifyWhenNewTypeIsAlreadyUsed() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "org.a.A",
            "org.ab.AB",
            true)),
          java(
            """
              package org.a;

              public class A {
                public static String A = "A";
              }
              """
          ),
          java(
            """
              package org.ab;

              public class AB {
                public static String A = "A";
                public static String B = "B";
              }
              """
          ),
          // language=java
          java(
            """
              import org.a.A;
              import org.ab.AB;

              class Letters {
                String a = A.A;
                String b = AB.B;
              }
              """,
            """
              import org.ab.AB;

              class Letters {
                String a = AB.A;
                String b = AB.B;
              }
              """
          )
        );
    }

    @Test
    void changeTypeInSpringXml() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("test.type.A", "test.type.B", true)),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xsi:schemaLocation="www.springframework.org/schema/beans">
                <bean id="abc" class="test.type.A"/>
              </beans>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xsi:schemaLocation="www.springframework.org/schema/beans">
                <bean id="abc" class="test.type.B"/>
              </beans>
              """
          )
        );
    }

    @Test
    void changeTypeWorksOnDirectInvocations() {
        rewriteRun(
          spec -> spec.recipe(Recipe.noop()), // do not run the default recipe
          java(
            """
              package hello;
              public class HelloClass {}
              """,
            spec -> spec.beforeRecipe((source) -> {
                TreeVisitor<?, ExecutionContext> visitor = new ChangeType("hello.HelloClass", "hello.GoodbyeClass", false).getVisitor();

                var cu = (J.CompilationUnit) visitor.visit(source, new InMemoryExecutionContext());
                assertEquals("GoodbyeClass", cu.getClasses().getFirst().getSimpleName());

                var cd = (J.ClassDeclaration) visitor.visit(source.getClasses().getFirst(), new InMemoryExecutionContext());
                assertEquals("GoodbyeClass", cd.getSimpleName());
            }))
        );
    }

    @Test
    void changeTypeInPropertiesFile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.lang.String", "java.lang.Integer", true)),
          properties(
            """
              a.property=java.lang.String
              c.property=java.lang.StringBuilder
              b.property=String
              """,
            """
              a.property=java.lang.Integer
              c.property=java.lang.StringBuilder
              b.property=String
              """,
                spec -> spec.path("application.properties"))
        );
    }

    @Test
    void changeTypeInYaml() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.lang.String", "java.lang.Integer", true)),
          yaml(
            """
              root:
                  a: java.lang.String
                  b: java.lang.StringBuilder
                  c: java.lang.test.String
                  d: String
              """,
            """
              root:
                  a: java.lang.Integer
                  b: java.lang.StringBuilder
                  c: java.lang.test.String
                  d: String
              """,
            spec -> spec.path("application.yaml")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6410")
    @Test
    void nestedClassTypeInfoUpdatedWhenOuterClassRenamed() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeType(
              "a.b.c.A",
              "a.b.c.B",
              false),
            new ChangeMethodName(
              "a.b.c.B foo()",
              "newFoo",
              null,
              null),
            // After ChangeType, the nested class should be referenced as a.b.c.B.NestedInA, not a.b.c.A.NestedInA
            new ChangeMethodName(
              "a.b.c.B$NestedInA bar()",
              "newBar",
              null,
              null)
          ),
          java(
            """
              package a.b.c;

              class A {
                  void foo() {}

                  class NestedInA {
                      void bar() {}
                  }
              }
              """,
            """
              package a.b.c;

              class B {
                  void newFoo() {}

                  class NestedInA {
                      void newBar() {}
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4773")
    @Test
    void noRenameOfTypeWithMatchingPrefix() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("org.codehaus.jackson.annotate.JsonIgnoreProperties", "com.fasterxml.jackson.annotation.JsonIgnoreProperties", false))
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  package org.codehaus.jackson.annotate;
                  public @interface JsonIgnoreProperties {
                      boolean ignoreUnknown() default false;
                  }
                  """,
                """
                  package org.codehaus.jackson.annotate;
                  public @interface JsonIgnore {
                  }
                  """
              )
            ),
          java(
            """
              import org.codehaus.jackson.annotate.JsonIgnore;
              import org.codehaus.jackson.annotate.JsonIgnoreProperties;

              @JsonIgnoreProperties(ignoreUnknown = true)
              public class myClass {
                  @JsonIgnore
                  public boolean isDirty() {
                      return false;
                  }
              }
              """,
            """
              import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
              import org.codehaus.jackson.annotate.JsonIgnore;

              @JsonIgnoreProperties(ignoreUnknown = true)
              public class myClass {
                  @JsonIgnore
                  public boolean isDirty() {
                      return false;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4764")
    @Test
    void changeTypeOfInnerClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("foo.A$Builder", "bar.A$Builder", true))
            .parser(JavaParser.fromJavaVersion().dependsOn(
                """
                  package foo;

                  public class A {
                    public A.Builder builder() {
                      return new A.Builder();
                    }

                    public static class Builder {
                      public A build() {
                        return new A();
                      }
                    }
                  }
                  """,
                """
                  package bar;

                  public class A {
                    public A.Builder builder() {
                      return new A.Builder();
                    }

                    public static class Builder {
                      public A build() {
                        return new A();
                      }
                    }
                  }
                  """
              )
            ),
          java(
            """
              import foo.A;
              import foo.A.Builder;

              class Test {
                A a;
                A.Builder b;
              }
              """,
                """
              import foo.A;

              class Test {
                A a;
                bar.A.Builder b;
              }
              """
          )
        );
    }

    @Test
    void inheritedTypesUpdated() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("com.demo.Before", "com.demo.After", true))
            .parser(JavaParser.fromJavaVersion().dependsOn(
                """
                  package com.demo;
                  
                  public class Before { }
                  """,
                """
                  package com.demo;
                  
                  public class After { }
                  """
              )
            ),
          java(
            //language=java
            """
              package app;
              
              import com.demo.Before;
              
              class X extends Before { }
              """,
            """
              package app;
              
              import com.demo.After;
              
              class X extends After { }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(FindTypes.find(cu, "app.X"))
                .singleElement()
                .extracting(NameTree::getType)
                .matches(type -> TypeUtils.isAssignableTo("com.demo.After", type), "Assignable to updated type")
            )
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2482")
    void changeTypeInServiceProviderFileContent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("org.foo.OldImpl", "org.bar.NewImpl", false)),
          text(
            """
              org.foo.OldImpl
              org.other.Unrelated
              """,
            """
              org.bar.NewImpl
              org.other.Unrelated
              """,
            spec -> spec.path("META-INF/services/org.foo.MyInterface")
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2482")
    void changeTypeInServiceProviderFileName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("org.foo.OldInterface", "org.bar.NewInterface", false)),
          text(
            "org.foo.SomeImpl\n",
            "org.foo.SomeImpl\n",
            spec -> spec.path("META-INF/services/org.foo.OldInterface")
              .afterRecipe(pt -> assertThat(pt.getSourcePath().toString().replace('\\', '/'))
                .isEqualTo("META-INF/services/org.bar.NewInterface"))
          )
        );
    }

    @Test
    void changeTypeAddsExplicitImportWhenSourceSetIsDirty() {
        // Classpath knows of no other Ambiguous type in b.* or c.*, so ambiguity cannot be proven.
        // A dirty source set must force the safe path: add an explicit import anyway.
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        markSourceSetDirty(ctx, "demo");
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Ambiguous", "b.Ambiguous", true))
                  .executionContext(ctx)
                  .beforeRecipe(withSourceTypesOnClasspath()),
          mavenProject("demo",
            java(
              """
                package a;
                public class Ambiguous {}
                """
            ),
            java(
              """
                package b;
                public class Ambiguous {}
                """
            ),
            java(
              """
                import a.Ambiguous;
                import b.*;
                import c.*;

                class Test {
                    Ambiguous a;
                }
                """,
              """
                import b.Ambiguous;
                import b.*;
                import c.*;

                class Test {
                    Ambiguous a;
                }
                """
            )
          )
        );
    }

    @Test
    void changeTypeAddsExplicitImportWhenStarImportsWouldBeAmbiguous() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Ambiguous", "b.Ambiguous", true))
                  .beforeRecipe(withSourceTypesOnClasspath()),
          java(
            """
              package a;
              public class Ambiguous {}
              """
          ),
          java(
            """
              package b;
              public class Ambiguous {}
              """
          ),
          java(
            """
              package b;
              public class Other {}
              """
          ),
          java(
            """
              package c;
              public class Ambiguous {}
              """
          ),
          java(
            """
              import a.Ambiguous;
              import b.*;
              import c.*;

              class Test {
                  Ambiguous a;
                  Other o;
              }
              """,
            """
              import b.Ambiguous;
              import b.*;
              import c.*;

              class Test {
                  Ambiguous a;
                  Other o;
              }
              """
          )
        );
    }

    @Test
    void changeTypeAddsExplicitImportWhenStarImportCollidesWithJavaLang() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Number", "b.Number", true))
                  .beforeRecipe(withSourceTypesOnClasspath()),
          java(
            """
              package a;
              public class Number {}
              """
          ),
          java(
            """
              package b;
              public class Number {}
              """
          ),
          java(
            """
              import a.Number;
              import b.*;

              class Test {
                  Number n;
              }
              """,
            """
              import b.Number;
              import b.*;

              class Test {
                  Number n;
              }
              """,
            spec -> spec.markers(JavaSourceSet.build("main", emptyList()))
          )
        );
    }

    @Test
    void fullyQualifyWhenTargetSimpleNameMeansAnotherTypeInFile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "b.Bar", true)),
          java(a1),
          java(
            """
              package c;
              public class Bar {
              }
              """
          ),
          java(
            """
              package c;

              import a.A1;

              class SamePackage {
                  A1 a;
                  Bar bar;
              }
              """,
            """
              package c;

              class SamePackage {
                  b.Bar a;
                  Bar bar;
              }
              """
          ),
          java(
            """
              import a.A1;

              class TypeParameter<Bar> {
                  A1 a;
              }
              """,
            """
              class TypeParameter<Bar> {
                  b.Bar a;
              }
              """
          ),
          java(
            """
              import a.A1;

              class NestedClass {
                  A1 a;

                  class Bar {
                  }
              }
              """,
            """
              class NestedClass {
                  b.Bar a;

                  class Bar {
                  }
              }
              """
          )
        );
    }

    @Test
    void shortenWhenFileOnlyQualifiesAnotherTypeOfTheSameName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.A1", "b.Bar", true)),
          java(a1),
          java(
            """
              package c;
              public class Bar {
              }
              """
          ),
          java(
            """
              import a.A1;

              class Test {
                  A1 a;
                  c.Bar bar;
              }
              """,
            """
              import b.Bar;

              class Test {
                  Bar a;
                  c.Bar bar;
              }
              """
          )
        );
    }

    @Test
    void fullyQualifyInFileThatKeepsOriginalDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo", "b.Foo", true)),
          java(
            """
              package a;

              public class Foo {
                  Foo self;
              }

              class Other {
                  Foo foo;
              }
              """,
            """
              package a;

              public class Foo {
                  Foo self;
              }

              class Other {
                  b.Foo foo;
              }
              """
          )
        );
    }

    @Test
    void doesNotFailOnClassDeclarationWithNullType() {
        // Non-Java (e.g. Python) sources may produce a J.ClassDeclaration whose type is null.
        // When such a file also references the type being changed, the precondition passes and
        // ChangeClassDefinition iterates every class declaration; it must skip the null-typed one
        // rather than dereference its type. ignoreDefinition is left unset so ChangeClassDefinition runs.
        rewriteRun(
          spec -> spec
            .recipe(new ChangeType("java.util.List", "java.util.Collection", null))
            .typeValidationOptions(TypeValidation.all().classDeclarations(false)),
          java(
            """
              import java.util.List;

              class Foo {
                  List<String> l;
              }
              """,
            """
              import java.util.Collection;

              class Foo {
                  Collection<String> l;
              }
              """,
            spec -> spec.mapBeforeRecipe(cu -> (J.CompilationUnit) new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                    return classDecl.withType(null);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9291")
    @Test
    void preserveOuterClassTypeArgumentsWhenChangingInnerType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "com.example.OldOuter.Registry",
            "com.example.NewOuter.Registry2",
            true
          )),
          java(
            """
              package com.example;

              public class OldOuter<T> {
                  public class Registry {
                      public T get() {
                          return null;
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              public class NewOuter<T> {
                  public class Registry2 {
                      public T get() {
                          return null;
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class SecurityConfig {
                  void configure(OldOuter<String>.Registry r) {
                      String s = r.get().trim();
                  }
              }
              """,
            """
              package com.example;

              class SecurityConfig {
                  void configure(NewOuter<String>.Registry2 r) {
                      String s = r.get().trim();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9291")
    @Test
    void preserveOuterClassTypeArgumentsAcrossPackages() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer$ExpressionInterceptUrlRegistry",
            "org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer$AuthorizationManagerRequestMatcherRegistry",
            true
          )),
          java(
            """
              package org.springframework.security.config.annotation.web.builders;

              public class HttpSecurity {
              }
              """
          ),
          java(
            """
              package org.springframework.security.config.annotation.web.configurers;

              public class ExpressionUrlAuthorizationConfigurer<H> {
                  public class ExpressionInterceptUrlRegistry {
                  }
              }
              """
          ),
          java(
            """
              package org.springframework.security.config.annotation.web.configurers;

              public class AuthorizeHttpRequestsConfigurer<H> {
                  public class AuthorizationManagerRequestMatcherRegistry {
                  }
              }
              """
          ),
          java(
            """
              package com.macro.mall.tiny.security.config;

              import org.springframework.security.config.annotation.web.builders.HttpSecurity;
              import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

              public class SecurityConfig {
                  void configure(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
                  }
              }
              """,
            """
              package com.macro.mall.tiny.security.config;

              import org.springframework.security.config.annotation.web.builders.HttpSecurity;
              import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

              public class SecurityConfig {
                  void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9291")
    @Test
    void preserveMultiLevelOuterClassTypeArgumentsWhenChangingInnerType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "com.example.oldpkg.OldOuter$Middle$Registry",
            "com.example.newpkg.NewOuter$NewMiddle$Registry2",
            true
          )),
          java(
            """
              package com.example.oldpkg;

              public class OldOuter<T> {
                  public class Middle<U> {
                      public class Registry {
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example.newpkg;

              public class NewOuter<T> {
                  public class NewMiddle<U> {
                      public class Registry2 {
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example.app;

              import com.example.oldpkg.OldOuter;

              class Config {
                  void configure(OldOuter<String>.Middle<Integer>.Registry r) {
                  }
              }
              """,
            """
              package com.example.app;

              import com.example.newpkg.NewOuter;

              class Config {
                  void configure(NewOuter<String>.NewMiddle<Integer>.Registry2 r) {
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldNamedLikeTheTypeItHoldsKeepsItsName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Outer$Inner", "a.Other$Inner", true)),
          java("package a; public class Other { public static class Inner {} }"),
          java(
            """
              package a;

              public class Outer {
                  public static class Inner {
                  }

                  public static final Inner Inner = new Inner();

                  static Object read() {
                      return Inner;
                  }
              }
              """,
            """
              package a;

              public class Outer {
                  public static class Inner {
                  }

                  public static final Other.Inner Inner = new Other.Inner();

                  static Object read() {
                      return Inner;
                  }
              }
              """
          )
        );

        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo", "b.Bar", true)),
          java("package a; public class Foo {}"),
          java("package b; public class Bar {}"),
          java(
            """
              package c;

              import a.Foo;

              class C {
                  static final Foo Foo = new Foo();
                  Object o = Foo;
              }
              """,
            """
              package c;

              import b.Bar;

              class C {
                  static final Bar Foo = new Bar();
                  Object o = Foo;
              }
              """
          )
        );
    }

    @Test
    void qualifiedReadOfAFieldNamedLikeItsTypeIsLeftAlone() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Outer$Inner", "a.Other$Inner", true)),
          java("package a; public class Other { public static class Inner {} }"),
          java(
            """
              package a;

              public class Outer {
                  public static class Inner {
                  }

                  public static Inner Inner;
              }
              """,
            """
              package a;

              public class Outer {
                  public static class Inner {
                  }

                  public static Other.Inner Inner;
              }
              """
          ),
          java(
            """
              package c;

              import a.Outer;

              class C {
                  Object o = Outer.Inner;
                  Object p = a.Outer.Inner;
              }
              """
          )
        );
    }

    @Test
    void renamedEnumAndTheTypesNestedInItKeepTheirKind() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo", "a.Bar", false)),
          java(
            """
              package a;

              public enum Foo {
                  A, B;

                  public interface Nested {
                  }
              }
              """,
            """
              package a;

              public enum Bar {
                  A, B;

                  public interface Nested {
                  }
              }
              """
          )
        );
    }

    @Test
    void enumConstantNamedLikeItsEnumKeepsItsName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Foo", "a.Bar", false)),
          java(
            """
              package a;

              public enum Foo {
                  Foo, B
              }
              """,
            """
              package a;

              public enum Bar {
                  Foo, B
              }
              """
          )
        );
    }

    @Test
    void keepImportOfTheOuterClassWhenTheTypeMovesWithinIt() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("a.Outer$Old", "a.Outer$New", null))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package a;

                public class Outer {
                    public static class Old {
                    }

                    public static class New {
                    }
                }
                """
            )),
          java(
            """
              import a.Outer;

              class Test {
                  Outer.Old o;
              }
              """,
            """
              import a.Outer;

              class Test {
                  Outer.New o;
              }
              """
          )
        );
    }
}
