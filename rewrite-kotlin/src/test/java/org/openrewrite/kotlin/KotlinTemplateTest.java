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
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
    void wrapControlFlowReturnExpressionWithBuild() {
        // A `return if (...) ... else ...` parses the `if` into a K.StatementExpression in the
        // return's expression slot. Wrapping it with a `#{any(...)}.build()` template (as
        // rewrite-testing-frameworks' UpdateMockWebServerDispatcher does) previously left the
        // K.StatementExpression untouched, so callers casting the result to J.MethodInvocation hit a
        // ClassCastException. The wrapper must be a first-class visited node for the scope-based
        // template replacement to target it.
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.Return visitReturn(J.Return aReturn, ExecutionContext ctx) {
                    J.Return r = super.visitReturn(aReturn, ctx);
                    Expression expr = r.getExpression();
                    if (!(expr instanceof K.StatementExpression)) {
                        return r;
                    }
                    J.MethodInvocation wrapped = JavaTemplate.builder("#{any(a.b.Resp)}.build()")
                      .build()
                      .apply(new Cursor(getCursor(), expr), expr.getCoordinates().replace(), expr);
                    return r.withExpression(wrapped);
                }
            })),
          kotlin(
            """
              package a.b
              class Resp {
                  fun build(): Resp = this
                  companion object {
                      fun ok(): Resp = Resp()
                      fun notFound(): Resp = Resp()
                  }
              }
              fun dispatch(path: String): Resp {
                  return if (path == "/") {
                      Resp.ok()
                  } else {
                      Resp.notFound()
                  }
              }
              """,
            """
              package a.b
              class Resp {
                  fun build(): Resp = this
                  companion object {
                      fun ok(): Resp = Resp()
                      fun notFound(): Resp = Resp()
                  }
              }
              fun dispatch(path: String): Resp {
                  return if (path == "/") {
                      Resp.ok()
                  } else {
                      Resp.notFound()
                  }.build()
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

    @Test
    void replaceAnnotationArgumentsOnMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              class Test {
                  @Suppress("UNCHECKED_CAST")
                  fun foo() {
                  }
              }
              """,
            """
              class Test {
                  @Suppress("RedundantSuppression")
                  fun foo() {
                  }
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnMethodWithKotlinOnlyClassMembers() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null && annotation.getArguments().size() == 1) {
                      return KotlinTemplate.builder("#{any()}, \"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments(),
                          annotation.getArguments().getFirst());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              interface Api {
                  fun fetch(): String
              }
              """
          ),
          kotlin(
            """
              class Gateway(private val endpoint: String) : Api {
                  companion object {
                      const val REASON = "UNCHECKED_CAST"
                  }

                  enum class Status { ACTIVE, INACTIVE }

                  init {
                      require(endpoint.isNotEmpty())
                  }

                  @Suppress(REASON)
                  override fun fetch(): String = endpoint
              }
              """,
            """
              class Gateway(private val endpoint: String) : Api {
                  companion object {
                      const val REASON = "UNCHECKED_CAST"
                  }

                  enum class Status { ACTIVE, INACTIVE }

                  init {
                      require(endpoint.isNotEmpty())
                  }

                  @Suppress(REASON, "RedundantSuppression")
                  override fun fetch(): String = endpoint
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnMethodWithParameterSubstitution() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null && annotation.getArguments().size() == 1) {
                      return KotlinTemplate.builder("#{any()}, \"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments(),
                          annotation.getArguments().getFirst());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              class Test {
                  @Suppress("UNCHECKED_CAST")
                  fun foo() {
                  }
              }
              """,
            """
              class Test {
                  @Suppress("UNCHECKED_CAST", "RedundantSuppression")
                  fun foo() {
                  }
              }
              """
          ));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/2824")
    @Test
    void replaceAnnotationArgumentsWithWildcardTypedSubstitution() {
        List<String> stubs = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Retry".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null && !annotation.getArguments().isEmpty() &&
                      annotation.getArguments().getFirst() instanceof J.Assignment assignment &&
                      assignment.getVariable() instanceof J.Identifier attribute &&
                      "include".equals(attribute.getSimpleName())) {
                      return KotlinTemplate.builder("includes = #{any()}")
                        .doBeforeParseTemplate(stubs::add)
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments(),
                          assignment.getAssignment());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              import kotlin.reflect.KClass

              annotation class Retry(
                  val include: Array<KClass<out Throwable>> = [],
                  val includes: Array<KClass<out Throwable>> = []
              )
              """
          ),
          kotlin(
            """
              class MyService {
                  @Retry(include = [IllegalStateException::class])
                  fun doWork() {}
              }
              """,
            """
              class MyService {
                  @Retry(includes = [IllegalStateException::class])
                  fun doWork() {}
              }
              """
          ));
        assertThat(stubs).anyMatch(stub -> stub.contains("p<kotlin.Array<kotlin.reflect.KClass<out kotlin.Throwable>>>()"));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/2824")
    @Test
    void contravariantAndStarProjectionTypedSubstitution() {
        List<String> stubs = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().getFirst().getSimpleName().startsWith("x")) {
                      return KotlinTemplate.builder("println(#{any()})")
                        .doBeforeParseTemplate(stubs::add)
                        .build()
                        .apply(getCursor(), multiVariable.getCoordinates().replace(),
                          multiVariable.getVariables().getFirst().getInitializer());
                  }
                  return multiVariable;
              }
          })),
          kotlin(
            """
              import kotlin.reflect.KClass

              fun test(c: Comparator<in String>, k: KClass<*>) {
                  val x1 = c
                  val x2 = k
              }
              """,
            """
              import kotlin.reflect.KClass

              fun test(c: Comparator<in String>, k: KClass<*>) {
                  println(c)
                  println(k)
              }
              """
          ));
        assertThat(stubs).anyMatch(stub -> stub.contains("Comparator<in kotlin.String>"));
        assertThat(stubs).anyMatch(stub -> stub.contains("KClass<*>"));
    }

    @Test
    void replaceAnnotationArgumentsOnProperty() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              class Test {
                  @Suppress("UNCHECKED_CAST")
                  val foo: Int = 0
              }
              """,
            """
              class Test {
                  @Suppress("RedundantSuppression")
                  val foo: Int = 0
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              @Suppress("UNCHECKED_CAST")
              class Test
              """,
            """
              @Suppress("RedundantSuppression")
              class Test
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnTopLevelFunction() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              @Suppress("UNCHECKED_CAST")
              fun foo() {
              }
              """,
            """
              @Suppress("RedundantSuppression")
              fun foo() {
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnLocalVariableInFunctionWithReturnType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo(): Int {
                      @Suppress("UNCHECKED_CAST")
                      val x = 0
                      return x
                  }
              }
              """,
            """
              class Test {
                  fun foo(): Int {
                      @Suppress("RedundantSuppression")
                      val x = 0
                      return x
                  }
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnLocalVariablePrecededByTypedLocal() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                      val y: Int = 1
                      @Suppress("UNCHECKED_CAST")
                      val x = y
                  }
              }
              """,
            """
              class Test {
                  fun foo() {
                      val y: Int = 1
                      @Suppress("RedundantSuppression")
                      val x = y
                  }
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnLocalVariablePrecededByUntypedLocal() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Suppress".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("RedundantSuppression"))) {
                      return KotlinTemplate.builder("\"RedundantSuppression\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                      val y = 1
                      @Suppress("UNCHECKED_CAST")
                      val x = y
                  }
              }
              """,
            """
              class Test {
                  fun foo() {
                      val y = 1
                      @Suppress("RedundantSuppression")
                      val x = y
                  }
              }
              """
          ));
    }

    @Test
    void addAnnotationToMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return KotlinTemplate.builder("@Suppress(\"RedundantSuppression\")")
                        .build()
                        .apply(getCursor(), method.getCoordinates().addAnnotation(
                          Comparator.comparing(J.Annotation::getSimpleName)));
                  }
                  return method;
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                  }
              }
              """,
            """
              class Test {
                  @Suppress("RedundantSuppression")
                  fun foo() {
                  }
              }
              """
          ));
    }
}
