/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("RemoveRedundantQualifierName")
class LambdaTest implements RewriteTest {

    @Test
    void binaryExpressionAsBody() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val square =  {   number    :     Int ->  number   *    number     }
              }
              """
          )
        );
    }

    @Test
    void invokedLambda() {
        rewriteRun(
          kotlin(
            """
              fun plugins ( input : ( ) -> String ) {
                println ( input( ) )
              }
              """
          )
        );
    }

    @Test
    void destructuredLambdaParams() {
        rewriteRun(
          kotlin(
            """
              abstract class SomeClass {

                  private val defaults = emptySet < String > ( )

                  abstract fun fields ( ) : List < Pair < String , Any ? > >

                  fun inputValues ( ) : List < Pair < String , Any ? > > {
                      return fields ( ) . filter { ( k , _ ) -> ! defaults . contains ( k ) }
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleDestructuredLambdaParams() {
        rewriteRun(
          kotlin(
            """
              val m = mapOf(Pair("", "")).forEach { (key, value) ->
                       println(key + value)
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/60")
    @Test
    void suspendLambda() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val lambda : suspend ( ) -> Int = suspend { 1 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/56")
    @Test
    void suspendLambdaWithParameter() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val lambda : suspend ( Int ) -> Int = { number : Int -> number * number }
              }
              """
          )
        );
    }

    @Test
    void ignored() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val list = listOf ( 1 , 2 , 3 )
                  list . filterIndexed { index , ignored -> index % 2 == 0 }
              }
              """
          )
        );
    }

    @Test
    void underscore() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val list = listOf ( 1 , 2 , 3 )
                  list . filterIndexed { index , _ -> index % 2 == 0 }
              }
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              val sum: (Int, Int, ) -> Int = { x, y , -> x + y }
              """
          )
        );
    }

    @Test
    void redundantArrow() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Supplier

              class Test {
                  fun method(n: Int) {
                      val ns: Supplier<Int> = Supplier {   ->
                          n
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/110")
    @Test
    void unusedVar() {
        rewriteRun(
          kotlin(
            """
              package foo
              class Bar {
                  companion object {
                      fun bar ( e : Any ) {
                      }
                  }
              }
              """
          ),
          kotlin(
            """
              import foo.Bar
              fun test() {
                  val a = Bar . bar {
                      _ : Any? ->
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/110")
    @Test
    void fullyQualifiedKotlinTypeReference() {
        rewriteRun(
          kotlin(
            """
              package foo
              class Bar {
                  companion object {
                      fun bar ( e : Any ) {
                      }
                  }
              }
              """
          ),
          kotlin(
            """
              import foo.Bar
              fun test() {
                  val a = Bar . bar {
                      _ : kotlin . Int? ->
                  }
              }
              """
          )
        );
    }

    @Test
    void underScoreAsLamdbaParameters() {
        rewriteRun(
          kotlin(
            """
              fun method(map : HashMap<String, String>) {
                  map.forEach { (_, value) -> println("$value!") }
              }
              """
          )
        );
    }
}
