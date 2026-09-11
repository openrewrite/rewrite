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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Context-sensitive Kotlin templates build their stub by printing the whole enclosing source file with the
 * template substituted at the insertion point, eliding only what is provably safe to elide — rather than
 * reconstructing the enclosing scope bottom-up as the Java implementation does.
 * <p>
 * The tests split in two. The behavioural tests assert that a template referencing symbols from its insertion
 * scope resolves. The stub-shape tests pin the elision rules directly, so a regression in what gets elided is
 * caught as such rather than surfacing as a confusing parse failure.
 */
class KotlinContextSensitiveTemplateTest implements RewriteTest {

    /**
     * Replaces the call to {@code placeholder()} with {@code template}, capturing the generated stub.
     */
    private static org.openrewrite.Recipe replacePlaceholder(String template, AtomicReference<String> stub) {
        return toRecipe(() -> new KotlinVisitor<>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if ("placeholder".equals(method.getSimpleName())) {
                    return KotlinTemplate.builder(template)
                      .contextSensitive()
                      .doBeforeParseTemplate(stub::set)
                      .build()
                      .apply(getCursor(), method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }

    @Test
    void referencesPrecedingLocalVariable() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(local)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun test(param: String) {
                  val local = param.length
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun test(param: String) {
                  val local = param.length
                  println(local)
              }
              """
          )
        );
    }

    @Test
    void referencesFunctionParameterAndClassProperty() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(field + param)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              class Holder(val field: String) {
                  fun test(param: String) {
                      placeholder()
                  }
              }
              """,
            """
              fun placeholder() {}
              class Holder(val field: String) {
                  fun test(param: String) {
                      println(field + param)
                  }
              }
              """
          )
        );
    }

    /**
     * The headline case. Java's generator replaces preceding initializers with dummy values, which destroys the
     * smart cast; printing the enclosing scope verbatim preserves it, so the template can call a member that
     * only exists on the narrowed type.
     */
    @Test
    void smartCastIsLiveAtTheHole() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(x.length)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun test(x: Any) {
                  if (x is String) {
                      placeholder()
                  }
              }
              """,
            """
              fun placeholder() {}
              fun test(x: Any) {
                  if (x is String) {
                      println(x.length)
                  }
              }
              """
          )
        );
    }

    @Test
    void whenSubjectBindingIsVisible() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(y)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun source(): Int = 1
              fun test() {
                  when (val y = source()) {
                      1 -> placeholder()
                      else -> {}
                  }
              }
              """,
            """
              fun placeholder() {}
              fun source(): Int = 1
              fun test() {
                  when (val y = source()) {
                      1 -> println(y)
                      else -> {}
                  }
              }
              """
          )
        );
    }

    @Test
    void destructuringDeclarationBeforeTheHole() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(a + b)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun test(pair: Pair<Int, Int>) {
                  val (a, b) = pair
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun test(pair: Pair<Int, Int>) {
                  val (a, b) = pair
                  println(a + b)
              }
              """
          )
        );
    }

    @Test
    void genericTypeVariableFromEnclosingFunction() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("val copy: T = value", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun <T> test(value: T) {
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun <T> test(value: T) {
                  val copy: T = value
              }
              """
          )
        );
    }

    @Test
    void genericTypeVariableFromWhereClause() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("val copy: T = value", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun <T> test(value: T) where T : Comparable<T> {
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun <T> test(value: T) where T : Comparable<T> {
                  val copy: T = value
              }
              """
          )
        );
    }

    @Test
    void extensionReceiverAndThis() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(this.length)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              fun String.test() {
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun String.test() {
                  println(this.length)
              }
              """
          )
        );
    }

    @Test
    void initBlockAndSecondaryConstructorPreserveDefiniteAssignment() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(assigned)", new AtomicReference<>())),
          kotlin(
            """
              fun placeholder() {}
              class Holder {
                  val assigned: Int
                  init {
                      assigned = 1
                  }

                  constructor(other: Int) {
                      println(other)
                  }

                  fun test() {
                      placeholder()
                  }
              }
              """,
            """
              fun placeholder() {}
              class Holder {
                  val assigned: Int
                  init {
                      assigned = 1
                  }

                  constructor(other: Int) {
                      println(other)
                  }

                  fun test() {
                      println(assigned)
                  }
              }
              """
          )
        );
    }

    // --- stub shape: the elision rules, pinned directly ---

    @Test
    void elidesBodyOfFunctionWithDeclaredReturnType() {
        AtomicReference<String> stub = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(1)", stub)),
          kotlin(
            """
              fun placeholder() {}
              fun declared(): String { return "y" }
              fun test() {
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun declared(): String { return "y" }
              fun test() {
                  println(1)
              }
              """
          )
        );
        assertThat(stub.get()).contains("fun declared(): String { kotlin.TODO() }");
    }

    /**
     * R6. Eliding an inferred return type to {@code = kotlin.TODO()} would infer {@code Nothing} and silently
     * mis-attribute every reference to the function, so the body must survive.
     */
    @Test
    void doesNotElideFunctionWithInferredReturnType() {
        AtomicReference<String> stub = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(1)", stub)),
          kotlin(
            """
              fun placeholder() {}
              fun inferred() = "x"
              fun test() {
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun inferred() = "x"
              fun test() {
                  println(1)
              }
              """
          )
        );
        assertThat(stub.get()).contains("fun inferred() = \"x\"");
    }

    @Test
    void doesNotElideConstOrDelegatedProperty() {
        AtomicReference<String> stub = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(1)", stub)),
          kotlin(
            """
              fun placeholder() {}
              const val MAX = 10
              val lazyProp: String by lazy { "z" }
              fun test() {
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              const val MAX = 10
              val lazyProp: String by lazy { "z" }
              fun test() {
                  println(1)
              }
              """
          )
        );
        assertThat(stub.get())
          .contains("const val MAX = 10")
          .contains("val lazyProp: String by lazy { \"z\" }");
    }

    /**
     * R4. Nothing after the hole can be referenced from it, because Kotlin has no forward references for local
     * declarations — so dropping the tail keeps the stub smaller without losing anything the template can see.
     */
    @Test
    void dropsStatementsAfterTheHole() {
        AtomicReference<String> stub = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(replacePlaceholder("println(1)", stub)),
          kotlin(
            """
              fun placeholder() {}
              fun test() {
                  val before = 1
                  placeholder()
                  val after = 2
              }
              """,
            """
              fun placeholder() {}
              fun test() {
                  val before = 1
                  println(1)
                  val after = 2
              }
              """
          )
        );
        assertThat(stub.get()).contains("val before = 1").doesNotContain("val after");
    }

    /**
     * Annotation arguments must be compile-time constants, so no placeholder can stand in for the surrounding
     * scope. Java has an open TODO admitting it gets this case wrong; we reject it instead.
     */
    @Test
    void annotationArgumentsAreRejectedWithAnActionableError() {
        assertThatExceptionOfType(Throwable.class)
          .isThrownBy(() -> rewriteRun(
            spec -> spec.typeValidationOptions(TypeValidation.none())
              .recipe(toRecipe(() -> new KotlinVisitor<>() {
                  @Override
                  public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                      if (getCursor().firstEnclosing(J.Annotation.class) != null) {
                          return KotlinTemplate.builder("\"replaced\"")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), literal.getCoordinates().replace());
                      }
                      return super.visitLiteral(literal, ctx);
                  }
              })),
            kotlin(
              """
                annotation class Ann(val value: String)

                @Ann("x")
                fun test() {}
                """
            )
          ))
          .withStackTraceContaining("compile-time constants");
    }

    /**
     * The inherited builder methods returned {@code JavaTemplate.Builder}, so any of them appearing before a
     * Kotlin-specific method ended the chain and would not compile. This pins the narrowed return types: the
     * body below is the assertion, since a regression is a compile error rather than a test failure.
     */
    @Test
    void builderStaysChainableAfterInheritedMethods() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return KotlinTemplate.builder("println(local)")
                          .contextSensitive()
                          .doBeforeParseTemplate(s -> {
                          })
                          .imports("kotlin.collections.List")
                          .parser(KotlinParser.builder())
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              fun placeholder() {}
              fun test() {
                  val local = 1
                  placeholder()
              }
              """,
            """
              fun placeholder() {}
              fun test() {
                  val local = 1
                  println(local)
              }
              """
          )
        );
    }

    @Test
    void javaParserIsRejectedRatherThanSilentlyDiscarded() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
          .isThrownBy(() -> KotlinTemplate.builder("println(1)")
            .javaParser(org.openrewrite.java.JavaParser.fromJavaVersion()))
          .withMessageContaining("parser(KotlinParser.Builder)");
    }
}
