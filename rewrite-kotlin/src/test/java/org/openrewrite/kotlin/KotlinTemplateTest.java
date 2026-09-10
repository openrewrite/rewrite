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
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.internal.template.KotlinTemplateStubs;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.Assertions.kotlinScript;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"NullableProblems", "LombokKotlinCompilerPlugin", "SimplifyBooleanWithConstants", "DataFlowIssue", "SequencedCollectionMethodCanBeUsed", "UnusedExpression", "RedundantSemicolon", "RedundantExplicitType", "UnusedReceiverParameter"})
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
    void replaceExpressionStatementWithTemplate() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                    J.Block b = super.visitBlock(block, ctx);
                    return b.withStatements(ListUtils.map(b.getStatements(), s -> {
                        if (!(s instanceof K.ExpressionStatement)) {
                            return s;
                        }
                        return JavaTemplate.builder("#{any()}.hashCode()")
                          .build()
                          .apply(new Cursor(getCursor(), s), s.getCoordinates().replace(), s);
                    }));
                }
            })),
          kotlin(
            """
              fun test(a: Int) {
                  a
              }
              """,
            """
              fun test(a: Int) {
                  a.hashCode()
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
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
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
    void attributesMemberCallOnSubstitutedPlaceholder() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("placeholder".equals(method.getSimpleName())) {
                      J.MethodInvocation applied = KotlinTemplate.builder("require(#{any(java.util.Collection)}.isEmpty())")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                      J.MethodInvocation inner = (J.MethodInvocation) applied.getArguments().get(0);
                      assertThat(inner.getMethodType()).as("inner isEmpty() methodType").isNotNull();
                      assertThat(inner.getName().getType()).as("inner isEmpty() name type").isSameAs(inner.getMethodType());
                      assertThat(applied.getMethodType()).as("enclosing require() methodType").isNotNull();
                      return applied;
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun placeholder(c: java.util.Collection<Int>) {}
              fun test(c: java.util.Collection<Int>) {
                  placeholder(c)
              }
              """,
            """
              fun placeholder(c: java.util.Collection<Int>) {}
              fun test(c: java.util.Collection<Int>) {
                  require(c.isEmpty())
              }
              """
          ));
    }

    @Test
    void attributesPropertyAccessOnSubstitutedPlaceholder() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("placeholder".equals(method.getSimpleName())) {
                      J.MethodInvocation applied = KotlinTemplate.builder("require(#{any(java.util.Collection)}.size == 0)")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                      J.Binary binary = (J.Binary) applied.getArguments().get(0);
                      assertThat(binary.getLeft().getType()).as(".size property access type").isNotNull();
                      return applied;
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun placeholder(c: java.util.Collection<Int>) {}
              fun test(c: java.util.Collection<Int>) {
                  placeholder(c)
              }
              """,
            """
              fun placeholder(c: java.util.Collection<Int>) {}
              fun test(c: java.util.Collection<Int>) {
                  require(c.size == 0)
              }
              """
          ));
    }

    @Test
    void attributesAnyArrayPlaceholder() {
        List<String> stubs = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("placeholder".equals(method.getSimpleName())) {
                      J.MethodInvocation applied = KotlinTemplate.builder("require(#{anyArray(kotlin.String)}.isEmpty())")
                        .doBeforeParseTemplate(stubs::add)
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                      J.MethodInvocation inner = (J.MethodInvocation) applied.getArguments().get(0);
                      assertThat(inner.getMethodType()).as("inner isEmpty() methodType").isNotNull();
                      return applied;
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          kotlin(
            """
              fun placeholder(a: Array<String>) {}
              fun test(a: Array<String>) {
                  placeholder(a)
              }
              """,
            """
              fun placeholder(a: Array<String>) {}
              fun test(a: Array<String>) {
                  require(a.isEmpty())
              }
              """
          ));
        assertThat(stubs).anyMatch(stub -> stub.contains("p<kotlin.Array<kotlin.String>>()"));
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

    @Test
    void replaceMethodParameters() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("foo".equals(method.getSimpleName()) && method.getParameters().get(0) instanceof J.Empty) {
                        return KotlinTemplate.builder("s: String, n: Int")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replaceParameters());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin(
            "fun foo() {}",
            "fun foo(s: String, n: Int) {}"
          )
        );
    }

    @Test
    void replaceClassTypeParameters() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if (classDecl.getTypeParameters() == null) {
                        return KotlinTemplate.builder("T")
                          .build()
                          .apply(getCursor(), classDecl.getCoordinates().replaceTypeParameters());
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
          kotlin(
            "class A",
            "class A<T>"
          )
        );
    }

    /**
     * Kotlin declares a superclass in the same `:` list as its interfaces, so `replaceExtendsClause` addresses
     * the first supertype rather than a separate `extends` slot.
     */
    @Test
    void replaceExtendsClauseTargetsFirstSupertype() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if ("A".equals(classDecl.getSimpleName()) && classDecl.getImplements() == null) {
                        return KotlinTemplate.builder("Base")
                          .build()
                          .apply(getCursor(), classDecl.getCoordinates().replaceExtendsClause());
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
          kotlin(
            """
              open class Base
              class A
              """,
            """
              open class Base
              class A : Base
              """
          )
        );
    }

    @Test
    void replaceImplementsClause() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if ("A".equals(classDecl.getSimpleName()) && classDecl.getImplements() == null) {
                        return KotlinTemplate.builder("Marker")
                          .build()
                          .apply(getCursor(), classDecl.getCoordinates().replaceImplementsClause());
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
          kotlin(
            """
              interface Marker
              class A
              """,
            """
              interface Marker
              class A : Marker
              """
          )
        );
    }

    @Test
    void replaceMethodBody() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("foo".equals(method.getSimpleName()) && !method.printTrimmed(getCursor()).contains("replaced")) {
                        return KotlinTemplate.builder("println(\"replaced\")")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replaceBody());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin(
            """
              fun foo() {
                  println("original")
              }
              """,
            """
              fun foo() {
                  println("replaced")
              }
              """
          )
        );
    }

    /**
     * Kotlin holds top-level declarations directly on the compilation unit rather than in a `J.Block`, so
     * replacing the file's first declaration exercises a path Java never reaches.
     */
    @Test
    void replaceTopLevelFunctionDeclaration() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("old".equals(method.getSimpleName())) {
                        return KotlinTemplate.builder("fun renamed() { println(1) }")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin(
            "fun old() { println(0) }",
            """
              fun renamed() {
                  println(1)
              }
              """
          )
        );
    }

    @Test
    void replaceClassDeclaration() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if ("A".equals(classDecl.getSimpleName())) {
                        return KotlinTemplate.builder("class B")
                          .build()
                          .apply(getCursor(), classDecl.getCoordinates().replace());
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
          kotlin(
            "class A",
            "class B"
          )
        );
    }

    /**
     * A `where` clause wraps the declaration in a `K.ClassDeclaration`, so the stub extractors have to unwrap
     * it rather than assuming a bare `J.ClassDeclaration`.
     */
    @Test
    void replaceTypeParametersOnClassWithWhereClause() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if ("A".equals(classDecl.getSimpleName()) && classDecl.getTypeParameters() != null &&
                        classDecl.getTypeParameters().size() == 1) {
                        return KotlinTemplate.builder("T, U")
                          .build()
                          .apply(getCursor(), classDecl.getCoordinates().replaceTypeParameters());
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
          kotlin(
            "class A<T> where T : Comparable<T>",
            "class A<T, U> where T : Comparable<T>"
          )
        );
    }

    /**
     * An extension function carries a synthetic first parameter for the receiver, which must not be mistaken
     * for a declared parameter when replacing the parameter list.
     */
    @Test
    void replaceParametersOnExtensionFunction() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("foo".equals(method.getSimpleName()) && !method.printTrimmed(getCursor()).contains("n: Int")) {
                        return KotlinTemplate.builder("n: Int")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replaceParameters());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin(
            "fun String.foo() {}",
            "fun String.foo(n: Int) {}"
          )
        );
    }

    @Test
    void replaceBodyOfExpressionBodiedFunction() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("foo".equals(method.getSimpleName()) && !method.printTrimmed(getCursor()).contains("2")) {
                        return KotlinTemplate.builder("return 2")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replaceBody());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin(
            "fun foo(): Int = 1",
            """
              fun foo(): Int {
                  return 2
              }
              """
          )
        );
    }

    /**
     * Kotlin has no checked exceptions, so there is no `throws` clause for this coordinate to address. It
     * should say so rather than failing deep in the parser on a Java stub.
     */
    @Test
    void replaceThrowsReportsThatKotlinHasNoCheckedExceptions() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
          .isThrownBy(() -> new KotlinTemplateStubs().checkedExceptions())
          .withMessageContaining("no checked exceptions");
    }

    @Test
    void replaceLambdaParameters() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                    if (!lambda.getParameters().getParameters().toString().contains("renamed")) {
                        return KotlinTemplate.builder("renamed")
                          .build()
                          .apply(getCursor(), lambda.getParameters().getCoordinates().replace());
                    }
                    return super.visitLambda(lambda, ctx);
                }
            })),
          kotlin(
            """
              fun test() {
                  listOf(1).forEach { it -> println(it) }
              }
              """,
            """
              fun test() {
                  listOf(1).forEach { renamed -> println(it) }
              }
              """
          )
        );
    }

    @Test
    void replacePackageDeclaration() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitPackage(J.Package pkg, ExecutionContext ctx) {
                    if (!pkg.printTrimmed(getCursor()).contains("b")) {
                        return KotlinTemplate.builder("b")
                          .build()
                          .apply(getCursor(), pkg.getCoordinates().replace());
                    }
                    return super.visitPackage(pkg, ctx);
                }
            })),
          kotlin(
            """
              package a

              fun test() {}
              """,
            """
              package b

              fun test() {}
              """
          )
        );
    }

    /**
     * Declared generic types were previously discarded on the way from the builder to the substitutions, so a
     * template parameter typed by one failed with "Unknown type T. Make sure all types are fully qualified."
     */
    @Test
    void parameterTypedByDeclaredGenericType() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return KotlinTemplate.builder("println(#{any(T)})")
                          .genericTypes("T")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              fun placeholder(v: Any): Any = v
              fun <T> test(value: T) {
                  val x = placeholder(value)
              }
              """,
            """
              fun placeholder(v: Any): Any = v
              fun <T> test(value: T) {
                  val x = println(value)
              }
              """
          )
        );
    }

    /**
     * A bound on a declared generic reaches the stub's type parameter list as Kotlin's `T : Bound` syntax.
     */
    @Test
    void boundedGenericTypeReachesTheStub() {
        AtomicReference<String> stub = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return KotlinTemplate.builder("println(#{any(T)})")
                          .genericTypes("T extends java.lang.CharSequence")
                          .doBeforeParseTemplate(stub::set)
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              fun placeholder(v: Any): Any = v
              fun test(value: String) {
                  val x = placeholder(value)
              }
              """,
            """
              fun placeholder(v: Any): Any = v
              fun test(value: String) {
                  val x = println(value)
              }
              """
          )
        );
        assertThat(stub.get()).contains("class Template<T : java.lang.CharSequence>");
    }

    /**
     * A context-free expression template is assigned to a synthetic binding whose declared type is the expected
     * type the expression gets inferred against. `emptyList()` has no element type without one, so the default
     * `Any` leaves it undefined and `bindType` is what makes it resolvable.
     */
    @Test
    void bindTypeSuppliesTheExpectedTypeForInference() {
        assertThat(applyEmptyList(null)).hasToString("kotlin.collections.List<{undefined}>");
        assertThat(applyEmptyList("kotlin.collections.List<kotlin.String>"))
          .hasToString("kotlin.collections.List<kotlin.String>");
    }

    /**
     * Applies {@code emptyList()} over {@code placeholder()} and returns the inferred type of the result.
     * Each call is its own run so that the bind types are compared independently of the context-free stub
     * cache, which {@code JavaTemplateGenericsTest#bindTypeDiscriminatesCachedTemplates} covers separately.
     */
    private JavaType applyEmptyList(@Nullable String bindType) {
        AtomicReference<JavaType> type = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        KotlinTemplate.Builder builder = KotlinTemplate.builder("emptyList()");
                        if (bindType != null) {
                            builder.bindType(bindType);
                        }
                        J result = builder.build().apply(getCursor(), method.getCoordinates().replace());
                        type.set(((Expression) result).getType());
                        return result;
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              fun placeholder(): Any = 1
              fun test() {
                  val x = placeholder()
              }
              """,
            """
              fun placeholder(): Any = 1
              fun test() {
                  val x = emptyList()
              }
              """
          )
        );
        return type.get();
    }

    /** Type parameters on a function, as distinct from the class-level coordinate covered above. */
    @Test
    void replaceMethodTypeParameters() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("test".equals(method.getSimpleName()) && method.getTypeParameters() == null) {
                        return KotlinTemplate.builder("T")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replaceTypeParameters());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin("fun test() {}", "fun <T> test() {}")
        );
    }

    /** Annotating a class reaches a different dummy-scaffold branch than annotating a method. */
    @Test
    void addAnnotationToClass() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if (classDecl.getLeadingAnnotations().isEmpty()) {
                        return KotlinTemplate.builder("@Suppress(\"unused\")")
                          .build()
                          .apply(getCursor(), classDecl.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
          kotlin(
            "class A",
            """
              @Suppress("unused")
              class A
              """
          )
        );
    }

    /**
     * Parameter syntax with no Java counterpart — an annotation, a nullable type, a default value and
     * {@code vararg} — all of which must survive the round trip through the parameter stub.
     */
    @Test
    void replaceParametersWithKotlinOnlyParameterSyntax() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if ("test".equals(method.getSimpleName()) && method.getParameters().get(0) instanceof J.Empty) {
                        return KotlinTemplate.builder("@Suppress(\"x\") n: Int? = null, vararg rest: Int")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replaceParameters());
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            })),
          kotlin(
            "fun test() {}",
            "fun test(@Suppress(\"x\") n: Int? = null, vararg rest: Int) {}"
          )
        );
    }

    // A Kotlin script wraps its statements in a block whose first statement starts at column 0
    private static Recipe insertAtTopLevel(boolean before) {
        return toRecipe(() -> new KotlinVisitor<>() {
            @Override
            public J visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                K.CompilationUnit c = (K.CompilationUnit) super.visitCompilationUnit(cu, ctx);
                J.Block block = (J.Block) c.getStatements().get(0);
                if (block.getStatements().size() != 1) {
                    return c;
                }
                Statement only = block.getStatements().get(0);
                return KotlinTemplate.builder("val y = 2")
                  .build()
                  .apply(updateCursor(c), before ? only.getCoordinates().before() : only.getCoordinates().after());
            }
        });
    }

    @Test
    void insertAfterTopLevelScriptStatement() {
        rewriteRun(
          spec -> spec.recipe(insertAtTopLevel(false)).typeValidationOptions(TypeValidation.none()),
          kotlinScript(
            """
              val x = 1
              """,
            spec -> spec.after(a -> """
              val x = 1
              val y = 2
              """)
          ));
    }

    @Test
    void insertBeforeTopLevelScriptStatement() {
        rewriteRun(
          spec -> spec.recipe(insertAtTopLevel(true)).typeValidationOptions(TypeValidation.none()),
          kotlinScript(
            """
              val x = 1
              """,
            spec -> spec.after(a -> """
              val y = 2
              val x = 1
              """)
          ));
    }
}
