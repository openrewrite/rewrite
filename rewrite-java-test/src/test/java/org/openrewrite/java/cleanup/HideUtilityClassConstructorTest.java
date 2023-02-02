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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.HideUtilityClassConstructorStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.Arrays;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class HideUtilityClassConstructorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new HideUtilityClassConstructor());
    }

    @SuppressWarnings("UnnecessaryModifier")
    @Issue("https://github.com/openrewrite/rewrite/issues/1780")
    @Test
    void doNotAddConstructorToInterface() {
        rewriteRun(
          java(
            """
              public interface A {
                  public static final String utility = "";
              }
              """
          )
        );
    }

    /**
     * Should be a utility class since all methods are static, but class has public constructor
     */
    @Test
    void changePublicConstructorToPrivate() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {
                  }

                  public static void utility() {
                  }
              }
              """,
            """
              public class A {
                  private A() {
                  }

                  public static void utility() {
                  }
              }
              """
          )
        );
    }

    @Test
    void changePackagePrivateConstructorToPrivate() {
        rewriteRun(
          java(
            """
              public class A {
                  A() {
                  }

                  public static void utility() {
                  }
              }
              """,
            """
              public class A {
                  private A() {
                  }

                  public static void utility() {
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyUtilityClassesWithProtectedConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  protected A() {
                  }

                  public static void utility() {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeInapplicableNestedClass() {
        rewriteRun(
          java(
            """
              public class A {
                  private A() {}
                  public static String foo() { return "foo"; }
              }
              """
          )
        );
    }

    @Test
    void changeApplicableNestedClass() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public static String foo() { return "foo"; }
                  private static class Builder() {
                      public Builder() {}
                      public static String foo() { return "foo"; }
                  }
              }
              """,
            """
              public class A {
                  private A() {}
                  public static String foo() { return "foo"; }
                  private static class Builder() {
                      private Builder() {}
                      public static String foo() { return "foo"; }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeUtilityClassesWithMixedExposedConstructors() {
        rewriteRun(
          java(
            """
              public class A {
                  protected A() {
                  }

                  public A(String a) {
                  }

                  A(String a, String b) {
                  }

                  private A(String a, String b, String c) {
                  }

                  public static void utility() {
                  }
              }
              """,
            """
              public class A {
                  protected A() {
                  }

                  private A(String a) {
                  }

                  private A(String a, String b) {
                  }

                  private A(String a, String b, String c) {
                  }

                  public static void utility() {
                  }
              }
              """
          )
        );
    }

    /**
     * Should be a utility class since all methods are static, but class has no constructor (default/package-private)
     */
    @Test
    void addPrivateConstructorWhenOnlyDefaultConstructor() {
        rewriteRun(
          java(
            """
              public class Math {
                  public static final int TWO = 2;

                  public static int addTwo(int a) {
                      return a + TWO;
                  }
              }
              """,
            """
              public class Math {
                  public static final int TWO = 2;

                  public static int addTwo(int a) {
                      return a + TWO;
                  }

                  private Math() {
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/538")
    void ignoreClassesWithMainMethod() {
        rewriteRun(
          java(
            """
              package a;

              public class A {
                  public static void main(String[] args) {
                      // SpringApplication.run(A.class, args);
                  }
              }
              """
          )
        );
    }

    /**
     * Should be a utility class since all fields are static, but class has public constructor
     */
    @Test
    void identifyUtilityClassesOnlyStaticFields() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {
                  }

                  public static int a;
              }
              """,
            """
              public class A {
                  private A() {
                  }

                  public static int a;
              }
              """
          )
        );
    }

    /**
     * Not a utility class since the class implements an interface
     */
    @Test
    void identifyNonUtilityClassesWhenImplementsInterface() {
        rewriteRun(
          java(
            """
              public interface B {
                  static void utility() {
                  }
              }
              """
          ),
          java(
            """
              public class A implements B {
                  public A() {
                  }

                  public static void utility() {
                      B.utility();
                  }
              }
              """
          )
        );
    }

    /**
     * Not a utility class since the class extends another
     */
    @Test
    void identifyNonUtilityClassesWhenExtendsClass() {
        rewriteRun(
          java(
            """
              public class B {
                  public static void utility() {}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              public class A extends B {
                  public A() {
                  }
              
                  public static void doSomething() {
                  }
              }
              """
          )
        );
    }

    /**
     * Not a utility class since some fields are static, but at least one non-static
     */
    @Test
    void identifyNonUtilityClassesMixedFields() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {
                  }

                  public int a;

                  public static int b;
              }
              """
          )
        );
    }

    /**
     * Should be a utility class since all methods are static, but class has public constructor
     */
    @Test
    void identifyUtilityClassesOnlyStaticMethods() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {
                  }

                  public static void utility() {
                  }

                  public static void utility(String[] args) {
                      utility();
                  }
              }
              """,
            """
              public class A {
                  private A() {
                  }

                  public static void utility() {
                  }

                  public static void utility(String[] args) {
                      utility();
                  }
              }
              """
          )
        );
    }

    /**
     * Inner class should be a utility class since all it's methods are static, but it has public constructor
     */
    @Test
    void identifyUtilityClassesInnerStaticClasses() {
        rewriteRun(
          java(
            """
              public class A {

                  static class Inner {
                      public Inner() {
                      }

                      public static void utility() {
                      }
                  }

                  public void utility() {
                  }
              }
              """,
            """
              public class A {

                  static class Inner {
                      private Inner() {
                      }

                      public static void utility() {
                      }
                  }

                  public void utility() {
                  }
              }
              """
          )
        );
    }

    /**
     * Not a utility class since some methods are static, but at least one non-static
     */
    @Test
    void identifyNonUtilityClassesMixedMethods() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {
                  }

                  public static void someStatic() {
                  }

                  public void notStatic() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1060")
    @Test
    void identifyAbstractClass() {
        rewriteRun(
          java(
            """
              public abstract class A {
                  public A() {
                  }

                  public static void someStatic1() {
                  }

                  public static void someStatic2() {
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyNonUtilityClassesOnlyPublicConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyNonUtilityClassesTotallyEmpty() {
        rewriteRun(
          java(
            """
              public class A {
              }
              """
          )
        );
    }

    private static Consumer<RecipeSpec> hideUtilityClassConstructor(String... ignoreIfAnnotatedBy) {
        return spec -> spec.parser(JavaParser.fromJavaVersion().styles(
          singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(), singletonList(
              new HideUtilityClassConstructorStyle(Arrays.asList(ignoreIfAnnotatedBy))
            )
            )
          )
        ));
    }

    @Test
    void dontChangeSuppressedUtilityClasses() {
        rewriteRun(
          hideUtilityClassConstructor(
            "@lombok.experimental.UtilityClass",
            "@java.lang.SuppressWarnings(\"checkstyle:HideUtilityClassConstructor\")"
          ),
          java(
            """
              package lombok.experimental;
              public @interface UtilityClass {}
              """
          ),
          java(
            """
              import lombok.experimental.UtilityClass;
              
              @UtilityClass
              public class DoNotChangeMeA {
                  public static void utility() {
                  }
              }
              
              @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
              class DoNotChangeMeB {
                  public static void utility() {
                  }
              }
              """
          )
        );
    }

    @Test
    void suppressedUtilityClassWithDifferentArgument() {
        rewriteRun(
          hideUtilityClassConstructor("@java.lang.SuppressWarnings(\"checkstyle:HideUtilityClassConstructor\")"),
          java(
            """
              class ChangeMeA {
                  public static void utility() {
                  }
              }
              """,
            """
              class ChangeMeA {
                  public static void utility() {
                  }

                  private ChangeMeA() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1058")
    @Test
    void doesNotChangePackagePrivateEnumConstructorToPrivate() {
        rewriteRun(
          java(
            """
              public enum SomeEnum {
                  A,B,C;

                  SomeEnum() {
                  }

                  public static void utility() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1058")
    @Test
    void enumClass() {
        rewriteRun(
          java(
            """
              public enum SomeEnum {
                  A,B,C;

                  public static void utility() {
                  }
              }
              """
          )
        );
    }
}
