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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceConstantTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceConstant("com.google.common.base.Charsets", "UTF_8", "\"UTF_8\""));
    }

    @DocumentExample
    @Test
    void replaceConstant() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("guava")),
          java(
            """
              import com.google.common.base.Charsets;
              class Test {
                  Object o = Charsets.UTF_8;
              }
              """,
            """
              class Test {
                  Object o = "UTF_8";
              }
              """
          )
        );
    }

    @Test
    void replaceStaticallyImportedConstant() {
        rewriteRun(
          // the recipe doesn't retype the call whose argument it replaces, which then calls println(String)
          spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build())
            .parser(JavaParser.fromJavaVersion().classpath("guava")),
          java(
            """
              import static com.google.common.base.Charsets.UTF_8;
              class Test {
                  Object o = UTF_8;
                  void foo() {
                      System.out.println(UTF_8);
                  }
              }
              """,
            """
              class Test {
                  Object o = "UTF_8";
                  void foo() {
                      System.out.println("UTF_8");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstantInLambda() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("guava")),
          java(
            """
              import java.util.function.Supplier;
              import static com.google.common.base.Charsets.UTF_8;

              class Test {
                  void run(Supplier<Object> supplier) {
                  }

                  void method() {
                      run(() -> UTF_8);
                  }
              }
              """,
            """
              import java.util.function.Supplier;

              class Test {
                  void run(Supplier<Object> supplier) {
                  }

                  void method() {
                      run(() -> "UTF_8");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1752")
    @Test
    void doesNotChangeOriginalVariableDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("com.constant.B", "VAR", "\"newValue\"")),
          java(
            """
              package com.constant;
              public class B {
                  public static final String VAR = "default";
                  void method() {
                      String VAR = "";
                  }
              }
              """
          ),
          java(
            """
              package com.abc;
              import com.constant.B;
              class A {
                  String v = B.VAR;
                  private String method() {
                      return B.VAR;
                  }
              }
              """,
            """
              package com.abc;
              class A {
                  String v = "newValue";
                  private String method() {
                      return "newValue";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2579")
    @Test
    void addsExplicitCastWhenReplacingWithNullAndOverloadRequiresIt() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("com.abc.A", "VAR", "null")),
          java(
            """
              package com.abc;
              class A {
                  public static final String VAR = "default";
                  private String m1() {
                      return m2(VAR);
                  }
                  private String m2(String s) {
                      return s;
                  }
                  private String m2(Integer i) {
                      return i.toString();
                  }
              }
              """,
            """
              package com.abc;
              class A {
                  public static final String VAR = "default";
                  private String m1() {
                      return m2((String) null);
                  }
                  private String m2(String s) {
                      return s;
                  }
                  private String m2(Integer i) {
                      return i.toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesNullAssignmentsAndExistingCasts() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("A", "VAR", "null")),
          java(
            """
              class A {
                  static final String VAR = "default";
                  Object value = VAR;
                  String m() {
                      return m((String) VAR);
                  }
                  String m(String s) { return s; }
                  String m(Integer i) { return i.toString(); }
              }
              """,
            """
              class A {
                  static final String VAR = "default";
                  Object value = null;
                  String m() {
                      return m((String) null);
                  }
                  String m(String s) { return s; }
                  String m(Integer i) { return i.toString(); }
              }
              """
          )
        );
    }

    @Test
    void castsQualifiedConstantInConstructorArgument() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("A", "VAR", "null")),
          java(
            """
              import java.util.List;

              class A {
                  static final List<String> VAR = null;
                  A(List<String> value) {}
                  A(Integer value) {}
                  static A create() {
                      return new A(A.VAR);
                  }
              }
              """,
            """
              import java.util.List;

              class A {
                  static final List<String> VAR = null;
                  A(List<String> value) {}
                  A(Integer value) {}
                  static A create() {
                      return new A((List<String>) null);
                  }
              }
              """
          )
        );
    }

    @Test
    void castsParenthesizedArrayArgument() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("A", "VAR", "null")),
          java(
            """
              class A {
                  static final String[] VAR = {"default"};
                  String m() {
                      return m((VAR));
                  }
                  String m(String[] values) { return "array"; }
                  String m(Integer value) { return "integer"; }
              }
              """,
            """
              class A {
                  static final String[] VAR = {"default"};
                  String m() {
                      return m(((String[]) null));
                  }
                  String m(String[] values) { return "array"; }
                  String m(Integer value) { return "integer"; }
              }
              """
          )
        );
    }

    @Test
    void preservesVarargsInvocation() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("A", "VAR", "null")),
          java(
            """
              class A {
                  static final String VAR = "default";
                  int count() {
                      return count(VAR);
                  }
                  int count(Object... values) {
                      return values.length;
                  }
              }
              """,
            """
              class A {
                  static final String VAR = "default";
                  int count() {
                      return count((Object) null);
                  }
                  int count(Object... values) {
                      return values.length;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotExposeInaccessibleConstantType() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("Holder", "VAR", "null")),
          java(
            """
              class Holder {
                  private static class Value {}
                  static final Value VAR = null;
              }
              class A {
                  Object m() { return m(Holder.VAR); }
                  Object m(Object value) { return value; }
              }
              """,
            """
              class Holder {
                  private static class Value {}
                  static final Value VAR = null;
              }
              class A {
                  Object m() { return m((Object) null); }
                  Object m(Object value) { return value; }
              }
              """
          )
        );
    }

    @Test
    void preservesGenericMethodInference() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstant("A", "VAR", "null")),
          java(
            """
              class A {
                  static final String VAR = "default";
                  String m() { return identity(VAR); }
                  static <T> T identity(T value) { return value; }
              }
              """,
            """
              class A {
                  static final String VAR = "default";
                  String m() { return identity((String) null); }
                  static <T> T identity(T value) { return value; }
              }
              """
          )
        );
    }
}
