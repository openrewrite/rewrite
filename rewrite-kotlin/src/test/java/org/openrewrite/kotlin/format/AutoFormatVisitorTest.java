/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.AddImportTest;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.kotlin.Assertions.kotlin;

class AutoFormatVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat());
    }

    @DocumentExample
    @Test
    void keepMaximumBetweenHeaderAndPackage() {
        rewriteRun(
          kotlin(
            """
              /*
               * This is a sample file.
               */




              package com.intellij.samples

              class Test {
              }
              """,
            """
              /*
               * This is a sample file.
               */


              package com.intellij.samples

              class Test {
              }
              """
          )
        );
    }

    @Test
    void primaryConstructor() {
        rewriteRun(
          kotlin(
            """
              package t

              class A(
                  val a: Boolean,
                  val b: Boolean,
                  val c: Boolean,
                  val d: Boolean
              ): Any()
              """
          )
        );
    }

    @Test
    void tabsAndIndents() {
        rewriteRun(
          kotlin(
            """
              open class Some {
                  private val f: (Int) -> Int = { a: Int -> a * 2 }
                  fun foo(): Int {
                  val test: Int = 12
                  for (i in 10..42) {
                  println(when {
                  i < test -> -1
                  i > test -> 1
                  else -> 0
                  })
                  }
                  if (true) {
                  }
                  while (true) {
                      break
                  }
                  try {
                      when (test) {
                          12 -> println("foo")
                          in 10..42 -> println("baz")
                          else -> println("bar")
                      }
                  } catch (e: Exception) {
                  } finally {
                  }
                  return test
                  }

                  fun multilineMethod(
                          foo: String,
                          bar: String
                          ) {
                      foo
                              .length
                  }

                  fun expressionBodyMethod() =
                  "abc"
              }

              class AnotherClass<T : Any> : Some()
              """,
            """
              open class Some {
                  private val f: (Int) -> Int = { a: Int -> a * 2 }
                  fun foo(): Int {
                      val test: Int = 12
                      for (i in 10..42) {
                          println(when {
                              i < test -> -1
                              i > test -> 1
                              else -> 0
                          })
                      }
                      if (true) {
                      }
                      while (true) {
                          break
                      }
                      try {
                          when (test) {
                              12 -> println("foo")
                              in 10..42 -> println("baz")
                              else -> println("bar")
                          }
                      } catch (e: Exception) {
                      } finally {
                      }
                      return test
                  }

                  fun multilineMethod(
                          foo: String,
                          bar: String
                  ) {
                      foo
                          .length
                  }

                  fun expressionBodyMethod() =
                      "abc"
              }

              class AnotherClass<T : Any> : Some()
              """
          )
        );
    }

    @Test
    void classConstructor() {
        rewriteRun(
          kotlin(
            """
              package com.netflix.graphql.dgs.client.codegen

              class BaseProjectionNode (
                      val type: Int = 1
              ) {
              }
              """
          )
        );
    }

    @Test
    void composite() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath("junit-jupiter-api")),
          kotlin(
            """
              package com.netflix.graphql.dgs.client.codegen

              import org.junit.jupiter.api.Test

              class GraphQLMultiQueryRequestTest {

                  @Suppress
                  @Test
                  fun testSerializeInputClassWithProjectionAndMultipleQueries() {
                  }

                  public fun me() {
                  }
              }
              """
          )
        );
    }

    @Test
    void extensionMethod() {
        rewriteRun(
          kotlin(
            """
              fun String.extension(): Any {
                  return ""
              }
              """
          )
        );
    }

    @Test
    void extensionProperty() {
        rewriteRun(
          kotlin(
            """
              val String.extension: Any
                  get() = ""
              """
          )
        );
    }

    @Test
    void trailingLambda() {
        rewriteRun(
          kotlin(
            """
              val x = "foo".let {
                  it.length
              }
              """
          )
        );
    }

    @Test
    void trailingLambdaWithParam() {
        rewriteRun(
          kotlin(
            """
              val x = listOf(1).forEach { e -> println(e) }
              """
          )
        );
    }

    @Test
    void trailingLambdaWithParamTrailingComment() {
        rewriteRun(
          kotlin(
            """
              val x = listOf(1).forEach { e,->println(e) }
              """
          )
        );
    }

    @Test
    void trailingLambdaWithMethodRefParam() {
        rewriteRun(
          kotlin(
            """
              val x = listOf(1).forEach(::println)
              """
          )
        );
    }

    @Test
    void composite2() {
        rewriteRun(
          kotlin(
            """
              package com.netflix.graphql.dgs.client.codegen

              import org.junit.jupiter.api.Test

              class GraphQLMultiQueryRequestTest {
                  private fun listAllFiles(suffix: String): String {
                      return ""
                  }
              }
              """
          )
        );
    }

    @Test
    void companionType() {
        rewriteRun(
          kotlin(
            """
              class A {
                  companion object {

                      @JvmField
                      val GRANT_TYPE = "password"
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "DataFlowIssue"})
    @Test
    void visitorAutoFormatTest() {
        K.CompilationUnit unFormatted = KotlinParser.builder().build()
          .parse("""
            class Test {
            fun method():String{return ""}
            }
            """)
          .map(K.CompilationUnit.class::cast)
          .findFirst()
          .get();

        K.CompilationUnit autoFormatted = (K.CompilationUnit) new KotlinIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object p) {
                return autoFormat(method, p);
            }
        }.visit(unFormatted, new InMemoryExecutionContext());

        assertThat(autoFormatted.printAll()).isEqualTo("""
          class Test {
              fun method(): String {
                  return ""
              }
          }
          """);
    }

    @Test
    void reorderImportsDefault() {
        rewriteRun(
          kotlin(
            """
              import java.util.LinkedList
              import java.util.HashMap
              import java.util.Calendar

              class T(l: LinkedList<String>, h: HashMap<String, String>, c: Calendar)
              """,
            """
              import java.util.Calendar
              import java.util.HashMap
              import java.util.LinkedList

              class T(l: LinkedList<String>, h: HashMap<String, String>, c: Calendar)
              """
          )
        );
    }

    @Test
    void reorderImportsAliasesSeparately() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(),
              singletonList((AddImportTest.importAliasesSeparatelyStyle()))
            )
          ))),
          kotlin(
            """
              import java.util.regex.Pattern as Pat
              import java.util.*

              class T(s: StringJoiner, p: Pat)
              """,
            """
              import java.util.*
              import java.util.regex.Pattern as Pat

              class T(s: StringJoiner, p: Pat)
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/370")
    @Test
    void doNotFoldImport() {
        rewriteRun(
          kotlin(
            """
              import java.util.Calendar
              import java.util.HashMap
              import java.util.LinkedList
              import java.util.Objects
              import java.util.StringJoiner

              class T(l: LinkedList<String>, h: HashMap<String, String>, c: Calendar) {
                  fun method() {
                      val x: Objects
                      val y: StringJoiner
                  }
              }
              """
          )
        );
    }
}
