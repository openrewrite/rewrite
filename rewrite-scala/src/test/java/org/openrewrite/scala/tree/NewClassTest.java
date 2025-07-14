/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class NewClassTest implements RewriteTest {

    @Test
    void simpleNewClass() {
        rewriteRun(
          scala(
            """
            object Test {
              val p = new Person()
            }
            """
          )
        );
    }

    @Test
    void newClassWithArguments() {
        rewriteRun(
          scala(
            """
            object Test {
              val p = new Person("John", 30)
            }
            """
          )
        );
    }

    @Test
    void newClassWithoutParentheses() {
        rewriteRun(
          scala(
            """
            object Test {
              val p = new Person
            }
            """
          )
        );
    }

    @Test
    void newClassWithTypeParameters() {
        rewriteRun(
          scala(
            """
            object Test {
              val list = new ArrayList[String]()
            }
            """
          )
        );
    }

    @Test
    void newClassWithQualifiedName() {
        rewriteRun(
          scala(
            """
            object Test {
              val date = new java.util.Date()
            }
            """
          )
        );
    }

    @Test
    void newClassWithNamedArguments() {
        rewriteRun(
          scala(
            """
            object Test {
              val p = new Person(name = "John", age = 30)
            }
            """
          )
        );
    }

    @Test
    void newClassNested() {
        rewriteRun(
          scala(
            """
            object Test {
              val p = new Person(new Address("123 Main St"))
            }
            """
          )
        );
    }

    @Test
    void newAnonymousClass() {
        rewriteRun(
          scala(
            """
            object Test {
              val runnable = new Runnable {
                def run(): Unit = println("Running")
              }
            }
            """
          )
        );
    }

    @Test
    void newClassWithBlock() {
        rewriteRun(
          scala(
            """
            object Test {
              val p = new Person("John", 30) {
                val nickname = "Johnny"
              }
            }
            """
          )
        );
    }
}