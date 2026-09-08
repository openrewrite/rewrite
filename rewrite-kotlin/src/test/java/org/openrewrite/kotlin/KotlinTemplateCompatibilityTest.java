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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Templating against a Kotlin source file has two independent axes, and this test pins every combination of
 * them so no cell is left to assumption:
 * <ul>
 *   <li><b>Snippet language</b> — whether the recipe author reached for {@link JavaTemplate} or
 *       {@link KotlinTemplate}. This decides which parser and which stubs compile the snippet.</li>
 *   <li><b>Visitor type</b> — whether the recipe is written as a {@link JavaIsoVisitor} or a
 *       {@link KotlinVisitor}. This should make no difference at all.</li>
 * </ul>
 * The tree surgery is chosen from the <em>target file</em> rather than from either axis, so a Java snippet
 * applied to a Kotlin file still gets Kotlin-shaped handling.
 */
class KotlinTemplateCompatibilityTest implements RewriteTest {

    /**
     * Rows 1-2: a Java snippet applied to a Kotlin file. Supported for snippets that are trivial to translate —
     * the snippet is parsed by the Java parser and the resulting J nodes are rendered by the Kotlin printer.
     */
    @Nested
    class JavaSnippet {

        @Test
        void fromJavaVisitor() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if ("foo".equals(method.getSimpleName())) {
                            return JavaTemplate.builder("bar()")
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
              )
            );
        }

        /** Same snippet and target, driven from a Kotlin visitor: the visitor type must make no difference. */
        @Test
        void fromKotlinVisitor() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(toRecipe(() -> new KotlinVisitor<>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if ("foo".equals(method.getSimpleName())) {
                            return JavaTemplate.builder("bar()")
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
              )
            );
        }

        /**
         * The supertype coordinate is where a Java snippet most obviously needs Kotlin-shaped surgery: Kotlin
         * renders supertypes after a single {@code :} and never reads the {@code extends} slot, so this would
         * silently make no change if the Java extension were used just because the snippet was Java.
         */
        @Test
        void supertypeCoordinateGetsKotlinTreeSurgery() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        if ("A".equals(classDecl.getSimpleName()) && classDecl.getImplements() == null) {
                            return JavaTemplate.builder("Marker")
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
    }

    /**
     * Rows 3-4: a Kotlin snippet applied to a Kotlin file, including constructs the Java parser could not
     * represent at all. The visitor type must make no difference.
     */
    @Nested
    class KotlinSnippet {

        /**
         * A `when` expression has no Java equivalent, so this can only work through the Kotlin parser. Driven
         * from a plain JavaVisitor to show the snippet language, not the visitor, is what enables it. Note it
         * cannot be an iso visitor: the template replaces a `J.MethodInvocation` with a `K.When`, and an iso
         * visitor is by contract not allowed to change the node type.
         */
        @Test
        void fromJavaVisitor() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(toRecipe(() -> new JavaVisitor<>() {
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
                  fun test(x: Int) {
                      val s = placeholder()
                  }
                  """,
                """
                  fun placeholder(): String = ""
                  fun test(x: Int) {
                      val s = when (x) {
                          1 -> "one";
                          else -> "other"
                      }
                  }
                  """
              )
            );
        }

        /**
         * A null-safe call with a trailing lambda, driven from a Kotlin visitor. Pairs with the case above:
         * between them the two visitor types are covered, and each carries syntax the Java parser could not
         * represent, so neither cell is merely a trivial call.
         */
        @Test
        void fromKotlinVisitor() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(toRecipe(() -> new KotlinVisitor<>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if ("placeholder".equals(method.getSimpleName())) {
                            return KotlinTemplate.builder("s?.let { it.length }")
                              .build()
                              .apply(getCursor(), method.getCoordinates().replace());
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                })),
              kotlin(
                """
                  fun placeholder(): Int? = null
                  fun test(s: String?) {
                      val n = placeholder()
                  }
                  """,
                """
                  fun placeholder(): Int? = null
                  fun test(s: String?) {
                      val n = s?.let { it.length }
                  }
                  """
              )
            );
        }
    }

    /**
     * The context-free stub cache is scoped to the source file, not to a language, and a Kotlin file can
     * legitimately have both kinds of template applied to it. Their stubs are different source in different
     * languages, so the key has to discriminate them — otherwise a JavaTemplate is served the KotlinTemplate's
     * tree and silently "succeeds" on text that is not valid Java.
     */
    @Test
    void javaAndKotlinTemplatesDoNotShareCacheEntries() {
        AtomicReference<String> kotlinResult = new AtomicReference<>();
        AtomicBoolean javaRejected = new AtomicBoolean();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        // `1 as Any` is Kotlin-only syntax, and both templates are given the same bind type so
                        // that every other component of the cache key matches.
                        J fromKotlin = KotlinTemplate.builder("1 as Any")
                          .bindType("java.lang.Object")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                        kotlinResult.set(fromKotlin.printTrimmed(getCursor()));
                        try {
                            JavaTemplate.builder("1 as Any")
                              .bindType("java.lang.Object")
                              .build()
                              .apply(getCursor(), method.getCoordinates().replace());
                        } catch (RuntimeException e) {
                            javaRejected.set(true);
                        }
                        return fromKotlin;
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
                  val x = 1 as Any
              }
              """
          )
        );
        assertThat(kotlinResult.get()).isEqualTo("1 as Any");
        assertThat(javaRejected.get())
          .as("JavaTemplate must reject Kotlin-only syntax rather than reuse the KotlinTemplate's cached tree")
          .isTrue();
    }

    /**
     * Two templates of the same language differing only in their parser's classpath attribute differently, so
     * the cache key carries the parser builder rather than just its type. Without this the second application
     * receives the first's tree and its types come back unresolved.
     */
    @Test
    void classpathDiscriminatesCachedTemplates() {
        AtomicReference<JavaType> withoutClasspath = new AtomicReference<>();
        AtomicReference<JavaType> withClasspath = new AtomicReference<>();
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        KotlinTemplate.Builder bare = KotlinTemplate.builder("jakarta.persistence.Entity::class");
                        bare.parser(KotlinParser.builder());
                        J a = bare.build().apply(getCursor(), method.getCoordinates().replace());
                        withoutClasspath.set(((Expression) a).getType());

                        KotlinTemplate.Builder resolved = KotlinTemplate.builder("jakarta.persistence.Entity::class");
                        resolved.parser(KotlinParser.builder()
                          .classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api"));
                        J b = resolved.build().apply(getCursor(), method.getCoordinates().replace());
                        withClasspath.set(((Expression) b).getType());
                        return b;
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
                  val x = jakarta.persistence.Entity::class
              }
              """
          )
        );
        assertThat(withoutClasspath.get()).hasToString("kotlin.reflect.KClass<{undefined}>");
        assertThat(withClasspath.get()).hasToString("kotlin.reflect.KClass<jakarta.persistence.Entity>");
    }
}
