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
import org.openrewrite.java.ChangeType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ChangeTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("java.util.ArrayList", "java.util.concurrent.CopyOnWriteArrayList", null));
    }

    @Test
    void importOfTheTypeIsReplacedAmongTheLeadingImports() {
        rewriteRun(
          scala(
            """
              import java.util.ArrayList

              class A {
                val a = new ArrayList[String]()
              }
              """,
            """
              import java.util.concurrent.CopyOnWriteArrayList

              class A {
                val a = new CopyOnWriteArrayList[String]()
              }
              """
          ),

          scala(
            """
              class B
              import java.util.ArrayList
              class C {
                val a = new ArrayList[String]()
              }
              """,
            """
              import java.util.concurrent.CopyOnWriteArrayList

              class B
              class C {
                val a = new CopyOnWriteArrayList[String]()
              }
              """
          )
        );
    }

    @Test
    void selectorMovesToTargetPackage() {
        rewriteRun(
          scala(
            """
              import java.util.{ArrayList, Map}

              class A {
                val a = new ArrayList[String]()
                val m: Map[String, String] = null
              }
              """,
            """
              import java.util.Map
              import java.util.concurrent.CopyOnWriteArrayList

              class A {
                val a = new CopyOnWriteArrayList[String]()
                val m: Map[String, String] = null
              }
              """
          )
        );
    }

    @Test
    void selectorStaysInSamePackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", null)),
          scala(
            """
              import java.util.{ArrayList, Map}

              class A {
                val a = new ArrayList[String]()
                val m: Map[String, String] = null
              }
              """,
            """
              import java.util.{LinkedList, Map}

              class A {
                val a = new LinkedList[String]()
                val m: Map[String, String] = null
              }
              """
          )
        );
    }

    @Test
    void renamingSelectorKeepsAlias() {
        rewriteRun(
          scala(
            """
              import java.util.{ArrayList => AL}

              class A {
                val a = new AL[String]()
              }
              """,
            """
              import java.util.concurrent.{CopyOnWriteArrayList => AL}

              class A {
                val a = new AL[String]()
              }
              """
          )
        );
    }

    @Test
    void importInClassBody() {
        rewriteRun(
          scala(
            """
              class A {
                import java.util.ArrayList
                val a = new ArrayList[String]()
              }
              """,
            """
              import java.util.concurrent.CopyOnWriteArrayList

              class A {
                val a = new CopyOnWriteArrayList[String]()
              }
              """
          )
        );
    }

    @Test
    void wildcardStillInUse() {
        rewriteRun(
          scala(
            """
              import java.util._

              class A {
                val a = new ArrayList[String]()
                val m: Map[String, String] = null
              }
              """,
            """
              import java.util._
              import java.util.concurrent.CopyOnWriteArrayList

              class A {
                val a = new CopyOnWriteArrayList[String]()
                val m: Map[String, String] = null
              }
              """
          )
        );
    }

    @Test
    void sameSimpleNameInAnotherPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.List", "scala.collection.immutable.List", null)),
          scala(
            """
              import java.util.{List, Map}

              class A {
                val a: List[String] = null
                val m: Map[String, String] = null
              }
              """,
            """
              import java.util.Map
              import scala.collection.immutable.List

              class A {
                val a: List[String] = null
                val m: Map[String, String] = null
              }
              """
          )
        );
    }
}
