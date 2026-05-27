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

    @Test
    void newClassWithArgumentsAndMixin() {
        rewriteRun(
          scala(
            """
            class Service(conf: String, client: String)
            trait Logging
            object Test {
              val service = new Service(
                "conf",
                "client") with Logging {
              }
            }
            """
          )
        );
    }

    @Test
    void newClassWithMultiLineArguments() {
        rewriteRun(
          scala(
            """
            class Person(name: String, age: Int)
            object Test {
              val p = new Person(
                "John",
                30
              )
            }
            """
          )
        );
    }

    @Test
    void newAnonymousClassWithMixin() {
        rewriteRun(
          scala(
            """
            object Test {
              trait Greeter { def greet(): String }
              trait Logger { def log(msg: String): Unit }
              val x = new Greeter with Logger {
                def greet(): String = "hi"
                def log(msg: String): Unit = println(msg)
              }
            }
            """
          )
        );
    }

    @Test
    void newEmptyAnonymousClass() {
        rewriteRun(
          scala(
            """
            object Test {
              val empty = new {}
            }
            """
          )
        );
    }

    @Test
    void newAnonymousClassWithThreeMixins() {
        rewriteRun(
          scala(
            """
            object Test {
              trait A { def a(): Int }
              trait B { def b(): Int }
              trait C { def c(): Int }
              val x = new A with B with C {
                def a(): Int = 1
                def b(): Int = 2
                def c(): Int = 3
              }
            }
            """
          )
        );
    }

    @Test
    void mixinWithExtraSpaceAroundWith() {
        rewriteRun(
          scala(
            """
            object Test {
              trait A
              trait B
              trait C
              val x = new A  with  B  with  C {}
            }
            """
          )
        );
    }

    @Test
    void newAnonymousClassBracelessIndentedBody() {
        rewriteRun(
          scala(
            """
            object Test {
              val runnable = new Runnable:
                def run(): Unit = println("Running")
            }
            """
          )
        );
    }

    @Test
    void newClassFirstArgIsIfElseOnNewLine() {
        rewriteRun(
          scala(
            """
            object Test {
              val x = new Foo(
                if (cond) 1 else 2,
                3)
            }
            """
          )
        );
    }

    @Test
    void significantCharactersInComments() {
        // visitNewClassWithArgs — close paren in line comment
        rewriteRun(
          scala(
            """
            class Foo(val x: Int)
            val f = new Foo(1 // )
            )
            """
          )
        );
        // visitNew — close paren in block comment within non-empty arg list
        rewriteRun(
          scala(
            """
            class Foo(a: Int, b: Int)
            val f = new Foo(1 /* ) */, 2)
            """
          )
        );
    }
}
