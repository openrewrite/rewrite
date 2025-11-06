package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CustomAutoFormatTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CustomAutoFormat())
          .parser(JavaParser.fromJavaVersion().dependsOn("""
            package com.example;

            import java.util.function.Function;

            public class MyObject {
                public static Builder notBuilder() { return new Builder(); }
                public static Builder builder() { return new Builder(); }
                public static Builder newBuilder() { return new Builder(); }
                public static class Builder {
                    Builder name(String n) { return this; }
                    Builder age(int a) { return this; }
                    Builder items(java.util.List<String> items) { return this; }
                    Builder nested(MyObject nested) { return this; }
                    Builder function(Function<MyObject, MyObject> fn) { return this; }
                    MyObject build() { return new MyObject(); }
                }
            }
            """,
            """
            package org.jspecify.annotations;
            
            public @interface Nullable{}
            """));
    }

    @Nested
    class WrapMethodDeclarations {
        @Test
        void formatMethodWithMultipleParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age, boolean active) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          boolean active
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatInterfaceMethodWithMultipleParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  interface Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age, boolean active);
                  }
                  """,
                """
                  package com.example;
    
                  interface Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          boolean active
                      );
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithTwoParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void doNotFormatMethodWithSingleParameter() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name) {
                      }
                  }
                  """,
                  """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void preserveAlreadyFormattedsomeInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          boolean active
                      ) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          boolean active
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithGenericTypes() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.Map;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(List<String> names, Map<String, Integer> ages) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.Map;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          List<String> names,
                          Map<String, Integer> ages
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithArrayParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String[] names, int[] ages, boolean[][] flags) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String[] names,
                          int[] ages,
                          boolean[][] flags
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithVarargs() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int... values) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int... values
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatConstructor() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class SomeLongClassNameThatForSureWillTriggerChoppingOfParametersAsTheClassNameItselfIsAlreadyLong {
                      SomeLongClassNameThatForSureWillTriggerChoppingOfParametersAsTheClassNameItselfIsAlreadyLong(String name, int age, boolean active) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class SomeLongClassNameThatForSureWillTriggerChoppingOfParametersAsTheClassNameItselfIsAlreadyLong {
                      SomeLongClassNameThatForSureWillTriggerChoppingOfParametersAsTheClassNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          boolean active
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithAnnotatedParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import org.jspecify.annotations.Nullable;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(@Nullable String name, int age) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import org.jspecify.annotations.Nullable;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          @Nullable String name,
                          int age
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithParameterComments() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, /* age parameter */ int age, boolean active) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          /* age parameter */ int age,
                          boolean active
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithReturnType() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      String someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age) {
                          return name;
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      String someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      ) {
                          return name;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithThrowsClause() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.io.IOException;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age) throws IOException {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.io.IOException;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      ) throws IOException {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatPublicsomeInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      public void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      public void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatStaticsomeInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      static void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      static void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatAbstractsomeInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  abstract class Test {
                      abstract void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age);
                  }
                  """,
                """
                  package com.example;
    
                  abstract class Test {
                      abstract void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      );
                  }
                  """
              )
            );
        }

        @Test
        void formatInterfacesomeInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  interface Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age);
                  }
                  """,
                """
                  package com.example;
    
                  interface Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      );
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithComplexGenerics() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.Map;
                  import java.util.function.Function;
    
                  class Test {
                      <T, R> Map<T, List<R>> someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(List<T> input, Function<T, List<R>> mapper) {
                          return null;
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.Map;
                  import java.util.function.Function;
    
                  class Test {
                      <T, R> Map<T, List<R>> someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          List<T> input,
                          Function<T, List<R>> mapper
                      ) {
                          return null;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void doNotFormatMethodWithNoParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMultipleMethodsInClass() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong1(String name, int age) {
                      }
    
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong2(boolean active, double value) {
                      }
                      
                      void doNotChopMe(boolean active, double value) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong1(
                          String name,
                          int age
                      ) {
                      }
    
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong2(
                          boolean active,
                          double value
                      ) {
                      }
    
                      void doNotChopMe(boolean active, double value) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodInInnerClass() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      static class Inner {
                          void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age) {
                          }
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      static class Inner {
                          void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                              String name,
                              int age
                          ) {
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithFinalParameters() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(final String name, final int age) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          final String name,
                          final int age
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithMixedParameterTypes() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name, int age, List<String> items, boolean active, double[] values) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          List<String> items,
                          boolean active,
                          double[] values
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void reindentIncorrectlyFormattedsomeInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                              String name,
                              int age) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age
                      ) {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMethodWithPartialNewlines() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(String name,
                          int age, boolean active) {
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void someInsanelyLongMethodNameThatForSureWillTriggerChoppingOfParametersAsTheMethodNameItselfIsAlreadyLong(
                          String name,
                          int age,
                          boolean active
                      ) {
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class WrapMethodChains {
        @Test
        void formatBuilderMethod() {
            rewriteRun(
              java(
                """
                  package com.example;

                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder().name("test").age(25).build();
                      }
                  }
                  """,
                """
                  package com.example;

                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .age(25)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatNewBuilderMethod() {
            rewriteRun(
              java(
                """
                  package com.example;

                  class Test {
                      void test() {
                          MyObject obj = MyObject.newBuilder().name("test").age(25).build();
                      }
                  }
                  """,
                """
                  package com.example;

                  class Test {
                      void test() {
                          MyObject obj = MyObject.newBuilder()
                              .name("test")
                              .age(25)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void preserveAlreadyFormattedBuilder() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .age(25)
                              .build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .age(25)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatAlreadyNewlinedBuilder() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                          .name("test")
                          .age(25)
                          .build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .age(25)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void reindentIncorrectlyIndented() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                                                 .name("test")
                                                 .age(25)
                                                 .build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .age(25)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatNestedBuilders() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder().name("test").nested(MyObject.builder().name("nested").build()).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .nested(MyObject.builder()
                                  .name("nested")
                                  .build())
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatBuilderInFieldDeclaration() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      private final MyObject value = MyObject.builder().name("hello").age(30).build();
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      private final MyObject value = MyObject.builder()
                          .name("hello")
                          .age(30)
                          .build();
                  }
                  """
              )
            );
        }

        @Test
        void formatBuilderInReturn() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      MyObject test() {
                          return MyObject.builder().name("hello").age(30).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      MyObject test() {
                          return MyObject.builder()
                              .name("hello")
                              .age(30)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatSingleMethodCallAfterBuilder() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder().build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatBuilderWithComments() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder().name("hello") /* comment */ .age(30).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("hello") /* comment */
                              .age(30)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatBuilderInLambda() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.function.Supplier;
    
                  class Test {
                      void test() {
                          Supplier<MyObject> supplier = () -> MyObject.builder().name("hello").age(30).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.function.Supplier;
    
                  class Test {
                      void test() {
                          Supplier<MyObject> supplier = () -> MyObject.builder()
                              .name("hello")
                              .age(30)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void doNotFormatNonBuilderChainedCalls() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          String result = "hello".toUpperCase().substring(1).trim();
                          String sb = new StringBuilder().append("a").append("b").toString();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          String result = "hello".toUpperCase().substring(1).trim();
                          String sb = new StringBuilder().append("a").append("b").toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatNonBuilderLongChainedCalls() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          String result = "hello".toUpperCase().substring(0,1).substring(0,1).substring(0,1).substring(0,1).substring(0,1).trim();
                          String sb = new StringBuilder().append("a").append("b").append("c").append("d").append("e").append("f").toString();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          String result = "hello".toUpperCase()
                              .substring(0, 1)
                              .substring(0, 1)
                              .substring(0, 1)
                              .substring(0, 1)
                              .substring(0, 1)
                              .trim();
                          String sb = new StringBuilder().append("a")
                              .append("b")
                              .append("c")
                              .append("d")
                              .append("e")
                              .append("f")
                              .toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatMultipleBuilderChainsInMethod() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject s1 = MyObject.builder().name("a").age(1).build();
                          MyObject s2 = MyObject.newBuilder().name("b").age(2).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject s1 = MyObject.builder()
                              .name("a")
                              .age(1)
                              .build();
                          MyObject s2 = MyObject.newBuilder()
                              .name("b")
                              .age(2)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatBuilderWithMethodArguments() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.Arrays;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder().name("test").items(Arrays.asList("a", "b", "c")).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.Arrays;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .items(Arrays.asList("a", "b", "c"))
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatNestedBuilderImmediatelyCallingBuild() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder().name("test").nested(MyObject.builder().build()).age(30).build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject obj = MyObject.builder()
                              .name("test")
                              .nested(MyObject.builder()
                                  .build())
                              .age(30)
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamChain() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      void test(List<String> list) {
                          List<Nested> result = list.stream().filter(s -> s.length() > 3)
                          .map(String::toUpperCase).sorted()
                          .map(s -> (Nested) new Nested(s))
                          .collect(Collectors.toList());
                      }
    
                      static class Nested {
                          Nested(String s) {}
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      void test(List<String> list) {
                          List<Nested> result = list.stream()
                              .filter(s -> s.length() > 3)
                              .map(String::toUpperCase)
                              .sorted()
                              .map(s -> (Nested) new Nested(s))
                              .collect(Collectors.toList());
                      }
    
                      static class Nested {
                          Nested(String s) {}
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatDeeplyNestedBuilders() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject root = MyObject.builder().name("root").nested(
                              MyObject.builder().name("level1").nested(
                                  MyObject.builder().name("level2").nested(
                                      MyObject.builder().name("level3").nested(
                                          MyObject.builder().name("level4")
                                      .build())
                                  .build())
                              .build())
                              .build())
                          .build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  class Test {
                      void test() {
                          MyObject root = MyObject.builder()
                              .name("root")
                              .nested(MyObject.builder()
                                  .name("level1")
                                  .nested(MyObject.builder()
                                      .name("level2")
                                      .nested(MyObject.builder()
                                          .name("level3")
                                          .nested(MyObject.builder()
                                              .name("level4")
                                              .build())
                                          .build())
                                      .build())
                                  .build())
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithMultilineFilterLambda() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.Collection;
                  import java.util.Optional;
    
                  class Test {
                      Optional<Item> findItem(Collection<Item> collection) {
                          return collection.stream().filter(item -> {
                              if (someCondition(item)) {
                                  return true;
                              } else if (otherCondition(item)) {
                                  return true;
                              }
                              return false;
                          }).findFirst();
                      }
    
                      boolean someCondition(Item item) { return true; }
                      boolean otherCondition(Item item) { return false; }
    
                      static class Item {}
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.Collection;
                  import java.util.Optional;
    
                  class Test {
                      Optional<Item> findItem(Collection<Item> collection) {
                          return collection.stream()
                              .filter(item -> {
                                  if (someCondition(item)) {
                                      return true;
                                  } else if (otherCondition(item)) {
                                      return true;
                                  }
                                  return false;
                              })
                              .findFirst();
                      }
    
                      boolean someCondition(Item item) { return true; }
                      boolean otherCondition(Item item) { return false; }
    
                      static class Item {}
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithMultipleMultilineLambdas() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<Item> items) {
                          return items.stream().filter(item -> {
                              boolean valid = item.isValid();
                              if (valid) {
                                  System.out.println("Valid: " + item);
                              }
                              return valid;
                          }).map(item -> {
                              String result = item.toString();
                              System.out.println("Mapping: " + result);
                              return result.toUpperCase();
                          }).collect(Collectors.toList());
                      }
    
                      static class Item {
                          boolean isValid() { return true; }
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<Item> items) {
                          return items.stream()
                              .filter(item -> {
                                  boolean valid = item.isValid();
                                  if (valid) {
                                      System.out.println("Valid: " + item);
                                  }
                                  return valid;
                              })
                              .map(item -> {
                                  String result = item.toString();
                                  System.out.println("Mapping: " + result);
                                  return result.toUpperCase();
                              })
                              .collect(Collectors.toList());
                      }
    
                      static class Item {
                          boolean isValid() { return true; }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithMixedLambdaStyles() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<Integer> process(List<String> items) {
                          return items.stream().filter(s -> s.length() > 3).map(s -> {
                              try {
                                  return Integer.parseInt(s);
                              } catch (NumberFormatException e) {
                                  return 0;
                              }
                          }).filter(i -> i > 0).sorted().collect(Collectors.toList());
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<Integer> process(List<String> items) {
                          return items.stream()
                              .filter(s -> s.length() > 3)
                              .map(s -> {
                                  try {
                                      return Integer.parseInt(s);
                                  } catch (NumberFormatException e) {
                                      return 0;
                                  }
                              })
                              .filter(i -> i > 0)
                              .sorted()
                              .collect(Collectors.toList());
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithComplexNestedLambda() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<Department> departments) {
                          return departments.stream().flatMap(dept -> {
                              System.out.println("Processing department: " + dept.name);
                              return dept.employees.stream()
                              .filter(emp -> emp.active)
                              .map(emp -> dept.name + ": " + emp.name);
                          }).sorted().distinct().collect(Collectors.toList());
                      }
    
                      static class Department {
                          String name;
                          List<Employee> employees;
                      }
    
                      static class Employee {
                          String name;
                          boolean active;
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<Department> departments) {
                          return departments.stream()
                              .flatMap(dept -> {
                                  System.out.println("Processing department: " + dept.name);
                                  return dept.employees.stream()
                                      .filter(emp -> emp.active)
                                      .map(emp -> dept.name + ": " + emp.name);
                              })
                              .sorted()
                              .distinct()
                              .collect(Collectors.toList());
                      }
    
                      static class Department {
                          String name;
                          List<Employee> employees;
                      }
    
                      static class Employee {
                          String name;
                          boolean active;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithMethodReferencesAndLambdas() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<String> items) {
                          return items.stream().filter(this::isValid).map(s -> {
                              String processed = preprocess(s);
                              return processed.toUpperCase();
                          }).sorted(String::compareTo).collect(Collectors.toList());
                      }
    
                      boolean isValid(String s) { return s != null && !s.isEmpty(); }
                      String preprocess(String s) { return s.trim(); }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<String> items) {
                          return items.stream()
                              .filter(this::isValid)
                              .map(s -> {
                                  String processed = preprocess(s);
                                  return processed.toUpperCase();
                              })
                              .sorted(String::compareTo)
                              .collect(Collectors.toList());
                      }
    
                      boolean isValid(String s) { return s != null && !s.isEmpty(); }
                      String preprocess(String s) { return s.trim(); }
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithPeekAndMultilineLambda() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<String> items) {
                          return items.stream().peek(item -> {
                              System.out.println("Before: " + item);
                              if (item.length() > 10) {
                                  System.out.println("Long item detected");
                              }
                          }).map(String::toUpperCase).peek(System.out::println).collect(Collectors.toList());
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      List<String> process(List<String> items) {
                          return items.stream()
                              .peek(item -> {
                                  System.out.println("Before: " + item);
                                  if (item.length() > 10) {
                                      System.out.println("Long item detected");
                                  }
                              })
                              .map(String::toUpperCase)
                              .peek(System.out::println)
                              .collect(Collectors.toList());
                      }
                  }
                  """
              )
            );
        }

        @Test
        void preserveAlreadyFormattedStreamWithMultilineLambda() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.Collection;
                  import java.util.Optional;
    
                  class Test {
                      Optional<Item> findItem(Collection<Item> collection) {
                          return collection.stream()
                              .filter(item -> {
                                  if (someCondition(item)) {
                                      return true;
                                  } else if (otherCondition(item)) {
                                      return true;
                                  }
                                  return false;
                              })
                              .findFirst();
                      }
    
                      boolean someCondition(Item item) { return true; }
                      boolean otherCondition(Item item) { return false; }
    
                      static class Item {}
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.Collection;
                  import java.util.Optional;
    
                  class Test {
                      Optional<Item> findItem(Collection<Item> collection) {
                          return collection.stream()
                              .filter(item -> {
                                  if (someCondition(item)) {
                                      return true;
                                  } else if (otherCondition(item)) {
                                      return true;
                                  }
                                  return false;
                              })
                              .findFirst();
                      }
    
                      boolean someCondition(Item item) { return true; }
                      boolean otherCondition(Item item) { return false; }
    
                      static class Item {}
                  }
                  """
              )
            );
        }

        @Test
        void formatStreamWithReduceMultilineLambda() {
            rewriteRun(
              java(
                """
                  package com.example;
    
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
                  """,
                """
                  package com.example;
    
                  import java.util.List;
    
                  class Test {
                      Integer sum(List<Integer> numbers) {
                          return numbers.stream()
                              .filter(n -> n > 0)
                              .reduce(0, (a, b) -> {
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

        @Test
        void formatStreamInBuilderArgument() {
            rewriteRun(
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      MyObject process(List<String> items) {
                          return MyObject.builder().items(items.stream().filter(s -> s.length() > 3).map(String::toUpperCase).collect(Collectors.toList())).name("name").build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      MyObject process(List<String> items) {
                          return MyObject.builder()
                              .items(items.stream()
                                  .filter(s -> s.length() > 3)
                                  .map(String::toUpperCase)
                                  .collect(Collectors.toList()))
                              .name("name")
                              .build();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void formatLongLinesOnly() {
            rewriteRun(
              //language=java
              java(
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      MyObject process(List<String> items) {
                          return MyObject.notBuilder().items(items).name("name").build();
                      }
                      MyObject process2(List<String> items) {
                          return MyObject.notBuilder().items(items.stream().filter(s -> s.length() > 3)
                              .map(String::toUpperCase).collect(Collectors.toList())).name("name").build();
                      }
                      MyObject process3(List<String> items) {
                          return MyObject.notBuilder().items(items.stream().filter(s -> s.length() > 3).
                              map(String::toUpperCase).collect(Collectors.toList())).name("name").build();
                      }
                      MyObject process4(List<String> items) {
                          return MyObject.notBuilder().items(items.stream().filter(s -> s.length() > 3).map(String::toUpperCase).collect(Collectors.toList())).name("name").build();
                      }
                      MyObject process5(List<String> items) {
                          return MyObject.notBuilder().name("name")
                              .items(items.stream().filter(s -> s.length() > 3).map(String::toUpperCase).collect(Collectors.toList()))
                              .build();
                      }
                  }
                  """,
                """
                  package com.example;
    
                  import java.util.List;
                  import java.util.stream.Collectors;
    
                  class Test {
                      MyObject process(List<String> items) {
                          return MyObject.notBuilder().items(items).name("name").build();
                      }
                      MyObject process2(List<String> items) {
                          return MyObject.notBuilder()
                              .items(items.stream()
                                  .filter(s -> s.length() > 3)
                                  .map(String::toUpperCase)
                                  .collect(Collectors.toList()))
                              .name("name")
                              .build();
                      }
                      MyObject process3(List<String> items) {
                          return MyObject.notBuilder()
                              .items(items.stream()
                                  .filter(s -> s.length() > 3)
                                  .map(String::toUpperCase)
                                  .collect(Collectors.toList()))
                              .name("name")
                              .build();
                      }
                      MyObject process4(List<String> items) {
                          return MyObject.notBuilder()
                              .items(items.stream()
                                  .filter(s -> s.length() > 3)
                                  .map(String::toUpperCase)
                                  .collect(Collectors.toList()))
                              .name("name")
                              .build();
                      }
                      MyObject process5(List<String> items) {
                          return MyObject.notBuilder()
                              .name("name")
                              .items(items.stream()
                                  .filter(s -> s.length() > 3)
                                  .map(String::toUpperCase)
                                  .collect(Collectors.toList()))
                              .build();
                      }
                  }
                  """
              )
            );
        }
    }
}
