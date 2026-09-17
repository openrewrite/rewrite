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
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Edge cases in the Kotlin template mechanism, each minimised to a single root cause.
 */
class KotlinTemplateEdgeCasesTest implements RewriteTest {

    /** Replaces the call to {@code placeholder()} with {@code template}. */
    private void replacePlaceholder(String template, String before, String after) {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return KotlinTemplate.builder(template).build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(before, after)
        );
    }


    /**
     * An invocation is both an {@code Expression} and a {@code Statement}. Taking the expression branch wraps
     * the template as {@code var o : Any = «template»}, which does not parse when the template is a
     * declaration; {@code KotlinBlockStatementTemplateGenerator} wraps in an initializer block instead.
     */
    @Test
    void statementTemplateAtMethodInvocationSite() {
        replacePlaceholder("val y = 5",
          """
            fun placeholder() {}
            fun test() {
                placeholder()
            }
            """,
          """
            fun placeholder() {}
            fun test() {
                val y = 5
            }
            """
        );
    }


    /**
     * {@code #{any(kotlin.String?)}} is rejected by the template-parameter grammar, which is shared with Java.
     * That is no gap: Kotlin cannot overload on nullability alone, so nullability never distinguishes one
     * candidate from another. The non-null spelling substitutes a nullable argument unchanged.
     */
    @Test
    void nullableArgumentSubstitutesThroughNonNullMatcher() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return KotlinTemplate.builder("println(#{any(kotlin.String)})")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              fun placeholder(v: String?): Any = 1
              fun test(x: String?) {
                  val r = placeholder(x)
              }
              """,
            """
              fun placeholder(v: String?): Any = 1
              fun test(x: String?) {
                  val r = println(x)
              }
              """
          )
        );
    }

    /**
     * A use-site target nests the real annotation inside a {@link org.openrewrite.kotlin.tree.K.AnnotationType}
     * belonging to an outer synthetic annotation, which the generator has to look through. All four targets
     * share that shape, so one case covers them.
     */
    @Test
    void replaceArgumentsOfAnnotationWithUseSiteTarget() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                    if (annotation.getArguments() != null && annotation.printTrimmed(getCursor()).contains("old")) {
                        return KotlinTemplate.builder("\"new\"")
                          .build()
                          .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                    }
                    return super.visitAnnotation(annotation, ctx);
                }
            })),
          kotlin(
            "class A { @get:Suppress(\"old\") val x: Int = 1 }",
            "class A { @get:Suppress(\"new\") val x: Int = 1 }"
          )
        );
    }

    /**
     * Nullability is carried structurally by {@link J.NullableType} rather than by
     * {@link org.openrewrite.java.tree.JavaType}, so preservation is asserted by comparing a templated result
     * against the same code parsed directly.
     */
    @Test
    void nullableTypeAttributionMatchesDirectlyParsedCode() {
        String expected = """
          fun placeholder() {}
          fun test() {
              val y: String? = null
          }
          """;

        List<String> fromParser = attribution(KotlinParser.builder().build()
          .parse(expected).map(K.CompilationUnit.class::cast).findFirst().orElseThrow(IllegalStateException::new));

        AtomicReference<List<String>> fromTemplate = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                    K.CompilationUnit c = (K.CompilationUnit) super.visitCompilationUnit(cu, ctx);
                    if (c.printAll().contains("val y")) {
                        fromTemplate.set(attribution(c));
                    }
                    return c;
                }

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    if ("placeholder".equals(method.getSimpleName()) && enclosing != null &&
                        "test".equals(enclosing.getSimpleName())) {
                        return KotlinTemplate.builder("val y: String? = null")
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
                  placeholder()
              }
              """,
            expected
          )
        );

        assertThat(fromParser).containsExactly(
          "NullableType type=kotlin.String", "var y type=kotlin.String");
        assertThat(fromTemplate.get()).isEqualTo(fromParser);
    }

    /** The nullable types and typed variables in a tree, as a comparable summary. */
    private static List<String> attribution(J tree) {
        List<String> found = new ArrayList<>();
        new KotlinVisitor<Integer>() {
            @Override
            public J visitNullableType(J.NullableType nullableType, Integer p) {
                found.add("NullableType type=" + nullableType.getType());
                return super.visitNullableType(nullableType, p);
            }

            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                found.add("var " + variable.getSimpleName() + " type=" + variable.getType());
                return super.visitVariable(variable, p);
            }
        }.visit(tree, 0);
        return found;
    }
}
