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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class OrderImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderImports());
    }

    @DocumentExample
    @Test
    void groupsJavaAndScalaImportsAfterOthers() {
        rewriteRun(
          scala(
            """
              package p

              import scala.concurrent.Future
              import java.util.{Map, List}
              import akka.actor.ActorSystem
              import java.util.ArrayList

              class A
              """,
            """
              package p

              import akka.actor.ActorSystem

              import java.util.ArrayList
              import java.util.{List, Map}
              import scala.concurrent.Future

              class A
              """
          )
        );
    }

    @Test
    void doesNotMoveImportsPastDefinitions() {
        rewriteRun(
          scala(
            """
              import java.util.UUID
              class A {
                val u = UUID.randomUUID()
              }
              import java.util.ArrayList
              class B {
                val l = new ArrayList[String]()
              }
              """
          )
        );
    }

    @Test
    void doesNotFoldIntoWildcard() {
        rewriteRun(
          scala(
            """
              import java.util.ArrayList
              import java.util.HashMap
              import java.util.HashSet
              import java.util.LinkedList
              import java.util.List
              import java.util.Map

              class A {
                val a: List[String] = new ArrayList[String]()
                val b: Map[String, String] = new HashMap[String, String]()
                val c = new HashSet[String]()
                val d = new LinkedList[String]()
              }
              """
          )
        );
    }

    @Test
    void keepsRelativeImportAfterItsBase() {
        rewriteRun(
          scala(
            """
              import scala.collection.mutable
              import mutable.ListBuffer
              import java.util.UUID

              class A
              """,
            """
              import java.util.UUID
              import scala.collection.mutable

              import mutable.ListBuffer

              class A
              """
          )
        );
    }

    @Test
    void putsWildcardSelectorLast() {
        rewriteRun(
          scala(
            """
              import java.util.{_, Map => M, List => _}

              class A
              """,
            """
              import java.util.{List => _, Map => M, _}

              class A
              """
          )
        );
    }

    @Test
    void ordersImportsInClassBody() {
        rewriteRun(
          scala(
            """
              class A {
                import java.util.UUID
                import java.util.ArrayList
                val x = 1
              }
              """,
            """
              class A {
                import java.util.ArrayList
                import java.util.UUID
                val x = 1
              }
              """
          )
        );
    }
}
