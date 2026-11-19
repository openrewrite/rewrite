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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Cursor;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.trait.VariableAccess;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTraitTest implements RewriteTest {

    @Test
    void namesAreVisibleWhereKotlinScopesThem() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> {
              String visible = Arrays.stream(new String[]{"top", "prop", "a", "b", "w", "it", "x"})
                .filter(name -> scope.lookup(name).isPresent())
                .map(name -> "it".equals(name) ? name + "=" + scope.lookup(name).get().getKind() : name)
                .collect(joining(" "));
              return visible.isEmpty() ? "none" : visible;
          })),
          kotlin(
            """
              val top = 1
              class C(val prop: Int) {
                  fun m(p: Pair<Int, Int>, s: Any) {
                      val (a, b) = p
                      when (val w = s) {
                          is String -> here()
                      }
                      here()
                      listOf(1).forEach { here() }
                      listOf(1).forEach { x -> here() }
                  }
              }
              fun here() {}
              """,
            """
              val top = 1
              class C(val prop: Int) {
                  fun m(p: Pair<Int, Int>, s: Any) {
                      val (a, b) = p
                      when (val w = s) {
                          is String -> /*~~(top prop a b w)~~>*/here()
                      }
                      /*~~(top prop a b)~~>*/here()
                      listOf(1).forEach { /*~~(top prop a b it=PARAMETER)~~>*/here() }
                      listOf(1).forEach { x -> /*~~(top prop a b x)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void functionWithoutAReturnTypeIsBoundAsAMethod() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("here", Scope.Namespace.METHOD).isPresent() ? "bound" : "unbound")),
          kotlin(
            """
              fun m() {
                  here()
              }
              fun here() {}
              """,
            """
              fun m() {
                  /*~~(bound)~~>*/here()
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void localFunctionIsVisibleInItsOwnBody() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("local", Scope.Namespace.METHOD).isPresent() ? "bound" : "unbound")),
          kotlin(
            """
              fun m() {
                  fun local() {
                      here()
                  }
              }
              fun here() {}
              """,
            """
              fun m() {
                  fun local() {
                      /*~~(bound)~~>*/here()
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void parameterIsVisibleInTheDefaultOfALaterOne() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> Stream.of("first", "second", "third")
            .filter(name -> scope.lookup(name).isPresent())
            .collect(joining(" ")))),
          kotlin(
            """
              fun m(first: Int, second: Any = here(), third: Int = 0) {}
              fun here() {}
              """,
            """
              fun m(first: Int, second: Any = /*~~(first)~~>*/here(), third: Int = 0) {}
              fun here() {}
              """
          )
        );
    }

    @Test
    void namedArgumentIsNotAVariableAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new VariableAccess.Matcher()
            .asVisitor(access -> SearchResult.found(access.getTree())))),
          kotlin(
            """
              fun g(named: Int) {}
              fun m(local: Int) {
                  g(named = local)
              }
              """,
            """
              fun g(named: Int) {}
              fun m(local: Int) {
                  g(named = /*~~>*/local)
              }
              """
          )
        );
    }

    @Test
    void freeNameAvoidsALaterDestructuredName() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> "a=" + scope.freeName("a", n -> false) + ", c=" + scope.freeName("c", n -> false))),
          kotlin(
            """
              fun m(pair: Pair<Int, Int>) {
                  here()
                  val (a, b) = pair
              }
              fun here() {}
              """,
            """
              fun m(pair: Pair<Int, Int>) {
                  /*~~(a=a1, c=c)~~>*/here()
                  val (a, b) = pair
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void referencesAndRenameSafetyFollowKotlinScoping() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references, " +
                      (b.isEffectivelyFinal() ? "final" : "assigned") + ", " +
                      Stream.of("free", "taken", "it").map(n -> n + (b.canRenameTo(n) ? ":safe" : ":unsafe")).collect(joining(" "))))
                    .orElse(identifier);
              }
          })),
          kotlin(
            """
              fun m(p: Int): Int {
                  val read_ = p
                  var assigned_ = p
                  val taken = 2
                  listOf(1).forEach { use(it + read_) }
                  assigned_ = taken
                  return read_ + assigned_
              }
              fun use(i: Int) {}
              class C(val property_: Int) {
                  val taken = 1
                  fun get() = property_
              }
              """,
            """
              fun m(p: Int): Int {
                  val /*~~(2 references, final, free:safe taken:unsafe it:unsafe)~~>*/read_ = p
                  var /*~~(2 references, assigned, free:safe taken:unsafe it:safe)~~>*/assigned_ = p
                  val taken = 2
                  listOf(1).forEach { use(it + read_) }
                  assigned_ = taken
                  return read_ + assigned_
              }
              fun use(i: Int) {}
              class C(val /*~~(1 references, final, free:safe taken:unsafe it:safe)~~>*/property_: Int) {
                  val taken = 1
                  fun get() = property_
              }
              """
          )
        );
    }

    @Test
    void blockCanMoveAfterItsStatementUnlessANameWouldClash() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
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
          kotlin(
            """
              val field = 0
              fun declaredLater(c: Boolean) {
                  if (c) { val x = 1 }
                  val x = 2
              }
              fun readLater(c: Boolean) {
                  if (c) { val field = 1 }
                  use(field)
              }
              fun untouched(c: Boolean) {
                  if (c) { val y = 1 }
                  use(field)
              }
              fun declaredEarlier(c: Boolean) {
                  val x = 0
                  if (c) { val x = 1 }
              }
              fun use(i: Int) {}
              """,
            """
              val field = 0
              fun declaredLater(c: Boolean) {
                  /*~~(stays)~~>*/if (c) { val x = 1 }
                  val x = 2
              }
              fun readLater(c: Boolean) {
                  /*~~(stays)~~>*/if (c) { val field = 1 }
                  use(field)
              }
              fun untouched(c: Boolean) {
                  /*~~(moves)~~>*/if (c) { val y = 1 }
                  use(field)
              }
              fun declaredEarlier(c: Boolean) {
                  val x = 0
                  /*~~(stays)~~>*/if (c) { val x = 1 }
              }
              fun use(i: Int) {}
              """
          )
        );
    }

    @Test
    void nestedClassReferencesAPropertyOfTheOuterOneOnlyIfInner() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("prop").map(scope::canReference).map(String::valueOf).orElse("unbound"))),
          kotlin(
            """
              class Outer {
                  val prop = 1
                  fun own() { here() }
                  class Nested {
                      fun m() { here() }
                  }
                  inner class Inner {
                      fun m() { here() }
                  }
                  companion object {
                      fun c() { here() }
                  }
              }
              fun here() {}
              """,
            """
              class Outer {
                  val prop = 1
                  fun own() { /*~~(true)~~>*/here() }
                  class Nested {
                      fun m() { /*~~(false)~~>*/here() }
                  }
                  inner class Inner {
                      fun m() { /*~~(true)~~>*/here() }
                  }
                  companion object {
                      fun c() { /*~~(false)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    private static Recipe atHere(Function<Scope, String> question) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return "here".equals(m.getSimpleName()) ? SearchResult.found(m, question.apply(Scope.at(getCursor()))) : m;
            }
        });
    }
}
