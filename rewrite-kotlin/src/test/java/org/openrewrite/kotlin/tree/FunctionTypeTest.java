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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("unused")
class FunctionTypeTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/326")
    @Test
    void functionWithFunctionTypeParameter() {
        rewriteRun(
          kotlin(
            """
              class GradleSpigotDependencyLoaderTestBuilder(
                  var init: TestInitializer.() -> Unit = {}
              )

              class TestInitializer(
                  val resourcesDir: String
              )
              
              fun runTest() {
                  val builder = GradleSpigotDependencyLoaderTestBuilder()
                  builder.init(TestInitializer("null"))
              }
              """
          )
        );
    }

    @Test
    void nested() {
        rewriteRun(
          kotlin(
            """
              val f: ((Int) -> Boolean) -> Boolean = { true }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/310")
    @Test
    void generic() {
        rewriteRun(
          kotlin(
            """
              val f: Function<() -> Boolean> = { { true } }
              """
          )
        );
    }

    @Test
    void namedParameter() {
        rewriteRun(
          kotlin(
            """
              val f: (  p   :    Any ) -> Boolean = { true }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/275")
    @Test
    void parenthesizedNullableType() {
        rewriteRun(
          kotlin(
            """
              val v: (  (   Int )  -> Any) /*c1*/  ? = null
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/365")
    @Test
    void parenthesizedNullableTypeWithTrailingComment() {
        rewriteRun(
          kotlin(
            """
              val v: (  (   Int )  -> Any /*C*/)  ? = null
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/292")
    @Test
    void functionTypeParentheses() {
        rewriteRun(
          kotlin(
            """
              fun readMetadata(lookup: ((Class<Metadata>) -> Metadata?/*22*/)): Metadata {
                  return null!!
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/445")
    @Test
    void functionTypeWithModifiers() {
        rewriteRun(
          kotlin(
            """
              fun foo() :   suspend    ( param : Int )  -> Unit = { }
              """
          )
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/553")
    @Test
    void trailingAnnotation() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.TYPE)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno
              abstract class  Test   :    suspend @Anno (  )   ->    String
              """
          )
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/571")
    @Test
    void suspendBeforeParenthesized() {
        rewriteRun(
          kotlin(
            """
              class SomeReceiver
              suspend inline fun SomeReceiver  .   method(
                crossinline body  : suspend  (    SomeReceiver .  () -> Unit    )
              ) {}
              """
          )
        );
    }
}
