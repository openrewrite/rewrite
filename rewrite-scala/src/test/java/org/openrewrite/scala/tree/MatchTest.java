/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class MatchTest implements RewriteTest {

    @Test
    void simpleMatch() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String => s
                case _ => "unknown"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuard() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String if s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithAtBinding() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(xs: List[Int]): Int = xs match {
                case all@List(head, _*) => head
                case _ => 0
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithAtBindingAndTypedPattern() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case msg@(_: String) => msg
                case _ => ""
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuardAndComplexCondition() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String if s.nonEmpty && s.length > 3 => s
                case i: Int if i > 0 => i.toString
                case _ => "other"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithUnapplyPattern() {
        rewriteRun(
          scala(
            """
            object Test {
              case class Person(name: String, age: Int)
              def handle(x: Any): String = x match {
                case Person(name, age) => name
                case _ => ""
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuardExtraWhitespace() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String  if  s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuardComment() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String /*guard*/ if /*filter*/ s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithAtBindingAndUnapply() {
        rewriteRun(
          scala(
            """
            object Test {
              case class Person(name: String, age: Int)
              def handle(x: Any): String = x match {
                case p@Person(name, _) => name
                case _ => ""
              }
            }
            """
          )
        );
    }
}
