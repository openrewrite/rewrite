/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MinimizationVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion())
          .recipe(toRecipe(() -> new MinimizationVisitor<>(IntelliJ.spaces())));
    }

    @DocumentExample
    @Test
    void minimizeMethodDeclarationParameters() {
        rewriteRun(
          java(
            """
            class Test {
                void method(  String   name  ,   int   age  ) {
                }
            }
            """,
            """
            class Test {
                void method(String name, int age) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeMethodDeclarationParametersWithMultipleSpaces() {
        rewriteRun(
          java(
            """
            class Test {
                void method(     String     param1     ,     int     param2     ,     boolean     param3     ) {
                }
            }
            """,
            """
            class Test {
                void method(String param1, int param2, boolean param3) {
                }
            }
            """
          )
        );
    }

    @Test
    void preserveCommentsInMethodDeclarationParameters() {
        rewriteRun(
          java(
            """
            class Test {
                void method(String name  /* comment */  , int age) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeMethodInvocationArguments() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    method(  "hello"  ,   42  ,   true  );
                }
                void method(String s, int i, boolean b) {}
            }
            """,
            """
            class Test {
                void test() {
                    method("hello", 42, true);
                }
                void method(String s, int i, boolean b) {}
            }
            """
          )
        );
    }

    @Test
    void preserveCommentsInMethodInvocationArguments() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    method("hello"  /* comment */  , 42);
                }
                void method(String s, int i) {}
            }
            """
          )
        );
    }

    @Test
    void minimizeRecordStateVector() {
        rewriteRun(
          java(
            """
            record Person(  String   name  ,   int   age  ) {}
            """,
            """
            record Person(String name, int age) {}
            """
          )
        );
    }

    @Test
    void preserveCommentsInRecordStateVector() {
        rewriteRun(
          java(
            """
            record Person(String name  /* user's name */  , int age) {}
            """
          )
        );
    }

    @Test
    void minimizeModifierPrefix() {
        rewriteRun(
          java(
            """
            class Test {
                public     static     final     void method() {
                }
            }
            """,
            """
            class Test {
                public static final void method() {
                }
            }
            """
          )
        );
    }

    @Test
    void preserveCommentsInModifiers() {
        rewriteRun(
          java(
            """
            class Test {
                public  /* important */  static void method() {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeChainedMethodCalls() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    String result = "hello"  .  toUpperCase()  .  trim();
                }
            }
            """,
            """
            class Test {
                void test() {
                    String result = "hello".toUpperCase().trim();
                }
            }
            """
          )
        );
    }

    @Test
    void preserveCommentsInChainedMethodCalls() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    String result = "hello"  /* to uppercase */  .toUpperCase().trim();
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBlockPrefix() {
        rewriteRun(
          java(
            """
            class Test {
                void method()     {
                    if (true)     {
                        System.out.println("hello");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("hello");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void preserveCommentsBeforeBlock() {
        rewriteRun(
          java(
            """
            class Test {
                void method()  /* comment */  {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeComplexMethodDeclaration() {
        rewriteRun(
          java(
            """
            class Test {
                public     <T>     void method(  T   param1  ,   String   param2  ) {
                }
            }
            """,
            """
            class Test {
                public <T> void method(T param1, String param2) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeConstructorParameters() {
        rewriteRun(
          java(
            """
            class Test {
                Test(  String   name  ,   int   age  ) {
                }
            }
            """,
            """
            class Test {
                Test(String name, int age) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAnnotatedParameters() {
        rewriteRun(
          java(
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            @interface MyAnnotation {}

            class Test {
                void method(  @MyAnnotation   String   name  ,   int   age  ) {
                }
            }
            """,
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            @interface MyAnnotation {}

            class Test {
                void method(@MyAnnotation String name, int age) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeVarargsParameter() {
        rewriteRun(
          java(
            """
            class Test {
                void method(  String   first  ,   int  ...   values  ) {
                }
            }
            """,
            """
            class Test {
                void method(String first, int... values) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeMethodDeclarationParameterSuffix() {
        rewriteRun(
          java(
            """
            class Test {
                void method(String param1  ,  String param2  ) {
                }
            }
            """,
            """
            class Test {
                void method(String param1, String param2) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeMethodInvocationArgumentSuffix() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    method("arg1"  ,  "arg2"  );
                }
                void method(String s1, String s2) {}
            }
            """,
            """
            class Test {
                void test() {
                    method("arg1", "arg2");
                }
                void method(String s1, String s2) {}
            }
            """
          )
        );
    }

    @Test
    void minimizeNestedMethodCalls() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    outer(  inner(  "value"  )  );
                }
                void outer(String s) {}
                String inner(String s) { return s; }
            }
            """,
            """
            class Test {
                void test() {
                    outer(inner("value"));
                }
                void outer(String s) {}
                String inner(String s) { return s; }
            }
            """
          )
        );
    }

    @Test
    void minimizeEmptyParameters() {
        rewriteRun(
          java(
            """
            class Test {
                void method(  ) {
                }
            }
            """,
            """
            class Test {
                void method() {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeMultipleConsecutiveSpaces() {
        rewriteRun(
          java(
            """
            class Test {
                void method(          String          param          ) {
                }
            }
            """,
            """
            class Test {
                void method(String param) {
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeInterfaceMethodParameters() {
        rewriteRun(
          java(
            """
            interface Test {
                void method(  String   name  ,   int   age  );
            }
            """,
            """
            interface Test {
                void method(String name, int age);
            }
            """
          )
        );
    }

    @Test
    void minimizeAbstractMethodParameters() {
        rewriteRun(
          java(
            """
            abstract class Test {
                abstract void method(  String   name  ,   int   age  );
            }
            """,
            """
            abstract class Test {
                abstract void method(String name, int age);
            }
            """
          )
        );
    }

    @Test
    void minimizeLambdaParameters() {
        rewriteRun(
          java(
            """
            import java.util.function.BiFunction;
            import java.util.function.Supplier;

            class Test {
                void test() {
                    BiFunction<String, Integer, String> fn = (  s  ,   i  ) -> s + i;
                    Supplier<String> fn2 = (     ) -> "yay";
                }
            }
            """,
            """
            import java.util.function.BiFunction;
            import java.util.function.Supplier;

            class Test {
                void test() {
                    BiFunction<String, Integer, String> fn = (s, i) -> s + i;
                    Supplier<String> fn2 = () -> "yay";
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeRecordWithMultipleComponents() {
        rewriteRun(
          java(
            """
            record Address(  String   street  ,   String   city  ,   String   state  ,   int   zipCode  ) {}
            """,
            """
            record Address(String street, String city, String state, int zipCode) {}
            """
          )
        );
    }

    @Test
    void minimizeRecordWithGenericComponents() {
        rewriteRun(
          java(
            """
            import java.util.List;

            record Container<T>(  List<T>   items  ,   int   count  ) {}
            """,
            """
            import java.util.List;

            record Container<T>(List<T> items, int count) {}
            """
          )
        );
    }

    @Test
    void doNotChangeAlreadyMinimizedCode() {
        rewriteRun(
          java(
            """
            class Test {
                void method(String name, int age) {
                    method("hello", 42);
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeMixedSpacesAndNewlines() {
        rewriteRun(
          java(
            """
            class Test {
                void   method  (
                    String   name  ,

                    int   age
                ) {
                }
            }
            """,
            """
            class Test {
                void method(String name, int age) {
                }
            }
            """
          )
        );
    }
}