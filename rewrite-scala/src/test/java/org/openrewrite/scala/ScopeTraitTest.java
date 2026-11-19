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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTraitTest implements RewriteTest {

    @Test
    void namesAreVisibleWhereScalaScopesThem() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"here".equals(m.getSimpleName())) {
                      return m;
                  }
                  Scope scope = Scope.at(getCursor());
                  String visible = Arrays.stream(new String[]{"top", "a", "b", "K", "L", "x", "all", "inner", "i", "j", "lam"})
                    .filter(name -> scope.lookup(name).isPresent())
                    .collect(joining(" "));
                  String helper = scope.lookup("helper", Scope.Namespace.METHOD).isPresent() ? " helper" : "";
                  return SearchResult.found(m, visible + helper);
              }
          })),
          scala(
            """
              object O {
                val top = 1
                def f(p: (Int, Int), s: Any): Int = {
                  here()
                  val (a, b) = p
                  val K, L = 1
                  s match {
                    case x: String => here()
                    case all @ Some(inner) => here()
                  }
                  for (i <- List(1); j = i) yield here()
                  List(1).foreach(lam => here())
                  def helper(): Int = 1
                  here()
                }
                def here(): Int = 0
              }
              """,
            """
              object O {
                val top = 1
                def f(p: (Int, Int), s: Any): Int = {
                  /*~~(top helper)~~>*/here()
                  val (a, b) = p
                  val K, L = 1
                  s match {
                    case x: String => /*~~(top a b K L x helper)~~>*/here()
                    case all @ Some(inner) => /*~~(top a b K L all inner helper)~~>*/here()
                  }
                  for (i <- List(1); j = i) yield /*~~(top a b K L i j helper)~~>*/here()
                  List(1).foreach(lam => /*~~(top a b K L lam helper)~~>*/here())
                  def helper(): Int = 1
                  /*~~(top a b K L helper)~~>*/here()
                }
                def here(): Int = 0
              }
              """
          )
        );
    }

    @Test
    void patternDefinitionBindsItsOwnNamesAndNotThoseOfACaseInItsValue() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"here".equals(m.getSimpleName())) {
                      return m;
                  }
                  Scope scope = Scope.at(getCursor());
                  return SearchResult.found(m, Stream.of("a", "b", "inCase")
                    .filter(name -> scope.lookup(name).isPresent())
                    .collect(joining(" ")));
              }
          })),
          scala(
            """
              object O {
                def f(s: Any): Int = {
                  val (a, b) = s match {
                    case inCase: Int => (inCase, inCase)
                  }
                  here()
                }
                def here(): Int = 0
              }
              """,
            """
              object O {
                def f(s: Any): Int = {
                  val (a, b) = s match {
                    case inCase: Int => (inCase, inCase)
                  }
                  /*~~(a b)~~>*/here()
                }
                def here(): Int = 0
              }
              """
          )
        );
    }

    @Test
    void referencesAndRenameSafetyFollowScalaScoping() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references, " +
                      (b.isEffectivelyFinal() ? "final" : "assigned") + ", " +
                      Stream.of("free", "taken", "x").map(n -> n + (b.canRenameTo(n) ? ":safe" : ":unsafe")).collect(joining(" "))))
                    .orElse(identifier);
              }
          })),
          scala(
            """
              object O {
                def m(p: Int): Int = {
                  val read_ = p
                  var assigned_ = p
                  val taken = 2
                  List(1).foreach(x => use(x + read_))
                  assigned_ = taken
                  read_ + assigned_
                }
                def use(i: Int): Unit = ()
              }
              """,
            """
              object O {
                def m(p: Int): Int = {
                  val /*~~(2 references, final, free:safe taken:unsafe x:unsafe)~~>*/read_ = p
                  var /*~~(2 references, assigned, free:safe taken:unsafe x:safe)~~>*/assigned_ = p
                  val taken = 2
                  List(1).foreach(x => use(x + read_))
                  assigned_ = taken
                  read_ + assigned_
                }
                def use(i: Int): Unit = ()
              }
              """
          )
        );
    }

    @Test
    void blockCanMoveAfterItsStatementUnlessALaterOneUsesItsNames() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  Cursor parent = getCursor().getParentTreeCursor();
                  if (!(parent.getValue() instanceof J.Block)) {
                      return iff;
                  }
                  Cursor block = new Cursor(getCursor(), iff.getThenPart());
                  return SearchResult.found(iff, Scope.at(block).canMoveAfter(getCursor()) ? "moves" : "stays");
              }
          })),
          scala(
            """
              object O {
                val field = 0
                def declaredLater(c: Boolean): Unit = {
                  if (c) { val x = 1 }
                  val x = 2
                }
                def readLater(c: Boolean): Unit = {
                  if (c) { val field = 1 }
                  use(field)
                }
                def untouched(c: Boolean): Unit = {
                  if (c) { val y = 1 }
                  use(field)
                }
                def use(i: Int): Unit = ()
              }
              """,
            """
              object O {
                val field = 0
                def declaredLater(c: Boolean): Unit = {
                  /*~~(stays)~~>*/if (c) { val x = 1 }
                  val x = 2
                }
                def readLater(c: Boolean): Unit = {
                  /*~~(stays)~~>*/if (c) { val field = 1 }
                  use(field)
                }
                def untouched(c: Boolean): Unit = {
                  /*~~(moves)~~>*/if (c) { val y = 1 }
                  use(field)
                }
                def use(i: Int): Unit = ()
              }
              """
          )
        );
    }
}
