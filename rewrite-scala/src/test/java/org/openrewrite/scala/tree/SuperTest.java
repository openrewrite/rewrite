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

class SuperTest implements RewriteTest {

    @Test
    void superMethodCall() {
        rewriteRun(
          scala(
            """
              trait Base { def greet(): Unit = println("base") }
              class Derived extends Base {
                override def greet(): Unit = {
                  super.greet()
                  println("derived")
                }
              }
              """
          )
        );
    }

    @Test
    void superFieldAccess() {
        rewriteRun(
          scala(
            """
              class Base { def name: String = "base" }
              class Derived extends Base {
                def combined: String = super.name + " derived"
              }
              """
          )
        );
    }

    @Test
    void superWithTraitQualifier() {
        rewriteRun(
          scala(
            """
              trait A { def hello: String = "A" }
              trait B { def hello: String = "B" }
              class C extends A with B {
                override def hello: String = super[A].hello
              }
              """
          )
        );
    }
}
