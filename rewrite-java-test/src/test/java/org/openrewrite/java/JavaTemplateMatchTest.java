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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateMatchTest implements RewriteTest {

    @Test
    @Issue("https://github.com/openrewrite/rewrite-templating/pull/91")
    void shouldMatchAbstractStringAssertIsEqualToEmptyString() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion().classpath("assertj-core"))
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                // Mimics what we saw in rewrite-templating
                final JavaTemplate before = JavaTemplate
                  .builder("#{stringAssert:any(org.assertj.core.api.AbstractStringAssert<?>)}.isEqualTo(\"\");")
                  .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                  .build();

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                    return before.matches(getCursor()) ? SearchResult.found(mi) : mi;
                }
            })),
          //language=java
          java(
            """
              import static org.assertj.core.api.Assertions.assertThat;
              class Foo {
                  void test() {
                      assertThat("foo").isEqualTo("");
                  }
              }
              """,
            """
              import static org.assertj.core.api.Assertions.assertThat;
              class Foo {
                  void test() {
                      /*~~>*/assertThat("foo").isEqualTo("");
                  }
              }
              """
          )
        );

    }

    @DocumentExample
    @SuppressWarnings({"ConstantValue", "ConstantConditions"})
    @Test
    void matchBinary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return JavaTemplate.matches("1 == #{any(int)}", getCursor()) ?
                    SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              class Test {
                  boolean b1 = /*~~>*/1 == 2;
                  boolean b2 = /*~~>*/1 == 3;

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void matchNamedParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return JavaTemplate.matches("#{n:any(int)} == #{n}", getCursor()) ?
                    SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 1;
              }
              """,
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = /*~~>*/1 == 1;
              }
              """
          ));
    }

    @Test
    void matchNamedParameterMultipleReferences() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return JavaTemplate.matches("#{i:any(int)} == 1 && #{i} == #{j:any(int)}", getCursor()) ?
                    SearchResult.found(binary) :
                    super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean foo(int i, int j) {
                      return i == 1 && i == j;
                  }
              }
              """,
            """
              class Test {
                  boolean foo(int i, int j) {
                      return /*~~>*/i == 1 && i == j;
                  }
              }
              """
          ));
    }

    @SuppressWarnings({"ConstantValue", "ConstantConditions"})
    @Test
    void extractParameterUsingMatcher() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate template = JavaTemplate.builder("1 == #{any(int)}").build();
              final JavaTemplate replacement = JavaTemplate.builder("Objects.equals(#{any()}, 1)")
                .imports("java.util.Objects")
                .build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  JavaTemplate.Matcher matcher = template.matcher(getCursor());
                  if (matcher.find()) {
                      maybeAddImport("java.util.Objects");
                      return replacement.apply(getCursor(), binary.getCoordinates().replace(), matcher.parameter(0));
                  }
                  return super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              import java.util.Objects;

              class Test {
                  boolean b1 = Objects.equals(2, 1);
                  boolean b2 = Objects.equals(3, 1);

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"ObviousNullCheck"})
    void matchAgainstQualifiedReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate faTemplate = JavaTemplate.builder("java.util.regex.Pattern.UNIX_LINES").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return JavaTemplate.matches("java.util.Objects.requireNonNull(#{any(String)})", getCursor()) ?
                    SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.Import) {
                      return fieldAccess;
                  }
                  return faTemplate.matches(getCursor()) ? SearchResult.found(fieldAccess) : super.visitFieldAccess(fieldAccess, ctx);
              }

              @Override
              public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                  if (ident.getFieldType() == null) {
                      return ident;
                  }
                  return faTemplate.matches(getCursor()) ? SearchResult.found(ident) : super.visitIdentifier(ident, ctx);
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = java.util.Objects.requireNonNull("");
                  String s2 = Objects.requireNonNull("");
                  String s3 = requireNonNull("");

                  int i1 = java.util.regex.Pattern.UNIX_LINES;
                  int i2 = Pattern.UNIX_LINES;
                  int i3 = UNIX_LINES;
              }
              """,
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = /*~~>*/java.util.Objects.requireNonNull("");
                  String s2 = /*~~>*/Objects.requireNonNull("");
                  String s3 = /*~~>*/requireNonNull("");

                  int i1 = /*~~>*/java.util.regex.Pattern.UNIX_LINES;
                  int i2 = /*~~>*/Pattern.UNIX_LINES;
                  int i3 = /*~~>*/UNIX_LINES;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"ObviousNullCheck"})
    void matchAgainstUnqualifiedReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate miTemplate = JavaTemplate.builder("Objects.requireNonNull(#{any(String)})")
                .imports("java.util.Objects").build();
              private final JavaTemplate faTemplate = JavaTemplate.builder("Pattern.UNIX_LINES")
                .imports("java.util.regex.Pattern").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return miTemplate.matches(getCursor()) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.Import) {
                      return fieldAccess;
                  }
                  return faTemplate.matches(getCursor()) ? SearchResult.found(fieldAccess) : super.visitFieldAccess(fieldAccess, ctx);
              }

              @Override
              public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                  if (ident.getFieldType() == null) {
                      return ident;
                  }
                  return faTemplate.matches(getCursor()) ? SearchResult.found(ident) : super.visitIdentifier(ident, ctx);
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = java.util.Objects.requireNonNull("");
                  String s2 = Objects.requireNonNull("");
                  String s3 = requireNonNull("");

                  int i1 = java.util.regex.Pattern.UNIX_LINES;
                  int i2 = Pattern.UNIX_LINES;
                  int i3 = UNIX_LINES;
              }
              """,
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = /*~~>*/java.util.Objects.requireNonNull("");
                  String s2 = /*~~>*/Objects.requireNonNull("");
                  String s3 = /*~~>*/requireNonNull("");

                  int i1 = /*~~>*/java.util.regex.Pattern.UNIX_LINES;
                  int i2 = /*~~>*/Pattern.UNIX_LINES;
                  int i3 = /*~~>*/UNIX_LINES;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"ObviousNullCheck"})
    void matchAgainstStaticallyImportedReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate miTemplate = JavaTemplate.builder("requireNonNull(#{any(String)})")
                .staticImports("java.util.Objects.requireNonNull").build();
              private final JavaTemplate faTemplate = JavaTemplate.builder("UNIX_LINES")
                .staticImports("java.util.regex.Pattern.UNIX_LINES").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return miTemplate.matches(getCursor()) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.Import) {
                      return fieldAccess;
                  }
                  return faTemplate.matches(getCursor()) ? SearchResult.found(fieldAccess) : super.visitFieldAccess(fieldAccess, ctx);
              }

              @Override
              public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                  if (ident.getFieldType() == null) {
                      return ident;
                  }
                  return faTemplate.matches(getCursor()) ? SearchResult.found(ident) : super.visitIdentifier(ident, ctx);
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = java.util.Objects.requireNonNull("");
                  String s2 = Objects.requireNonNull("");
                  String s3 = requireNonNull("");

                  int i1 = java.util.regex.Pattern.UNIX_LINES;
                  int i2 = Pattern.UNIX_LINES;
                  int i3 = UNIX_LINES;
              }
              """,
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = /*~~>*/java.util.Objects.requireNonNull("");
                  String s2 = /*~~>*/Objects.requireNonNull("");
                  String s3 = /*~~>*/requireNonNull("");

                  int i1 = /*~~>*/java.util.regex.Pattern.UNIX_LINES;
                  int i2 = /*~~>*/Pattern.UNIX_LINES;
                  int i3 = /*~~>*/UNIX_LINES;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"UnnecessaryCallToStringValueOf", "RedundantCast"})
    void matchCompatibleTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder("#{any(long)}").build();

              @Override
              public J visitExpression(Expression expression, ExecutionContext ctx) {
                  if (template.matches(getCursor())) {
                      return SearchResult.found(expression);
                  }
                  return super.visitExpression(expression, ctx);
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  void m() {
                      System.out.println(new Object().hashCode());
                      System.out.println((int) new Object().hashCode());
                      System.out.println(Long.parseLong("123"));
                      System.out.println(String.valueOf(Long.parseLong("123")));

                      System.out.println(new Object());
                      System.out.println((1L));
                      System.out.println((long) 1);
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      System.out.println(/*~~>*/new Object().hashCode());
                      System.out.println(/*~~>*/(int) /*~~>*/new Object().hashCode());
                      System.out.println(/*~~>*/Long.parseLong("123"));
                      System.out.println(String.valueOf(/*~~>*/Long.parseLong("123")));

                      System.out.println(new Object());
                      System.out.println(/*~~>*/(/*~~>*/1L));
                      System.out.println(/*~~>*/(long) /*~~>*/1);
                  }
              }
              """)
        );
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void matchMethodInvocationParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                "#{any(java.sql.Statement)}.executeUpdate(#{any(java.lang.String)})"
              ).build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return method.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor()) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              import java.sql.*;
              class Test {
                  void m(StringBuilder sb) throws SQLException {
                      try (Connection c = null) {
                          try (Statement s = c.createStatement()) {
                              s.executeUpdate("foo");
                              s.executeUpdate(sb.toString());
                          }
                      }
                  }
              }
              """,
            """
              import java.sql.*;
              class Test {
                  void m(StringBuilder sb) throws SQLException {
                      try (Connection c = null) {
                          try (Statement s = c.createStatement()) {
                              /*~~>*/s.executeUpdate("foo");
                              /*~~>*/s.executeUpdate(sb.toString());
                          }
                      }
                  }
              }
              """)
        );
    }

    @Test
    void matchExpressionInAssignmentOperation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                "1 + 1"
              ).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return binary.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor()) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  int m() {
                      int i = 1;
                      i += 1 + 1;
                      return i;
                  }
              }
              """,
            """
              class Test {
                  int m() {
                      int i = 1;
                      i += /*~~>*/1 + 1;
                      return i;
                  }
              }
              """)
        );
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void matchExpressionInThrow() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                "\"a\" + \"b\""
              ).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return template.matches(getCursor()) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  int m() {
                      if (true)
                          throw new IllegalArgumentException("a" + "b");
                      else
                          return ("a" + "b").length();
                  }
                  int f = 1;
              }
              """,
            """
              class Test {
                  int m() {
                      if (true)
                          throw new IllegalArgumentException(/*~~>*/"a" + "b");
                      else
                          return (/*~~>*/"a" + "b").length();
                  }
                  int f = 1;
              }
              """)
        );
    }

    @Test
    void matchExpressionInAnnotationAssignment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                "\"a\" + \"b\""
              ).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return binary.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor()) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              @SuppressWarnings(value = {"a" + "b", "c"})
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = {/*~~>*/"a" + "b", "c"})
              class Test {
              }
              """)
        );
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void matchRepeatedParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate before = JavaTemplate
                .builder("#{b1:any(boolean)} || (!#{b1} && #{b2:any(boolean)})").build();
              final JavaTemplate after = JavaTemplate
                .builder("#{b1:any(boolean)} || #{b2:any(boolean)}").build();

              @Override
              public J visitBinary(J.Binary elem, ExecutionContext ctx) {
                  JavaTemplate.Matcher matcher;
                  if ((matcher = before.matcher(getCursor())).find()) {
                      return after.apply(getCursor(), elem.getCoordinates().replace(), matcher.parameter(0), matcher.parameter(1));
                  }
                  return super.visitBinary(elem, ctx);
              }
          })),
          java(
            """
              class T {
                  boolean m(boolean b1, boolean b2) {
                      return b1 || (!b1 && b2);
                  }
              }
              """,
            """
              class T {
                  boolean m(boolean b1, boolean b2) {
                      return b1 || b2;
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.Collections.emptyList()",
      "java.util.Collections.<Object>emptyList()"
    })
    void matchMethodWithGenericType(String templateString) {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                private final JavaTemplate template = JavaTemplate.builder(templateString).build();

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    boolean found = template.matches(getCursor());
                    return found ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
                }
          })),
          java(
              """
              import java.util.Collections;
              import java.util.List;
              class Test {
                  static List<Object> EMPTY_LIST = Collections.emptyList();
              }
              """,
              """
              import java.util.Collections;
              import java.util.List;
              class Test {
                  static List<Object> EMPTY_LIST = /*~~>*/Collections.emptyList();
              }
              """)
        );
    }

    @Test
    void matchMethodWithGenericTypeWithConcreteType() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                private final JavaTemplate template = JavaTemplate.builder("java.util.Collections.<CharSequence>emptyList()").build();

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    boolean found = template.matches(getCursor());
                    return found ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
                }
            })),
          java(
              """
              import java.util.Collections;
              import java.util.List;
              class Test {
                  static List<CharSequence> EXACT_MATCH            = Collections.emptyList();
                  static List<CharSequence> EXACT_MATCH_EXPLICIT   = Collections.<CharSequence>emptyList();
                  static List<String> COLLECTION_OF_SUBTYPE        = Collections.emptyList(); // List of Dogs is not List of Animals
              }
              """,
              """
              import java.util.Collections;
              import java.util.List;
              class Test {
                  static List<CharSequence> EXACT_MATCH            = /*~~>*/Collections.emptyList();
                  static List<CharSequence> EXACT_MATCH_EXPLICIT   = /*~~>*/Collections.<CharSequence>emptyList();
                  static List<String> COLLECTION_OF_SUBTYPE        = Collections.emptyList(); // List of Dogs is not List of Animals
              }
              """)
        );
    }

    @Test
    void matchPrimitiveArrays() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                final JavaTemplate before = JavaTemplate
                  .builder("Arrays.binarySearch(#{a:any(int[])}, #{key:any(int)})")
                  .imports("java.util.Arrays")
                  .build();

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                    return before.matches(getCursor()) ? SearchResult.found(mi) : mi;
                }
            })),
          //language=java
          java(
            """
              import java.util.Arrays;
              class Foo {
                  void test() {
                      Arrays.binarySearch(new int[]{1, 2, 3}, 2);
                      Arrays.binarySearch(new short[]{1, 2, 3}, (short) 2);
                  }
              }
              """,
            """
              import java.util.Arrays;
              class Foo {
                  void test() {
                      /*~~>*/Arrays.binarySearch(new int[]{1, 2, 3}, 2);
                      Arrays.binarySearch(new short[]{1, 2, 3}, (short) 2);
                  }
              }
              """
          )
        );

    }

    @Test
    void matchClassArrays() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                final JavaTemplate before = JavaTemplate
                  .builder("Objects.hash(#{a:any(java.lang.Object[])})")
                  .imports("java.util.Objects")
                  .build();

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                    return before.matches(getCursor()) ? SearchResult.found(mi) : mi;
                }
            })),
          //language=java
          java(
            """
              import java.util.Objects;
              class Foo {
                  void test() {
                      Objects.hash(new Object[5]);
                      Objects.hash(new Object(), new Object()); // varargs not yet supported
                  }
              }
              """,
            """
              import java.util.Objects;
              class Foo {
                  void test() {
                      /*~~>*/Objects.hash(new Object[5]);
                      Objects.hash(new Object(), new Object()); // varargs not yet supported
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("RedundantCast")
    void matchSpecialPrimitives() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                final JavaTemplate nullMatcher = JavaTemplate
                  .builder("System.out.append(#{any(java.lang.CharSequence)})")
                  .build();

                @Override
                public Expression visitExpression(Expression expression, ExecutionContext executionContext) {
                    return nullMatcher.matches(getCursor()) ? SearchResult.found(expression) : super.visitExpression(expression, executionContext);
                }
            })),
          //language=java
          java(
            """
              class Foo {
                  void test() {
                      String a = null;
                      System.out.append(null);
                      System.out.append(a);
                      System.out.append((String) null);
                      System.out.append("Text");
                  }
              }
              """,
            """
              class Foo {
                  void test() {
                      String a = null;
                      /*~~>*/System.out.append(null);
                      /*~~>*/System.out.append(a);
                      /*~~>*/System.out.append((String) null);
                      /*~~>*/System.out.append("Text");
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    void matchBoxedTypes() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                final JavaTemplate template = JavaTemplate
                  .builder("foo.Utils.fromNumber(#{any(java.lang.Number)})")
                  .javaParser(JavaParser.fromJavaVersion().dependsOn("""
                            package foo;
                            public class Utils {
                                public static void fromNumber(Number n) {}
                            }
                            """))
                  .build();

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                    return template.matches(getCursor()) ? SearchResult.found(method) : super.visitMethodInvocation(method, executionContext);
                }
            })),
          //language=java
          java(
            """
              package foo;
              import java.math.BigDecimal;
              import java.util.Collections;
              
              class Utils {
                  public static void fromNumber(Number n) {}
              }

              class Foo {
                  void test() {
                      Utils.fromNumber(null);
                      Utils.fromNumber(1);
                      Utils.fromNumber(2L);
                      Utils.fromNumber(Integer.valueOf(4));
                      Utils.fromNumber(BigDecimal.valueOf(23L));
                  }
              }
              """,
            """
              package foo;
              import java.math.BigDecimal;
              import java.util.Collections;
              
              class Utils {
                  public static void fromNumber(Number n) {}
              }

              class Foo {
                  void test() {
                      /*~~>*/Utils.fromNumber(null);
                      /*~~>*/Utils.fromNumber(1);
                      /*~~>*/Utils.fromNumber(2L);
                      /*~~>*/Utils.fromNumber(Integer.valueOf(4));
                      /*~~>*/Utils.fromNumber(BigDecimal.valueOf(23L));
                  }
              }
              """
          )
        );
    }

    @Test
    void matchUnboxing() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                final JavaTemplate valueOfLong = JavaTemplate
                  .builder("java.lang.Long.valueOf(#{any(long)})")
                  .build();
                final JavaTemplate valueOfDouble = JavaTemplate
                  .builder("java.lang.Double.valueOf(#{any(double)})")
                  .build();

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                    if (valueOfLong.matches(getCursor())) {
                        return SearchResult.found(method, "long");
                    } else if (valueOfDouble.matches(getCursor())) {
                        return SearchResult.found(method, "double");
                    } else {
                        return super.visitMethodInvocation(method, executionContext);
                    }
                }
            })),
          //language=java
          java(
            """
              class Foo {
                  @SuppressWarnings("all")
                  void test() {
                      Long.valueOf(null);
                      Long.valueOf("1");
                      Long.valueOf('a');
                      Long.valueOf((byte) 1);
                      Long.valueOf((short) 1);
                      Long.valueOf(1);
                      Long.valueOf(1L);
                      Long.valueOf(Character.valueOf('a'));
                      Long.valueOf(Byte.valueOf((byte) 1));
                      Long.valueOf(Short.valueOf((short) 1));
                      Long.valueOf(Integer.valueOf(1));
                      Long.valueOf((Long) 1L);
              
                      Double.valueOf(null);
                      Double.valueOf("1.0");
                      Double.valueOf('a');
                      Double.valueOf((byte) 1);
                      Double.valueOf((short) 1);
                      Double.valueOf(1);
                      Double.valueOf(1L);
                      Double.valueOf(1.2f);
                      Double.valueOf(1.2);
                      Double.valueOf(Character.valueOf('a'));
                      Double.valueOf(Byte.valueOf((byte) 1));
                      Double.valueOf(Short.valueOf((short) 1));
                      Double.valueOf(Integer.valueOf(1));
                      Double.valueOf((Long) 1L);
                      Double.valueOf(Float.valueOf(1.2f));
                      Double.valueOf((Double) 1.2);
                  }
              }
              """,
            """
              class Foo {
                  @SuppressWarnings("all")
                  void test() {
                      Long.valueOf(null);
                      Long.valueOf("1");
                      /*~~(long)~~>*/Long.valueOf('a');
                      /*~~(long)~~>*/Long.valueOf((byte) 1);
                      /*~~(long)~~>*/Long.valueOf((short) 1);
                      /*~~(long)~~>*/Long.valueOf(1);
                      /*~~(long)~~>*/Long.valueOf(1L);
                      /*~~(long)~~>*/Long.valueOf(Character.valueOf('a'));
                      /*~~(long)~~>*/Long.valueOf(Byte.valueOf((byte) 1));
                      /*~~(long)~~>*/Long.valueOf(Short.valueOf((short) 1));
                      /*~~(long)~~>*/Long.valueOf(Integer.valueOf(1));
                      /*~~(long)~~>*/Long.valueOf((Long) 1L);
        
                      Double.valueOf(null);
                      Double.valueOf("1.0");
                      /*~~(double)~~>*/Double.valueOf('a');
                      /*~~(double)~~>*/Double.valueOf((byte) 1);
                      /*~~(double)~~>*/Double.valueOf((short) 1);
                      /*~~(double)~~>*/Double.valueOf(1);
                      /*~~(double)~~>*/Double.valueOf(1L);
                      /*~~(double)~~>*/Double.valueOf(1.2f);
                      /*~~(double)~~>*/Double.valueOf(1.2);
                      /*~~(double)~~>*/Double.valueOf(Character.valueOf('a'));
                      /*~~(double)~~>*/Double.valueOf(Byte.valueOf((byte) 1));
                      /*~~(double)~~>*/Double.valueOf(Short.valueOf((short) 1));
                      /*~~(double)~~>*/Double.valueOf(Integer.valueOf(1));
                      /*~~(double)~~>*/Double.valueOf((Long) 1L);
                      /*~~(double)~~>*/Double.valueOf(Float.valueOf(1.2f));
                      /*~~(double)~~>*/Double.valueOf((Double) 1.2);
                  }
              }
              """
          )
        );
    }
}
