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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

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
    void newClassWithMultiLineChainedArgument() {
        rewriteRun(
          scala(
            """
            object Test {
              val fmt = new CollectionInputFormat[Row](
                values.asJava,
                fromDataTypeToTypeInfo(getProducedDataType)
                  .createSerializer(new SerializerConfigImpl)
                  .asInstanceOf[TypeSerializer[Row]])
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
    void newClassWithInterpolatedStringArgOnNewLine() {
        rewriteRun(
          scala(
            """
            val n = 1
            val x = new RuntimeException(
              s"value: $n")
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

    @Test
    void curriedNewClass() {
        rewriteRun(
          scala(
            """
            val x = new Foo(a)(b)
            """
          )
        );
    }

    @Test
    void curriedNewClassAsDefBody() {
        rewriteRun(
          scala(
            """
            def make: Foo =
              new Foo(0)(f)
            """
          )
        );
    }

    @Test
    void curriedAnonymousClass() {
        rewriteRun(
          scala(
            """
            class Foo(a: Int)(b: Int)
            trait Bar
            object O {
              val x = new Foo(1)(2) with Bar {}
            }
            """
          )
        );
    }

    @Test
    void semicolonSeparatedAnonymousClassMembers() {
        rewriteRun(
            scala(
                """
                object O {
                  val x = new Object { val a = 1; val b = 2 }
                  val r = new Runnable { def run() = (); val y = 1 }
                }
                """
            )
        );
    }

    @Test
    void emptyStatementsInAnonymousClass() {
        rewriteRun(
            scala(
                """
                object O {
                  val x = new Object { ; val a = 1;; val b = 2; }
                }
                """
            )
        );
    }

    @Test
    void argumentsAreNeverNull() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  J.NewClass nc = super.visitNewClass(newClass, ctx);
                  return SearchResult.found(nc, String.valueOf(nc.getArguments().size()));
              }
          })),
          scala(
            """
            trait Logger
            class A {
              val r = new Runnable { def run(): Unit = () }
              val p = new Object
              val q = new Object()
              val b = new java.util.ArrayList[java.lang.Boolean] { }
              val l = new Object with Logger { }
              val s = new StringBuilder("x") { }
              val e = new {}
            }
            """,
            """
            trait Logger
            class A {
              val r = /*~~(0)~~>*/new Runnable { def run(): Unit = () }
              val p = /*~~(0)~~>*/new Object
              val q = /*~~(0)~~>*/new Object()
              val b = /*~~(0)~~>*/new java.util.ArrayList[java.lang.Boolean] { }
              val l = /*~~(0)~~>*/new Object with Logger { }
              val s = /*~~(1)~~>*/new StringBuilder("x") { }
              val e = /*~~(0)~~>*/new {}
            }
            """
          )
        );
    }

    @Test
    void anonymousClassWithEmptyParentheses() {
        rewriteRun(
          scala(
            """
            trait Logger
            object Test {
              val a = new Object() { }
              val b = new Object() with Logger { }
              val c = new Object ( ) { }
              val d = new StringBuilder("x") with Logger { }
              val e = new Object( )
              val f = new Object() with Logger
            }
            """
          )
        );
    }
}
