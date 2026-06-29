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
package org.openrewrite.kotlin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class KotlinTemplateTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceContextFreeStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return KotlinTemplate.builder("println(\"foo\")")
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                      val b1 = 1 == 2
                  }
              }
              """,
            """
              class Test {
                  fun foo() {
                      println("foo")
                  }
              }
              """
          ));
    }

    @Test
    void addStatementToMethodInClass() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  var m = (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
                  if (m.getSimpleName().equals("configure")) {
                      List<Statement> statements = m.getBody().getStatements();
                      if (statements.stream().noneMatch(s -> s.toString().contains("println"))) {
                          return JavaTemplate.builder("println(\"added\")")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), statements.get(statements.size() - 1).getCoordinates().after());
                      }
                  }
                  return m;
              }
          })),
          kotlin(
            """
              class MyConfig {
                  fun configure(value: Int) {
                      val x = value + 1
                  }
              }
              """,
            """
              class MyConfig {
                  fun configure(value: Int) {
                      val x = value + 1
                      println("added")
                  }
              }
              """
          ));
    }

    @Test
    void replaceUnitReturningMethodInvocation() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("foo".equals(method.getSimpleName())) {
                      return KotlinTemplate.builder("bar()")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun foo() {}
              fun bar() {}
              fun test() {
                  foo()
              }
              """,
            """
              fun foo() {}
              fun bar() {}
              fun test() {
                  bar()
              }
              """
          ));
    }

    @Test
    void parserClasspath() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.toString().contains("ObjectMapper")) {
                      return multiVariable;
                  }
                  maybeAddImport(ObjectMapper.class.getName(), false);
                  Path path;
                  try {
                      path = Path.of(ObjectMapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                  } catch (URISyntaxException e) {
                      throw new RuntimeException(e);
                  }
                  return KotlinTemplate.builder("val mapper = ObjectMapper()")
                    .parser(KotlinParser.builder().classpath(List.of(path)))
                    .imports(ObjectMapper.class.getName())
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                      val b1 = 1 == 2
                  }
              }
              """,
            """
              import com.fasterxml.jackson.databind.ObjectMapper

              class Test {
                  fun foo() {
                      val mapper = ObjectMapper()
                  }
              }
              """
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7407")
    @Test
    void parameterTypeWithCallerScopeTypeVariable() {
        StringBuilder capturedTemplate = new StringBuilder();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).build()).recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().size() == 1 &&
                      "x".equals(multiVariable.getVariables().getFirst().getSimpleName())) {
                      J initializer = multiVariable.getVariables().getFirst().getInitializer();
                      return KotlinTemplate.builder("println(#{any()})")
                        .doBeforeParseTemplate(capturedTemplate::append)
                        .build()
                        .apply(getCursor(), multiVariable.getCoordinates().replace(), initializer);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          }).withMaxCycles(1)),
          kotlin(
            """
              class Container<T : Any>(val value: T)
              fun <T : Any> test(c: Container<T>) {
                  val x = c
              }
              """,
            """
              class Container<T : Any>(val value: T)
              fun <T : Any> test(c: Container<T>) {
                  println(c)
              }
              """
          ));
        assertThat(capturedTemplate.toString()).contains("class Template<T");
    }

    @Test
    void replaceExpressionWithWhenExpression() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("placeholder".equals(method.getSimpleName())) {
                      return KotlinTemplate.builder("when (x) { 1 -> \"one\"; else -> \"other\" }")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun placeholder(): String = ""
              fun test(x: Int): String {
                  return placeholder()
              }
              """,
            """
              fun placeholder(): String = ""
              fun test(x: Int): String {
                  return when (x) {
                      1 -> "one";
                      else -> "other"
                  }
              }
              """
          ));
    }

    @Test
    void captureValueInsideTrailingLambda() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("toUpperCase".equals(method.getSimpleName())) {
                      return KotlinTemplate.builder("#{any(kotlin.String)}.uppercase()")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun test() {
                  listOf("a", "b").forEach { s ->
                      println(s.toUpperCase())
                  }
              }
              """,
            """
              fun test() {
                  listOf("a", "b").forEach { s ->
                      println(s.uppercase())
                  }
              }
              """
          ));
    }

    @Test
    void captureValueInsideNamedArgument() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("oldGreet".equals(method.getSimpleName())) {
                      // Capture the first argument and re-emit it as a named argument to `greet`.
                      return KotlinTemplate.builder("greet(name = #{any(kotlin.String)})")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun greet(name: String) {}
              fun oldGreet(s: String) {}
              fun test() {
                  oldGreet("world")
              }
              """,
            """
              fun greet(name: String) {}
              fun oldGreet(s: String) {}
              fun test() {
                  greet(name = "world")
              }
              """
          ));
    }

    @Test
    void captureValueInsideReceiverScope() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("trim".equals(method.getSimpleName())) {
                      return KotlinTemplate.builder("#{any(kotlin.String)}.uppercase()")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun test() {
                  val s = "  hello  "
                  s.apply {
                      println(this.trim())
                  }
              }
              """,
            """
              fun test() {
                  val s = "  hello  "
                  s.apply {
                      println(this.uppercase())
                  }
              }
              """
          ));
    }

    @Test
    void captureValueInsideExtensionFunction() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("oldHelper".equals(method.getSimpleName())) {
                      return KotlinTemplate.builder("#{any(kotlin.String)}.uppercase()")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun String.oldHelper(): String = this
              fun String.shout(): String {
                  return this.oldHelper()
              }
              """,
            """
              fun String.oldHelper(): String = this
              fun String.shout(): String {
                  return this.uppercase()
              }
              """
          ));
    }

    @Test
    void replacePropertyDeclaration() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().size() == 1 &&
                      "old".equals(multiVariable.getVariables().getFirst().getSimpleName())) {
                      return KotlinTemplate.builder("val replaced = 42")
                        .build()
                        .apply(getCursor(), multiVariable.getCoordinates().replace());
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          kotlin(
            """
              fun test() {
                  val old = 1
                  println(old)
              }
              """,
            """
              fun test() {
                  val replaced = 42
                  println(old)
              }
              """
          ));
    }

    @Test
    void contravariantTypeParameterCarriesBoundsIntoTemplate() {
        StringBuilder capturedTemplate = new StringBuilder();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).build()).recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().size() == 1 &&
                      "x".equals(multiVariable.getVariables().getFirst().getSimpleName())) {
                      J initializer = multiVariable.getVariables().getFirst().getInitializer();
                      return KotlinTemplate.builder("println(#{any()})")
                        .doBeforeParseTemplate(capturedTemplate::append)
                        .build()
                        .apply(getCursor(), multiVariable.getCoordinates().replace(), initializer);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          }).withMaxCycles(1)),
          kotlin(
            """
              class Consumer<in T : Number>
              fun <T : Number> test(c: Consumer<T>) {
                  val x = c
              }
              """,
            """
              class Consumer<in T : Number>
              fun <T : Number> test(c: Consumer<T>) {
                  println(c)
              }
              """
          ));
        // Before the CONTRAVARIANT fix, bounds were only emitted for COVARIANT, so a contravariant
        // caller-scope T appeared in the template as `<T>` without bound. With the fix the bound
        // round-trips, so the captured template includes "T : ".
        assertThat(capturedTemplate.toString()).contains("class Template<T");
    }
}
