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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class MemberReferenceTest implements RewriteTest {

    @Test
    void fieldReference() {
        rewriteRun(
          kotlin(
            """
              class Test ( val answer : Int )
              fun method ( ) {
                  val l = listOf ( Test ( 42 ) )
                  l . map { Test :: answer }
              }
              """
          )
        );
    }

    @Test
    void fieldReferenceWithTypeParameter() {
        rewriteRun(
          kotlin(
            """
              class Test < T: Number > ( val answer : T )
              fun method ( ) {
                  val l = listOf ( Test ( 42) ) /*c*/
                  l . map {  Test <  Int   > :: answer }
              }
              """
          )
        );
    }

    @Test
    void methodReference() {
        rewriteRun(
          kotlin("val str = 42 :: toString ")
        );
    }

    @Test
    void getJavaClass() {
        rewriteRun(
          kotlin("val a = Int ::   class  .     java ")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/64")
    @Test
    void noReceiver() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  listOf ( 1 , 2 , 3 ) . map ( :: println )
              }
              """
          )
        );
    }

    @Test
    void conditionalFieldReference() {
        rewriteRun(
          kotlin(
            """
              class Test ( val a : Int , val b : Int )
              fun method ( ) {
                  val l = listOf ( Test ( 42 , 24 ) )
                  l . map { if ( true ) Test :: a else Test :: b }
              }
              """
          )
        );
    }

    @Test
    void anonymousClassArgument() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Supplier/*c*/

              val s1: Supplier<List<String>> = Supplier(::emptyList)
              val s2: Supplier<List<String>> = Supplier { emptyList() }

              val s3 = Supplier<List<String>>(::emptyList)
              val s4 = Supplier<List<String>> { emptyList() }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/376")
    @Test
    void memberReferenceWithParameterizedType() {
        rewriteRun(
          kotlin(
            """
              val x: MutableMap<String, String> = hashMapOf()
              val keys = x.entries.map(MutableMap.MutableEntry<String, String>::key)
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/409")
    @Test
    void nullableMethodReference() {
        rewriteRun(
          kotlin(
            """
              fun method ( s: List<String   ? > ) {
                  s.none( CharSequence  ?   :: isNullOrBlank)
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/386")
    @Test
    void firCallableReferenceAccess() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          kotlin(
            """
              open class Foo<out A, out B> {
                  data class Bar<out B>(val value: B) : Foo<Nothing, B>()
                  @Suppress("UNUSED_PARAMETER")
                  inline fun method(action: (right: B) -> Unit): Foo<A, B>? {
                      return null
                  }
              }
              fun test() {
                Foo.Bar(1).method(::unresolved)
              }
              """
          )
        );
    }
}
