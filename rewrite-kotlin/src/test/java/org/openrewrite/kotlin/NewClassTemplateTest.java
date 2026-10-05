/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NewClassTemplateTest implements RewriteTest {

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite/issues/8214")
    @Test
    void alreadyImportedTypeRendersWithoutStrayLeadingSpace() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return JavaTemplate.builder("new Random()")
                          .imports("java.util.Random")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              import java.util.Random

              fun placeholder(): Any = ""
              fun test() {
                  val x = placeholder()
              }
              """,
            """
              import java.util.Random

              fun placeholder(): Any = ""
              fun test() {
                  val x = Random()
              }
              """
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8214")
    @Test
    void freshlyImportedTypeIsShortenedAndRendersWithoutStrayLeadingSpace() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        maybeAddImport("java.util.Random", false);
                        return JavaTemplate.builder("new java.util.Random()")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              fun placeholder(): Any = ""
              fun test() {
                  val x = placeholder()
              }
              """,
            """
              import java.util.Random

              fun placeholder(): Any = ""
              fun test() {
                  val x = Random()
              }
              """
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8214")
    @Test
    void constructorWithTypeArgumentsRendersWithoutStrayLeadingSpace() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return JavaTemplate.builder("new ArrayList<String>()")
                          .imports("java.util.ArrayList")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              import java.util.ArrayList

              fun placeholder(): Any = ""
              fun test() {
                  val x = placeholder()
              }
              """,
            """
              import java.util.ArrayList

              fun placeholder(): Any = ""
              fun test() {
                  val x = ArrayList<String>()
              }
              """
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8214")
    @Test
    void constructorInArgumentPositionRendersWithoutStrayLeadingSpace() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new KotlinVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("placeholder".equals(method.getSimpleName())) {
                        return JavaTemplate.builder("println(new Random())")
                          .imports("java.util.Random")
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            })),
          kotlin(
            """
              import java.util.Random

              fun placeholder() {}
              fun test() {
                  placeholder()
              }
              """,
            """
              import java.util.Random

              fun placeholder() {}
              fun test() {
                  println(Random())
              }
              """
          ));
    }
}
