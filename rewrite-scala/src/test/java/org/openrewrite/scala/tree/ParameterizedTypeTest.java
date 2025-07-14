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

class ParameterizedTypeTest implements RewriteTest {

    @Test
    void simpleParameterizedType() {
        rewriteRun(
          scala(
            """
            object Test {
              val list: List[String] = List("a", "b", "c")
            }
            """
          )
        );
    }

    @Test
    void multipleTypeParameters() {
        rewriteRun(
          scala(
            """
            object Test {
              val map: Map[String, Int] = Map("one" -> 1, "two" -> 2)
            }
            """
          )
        );
    }

    @Test
    void nestedParameterizedTypes() {
        rewriteRun(
          scala(
            """
            object Test {
              val nested: List[Option[String]] = List(Some("a"), None, Some("b"))
            }
            """
          )
        );
    }

    @Test
    void parameterizedTypeInMethodSignature() {
        rewriteRun(
          scala(
            """
            object Test {
              def getList(): List[Int] = List(1, 2, 3)
              
              def processMap(m: Map[String, Any]): Unit = {
                println(m)
              }
            }
            """
          )
        );
    }

    @Test
    void parameterizedTypeInNew() {
        rewriteRun(
          scala(
            """
            object Test {
              val list = new ArrayList[String]()
              val map = new HashMap[Int, String]()
            }
            """
          )
        );
    }

    @Test
    void wildcardType() {
        rewriteRun(
          scala(
            """
            object Test {
              def process(list: List[_]): Unit = {
                println(list.size)
              }
            }
            """
          )
        );
    }

    @Test
    void boundedTypeParameters() {
        rewriteRun(
          scala(
            """
            object Test {
              def sort[T <: Comparable[T]](list: List[T]): List[T] = {
                list.sorted
              }
            }
            """
          )
        );
    }

    @Test
    void varianceAnnotations() {
        rewriteRun(
          scala(
            """
            object Test {
              class Container[+T](value: T)
              class MutableContainer[-T]
              
              val container: Container[String] = new Container("test")
            }
            """
          )
        );
    }

    @Test
    void typeProjection() {
        rewriteRun(
          scala(
            """
            object Test {
              trait Outer {
                type Inner
              }
              
              def process(x: Outer#Inner): Unit = {}
            }
            """
          )
        );
    }

    @Test
    void higherKindedTypes() {
        rewriteRun(
          scala(
            """
            object Test {
              def transform[F[_], A, B](fa: F[A])(f: A => B): F[B] = ???
            }
            """
          )
        );
    }
}