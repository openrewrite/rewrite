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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AddImportTest implements RewriteTest {

    private static ScalaVisitor<ExecutionContext> addImport(String fullyQualifiedName, boolean onlyIfReferenced) {
        return new ScalaVisitor<>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                maybeAddImport(fullyQualifiedName, onlyIfReferenced);
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    @DocumentExample
    @Test
    void addsInOrder() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("java.util.UUID", false))),
          scala(
            """
              package p

              import akka.actor.ActorSystem

              import java.util.ArrayList
              import scala.concurrent.Future

              class A
              """,
            """
              package p

              import akka.actor.ActorSystem

              import java.util.ArrayList
              import java.util.UUID
              import scala.concurrent.Future

              class A
              """
          )
        );
    }

    @Test
    void addsAheadOfTheFirstStatementWhenThereAreNoImports() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("java.util.UUID", false))),
          scala(
            """
              package p

              class A
              """,
            """
              package p

              import java.util.UUID

              class A
              """
          ),

          scala(
            """
              class B
              """,
            """
              import java.util.UUID

              class B
              """
          )
        );
    }

    @Test
    void skipsNameImportedBySelectorOrWildcard() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  maybeAddImport("java.util.UUID", false);
                  maybeAddImport("java.time.Instant", false);
                  return super.visitClassDeclaration(classDecl, ctx);
              }
          })),
          scala(
            """
              import java.util.{List, UUID}
              import java.time._

              class A
              """
          )
        );
    }

    @Test
    void findsNameImportedAfterARenameOfIt() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("java.util.List", false))),
          scala(
            """
              import java.util.{List => JList}
              import java.util.List

              class A
              """
          )
        );
    }

    @Test
    void defaultImportIsVisibleUnlessAWildcardImportMayShadowIt() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  maybeAddImport("scala.collection.immutable.List", false);
                  maybeAddImport("scala.collection.immutable.Map", false);
                  maybeAddImport("scala.Option", false);
                  return super.visitClassDeclaration(classDecl, ctx);
              }
          })),
          scala(
            """
              class A
              """
          ),

          scala(
            """
              import java.util._

              class B
              """,
            """
              import java.util._
              import scala.Option
              import scala.collection.immutable.List
              import scala.collection.immutable.Map

              class B
              """
          )
        );
    }

    @Test
    void keepsSelectorOfAScalaAlias() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("scala.collection.immutable.List", false))),
          scala(
            """
              import scala.{List, Seq}

              class A
              """
          )
        );
    }

    @Test
    void keepsSelectorOfATypeAlias() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("java.lang.String", true))),
          scala(
            """
              package t
              object T {
                type Str = String
              }
              """
          ),
          scala(
            """
              package u
              import t.T.{Str}
              class A {
                val s: Str = null
              }
              """
          )
        );
    }

    @Test
    void doesNotFoldIntoWildcard() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("java.util.UUID", false))),
          scala(
            """
              import java.util.ArrayList
              import java.util.HashMap
              import java.util.HashSet
              import java.util.LinkedList
              import java.util.List

              class A
              """,
            """
              import java.util.ArrayList
              import java.util.HashMap
              import java.util.HashSet
              import java.util.LinkedList
              import java.util.List
              import java.util.UUID

              class A
              """
          )
        );
    }

    @Test
    void onlyIfReferenced() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> addImport("java.util.UUID", true))),
          scala(
            """
              class A {
                val u = java.util.UUID.randomUUID()
              }
              """
          )
        );
    }

    @Test
    void addsAfterPackageClauseOfFileWithoutStatements() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitCompilationUnit(S.CompilationUnit cu, ExecutionContext ctx) {
                  maybeAddImport("java.util.UUID", false);
                  return super.visitCompilationUnit(cu, ctx);
              }
          })),
          scala(
            """
              package p
              """,
            """
              package p

              import java.util.UUID
              """
          )
        );
    }

    @Test
    void wildcardImportDoesNotProvideARename() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  maybeAddImport("java.util", "List", null, "JList", false);
                  return super.visitClassDeclaration(classDecl, ctx);
              }
          })),
          scala(
            """
              import java.util._

              class A
              """,
            """
              import java.util._
              import java.util.{List => JList}

              class A
              """
          )
        );
    }
}
