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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateMatchTest implements RewriteTest {

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
                  return JavaTemplate.matches("#{i:any(int)} == 1 && #{i} == #{j:any(int)}", getCursor())
                    ? SearchResult.found(binary)
                    : super.visitBinary(binary, ctx);
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
}

