/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class TraitClassDeclarationTest implements RewriteTest {

    @Test
    void trait() {
        rewriteRun(
          groovy(
            """
              trait Foo {
              }
              """
          )
        );
    }

    @Test
    void traitExtendsTrait() {
        rewriteRun(
          groovy(
            """
              trait A {
              }
              trait B extends A {
              }
              """
          )
        );
    }

    @Test
    void traitWithMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMethodCallInBody() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet() {
                      println("hello")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMethodTakingParameter() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String greet(String name) {
                      "hello $name"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMultipleMethods() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String greet() {
                      "hello"
                  }

                  String shout(String msg) {
                      msg.toUpperCase()
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithField() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String prefix = "Hello"
              }
              """
          )
        );
    }

    @Test
    void traitWithPrivateField() {
        rewriteRun(
          groovy(
            """
              trait Counter {
                  private int count = 0
              }
              """
          )
        );
    }

    @Test
    void traitWithStaticField() {
        rewriteRun(
          groovy(
            """
              trait Counter {
                  static int MAX = 10
              }
              """
          )
        );
    }

    @Test
    void traitWithAbstractMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  abstract String greet()
              }
              """
          )
        );
    }

    @Test
    void traitWithVoidMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  void greet() {
                      println("hello")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithDefMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithVarargsMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String... names) {
                      names.join(", ")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithGenericMethod() {
        rewriteRun(
          groovy(
            """
              trait Container {
                  def <T> T pick(List<T> items) {
                      items.first()
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithIfStatement() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String name) {
                      if (name) {
                          println("hi " + name)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithForLoop() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greetAll(List<String> names) {
                      for (String name : names) {
                          println(name)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithClosureInBody() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greetAll(List<String> names) {
                      names.each { println(it) }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithGStringInBody() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String name) {
                      println("hello $name")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMultilineGString() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String name) {
                      println(\"\"\"
                          hello $name
                      \"\"\")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithCastExpression() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def asArray(List items) {
                      items.toArray() as String[]
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithLeftShiftOperator() {
        rewriteRun(
          groovy(
            """
              trait Builder {
                  StringBuilder sb = new StringBuilder()
                  def append(String s) {
                      sb << s
                  }
              }
              """
          )
        );
    }

    @Test
    void traitImplementsInterface() {
        rewriteRun(
          groovy(
            """
              trait Greeter implements Runnable {
                  void run() {
                      println("hello")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithAnnotatedMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  @Deprecated
                  def greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithStaticMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  static String greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithGenericTypeParameter() {
        rewriteRun(
          groovy(
            """
              trait Container<T> {
                  T item
              }
              """
          )
        );
    }
}
