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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class SpacesVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion())
          .recipe(toRecipe(() -> new SpacesVisitor<>(emptyList(), true, null)));
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
                void method(String name, int age) {}
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
                void method(String param1, int param2, boolean param3) {}
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
                void method(String name  /* comment */  , int age) {}
            }
            """,
            """
            class Test {
                void method(String name  /* comment */, int age) {}
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
            """,
            """
            class Test {
                void test() {
                    method("hello"  /* comment */, 42);
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
            """,
            """
            record Person(String name  /* user's name */, int age) {}
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
                public static final void method() {}
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
                public  /* important */  static void method() {}
            }
            """,
            """
            class Test {
                public  /* important */ static void method() {}
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
            """,
            """
            class Test {
                void test() {
                    String result = "hello"  /* to uppercase */.toUpperCase().trim();
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
                void method()  /* comment */  {}
            }
            """,
            """
            class Test {
                void method()  /* comment */ {}
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
                public <T> void method(T param1, String param2) {}
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
                Test(String name, int age) {}
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
                void method(@MyAnnotation String name, int age) {}
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
                void method(String first, int... values) {}
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
                void method(String param1, String param2) {}
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
                void method() {}
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
                void method(String param) {}
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
                void method(String name, int age) {}
            }
            """
          )
        );
    }

    // ===========================
    // BeforeParentheses tests
    // ===========================

    @Test
    void minimizeBeforeIfParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    if     (true) {
                        System.out.println("true");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("true");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeForParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    for     (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeWhileParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    while     (true) {
                        break;
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    while (true) {
                        break;
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeSwitchParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method(int value) {
                    switch     (value) {
                        case 1:
                            break;
                    }
                }
            }
            """,
            """
            class Test {
                void method(int value) {
                    switch (value) {
                        case 1:
                            break;
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeTryParentheses() {
        rewriteRun(
          java(
            """
            import java.io.FileInputStream;

            class Test {
                void method() {
                    try     (FileInputStream fis = new FileInputStream("file.txt")) {
                        // do something
                    } catch (Exception e) {
                    }
                }
            }
            """,
            """
            import java.io.FileInputStream;

            class Test {
                void method() {
                    try (FileInputStream fis = new FileInputStream("file.txt")) {
                        // do something
                    } catch (Exception e) {
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeCatchParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    try {
                        throw new Exception();
                    } catch     (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeSynchronizedParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    Object lock = new Object();
                    synchronized     (lock) {
                        // critical section
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    Object lock = new Object();
                    synchronized (lock) {
                        // critical section
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeAnnotationParameters() {
        rewriteRun(
          java(
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention     (RetentionPolicy.RUNTIME)
            @interface MyAnnotation {
                String value();
            }

            class Test {
                @MyAnnotation     ("test")
                void method() {}
            }
            """,
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            @interface MyAnnotation {
                String value();
            }

            class Test {
                @MyAnnotation("test")
                void method() {}
            }
            """
          )
        );
    }

    // ===========================
    // AroundOperators tests
    // ===========================

    @Test
    void minimizeAroundAssignmentOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x   =   5;
                    x   +=   3;
                    x   -=   2;
                    x   *=   4;
                    x   /=   2;
                    x   %=   3;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = 5;
                    x += 3;
                    x -= 2;
                    x *= 4;
                    x /= 2;
                    x %= 3;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundLogicalOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    boolean result = true   &&   false   ||   true;
                }
            }
            """,
            """
            class Test {
                void method() {
                    boolean result = true && false || true;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundEqualityOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    boolean eq = 5   ==   5;
                    boolean neq = 5   !=   3;
                }
            }
            """,
            """
            class Test {
                void method() {
                    boolean eq = 5 == 5;
                    boolean neq = 5 != 3;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundRelationalOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    boolean lt = 5   <   10;
                    boolean gt = 5   >   3;
                    boolean lte = 5   <=   5;
                    boolean gte = 5   >=   5;
                }
            }
            """,
            """
            class Test {
                void method() {
                    boolean lt = 5 < 10;
                    boolean gt = 5 > 3;
                    boolean lte = 5 <= 5;
                    boolean gte = 5 >= 5;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundBitwiseOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = 5   &   3;
                    int y = 5   |   3;
                    int z = 5   ^   3;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = 5 & 3;
                    int y = 5 | 3;
                    int z = 5 ^ 3;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundAdditiveOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int sum = 5   +   3;
                    int diff = 5   -   3;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int sum = 5 + 3;
                    int diff = 5 - 3;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundMultiplicativeOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int product = 5   *   3;
                    int quotient = 10   /   2;
                    int remainder = 10   %   3;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int product = 5 * 3;
                    int quotient = 10 / 2;
                    int remainder = 10 % 3;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundShiftOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = 5   <<   2;
                    int y = 20   >>   2;
                    int z = -20   >>>   2;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = 5 << 2;
                    int y = 20 >> 2;
                    int z = -20 >>> 2;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundUnaryOperators() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = 5;
                    int y =   +   x;
                    int z =   -   x;
                    boolean b =   !   true;
                    int complement =   ~   x;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = 5;
                    int y = +x;
                    int z = -x;
                    boolean b = !true;
                    int complement = ~x;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundMethodReferenceDoubleColon() {
        rewriteRun(
          java(
            """
            import java.util.function.Function;

            class Test {
                void method() {
                    Function<String, Integer> f = String   ::   length;
                }
            }
            """,
            """
            import java.util.function.Function;

            class Test {
                void method() {
                    Function<String, Integer> f = String::length;
                }
            }
            """
          )
        );
    }

    // ===========================
    // BeforeKeywords tests
    // ===========================

    @Test
    void minimizeBeforeElseKeyword() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("true");
                    }     else {
                        System.out.println("false");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("true");
                    } else {
                        System.out.println("false");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeWhileKeywordInDoWhile() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    do {
                        System.out.println("loop");
                    }     while (false);
                }
            }
            """,
            """
            class Test {
                void method() {
                    do {
                        System.out.println("loop");
                    } while (false);
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeCatchKeyword() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    try {
                        throw new Exception();
                    }     catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeFinallyKeyword() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    try {
                        System.out.println("try");
                    }     finally {
                        System.out.println("finally");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    try {
                        System.out.println("try");
                    } finally {
                        System.out.println("finally");
                    }
                }
            }
            """
          )
        );
    }

    // ===========================
    // Within tests
    // ===========================

    @Test
    void minimizeWithinCodeBraces() {
        rewriteRun(
          java(
            """
            interface EmptyInterface {   }
            class EmptyClass {   }
            """,
            """
            interface EmptyInterface {}
            class EmptyClass {}
            """
          )
        );
    }

    @Test
    void minimizeWithinBrackets() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int[] array = new int[   10   ];
                    int value = array[   0   ];
                }
            }
            """,
            """
            class Test {
                void method() {
                    int[] array = new int[10];
                    int value = array[0];
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinArrayInitializerBraces() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int[] array = {   1, 2, 3   };
                }
            }
            """,
            """
            class Test {
                void method() {
                    int[] array = {1, 2, 3};
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinEmptyArrayInitializerBraces() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int[] array = {   };
                }
            }
            """,
            """
            class Test {
                void method() {
                    int[] array = {};
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinGroupingParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int result = (   5 + 3   ) * 2;
                    int result2 = (   5 + 3   * 2);
                }
            }
            """,
            """
            class Test {
                void method() {
                    int result = (5 + 3) * 2;
                    int result2 = (5 + 3 * 2);
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinEmptyMethodCallParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    test(   );
                }
                void test() {}
            }
            """,
            """
            class Test {
                void method() {
                    test();
                }
                void test() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinIfParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    if (   true   ) {
                        System.out.println("true");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("true");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinForParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    for (   int i = 0; i < 10; i++   ) {
                        System.out.println(i);
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinWhileParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    while (   true   ) {
                        break;
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    while (true) {
                        break;
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinSwitchParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method(int value) {
                    switch   (   value   ) {
                        case 1:
                            break;
                    }
                }
            }
            """,
            """
            class Test {
                void method(int value) {
                    switch (value) {
                        case 1:
                            break;
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinTryParentheses() {
        rewriteRun(
          java(
            """
            import java.io.FileInputStream;

            class Test {
                void method() {
                    try (   FileInputStream fis   =   new FileInputStream("file.txt")   ) {
                        // do something
                    } catch (Exception e) {
                    }
                }
            }
            """,
            """
            import java.io.FileInputStream;

            class Test {
                void method() {
                    try (FileInputStream fis = new FileInputStream("file.txt")) {
                        // do something
                    } catch (Exception e) {
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinCatchParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    try {
                        throw new Exception();
                    } catch (   Exception e   ) {
                        e.printStackTrace();
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinSynchronizedParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    Object lock = new Object();
                    synchronized (   lock   ) {
                        // critical section
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    Object lock = new Object();
                    synchronized (lock) {
                        // critical section
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinTypeCastParentheses() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    Object obj = "hello";
                    String str = (   String   )obj;
                }
            }
            """,
            """
            class Test {
                void method() {
                    Object obj = "hello";
                    String str = (String) obj;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinAnnotationParentheses() {
        rewriteRun(
          java(
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            @interface MyAnnotation {
                String value();
            }

            class Test {
                @MyAnnotation(   "test"   )
                void method() {}
            }
            """,
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            @interface MyAnnotation {
                String value();
            }

            class Test {
                @MyAnnotation("test")
                void method() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinAngleBrackets() {
        rewriteRun(
          java(
            """
            import java.util.List;

            class Test {
                void method() {
                    List<   String   > list = null;
                }
            }
            """,
            """
            import java.util.List;

            class Test {
                void method() {
                    List<String> list = null;
                }
            }
            """
          )
        );
    }

    // ===========================
    // TernaryOperator tests
    // ===========================

    @Test
    void minimizeBeforeQuestionMarkInTernary() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = true   ? 1 : 2;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = true ? 1 : 2;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAfterQuestionMarkInTernary() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = true ?   1 : 2;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = true ? 1 : 2;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeColonInTernary() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = true ? 1   : 2;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = true ? 1 : 2;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAfterColonInTernary() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = true ? 1 :   2;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = true ? 1 : 2;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeTernaryOperatorCompletely() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x = true   ?   1   :   2;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = true ? 1 : 2;
                }
            }
            """
          )
        );
    }

    // ===========================
    // TypeArguments tests
    // ===========================

    @Test
    void minimizeAfterCommaInTypeArguments() {
        rewriteRun(
          java(
            """
            import java.util.Map;

            class Test {
                void method() {
                    Map<String,     Integer> map = null;
                }
            }
            """,
            """
            import java.util.Map;

            class Test {
                void method() {
                    Map<String, Integer> map = null;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeOpeningAngleBracketInTypeArguments() {
        rewriteRun(
          java(
            """
            import java.util.List;

            class Test {
                void method() {
                    List     <String> list = null;
                }
            }
            """,
            """
            import java.util.List;

            class Test {
                void method() {
                    List<String> list = null;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAfterClosingAngleBracketInTypeArguments() {
        rewriteRun(
          java(
            """
            import java.util.List;

            class Test {
                List<String>     getList() {
                    return null;
                }
            }
            """,
            """
            import java.util.List;

            class Test {
                List<String> getList() {
                    return null;
                }
            }
            """
          )
        );
    }

    // ===========================
    // Other tests
    // ===========================

    @Test
    void minimizeBeforeForSemicolon() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    for (int i = 0   ; i < 10   ; i++) {
                        System.out.println(i);
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeForSemicolon() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    for (   int i = 0   ;     i < 10         ;     i++   ) {
                        System.out.println(i);
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeAfterTypeCast() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    Object obj = "hello";
                    String str = (String)     obj;
                }
            }
            """,
            """
            class Test {
                void method() {
                    Object obj = "hello";
                    String str = (String) obj;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeBeforeColonInForEach() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int[] array = {1, 2, 3};
                    for (int value   : array) {
                        System.out.println(value);
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    int[] array = {1, 2, 3};
                    for (int value : array) {
                        System.out.println(value);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeInsideOneLineEnumBraces() {
        rewriteRun(
          java(
            """
            enum Color {   RED, GREEN, BLUE   }
            """,
            """
            enum Color {RED, GREEN, BLUE}
            """
          )
        );
    }

    // ===========================
    // TypeParameters tests
    // ===========================

    @Test
    void minimizeBeforeOpeningAngleBracketInTypeParameters() {
        rewriteRun(
          java(
            """
            class Test     <T> {
                void method() {}
            }
            """,
            """
            class Test<T> {
                void method() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundTypeBounds() {
        rewriteRun(
          java(
            """
            import java.io.Serializable;

            class Test<T   extends   Serializable> {
                void method() {}
            }
            """,
            """
            import java.io.Serializable;

            class Test<T extends Serializable> {
                void method() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeMethodTypeParameters() {
        rewriteRun(
          java(
            """
            class Test {
                <T   extends   Comparable<T>>   T method(T value) {
                    return value;
                }
            }
            """,
            """
            class Test {
                <T extends Comparable<T>> T method(T value) {
                    return value;
                }
            }
            """
          )
        );
    }

    // ===========================
    // Additional comprehensive tests
    // ===========================

    @Test
    void minimizeBeforeAnnotationArrayInitializerLeftBrace() {
        rewriteRun(
          java(
            """
            @interface MyAnnotation {
                String[] values() default     {};
            }

            class Test {
                @MyAnnotation(values =     {"a", "b"})
                void method() {}
            }
            """,
            """
            @interface MyAnnotation {
                String[] values() default {};
            }

            class Test {
                @MyAnnotation(values = {"a", "b"})
                void method() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeWithinAnnotationArrayValueBraces() {
        rewriteRun(
          java(
            """
            @interface MyAnnotation {
                String[] values();
                int[] numbers() default {   };
            }

            class Test {
                @MyAnnotation(values = {   "a", "b", "c"   }, numbers = {   1, 2, 3   })
                void method() {}
            }
            """,
            """
            @interface MyAnnotation {
                String[] values();
                int[] numbers() default {};
            }

            class Test {
                @MyAnnotation(values = {"a", "b", "c"}, numbers = {1, 2, 3})
                void method() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeAroundEqualsInAnnotationAssignment() {
        rewriteRun(
          java(
            """
            @interface MyAnnotation {
                String value();
                int number() default 0;
            }

            class Test {
                @MyAnnotation(value   =   "test", number   =   42)
                void method() {}
            }
            """,
            """
            @interface MyAnnotation {
                String value();
                int number() default 0;
            }

            class Test {
                @MyAnnotation(value = "test", number = 42)
                void method() {}
            }
            """
          )
        );
    }

    @Test
    void minimizeComplexNestedStructures() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    if     (true)     {
                        for     (int i = 0   ;   i < 10   ;   i++)     {
                            while     (i > 0)     {
                                switch     (i)     {
                                    case 1:
                                        break;
                                }
                            }
                        }
                    }     else     {
                        System.out.println("false");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    if (true) {
                        for (int i = 0; i < 10; i++) {
                            while (i > 0) {
                                switch (i) {
                                    case 1:
                                        break;
                                }
                            }
                        }
                    } else {
                        System.out.println("false");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeComplexExpressions() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int result = (   (   5   +   3   )   *   2   )   -   (   10   /   2   );
                    boolean condition = true   &&   (   false   ||   true   )   &&   !   false;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int result = ((5 + 3) * 2) - (10 / 2);
                    boolean condition = true && (false || true) && !false;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeForEachWithComplexTypes() {
        rewriteRun(
          java(
            """
            import java.util.List;
            import java.util.Map;

            class Test {
                void method() {
                    Map<String, List<Integer>> map = null;
                    for (   Map.Entry<String, List<Integer>>   entry   :   map.entrySet()   ) {
                        System.out.println(entry);
                    }
                }
            }
            """,
            """
            import java.util.List;
            import java.util.Map;

            class Test {
                void method() {
                    Map<String, List<Integer>> map = null;
                    for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
                        System.out.println(entry);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeTryWithMultipleResourcesAndMultipleCatches() {
        rewriteRun(
          java(
            """
            import java.io.FileInputStream;
            import java.io.IOException;

            class Test {
                void method() {
                    try     (   FileInputStream fis1 = new FileInputStream("file1.txt")   ;   FileInputStream fis2 = new FileInputStream("file2.txt")   )     {
                        // do something
                    }     catch     (   IOException e   )     {
                        e.printStackTrace();
                    }     catch     (   Exception e   )     {
                        e.printStackTrace();
                    }     finally     {
                        System.out.println("done");
                    }
                }
            }
            """,
            """
            import java.io.FileInputStream;
            import java.io.IOException;

            class Test {
                void method() {
                    try (FileInputStream fis1 = new FileInputStream("file1.txt"); FileInputStream fis2 = new FileInputStream("file2.txt")) {
                        // do something
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("done");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeComplexGenericDeclarations() {
        rewriteRun(
          java(
            """
            import java.util.List;
            import java.util.Map;

            class Test     <   K   extends   Comparable<K>   ,   V   extends   List<String>   >     {
                <   T   extends   Map<K, V>   >   T method(   T   param   ) {
                    return param;
                }
            }
            """,
            """
            import java.util.List;
            import java.util.Map;

            class Test<K extends Comparable<K>, V extends List<String>> {
                <T extends Map<K, V>> T method(T param) {
                    return param;
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeForLoopWithOnlyCondition() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    for (   ;   true    ;   ) {
                        break;
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    for (; true; ) {
                        break;
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void minimizeLambdaWithSingleParameterWithoutParentheses() {
        rewriteRun(
          java(
            """
            import java.util.function.Function;

            class Test {
                void test() {
                    Function<String, String> fn = s   ->   s.toUpperCase();
                }
            }
            """,
            """
            import java.util.function.Function;

            class Test {
                void test() {
                    Function<String, String> fn = s -> s.toUpperCase();
                }
            }
            """
          )
        );
    }

    @Test
    void minimizePackagePrivateClassAndMethods() {
        rewriteRun(
          java(
            """
            class Test {
                void method(  String   param  ) {
                }
            }
            """,
            """
            class Test {
                void method(String param) {}
            }
            """
          )
        );
    }

    @Test
    void minimizeMethodInvocationWithExplicitTypeArguments() {
        rewriteRun(
          java(
            """
            class Test {
                void test() {
                    this.  <  String  >  method(  "hello"  );
                }
                <T> void method(T value) {}
            }
            """,
            """
            class Test {
                void test() {
                    this.<String>method("hello");
                }
                <T> void method(T value) {}
            }
            """
          )
        );
    }

    @Test
    void addSpaceAfterCommaWhenMissing() {
        rewriteRun(
          java(
            """
            class Test {
                void method(String param1,String param2,int param3) {
                    method("a","b",1);
                }
            }
            """,
            """
            class Test {
                void method(String param1, String param2, int param3) {
                    method("a", "b", 1);
                }
            }
            """
          )
        );
    }

    @Test
    void addSpaceAroundBinaryOperatorsWhenMissing() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    int x=5+3;
                    boolean b=true&&false;
                    x=2-1;
                }
            }
            """,
            """
            class Test {
                void method() {
                    int x = 5 + 3;
                    boolean b = true && false;
                    x = 2 - 1;
                }
            }
            """
          )
        );
    }

    @Test
    void addSpaceBeforeKeywordsWhenMissing() {
        rewriteRun(
          java(
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("true");
                    }else{
                        System.out.println("false");
                    }
                }
            }
            """,
            """
            class Test {
                void method() {
                    if (true) {
                        System.out.println("true");
                    } else {
                        System.out.println("false");
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void lambdaMethodsArgumentsHaveSpace() {
        rewriteRun(
          java(
            """
            import java.util.List;
            
            class Test {
                Integer sum(List<Integer> numbers) {
                    return numbers.stream().filter(n -> n > 0).reduce(  0,    (a, b) -> {
                        int sum = a + b;
                        System.out.println("Current sum: " + sum);
                        return sum;
                    }  );
                }
            }
            """,
            """
            import java.util.List;
            
            class Test {
                Integer sum(List<Integer> numbers) {
                    return numbers.stream().filter(n -> n > 0).reduce(0, (a, b) -> {
                        int sum = a + b;
                        System.out.println("Current sum: " + sum);
                        return sum;
                    });
                }
            }
            """
          )
        );
    }
}
