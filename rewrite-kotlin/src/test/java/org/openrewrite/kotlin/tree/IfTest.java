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

@SuppressWarnings({"RedundantExplicitType", "KotlinConstantConditions", "ControlFlowWithEmptyBody", "CascadeIf", "LiftReturnOrAssignment"})
class IfTest implements RewriteTest {

    @Test
    void noElse() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val n : Int = 0
                  if ( n == 0 ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val n : Int = 0
                  if ( n == 0 ) {
                      val x = 0
                  } else if ( n == 1 ) {
                      val x = 1
                  } else {
                      val x = 2
                  }
              }
              """
          )
        );
    }

    @Test
    void singleLineIfElseStatements() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  var n : Int = 0
                  if ( n == 0 )
                      n = 1
                  else if ( n == 1 )
                      n = 2
                  else
                      n = 3
              }
              """
          )
        );
    }

    @Test
    void inRange() {
        rewriteRun(
          kotlin(
            """
              fun method ( n : Int ) {
                  if ( n in 1 .. 4 ) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/140")
    @Test
    void returnFromIfWithoutBody() {
        rewriteRun(
          kotlin(
            """
              fun method ( n : Int ) : List < Int > {
                  if ( n == 0 ) return emptyList ( )
                  if ( n == 1 ) return listOf ( 1 )
                  val list = mutableListOf < Int > ( )
                  return list
              }
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/138")
    @Test
    void inParens() {
        rewriteRun(
          kotlin(
            """
              fun method ( a : Any ) {
                   val any = ( if ( a is Boolean ) "true" else "false" )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/138")
    @Test
    void multipleDeSugaredParens() {
        rewriteRun(
          kotlin(
            """
              fun method ( a : Any? ) {
                  ( ( ( ( if ( ( ( a ) ) == ( ( null ) ) ) return ) ) ) )
                  val r = a
              }
              """
          )
        );
    }

    @Test
    void annotatedIf() {
        rewriteRun(
          kotlin(
            """
              fun foo(t: Boolean) {
                  @Suppress
                  if (t)
                      print("t")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/298")
    @Test
    void functionCallCondition() {
        rewriteRun(
          kotlin(
            """
              import kotlin.text.Regex

              fun foo(choices: List<Pair<String, String>>, peekedHeader: Regex) {
                  for ((_, adapter) in choices) {
                      if (adapter.matches(peekedHeader)) {
                          print("1")
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/390")
    @Test
    void typeParameter() {
        rewriteRun(
          kotlin(
            """
              val enabled = true
              fun foo(arg: String = if (enabled) "foo" else "bar"): String {
                return arg
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/394")
    @Test
    void fieldAccess() {
        rewriteRun(
          kotlin(
            """
              val foo = if (true) { "" } else { null }?.plus("bar")
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/392")
    @Test
    void postfixExpression() {
        rewriteRun(
          kotlin(
            """
              fun foo(arg: Boolean): String? {
                  return if (arg) "" else null
              }
              fun bar(arg: Boolean) {
                  if (arg) { foo(true) } else { foo(false) }!!
              }
              """
          )
        );
    }
}
