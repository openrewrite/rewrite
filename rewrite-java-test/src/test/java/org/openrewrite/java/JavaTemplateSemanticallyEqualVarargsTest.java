/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateSemanticallyEqualVarargsTest implements RewriteTest {

    @Nested
    class BasicMatching {

        @Test
        void matchVarargsWithMultipleElements() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list1 = Arrays.asList("a", "b", "c");
                          List<Object> list2 = Arrays.asList(1, 2);
                          List<Object> list3 = Arrays.asList("single");
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list1 = /*~~>*/Arrays.asList("a", "b", "c");
                          List<Object> list2 = /*~~>*/Arrays.asList(1, 2);
                          List<Object> list3 = /*~~>*/Arrays.asList("single");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchVarargsWithZeroElements() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list = Arrays.asList();
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list = /*~~>*/Arrays.asList();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchVarargsWithExplicitArray() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          Object[] arr = new Object[]{"a", "b"};
                          List<Object> list = Arrays.asList(arr);
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          Object[] arr = new Object[]{"a", "b"};
                          List<Object> list = /*~~>*/Arrays.asList(arr);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchPrimitiveVarargs() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("com.google.common.primitives.Booleans.asList(#{items:anyArray(boolean)})")
                    .javaParser(JavaParser.fromJavaVersion().classpath("guava"))
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })).parser(JavaParser.fromJavaVersion().classpath("guava")),
              //language=java
              java(
                """
                  import com.google.common.primitives.Booleans;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Boolean> list1 = Booleans.asList(true, false);
                          List<Boolean> list2 = Booleans.asList(true);
                          List<Boolean> list3 = Booleans.asList();
                      }
                  }
                  """,
                """
                  import com.google.common.primitives.Booleans;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Boolean> list1 = /*~~>*/Booleans.asList(true, false);
                          List<Boolean> list2 = /*~~>*/Booleans.asList(true);
                          List<Boolean> list3 = /*~~>*/Booleans.asList();
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class MixedFixedAndVarargs {

        @Test
        void matchVarargsWithMixedFixedAndVarargs() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("String.format(#{format:any(String)}, #{args:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  class Foo {
                      void test() {
                          String s1 = String.format("Hello %s", "World");
                          String s2 = String.format("Value: %d, %d", 1, 2);
                          String s3 = String.format("No args");
                      }
                  }
                  """,
                """
                  class Foo {
                      void test() {
                          String s1 = /*~~>*/String.format("Hello %s", "World");
                          String s2 = /*~~>*/String.format("Value: %d, %d", 1, 2);
                          String s3 = /*~~>*/String.format("No args");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchInstanceMethodVarargs() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("#{ps:any(java.io.PrintStream)}.printf(#{format:any(java.lang.String)}, #{args:anyArray(java.lang.Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      if (mi.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor())) {
                          return SearchResult.found(mi);
                      }
                      return mi;
                  }
              })),
              //language=java
              java(
                """
                  class Foo {
                      void test() {
                          System.out.printf("Hello %s %s", "World", "!");
                          System.out.printf("No args");
                      }
                  }
                  """,
                """
                  class Foo {
                      void test() {
                          /*~~>*/System.out.printf("Hello %s %s", "World", "!");
                          /*~~>*/System.out.printf("No args");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchVarargsWithPlaceholderPrefix() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("String.format(#{format:any(java.lang.String)}, #{rest:anyArray(java.lang.Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  class Foo {
                      void test() {
                          String s1 = String.format("Hello %s %s", "a", "b"); // Matches - format + varargs
                          String s2 = String.format("Just format"); // Matches - format + empty varargs
                          String s3 = String.format("One %s", "arg"); // Matches - format + one vararg
                      }
                  }
                  """,
                """
                  class Foo {
                      void test() {
                          String s1 = /*~~>*/String.format("Hello %s %s", "a", "b"); // Matches - format + varargs
                          String s2 = /*~~>*/String.format("Just format"); // Matches - format + empty varargs
                          String s3 = /*~~>*/String.format("One %s", "arg"); // Matches - format + one vararg
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class ParameterExtraction {

        @Test
        void extractVarargsMatchedElements() {
            rewriteRun(
              spec -> spec
                .expectedCyclesThatMakeChanges(1).cycles(1)
                .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                    final JavaTemplate template = JavaTemplate
                      .builder("java.util.Arrays.asList(#{items:anyArray(Object)})")
                      .build();

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        JavaTemplate.Matcher matcher = template.matcher(getCursor());
                        if (matcher.find()) {
                            J param = matcher.parameter(0);
                            // Varargs matches return a J.NewArray with the matched elements as the initializer
                            if (param instanceof J.NewArray) {
                                var arr = (J.NewArray) param;
                                assertThat(arr.getInitializer()).hasSize(3);
                                return SearchResult.found(mi, "varargs:" + arr.getInitializer().size());
                            }
                        }
                        return mi;
                    }
                })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list = Arrays.asList("a", "b", "c");
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list = /*~~(varargs:3)~~>*/Arrays.asList("a", "b", "c");
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class NoMatchCases {

        @Test
        void noMatchWhenMethodNameDiffers() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  class Foo {
                      void test() {
                          int[] arr = {1, 2, 3};
                          Arrays.sort(arr); // Different method name - should not match
                          String s = Arrays.toString(arr); // Different method name - should not match
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenVarargsTypeMismatch() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(String)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<String> list1 = Arrays.asList("a", "b");
                          List<Integer> list2 = Arrays.asList(1, 2); // Should not match - Integer not String
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<String> list1 = /*~~>*/Arrays.asList("a", "b");
                          List<Integer> list2 = Arrays.asList(1, 2); // Should not match - Integer not String
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenFixedArgumentsDontMatch() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("String.format(\"Hello %s\", #{args:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  class Foo {
                      void test() {
                          String s1 = String.format("Hello %s", "World"); // Matches - format string is same
                          String s2 = String.format("Goodbye %s", "World"); // Does not match - format string differs
                          String s3 = String.format("Different", "arg"); // Does not match - format string differs
                      }
                  }
                  """,
                """
                  class Foo {
                      void test() {
                          String s1 = /*~~>*/String.format("Hello %s", "World"); // Matches - format string is same
                          String s2 = String.format("Goodbye %s", "World"); // Does not match - format string differs
                          String s3 = String.format("Different", "arg"); // Does not match - format string differs
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenDeclaringTypeDiffers() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list1 = Arrays.asList("a", "b"); // Matches
                          List<Object> list2 = List.of("a", "b"); // Does not match - different class
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list1 = /*~~>*/Arrays.asList("a", "b"); // Matches
                          List<Object> list2 = List.of("a", "b"); // Does not match - different class
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenReceiverTypeDiffers() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("#{ps:any(java.io.PrintStream)}.printf(#{format:any(java.lang.String)}, #{args:anyArray(java.lang.Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      if (mi.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor())) {
                          return SearchResult.found(mi);
                      }
                      return mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.io.PrintWriter;
                  import java.io.StringWriter;
                  class Foo {
                      void test() {
                          System.out.printf("Hello %s", "World"); // Matches - PrintStream
                          PrintWriter pw = new PrintWriter(new StringWriter());
                          pw.printf("Hello %s", "World"); // Does not match - PrintWriter not PrintStream
                      }
                  }
                  """,
                """
                  import java.io.PrintWriter;
                  import java.io.StringWriter;
                  class Foo {
                      void test() {
                          /*~~>*/System.out.printf("Hello %s", "World"); // Matches - PrintStream
                          PrintWriter pw = new PrintWriter(new StringWriter());
                          pw.printf("Hello %s", "World"); // Does not match - PrintWriter not PrintStream
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenNotEnoughFixedArguments() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  // Template requires 2 fixed args before varargs
                  final JavaTemplate template = JavaTemplate
                    .builder("#{ps:any(java.io.PrintStream)}.printf(#{locale:any(java.util.Locale)}, #{format:any(java.lang.String)}, #{args:anyArray(java.lang.Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      if (mi.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor())) {
                          return SearchResult.found(mi);
                      }
                      return mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Locale;
                  class Foo {
                      void test() {
                          // Only 1 fixed arg (format) - should not match template expecting 2 fixed args
                          System.out.printf("Hello %s", "World");
                          // 2 fixed args (locale, format) - should match
                          System.out.printf(Locale.US, "Hello %s", "World");
                      }
                  }
                  """,
                """
                  import java.util.Locale;
                  class Foo {
                      void test() {
                          // Only 1 fixed arg (format) - should not match template expecting 2 fixed args
                          System.out.printf("Hello %s", "World");
                          // 2 fixed args (locale, format) - should match
                          /*~~>*/System.out.printf(Locale.US, "Hello %s", "World");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenUsingAnyArrayOnNonVarargsMethod() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  // String.valueOf(Object) is NOT a varargs method
                  final JavaTemplate template = JavaTemplate
                    .builder("String.valueOf(#{items:anyArray(Object)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  class Foo {
                      void test() {
                          // String.valueOf is not varargs, so anyArray should not expand
                          String s1 = String.valueOf("hello");
                          String s2 = String.valueOf(123);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenVarargsElementTypeIsIncompatible() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  // Template expects Number varargs
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(java.lang.Number)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Number> list1 = Arrays.asList(1, 2, 3); // Matches - Integer extends Number
                          List<Number> list2 = Arrays.asList(1.0, 2.0); // Matches - Double extends Number
                          List<String> list3 = Arrays.asList("a", "b"); // Does not match - String is not Number
                          List<Object> list4 = Arrays.asList(new Object()); // Does not match - Object is not Number
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Number> list1 = /*~~>*/Arrays.asList(1, 2, 3); // Matches - Integer extends Number
                          List<Number> list2 = /*~~>*/Arrays.asList(1.0, 2.0); // Matches - Double extends Number
                          List<String> list3 = Arrays.asList("a", "b"); // Does not match - String is not Number
                          List<Object> list4 = Arrays.asList(new Object()); // Does not match - Object is not Number
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchWhenMixedValidAndInvalidVarargsElements() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(#{items:anyArray(java.lang.Number)})")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          // Mix of Number and String - should not match since String is not Number
                          List<Object> list = Arrays.asList(1, "two", 3);
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class ConstantArguments {

        @Test
        void matchVarargsWithSpecificConstants() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  // Template with specific constant values in varargs position
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(\"a\", \"b\")")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<String> list1 = Arrays.asList("a", "b"); // Matches exactly
                          List<String> list2 = Arrays.asList("x", "y"); // Different values - no match
                          List<String> list3 = Arrays.asList("a", "b", "c"); // Different count - no match
                          List<String> list4 = Arrays.asList("a"); // Different count - no match
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<String> list1 = /*~~>*/Arrays.asList("a", "b"); // Matches exactly
                          List<String> list2 = Arrays.asList("x", "y"); // Different values - no match
                          List<String> list3 = Arrays.asList("a", "b", "c"); // Different count - no match
                          List<String> list4 = Arrays.asList("a"); // Different count - no match
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchVarargsWithNumericConstants() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList(1, 2, 3)")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Integer> list1 = Arrays.asList(1, 2, 3); // Matches exactly
                          List<Integer> list2 = Arrays.asList(1, 2, 4); // Different value - no match
                          List<Integer> list3 = Arrays.asList(1, 2); // Different count - no match
                          List<Integer> list4 = Arrays.asList(3, 2, 1); // Different order - no match
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Integer> list1 = /*~~>*/Arrays.asList(1, 2, 3); // Matches exactly
                          List<Integer> list2 = Arrays.asList(1, 2, 4); // Different value - no match
                          List<Integer> list3 = Arrays.asList(1, 2); // Different count - no match
                          List<Integer> list4 = Arrays.asList(3, 2, 1); // Different order - no match
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchConstantVarargsWithDifferentArgumentCount() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  // Template expects exactly 2 varargs arguments
                  final JavaTemplate template = JavaTemplate
                    .builder("String.format(\"%s %s\", \"hello\", \"world\")")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  class Foo {
                      void test() {
                          String s1 = String.format("%s %s", "hello", "world"); // Matches exactly
                          String s2 = String.format("%s %s", "hello"); // Missing arg - no match
                          String s3 = String.format("%s %s", "hello", "world", "extra"); // Extra arg - no match
                          String s4 = String.format("%s %s", "hi", "there"); // Different values - no match
                      }
                  }
                  """,
                """
                  class Foo {
                      void test() {
                          String s1 = /*~~>*/String.format("%s %s", "hello", "world"); // Matches exactly
                          String s2 = String.format("%s %s", "hello"); // Missing arg - no match
                          String s3 = String.format("%s %s", "hello", "world", "extra"); // Extra arg - no match
                          String s4 = String.format("%s %s", "hi", "there"); // Different values - no match
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchEmptyVarargsWithConstantTemplate() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  // Template with empty varargs (just the method call)
                  final JavaTemplate template = JavaTemplate
                    .builder("java.util.Arrays.asList()")
                    .build();

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                      return template.matches(getCursor()) ? SearchResult.found(mi) : mi;
                  }
              })),
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list1 = Arrays.asList(); // Matches - empty args
                          List<Object> list2 = Arrays.asList("a"); // No match - has args
                          List<Object> list3 = Arrays.asList("a", "b"); // No match - has args
                      }
                  }
                  """,
                """
                  import java.util.Arrays;
                  import java.util.List;
                  class Foo {
                      void test() {
                          List<Object> list1 = /*~~>*/Arrays.asList(); // Matches - empty args
                          List<Object> list2 = Arrays.asList("a"); // No match - has args
                          List<Object> list3 = Arrays.asList("a", "b"); // No match - has args
                      }
                  }
                  """
              )
            );
        }
    }
}
