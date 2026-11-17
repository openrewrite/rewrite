/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScalaTemplateTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return ScalaTemplate.builder("println(\"foo\")")
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          scala(
            """
              class Test {
                  val b1 = 1 == 2
              }
              """,
            """
              class Test {
                  println("foo")
              }
              """
          ));
    }

    @Test
    void replaceExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  if (literal.getValue() instanceof Integer && (Integer) literal.getValue() == 42) {
                      return ScalaTemplate.builder("99")
                        .build()
                        .apply(getCursor(), literal.getCoordinates().replace());
                  }
                  return super.visitLiteral(literal, ctx);
              }
          })),
          scala(
            """
              class Test {
                  val x = 42
              }
              """,
            """
              class Test {
                  val x = 99
              }
              """
          ));
    }

    @Test
    void addStatementToBlockEnd() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  J.ClassDeclaration c = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
                  List<Statement> statements = c.getBody().getStatements();
                  if (statements.stream().noneMatch(s -> s.toString().contains("added"))) {
                      return ScalaTemplate.builder("val added = true")
                        .build()
                        .apply(getCursor(), c.getBody().getCoordinates().lastStatement());
                  }
                  return c;
              }
          })),
          scala(
            """
              class Test {
                  val x = 1
              }
              """,
            """
              class Test {
                  val x = 1
                  val  added = true
              }
              """
          ));
    }

    @Test
    void staticConvenienceApply() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return ScalaTemplate.apply("println(\"static\")", getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          scala(
            """
              class Test {
                  val b1 = 1
              }
              """,
            """
              class Test {
                  println("static")
              }
              """
          ));
    }

    @Test
    void replaceSplatExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitSplatExpression(S.SplatExpression splat, ExecutionContext ctx) {
                  if (splat.getExpression().toString().contains("ys")) {
                      return super.visitSplatExpression(splat, ctx);
                  }
                  return ScalaTemplate.apply("ys: _*", getCursor(), splat.getCoordinates().replace());
              }
          })),
          scala(
            """
              object Test {
                  def f(xs: Int*): Unit = {}
                  val ys = Seq(1, 2, 3)
                  val xs = Seq(4, 5, 6)
                  f(xs: _*)
              }
              """,
            """
              object Test {
                  def f(xs: Int*): Unit = {}
                  val ys = Seq(1, 2, 3)
                  val xs = Seq(4, 5, 6)
                  f(ys: _*)
              }
              """
          ));
    }

    private static Recipe replaceDeprecatedArgument() {
        return toRecipe(() -> new ScalaVisitor<>() {
            @Override
            public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if ("deprecated".equals(annotation.getSimpleName()) &&
                    annotation.getArguments() != null &&
                    annotation.getArguments().stream().noneMatch(a -> a.toString().contains("new"))) {
                    return ScalaTemplate.builder("\"new\"")
                      .build()
                      .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                }
                return annotation;
            }
        });
    }

    @Test
    void replaceAnnotationArgumentsOnMethodWithNestedClassSibling() {
        rewriteRun(
          spec -> spec.recipe(replaceDeprecatedArgument()),
          scala(
            """
              class Test {
                  class Sibling {
                  }

                  @deprecated("old")
                  def foo(): Unit = {}
              }
              """,
            """
              class Test {
                  class Sibling {
                  }

                  @deprecated("new")
                  def foo(): Unit = {}
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnVal() {
        rewriteRun(
          spec -> spec.recipe(replaceDeprecatedArgument()),
          scala(
            """
              class Test {
                  @deprecated("old")
                  val x: Int = 1
              }
              """,
            """
              class Test {
                  @deprecated("new")
                  val x: Int = 1
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnLocalDef() {
        rewriteRun(
          spec -> spec.recipe(replaceDeprecatedArgument()),
          scala(
            """
              class Test {
                  def outer(): Unit = {
                      val a, b: Int = 1
                      val c = 2
                      @deprecated("old")
                      def foo(): Unit = {}
                  }
              }
              """,
            """
              class Test {
                  def outer(): Unit = {
                      val a, b: Int = 1
                      val c = 2
                      @deprecated("new")
                      def foo(): Unit = {}
                  }
              }
              """
          ));
    }

    @Test
    void parenthesizeWithScalaPrecedence() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  String template = switch (method.getSimpleName()) {
                      case "both" -> "#{any(boolean)} & #{any(boolean)}";
                      case "either" -> "#{any(boolean)} | #{any(boolean)}";
                      case "differ" -> "#{any(boolean)} ^ #{any(boolean)}";
                      case "shl" -> "#{any(int)} << #{any(int)}";
                      default -> null;
                  };
                  if (template == null) {
                      return super.visitMethodInvocation(method, ctx);
                  }
                  return ScalaTemplate.builder(template)
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().toArray());
              }
          })),
          scala(
            """
              object T {
                def both(a: Boolean, b: Boolean): Boolean = a & b
                def either(a: Boolean, b: Boolean): Boolean = a | b
                def differ(a: Boolean, b: Boolean): Boolean = a ^ b
                def shl(a: Int, b: Int): Int = a << b
                def f(p: Boolean, x: Boolean, y: Boolean, a: Int, b: Int, c: Int): Unit = {
                  val strict = p && both(x, y)
                  val loose = either(x, y) && p
                  val odd = p && differ(x, y)
                  val shifted = a < shl(b, c)
                }
              }
              """,
            """
              object T {
                def both(a: Boolean, b: Boolean): Boolean = a & b
                def either(a: Boolean, b: Boolean): Boolean = a | b
                def differ(a: Boolean, b: Boolean): Boolean = a ^ b
                def shl(a: Int, b: Int): Int = a << b
                def f(p: Boolean, x: Boolean, y: Boolean, a: Int, b: Int, c: Int): Unit = {
                  val strict = p && (x & y)
                  val loose = (x | y) && p
                  val odd = p && (x ^ y)
                  val shifted = a < (b << c)
                }
              }
              """
          ));
    }

    @Test
    void parameterOfParameterizedType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitSwitch(J.Switch s, ExecutionContext ctx) {
                  return ScalaTemplate.builder("#{any()}.getOrElse(\"d\")")
                    .build()
                    .apply(getCursor(), s.getCoordinates().replace(), s.getSelector().getTree());
              }
          })),
          scala(
            """
              class Example {
                  val opt: Option[String] = Some("hello")
                  val value = opt match {
                    case Some(x) => x
                    case None => "default"
                  }
              }
              """,
            """
              class Example {
                  val opt: Option[String] = Some("hello")
                  val value = opt.getOrElse("d")
              }
              """
          ));
    }

    @Test
    void parameterTypesInScalaSyntax() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!"id".equals(method.getSimpleName())) {
                      return super.visitMethodInvocation(method, ctx);
                  }
                  return ScalaTemplate.builder("#{any()}.toString()")
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
              }
          })),
          scala(
            """
              object T {
                def id[A](a: A): A = a
                def f(ints: Array[Int], nested: Array[Array[String]], map: Map[String, Int],
                      wild: java.util.List[_ <: Number], sup: java.util.List[_ >: Integer],
                      union: List[String] | Int): Unit = {
                  val a = id(ints)
                  val b = id(nested)
                  val c = id(map)
                  val d = id(wild)
                  val e = id(sup)
                  val f = id(union)
                }
              }
              """,
            """
              object T {
                def id[A](a: A): A = a
                def f(ints: Array[Int], nested: Array[Array[String]], map: Map[String, Int],
                      wild: java.util.List[_ <: Number], sup: java.util.List[_ >: Integer],
                      union: List[String] | Int): Unit = {
                  val a = ints.toString()
                  val b = nested.toString()
                  val c = map.toString()
                  val d = wild.toString()
                  val e = sup.toString()
                  val f = union.toString()
                }
              }
              """,
            // ScalaVisitor swallows exceptions thrown while visiting a top-level statement, so assert after the visit
            spec -> spec.afterRecipe(cu -> assertThat(new ScalaIsoVisitor<List<String>>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<String> unattributed) {
                    if (method.getMethodType() == null) {
                        unattributed.add(method.toString());
                    }
                    return method;
                }
            }.reduce(cu, new ArrayList<>())).isEmpty())
          ));
    }
}
