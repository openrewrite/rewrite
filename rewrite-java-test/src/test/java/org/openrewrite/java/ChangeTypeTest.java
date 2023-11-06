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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.PathUtils;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

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
              import java.util.logging.*;

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

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Issue("https://github.com/openrewrite/rewrite/issues/788")
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

    @SuppressWarnings("rawtypes")
    @Issue("https://github.com/openrewrite/rewrite/issues/868")
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
              }
              """,
            """
              import java.util.List;
              
              class Test {
                  List p;
                  List p2;
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

    @SuppressWarnings("rawtypes")
    @Issue("https://github.com/openrewrite/rewrite/issues/774")
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
            "public class B extends a.A2 {}"
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
            "@a.b.c.A1 public class B {}",
            "@a.b.d.A2 public class B {}"
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

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/704")
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
          spec -> spec.recipe(new ChangeType("java.lang.Integer", "int", true)),
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
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "x.y.Target")).isTrue();
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
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "x.y.Target")).isTrue();
            })
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
                  }
              }
              """,
            """
              package a.b;
              public class C {
                  public static class Target {
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
                assertThat(TypeUtils.asFullyQualified(methodType.getParameterTypes().get(0)).getFullyQualifiedName())
                  .isEqualTo("a.A2");
            })
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
    @SuppressWarnings("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/2478")
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
    void doesNotModifyInnerClassesIfIgnoreDefinitionTrue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("Test.InnerA", "Test.InnerB", true)),
          java(
            """

              public class Test {
                  private class InnerA {
                  }
                  
                  private class InnerB {
                  }
              
                  public void test(String s) {
                      InnerA a = new InnerA();
                  }
              }
              """,
            """
              public class Test {
                  private class InnerA {
                  }
                  
                  private class InnerB {
                  }
              
                  public void test(String s) {
                      InnerB a = new InnerB();
                  }
              }
              """
          )
        );

    }
}
