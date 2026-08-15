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

class AnnotatedExprTest implements RewriteTest {

    @Test
    void switchAnnotation() {
        rewriteRun(
          scala(
            """
              import scala.annotation.switch
              object Test {
                def f(n: Int): String = (n: @switch) match {
                  case 1 => "one"
                  case 2 => "two"
                  case _ => "other"
                }
              }
              """
          )
        );
    }

    @Test
    void uncheckedAnnotation() {
        rewriteRun(
          scala(
            """
              object Test {
                def f(x: Any): String = (x: @unchecked) match {
                  case s: String => s
                }
              }
              """
          )
        );
    }
    @Test
    void captureSetSuffixOnType() {
        rewriteRun(
          scala(
            """
            import language.experimental.captureChecking
            trait T {
              def f(xs: IterableOnce[Int]^): Int = 1
            }
            """
          )
        );
    }

    @Test
    void captureSetSuffixInReturnType() {
        rewriteRun(
          scala(
            """
            import language.experimental.captureChecking
            trait T {
              def f(): Iterator[Int]^ = Iterator.empty
            }
            """
          )
        );
    }

}
