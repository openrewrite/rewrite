/*
 * Copyright 2025 the original author or authors.
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

class TypeApplyTest implements RewriteTest {

    @Test
    void listEmpty() {
        rewriteRun(
          scala(
            """
              object Test {
                val empties: List[Int] = List.empty[Int]
              }
              """
          )
        );
    }

    @Test
    void arrayEmpty() {
        rewriteRun(
          scala(
            """
              object Test {
                val arr = Array.empty[String]
              }
              """
          )
        );
    }

    @Test
    void qualifiedModuleEmpty() {
        rewriteRun(
          scala(
            """
              import scala.collection.mutable
              object Test {
                val m = mutable.Map.empty[String, Int]
              }
              """
          )
        );
    }

    @Test
    void multipleTypeArgs() {
        rewriteRun(
          scala(
            """
              object Test {
                val m: Map[String, Int] = Map.empty[String, Int]
              }
              """
          )
        );
    }

    @Test
    void typeApplyWithComplexType() {
        rewriteRun(
          scala(
            """
              object Test {
                val l = List.empty[List[Int]]
              }
              """
          )
        );
    }

    @Test
    void typeAppliedFunctionIdent() {
        rewriteRun(
          scala(
            """
              object Test {
                def id[A](a: A): A = a
                val x = id[Int](5)
              }
              """
          )
        );
    }

    @Test
    void typeApplyInMethodChain() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = List(1, 2, 3).map[String](x => x.toString)
              }
              """
          )
        );
    }

    @Test
    void typeApplyWithChainedSelect() {
        rewriteRun(
          scala(
            """
              object Outer {
                object Inner {
                  def empty[A](): List[A] = Nil
                }
              }
              object Test {
                val x = Outer.Inner.empty[Int]()
              }
              """
          )
        );
    }
}
