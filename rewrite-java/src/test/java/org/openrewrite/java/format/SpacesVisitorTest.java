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

class SpacesVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion())
          .recipe(toRecipe(() -> new SpacesVisitor<>(IntelliJ.spaces(), null, null, IntelliJ.wrappingAndBraces().withKeepWhenFormatting(IntelliJ.wrappingAndBraces().getKeepWhenFormatting().withLineBreaks(false)), null)));
    }

    @DocumentExample
    @Test
    void normalizeMethodDeclarationParameters() {
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
    void normalizeMethodDeclarationParametersWithMultipleSpaces() {
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
    void normalizeMethodInvocationArguments() {
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
    void normalizeRecordStateVector() {
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
    void normalizeModifierPrefix() {
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
    void normalizeChainedMethodCalls() {
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
    void normalizeBlockPrefix() {
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
    void normalizeComplexMethodDeclaration() {
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
    void normalizeConstructorParameters() {
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
    void normalizeAnnotatedParameters() {
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
    void normalizeVarargsParameter() {
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
                void method(String first, int ... values) {}
            }
            """
          )
        );
    }

    @Test
    void normalizeMethodDeclarationParameterSuffix() {
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
    void normalizeMethodInvocationArgumentSuffix() {
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
    void normalizeNestedMethodCalls() {
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
    void normalizeEmptyParameters() {
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
    void normalizeMultipleConsecutiveSpaces() {
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
    void normalizeInterfaceMethodParameters() {
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
    void normalizeAbstractMethodParameters() {
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
    void normalizeLambdaParameters() {
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
    void normalizeRecordWithMultipleComponents() {
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
    void normalizeRecordWithGenericComponents() {
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
    void normalizeMixedSpacesAndNewlines() {
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
    void normalizeBeforeIfParentheses() {
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
    void normalizeBeforeForParentheses() {
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
    void normalizeBeforeWhileParentheses() {
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
    void normalizeBeforeSwitchParentheses() {
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
    void normalizeBeforeTryParentheses() {
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
    void normalizeBeforeCatchParentheses() {
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
    void normalizeBeforeSynchronizedParentheses() {
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
    void normalizeBeforeAnnotationParameters() {
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
                @MyAnnotation     ("test") void method() {}
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
                @MyAnnotation("test") void method() {}
            }
            """
          )
        );
    }

    // ===========================
    // AroundOperators tests
    // ===========================

    @Test
    void normalizeAroundAssignmentOperators() {
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
    void normalizeAroundLogicalOperators() {
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
    void normalizeAroundEqualityOperators() {
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
    void normalizeAroundRelationalOperators() {
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
    void normalizeAroundBitwiseOperators() {
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
    void normalizeAroundAdditiveOperators() {
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
    void normalizeAroundMultiplicativeOperators() {
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
    void normalizeAroundShiftOperators() {
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
    void normalizeAroundUnaryOperators() {
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
    void normalizeAroundMethodReferenceDoubleColon() {
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
    void normalizeBeforeElseKeyword() {
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
    void normalizeBeforeWhileKeywordInDoWhile() {
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
    void normalizeBeforeCatchKeyword() {
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
    void normalizeBeforeFinallyKeyword() {
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
    void normalizeWithinCodeBraces() {
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
    void normalizeWithinBrackets() {
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
    void normalizeWithinArrayInitializerBraces() {
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
    void normalizeWithinEmptyArrayInitializerBraces() {
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
    void normalizeWithinGroupingParentheses() {
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
    void normalizeWithinEmptyMethodCallParentheses() {
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
    void normalizeWithinIfParentheses() {
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
    void normalizeWithinForParentheses() {
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
    void normalizeWithinWhileParentheses() {
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
    void normalizeWithinSwitchParentheses() {
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
    void normalizeWithinTryParentheses() {
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
    void normalizeWithinCatchParentheses() {
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
    void normalizeWithinSynchronizedParentheses() {
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
    void normalizeWithinTypeCastParentheses() {
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
    void normalizeWithinAnnotationParentheses() {
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
                @MyAnnotation(   "test"   ) void method() {}
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
                @MyAnnotation("test") void method() {}
            }
            """
          )
        );
    }

    @Test
    void normalizeWithinAngleBrackets() {
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
    void normalizeBeforeQuestionMarkInTernary() {
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
    void normalizeAfterQuestionMarkInTernary() {
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
    void normalizeBeforeColonInTernary() {
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
    void normalizeAfterColonInTernary() {
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
    void normalizeTernaryOperatorCompletely() {
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
    void normalizeAfterCommaInTypeArguments() {
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
    void normalizeBeforeOpeningAngleBracketInTypeArguments() {
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
    void normalizeAfterClosingAngleBracketInTypeArguments() {
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
    void normalizeBeforeForSemicolon() {
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
    void normalizeForSemicolon() {
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
    void normalizeAfterTypeCast() {
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
    void normalizeBeforeColonInForEach() {
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
    void normalizeInsideOneLineEnumBraces() {
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
    void normalizeBeforeOpeningAngleBracketInTypeParameters() {
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
    void normalizeAroundTypeBounds() {
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
    void normalizeMethodTypeParameters() {
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
    void normalizeBeforeAnnotationArrayInitializerLeftBrace() {
        rewriteRun(
          java(
            """
            @interface MyAnnotation {
                String[] values() default     {};
            }

            class Test {
                @MyAnnotation(values =     {"a", "b"}) void method() {}
            }
            """,
            """
            @interface MyAnnotation {
                String[] values() default {};
            }

            class Test {
                @MyAnnotation(values = {"a", "b"}) void method() {}
            }
            """
          )
        );
    }

    @Test
    void normalizeWithinAnnotationArrayValueBraces() {
        rewriteRun(
          java(
            """
            @interface MyAnnotation {
                String[] values();
                int[] numbers() default {   };
            }

            class Test {
                @MyAnnotation(values = {   "a", "b", "c"   }, numbers = {   1, 2, 3   }) void method() {}
            }
            """,
            """
            @interface MyAnnotation {
                String[] values();
                int[] numbers() default {};
            }

            class Test {
                @MyAnnotation(values = {"a", "b", "c"}, numbers = {1, 2, 3}) void method() {}
            }
            """
          )
        );
    }

    @Test
    void normalizeAroundEqualsInAnnotationAssignment() {
        rewriteRun(
          java(
            """
            @interface MyAnnotation {
                String value();
                int number() default 0;
            }

            class Test {
                @MyAnnotation(value   =   "test", number   =   42) void method() {}
            }
            """,
            """
            @interface MyAnnotation {
                String value();
                int number() default 0;
            }

            class Test {
                @MyAnnotation(value = "test", number = 42) void method() {}
            }
            """
          )
        );
    }

    @Test
    void normalizeComplexNestedStructures() {
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
    void normalizeComplexExpressions() {
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
    void normalizeForEachWithComplexTypes() {
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
    void normalizeTryWithMultipleResourcesAndMultipleCatches() {
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
    void normalizeComplexGenericDeclarations() {
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
    void normalizeForLoopWithOnlyCondition() {
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
    void normalizeLambdaWithSingleParameterWithoutParentheses() {
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
    void normalizePackagePrivateClassAndMethods() {
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
    void normalizeMethodInvocationWithExplicitTypeArguments() {
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
