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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class RemoveUnusedImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveUnusedImports());
    }

    @DocumentExample
    @Test
    void removesUnusedImportsAndSelectors() {
        rewriteRun(
          scala(
            """
              import java.util.UUID
              import java.util.{List, Map}

              class A {
                val a: List[String] = null
              }
              """,
            """
              import java.util.List

              class A {
                val a: List[String] = null
              }
              """
          )
        );
    }

    @Test
    void keepsImportUsedOnlyImplicitly() {
        rewriteRun(
          scala(
            """
              import scala.concurrent.ExecutionContext.Implicits.global
              import scala.concurrent.Future

              class A {
                val f = Future { 1 }
              }
              """
          )
        );
    }

    @Test
    void keepsImportThatDoesNotResolve() {
        rewriteRun(
          scala(
            """
              import akka.actor.ActorSystem
              import akka.actor.{Props, Actor => A}
              import akka.Defaults.given
              import java.util.UUID

              object X
              """,
            """
              import akka.actor.ActorSystem
              import akka.actor.{Props, Actor => A}
              import akka.Defaults.given

              object X
              """
          )
        );
    }

    @Test
    void keepsLanguageFeatureImport() {
        rewriteRun(
          scala(
            """
              import scala.language.postfixOps

              object A {
                val xs = List(1, 2) toList
              }
              """
          )
        );
    }

    @Test
    void givenImportIsKeptOnlyWhenAGivenIsSummoned() {
        rewriteRun(
          scala(
            """
              package example
              object Defaults {
                given Int = 5
              }
              """
          ),
          scala(
            """
              package example
              import example.Defaults.given
              object A {
                val x = summon[Int]
              }
              """
          ),
          scala(
            """
              package example
              import example.Defaults.given
              object B
              """,
            """
              package example
              object B
              """
          )
        );
    }

    @Test
    void keepsWildcardUsedOnlyByExtensionMethod() {
        rewriteRun(
          scala(
            """
              import scala.jdk.CollectionConverters._

              class A {
                val l = java.util.List.of(1).asScala
              }
              """
          )
        );
    }

    @Test
    void extensionMethodCallKeepsOnlyTheImportOfItsObject() {
        rewriteRun(
          scala(
            """
              package s
              object Syntax {
                extension (s: String) def shout(n: Int): String = s.toUpperCase
              }
              """
          ),
          scala(
            """
              package u
              import s.Syntax._
              class A {
                val x = "a".shout(1)
              }
              """
          ),
          scala(
            """
              package v
              import s.Syntax.shout
              class B {
                val x = "a".shout(1)
              }
              """
          ),
          scala(
            """
              package w
              import s.Syntax._
              class C {
                val o = new Object
                val x = o.equals(o)
              }
              """,
            """
              package w
              class C {
                val o = new Object
                val x = o.equals(o)
              }
              """
          )
        );
    }

    @Test
    void collapsesUsedSelectorOfAValueToAStaticImport() {
        rewriteRun(
          scala(
            """
              import scala.util.Properties.{versionString, javaVersion}

              class A {
                val v = versionString
              }
              """,
            """
              import scala.util.Properties.versionString

              class A {
                val v = versionString
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.Import collapsed = (J.Import) ((S.CompilationUnit) cu).getStatements().get(0);
                assertThat(collapsed.isStatic()).isTrue();
                assertThat(collapsed.getTypeName()).isEqualTo("scala.util.Properties");
            })
          )
        );
    }

    @Test
    void keepsPackageImportUsedAsQualifier() {
        rewriteRun(
          scala(
            """
              import scala.collection.mutable

              class A {
                val b = mutable.ListBuffer(1)
              }
              """
          )
        );
    }

    @Test
    void keepsImportOfATypeAlias() {
        rewriteRun(
          scala(
            """
              package t
              object T {
                type Str = String
                type MyList[A] = List[A]
              }
              """
          ),
          scala(
            """
              package u
              import t.T._
              class A {
                val s: Str = null
                val l: MyList[Int] = null
              }
              """
          ),
          scala(
            """
              package v
              import t.T.Str
              import t.T.MyList
              class B {
                val s: Str = null
                val l: MyList[Int] = null
              }
              """
          )
        );
    }

    @Test
    void importIsInScopeOnlyAfterItsPosition() {
        rewriteRun(
          scala(
            """
              class A {
                val u = java.util.UUID.randomUUID()
              }
              import java.util.UUID
              class B {
                val u = UUID.randomUUID()
              }
              import java.util.ArrayList
              class C {
                val l = new java.util.ArrayList[String]()
              }
              """,
            """
              class A {
                val u = java.util.UUID.randomUUID()
              }
              import java.util.UUID
              class B {
                val u = UUID.randomUUID()
              }
              class C {
                val l = new java.util.ArrayList[String]()
              }
              """
          )
        );
    }

    @Test
    void laterSelectorOfTheSameNameIsNoUse() {
        rewriteRun(
          scala(
            """
              import java.util.UUID
              import java.util.{UUID => U}

              class A {
                val u: U = null
              }
              """,
            """
              import java.util.{UUID => U}

              class A {
                val u: U = null
              }
              """
          )
        );
    }

    @Test
    void removesUnusedImportInClassBody() {
        rewriteRun(
          scala(
            """
              class A {
                import java.util.UUID
                val x = 1
              }
              """,
            """
              class A {
                val x = 1
              }
              """
          )
        );
    }

    @Test
    void keepsTheRestOfACommaSeparatedClause() {
        rewriteRun(
          scala(
            """
              import java.util.UUID, java.util.List, java.util.Map

              class A {
                val a: List[String] = null
                val b: Map[String, String] = null
              }
              """,
            """
              import java.util.List, java.util.Map

              class A {
                val a: List[String] = null
                val b: Map[String, String] = null
              }
              """
          )
        );
    }

    @Test
    void removesBaseOfARemovedRelativeImport() {
        rewriteRun(
          scala(
            """
              import scala.collection.mutable
              import mutable.ListBuffer

              class A {
                val a = 1
              }
              """,
            """
              class A {
                val a = 1
              }
              """
          )
        );
    }

    @Test
    void keepsHidingSelectorWithUsedWildcard() {
        rewriteRun(
          scala(
            """
              import java.util.{List => _, Map, _}

              class A {
                val a = new ArrayList[String]()
              }
              """,
            """
              import java.util.{List => _, _}

              class A {
                val a = new ArrayList[String]()
              }
              """
          )
        );
    }

    @Test
    void maybeRemoveImportRemovesOnlyThatImportAndOnlyIfUnused() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<ExecutionContext>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  maybeRemoveImport("scala.concurrent.ExecutionContext.Implicits.global");
                  maybeRemoveImport("java.util.Map");
                  return super.visitClassDeclaration(classDecl, ctx);
              }
          })),
          scala(
            """
              import scala.concurrent.ExecutionContext.Implicits.global
              import scala.concurrent.Future
              import java.util.{List, Map}

              class A {
                val f = Future { 1 }
              }
              """,
            """
              import scala.concurrent.ExecutionContext.Implicits.global
              import scala.concurrent.Future
              import java.util.List

              class A {
                val f = Future { 1 }
              }
              """
          )
        );
    }
}
